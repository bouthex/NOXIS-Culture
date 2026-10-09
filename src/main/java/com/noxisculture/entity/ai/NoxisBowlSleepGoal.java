package com.noxisculture.entity.ai;

import com.noxisculture.block.ModBlocks;
import com.noxisculture.block.custom.NoxisBowlBlock;
import com.noxisculture.entity.idle.NoxisHatAnimation;
import com.noxisculture.sound.ModSounds;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
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
 * busca una pecera LIBRE cerca, la reserva, camina hasta ella, se saca el sombrero y lo deja
 * apoyado al lado (como bloque), se mete adentro y duerme hasta la mañana con el descanso de
 * siempre (ojitos de sueño y globito). Al despertarse sale, y si su sombrero sigue ahí se lo
 * vuelve a poner; si alguien se lo llevó, se queda sin sombrero.
 *
 * <p>Entra y sale SALTANDO por la abertura de arriba (nunca atraviesa el vidrio ni se
 * teletransporta): un saltito en arco que pasa por encima del borde. Mientras salta, el Noxis
 * no choca con nada (lo mueve este objetivo, tick a tick).</p>
 *
 * <p>Una pecera, un Noxis: la reserva se marca en el bloque apenas la elige. Si no encuentra
 * pecera libre, sigue despierto con lo suyo. Todo se guarda con el mundo (la pecera, el estado
 * del Noxis y la posición del sombrero), así nada se duplica ni se pierde.</p>
 */
public class NoxisBowlSleepGoal<T extends PathfinderMob & NoxisBowlSleeper & NoxisRestful> extends Goal {
    private static final int SEARCH_RADIUS = 16;
    private static final int SEARCH_HEIGHT = 4;
    private static final int SEARCH_INTERVAL = 100;      // busca pecera cada 5 s
    private static final int WALK_TIMEOUT = 600;         // 30 s para llegar
    private static final double REACH = 1.7D;
    private static final double FLOOR = 2.0D / 16.0D;    // el piso de madera de la pecera
    /** Punto más alto del saltito: por encima del borde de vidrio (21 px). */
    private static final double APEX = 23.5D / 16.0D;
    /** Arriba del borde (si no hay lugar al costado para bajar). */
    private static final double LIP_TOP = 21.0D / 16.0D;
    private static final int HOP_TICKS = 14;

    private enum Phase { WALK, HAT_OFF, CLIMB_IN, SLEEP, CLIMB_OUT, HAT_ON, DONE }

    private final T mob;
    private Phase phase = Phase.DONE;
    private int ticks;
    private int nextSearchTick;
    /** Dónde va a dejar el sombrero (mientras se lo saca). */
    private @Nullable BlockPos hatSpot;
    /** Saltito para entrar o salir: desde dónde y hasta dónde. */
    private @Nullable Vec3 hopFrom;
    private @Nullable Vec3 hopTo;

    public NoxisBowlSleepGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    // ------------------------------------------------------------------ empezar

