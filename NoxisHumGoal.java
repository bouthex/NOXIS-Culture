package com.noxisculture.entity.ai;

import com.noxisculture.config.NoxisConfig;
import com.noxisculture.entity.idle.NoxisHumAnimation;
import com.noxisculture.entity.personality.NoxisPersonality;
import com.noxisculture.sound.ModSounds;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Tararear o silbar una melodía espontánea (servidor).
 *
 * <p>De vez en cuando, un Noxis tranquilo se queda quieto y tararea (melodías 1-3) o silba (4-6) una
 * melodía breve y original del mod. Los Artistas y los lectores/escritores (los "creativos") lo hacen
 * más seguido, pero nunca de forma constante; el resto también puede, con menos frecuencia.</p>
 *
 * <ul>
 *   <li>Espera propia de 4 a 8 minutos (escalada por la opción "Melodías" del menú; 0 = nunca).</li>
 *   <li>Nunca dos Noxis tarareando a la vez en 16 bloques.</li>
 *   <li>Empieza siempre en un pulso (tick múltiplo de {@link NoxisHumAnimation#BEAT}), así quienes se
 *   mecen al lado van al compás.</li>
 *   <li>Prioridad baja (5): sueño, descanso, amigos, objetos, regalos, lectura, escucha, bailes de
 *   discos y demás actividades no lo dejan empezar y lo cortan.</li>
 * </ul>
 */
public class NoxisHumGoal<T extends PathfinderMob & NoxisMusical> extends Goal {
    private static final double ZONE = 16.0D;
    private static final int COOLDOWN = 4800;           // 4 min ...
    private static final int COOLDOWN_RANDOM = 4800;    // ... a 8 min
    private static final float BASE_CHANCE = 0.12F;     // por revisión (cada 4-7 s), pasada la espera

    /** Melodías sonando ahora (servidor). Las usan los que reaccionan. */
    static final List<ActiveHum> ACTIVE = new ArrayList<>();

    /** Una melodía en curso: quién, cuál, desde qué tick y cuántos reaccionaron. */
    static final class ActiveHum {
        final PathfinderMob hummer;
        final byte melody;
        final long start;
        int reactors;

        ActiveHum(PathfinderMob hummer, byte melody, long start) {
            this.hummer = hummer;
            this.melody = melody;
            this.start = start;
        }

        long end() {
            return this.start + NoxisHumAnimation.length(this.melody);
        }
    }

    private final T mob;
    private int nextCheckTick;
    private long nextAllowedTime = -1L;
    private byte melody;
    private long startTick = -1L;
    private @org.jspecify.annotations.Nullable ActiveHum active;

    public NoxisHumGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.nextCheckTick = mob.getRandom().nextInt(200);
    }

    private static float wish(NoxisMusical m) {
        float w = m.getPersonality() == NoxisPersonality.ARTIST ? 3.0F : 1.0F;
        if (m.isScholar()) w *= 2.5F;
        return w;
    }

    static void cleanup(long now) {
        ACTIVE.removeIf(h -> !h.hummer.isAlive() || now > h.end() + 20);
    }

    @Override
    public boolean canUse() {
        if (this.mob.tickCount < this.nextCheckTick) return false;
        RandomSource random = this.mob.getRandom();
        this.nextCheckTick = this.mob.tickCount + 80 + random.nextInt(60);
        int freq = NoxisConfig.get().humming;
        if (!NoxisConfig.enabled(freq)) return false;
        long now = this.mob.level().getGameTime();
        if (this.nextAllowedTime < 0L) {
            this.nextAllowedTime = now + NoxisConfig.scaleCooldown(2400 + random.nextInt(3600), freq);   // el primero: 2 a 5 min
        }
        if (now < this.nextAllowedTime || !this.mob.canHum() || this.mob.getHum() != NoxisHumAnimation.NONE) return false;
        if (random.nextFloat() >= BASE_CHANCE * wish(this.mob)) return false;
        cleanup(now);
        for (ActiveHum h : ACTIVE) {
            if (h.hummer.level() == this.mob.level() && h.hummer.distanceToSqr(this.mob) < ZONE * ZONE) return false;
        }
        return true;
    }

    @Override
    public void start() {
        RandomSource random = this.mob.getRandom();
        // Los artistas tararean un poco más; los demás, mitad y mitad.
        boolean hum = this.mob.getPersonality() == NoxisPersonality.ARTIST ? random.nextFloat() < 0.65F : random.nextBoolean();
        this.melody = (byte) ((hum ? 1 : 4) + random.nextInt(3));
        this.startTick = -1L;
        this.active = null;
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.mob.canHum() && this.startTick < 0L) return false;
        if (this.mob.hurtTime > 0) return false;
        if (this.startTick < 0L) return true;                       // esperando el pulso (como mucho 12 ticks)
        return this.mob.level().getGameTime() < this.startTick + NoxisHumAnimation.length(this.melody)
                && this.mob.canHum();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.mob.getNavigation().stop();
        long now = this.mob.level().getGameTime();
        if (this.startTick < 0L && now % NoxisHumAnimation.BEAT == 0) {
            this.startTick = now;
            this.mob.setHum(this.melody);
            this.mob.playSound(ModSounds.NOXIS_MELODIES[this.melody - 1], 0.6F, 1.0F);
            this.active = new ActiveHum(this.mob, this.melody, now);
            ACTIVE.add(this.active);
        }
    }

    @Override
    public void stop() {
        this.mob.setHum(NoxisHumAnimation.NONE);
        if (this.active != null) ACTIVE.remove(this.active);
        this.active = null;
        this.startTick = -1L;
        RandomSource random = this.mob.getRandom();
        this.nextAllowedTime = this.mob.level().getGameTime()
                + NoxisConfig.scaleCooldown(COOLDOWN + random.nextInt(COOLDOWN_RANDOM), NoxisConfig.get().humming);
    }
}
