package com.noxisculture.entity.idle;

import net.minecraft.util.Mth;

/**
 * Gesto propio de cada personalidad (solo visual, cliente; el servidor dice cuál y cuándo):
 *
 * <ul>
 *   <li>{@link #CURIOUS}: sigue con la mirada algo que flota (arriba, abajo, de lado a lado),
 *   ladea mucho la cabeza y mueve una orejita para adelante y otra para atrás. "¿mrr?"</li>
 *   <li>{@link #MISCHIEF}: se frota las patitas con picardía, con los hombritos arriba, mirando
 *   rápido a un lado y al otro como planeando algo. "jejeje"</li>
 *   <li>{@link #ARTIST}: encuadra algo con las dos patitas como un pintor y después, soñador, pinta
 *   pinceladas en el aire mirando para arriba. Tarareo.</li>
 *   <li>{@link #AFFECTION}: saltito, abre los bracitos bien grandes y después llama con la patita
 *   ("vení"), con la colita contenta. "¡eeey!"</li>
 *   <li>{@link #CALM}: se balancea relajado, respira hondo, afloja los hombros y cierra los
 *   ojitos contento con la colita enroscada. Ronroneo.</li>
 * </ul>
 */
public final class NoxisPersonaAnimation {
    public static final byte NONE = 0;
    public static final byte CURIOUS = 1;
    public static final byte MISCHIEF = 2;
    public static final byte ARTIST = 3;
    public static final byte AFFECTION = 4;
    public static final byte CALM = 5;
    private static final float FADE_TICKS = 5.0F;

    private byte anim = NONE;
    private int t;
    private boolean fading;
    private float weight;
    private float weightO;

    public static int length(byte g) {
        return switch (g) {
            case CURIOUS -> 64;
            case MISCHIEF -> 70;
            case ARTIST -> 84;
            case AFFECTION -> 60;
            case CALM -> 80;
            default -> 0;
        };
    }

    /** Tick en que vocaliza (el servidor pone el sonido). */
    public static int voiceTick(byte g) {
        return switch (g) {
            case CURIOUS -> 40;
            case MISCHIEF -> 24;
            case ARTIST -> 34;
            case AFFECTION -> 10;
            case CALM -> 30;
            default -> -1;
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

    // ------------------------------------------------------------------ ojitos

    public static boolean bigPupils(byte g, float t) {
        return (g == CURIOUS && t >= 4.0F && t < 58.0F) || (g == AFFECTION && t >= 2.0F && t < 28.0F);
    }

    /** Entrecerrados: picardía (travieso) o mirada soñadora (artista). */
    public static boolean halfClosed(byte g, float t) {
        return (g == MISCHIEF && t >= 6.0F && t < 62.0F) || (g == ARTIST && t >= 8.0F && t < 28.0F)
                || (g == ARTIST && t >= 34.0F && t < 74.0F);
    }

    public static boolean closed(byte g, float t) {
        return (g == AFFECTION && t >= 30.0F && t < 42.0F) || (g == CALM && t >= 40.0F && t < 72.0F);
    }
}
