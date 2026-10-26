package com.noxisculture.entity.idle;

import net.minecraft.util.Mth;

/**
 * Saltito para entrar y salir de la pecera: la trayectoria (servidor) y la pose (cliente).
 *
 * <p>El servidor mueve al Noxis tick a tick por un camino simple y controlado (sin física):
 * una parábola de salto (sube, frena arriba y cae, sin quedarse flotando). <b>Entrar</b> = salta
 * desde el lado de la pecera, pasa por encima del borde y cae adentro; <b>salir</b> = se agacha,
 * salta por la abertura, pasa por encima del borde y baja afuera. Con el byte {@link #PREP}/{@link #IN}/{@link #OUT} el cliente
 * arma la pose: se agacha preparándose, recoge las patitas en el aire, levanta los bracitos, se
 * "aprieta" (más angostito) al pasar por el cuellito de la pecera y aterriza con un rebotito.</p>
 *
 * <p>Compartido por todas las especies Noxis (no depende de la ropa).</p>
 */
public final class NoxisBowlHop {
    public static final byte NONE = 0;
    public static final byte PREP = 1;
    public static final byte IN = 2;
    public static final byte OUT = 3;

    /** Se agacha preparándose antes de saltar adentro. */
    public static final int PREP_TICKS = 8;
    /** Duración de cada saltito (entrar o salir): corto, como un salto de verdad. */
    public static final int HOP_TICKS = 14;
    /** Punto más alto: los pies pasan por encima del borde de vidrio (21 px). */
    public static final double APEX = 25.0D / 16.0D;
    /** Momento del punto más alto (entrar); al salir es el espejo. */
    private static final float PEAK_IN = 0.42F;

    private byte anim = NONE;
    private int t;

    // ------------------------------------------------------------------ servidor: trayectoria

    /**
     * Avance horizontal (0 = donde empezó, 1 = destino) en la fracción {@code f} del saltito.
     * Entrando, empieza a avanzar recién cuando ya subió lo suficiente para pasar el borde;
     * saliendo, termina de avanzar antes de bajar del borde (nunca cruza el vidrio).
     */
    public static float horizontal(byte type, float f) {
        return type == IN ? NoxisHatAnimation.smooth((f - 0.25F) / 0.4F)
                : NoxisHatAnimation.smooth((f - 0.35F) / 0.4F);
    }

    /**
     * Altura en la fracción {@code f}: una parábola de salto de verdad (sube rápido, frena
     * arriba sin quedarse flotando y cae acelerando), con el punto más alto por encima del borde.
     */
    public static double height(byte type, float f, double fromY, double apexY, double toY) {
        float peak = type == IN ? PEAK_IN : 1.0F - PEAK_IN;
        if (f < peak) {
            float x = 1.0F - f / peak;
            return Mth.lerp(1.0F - x * x, fromY, apexY);
        }
        float d = Math.min(1.0F, (f - peak) / (1.0F - peak));
        return Mth.lerp(d * d, apexY, toY);
    }

    // ------------------------------------------------------------------ cliente: pose

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
     * Curvas de la pose: {agachado, patitas recogidas, apretadito, bracitos arriba, rebote}.
     * Todas van de 0 a 1.
     */
    public static float[] curves(byte type, float ticks) {
        float crouch = 0.0F, tuck = 0.0F, squeeze = 0.0F, arms = 0.0F, land = 0.0F;
        if (type == PREP) {
            crouch = NoxisHatAnimation.smooth(ticks / 4.0F);
        } else if (type == IN || type == OUT) {
            float f = ticks / HOP_TICKS;
            if (type == IN) {
                crouch = 1.0F - NoxisHatAnimation.smooth(f / 0.12F);           // ¡boing! se suelta
                tuck = window(f, 0.08F, 0.9F);
                arms = window(f, 0.06F, 0.5F);
                squeeze = window(f, 0.58F, 0.97F);                             // pasa por el cuellito
            } else {
                crouch = window(f, 0.0F, 0.1F);
                tuck = window(f, 0.12F, 0.92F);
                arms = window(f, 0.08F, 0.55F);
                squeeze = window(f, 0.04F, 0.42F);
            }
            if (f > 0.9F) land = Mth.sin((f - 0.9F) / 0.1F * Mth.PI);
        }
        return new float[] {crouch, tuck, squeeze, arms, land};
    }

    /** Sube suave al entrar en [a, b] y baja suave al salir (rampas de 0.08). */
    private static float window(float f, float a, float b) {
        float in = NoxisHatAnimation.smooth((f - a) / 0.08F);
        float out = 1.0F - NoxisHatAnimation.smooth((f - b) / 0.08F);
        return Math.min(in, out);
    }
}
