package com.noxisculture.entity.ai;

import com.noxisculture.entity.social.NoxisFriendship;
import java.util.EnumSet;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jspecify.annotations.Nullable;

/**
 * Buscarse un poco (servidor): de vez en cuando, en vez de pasear al azar, un Noxis camina hacia
 * donde está su amigo especial, hasta quedar cerca (no pegado). Ocasional y con espera propia,
 * así cada uno sigue con su vida. Lo corta cualquier otra actividad.
 */
public class NoxisSeekFriendGoal<T extends PathfinderMob & NoxisFriend> extends Goal {
    private static final double MIN_DISTANCE = 8.0D;    // más cerca que esto, no hace falta buscarlo
    private static final double MAX_DISTANCE = 28.0D;   // más lejos, no lo busca
    private static final double STOP_DISTANCE = 4.0D;
    private static final int CHANCE = 120;              // por consulta (cada 2 ticks): ~12 s en promedio
    private static final int COOLDOWN = 1200;           // 1 min ...
    private static final int COOLDOWN_RANDOM = 1800;    // ... a 2,5 min
    private static final int TIMEOUT = 400;
    private static final double SPEED = 0.38D;

    private final T mob;
    private long nextAllowedTime;
    private @Nullable PathfinderMob friend;
    private int ticks;

    public NoxisSeekFriendGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.mob.level().getGameTime() < this.nextAllowedTime) return false;
        if (this.mob.getRandom().nextInt(CHANCE) != 0 || !this.mob.canSocialize()) return false;
        PathfinderMob other = NoxisFriendship.findFriend(this.mob);
        if (other == null) return false;
        double d2 = this.mob.distanceToSqr(other);
        if (d2 < MIN_DISTANCE * MIN_DISTANCE || d2 > MAX_DISTANCE * MAX_DISTANCE) return false;
        if (this.mob.getNavigation().createPath(other, 3) == null) return false;
        this.friend = other;
        return true;
    }

    @Override
    public void start() {
        this.ticks = 0;
        if (this.friend != null) this.mob.getNavigation().moveTo(this.friend, SPEED);
    }

    @Override
    public boolean canContinueToUse() {
        PathfinderMob other = this.friend;
        return other != null && other.isAlive() && this.ticks < TIMEOUT && this.mob.canSocialize()
                && this.mob.distanceToSqr(other) > STOP_DISTANCE * STOP_DISTANCE;
    }

    @Override
    public void tick() {
        this.ticks++;
        if (this.friend != null && (this.ticks % 20 == 0 || this.mob.getNavigation().isDone())) {
            this.mob.getNavigation().moveTo(this.friend, SPEED);
        }
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
        this.friend = null;
        this.nextAllowedTime = this.mob.level().getGameTime() + COOLDOWN + this.mob.getRandom().nextInt(COOLDOWN_RANDOM);
    }
}
