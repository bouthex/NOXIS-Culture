package com.noxisculture.entity.ai;

import java.util.EnumSet;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Mientras se estira al despertar, el Noxis se queda quieto (no camina ni mira a otro lado).
 * Comercio, miedo y sueño (prioridad 1) lo cortan igual; la propia entidad cancela el
 * estiramiento si algo más importante pasa.
 */
public class NoxisStretchGoal<T extends PathfinderMob & NoxisRestful> extends Goal {
    private final T mob;

    public NoxisStretchGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return this.mob.isStretching();
    }

    @Override
    public boolean canContinueToUse() {
        return this.mob.isStretching();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.mob.getNavigation().stop();
    }
}
