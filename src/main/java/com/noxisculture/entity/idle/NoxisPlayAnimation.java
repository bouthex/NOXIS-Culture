package com.noxisculture.entity.idle;

import net.minecraft.util.Mth;

/**
 * Curiosidad y travesuras entre compañeros (solo visual, cliente). El servidor dice en qué
 * estado está cada Noxis (byte sincronizado); acá se cuenta el tiempo de cada estado y se mezclan
 * con transiciones suaves (cada estado tiene su propio peso que sube y baja).
 *
 * <p>El que sigue: {@link #FOLLOW} (lo sigue curioso, ladeando la cabeza de vez en cuando),
 * {@link #WATCH} (el otro se frenó: lo observa con la patita en el pecho), y en la travesura
 * {@link #SNEAK} (se acerca agachadito, en puntitas), {@link #CROUCH} (espera agazapado moviendo
 * la colita), {@link #POUNCE} (¡salta de golpe con los bracitos arriba!) y {@link #CHEER} (festeja
 * riéndose). El sorprendido: {@link #STARTLED} (pega un saltito con las manos arriba; el miedo de
 * siempre lo pone el ánimo) y {@link #PLAYFUL} (se recupera y le devuelve un manotazo juguetón).</p>
 *
 * <p>Compartido por todas las especies Noxis (no depende de la ropa).</p>
 */
public final class NoxisPlayAnimation {
    public static final byte NONE = 0;
    public static final byte FOLLOW = 1;
    public static final byte WATCH = 2;
    public static final byte SNEAK = 3;
    public static final byte CROUCH = 4;
    public static final byte POUNCE = 5;
    public static final byte CHEER = 6;
    public static final byte STARTLED = 7;
    public static final byte PLAYFUL = 8;
    private static final int COUNT = 9;

    /** Largos (ticks) de los gestos con duración fija. */
    public static final int POUNCE_LENGTH = 14;
    public static final int CHEER_LENGTH = 40;
    public static final int STARTLED_LENGTH = 24;
    public static final int PLAYFUL_LENGTH = 32;

    private byte state = NONE;
    private final float[] weight = new float[COUNT];
    private final float[] weightO = new float[COUNT];
    private final int[] time = new int[COUNT];

    public void tick(byte serverState) {
        if (serverState < 0 || serverState >= COUNT) serverState = NONE;
        if (serverState != this.state) {
            this.state = serverState;
            this.time[serverState] = 0;                  // cada estado arranca su coreografía de cero
        }
        for (int i = 1; i < COUNT; i++) {
            this.weightO[i] = this.weight[i];
            boolean on = i == this.state;
            // Los gestos repentinos (salto, susto) entran rápido; el resto, suave.
            float speed = on && (i == POUNCE || i == STARTLED) ? 0.5F : 0.18F;
            this.weight[i] = on ? Math.min(1.0F, this.weight[i] + speed) : Math.max(0.0F, this.weight[i] - 0.18F);
            if (on || this.weight[i] > 0.0F) this.time[i]++;
        }
    }

    public boolean isBusy() {
        if (this.state != NONE) return true;
        for (int i = 1; i < COUNT; i++) if (this.weight[i] > 0.0F) return true;
        return false;
    }

    public float getWeight(byte s, float pt) {
        return Mth.lerp(pt, this.weightO[s], this.weight[s]);
    }

    public float getTime(byte s, float pt) {
        return this.time[s] + pt;
    }

    public byte getState() {
        return this.state;
    }

    // ------------------------------------------------------------------ ojitos

    /** Ojitos entrecerrados de pícaro (acercándose a escondidas). */
    public static boolean slyEyes(byte state) {
        return state == SNEAK || state == CROUCH;
    }

    /** Pupilas grandes (observando, saltando, sorprendido, jugando). */
    public static boolean bigPupils(byte state) {
        return state == WATCH || state == POUNCE || state == STARTLED || state == PLAYFUL;
    }

    /** Ojitos cerrados de risa (festejando la travesura). */
    public static boolean laughing(byte state, float t) {
        return state == CHEER && t > 2.0F && t < 34.0F;
    }
}
