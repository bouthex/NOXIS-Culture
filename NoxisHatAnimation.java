package com.noxisculture.entity.idle;

import net.minecraft.util.Mth;

/**
 * Animación de sacarse y ponerse el sombrero (solo visual, cliente). El servidor decide CUÁNDO
 * (byte {@link #OFF} / {@link #ON}) y en qué tick aparece o desaparece el sombrero de verdad; acá
 * solo se cuenta el tiempo y se calculan las curvas que usa el modelo.
 *
 * <ul>
 *   <li><b>Sacárselo</b> ({@link #OFF_LENGTH} ticks): levanta los bracitos, el sombrero sube de
 *   la cabeza con un bamboleo, se inclina hacia adelante y baja hasta el piso, donde lo deja
 *   (en ese momento el servidor coloca el bloque).</li>
 *   <li><b>Ponérselo</b> ({@link #ON_LENGTH} ticks): agachadito, lo levanta del piso (el bloque
 *   desaparece), lo sube por encima de la cabeza, lo baja hasta acomodarlo y se bambolea.</li>
 * </ul>
 *
 * <p>Compartido por todas las especies Noxis (no depende de la ropa).</p>
 */
public final class NoxisHatAnimation {
    public static final byte NONE = 0;
    public static final byte OFF = 1;
    public static final byte ON = 2;

    public static final int OFF_LENGTH = 20;
    /** Tick de "sacarse" en el que el sombrero queda apoyado en el piso (aparece el bloque). */
    public static final int OFF_PLACE_TICK = 15;
    public static final int ON_LENGTH = 22;
    /** Tick de "ponerse" en el que agarra el sombrero (desaparece el bloque). */
    public static final int ON_TAKE_TICK = 2;

    private byte anim = NONE;
    private int t;

    public void tick(byte serverAnim) {
        if (serverAnim != this.anim) {
            this.anim = serverAnim;
            this.t = 0;
        } else if (this.anim != NONE) {
            this.t++;
        }
    }

    public byte getAnim() {
        return this.anim;
    }

    public float getTime(float partialTick) {
        return this.anim == NONE ? 0.0F : this.t + partialTick;
    }

    /**
     * Curvas de la animación: {lift, carry, relax, wobble}.
     * lift = sombrero levantado sobre la cabeza; carry = llevado hacia adelante y abajo (piso);
     * relax = vuelta a la postura normal; wobble = bamboleo del sombrero (radianes).
     */
    public static float[] curves(byte type, float t) {
        float lift = 0.0F, carry = 0.0F, relax = 0.0F, wob = 0.0F;
        if (type == OFF) {
            lift = smooth((t - 2.0F) / 6.0F);
            carry = smooth((t - 9.0F) / 6.0F);
            relax = smooth((t - 16.0F) / 4.0F);
            wob = Mth.sin(t * 0.9F) * 0.12F * lift * (1.0F - carry);
        } else if (type == ON) {
            carry = 1.0F - smooth((t - 2.0F) / 7.0F);
            lift = 1.0F - smooth((t - 12.0F) / 5.0F);
            relax = smooth((t - 17.0F) / 4.0F);
            if (t >= 17.0F && t < 22.0F) {
                wob = Mth.sin((t - 17.0F) * 1.6F) * 0.15F * (1.0F - (t - 17.0F) / 5.0F);   // se acomoda
            }
        }
        return new float[] {lift, carry, relax, wob};
    }

    public static float smooth(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }
}
