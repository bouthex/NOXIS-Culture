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
        float twitchRight = (t % 90) < 3 ? 0.35F : 0.0F;
        float twitchLeft = ((t + 47) % 113) < 3 ? 0.35F : 0.0F;
        float wiggle = Mth.sin(age * 0.6F) * 0.18F * happy;
        float happyAngle = baseAngle * 0.25F;

        float right = Mth.lerp(scared, Mth.lerp(happy, baseAngle + twitchRight, happyAngle + wiggle), EARS_FLAT_ANGLE);
        float left = Mth.lerp(scared, Mth.lerp(happy, baseAngle + twitchLeft, happyAngle - wiggle), EARS_FLAT_ANGLE);
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
        tail.xRot = Mth.lerp(scared, baseAngle + 0.3F * happy, -0.25F);
        tail.yRot = Mth.sin(age * (0.15F + 0.35F * happy)) * 0.45F * (1.0F - scared);
    }

    /** Temblor de cabeza (miedo). */
    public static float headTremble(float age, float scared) {
        return Mth.sin(age * 2.6F) * 0.05F * scared;
    }
}
