package com.noxisculture.entity.idle;

import com.noxisculture.entity.social.NoxisSocialAction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Animación de las interacciones entre compañeros (solo visual, cliente): el saludo y el
 * descanso en compañía. El servidor decide QUÉ gesto toca (byte {@link NoxisSocialAction}) y
 * cuándo bostezan; acá se arma cómo se ve, con transiciones suaves. La postura sentada es la
 * del descanso de siempre: esto solo suma la inclinación hacia el compañero, el cabeceo y los
 * bostezos. Compartido por todas las especies Noxis (no depende de la ropa).
 */
public final class NoxisSocialAnimation {
    private static final int YAWN_LENGTH = 26;

    private byte anim = NoxisSocialAction.NONE;
    private int t;
    private float side = 1.0F;
    private int yawnT = -1;

    private float tilt, tiltO;
    private float ears, earsO;
    private float wave, waveO;
    private float nod, nodO;
    private float hop, hopO;
    private float doze, dozeO;
    private float yawn, yawnO;
    private float lean, leanO;

    public void startYawn() {
        this.yawnT = 0;
    }

    public void tick(byte serverAnim, RandomSource random) {
        this.tiltO = this.tilt;
        this.earsO = this.ears;
        this.waveO = this.wave;
        this.nodO = this.nod;
        this.hopO = this.hop;
        this.dozeO = this.doze;
        this.yawnO = this.yawn;
        this.leanO = this.lean;
        if (serverAnim != this.anim) {
            this.anim = serverAnim;
            this.t = 0;
            this.side = random.nextBoolean() ? 1.0F : -1.0F;
        } else {
            this.t++;
        }
        int t = this.t;
        float tiltT = 0.0F, earsT = 0.0F, waveT = 0.0F, nodT = 0.0F, dozeT = 0.0F, leanT = 0.0F;
        float hopNow = 0.0F;
        switch (this.anim) {
            case NoxisSocialAction.GREET_WAVE, NoxisSocialAction.GREET_NOD, NoxisSocialAction.GREET_NOD_HOP -> {
                // Se miran, ladean la cabecita y mueven las orejas; después el gesto amistoso.
                tiltT = window(t, 6, 50) * 0.55F;
                earsT = window(t, 2, 54);
                if (this.anim == NoxisSocialAction.GREET_WAVE) {
                    waveT = window(t, 12, 36);
                } else {
                    nodT = window(t, 12, 30) + 0.6F * window(t, 42, 50);
                }
                if (this.anim == NoxisSocialAction.GREET_NOD_HOP) {
                    int p = t - 34;
                    if (p >= 0 && p < 2) hopNow = -0.3F;
                    else if (p >= 2 && p < 9) {
                        float x = (p - 1) / 8.0F;
                        hopNow = 0.55F * 4.0F * x * (1.0F - x);
                    } else if (p >= 9 && p < 11) hopNow = -0.25F;
                }
            }
            case NoxisSocialAction.REST_DROWSY -> dozeT = 1.0F;
            case NoxisSocialAction.REST_WATCH -> earsT = 0.4F;
            case NoxisSocialAction.REST_LEAN_RIGHT -> {
                leanT = t > 14 ? 1.0F : 0.0F;            // primero se sienta, después se apoya
                earsT = 0.6F;
            }
            case NoxisSocialAction.REST_LEAN_LEFT -> {
                leanT = t > 14 ? -1.0F : 0.0F;
                earsT = 0.6F;
            }
            default -> { }
        }
        this.tilt = Mth.approach(this.tilt, tiltT, 0.12F);
        this.ears = Mth.approach(this.ears, earsT, 0.1F);
        this.wave = Mth.approach(this.wave, waveT, 0.15F);
        this.nod = Mth.approach(this.nod, nodT, 0.2F);
        this.doze = Mth.approach(this.doze, dozeT, 0.08F);
        this.lean = Mth.approach(this.lean, leanT, 0.06F);
        this.hop = hopNow;
        // Bostezo: estira la cabecita para atrás con los ojitos cerrados.
        if (this.yawnT >= 0) {
            this.yawnT++;
            float x = this.yawnT / (float) YAWN_LENGTH;
            this.yawn = x < 0.25F ? x / 0.25F : x < 0.7F ? 1.0F : Math.max(0.0F, 1.0F - (x - 0.7F) / 0.3F);
            if (this.yawnT >= YAWN_LENGTH) {
                this.yawnT = -1;
                this.yawn = 0.0F;
            }
        }
    }

    private static float window(int t, int from, int to) {
        if (t < from || t > to + 8) return 0.0F;
        if (t > to) return 1.0F - (t - to) / 8.0F;
        return Math.min(1.0F, (t - from) / 6.0F);
    }

    /** ¿Hay algún gesto social en curso (aunque sea terminando)? */
    public boolean isBusy() {
        return this.anim != NoxisSocialAction.NONE || this.yawnT >= 0 || Math.abs(this.lean) > 0.01F
                || this.tilt > 0.01F || this.ears > 0.01F || this.doze > 0.01F;
    }

    public float getTilt(float pt) { return Mth.lerp(pt, this.tiltO, this.tilt); }
    public float getEars(float pt) { return Mth.lerp(pt, this.earsO, this.ears); }
    public float getWave(float pt) { return Mth.lerp(pt, this.waveO, this.wave); }
    public float getNod(float pt) { return Mth.lerp(pt, this.nodO, this.nod); }
    public float getHop(float pt) { return Mth.lerp(pt, this.hopO, this.hop); }
    public float getDoze(float pt) { return Mth.lerp(pt, this.dozeO, this.doze); }
    public float getYawn(float pt) { return Mth.lerp(pt, this.yawnO, this.yawn); }
    /** -1..1: apoyado en el compañero (+ = el compañero está a su derecha). */
    public float getLean(float pt) { return Mth.lerp(pt, this.leanO, this.lean); }
    public float getSide() { return this.side; }
}
