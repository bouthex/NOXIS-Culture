package com.noxisculture.entity.ai;

import com.noxisculture.entity.idle.NoxisPersonaAnimation;
import com.noxisculture.entity.personality.NoxisPersonable;
import com.noxisculture.entity.personality.NoxisPersonality;
import java.util.EnumSet;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Gesto propio de la personalidad (servidor): de vez en cuando, estando tranquilo, el Noxis hace
 * su gesto característico y vocaliza. Ocasional (espera de 2,5 a 5 min), nunca encima de otra
 * actividad, y nunca dos Noxis cerca a la vez (así no vocalizan todos juntos). Cualquier cosa
 * más importante lo corta (el cliente vuelve suave a la postura normal).
 */
public class NoxisPersonaGestureGoal<T extends PathfinderMob & NoxisPersonable> extends Goal {
    private static final double ZONE = 10.0D;
    private static final int COOLDOWN = 3000;          // 2,5 min ...
    private static final int COOLDOWN_RANDOM = 3000;   // ... a 5 min

    private final T mob;
    private int nextCheckTick;
    private long nextAllowedTime = -1L;
    private byte gesture;
    private int ticks;

    public NoxisPersonaGestureGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.nextCheckTick = mob.getRandom().nextInt(200);
    }

    @Override
    public boolean canUse() {
        if (this.mob.tickCount < this.nextCheckTick) return false;
        RandomSource random = this.mob.getRandom();
        this.nextCheckTick = this.mob.tickCount + 60 + random.nextInt(60);
        long now = this.mob.level().getGameTime();
        if (this.nextAllowedTime < 0L) this.nextAllowedTime = now + 1200 + random.nextInt(2400);   // el primero: 1 a 3 min
        if (now < this.nextAllowedTime || random.nextInt(3) != 0 || !this.mob.canDoPersonaGesture()) return false;
        // Nadie más haciendo su gesto cerca (sin coros ni gestos repetidos al mismo tiempo).
        return this.mob.level().getEntitiesOfClass(PathfinderMob.class, this.mob.getBoundingBox().inflate(ZONE, 4.0D, ZONE),
                e -> e != this.mob && e instanceof NoxisPersonable p && p.getPersonaGesture() != NoxisPersonaAnimation.NONE)
                .isEmpty();
    }

    @Override
    public void start() {
        NoxisPersonality personality = this.mob.getPersonality();
        this.gesture = personality.gesture();
        this.ticks = 0;
        this.mob.getNavigation().stop();
        this.mob.setPersonaGesture(this.gesture);
    }

    @Override
    public boolean canContinueToUse() {
        return this.ticks < NoxisPersonaAnimation.length(this.gesture) && this.mob.canDoPersonaGesture()
                && this.mob.hurtTime == 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.ticks++;
        this.mob.getNavigation().stop();
        if (this.ticks == NoxisPersonaAnimation.voiceTick(this.gesture)) {
            this.mob.playSound(this.mob.getPersonality().voice(), 0.7F, 0.95F + this.mob.getRandom().nextFloat() * 0.1F);
        }
    }

    @Override
    public void stop() {
        this.mob.setPersonaGesture(NoxisPersonaAnimation.NONE);
        this.nextAllowedTime = this.mob.level().getGameTime() + COOLDOWN + this.mob.getRandom().nextInt(COOLDOWN_RANDOM);
    }
}
