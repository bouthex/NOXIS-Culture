package com.noxisculture.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Acercarse a algo para usarlo (servidor), sin quedarse trabado mirándolo.
 *
 * <p>De lejos usa la navegación normal (camino calculado, que se recalcula cada segundo). De
 * cerca (los últimos pasitos, donde la navegación suele darse por "llegada" antes de tiempo)
 * camina derecho hacia el objetivo. Además vigila que avance de verdad: si en 3 s no se acercó,
 * cuenta un intento fallido y pide otra posición ({@link Result#REPLAN}); después de varios
 * intentos fallidos se rinde ({@link Result#FAILED}) y el que lo usa cancela la acción.</p>
 */
public final class NoxisApproach {
    public enum Result { MOVING, ARRIVED, REPLAN, FAILED }

    private static final double DIRECT_RANGE = 2.5D;     // de acá en adelante camina derecho
    private static final int REPATH_EVERY = 20;
    private static final int CHECK_EVERY = 10;
    private static final int STALL_LIMIT = 60;           // 3 s sin acercarse = intento fallido
    private static final int MAX_STRIKES = 3;

    private final PathfinderMob mob;
    private double bestDist = Double.MAX_VALUE;
    private int stall;
    private int strikes;
    private int repathIn;
    private int clock;

    public NoxisApproach(PathfinderMob mob) {
        this.mob = mob;
    }

    public void reset() {
        this.bestDist = Double.MAX_VALUE;
        this.stall = 0;
        this.strikes = 0;
        this.repathIn = 0;
        this.clock = 0;
    }

    /** Nuevo objetivo (otra posición): conserva los intentos fallidos acumulados. */
    public void newTarget() {
        this.bestDist = Double.MAX_VALUE;
        this.stall = 0;
        this.repathIn = 0;
    }

    /**
     * Un tick de acercamiento.
     *
     * @param target     adónde quiere llegar
     * @param pathTarget bloque para calcular el camino (o null: el de {@code target})
     * @param accuracy   cuán cerca del bloque tiene que terminar el camino
     * @param arrived    si ya está lo bastante cerca para hacer la acción
     */
    public Result tick(Vec3 target, @Nullable BlockPos pathTarget, int accuracy, boolean arrived, double speed) {
        if (arrived) {
            this.mob.getNavigation().stop();
            return Result.ARRIVED;
        }
        this.clock++;
        double dx = target.x - this.mob.getX();
        double dz = target.z - this.mob.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        // ¿Avanza de verdad?
        if (this.clock % CHECK_EVERY == 0) {
            if (dist < this.bestDist - 0.15D) {
                this.bestDist = dist;
                this.stall = 0;
            } else {
                this.stall += CHECK_EVERY;
            }
            if (this.stall >= STALL_LIMIT) {
                this.stall = 0;
                this.bestDist = Double.MAX_VALUE;
                this.mob.getNavigation().stop();
                return ++this.strikes >= MAX_STRIKES ? Result.FAILED : Result.REPLAN;
            }
        }

        if (dist < DIRECT_RANGE && Math.abs(target.y - this.mob.getY()) < 1.2D) {
            // Últimos pasitos: derecho al objetivo (la navegación a veces frena antes).
            this.mob.getNavigation().stop();
            this.mob.getMoveControl().setWantedPosition(target.x, target.y, target.z, speed);
            return Result.MOVING;
        }
        if (--this.repathIn <= 0 || this.mob.getNavigation().isDone()) {
            this.repathIn = REPATH_EVERY;
            BlockPos goal = pathTarget != null ? pathTarget : BlockPos.containing(target);
            Path path = this.mob.getNavigation().createPath(goal, accuracy);
            if (path == null) {
                this.mob.getNavigation().stop();
                this.stall += REPATH_EVERY;                 // sin camino: cuenta como no avanzar
            } else {
                this.mob.getNavigation().moveTo(path, speed);
            }
        }
        return Result.MOVING;
    }
}
