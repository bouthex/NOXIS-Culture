package com.noxisculture.entity.idle;

import net.minecraft.util.Mth;

/**
 * Saltito para entrar y salir de la pecera: la trayectoria (servidor) y la pose (cliente).
 *
 * <p>El servidor mueve al Noxis tick a tick por un camino simple y controlado (sin física):
 * <b>entrar</b> = sube derecho al lado de la pecera hasta pasar el borde, avanza por encima de
 * la abertura y cae adentro; <b>salir</b> = se agacha, sube derecho por la abertura, pasa por
 * encima del borde y baja afuera. Con el byte {@link #PREP}/{@link #IN}/{@link #OUT} el cliente
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
    /** Duración de cada saltito (entrar o salir). */
    public static final int HOP_TICKS = 18;
    /** Punto más alto: los pies pasan por encima del borde de vidrio (21 px). */
    public static final double APEX = 24.0D / 16.0D;

    private byte anim = NONE;
    private int t;

    // ------------------------------------------------------------------ servidor: trayectoria

    /** Avance horizontal (0 = donde empezó, 1 = destino) en la fracción {@code f} del saltito. */
    public static float horizontal(byte type, float f) {
        return type == IN ? NoxisHatAnimation.smooth((f - 0.25F) / 0.35F)
                : NoxisHatAnimation.smooth((f - 0.45F) / 0.3F);
    }

    /** Altura en la fracción {@code f}: sube derecho, se mantiene arriba del borde y cae. */
    public static double height(byte type, float f, double fromY, double apexY, double toY) {
        float riseStart = type == IN ? 0.0F : 0.12F;      // al salir, primero se agacha adentro
        float riseEnd = type == IN ? 0.35F : 0.45F;
        float fallStart = type == IN ? 0.6F : 0.72F;
        if (f < riseStart) return fromY;
        if (f < riseEnd) {
            float x = (f - riseStart) / (riseEnd - riseStart);
            return Mth.lerp(1.0F - (1.0F - x) * (1.0F - x), fromY, apexY);
        }
        if (f < fallStart) return apexY;
        float d = Math.min(1.0F, (f - fallStart) / (1.0F - fallStart));
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
                arms = window(f, 0.05F, 0.55F);
                squeeze = window(f, 0.55F, 0.97F);                             // pasa por el cuellito
            } else {
                crouch = window(f, 0.0F, 0.14F);
                tuck = window(f, 0.16F, 0.92F);
                arms = window(f, 0.12F, 0.6F);
                squeeze = window(f, 0.1F, 0.5F);
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
