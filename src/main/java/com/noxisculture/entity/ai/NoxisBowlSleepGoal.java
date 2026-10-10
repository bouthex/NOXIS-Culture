package com.noxisculture.entity.ai;

import com.noxisculture.block.ModBlocks;
import com.noxisculture.block.custom.NoxisBowlBlock;
import com.noxisculture.block.custom.NoxisBowlClaims;
import com.noxisculture.block.custom.NoxisHatBlock;
import com.noxisculture.entity.idle.NoxisBowlHop;
import com.noxisculture.entity.idle.NoxisHatAnimation;
import com.noxisculture.sound.ModSounds;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Dormir de noche en una pecera (servidor). Cuando le da sueño (cada Noxis a su hora), el Noxis
 * busca una pecera LIBRE y destapada cerca, la reserva, camina hasta pararse pegado a ella, se
 * saca el sombrero y lo deja apoyado al lado (como bloque), se mete y duerme hasta la mañana con
 * el descanso de siempre (ojitos de sueño y globito). Al despertarse sale, y si su sombrero
 * sigue ahí se lo vuelve a poner; si alguien se lo llevó, sale a buscar uno.
 *
 * <p><b>Una pecera, un Noxis.</b> La reserva la lleva {@link NoxisBowlClaims} en el servidor:
 * la consigue el primero que la pide y se vuelve a comprobar justo antes de saltar adentro.
 * Si por cualquier motivo (una carga del mundo, por ejemplo) un Noxis descubre que su pecera es
 * de otro, sale saltando (sin atravesar el vidrio) y la deja en paz.</p>
 *
 * <p><b>Entrar y salir</b> es un saltito de verdad por la abertura de arriba ({@link NoxisBowlHop}):
 * se para al lado, mira la abertura, se agacha, salta por encima del borde y cae adentro
 * (al salir, lo mismo al revés). Una pecera <b>tapada</b> (con otra encima) no deja entrar ni
 * salir: el que duerme adentro espera a que la destapen.</p>
 *
 * <p><b>Acercarse</b> lo hace con {@link NoxisApproach}: si se traba, prueba desde otro lado de
 * la pecera, y si sigue sin poder, se rinde y sigue con su vida (nunca se queda mirando).</p>
 */
public class NoxisBowlSleepGoal<T extends PathfinderMob & NoxisBowlSleeper & NoxisRestful> extends Goal {
    private static final int SEARCH_RADIUS = 16;
    private static final int SEARCH_HEIGHT = 4;
    private static final int SEARCH_INTERVAL = 100;      // busca pecera cada 5 s
    private static final int GIVE_UP_COOLDOWN = 400;     // si no pudo llegar, espera 20 s antes de volver a buscar
    private static final int WALK_TIMEOUT = 600;         // 30 s para llegar
    private static final double REACH = 1.55D;           // distancia al centro de la pecera para saltar
    private static final double FLOOR = 2.0D / 16.0D;    // el piso de madera de la pecera
    /** Arriba del borde (salida de emergencia, si no hay ningún lugar al costado). */
    private static final double LIP_TOP = 21.0D / 16.0D;
    /** Si al despertarse no puede salir (tapada o sin lugar al costado), sigue adentro y reintenta. */
    private static final int EXIT_RETRY = 100;

    private enum Phase { WALK, HAT_OFF, PREP, CLIMB_IN, SLEEP, CLIMB_OUT, HAT_ON, DONE }

    private final T mob;
    private final NoxisApproach approacher;
    private Phase phase = Phase.DONE;
    private int ticks;
    private int nextSearchTick;
    /** Dónde va a dejar el sombrero (mientras se lo saca). */
    private @Nullable BlockPos hatSpot;
    /** Saltito para entrar o salir: desde dónde y hasta dónde. */
    private @Nullable Vec3 hopFrom;
    private @Nullable Vec3 hopTo;
    /** Lugar pegado a la pecera desde donde salta adentro (y los que ya fallaron). */
    private @Nullable BlockPos approach;
    private final Set<BlockPos> failedSpots = new HashSet<>();
    private int nextExitTry;
    /** La pecera resultó ser de otro: sale sin tocar la reserva ajena. */
    private boolean evicted;

