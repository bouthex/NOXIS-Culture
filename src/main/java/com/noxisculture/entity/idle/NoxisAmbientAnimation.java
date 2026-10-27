package com.noxisculture.entity.idle;

import net.minecraft.util.Mth;

/**
 * Momentos tranquilos de la etapa 2 (solo visual, cliente; el servidor manda un byte sincronizado):
 * contemplar el cielo, compartir un rato con un amigo, descubrir algo, preocuparse por un compañero,
 * pintar y dejar las obras, y mirar una pintura. Cada estado tiene su peso, que sube y baja suave
 * (así se pasa de uno a otro sin saltos), y su propio tiempo.
 *
 * <p>Byte: estado en los bits 0-5 y, en los momentos de a dos, el bit {@link #PARTNER_RIGHT} dice
 * de qué lado está el otro.</p>
 *
 * <p>Compartido por todas las especies Noxis (con capa o sin ella: las poses solo mueven cabeza,
 * orejas, bracitos, cuerpo y colita).</p>
 */
public final class NoxisAmbientAnimation {
    public static final byte NONE = 0;
    // Cielo
    public static final byte SKY_STAND = 1;
    public static final byte SKY_SIT = 2;
    // Compartir un momento de tranquilidad
    public static final byte QUIET_SIT = 3;
    public static final byte QUIET_STAND = 4;
    // Descubrir
    public static final byte DISCOVER_LOOK = 5;
    public static final byte DISCOVER_LOW = 6;
    public static final byte DISCOVER_SURPRISE = 7;
    public static final byte DISCOVER_SCAN = 8;
    // Preocuparse por un compañero
    public static final byte CONCERN_WATCH = 9;
    public static final byte CONCERN_SIT = 10;
    public static final byte CONCERN_STAND = 11;
    public static final byte CONCERN_RECEIVE = 12;
    // Pintar
    public static final byte PAINT_LOOK = 13;
    public static final byte PAINT_SETUP = 14;
    public static final byte PAINT_STROKE = 15;
    public static final byte PAINT_STEPBACK = 16;
    public static final byte PAINT_RETOUCH = 17;
    public static final byte PAINT_ADMIRE = 18;
    public static final byte PAINT_PACK = 19;
    // Dejar una obra o un documento / mirar una pintura
    public static final byte PLACE_ITEM = 20;
    public static final byte GAZE_ART = 21;
    public static final int COUNT = 22;

    /** Bit: el compañero está a SU derecha. */
    public static final int PARTNER_RIGHT = 0x40;
    private static final int STATE_MASK = 0x3F;

    /** Largos (ticks) de los gestos que tienen duración fija. */
    public static final int SURPRISE_LENGTH = 16;
    public static final int SETUP_LENGTH = 40;
    public static final int PACK_LENGTH = 36;
    public static final int PLACE_LENGTH = 30;

    private byte raw = NONE;
    private byte state = NONE;
    private boolean partnerRight;
    private final float[] weight = new float[COUNT];
    private final float[] weightO = new float[COUNT];
    private final int[] time = new int[COUNT];

    public static byte encode(byte state, boolean partnerRight) {
        return (byte) (state | (partnerRight ? PARTNER_RIGHT : 0));
    }

    public static byte stateOf(byte raw) {
        int s = raw & STATE_MASK;
        return s < COUNT ? (byte) s : NONE;
    }

    public static boolean partnerRight(byte raw) {
        return (raw & PARTNER_RIGHT) != 0;
    }

    /** ¿Es una postura sentada? (para la postura de sentarse que ya existe). */
    public static boolean isSitting(byte s) {
        return s == SKY_SIT || s == QUIET_SIT || s == CONCERN_SIT;
    }

    /** ¿Está con el caballete armado? */
    public static boolean isPainting(byte s) {
        return s >= PAINT_SETUP && s <= PAINT_PACK;
    }

    public void tick(byte serverRaw) {
        byte s = stateOf(serverRaw);
        if (serverRaw != this.raw) {
            if (s != this.state) this.time[s] = 0;     // cada estado arranca su coreografía de cero
            this.raw = serverRaw;
            this.state = s;
            this.partnerRight = partnerRight(serverRaw);
        }
        for (int i = 1; i < COUNT; i++) {
            this.weightO[i] = this.weight[i];
            boolean on = i == this.state;
            float in = i == DISCOVER_SURPRISE ? 0.5F : isSitting((byte) i) ? 0.07F : 0.12F;
            float out = isSitting((byte) i) ? 0.07F : 0.12F;
            this.weight[i] = on ? Math.min(1.0F, this.weight[i] + in) : Math.max(0.0F, this.weight[i] - out);
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

    public boolean isPartnerRight() {
        return this.partnerRight;
    }

    public float getWeight(int s, float pt) {
        return Mth.lerp(pt, this.weightO[s], this.weight[s]);
    }

    public float getTime(int s, float pt) {
        return this.time[s] + pt;
    }

    /** Cuánto está sentado (0-1), sumando las posturas sentadas. */
    public float getSit(float pt) {
        return Math.min(1.0F, this.getWeight(SKY_SIT, pt) + this.getWeight(QUIET_SIT, pt) + this.getWeight(CONCERN_SIT, pt));
    }
}
