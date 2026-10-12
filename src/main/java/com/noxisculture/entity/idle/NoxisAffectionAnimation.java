package com.noxisculture.entity.idle;

import net.minecraft.util.Mth;

/**
 * Gestos de cariño entre dos amigos (solo visual, cliente): tierno e inocente, siempre uno AL
 * LADO del otro (mirando para el mismo lado), nunca cara a cara. El servidor elige uno de tres
 * gestos y se lo pone a los DOS en el mismo tick, cada uno con su papel ({@link #LEAD}: el que
 * vino; {@link #PARTNER}: el que lo recibe) y de qué lado tiene al amigo. Así la coreografía es
 * una sola y compartida, nunca dos animaciones sueltas.
 *
 * <ul>
 *   <li>{@link #HANDS} — <b>agarrarse de las manitos</b>: se miran, estiran el bracito de adentro
 *   y se dan la mano; balancean las manitos juntas, se sueltan y se miran contentos.</li>
 *   <li>{@link #CHEEK} — <b>frotar las cabecitas de costado</b>: se inclinan apenas y apoyan un
 *   costado de la cabeza contra el del otro un instante (las caras miran adelante).</li>
 *   <li>{@link #HUG} — <b>abrazo de amiguitos</b>: el que vino abre los bracitos y lo abraza por
 *   el costado; el otro para las orejitas y le devuelve el abrazo con su bracito.</li>
 * </ul>
 *
 * <p>Compartido por todas las especies Noxis (no depende de la ropa).</p>
 */
public final class NoxisAffectionAnimation {
    public static final byte NONE = 0;
    // Papel (bits 0-1)
    public static final byte LEAD = 1;
    public static final byte PARTNER = 2;
    // Gesto (bits 2-3)
    public static final byte HANDS = 1;
    public static final byte CHEEK = 2;
    public static final byte HUG = 3;
    /** Bit: el amigo está a SU derecha (si no, a su izquierda). */
    private static final int FRIEND_RIGHT = 0x10;

    public static final int HANDS_LENGTH = 96;
    public static final int CHEEK_LENGTH = 90;
    public static final int HUG_LENGTH = 96;
    /** Distancia entre los dos (bloques), uno al lado del otro, según el gesto. */
    public static double distance(byte gesture) {
        return switch (gesture) {
            case HANDS -> 0.85D;
            case CHEEK -> 0.8D;
            default -> 0.78D;
        };
    }
    /** Tick del servidor para el corazoncito y el soniditito contento. */
    public static int heartTick(byte gesture) {
        return switch (gesture) {
            case HANDS -> 40;
            case CHEEK -> 42;
            default -> 46;
        };
    }
    private static final float FADE_TICKS = 5.0F;

    private byte anim = NONE;
    private int t;
    private boolean fading;
    private float weight;
    private float weightO;

    public static byte encode(byte role, byte gesture, boolean friendOnRight) {
        return (byte) (role | (gesture << 2) | (friendOnRight ? FRIEND_RIGHT : 0));
    }

    public static byte roleOf(byte anim) {
        return (byte) (anim & 0x03);
    }

    public static byte gestureOf(byte anim) {
        return (byte) ((anim >> 2) & 0x03);
    }

    public static boolean friendOnRight(byte anim) {
        return (anim & FRIEND_RIGHT) != 0;
    }

    public static int length(byte anim) {
        return switch (gestureOf(anim)) {
            case HANDS -> HANDS_LENGTH;
            case CHEEK -> CHEEK_LENGTH;
            case HUG -> HUG_LENGTH;
            default -> 0;
        };
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
            this.fading = true;                      // lo cortó algo más importante: vuelve suave
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

    public boolean isBusy() {
        return this.anim != NONE;
    }

    public byte getAnim() {
        return this.anim;
    }

    public float getTime(float pt) {
        if (this.anim == NONE) return 0.0F;
        return this.fading ? this.t : Math.min(length(this.anim), this.t + pt);
    }

    public float getWeight(float pt) {
        return Mth.lerp(pt, this.weightO, this.weight);
    }

    /** Ojitos cerrados de contento (un momento, en el punto más tierno de cada gesto). */
    public static boolean eyesClosed(byte anim, float t) {
        return switch (gestureOf(anim)) {
            case HANDS -> t >= 74.0F && t < 79.0F;
            case CHEEK -> t >= 36.0F && t < 52.0F;
            case HUG -> t >= 40.0F && t < 54.0F;
            default -> false;
        };
    }

    /** Pupilas grandes de cariño al mirarse. */
    public static boolean bigPupils(byte anim, float t) {
        return switch (gestureOf(anim)) {
            case HANDS -> (t >= 2.0F && t < 20.0F) || (t >= 64.0F && t < 74.0F);
            case CHEEK -> (t >= 2.0F && t < 14.0F) || (t >= 66.0F && t < 84.0F);
            case HUG -> (t >= 2.0F && t < 12.0F) || (t >= 66.0F && t < 88.0F);
            default -> false;
        };
    }
}
