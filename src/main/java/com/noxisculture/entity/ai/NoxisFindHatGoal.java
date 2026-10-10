package com.noxisculture.entity.ai;

import com.noxisculture.block.ModBlocks;
import com.noxisculture.block.custom.NoxisHatBlock;
import com.noxisculture.item.NoxisHatColors;
import net.minecraft.world.item.ItemStack;
import com.noxisculture.entity.idle.NoxisHatAnimation;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Buscar un sombrero (servidor). Un Noxis sin sombrero (porque se lo sacaron mientras dormía,
 * lo rompieron o lo agarró un jugador) mira cada tanto a su alrededor: si encuentra un
 * Sombrero de Noxis, sea bloque colocado o ítem tirado en el piso, va hasta él y se lo pone
 * con la animación de siempre. Sirve cualquier sombrero, no solo el suyo (también los teñidos,
 * cuando existan).
 *
 * <p>El sombrero se consume al ponérselo (el bloque desaparece o el ítem se descuenta), así
 * nunca se duplica. No toma el sombrero que otro Noxis dejó al lado de su pecera mientras
 * duerme. Si otro llega primero, se rinde sin drama.</p>
 */
public class NoxisFindHatGoal<T extends PathfinderMob & NoxisBowlSleeper> extends Goal {
    private static final int RADIUS = 15;
    /** Los violetas les gustan más: cuentan como si estuvieran más cerca (pero ninguno se rechaza). */
    private static final double VIOLET_BONUS = 0.6D;
    /** En la búsqueda al despertarse: no se aleja más que esto del lugar donde se despertó. */
    private static final double SEARCH_LEASH = 15.0D;
    private static final int ROAM_RESCAN = 20;           // mientras recorre, mira cada 1 s
    private static final int ROAM_LEG = 120;             // cada tramo del recorrido, como mucho 6 s
    private static final int HEIGHT = 3;
    private static final int SEARCH_INTERVAL = 60;       // mira cada 3 s (más o menos)
    private static final int WALK_TIMEOUT = 400;         // 20 s para llegar
    private static final double REACH_BLOCK = 1.8D;
    private static final double REACH_ITEM = 1.3D;

    private enum Phase { WALK, PUT_ON, ROAM, DONE }

    private final T mob;
    private Phase phase = Phase.DONE;
    private int ticks;
    private int nextSearchTick;
    private @Nullable BlockPos targetBlock;
    private @Nullable ItemEntity targetItem;
    private int roamLegTicks;
    private final NoxisApproach approacher;
    /** Sombreros a los que no pudo llegar: no los vuelve a intentar por un rato (30 s). */
    private final java.util.Map<Object, Integer> blacklist = new java.util.HashMap<>();
    private static final int BLACKLIST_TICKS = 600;

