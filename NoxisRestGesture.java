package com.noxisculture.entity.idle;

import net.minecraft.util.Mth;

/**
 * Gestos alrededor del descanso, compartidos por todas las especies Noxis (con o sin capa):
 *
 * <ul>
 *   <li>{@link #SETTLE} — <b>acomodarse para descansar</b>: mira el lugar, junta las patitas, baja
 *   el cuerpo suave, se sienta (la transición de siempre) y se acomoda con un balanceo hasta
 *   quedar cómodo. En la pecera arranca directo del balanceo (el salto ya lo hizo).</li>
 *   <li>{@link #STRETCH_SEATED} — <b>estirarse al despertar</b> (descanso solo o en compañía):
 *   abre los ojos despacito, levanta la cabeza, se encoge (anticipación), estira los bracitos
 *   arriba y afuera, arquea el cuerpo y estira las patitas, bosteza, baja los brazos, se levanta,
 *   se sacude suave y, si tiene sombrero, se lo acomoda.</li>
 *   <li>{@link #STRETCH_STAND} — lo mismo pero de pie (al salir de la pecera).</li>
 * </ul>
 *
 * <p>El servidor decide CUÁL (byte sincronizado, ver {@link #encode}) y lo mantiene quieto; acá
 * se cuenta el tiempo en el cliente. Si algo más importante lo corta, se desvanece en pocos
 * ticks hasta la postura normal.</p>
 */
public final class NoxisRestGesture {
    public static final byte NONE = 0;
    public static final byte SETTLE = 1;
    public static final byte STRETCH_SEATED = 2;
    public static final byte STRETCH_STAND = 3;
    private static final int TYPE_MASK = 0x07;
    /** Tiene sombrero: al final se lo acomoda. */
    public static final int HAT_FLAG = 0x08;
    /** Desfase propio (0..7, x2 ticks) antes de abrir los ojos: así dos compañeros nunca van sincronizados. */
    private static final int LEAD_SHIFT = 4;

    // ---- Acomodarse ----
    public static final int SETTLE_LENGTH = 60;
    /** Hasta acá prepara (de pie); después empieza la sentada de siempre. */
    public static final int SETTLE_SIT_TICK = 14;
    /** En la pecera arranca acá (solo el balanceo para acomodarse). */
    public static final int SETTLE_BOWL_START = 28;

    // ---- Estirarse sentado (tiempos desde que abre los ojos, sin contar el desfase) ----
    private static final int SEATED_BASE = 80;
    /** Tick en que se levanta (vuelve la transición de siempre para pararse). */
    public static final int SEATED_STAND_TICK = 50;
    public static final int SEATED_YAWN_TICK = 34;
    // ---- Estirarse de pie ----
    private static final int STAND_BASE = 60;
    public static final int STAND_YAWN_TICK = 24;
    /** Lo que dura acomodarse el sombrero al final. */
    private static final int HAT_EXTRA = 16;
    private static final float FADE_TICKS = 5.0F;

    private byte gesture = NONE;
    private int t;
    private boolean fading;
    private float weight;
    private float weightO;

    public static byte encode(byte type, boolean hat, int lead) {
        return (byte) (type | (hat ? HAT_FLAG : 0) | ((lead & 7) << LEAD_SHIFT));
    }

    public static byte typeOf(byte g) {
        return (byte) (g & TYPE_MASK);
    }

    public static boolean hasHat(byte g) {
        return (g & HAT_FLAG) != 0;
    }

    /** Ticks antes de abrir los ojos (desfase natural). */
    public static int lead(byte g) {
        return ((g >> LEAD_SHIFT) & 7) * 2;
    }

    public static int length(byte g) {
        int hat = hasHat(g) ? HAT_EXTRA : 0;
        return switch (typeOf(g)) {
            case SETTLE -> SETTLE_LENGTH;
            case STRETCH_SEATED -> lead(g) + SEATED_BASE + hat;
            case STRETCH_STAND -> STAND_BASE + hat;
            default -> 0;
        };
    }

