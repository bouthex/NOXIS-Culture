package com.noxisculture.entity.ai;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Espacio personal (servidor): cuando hay demasiados Noxis parados en el mismo lugar, algunos se
 * corren un poquito a un lugar cercano pero libre, sin empujarse.
 *
 * <ul>
 *   <li>Dos o tres juntos es normal y no pasa nada. Recién con cuatro o más a menos de
 *   {@link #NEAR} bloques se acomodan: los dos de id menor se quedan y el resto se corre (así
 *   nunca se mueven todos a la vez ni se persiguen).</li>
 *   <li>Si dos quedan literalmente encimados, el de id mayor da un pasito al costado.</li>
 *   <li>Solo si siguen amontonados en dos revisiones seguidas (pasar caminando al lado no cuenta) y,
 *   después de acomodarse, espera un rato antes de volver a fijarse.</li>
 *   <li>Prioridad baja y solo si está libre: nunca corta abrazos, amistad, regalos, descanso
 *   compartido, bailes, comercio ni ninguna otra actividad (esas acercan a propósito).</li>
 * </ul>
 */
public class NoxisMakeRoomGoal<T extends PathfinderMob & NoxisRoamer> extends Goal {
    private static final int CHECK_INTERVAL = 20;
    /** Radio del "mismo lugar" (bloques). */
    public static final double NEAR = 1.6D;
    /** Con cuántos vecinos en ese radio ya es un amontonamiento (4 o más en total). */
    private static final int CROWD = 3;
    /** Encimados (el ancho de un Noxis es 0,7). */
    private static final double OVERLAP = 0.5D;
    private static final double MIN_STEP = 1.8D;
    private static final double MAX_STEP = 3.4D;
    private static final double SPEED = 0.32D;
    private static final int TIMEOUT = 80;
    private static final int COOLDOWN = 200;
    private static final int COOLDOWN_RANDOM = 120;

    private final T mob;
    private int nextCheckTick;
    private long nextAllowedTime = -1L;
    private int streak;
    private @Nullable Vec3 target;
    private @Nullable Vec3 crowdCenter;
    private int ticks;

    public NoxisMakeRoomGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.nextCheckTick = Math.floorMod(mob.getId(), CHECK_INTERVAL);
    }

    @Override
    public boolean canUse() {
        if (this.mob.tickCount < this.nextCheckTick) return false;
        this.nextCheckTick = this.mob.tickCount + CHECK_INTERVAL;
        if (this.mob.level().getGameTime() < this.nextAllowedTime || !this.mob.canRoam()
                || !this.mob.getNavigation().isDone()) {
            this.streak = 0;
            return false;
        }
        Vec3 here = this.mob.position();
        List<PathfinderMob> near = NoxisSpacing.around(this.mob, here, NEAR);
        int lower = 0;
        int count = 0;
        boolean overlap = false;
        double cx = 0.0D, cz = 0.0D;
        for (PathfinderMob e : near) {
            double d2 = NoxisSpacing.horizontal2(e.position(), here);
            if (d2 > NEAR * NEAR) continue;
            count++;
            cx += e.getX();
            cz += e.getZ();
            if (e.getId() < this.mob.getId()) {
                lower++;
                if (d2 < OVERLAP * OVERLAP) overlap = true;
            }
        }
        boolean crowded = (count >= CROWD && lower >= 2) || overlap;
        if (!crowded) {
            this.streak = 0;
            return false;
        }
        if (++this.streak < 2) return false;
        this.streak = 0;
        this.crowdCenter = new Vec3(cx / count, here.y, cz / count);
        this.target = this.pickSpot(here, this.crowdCenter);
        if (this.target == null) {
            this.nextAllowedTime = this.mob.level().getGameTime() + 100L;
            return false;
        }
        return true;
    }

    /** Un lugar cercano (1,8 a 3,4 bloques), hacia afuera del grupo y con espacio libre. */
    private @Nullable Vec3 pickSpot(Vec3 here, Vec3 crowd) {
        RandomSource random = this.mob.getRandom();
        List<PathfinderMob> others = NoxisSpacing.around(this.mob, here, MAX_STEP + NEAR + 1.0D);
        double away = Mth.atan2(here.z - crowd.z, here.x - crowd.x);
        if (NoxisSpacing.horizontal2(here, crowd) < 1.0E-3D) away = random.nextDouble() * Math.PI * 2.0D;
        Vec3 best = null;
        double bestRoom = -1.0D;
        for (int i = 0; i < 6; i++) {
            // Hacia afuera del grupo, abriéndose de a poco para los costados.
            double angle = away + (i == 0 ? 0.0D : ((i & 1) == 0 ? 1.0D : -1.0D) * ((i + 1) / 2) * 0.6D)
                    + (random.nextDouble() - 0.5D) * 0.3D;
            double r = MIN_STEP + random.nextDouble() * (MAX_STEP - MIN_STEP);
            Vec3 p = new Vec3(here.x + Math.cos(angle) * r, here.y, here.z + Math.sin(angle) * r);
            double room = NoxisSpacing.room(others, p, NEAR);
            if (room <= bestRoom) continue;
            if (this.mob.getNavigation().createPath(BlockPos.containing(p), 0) == null) continue;
            best = p;
            bestRoom = room;
            if (room >= NEAR) break;
        }
        return best;
    }

    @Override
    public void start() {
        this.ticks = 0;
        Vec3 t = this.target;
        if (t == null || !this.mob.getNavigation().moveTo(t.x, t.y, t.z, SPEED)) this.target = null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null && this.ticks < TIMEOUT && this.mob.canRoam()
                && !this.mob.getNavigation().isDone();
    }

    @Override
    public void tick() {
        this.ticks++;
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
        // Ya acomodado, mira un momento hacia el grupo (como diciendo "acá estoy bien").
        Vec3 c = this.crowdCenter;
        if (c != null) this.mob.getLookControl().setLookAt(c.x, this.mob.getEyeY(), c.z);
        this.target = null;
        this.crowdCenter = null;
        this.nextAllowedTime = this.mob.level().getGameTime() + COOLDOWN + this.mob.getRandom().nextInt(COOLDOWN_RANDOM);
    }
}
