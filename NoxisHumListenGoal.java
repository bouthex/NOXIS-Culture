package com.noxisculture.entity.ai;

import com.noxisculture.config.NoxisConfig;
import com.noxisculture.entity.idle.NoxisHumAnimation;
import com.noxisculture.entity.personality.NoxisPersonality;
import java.util.EnumSet;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jspecify.annotations.Nullable;

/**
 * Reaccionar a la melodía de otro Noxis (servidor).
 *
 * <p>Cuando alguien tararea o silba cerca, algunos (no todos) reaccionan según su personalidad:</p>
 * <ul>
 *   <li><b>Escuchar</b>: se queda mirándolo con la cabeza ladeada y las orejitas paradas. Los curiosos
 *   a veces se acercan unos pasos primero (sin pegarse).</li>
 *   <li><b>Mecerse</b>: un baile suave en el lugar, al compás de la melodía (100 BPM, sincronizado con
 *   la hora del mundo). No tiene nada que ver con los bailes de los discos.</li>
 * </ul>
 * <p>Como mucho dos reaccionan a cada melodía; cada uno tiene su espera (1 a 2 min) y la opción
 * "Reacciones a las melodías" del menú lo apaga. Prioridad baja (5): cualquier actividad importante
 * lo corta, y termina solo cuando termina la melodía.</p>
 */
public class NoxisHumListenGoal<T extends PathfinderMob & NoxisMusical> extends Goal {
    private static final double HEAR = 10.0D;
    private static final double APPROACH_TO = 3.2D;
    private static final int MAX_REACTORS = 2;
    private static final int COOLDOWN = 1200;
    private static final int COOLDOWN_RANDOM = 1200;
    private static final double SPEED = 0.35D;

    private final T mob;
    private int nextCheckTick;
    private long nextAllowedTime;
    private NoxisHumGoal.@Nullable ActiveHum hum;
    private boolean sway;
    private boolean approach;

    public NoxisHumListenGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /** Ganas de reaccionar y de qué forma, según la personalidad. */
    private static float chance(NoxisPersonality p) {
        return switch (p) {
            case ARTIST -> 0.6F;
            case CURIOUS -> 0.55F;
            case AFFECTIONATE -> 0.5F;
            case MISCHIEVOUS -> 0.4F;
            case CALM -> 0.35F;
        };
    }

    private static float swayChance(NoxisPersonality p) {
        return switch (p) {
            case AFFECTIONATE, ARTIST -> 0.6F;
            case MISCHIEVOUS -> 0.5F;
            case CALM -> 0.3F;
            case CURIOUS -> 0.2F;
        };
    }

    @Override
    public boolean canUse() {
        if (this.mob.tickCount < this.nextCheckTick) return false;
        this.nextCheckTick = this.mob.tickCount + 10 + this.mob.getRandom().nextInt(10);
        if (NoxisHumGoal.ACTIVE.isEmpty() || !NoxisConfig.get().humReactions) return false;
        long now = this.mob.level().getGameTime();
        if (now < this.nextAllowedTime || !this.mob.canHum() || this.mob.getHum() != NoxisHumAnimation.NONE) return false;
        NoxisHumGoal.cleanup(now);
        for (NoxisHumGoal.ActiveHum h : NoxisHumGoal.ACTIVE) {
            if (h.hummer == this.mob || h.hummer.level() != this.mob.level() || h.reactors >= MAX_REACTORS) continue;
            if (h.end() - now < 30 || h.hummer.distanceToSqr(this.mob) > HEAR * HEAR || !this.mob.hasLineOfSight(h.hummer)) continue;
            // Lo decide una sola vez por melodía (si dice que no, espera la siguiente).
            RandomSource random = this.mob.getRandom();
            this.nextAllowedTime = h.end() + 1;
            if (random.nextFloat() >= chance(this.mob.getPersonality())) return false;
            this.hum = h;
            this.sway = random.nextFloat() < swayChance(this.mob.getPersonality());
            this.approach = !this.sway && this.mob.getPersonality() == NoxisPersonality.CURIOUS && random.nextBoolean()
                    && this.mob.distanceTo(h.hummer) > APPROACH_TO + 1.0D;
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        NoxisHumGoal.ActiveHum h = this.hum;
        if (h == null) return;
        h.reactors++;
        this.mob.setHum((byte) ((this.sway ? NoxisHumAnimation.SWAY_BASE : NoxisHumAnimation.LISTEN_BASE) + h.melody));
        if (this.approach) this.mob.getNavigation().moveTo(h.hummer, SPEED);
        else this.mob.getNavigation().stop();
    }

    @Override
    public boolean canContinueToUse() {
        NoxisHumGoal.ActiveHum h = this.hum;
        if (h == null || !h.hummer.isAlive() || !NoxisHumGoal.ACTIVE.contains(h)) return false;
        if (this.mob.hurtTime > 0 || !this.mob.canHum()) return false;
        return this.mob.level().getGameTime() < h.end() + 6;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        NoxisHumGoal.ActiveHum h = this.hum;
        if (h == null) return;
        this.mob.getLookControl().setLookAt(h.hummer, 20.0F, 20.0F);
        if (this.approach) {
            if (this.mob.distanceTo(h.hummer) <= APPROACH_TO) {
                this.approach = false;
                this.mob.getNavigation().stop();
            } else if (this.mob.tickCount % 10 == 0) {
                this.mob.getNavigation().moveTo(h.hummer, SPEED);
            }
        } else {
            this.mob.getNavigation().stop();
        }
    }

    @Override
    public void stop() {
        NoxisHumGoal.ActiveHum h = this.hum;
        if (h != null && h.reactors > 0) h.reactors--;
        this.hum = null;
        this.mob.setHum(NoxisHumAnimation.NONE);
        this.mob.getNavigation().stop();
        this.nextAllowedTime = Math.max(this.nextAllowedTime,
                this.mob.level().getGameTime() + COOLDOWN + this.mob.getRandom().nextInt(COOLDOWN_RANDOM));
    }
}
