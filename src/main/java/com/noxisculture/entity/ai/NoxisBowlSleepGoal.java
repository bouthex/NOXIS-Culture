package com.noxisculture.entity.ai;

import com.noxisculture.block.ModBlocks;
import com.noxisculture.block.custom.NoxisBowlBlock;
import com.noxisculture.sound.ModSounds;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

    private enum Phase { WALK, SLEEP, DONE }

    private final T mob;
    private Phase phase = Phase.DONE;
    private int ticks;
    private int nextSearchTick;

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
        BlockPos bowl = this.mob.getBowlPos();
        Level level = this.mob.level();
        if (bowl == null || !level.getBlockState(bowl).is(ModBlocks.NOXIS_BOWL)) {
            // La pecera ya no está (la rompieron): se despierta / deja de ir.
            this.phase = Phase.DONE;
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
                    if (this.climbIn(bowl)) {
                        this.ticks = 0;
                        this.phase = Phase.SLEEP;
                    } else {
                        this.phase = Phase.DONE;                 // no había dónde dejar el sombrero
                    }
                } else if (this.ticks >= WALK_TIMEOUT) {
                    this.phase = Phase.DONE;                     // no pudo llegar
                } else if (this.ticks % 20 == 1 || this.mob.getNavigation().isDone()) {
                    this.mob.getNavigation().moveTo(center.x, bowl.getY(), center.z, 0.45D);
                }
            }
            case SLEEP -> {
                this.mob.getNavigation().stop();
                this.holdInside(bowl);
                if (this.mob.isWakeTime() || this.mob.hurtTime > 0) {
                    this.phase = Phase.DONE;                     // stop() lo despierta y lo saca
                }
            }
            case DONE -> { }
        }
    }

    /** Se saca el sombrero, lo deja al lado y se mete en la pecera. */
    private boolean climbIn(BlockPos bowl) {
        Level level = this.mob.level();
        if (this.mob.hasHat()) {
            BlockPos spot = this.findFreeSpotNear(bowl);
            if (spot == null) return false;
            level.setBlock(spot, ModBlocks.NOXIS_HAT.defaultBlockState(), Block.UPDATE_ALL);
            level.playSound(null, spot, ModBlocks.NOXIS_HAT.defaultBlockState().getSoundType().getPlaceSound(),
                    net.minecraft.sounds.SoundSource.NEUTRAL, 0.6F, 1.1F);
            this.mob.setHasHat(false);
            this.mob.setHatPos(spot);
        }
        this.mob.getNavigation().stop();
        this.mob.teleportTo(bowl.getX() + 0.5D, bowl.getY() + FLOOR, bowl.getZ() + 0.5D);
        this.mob.setInBowl(true);
        this.mob.setResting(true);
        this.mob.playSound(ModSounds.NOXIS_YAWN, 0.6F, 1.0F);
        return true;
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

    /** Sale de la pecera, recupera el sombrero si sigue ahí y libera la pecera. */
    private void wakeUp() {
        Level level = this.mob.level();
        BlockPos bowl = this.mob.getBowlPos();
        if (this.mob.isInBowl() && bowl != null) {
            BlockPos hat = this.mob.getHatPos();
            BlockPos out = hat != null && this.isStandable(hat) ? hat : this.findFreeSpotNear(bowl);
            if (out != null) {
                this.mob.teleportTo(out.getX() + 0.5D, out.getY(), out.getZ() + 0.5D);
            } else {
                this.mob.teleportTo(bowl.getX() + 0.5D, bowl.getY() + 1.4D, bowl.getZ() + 0.5D);   // sale por arriba
            }
        }
        // ¿Sigue su sombrero donde lo dejó? Se lo pone (el bloque desaparece: nunca hay dos).
        BlockPos hat = this.mob.getHatPos();
        if (hat != null && !this.mob.hasHat() && level.getBlockState(hat).is(ModBlocks.NOXIS_HAT)) {
            level.removeBlock(hat, false);
            this.mob.setHasHat(true);
        }
        if (this.mob.isInBowl() || hat != null) this.mob.setHatPos(null);
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

    /** Un lugar libre al lado de la pecera, con piso firme (para el sombrero o para salir). */
    private @Nullable BlockPos findFreeSpotNear(BlockPos bowl) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (int dy = 0; dy >= -1; dy--) {
                BlockPos p = bowl.relative(dir).above(dy);
                if (!this.isStandable(p)) continue;
                double d = p.distToCenterSqr(this.mob.position());
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
        // Termina (se hizo de día, lo despertaron o no pudo llegar): sale y libera todo.
        this.wakeUp();
        this.phase = Phase.DONE;
    }
}
