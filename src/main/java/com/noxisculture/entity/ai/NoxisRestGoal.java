package com.noxisculture.entity.ai;

import com.noxisculture.sound.ModSounds;
import java.util.EnumSet;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Más o menos una vez por día, cuando se siente seguro, el Noxis se sienta
 * donde esté a descansar entre 30 y 60 segundos, con carita de cansado.
 * Si algo lo asusta o alguien quiere comerciar, se levanta.
 */
public class NoxisRestGoal<T extends PathfinderMob & NoxisRestful> extends Goal {
    private static final int MIN_REST_TICKS = 20 * 30;
    private static final int MAX_REST_TICKS = 20 * 60;

    private final T mob;
    private int remaining;

    public NoxisRestGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.mob.wantsToRest() && this.mob.onGround() && !this.mob.isInWater()
                && this.mob.getRandom().nextInt(20) == 0;
    }

    @Override
    public void start() {
        this.remaining = MIN_REST_TICKS + this.mob.getRandom().nextInt(MAX_REST_TICKS - MIN_REST_TICKS);
        this.mob.getNavigation().stop();
        this.mob.setResting(true);
        this.mob.playSound(ModSounds.NOXIS_YAWN, 0.8F, 1.0F);
    }

    @Override
    public boolean canContinueToUse() {
        return this.remaining > 0 && this.mob.isSafeToRest() && this.mob.hurtTime == 0;
    }

    @Override
    public void tick() {
        this.remaining--;
        if (this.mob.getRandom().nextInt(500) == 0) {
            this.mob.playSound(ModSounds.NOXIS_YAWN, 0.6F, 1.05F);
        }
    }

    @Override
    public void stop() {
        this.mob.setResting(false);
        this.mob.onRestFinished();
    }
}
