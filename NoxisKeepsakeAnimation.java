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
 *   <li>Lectores y escritores (muy pocos Noxis): se sientan, sacan su libro y leen ({@link #READ_OPEN}
 *   → {@link #READ_MAIN} → {@link #READ_CLOSE}) o escriben ({@link #WRITE_OPEN} → {@link #WRITE_MAIN} →
 *   {@link #WRITE_CLOSE}). Leer: libro en alto con las dos patitas, la cabeza recorre las líneas de
 *   un lado al otro, da vuelta la página y a veces se queda pensando con la patita en el mentón.
 *   Escribir: libro sobre la falda, una patita lo sostiene y la otra escribe en garabatos rápidos de
 *   renglón en renglón, cabeza gacha y ojitos concentrados; a veces se golpea el mentón con la
 *   patita pensando y vuelve a escribir con ganas.</li>
 * </ul>
 */
public final class NoxisKeepsakeAnimation {
    public static final byte NONE = 0;
    public static final byte INSPECT = 1;
    public static final byte CONTEMPLATE = 2;
    public static final byte READ_OPEN = 3;
    public static final byte READ_MAIN = 4;
    public static final byte READ_CLOSE = 5;
    public static final byte WRITE_OPEN = 6;
    public static final byte WRITE_MAIN = 7;
    public static final byte WRITE_CLOSE = 8;
    /** Sentarse y sacar el libro / cerrarlo, guardarlo y pararse. */
    public static final int BOOK_OPEN_LENGTH = 30;
    public static final int BOOK_CLOSE_LENGTH = 30;
    /** Tick de "abrir" en que el libro aparece en sus manitos, y de "cerrar" en que lo guarda. */
    public static final int BOOK_SHOW_TICK = 22;
    public static final int BOOK_HIDE_TICK = 16;
    /** Leyendo: cada renglón dura esto; cada 5 renglones da vuelta la página. */
    public static final int LINE_TICKS = 28;
    public static final int PAGE_TICKS = LINE_TICKS * 5;

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

    /** Largo fijo del gesto (0 = dura lo que el servidor diga: mirar, leer, escribir). */
    public static int length(byte a) {
        return switch (a) {
            case CONTEMPLATE -> CONTEMPLATE_LENGTH;
            case READ_OPEN, WRITE_OPEN -> BOOK_OPEN_LENGTH;
            case READ_CLOSE, WRITE_CLOSE -> BOOK_CLOSE_LENGTH;
            default -> 0;
        };
    }

    public static boolean isBook(byte a) {
        return a >= READ_OPEN && a <= WRITE_CLOSE;
    }

    public static boolean isWriting(byte a) {
        return a >= WRITE_OPEN && a <= WRITE_CLOSE;
    }

    /** Cuánto está sentado (para la postura de sentarse de siempre). */
    public static float sit(byte a, float t) {
        return switch (a) {
            case READ_OPEN, WRITE_OPEN -> smooth(t / 16.0F);
            case READ_MAIN, WRITE_MAIN -> 1.0F;
            case READ_CLOSE, WRITE_CLOSE -> 1.0F - smooth((t - 18.0F) / 12.0F);
            default -> 0.0F;
        };
    }

    /** ¿Se queda pensando ahora? (leyendo o escribiendo). */
    public static float thinking(byte a, float t) {
        if (a == READ_MAIN) {
            float p = t % 280.0F;
            return env(p, 200, 208, 236, 248);
        }
        if (a == WRITE_MAIN) {
            float p = t % 240.0F;
            return env(p, 170, 178, 206, 216);
        }
        return 0.0F;
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    private static float env(float t, float a, float b, float c, float d) {
        return smooth((t - a) / (b - a)) * (1.0F - smooth((t - c) / (d - c)));
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
            if (!this.fading && length(this.anim) > 0 && this.t >= length(this.anim) - 2) {
                this.reset();
                return;
            }
            this.fading = true;
            this.weight = Math.max(0.0F, this.weight - 1.0F / FADE_TICKS);
            if (this.weight <= 0.0F) this.reset();
            return;
        }
        // Mirar, leer y escribir duran lo que el servidor diga; el resto, su largo fijo.
        if (length(this.anim) == 0 || this.t < length(this.anim)) this.t++;
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

    /** Pupilas grandes: mirando el objeto o su tesoro, o pensando (leyendo / escribiendo). */
    public static boolean bigPupils(byte a, float t) {
        return (a == INSPECT && t >= 4.0F) || (a == CONTEMPLATE && t >= 26.0F && t < 60.0F)
                || thinking(a, t) > 0.5F;
    }

    /** Ojitos entornados de concentración (escribiendo). */
    public static boolean focused(byte a, float t) {
        return a == WRITE_MAIN && thinking(a, t) < 0.2F;
    }
}
