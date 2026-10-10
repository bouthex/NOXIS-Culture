package com.noxisculture.entity.idle;

import net.minecraft.util.Mth;

/**
 * Aburrimiento: tres gestos propios de cuando un Noxis lleva un rato sin hacer nada.
 * El servidor decide CUÁL y CUÁNDO (byte sincronizado, ver {@link #encode}); acá se cuenta el
 * tiempo en el cliente y, si el servidor lo corta antes de terminar (comercio, susto, sueño...),
 * el gesto se desvanece en pocos ticks hasta la postura normal, sin saltos.
 *
 * <ul>
 *   <li>{@link #TAP}: pataditas impacientes contra el suelo.</li>
 *   <li>{@link #HAT}: se acomoda el sombrero (solo si lo lleva puesto).</li>
 *   <li>{@link #PAWS}: se entretiene mirando y jugando con sus propias patitas.</li>
 * </ul>
 *
 * <p>Compartido por todas las especies Noxis, con o sin capa (no depende de la ropa).</p>
 */
public final class NoxisBoredAnimation {
    public static final byte NONE = 0;
    public static final byte TAP = 1;
    public static final byte HAT = 2;
    public static final byte PAWS = 3;
    /** Bit de "espejado": usa la pata / el brazo izquierdo en vez del derecho. */
    public static final byte MIRRORED = 0x10;
    private static final byte TYPE_MASK = 0x0F;

    public static final int TAP_LENGTH = 76;
    public static final int HAT_LENGTH = 72;
    public static final int PAWS_LENGTH = 100;
    /** Ticks en que la patita golpea el suelo (sonido y polvito los pone el servidor). */
    public static final int[] TAP_HITS = {16, 23, 30};
    /** Tick en que agarra el ala del sombrero. */
    public static final int HAT_GRIP_TICK = 14;
    /** Tick del gesto de satisfacción al terminar de acomodarlo. */
    public static final int HAT_PROUD_TICK = 52;
    /** Tick en que empieza a jugar con las dos patitas. */
    public static final int PAWS_PLAY_TICK = 46;
    /** Ticks que tarda en volver a la postura normal si se corta a mitad de camino. */
    private static final float FADE_TICKS = 5.0F;

    private byte anim = NONE;
    private int t;
    private boolean fading;
    private float weight;
    private float weightO;

    public static byte encode(byte type, boolean mirrored) {
        return (byte) (type | (mirrored ? MIRRORED : 0));
    }

    public static byte typeOf(byte anim) {
        return (byte) (anim & TYPE_MASK);
    }

    public static boolean isMirrored(byte anim) {
        return (anim & MIRRORED) != 0;
    }

    public static int length(byte type) {
        return switch (typeOf(type)) {
            case TAP -> TAP_LENGTH;
            case HAT -> HAT_LENGTH;
            case PAWS -> PAWS_LENGTH;
            default -> 0;
        };
    }

    // ------------------------------------------------------------------ reloj del cliente

    public void tick(byte serverAnim) {
        this.weightO = this.weight;
        if (serverAnim != NONE && serverAnim != this.anim) {
            this.anim = serverAnim;          // empieza uno nuevo (desde la postura actual)
            this.t = 0;
            this.fading = false;
            this.weight = 1.0F;
            this.weightO = 1.0F;
            return;
        }
        if (this.anim == NONE) return;
        if (serverAnim == NONE || this.fading) {
            if (!this.fading && this.t >= length(this.anim) - 2) {
                this.reset();                // terminó normalmente: ya está en su postura
                return;
            }
            // Cortado por algo más importante: vuelve suave a la postura normal.
            this.fading = true;
            this.weight = Math.max(0.0F, this.weight - 1.0F / FADE_TICKS);
            if (this.weight <= 0.0F) this.reset();
            return;
        }
        if (this.t < length(this.anim)) this.t++;
    }

    private void reset() {
        this.anim = NONE;
        this.t = 0;
        this.fading = false;
        this.weight = 0.0F;
        this.weightO = 0.0F;
    }

    /** ¿Está haciendo (o terminando) un gesto de aburrimiento? */
    public boolean isBusy() {
        return this.anim != NONE;
    }

    public byte getType() {
        return typeOf(this.anim);
    }

    public boolean isMirrored() {
        return isMirrored(this.anim);
    }

    public float getTime(float partialTick) {
        if (this.anim == NONE) return 0.0F;
        return this.fading ? this.t : Math.min(length(this.anim), this.t + partialTick);
    }

    /** 0..1: cuánto del gesto se ve (1 normalmente; baja a 0 si se cortó). */
    public float getWeight(float partialTick) {
        return Mth.lerp(partialTick, this.weightO, this.weight);
    }

    // ------------------------------------------------------------------ ojitos

    /** Ojitos cerrados (satisfecho tras acomodarse el sombrero; parpadeo al jugar). */
    public static boolean eyesClosed(byte type, float t) {
        return (type == HAT && t >= 53.0F && t < 62.0F) || (type == PAWS && t >= 77.0F && t < 80.0F);
    }

    /** Pupilas grandes de curiosidad (mirando sus propias patitas). */
    public static boolean bigPupils(byte type, float t) {
        return type == PAWS && t >= 16.0F && t < 77.0F;
    }

    /** Ojitos entrecerrados de aburrimiento (al mirar a un costado tras las pataditas). */
    public static boolean tiredEyes(byte type, float t) {
        return type == TAP && t >= 46.0F && t < 68.0F;
    }

    // ------------------------------------------------------------------ curvas

    public static float smooth(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    /** Sube suave de a→b, se mantiene, y baja suave de c→d. */
    public static float env(float t, float a, float b, float c, float d) {
        return smooth((t - a) / (b - a)) * (1.0F - smooth((t - c) / (d - c)));
    }

    /** Pico corto (golpecito) centrado en {@code center}. */
    public static float pulse(float t, float center, float width) {
        return Math.max(0.0F, 1.0F - Math.abs(t - center) / width);
    }
}