    @Override
    public boolean canUse() {
        // Se cargó el mundo con este Noxis durmiendo (o en camino): sigue donde estaba.
        if (this.mob.getBowlPos() != null) return true;
        if (!this.mob.isBedtime() || !this.mob.canGoToBowl() || !this.mob.onGround()) return false;
        if (this.mob.tickCount < this.nextSearchTick) return false;
        this.nextSearchTick = this.mob.tickCount + SEARCH_INTERVAL + this.mob.getRandom().nextInt(40);
        BlockPos bowl = this.findFreeBowl();
        if (bowl == null) return false;                         // no hay pecera libre: sigue despierto
        // Reserva: desde ya nadie más puede elegirla.
        Level level = this.mob.level();
        level.setBlock(bowl, level.getBlockState(bowl).setValue(NoxisBowlBlock.OCCUPIED, true), Block.UPDATE_ALL);
        this.mob.setBowlPos(bowl);
        return true;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.hatSpot = null;
        this.hopFrom = null;
        this.hopTo = null;
        if (this.mob.isInBowl()) {
            this.phase = Phase.SLEEP;
            this.mob.setResting(true);
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
        Level level = this.mob.level();
        if (this.phase == Phase.HAT_ON) {
            this.tickHatOn();
            return;
        }
        if (this.phase == Phase.CLIMB_OUT) {
            if (this.tickHop(false)) this.landOutside();
            return;
        }
        BlockPos bowl = this.mob.getBowlPos();
        if (bowl == null || !level.getBlockState(bowl).is(ModBlocks.NOXIS_BOWL)) {
            // La pecera ya no está (la rompieron): se despierta / deja de ir.
            this.mob.setBowlHop(false);
            this.leaveBowl();
            this.afterLeaving();
            return;
        }
        switch (this.phase) {
            case WALK -> {
                if (!this.mob.isBedtime() || !this.mob.canGoToBowl()) { this.phase = Phase.DONE; return; }
                Vec3 center = Vec3.atBottomCenterOf(bowl);
                this.mob.getLookControl().setLookAt(center.x, center.y + 0.5D, center.z);
                double dx = center.x - this.mob.getX();
                double dz = center.z - this.mob.getZ();
                if (dx * dx + dz * dz < REACH * REACH && Math.abs(bowl.getY() - this.mob.getY()) < 1.5D) {
                    this.mob.getNavigation().stop();
                    if (this.mob.hasHat()) {
                        // Primero se saca el sombrero (con su animación) y lo deja al costado.
                        this.hatSpot = this.findFreeSpotNear(bowl, null);
                        if (this.hatSpot == null) { this.phase = Phase.DONE; return; }   // no hay dónde dejarlo
                        this.ticks = 0;
                        this.phase = Phase.HAT_OFF;
                        this.mob.setHatAnim(NoxisHatAnimation.OFF);
                    } else {
                        this.startClimbIn(bowl);
                    }
                } else if (this.ticks >= WALK_TIMEOUT) {
                    this.phase = Phase.DONE;                     // no pudo llegar
                } else if (this.ticks % 20 == 1 || this.mob.getNavigation().isDone()) {
                    this.mob.getNavigation().moveTo(center.x, bowl.getY(), center.z, 0.45D);
                }
            }
            case HAT_OFF -> {
                this.mob.getNavigation().stop();
                BlockPos spot = this.hatSpot;
                if (spot != null) this.mob.getLookControl().setLookAt(spot.getX() + 0.5D, spot.getY() + 0.2D, spot.getZ() + 0.5D);
                if (this.ticks == NoxisHatAnimation.OFF_PLACE_TICK) {
                    // Lo apoya en el piso: aparece el bloque y deja de tenerlo puesto.
                    if (spot == null || !this.isStandable(spot)) spot = this.findFreeSpotNear(bowl, null);
                    if (spot == null) {
                        this.mob.setHatAnim(NoxisHatAnimation.NONE);   // no hay dónde: se lo deja puesto
                        this.phase = Phase.DONE;
                        return;
                    }
                    level.setBlock(spot, ModBlocks.NOXIS_HAT.defaultBlockState(), Block.UPDATE_ALL);
                    level.playSound(null, spot, ModBlocks.NOXIS_HAT.defaultBlockState().getSoundType().getPlaceSound(),
                            SoundSource.NEUTRAL, 0.6F, 1.1F);
                    this.mob.setHasHat(false);
                    this.mob.setHatPos(spot);
                } else if (this.ticks >= NoxisHatAnimation.OFF_LENGTH) {
                    this.mob.setHatAnim(NoxisHatAnimation.NONE);
                    this.startClimbIn(bowl);
                }
            }
            case CLIMB_IN -> {
                if (this.tickHop(true)) {
                    // Aterrizó adentro: a dormir.
                    this.mob.setBowlHop(false);
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
                if (this.mob.isWakeTime() || this.mob.hurtTime > 0) {
                    this.startClimbOut(bowl);
                }
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ saltitos

    private void startClimbIn(BlockPos bowl) {
        this.mob.getNavigation().stop();
        this.startHop(this.mob.position(), new Vec3(bowl.getX() + 0.5D, bowl.getY() + FLOOR, bowl.getZ() + 0.5D));
        this.phase = Phase.CLIMB_IN;
    }

    /** Se despierta y sale saltando por arriba, del lado donde dejó el sombrero. */
    private void startClimbOut(BlockPos bowl) {
        BlockPos out = this.findFreeSpotNear(bowl, this.mob.getHatPos());
        Vec3 to = out != null
                ? new Vec3(out.getX() + 0.5D, out.getY(), out.getZ() + 0.5D)
                : new Vec3(bowl.getX() + 0.5D, bowl.getY() + LIP_TOP, bowl.getZ() + 0.5D);   // se queda arriba del borde
        this.mob.setResting(false);                              // se despereza mientras sale
        this.startHop(this.mob.position(), to);
        this.phase = Phase.CLIMB_OUT;
    }

    private void startHop(Vec3 from, Vec3 to) {
        this.hopFrom = from;
        this.hopTo = to;
        this.ticks = 0;
        this.mob.setBowlHop(true);
        this.mob.setDeltaMovement(Vec3.ZERO);
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        if (dx * dx + dz * dz > 1.0E-4D) {
            float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
            this.mob.setYRot(yaw);
            this.mob.yBodyRot = yaw;
            this.mob.yHeadRot = yaw;
        }
    }

    /**
     * Mueve el saltito un tick. Entrando: sube, cruza por encima del borde y cae adentro.
     * Saliendo: sube derecho por la abertura, cruza por encima del borde y baja afuera.
     * @return true cuando aterrizó
     */
    private boolean tickHop(boolean in) {
        Vec3 from = this.hopFrom;
        Vec3 to = this.hopTo;
        BlockPos bowl = this.mob.getBowlPos();
        if (from == null || to == null) return true;
        double apex = (bowl != null ? bowl.getY() : Math.max(from.y, to.y)) + APEX;
        apex = Math.max(apex, Math.max(from.y, to.y) + 0.3D);
        float t = Math.min(1.0F, this.ticks / (float) HOP_TICKS);
        float h;
        double y;
        if (in) {
            h = NoxisHatAnimation.smooth((t - 0.1F) / 0.45F);
            if (t < 0.35F) y = Mth.lerp(easeOut(t / 0.35F), from.y, apex);
            else if (t < 0.6F) y = apex;
            else { float d = (t - 0.6F) / 0.4F; y = Mth.lerp(d * d, apex, to.y); }
        } else {
            h = NoxisHatAnimation.smooth((t - 0.3F) / 0.4F);
            if (t < 0.3F) y = Mth.lerp(easeOut(t / 0.3F), from.y, apex);
            else if (t < 0.65F) y = apex;
            else { float d = (t - 0.65F) / 0.35F; y = Mth.lerp(d * d, apex, to.y); }
        }
        this.mob.setPos(Mth.lerp(h, from.x, to.x), y, Mth.lerp(h, from.z, to.z));
        this.mob.setDeltaMovement(Vec3.ZERO);
        this.mob.getNavigation().stop();
        return this.ticks >= HOP_TICKS;
    }

    private static float easeOut(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return 1.0F - (1.0F - x) * (1.0F - x);
    }

    /** Aterrizó afuera: libera la pecera y va por su sombrero (si sigue ahí). */
    private void landOutside() {
        this.mob.setBowlHop(false);
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
            // Alguien se lo llevó: se queda sin sombrero (después lo puede buscar por ahí).
            this.mob.setHatPos(null);
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
                level.removeBlock(hat, false);
                level.playSound(null, hat, ModBlocks.NOXIS_HAT.defaultBlockState().getSoundType().getBreakSound(),
                        SoundSource.NEUTRAL, 0.5F, 1.2F);
                this.mob.setHasHat(true);
            } else {
                this.mob.setHatAnim(NoxisHatAnimation.NONE);  // justo se lo llevaron
                this.mob.setHatPos(null);
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
     * Salida de emergencia (lo interrumpieron o rompieron la pecera): si la pecera sigue ahí,
     * aparece afuera al lado; si ya no está, simplemente queda donde estaba. Libera todo.
     */
    private void leaveBowl() {
        Level level = this.mob.level();
        BlockPos bowl = this.mob.getBowlPos();
        if (this.mob.isInBowl() && bowl != null && level.getBlockState(bowl).is(ModBlocks.NOXIS_BOWL)) {
            BlockPos out = this.findFreeSpotNear(bowl, this.mob.getHatPos());
            if (out != null) {
                this.mob.teleportTo(out.getX() + 0.5D, out.getY(), out.getZ() + 0.5D);
            } else {
                this.mob.teleportTo(bowl.getX() + 0.5D, bowl.getY() + LIP_TOP, bowl.getZ() + 0.5D);
            }
        }
        this.freeBowl();
    }

    /** Libera la pecera (bloque y datos del Noxis). */
    private void freeBowl() {
        Level level = this.mob.level();
        BlockPos bowl = this.mob.getBowlPos();
        if (bowl != null) {
            BlockState state = level.getBlockState(bowl);
            if (state.is(ModBlocks.NOXIS_BOWL)) {
                level.setBlock(bowl, state.setValue(NoxisBowlBlock.OCCUPIED, false), Block.UPDATE_ALL);
            }
        }
        boolean wasSleeping = this.mob.isInBowl();
        this.mob.setInBowl(false);
        this.mob.setBowlPos(null);
        if (wasSleeping) {
            this.mob.setResting(false);
        }
    }

    // ------------------------------------------------------------------ búsquedas

    /** La pecera libre más cercana (sin reservar y sin nadie adentro) a la que pueda llegar. */
    private @Nullable BlockPos findFreeBowl() {
        Level level = this.mob.level();
        BlockPos origin = this.mob.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-SEARCH_RADIUS, -SEARCH_HEIGHT, -SEARCH_RADIUS),
                origin.offset(SEARCH_RADIUS, SEARCH_HEIGHT, SEARCH_RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlocks.NOXIS_BOWL) || state.getValue(NoxisBowlBlock.OCCUPIED)) continue;
            double d = pos.distToCenterSqr(this.mob.position());
            if (d >= bestDist) continue;
            if (!level.getEntities(this.mob, new AABB(pos)).isEmpty()) continue;      // alguien adentro
            if (this.mob.getNavigation().createPath(pos, 1) == null) continue;
            best = pos.immutable();
            bestDist = d;
        }
        return best;
    }

    /**
     * Un lugar libre al lado de la pecera, con piso firme (para el sombrero o para salir).
     * Elige el más cercano a {@code prefer} (o al Noxis, si es null).
     */
    private @Nullable BlockPos findFreeSpotNear(BlockPos bowl, @Nullable BlockPos prefer) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (int dy = 0; dy >= -1; dy--) {
                BlockPos p = bowl.relative(dir).above(dy);
                if (!this.isStandable(p)) continue;
                // Sin nadie parado ahí (ni él mismo, ni un jugador): el sombrero va a un lugar libre.
                if (!this.mob.level().getEntities((Entity) null, new AABB(p)).isEmpty()) break;
                double d = prefer != null ? p.distSqr(prefer) : p.distToCenterSqr(this.mob.position());
                if (d < bestDist) { best = p.immutable(); bestDist = d; }
                break;
            }
        }
        return best;
    }

    private boolean isStandable(BlockPos p) {
        Level level = this.mob.level();
        return level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()
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
            this.mob.setInBowl(false);
        }
        this.mob.setBowlHop(false);
        this.hopFrom = null;
        this.hopTo = null;
        if (this.mob.isInBowl() || this.mob.getBowlPos() != null) this.leaveBowl();
        Level level = this.mob.level();
        BlockPos hat = this.mob.getHatPos();
        if (!this.mob.hasHat() && hat != null && !this.mob.isInBowl()) {
            // Cortado mientras iba a ponérselo: si sigue ahí, se lo pone igual (nunca se duplica).
            if (level.getBlockState(hat).is(ModBlocks.NOXIS_HAT)) {
                level.removeBlock(hat, false);
                this.mob.setHasHat(true);
            }
            this.mob.setHatPos(null);
        }
        this.mob.setHatAnim(NoxisHatAnimation.NONE);
        this.hatSpot = null;
        this.phase = Phase.DONE;
    }
}