    public NoxisFindHatGoal(T mob) {
        this.mob = mob;
        this.approacher = new NoxisApproach(mob);
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean canSearch() {
        return !this.mob.hasHat() && !this.mob.isInBowl() && this.mob.getBowlPos() == null
                && this.mob.canGoToBowl();
    }

    @Override
    public boolean canUse() {
        if (!this.canSearch()) return false;
        boolean searching = this.mob.getHatSearchOrigin() != null;
        if (!searching && this.mob.tickCount < this.nextSearchTick) return false;
        this.nextSearchTick = this.mob.tickCount + SEARCH_INTERVAL + this.mob.getRandom().nextInt(30);
        this.targetBlock = null;
        this.targetItem = null;
        this.findHat();
        // Recién despertado sin sombrero: aunque no vea ninguno, sale a recorrer la zona.
        return this.targetBlock != null || this.targetItem != null || searching;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.approacher.reset();
        if (this.targetBlock != null || this.targetItem != null) {
            this.phase = Phase.WALK;
        } else {
            this.startRoamLeg();
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.phase != Phase.DONE;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.ticks++;
        Level level = this.mob.level();
        if (this.phase == Phase.ROAM) {
            this.tickRoam();
            return;
        }
        if (!this.targetStillThere(level)) {
            // Otro llegó primero (o lo levantaron): sigue con lo suyo.
            this.mob.setHatAnim(NoxisHatAnimation.NONE);
            this.phase = Phase.DONE;
            return;
        }
        double tx, ty, tz;
        if (this.targetBlock != null) {
            tx = this.targetBlock.getX() + 0.5D;
            ty = this.targetBlock.getY();
            tz = this.targetBlock.getZ() + 0.5D;
        } else {
            ItemEntity item = this.targetItem;
            tx = item.getX();
            ty = item.getY();
            tz = item.getZ();
        }
        this.mob.getLookControl().setLookAt(tx, ty + 0.2D, tz);
        switch (this.phase) {
            case WALK -> {
                if (!this.canSearch()) { this.phase = Phase.DONE; return; }
                double dx = tx - this.mob.getX();
                double dz = tz - this.mob.getZ();
                double reach = this.targetBlock != null ? REACH_BLOCK : REACH_ITEM;
                boolean arrived = dx * dx + dz * dz < reach * reach && Math.abs(ty - this.mob.getY()) < 1.5D;
                NoxisApproach.Result r = this.ticks >= WALK_TIMEOUT ? NoxisApproach.Result.FAILED
                        : this.approacher.tick(new Vec3(tx, ty, tz), this.targetBlock, 1, arrived, 0.5D);
                switch (r) {
                    case ARRIVED -> {
                        this.ticks = 0;
                        this.phase = Phase.PUT_ON;
                        this.mob.setHatAnim(NoxisHatAnimation.ON);
                    }
                    case REPLAN -> { }                          // sigue intentando (recalcula el camino)
                    case FAILED -> {
                        // No hay forma de llegar: ese sombrero queda descartado un rato.
                        this.blacklist(this.targetBlock != null ? this.targetBlock : this.targetItem.getUUID());
                        this.phase = Phase.DONE;
                    }
                    default -> { }
                }
            }
            case PUT_ON -> {
                this.mob.getNavigation().stop();
                if (this.ticks == NoxisHatAnimation.ON_TAKE_TICK) {
                    this.takeHat(level);
                } else if (this.ticks >= NoxisHatAnimation.ON_LENGTH) {
                    this.mob.setHatAnim(NoxisHatAnimation.NONE);
                    this.phase = Phase.DONE;
                }
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ recorrer la zona

    /** Camina hacia otro punto de la zona, sin alejarse de donde se despertó. */
    private void startRoamLeg() {
        this.phase = Phase.ROAM;
        this.roamLegTicks = 0;
        BlockPos origin = this.mob.getHatSearchOrigin();
        if (origin == null) { this.phase = Phase.DONE; return; }
        Vec3 center = Vec3.atBottomCenterOf(origin);
        for (int i = 0; i < 6; i++) {
            Vec3 p = this.mob.position().distanceTo(center) > SEARCH_LEASH * 0.6D
                    ? LandRandomPos.getPosTowards(this.mob, 8, 4, center)
                    : LandRandomPos.getPos(this.mob, 8, 4);
            if (p != null && p.distanceTo(center) <= SEARCH_LEASH) {
                this.mob.getNavigation().moveTo(p.x, p.y, p.z, 0.45D);
                return;
            }
        }
        this.mob.getNavigation().moveTo(center.x, center.y, center.z, 0.45D);   // vuelve al centro
    }

    private void tickRoam() {
        if (!this.canSearch() || this.mob.getHatSearchOrigin() == null) {
            // Se acabó el tiempo de búsqueda (o algo más importante): vuelve a lo de siempre.
            this.mob.stopHatSearch();
            this.phase = Phase.DONE;
            return;
        }
        this.roamLegTicks++;
        if (this.ticks % ROAM_RESCAN == 0) {
            this.findHat();
            if (this.targetBlock != null || this.targetItem != null) {
                this.mob.getNavigation().stop();
                this.approacher.reset();
                this.ticks = 0;
                this.phase = Phase.WALK;                        // ¡vio uno!
                return;
            }
        }
        // Mira para los costados mientras camina, como buscando.
        if (this.ticks % 30 == 0) {
            double a = this.mob.getRandom().nextDouble() * Math.PI * 2.0D;
            this.mob.getLookControl().setLookAt(this.mob.getX() + Math.cos(a) * 4.0D, this.mob.getEyeY() - 0.6D,
                    this.mob.getZ() + Math.sin(a) * 4.0D);
        }
        if (this.mob.getNavigation().isDone() || this.roamLegTicks >= ROAM_LEG) {
            this.startRoamLeg();
        }
    }

    /** Lo agarra (ese sombrero exacto, con sus colores): el bloque desaparece o el ítem se descuenta. */
    private void takeHat(Level level) {
        if (this.targetBlock != null) {
            this.mob.setHatItem(NoxisHatBlock.takeHat(level, this.targetBlock));
            level.playSound(null, this.targetBlock, ModBlocks.NOXIS_HAT.defaultBlockState().getSoundType().getBreakSound(),
                    SoundSource.NEUTRAL, 0.5F, 1.2F);
        } else if (this.targetItem != null) {
            ItemStack hat = this.targetItem.getItem().split(1);
            if (this.targetItem.getItem().isEmpty()) this.targetItem.discard();
            else this.targetItem.setItem(this.targetItem.getItem().copy());   // avisa a los jugadores
            this.mob.setHatItem(hat);
            this.mob.playSound(ModBlocks.NOXIS_HAT.defaultBlockState().getSoundType().getBreakSound(), 0.5F, 1.2F);
        }
        this.mob.stopHatSearch();
    }

    private boolean targetStillThere(Level level) {
        if (this.phase == Phase.PUT_ON && this.ticks > NoxisHatAnimation.ON_TAKE_TICK) return true;   // ya lo tiene
        if (this.targetBlock != null) return level.getBlockState(this.targetBlock).is(ModBlocks.NOXIS_HAT);
        return this.targetItem != null && this.targetItem.isAlive() && !this.targetItem.getItem().isEmpty();
    }

    private void blacklist(Object key) {
        this.blacklist.put(key, this.mob.tickCount + BLACKLIST_TICKS);
    }

    private boolean isBlacklisted(Object key) {
        Integer until = this.blacklist.get(key);
        if (until == null) return false;
        if (this.mob.tickCount > until) {
            this.blacklist.remove(key);
            return false;
        }
        return true;
    }

    /** El sombrero más cercano (bloque o ítem) al que pueda llegar. */
    private void findHat() {
        this.targetBlock = null;
        this.targetItem = null;
        Level level = this.mob.level();
        BlockPos origin = this.mob.getHatSearchOrigin();
        double leash = (SEARCH_LEASH + 3.0D) * (SEARCH_LEASH + 3.0D);
        // Sombreros que otros Noxis dejaron al lado de su pecera: esos no se tocan.
        Set<BlockPos> reserved = new HashSet<>();
        List<PathfinderMob> others = level.getEntitiesOfClass(PathfinderMob.class,
                this.mob.getBoundingBox().inflate(RADIUS + 8, HEIGHT + 4, RADIUS + 8),
                e -> e != this.mob && e instanceof NoxisBowlSleeper);
        for (PathfinderMob other : others) {
            BlockPos p = ((NoxisBowlSleeper) other).getHatPos();
            if (p != null) reserved.add(p);
        }
        double best = Double.MAX_VALUE;
        // Ítems tirados en el piso.
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class,
                this.mob.getBoundingBox().inflate(RADIUS, HEIGHT, RADIUS),
                e -> e.isAlive() && e.getItem().is(ModBlocks.NOXIS_HAT.asItem()));
        for (ItemEntity item : items) {
            double d = this.mob.distanceToSqr(item) * (NoxisHatColors.looksViolet(item.getItem()) ? VIOLET_BONUS : 1.0D);
            if (d >= best || this.isBlacklisted(item.getUUID())) continue;
            if (origin != null && origin.distToCenterSqr(item.position()) > leash) continue;
            if (d > 6.25D && this.mob.getNavigation().createPath(item, 1) == null) continue;   // de cerca va derecho
            best = d;
            this.targetItem = item;
        }
        // Sombreros colocados como bloque.
        BlockPos here = this.mob.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(here.offset(-RADIUS, -HEIGHT, -RADIUS),
                here.offset(RADIUS, HEIGHT, RADIUS))) {
            if (!level.getBlockState(pos).is(ModBlocks.NOXIS_HAT) || reserved.contains(pos)) continue;
            if (this.isBlacklisted(pos.immutable())) continue;
            double d = pos.distToCenterSqr(this.mob.position())
                    * (NoxisHatColors.looksViolet(NoxisHatBlock.peekHat(level, pos)) ? VIOLET_BONUS : 1.0D);
            if (d >= best) continue;
            if (origin != null && pos.distSqr(origin) > leash) continue;
            if (d > 6.25D && this.mob.getNavigation().createPath(pos, 1) == null) continue;
            best = d;
            this.targetBlock = pos.immutable();
            this.targetItem = null;
        }
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
        this.mob.setHatAnim(NoxisHatAnimation.NONE);
        this.targetBlock = null;
        this.targetItem = null;
        this.phase = Phase.DONE;
    }
}
