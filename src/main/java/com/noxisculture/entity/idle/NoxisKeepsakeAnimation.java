package com.noxisculture.entity.idle;

import net.minecraft.util.Mth;

/**
 * Objetos personales (solo visual, cliente; el servidor dice cuál y cuándo):
 *
 * <ul>
 *   <li>{@link #INSPECT} — mirar con curiosidad un objeto (en el piso o en la mano de un jugador):
 *   se inclina hacia él, estira el cuello, lo olfatea con la naricita moviéndose, ladea la cabeza
 *   y para las orejitas hacia adelante.</li>
 *   <li>{@link #CONTEMPLATE} — sacar un tesoro de su inventario privado: busca en su costadito (como
 *   en un bolsillo), lo saca y lo acuna con las dos patitas delante del pecho, lo mira feliz
 *   meciéndose, lo acerca a la carita con cariño (en un regalo, con corazoncitos), lo guarda con
 *   cuidado y le da una palmadita al bolsillo.</li>
 * </ul>
 */
public final class NoxisKeepsakeAnimation {
    public static final byte NONE = 0;
    public static final byte INSPECT = 1;
    public static final byte CONTEMPLATE = 2;

    public static final int INSPECT_LENGTH = 60;
    public static final int CONTEMPLATE_LENGTH = 112;
    /** Tick en que el tesoro aparece en la mano y en que se guarda (el servidor lo muestra / lo oculta). */
    public static final int TAKE_OUT_TICK = 12;
    public static final int PUT_AWAY_TICK = 98;
    /** Corazoncitos (si es un regalo) y soniditito feliz. */
    public static final int LOVE_TICK = 58;
    private static final float FADE_TICKS = 5.0F;

    private byte anim = NONE;
    private int t;
    private boolean fading;
    private float weight;
    private float weightO;

    public static int length(byte a) {
        return a == INSPECT ? INSPECT_LENGTH : a == CONTEMPLATE ? CONTEMPLATE_LENGTH : 0;
    }

    public void tick(byte serverAnim) {
        this.weightO = this.weight;
        if (serverAnim != NONE && serverAnim != this.anim) {
            this.anim = serverAnim;
            this.t = 0;
            this.fading = false;
            this.weight = 1.0F;
            this.weightO = 1.0F;
            return;
        }
        if (this.anim == NONE) return;
        if (serverAnim == NONE || this.fading) {
            if (!this.fading && this.t >= length(this.anim) - 2) {
                this.reset();
                return;
            }
            this.fading = true;
            this.weight = Math.max(0.0F, this.weight - 1.0F / FADE_TICKS);
            if (this.weight <= 0.0F) this.reset();
            return;
        }
        // Mirar un objeto dura lo que el servidor diga (se queda en la pose hasta que termina).
        if (this.anim == INSPECT || this.t < length(this.anim)) this.t++;
    }

    private void reset() {
        this.anim = NONE;
        this.t = 0;
        this.fading = false;
        this.weight = 0.0F;
        this.weightO = 0.0F;
    }

    public boolean isBusy() {
        return this.anim != NONE;
    }

    public byte getAnim() {
        return this.anim;
    }

    public float getTime(float pt) {
        if (this.anim == NONE) return 0.0F;
        return this.fading ? this.t : this.t + pt;
    }

    public float getWeight(float pt) {
        return Mth.lerp(pt, this.weightO, this.weight);
    }

    /** Ojitos: cerrados de felicidad al acercarse el tesoro a la carita. */
    public static boolean eyesClosed(byte a, float t) {
        return a == CONTEMPLATE && t >= 62.0F && t < 76.0F;
    }

    /** Pupilas grandes: mirando el objeto o su tesoro. */
    public static boolean bigPupils(byte a, float t) {
        return (a == INSPECT && t >= 4.0F) || (a == CONTEMPLATE && t >= 26.0F && t < 60.0F);
    }
}