    public NoxisBowlSleepGoal(T mob) {
        this.mob = mob;
        this.approacher = new NoxisApproach(mob);
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    // ------------------------------------------------------------------ empezar

    @Override
    public boolean canUse() {
        if (!(this.mob.level() instanceof ServerLevel level)) return false;
        BlockPos saved = this.mob.getBowlPos();
        if (saved != null) {
            // Se cargó el mundo con este Noxis durmiendo (o en camino): retoma su reserva.
            this.evicted = !NoxisBowlClaims.reclaim(level, saved, this.mob);
            if (this.evicted && !this.mob.isInBowl()) {
                this.mob.setBowlPos(null);                    // iba en camino a una pecera ajena: la olvida
                return false;
            }
            return true;
        }
        if (!this.mob.isBedtime() || !this.mob.canGoToBowl() || !this.mob.onGround()) return false;
        if (this.mob.tickCount < this.nextSearchTick) return false;
        this.nextSearchTick = this.mob.tickCount + SEARCH_INTERVAL + this.mob.getRandom().nextInt(40);
        BlockPos bowl = this.findFreeBowl(level);
        if (bowl == null) return false;                         // no hay pecera libre: sigue despierto
        // Reserva segura: si otro la pidió antes (aunque sea en este mismo tick), no la consigue.
        if (!NoxisBowlClaims.tryClaim(level, bowl, this.mob)) {
            this.nextSearchTick = this.mob.tickCount + 20;      // busca otra enseguida
            return false;
        }
        this.mob.setBowlPos(bowl);
        this.evicted = false;
        return true;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.hatSpot = null;
        this.hopFrom = null;
        this.hopTo = null;
        this.approach = null;
        this.failedSpots.clear();
        this.approacher.reset();
        this.nextExitTry = 0;
        if (this.mob.isInBowl()) {
            this.phase = Phase.SLEEP;
            if (!this.evicted) this.mob.setResting(true);
        } else {
            this.phase = Phase.WALK;
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

    // ------------------------------------------------------------------ cada tick

    @Override
    public void tick() {
        this.ticks++;
        if (!(this.mob.level() instanceof ServerLevel level)) return;
        if (this.phase == Phase.HAT_ON) {
            this.tickHatOn();
            return;
        }
        if (this.phase == Phase.CLIMB_OUT) {
            if (this.tickHop(NoxisBowlHop.OUT)) this.landOutside();
            return;
        }
        BlockPos bowl = this.mob.getBowlPos();
        if (bowl == null || !level.getBlockState(bowl).is(ModBlocks.NOXIS_BOWL)) {
            // La pecera ya no está (la rompieron): se despierta / deja de ir.
            this.mob.setBowlHop(NoxisBowlHop.NONE);
            this.leaveBowl();
            this.afterLeaving();
            return;
        }
        BlockState state = level.getBlockState(bowl);
        boolean covered = state.getValue(NoxisBowlBlock.COVERED);
        // ¿Sigue siendo suya? (si no, la deja: una pecera, un Noxis)
        if (!this.evicted && !NoxisBowlClaims.isOwner(level, bowl, this.mob)) {
            this.evicted = true;
            if (!this.mob.isInBowl() && this.phase != Phase.CLIMB_IN) {
                this.mob.setBowlPos(null);
                this.phase = Phase.DONE;
                return;
            }
        }
        switch (this.phase) {
            case WALK -> {
                if (!this.mob.isBedtime() || !this.mob.canGoToBowl() || covered || this.evicted) {
                    this.giveUp(level, bowl, false);
                    return;
                }
                this.tickWalk(level, bowl);
            }
            case HAT_OFF -> {
                if (covered) { this.giveUp(level, bowl, false); return; }
                this.tickHatOff(level, bowl);
            }
            case PREP -> {
                // Se agacha mirando la abertura, preparándose para saltar.
                this.mob.getNavigation().stop();
                this.faceBowl(bowl);
                if (this.ticks >= NoxisBowlHop.PREP_TICKS) {
                    if (this.canEnterNow(level, bowl)) {
                        this.startClimbIn(bowl);
                    } else {
                        this.giveUp(level, bowl, true);      // ya no está libre: busca otra enseguida
                    }
                }
            }
            case CLIMB_IN -> {
                if (covered) { this.phase = Phase.DONE; return; }   // la taparon en pleno salto: vuelve afuera
                if (this.tickHop(NoxisBowlHop.IN)) {
                    // Aterrizó apretadito en el fondo: se acomoda y se duerme.
                    this.mob.setBowlHop(NoxisBowlHop.NONE);
                    this.mob.setInBowl(true);
                    this.mob.setResting(true);
                    this.mob.playSound(ModSounds.NOXIS_YAWN, 0.6F, 1.0F);
                    this.ticks = 0;
                    this.phase = Phase.SLEEP;
                }
            }
            case SLEEP -> {
                this.mob.getNavigation().stop();
                this.holdInside(bowl);
                boolean wantsOut = this.evicted || this.mob.isWakeTime() || this.mob.hurtTime > 0;
                if (wantsOut && !covered && this.mob.tickCount >= this.nextExitTry) {
                    this.startClimbOut(bowl);
                }
            }
            default -> { }
        }
    }

    /** Caminar hasta pararse pegado a la pecera (sin trabarse). */
    private void tickWalk(ServerLevel level, BlockPos bowl) {
        if (this.ticks >= WALK_TIMEOUT) { this.giveUp(level, bowl, false); return; }
        if (this.approach == null || (this.ticks % 40 == 1 && !this.isStandable(this.approach))) {
            this.approach = this.findApproachSpot(bowl);
            this.approacher.newTarget();
        }
        BlockPos spot = this.approach;
        Vec3 center = Vec3.atBottomCenterOf(bowl);
        Vec3 target = spot != null ? Vec3.atBottomCenterOf(spot) : center;
        double dx = center.x - this.mob.getX();
        double dz = center.z - this.mob.getZ();
        double standY = spot != null ? spot.getY() : bowl.getY();
        boolean arrived = dx * dx + dz * dz <= REACH * REACH && this.mob.onGround()
                && Math.abs(this.mob.getY() - standY) < 0.6D;
        this.mob.getLookControl().setLookAt(center.x, center.y + 0.8D, center.z);
        switch (this.approacher.tick(target, spot, 0, arrived, 0.45D)) {
            case ARRIVED -> {
                // Llegó al lado de la pecera: mira hacia la abertura.
                this.faceBowl(bowl);
                if (this.mob.hasHat()) {
                    // Primero se saca el sombrero (con su animación) y lo deja al costado.
                    this.hatSpot = this.findHatSpot(bowl);
                    if (this.hatSpot == null) { this.giveUp(level, bowl, false); return; }   // no hay dónde dejarlo
                    this.ticks = 0;
                    this.phase = Phase.HAT_OFF;
                    this.mob.setHatAnim(NoxisHatAnimation.OFF);
                } else {
                    this.startPrep();
                }
            }
            case REPLAN -> {
                // Desde acá no llega: prueba por otro lado de la pecera.
                if (spot != null) this.failedSpots.add(spot);
                this.approach = this.findApproachSpot(bowl);
                if (this.approach == null) this.giveUp(level, bowl, false);
            }
            case FAILED -> this.giveUp(level, bowl, false);
            default -> { }
        }
    }

    private void tickHatOff(ServerLevel level, BlockPos bowl) {
        this.mob.getNavigation().stop();
        BlockPos spot = this.hatSpot;
        if (spot != null) this.mob.getLookControl().setLookAt(spot.getX() + 0.5D, spot.getY() + 0.2D, spot.getZ() + 0.5D);
        if (this.ticks == NoxisHatAnimation.OFF_PLACE_TICK) {
            // Lo apoya en el piso: aparece el bloque y deja de tenerlo puesto.
            if (spot == null || !this.canPlaceHat(spot)) spot = this.findHatSpot(bowl);
            if (spot == null) {
                this.mob.setHatAnim(NoxisHatAnimation.NONE);   // no hay dónde: se lo deja puesto
                this.giveUp(level, bowl, false);
                return;
            }
            // Apoya ESE sombrero (con sus colores) y deja de tenerlo puesto: nunca hay dos.
            NoxisHatBlock.placeHat(level, spot, this.mob.getHatItem());
            level.playSound(null, spot, ModBlocks.NOXIS_HAT.defaultBlockState().getSoundType().getPlaceSound(),
                    SoundSource.NEUTRAL, 0.6F, 1.1F);
            this.mob.setHatItem(net.minecraft.world.item.ItemStack.EMPTY);
            this.mob.setHatPos(spot);
        } else if (this.ticks >= NoxisHatAnimation.OFF_LENGTH) {
            this.mob.setHatAnim(NoxisHatAnimation.NONE);
            this.startPrep();
        }
    }

    /** Se rinde con esta pecera: la libera y sigue con su vida (o busca otra enseguida). */
    private void giveUp(ServerLevel level, BlockPos bowl, boolean searchAgainSoon) {
        this.mob.getNavigation().stop();
        if (!this.evicted) NoxisBowlClaims.release(level, bowl, this.mob);
        this.mob.setBowlPos(null);
        this.nextSearchTick = this.mob.tickCount + (searchAgainSoon ? 20 : GIVE_UP_COOLDOWN);
        this.phase = Phase.DONE;
    }

    /**
     * Última comprobación justo antes de saltar: sigue siendo suya, está destapada y no hay
     * nadie adentro.
     */
    private boolean canEnterNow(ServerLevel level, BlockPos bowl) {
        if (this.evicted || !NoxisBowlClaims.isOwner(level, bowl, this.mob)) return false;
        BlockState state = level.getBlockState(bowl);
        if (!state.is(ModBlocks.NOXIS_BOWL) || state.getValue(NoxisBowlBlock.COVERED)) return false;
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(bowl), e -> e != this.mob && e.isAlive()).isEmpty();
    }

    // ------------------------------------------------------------------ saltitos

    private void startPrep() {
        this.ticks = 0;
        this.phase = Phase.PREP;
        this.mob.setBowlHop(NoxisBowlHop.PREP);
    }

    private void startClimbIn(BlockPos bowl) {
        this.mob.getNavigation().stop();
        this.startHop(this.mob.position(), new Vec3(bowl.getX() + 0.5D, bowl.getY() + FLOOR, bowl.getZ() + 0.5D));
        this.mob.setBowlHop(NoxisBowlHop.IN);
        this.phase = Phase.CLIMB_IN;
    }

    /**
     * Sale saltando por la abertura, hacia el lado donde dejó el sombrero. Si no hay ningún lugar
     * libre al costado para bajar, se queda adentro y vuelve a intentar.
     */
    private void startClimbOut(BlockPos bowl) {
        BlockPos out = this.findExitSpot(bowl, this.mob.getHatPos());
        if (out == null) {
            this.nextExitTry = this.mob.tickCount + EXIT_RETRY;
            return;
        }
        this.mob.setResting(false);                              // se despereza mientras sale
        this.mob.setInBowl(false);
        this.startHop(this.mob.position(), new Vec3(out.getX() + 0.5D, out.getY(), out.getZ() + 0.5D));
        this.mob.setBowlHop(NoxisBowlHop.OUT);
        this.phase = Phase.CLIMB_OUT;
    }

    private void startHop(Vec3 from, Vec3 to) {
        this.hopFrom = from;
        this.hopTo = to;
        this.ticks = 0;
        this.mob.getNavigation().stop();
        this.mob.setDeltaMovement(Vec3.ZERO);
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        if (dx * dx + dz * dz > 1.0E-4D) this.face(dx, dz);
    }

    private void faceBowl(BlockPos bowl) {
        double dx = bowl.getX() + 0.5D - this.mob.getX();
        double dz = bowl.getZ() + 0.5D - this.mob.getZ();
        if (dx * dx + dz * dz > 1.0E-4D) this.face(dx, dz);
        this.mob.getLookControl().setLookAt(bowl.getX() + 0.5D, bowl.getY() + 1.2D, bowl.getZ() + 0.5D);
    }

    private void face(double dx, double dz) {
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        this.mob.setYRot(yaw);
        this.mob.yBodyRot = yaw;
        this.mob.yHeadRot = yaw;
    }

    /**
     * Mueve el saltito un tick por su parábola (ver {@link NoxisBowlHop}): avanza por encima de
     * la abertura solo cuando los pies ya pasaron el borde, así nunca cruza el vidrio.
     * @return true cuando aterrizó
     */
    private boolean tickHop(byte type) {
        Vec3 from = this.hopFrom;
        Vec3 to = this.hopTo;
        BlockPos bowl = this.mob.getBowlPos();
        if (from == null || to == null) return true;
        double apex = (bowl != null ? bowl.getY() : Math.max(from.y, to.y)) + NoxisBowlHop.APEX;
        apex = Math.max(apex, Math.max(from.y, to.y) + 0.3D);
        float f = Math.min(1.0F, this.ticks / (float) NoxisBowlHop.HOP_TICKS);
        float h = NoxisBowlHop.horizontal(type, f);
        double y = NoxisBowlHop.height(type, f, from.y, apex, to.y);
        this.mob.setPos(Mth.lerp(h, from.x, to.x), y, Mth.lerp(h, from.z, to.z));
        this.mob.setDeltaMovement(Vec3.ZERO);
        this.mob.getNavigation().stop();
        return this.ticks >= NoxisBowlHop.HOP_TICKS;
    }

    /** Aterrizó afuera: libera la pecera y va por su sombrero (si sigue ahí). */
    private void landOutside() {
        this.mob.setBowlHop(NoxisBowlHop.NONE);
        this.freeBowl();
        this.afterLeaving();
    }

    /** Después de salir: si su sombrero sigue al lado, va a ponérselo con su animación. */
    private void afterLeaving() {
        BlockPos hat = this.mob.getHatPos();
        if (!this.mob.hasHat() && hat != null && this.mob.level().getBlockState(hat).is(ModBlocks.NOXIS_HAT)
                && hat.distToCenterSqr(this.mob.position()) < 16.0D) {
            this.ticks = 0;
            this.phase = Phase.HAT_ON;
            this.mob.setHatAnim(NoxisHatAnimation.ON);
        } else {
            // Alguien se lo llevó: sale a buscar uno por la zona (NoxisFindHatGoal), un rato.
            this.mob.setHatPos(null);
            if (!this.mob.hasHat()) this.mob.startHatSearch(this.mob.blockPosition());
            this.phase = Phase.DONE;
        }
    }

    /** Quieto en el centro de la pecera mientras duerme. */
    private void holdInside(BlockPos bowl) {
        double x = bowl.getX() + 0.5D;
        double y = bowl.getY() + FLOOR;
        double z = bowl.getZ() + 0.5D;
        if (this.mob.distanceToSqr(x, y, z) > 0.02D) {
            this.mob.teleportTo(x, y, z);
        }
        this.mob.setDeltaMovement(Vec3.ZERO);
    }

    /** Ponerse el sombrero que dejó al lado de la pecera. */
    private void tickHatOn() {
        this.mob.getNavigation().stop();
        Level level = this.mob.level();
        BlockPos hat = this.mob.getHatPos();
        if (hat != null) this.mob.getLookControl().setLookAt(hat.getX() + 0.5D, hat.getY() + 0.2D, hat.getZ() + 0.5D);
        if (this.ticks == NoxisHatAnimation.ON_TAKE_TICK) {
            if (hat != null && level.getBlockState(hat).is(ModBlocks.NOXIS_HAT)) {
                // Lo levanta: el bloque desaparece y lo tiene en las manitos (nunca hay dos).
                // Lo levanta: el bloque desaparece y se pone ESE mismo sombrero (nunca hay dos).
                this.mob.setHatItem(NoxisHatBlock.takeHat(level, hat));
                level.playSound(null, hat, ModBlocks.NOXIS_HAT.defaultBlockState().getSoundType().getBreakSound(),
                        SoundSource.NEUTRAL, 0.5F, 1.2F);
            } else {
                this.mob.setHatAnim(NoxisHatAnimation.NONE);  // justo se lo llevaron
                this.mob.setHatPos(null);
                this.mob.startHatSearch(this.mob.blockPosition());
                this.phase = Phase.DONE;
                return;
            }
        }
        if (this.ticks >= NoxisHatAnimation.ON_LENGTH) {
            this.mob.setHatAnim(NoxisHatAnimation.NONE);
            this.mob.setHatPos(null);
            this.phase = Phase.DONE;
        }
    }

    /**
     * Salida de emergencia (lo interrumpieron o rompieron la pecera): si la pecera sigue ahí y
     * está adentro, aparece al lado; si ya no está, queda donde estaba. Libera todo.
     */
    private void leaveBowl() {
        Level level = this.mob.level();
        BlockPos bowl = this.mob.getBowlPos();
        if (this.mob.isInBowl() && bowl != null && level.getBlockState(bowl).is(ModBlocks.NOXIS_BOWL)) {
            BlockPos out = this.findExitSpot(bowl, this.mob.getHatPos());
            if (out != null) {
                this.mob.teleportTo(out.getX() + 0.5D, out.getY(), out.getZ() + 0.5D);
            } else {
                this.mob.teleportTo(bowl.getX() + 0.5D, bowl.getY() + LIP_TOP, bowl.getZ() + 0.5D);
            }
        }
        this.freeBowl();
    }

    /** Libera la pecera (solo si es suya) y limpia sus datos. */
    private void freeBowl() {
        BlockPos bowl = this.mob.getBowlPos();
        if (bowl != null && !this.evicted && this.mob.level() instanceof ServerLevel level) {
            NoxisBowlClaims.release(level, bowl, this.mob);
        }
        boolean wasSleeping = this.mob.isInBowl();
        this.mob.setInBowl(false);
        this.mob.setBowlPos(null);
        if (wasSleeping) {
            this.mob.setResting(false);
        }
        this.evicted = false;
    }

    // ------------------------------------------------------------------ búsquedas

    /** La pecera libre más cercana (destapada, sin reserva y sin nadie adentro) a la que pueda llegar. */
    private @Nullable BlockPos findFreeBowl(ServerLevel level) {
        BlockPos origin = this.mob.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-SEARCH_RADIUS, -SEARCH_HEIGHT, -SEARCH_RADIUS),
                origin.offset(SEARCH_RADIUS, SEARCH_HEIGHT, SEARCH_RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlocks.NOXIS_BOWL) || state.getValue(NoxisBowlBlock.OCCUPIED)
                    || state.getValue(NoxisBowlBlock.COVERED)) continue;
            double d = pos.distToCenterSqr(this.mob.position());
            if (d >= bestDist) continue;
            if (NoxisBowlClaims.owner(level, pos) != null) continue;
            if (!level.getEntitiesOfClass(LivingEntity.class, new AABB(pos), e -> e != this.mob).isEmpty()) continue;
            if (this.findApproachSpotFrom(pos, level) == null) continue;    // no hay dónde pararse al lado
            best = pos.immutable();
            bestDist = d;
        }
        return best;
    }

    private @Nullable BlockPos findApproachSpotFrom(BlockPos bowl, Level level) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (int dy = 0; dy >= -1; dy--) {
                BlockPos p = bowl.relative(dir).above(dy);
                if (this.isStandable(p)) return p;
            }
        }
        return null;
    }

    /** El lugar pegado a la pecera más cercano al Noxis (sin otro parado ahí y que no haya fallado). */
    private @Nullable BlockPos findApproachSpot(BlockPos bowl) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (int dy = 0; dy >= -1; dy--) {
                BlockPos p = bowl.relative(dir).above(dy);
                if (!this.isStandable(p)) continue;
                if (!this.failedSpots.contains(p)
                        && this.mob.level().getEntities(this.mob, new AABB(p)).isEmpty()) {
                    double d = p.distToCenterSqr(this.mob.position());
                    if (d < bestDist) { best = p.immutable(); bestDist = d; }
                }
                break;
            }
        }
        return best;
    }

    /** Dónde dejar el sombrero: al lado de la pecera, sin nadie parado ahí (pasto y flores sirven). */
    private @Nullable BlockPos findHatSpot(BlockPos bowl) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (int dy = 0; dy >= -1; dy--) {
                BlockPos p = bowl.relative(dir).above(dy);
                if (!this.canPlaceHat(p)) continue;
                if (this.mob.level().getEntities((Entity) null, new AABB(p)).isEmpty()) {
                    double d = p.distToCenterSqr(this.mob.position());
                    if (d < bestDist) { best = p.immutable(); bestDist = d; }
                }
                break;
            }
        }
        return best;
    }

    /**
     * Dónde bajar al salir: un lugar al lado de la pecera donde se pueda parar y sin nadie,
     * el más cercano a {@code prefer} (su sombrero) o al Noxis.
     */
    private @Nullable BlockPos findExitSpot(BlockPos bowl, @Nullable BlockPos prefer) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (int dy = 0; dy >= -1; dy--) {
                BlockPos p = bowl.relative(dir).above(dy);
                if (!this.isStandable(p)) continue;
                if (this.mob.level().getEntities(this.mob, new AABB(p)).isEmpty()) {
                    double d = prefer != null ? p.distSqr(prefer) : p.distToCenterSqr(this.mob.position());
                    if (d < bestDist) { best = p.immutable(); bestDist = d; }
                }
                break;
            }
        }
        return best;
    }

    /** ¿Se puede parar ahí? (pasto, flores y nieve finita no molestan; agua y lava sí). */
    private boolean isStandable(BlockPos p) {
        Level level = this.mob.level();
        BlockState at = level.getBlockState(p);
        BlockState above = level.getBlockState(p.above());
        return at.getCollisionShape(level, p).isEmpty() && at.getFluidState().isEmpty()
                && above.getCollisionShape(level, p.above()).isEmpty()
                && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP);
    }

    /** ¿Se puede apoyar el sombrero ahí? (reemplaza pasto o flores, como cualquier bloque). */
    private boolean canPlaceHat(BlockPos p) {
        Level level = this.mob.level();
        BlockState at = level.getBlockState(p);
        return (at.isAir() || at.canBeReplaced()) && at.getFluidState().isEmpty()
                && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP);
    }

    @Override
    public void stop() {
        // Termina o lo interrumpen: nada queda a medias.
        Vec3 from = this.hopFrom;
        Vec3 to = this.hopTo;
        if (this.phase == Phase.CLIMB_IN && from != null) {
            this.mob.setPos(from.x, from.y, from.z);              // no llegó a entrar: vuelve afuera
        } else if (this.phase == Phase.CLIMB_OUT && to != null) {
            this.mob.setPos(to.x, to.y, to.z);                    // termina de salir
        }
        this.mob.setBowlHop(NoxisBowlHop.NONE);
        this.hopFrom = null;
        this.hopTo = null;
        if (this.mob.isInBowl() || this.mob.getBowlPos() != null) this.leaveBowl();
        Level level = this.mob.level();
        BlockPos hat = this.mob.getHatPos();
        if (!this.mob.hasHat() && hat != null && !this.mob.isInBowl()) {
            // Cortado mientras iba a ponérselo: si sigue ahí, se lo pone igual (nunca se duplica).
            if (level.getBlockState(hat).is(ModBlocks.NOXIS_HAT)) {
                this.mob.setHatItem(NoxisHatBlock.takeHat(level, hat));
            }
            this.mob.setHatPos(null);
        }
        this.mob.setHatAnim(NoxisHatAnimation.NONE);
        this.hatSpot = null;
        this.approach = null;
        this.evicted = false;
        this.phase = Phase.DONE;
    }
}