    /** Tick (contando el desfase) del bostezo: el servidor pone el sonidito. */
    public static int yawnTick(byte g) {
        return switch (typeOf(g)) {
            case STRETCH_SEATED -> lead(g) + SEATED_YAWN_TICK;
            case STRETCH_STAND -> STAND_YAWN_TICK;
            default -> -1;
        };
    }

    // ------------------------------------------------------------------ reloj del cliente

    /** @param startAt tick en el que arranca (en la pecera, el acomodarse empieza más adelante) */
    public void tick(byte serverGesture, int startAt) {
        this.weightO = this.weight;
        if (serverGesture != NONE && serverGesture != this.gesture) {
            this.gesture = serverGesture;
            this.t = startAt;
            this.fading = false;
            this.weight = 1.0F;
            this.weightO = 1.0F;
            return;
        }
        if (this.gesture == NONE) return;
        if (serverGesture == NONE || this.fading) {
            if (!this.fading && this.t >= length(this.gesture) - 2) {
                this.reset();
                return;
            }
            this.fading = true;
            this.weight = Math.max(0.0F, this.weight - 1.0F / FADE_TICKS);
            if (this.weight <= 0.0F) this.reset();
            return;
        }
        if (this.t < length(this.gesture)) this.t++;
    }

    private void reset() {
        this.gesture = NONE;
        this.t = 0;
        this.fading = false;
        this.weight = 0.0F;
        this.weightO = 0.0F;
    }

    public boolean isBusy() {
        return this.gesture != NONE;
    }

    public byte getGesture() {
        return this.gesture;
    }

    public byte getType() {
        return typeOf(this.gesture);
    }

    public int getTick() {
        return this.t;
    }

    public float getTime(float pt) {
        if (this.gesture == NONE) return 0.0F;
        return this.fading ? this.t : Math.min(length(this.gesture), this.t + pt);
    }

    public float getWeight(float pt) {
        return Mth.lerp(pt, this.weightO, this.weight);
    }

    /** Preparándose de pie: la sentada de siempre todavía no empieza. */
    public boolean holdsStanding() {
        return !this.fading && this.getType() == SETTLE && this.t < SETTLE_SIT_TICK;
    }

    /** Estirándose sentado: todavía no se levanta. */
    public boolean holdsSeated() {
        return !this.fading && this.getType() == STRETCH_SEATED && this.t < lead(this.gesture) + SEATED_STAND_TICK;
    }

    /** Momento exacto en que se le revienta el globito de sueño al abrir los ojos. */
    public boolean bubblePopsNow() {
        return !this.fading && this.getType() == STRETCH_SEATED && this.t == lead(this.gesture) + 7;
    }

    /**
     * Para el globito de sueño: el "descanso" equivalente mientras abre los ojos sentado, así el
     * globito hace su ¡plop! de siempre en ese momento (y no al pararse). -1 = no cambia nada.
     */
    public float bubbleRest(float pt) {
        if (this.getType() != STRETCH_SEATED || this.fading) return -1.0F;
        float u = this.getTime(pt) - lead(this.gesture);
        return 1.0F - 0.4F * Mth.clamp(u / 9.0F, 0.0F, 1.0F);
    }

    // ------------------------------------------------------------------ ojitos

    public static final int EYES_DEFAULT = 0;
    public static final int EYES_CLOSED = 1;
    public static final int EYES_TIRED = 2;
    public static final int EYES_OPEN = 3;

    /** Ojitos del gesto: cerrados al dormir y al bostezar, entreabiertos al despertarse, abiertos al resto. */
    public static int eyes(byte g, float t) {
        switch (typeOf(g)) {
            case STRETCH_SEATED -> {
                float u = t - lead(g);
                if (u < 4.0F) return EYES_CLOSED;
                if (u < 14.0F) return EYES_TIRED;
                if (u >= 33.0F && u < 45.0F) return EYES_CLOSED;     // bostezo
                if (u >= 45.0F && u < 50.0F) return EYES_TIRED;
                return EYES_OPEN;
            }
            case STRETCH_STAND -> {
                if (t >= 23.0F && t < 36.0F) return EYES_CLOSED;
                if (t < 6.0F) return EYES_TIRED;
                return EYES_OPEN;
            }
            default -> {
                return EYES_DEFAULT;
            }
        }
    }
}
