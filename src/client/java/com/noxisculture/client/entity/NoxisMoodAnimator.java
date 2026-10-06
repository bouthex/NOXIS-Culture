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
