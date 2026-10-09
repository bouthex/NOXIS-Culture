package com.noxisculture.entity.ai;

import com.noxisculture.block.ModBlocks;
import com.noxisculture.entity.idle.NoxisHatAnimation;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
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
    private static final int RADIUS = 10;
    private static final int HEIGHT = 3;
    private static final int SEARCH_INTERVAL = 60;       // mira cada 3 s (más o menos)
    private static final int WALK_TIMEOUT = 400;         // 20 s para llegar
    private static final double REACH_BLOCK = 1.8D;
    private static final double REACH_ITEM = 1.3D;

    private enum Phase { WALK, PUT_ON, DONE }

    private final T mob;
    private Phase phase = Phase.DONE;
    private int ticks;
    private int nextSearchTick;
    private @Nullable BlockPos targetBlock;
    private @Nullable ItemEntity targetItem;

    public NoxisFindHatGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean canSearch() {
        return !this.mob.hasHat() && !this.mob.isInBowl() && this.mob.getBowlPos() == null
                && this.mob.canGoToBowl();
    }

    @Override
    public boolean canUse() {
        if (!this.canSearch() || this.mob.tickCount < this.nextSearchTick) return false;
        this.nextSearchTick = this.mob.tickCount + SEARCH_INTERVAL + this.mob.getRandom().nextInt(30);
        this.targetBlock = null;
        this.targetItem = null;
        this.findHat();
        return this.targetBlock != null || this.targetItem != null;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.phase = Phase.WALK;
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
                if (dx * dx + dz * dz < reach * reach && Math.abs(ty - this.mob.getY()) < 1.5D) {
                    this.mob.getNavigation().stop();
                    this.ticks = 0;
                    this.phase = Phase.PUT_ON;
                    this.mob.setHatAnim(NoxisHatAnimation.ON);
                } else if (this.ticks >= WALK_TIMEOUT) {
                    this.phase = Phase.DONE;
                } else if (this.ticks % 20 == 1 || this.mob.getNavigation().isDone()) {
                    if (this.targetBlock != null) {
                        Path path = this.mob.getNavigation().createPath(this.targetBlock, 1);
                        if (path == null) { this.phase = Phase.DONE; return; }
                        this.mob.getNavigation().moveTo(path, 0.5D);
                    } else {
                        this.mob.getNavigation().moveTo(tx, ty, tz, 0.5D);
                    }
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

    /** Lo agarra: el bloque desaparece o el ítem se descuenta, y queda puesto. */
    private void takeHat(Level level) {
        if (this.targetBlock != null) {
            level.removeBlock(this.targetBlock, false);
            level.playSound(null, this.targetBlock, ModBlocks.NOXIS_HAT.defaultBlockState().getSoundType().getBreakSound(),
                    SoundSource.NEUTRAL, 0.5F, 1.2F);
        } else if (this.targetItem != null) {
            this.targetItem.getItem().shrink(1);
            if (this.targetItem.getItem().isEmpty()) this.targetItem.discard();
            this.mob.playSound(ModBlocks.NOXIS_HAT.defaultBlockState().getSoundType().getBreakSound(), 0.5F, 1.2F);
        }
        this.mob.setHasHat(true);
    }

    private boolean targetStillThere(Level level) {
        if (this.phase == Phase.PUT_ON && this.ticks > NoxisHatAnimation.ON_TAKE_TICK) return true;   // ya lo tiene
        if (this.targetBlock != null) return level.getBlockState(this.targetBlock).is(ModBlocks.NOXIS_HAT);
        return this.targetItem != null && this.targetItem.isAlive() && !this.targetItem.getItem().isEmpty();
    }

    /** El sombrero más cercano (bloque o ítem) al que pueda llegar. */
    private void findHat() {
        Level level = this.mob.level();
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
            double d = this.mob.distanceToSqr(item);
            if (d >= best) continue;
            if (this.mob.getNavigation().createPath(item, 1) == null) continue;
            best = d;
            this.targetItem = item;
        }
        // Sombreros colocados como bloque.
        BlockPos origin = this.mob.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-RADIUS, -HEIGHT, -RADIUS),
                origin.offset(RADIUS, HEIGHT, RADIUS))) {
            if (!level.getBlockState(pos).is(ModBlocks.NOXIS_HAT) || reserved.contains(pos)) continue;
            double d = pos.distToCenterSqr(this.mob.position());
            if (d >= best) continue;
            if (this.mob.getNavigation().createPath(pos, 1) == null) continue;
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
