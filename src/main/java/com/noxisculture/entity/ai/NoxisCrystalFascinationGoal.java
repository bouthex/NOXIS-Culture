package com.noxisculture.entity.ai;

import com.noxisculture.entity.idle.NoxisCrystals;
import java.util.EnumSet;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Fascinación por los cristales (servidor): si un Noxis tranquilo tiene cerca amatista o algo
 * cristalino de Noxus, se frena, lo mira fijo unos segundos y festeja (la animación y el brillo
 * de ojos los hace el cliente cuando recibe {@code setFascinated(true)}).
 *
 * <p>Barato: busca cristales una vez por segundo (no cada tick) y, después de cada reacción,
 * espera entre 30 y 50 segundos antes de poder repetirla.</p>
 */
public class NoxisCrystalFascinationGoal<T extends PathfinderMob & NoxisFascinatable> extends Goal {
    private static final int RADIUS = 6;
    private static final int RADIUS_Y = 3;
    private static final int SCAN_INTERVAL = 20;
    private static final int MIN_TICKS = 100;          // ~5 s mirándolo y festejando
    private static final int EXTRA_TICKS = 30;
    private static final int COOLDOWN = 600;           // 30 s ...
    private static final int COOLDOWN_RANDOM = 400;    // ... a 50 s

    private final T mob;
    /** Tick (del propio Noxis) del próximo escaneo y momento desde el que puede volver a fascinarse. */
    private int nextScanTick;
    private long nextAllowedTime;
    private int remaining;
    private @Nullable Vec3 target;

    public NoxisCrystalFascinationGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.nextScanTick = mob.getRandom().nextInt(SCAN_INTERVAL);   // los Noxis no buscan todos a la vez
    }

    @Override
    public boolean canUse() {
        // canUse se consulta seguido: el reloj del mundo y el tickCount mantienen los tiempos exactos.
        if (this.mob.level().getGameTime() < this.nextAllowedTime) return false;
        if (this.mob.tickCount < this.nextScanTick) return false;
        this.nextScanTick = this.mob.tickCount + SCAN_INTERVAL;
        if (!this.mob.canBeFascinated() || !this.mob.onGround() || this.mob.isInWater()) return false;
        this.target = NoxisCrystals.findNearest(this.mob, RADIUS, RADIUS_Y);
        return this.target != null;
    }

    @Override
    public void start() {
        this.remaining = MIN_TICKS + this.mob.getRandom().nextInt(EXTRA_TICKS);
        this.mob.getNavigation().stop();
        this.mob.setFascinated(true);
        this.lookAtTarget();
    }

    @Override
    public boolean canContinueToUse() {
        if (this.remaining <= 0 || this.target == null || !this.mob.canBeFascinated() || this.mob.hurtTime > 0) {
            return false;
        }
        // Cada medio segundo comprueba que el cristal siga ahí.
        return this.remaining % 10 != 0 || NoxisCrystals.stillThere(this.mob.level(), this.target);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.remaining--;
        this.mob.getNavigation().stop();
        this.lookAtTarget();
    }

    private void lookAtTarget() {
        if (this.target != null) {
            this.mob.getLookControl().setLookAt(this.target.x, this.target.y, this.target.z, 30.0F, 30.0F);
        }
    }

    @Override
    public void stop() {
        this.mob.setFascinated(false);
        this.target = null;
        this.nextAllowedTime = this.mob.level().getGameTime() + COOLDOWN + this.mob.getRandom().nextInt(COOLDOWN_RANDOM);
    }
}
