package com.noxisculture.entity.idle;

import net.minecraft.util.Mth;

/**
 * Bailar con un disco (solo visual, cliente). El servidor manda la etapa, el estilo (qué
 * coreografía y cómo), el tick del mundo en que empezó la canción y cuántos ticks dura cada
 * tiempo; acá se lleva el peso de cada etapa (entradas y salidas suaves) y la mezcla cuando cambia
 * la coreografía (por ejemplo, de bailar solo a bailar en pareja).
 *
 * <p>Coreografías ({@link #routine}), todas sobre el pulso de la canción:</p>
 * <ul>
 *   <li>{@link #SOLO_A}: rebote de patitas · balanceo de lado · paso torpe · ¡manitos arriba!</li>
 *   <li>{@link #SOLO_B}: pasito de pingüino · vueltita y aplausos · sentadillas felices.</li>
 *   <li>{@link #PAIR_RING}: en pareja, ronda de manitos (vaivén tomados de las manos y una vuelta juntos).</li>
 *   <li>{@link #PAIR_HOPS}: en pareja, saltitos alternados, pasos al costado, giro coordinado y ¡ta-da!</li>
 * </ul>
 *
 * <p>Caídas: {@link #FALL} (resbalón, baile solo) y {@link #PAIR_FALL} (los dos juntos, una sola secuencia).</p>
 */
public final class NoxisDanceAnimation {
    public static final byte NONE = 0;
    public static final byte DANCE = 1;
    public static final byte FALL = 2;
    public static final byte PAIR_FALL = 3;
    private static final int COUNT = 4;
    public static final int FALL_LENGTH = 80;
    public static final int PAIR_FALL_LENGTH = 90;
    /** Tick en que tocan el piso (para la nubecita y el "puf"). */
    public static final int FALL_LAND_TICK = 15;
    public static final int PAIR_FALL_LAND_TICK = 20;

    public static final int SOLO_A = 0;
    public static final int SOLO_B = 1;
    public static final int PAIR_RING = 2;
    public static final int PAIR_HOPS = 3;

    private byte state = NONE;
    private final float[] weight = new float[COUNT];
    private final float[] weightO = new float[COUNT];
    private final int[] time = new int[COUNT];
    private byte style;
    private byte prevStyle;
    private float styleBlend = 1.0F;
    private float styleBlendO = 1.0F;

    // ---------------------------------------------------------------- estilo (un byte)
    // bits 0-1: con qué paso empieza (solo A) · bit 2: lado / papel en la pareja (0 = el que propuso)
    // bits 3-4: retraso propio en ticks (solo) · bits 5-6: coreografía

    public static byte style(int routine, int variant, boolean mirrored, int lag) {
        return (byte) ((variant & 3) | (mirrored ? 4 : 0) | ((lag & 3) << 3) | ((routine & 3) << 5));
    }

    public static int variant(byte style) {
        return style & 3;
    }

    public static boolean mirrored(byte style) {
        return (style & 4) != 0;
    }

    public static int lag(byte style) {
        return (style >> 3) & 3;
    }

    public static int routine(byte style) {
        return (style >> 5) & 3;
    }

    public static boolean isPair(byte style) {
        return routine(style) >= PAIR_RING;
    }

    // ---------------------------------------------------------------- tiempo

    public void tick(byte serverState, byte serverStyle) {
        if (serverState < 0 || serverState >= COUNT) serverState = NONE;
        if (serverState != this.state) {
            this.state = serverState;
            this.time[serverState] = 0;
        }
        // Cambió la coreografía mientras baila: mezcla suave (8 ticks) de la anterior a la nueva.
        this.styleBlendO = this.styleBlend;
        if (serverStyle != this.style) {
            boolean dancing = this.weight[DANCE] > 0.0F;
            this.prevStyle = this.style;
            this.style = serverStyle;
            this.styleBlend = dancing ? 0.0F : 1.0F;
            this.styleBlendO = this.styleBlend;
        }
        this.styleBlend = Math.min(1.0F, this.styleBlend + 0.125F);
        for (int i = 1; i < COUNT; i++) {
            this.weightO[i] = this.weight[i];
            boolean on = i == this.state;
            // Entra con ganas (las caídas, de golpe); cuando termina la música se detiene suave.
            float speed = on ? (i == DANCE ? 0.15F : 0.5F) : 0.08F;
            this.weight[i] = on ? Math.min(1.0F, this.weight[i] + speed) : Math.max(0.0F, this.weight[i] - speed);
            if (on || this.weight[i] > 0.0F) this.time[i]++;
        }
    }

    public boolean isBusy() {
        if (this.state != NONE) return true;
        for (int i = 1; i < COUNT; i++) if (this.weight[i] > 0.0F) return true;
        return false;
    }

    public byte getState() {
        return this.state;
    }

    public float getWeight(byte s, float pt) {
        return Mth.lerp(pt, this.weightO[s], this.weight[s]);
    }

    public float getTime(byte s, float pt) {
        return this.time[s] + pt;
    }

    public byte getStyle() {
        return this.style;
    }

    public byte getPrevStyle() {
        return this.prevStyle;
    }

    public float getStyleBlend(float pt) {
        return Mth.lerp(pt, this.styleBlendO, this.styleBlend);
    }

    /** ¿Justo ahora toca el piso? (para la nubecita de polvo, una sola vez). */
    public boolean landsNow() {
        return (this.state == FALL && this.time[FALL] == FALL_LAND_TICK)
                || (this.state == PAIR_FALL && this.time[PAIR_FALL] == PAIR_FALL_LAND_TICK);
    }

    // ---------------------------------------------------------------- pasos y ojitos

    /** Qué paso del baile solo A toca en ese tiempo (0..3). */
    public static int move(float beat, int variant) {
        return Math.floorMod(Mth.floor(beat / 8.0F) + variant, 4);
    }

    private static float inPhrase(float beat) {
        return beat - 8.0F * Mth.floor(beat / 8.0F);
    }

    /** Ojitos cerrados disfrutando (balanceo de lado en solo A; la vuelta y el ta-da en pareja). */
    public static boolean danceClosed(float beat, byte style) {
        float q = inPhrase(beat);
        return switch (routine(style)) {
            case SOLO_A -> move(beat, variant(style)) == 1 && q >= 2.0F && q < 5.5F;
            case PAIR_RING -> q >= 4.6F && q < 7.4F;
            case PAIR_HOPS -> q >= 7.0F && q < 7.8F;
            default -> false;
        };
    }

    /** Resbalón: "¡pum!" con los ojitos cerrados. */
    public static boolean fallClosed(float t) {
        return t >= 26.0F && t < 36.0F;
    }

    /** Resbalón: pupilas grandes (resbala, y la sorpresa de estar en el piso). */
    public static boolean fallBig(float t) {
        return (t >= 9.0F && t < 20.0F) || (t >= 36.0F && t < 44.0F);
    }

    /** Resbalón: entrecerrados de vergüenza. */
    public static boolean fallHalf(float t) {
        return t >= 54.0F && t < 70.0F;
    }

    /** Caída en pareja: "¡pum!" y la risa con los ojitos cerrados. */
    public static boolean pairFallClosed(float t) {
        return (t >= 22.0F && t < 30.0F) || (t >= 57.0F && t < 67.0F);
    }

    /** Caída en pareja: mareados. */
    public static boolean pairFallHalf(float t) {
        return t >= 30.0F && t < 46.0F;
    }

    /** Caída en pareja: sorpresa al soltarse. */
    public static boolean pairFallBig(float t) {
        return t >= 6.0F && t < 18.0F;
    }
}
