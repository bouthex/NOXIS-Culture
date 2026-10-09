package com.noxisculture.entity.idle;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Animación de la "fascinación por los cristales" (solo visual, cliente).
 *
 * <p>Quién decide CUÁNDO pasa es el servidor ({@code NoxisCrystalFascinationGoal}): detecta el
 * cristal, frena al Noxis y le hace mirarlo. Acá solo se arma la reacción a partir de esa señal:
 * ladea la cabecita, para las orejas, levanta los bracitos y los sacude de alegría, da uno o dos
 * saltitos con rebote cartoon, mueve la colita y los ojos brillan con un pulso mágico.
 * Al terminar se calma y el brillo baja despacito.</p>
 *
 * <p>Compartido por todas las especies Noxis (no depende de la ropa).</p>
 */
public final class NoxisCrystalFascination {
    private static final int RISE = 6;          // gesto inicial rápido: "¡oh!"
    private static final int FALL = 14;         // se calma
    private static final int GLOW_IN = 10;
    private static final int GLOW_OUT = 34;     // el brillo baja más lento que el gesto

    // Bracitos arriba (festejo): suben, se sacuden un rato y bajan, aunque siga mirando.
    private static final int CHEER_START = 4;
    private static final int CHEER_UP = 6;
    private static final int CHEER_END = 52;
    private static final int CHEER_DOWN = 12;

    // Saltitos: agachadita, vuelo y aterrizaje con rebote.
    private static final int HOP1 = 7;
    private static final int HOP2 = 21;
    private static final int CROUCH = 2;
    private static final int AIR = 9;
    private static final int LAND = 3;

    private boolean wasActive;
    private int t = -1;            // ticks desde que empezó (-1 = nada)
    private boolean twoHops;
    private float side = 1.0F;

    private float amount, amountO;
    private float glow, glowO;
    private float cheer, cheerO;
    private float hop, hopO;
    private float twitch, twitchO;

    /**
     * @param active  la señal del servidor (está mirando un cristal)
     * @param allowed false si el cliente ve algo prioritario (comercio, miedo...): se calma enseguida
     */
    public void tick(boolean active, boolean allowed, RandomSource random) {
        this.amountO = this.amount;
        this.glowO = this.glow;
        this.cheerO = this.cheer;
        this.hopO = this.hop;
        this.twitchO = this.twitch;
        boolean on = active && allowed;

        if (on && !this.wasActive) {
            this.t = 0;
            this.side = random.nextBoolean() ? 1.0F : -1.0F;
            this.twoHops = random.nextInt(2) == 0;
        }
        this.wasActive = on;

        if (on) {
            this.t++;
            this.amount = Math.min(1.0F, this.amount + 1.0F / RISE);
            float in = smooth(Math.min(1.0F, this.t / (float) GLOW_IN));
            float pulse = 0.7F + 0.3F * Mth.sin(this.t * 0.2F);      // late suave y se nota (~1,6 s)
            this.glow = Math.max(this.glow * 0.9F, in * (this.t < GLOW_IN ? 1.0F : pulse));
            this.cheer = cheerCurve(this.t);
            this.hop = hopCurve(this.t, HOP1) + (this.twoHops ? hopCurve(this.t, HOP2) : 0.0F);
            this.twitch = (this.t == 3 || this.t == 4) ? 1.0F
                    : (this.t == 34 || this.t == 35) ? -1.0F
                    : (this.t == 60 || this.t == 61) ? 1.0F : 0.0F;
        } else {
            this.t = -1;
            this.amount = Math.max(0.0F, this.amount - 1.0F / FALL);
            this.glow = Math.max(0.0F, this.glow - 1.0F / GLOW_OUT);
            this.cheer = Math.max(0.0F, this.cheer - 1.0F / CHEER_DOWN);
            this.hop = 0.0F;
            this.twitch = 0.0F;
        }
        if (!allowed) {
            // Algo prioritario: corta el festejo de golpe (el brillo igual baja suave).
            this.amount = Math.max(0.0F, this.amount - 0.25F);
            this.cheer = Math.max(0.0F, this.cheer - 0.25F);
        }
    }

    private static float cheerCurve(int t) {
        if (t < CHEER_START) return 0.0F;
        if (t < CHEER_START + CHEER_UP) return smooth((t - CHEER_START) / (float) CHEER_UP);
        if (t < CHEER_END) return 1.0F;
        if (t < CHEER_END + CHEER_DOWN) return smooth(1.0F - (t - CHEER_END) / (float) CHEER_DOWN);
        return 0.0F;
    }

    /** Negativo = agachado / aplastado; positivo = en el aire (1 = punto más alto). */
    private static float hopCurve(int t, int start) {
        int p = t - start;
        if (p < 0) return 0.0F;
        if (p < CROUCH) return -0.4F * (p + 1) / CROUCH;
        p -= CROUCH;
        if (p < AIR) {
            float x = (p + 1) / (float) (AIR + 1);
            return 4.0F * x * (1.0F - x);
        }
        p -= AIR;
        if (p < LAND) return -0.35F * (1.0F - p / (float) LAND);
        return 0.0F;
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    public boolean isActive() {
        return this.t >= 0;
    }

    /** 0..1: cuánto de la pose de fascinación (cabeza ladeada, orejas paradas). */
    public float getAmount(float pt) {
        return Mth.lerp(pt, this.amountO, this.amount);
    }

    /** 0..1: brillo extra de los ojos (con el pulso incluido). */
    public float getGlow(float pt) {
        return Mth.lerp(pt, this.glowO, this.glow);
    }

    /** 0..1: bracitos arriba festejando (la cola también se entusiasma con esto). */
    public float getCheer(float pt) {
        return Mth.lerp(pt, this.cheerO, this.cheer);
    }

    /** -0.4..1: saltito (negativo = agachadita o rebote al aterrizar). */
    public float getHop(float pt) {
        return Mth.lerp(pt, this.hopO, this.hop);
    }

    /** -1..1: golpecito de oreja (positivo derecha, negativo izquierda). */
    public float getTwitch(float pt) {
        return Mth.lerp(pt, this.twitchO, this.twitch);
    }

    public float getSide() {
        return this.side;
    }
}
