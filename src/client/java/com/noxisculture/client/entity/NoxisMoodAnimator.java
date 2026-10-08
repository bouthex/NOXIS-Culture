package com.noxisculture.client.entity;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Animaciones de emoción compartidas por todos los modelos Noxis
 * (aldeano, guerrero, sabio). Cada modelo le pasa sus propias piezas.
 *
 *  Neutral: orejas con tics independientes, sombrero que se balancea.
 *  Feliz:   orejas erguidas que se menean, sombrero que salta, cola alta y rápida.
 *  Miedo:   "orejas de avión" (aplastadas hacia atrás como un gato real),
 *           sombrero hundido, cabeza que tiembla, cola escondida.
 */
public final class NoxisMoodAnimator {
    private NoxisMoodAnimator() {}

    private static final float EARS_FLAT_ANGLE = 1.25F;
    private static final float EARS_FLAT_BACK = 0.55F;

    public static void animateEars(ModelPart rightEar, ModelPart leftEar, float age,
                                   float happy, float scared, float baseAngle) {
        int t = (int) age;
        // Tics más seguidos (~cada 2 s) y con dos golpecitos, como un gato real.
        float twitchRight = ((t % 41) < 2 || (t % 41) == 4) ? 0.4F : 0.0F;
        float twitchLeft = (((t + 19) % 53) < 2 || ((t + 19) % 53) == 4) ? 0.4F : 0.0F;
        // Además, un vaivén suave constante para que nunca estén "quietas".
        float idleSway = Mth.sin(age * 0.13F) * 0.06F;
        float wiggle = Mth.sin(age * 0.6F) * 0.22F * happy;
        float happyAngle = baseAngle * 0.25F;

        float right = Mth.lerp(scared, Mth.lerp(happy, baseAngle + twitchRight + idleSway, happyAngle + wiggle), EARS_FLAT_ANGLE);
        float left = Mth.lerp(scared, Mth.lerp(happy, baseAngle + twitchLeft - idleSway, happyAngle - wiggle), EARS_FLAT_ANGLE);
        rightEar.zRot = -right;
        leftEar.zRot = left;
        rightEar.xRot = EARS_FLAT_BACK * scared;
        leftEar.xRot = EARS_FLAT_BACK * scared;
    }

    /**
     * Curiosidad repentina (compartida por todas las especies): ladea apenas la cabecita y
     * para la orejita del lado de arriba, con un par de golpecitos como si escuchara algo.
     * Se suma ENCIMA de la pose actual, así que con curious = 0 no cambia nada.
     */
    /**
     * Fascinación por un cristal (gesto aditivo, compartido por todas las especies): la cabeza
     * gira hacia el cristal y lo mira, se ladea con ternura, las dos orejitas se paran hacia
     * adelante y cada tanto una da un golpecito.
     */
    public static void applyFascination(ModelPart head, ModelPart rightEar, ModelPart leftEar,
                                        float amount, float yaw, float pitch, float side, float twitch) {
        if (amount <= 0.0F) return;
        head.yRot = Mth.lerp(amount, head.yRot, yaw);
        head.xRot = Mth.lerp(amount, head.xRot, pitch - 0.06F);   // apenas hacia arriba: "¡qué lindo!"
        head.zRot += 0.2F * amount * side;
        // Orejitas paradas y hacia adelante, atentas.
        rightEar.xRot -= 0.3F * amount;
        leftEar.xRot -= 0.3F * amount;
        rightEar.zRot += 0.15F * amount;
        leftEar.zRot -= 0.15F * amount;
        // Golpecito alternado (+ derecha, - izquierda).
        if (twitch > 0.0F) rightEar.zRot -= 0.35F * twitch;
        if (twitch < 0.0F) leftEar.zRot -= 0.35F * twitch;
    }

    public static void applyCuriosity(ModelPart head, ModelPart rightEar, ModelPart leftEar,
                                      float curious, float side, float flick) {
        if (curious <= 0.0F) {
            return;
        }
        head.zRot += 0.22F * curious * side;   // ladea la cabecita
        head.xRot -= 0.05F * curious;          // la levanta un pelín, atento
        // La orejita del lado de arriba se para (más derecha) y hace los golpecitos.
        ModelPart listening = side > 0.0F ? rightEar : leftEar;
        float out = side > 0.0F ? -1.0F : 1.0F;     // dirección "hacia afuera" de esa oreja
        listening.zRot -= out * 0.18F * curious;      // se para
        listening.zRot += out * 0.35F * flick;        // golpecito
        listening.xRot -= 0.12F * curious;            // apunta un poquito hacia adelante
    }

    public static void animateHat(ModelPart hat, float baseY, float baseTilt, float age,
                                  float walkPos, float walkAmount, float happy, float scared) {
        hat.zRot = baseTilt
                + Mth.sin(age * 0.09F) * 0.04F * (1.0F - scared)
                + Mth.cos(walkPos * 1.2F) * 0.08F * walkAmount
                + Mth.sin(age * 0.5F) * 0.07F * happy;
        hat.y = baseY - Math.abs(Mth.sin(age * 0.35F)) * 1.2F * happy + 1.0F * scared;
    }

    public static void animateTail(ModelPart tail, float baseAngle, float age, float happy, float scared) {
        // Feliz: rebota DENTRO de su rango normal (nunca sube más), así no llega a la nuca.
        float happyBounce = (-0.08F + 0.08F * Math.abs(Mth.sin(age * 0.6F))) * happy;
        tail.xRot = Mth.lerp(scared, baseAngle + happyBounce, -0.25F);
        tail.yRot = Mth.sin(age * (0.15F + 0.35F * happy)) * 0.45F * (1.0F - scared);
    }

    /**
     * Caminata "cartoon antiguo": se bambolea de lado a lado y sube/baja en cada paso.
     * Devuelve [inclinación lateral (rad), rebote vertical (px)].
     * Mientras más feliz, más saltarín.
     */
    public static float[] waddle(float walkPos, float walkAmount, float happy) {
        float step = walkPos * 0.6F;
        float roll = Mth.sin(step) * 0.14F * walkAmount;
        float bounce = Math.abs(Mth.cos(step)) * (1.1F + 0.6F * happy) * walkAmount;
        return new float[] {roll, bounce};
    }

    /** Cabecita viva: asiente al caminar y se mece de felicidad. Devuelve [xRot extra, zRot extra]. */
    public static float[] headLife(float age, float walkPos, float walkAmount, float happy) {
        float nod = Mth.sin(walkPos * 1.2F) * 0.12F * walkAmount + Mth.sin(age * 0.35F) * 0.1F * happy;
        float tilt = Mth.sin(age * 0.18F) * 0.18F * happy;
        return new float[] {nod, tilt};
    }

    /** Temblor de cabeza (miedo). */
    public static float headTremble(float age, float scared) {
        return Mth.sin(age * 2.6F) * 0.05F * scared;
    }
}
