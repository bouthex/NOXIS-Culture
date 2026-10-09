package com.noxisculture.entity.idle;

import com.noxisculture.entity.nature.NoxisNatureAction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Animación de las interacciones con la naturaleza (solo visual, cliente). El servidor decide
 * QUÉ hace (byte {@link NoxisNatureAction}); acá se arma CÓMO se ve, con transiciones suaves:
 * agacharse a recoger, sostener la flor, acercarla a la carita, olerla con los ojitos cerrados,
 * orejitas que se mueven, cabecita ladeada, pupilas mirando la flor y sentarse a contemplar.
 *
 * <p>Compartido por todas las especies Noxis (no depende de la ropa).</p>
 */
public final class NoxisNatureAnimation {
    private byte action = NoxisNatureAction.NONE;
    private int t;
    private float side = 1.0F;

    // Valores actuales y del tick anterior (para interpolar entre frames).
    private final float[] now = new float[CHANNELS];
    private final float[] old = new float[CHANNELS];

    private static final int SURVEY = 0;     // mira alrededor decidiendo
    private static final int LEAN = 1;       // agachado (recoger / plantar / olfatear)
    private static final int HOLD = 2;       // sostiene la flor
    private static final int RAISE = 3;      // 0 = a la altura del pecho, 1 = junto a la nariz
    private static final int SNIFF = 4;      // olfateo (nariz que se mueve)
    private static final int EYES = 5;       // ojitos cerrados disfrutando
    private static final int EARS = 6;       // orejitas que se mueven suave
    private static final int TILT = 7;       // cabecita ladeada con ternura
    private static final int PUPILS = 8;     // pupilas mirando la flor
    private static final int SIT = 9;        // sentado contemplando
    private static final int CHANNELS = 10;

    public void tick(byte serverAction, boolean holdingFlower, RandomSource random) {
        System.arraycopy(this.now, 0, this.old, 0, CHANNELS);
        if (serverAction != this.action) {
            this.action = serverAction;
            this.t = 0;
            this.side = random.nextBoolean() ? 1.0F : -1.0F;
        } else {
            this.t++;
        }
        float[] target = new float[CHANNELS];
        int t = this.t;
        switch (this.action) {
            case NoxisNatureAction.SURVEY -> {
                target[SURVEY] = 1.0F;
                target[EARS] = 0.4F;
            }
            case NoxisNatureAction.PICK, NoxisNatureAction.REPLANT -> {
                target[LEAN] = 1.0F;
                target[TILT] = 0.4F;
            }
            case NoxisNatureAction.ADMIRE -> {
                // 0-12: sube la flor · 12-60: la contempla · 60-72: la acerca a la nariz
                // 72-100: la huele con los ojitos cerrados · después vuelve a contemplarla.
                target[RAISE] = t < 60 ? 0.55F : t < 100 ? 1.0F : 0.55F;
                target[TILT] = 1.0F;
                target[PUPILS] = (t > 8 && t < 66) || t > 104 ? 1.0F : 0.0F;
                target[SNIFF] = t >= 70 && t < 100 ? 1.0F : 0.0F;
                target[EYES] = t >= 74 && t < 97 ? 1.0F : 0.0F;
                target[EARS] = t >= 66 && t < 104 ? 1.0F : 0.35F;
            }
            case NoxisNatureAction.LAST_LOOK -> {
                target[TILT] = 0.7F;
                target[EARS] = 0.3F;
                target[PUPILS] = 1.0F;
            }
            case NoxisNatureAction.SNIFF -> {
                target[LEAN] = t < 56 ? 0.75F : 0.0F;
                target[TILT] = 0.6F;
                target[SNIFF] = t >= 8 && t < 52 ? 1.0F : 0.0F;
                target[EYES] = t >= 20 && t < 40 ? 1.0F : 0.0F;
                target[EARS] = t >= 8 && t < 56 ? 1.0F : 0.0F;
            }
            case NoxisNatureAction.SIT -> {
                target[SIT] = 1.0F;
                target[EARS] = 0.3F;
                // De vez en cuando cierra los ojitos un instante, disfrutando del lugar.
                target[EYES] = (t % 110) > 90 ? 1.0F : 0.0F;
            }
            default -> { }
        }
        if (holdingFlower) target[HOLD] = 1.0F;

        for (int i = 0; i < CHANNELS; i++) {
            // Ojos y pupilas cambian rápido (son píxeles); el resto, suave.
            float speed = i == EYES || i == PUPILS ? 0.5F : i == SIT ? 0.08F : 0.15F;
            this.now[i] = Mth.approach(this.now[i], target[i], speed);
        }
    }

    /** ¿Hay alguna pose de naturaleza en curso (aunque sea terminando)? */
    public boolean isBusy() {
        if (this.action != NoxisNatureAction.NONE) return true;
        for (float v : this.now) if (v > 0.01F) return true;
        return false;
    }

    private float get(int channel, float pt) {
        return Mth.lerp(pt, this.old[channel], this.now[channel]);
    }

    public float getSurvey(float pt) { return this.get(SURVEY, pt); }
    public float getLean(float pt) { return this.get(LEAN, pt); }
    public float getHold(float pt) { return this.get(HOLD, pt); }
    public float getRaise(float pt) { return this.get(RAISE, pt); }
    public float getSniff(float pt) { return this.get(SNIFF, pt); }
    public float getEyesClosed(float pt) { return this.get(EYES, pt); }
    public float getEars(float pt) { return this.get(EARS, pt); }
    public float getTilt(float pt) { return this.get(TILT, pt); }
    public float getPupils(float pt) { return this.get(PUPILS, pt); }
    public float getSit(float pt) { return this.get(SIT, pt); }

    /** +1 o -1: hacia qué lado ladea la cabecita esta vez. */
    public float getSide() { return this.side; }
}
