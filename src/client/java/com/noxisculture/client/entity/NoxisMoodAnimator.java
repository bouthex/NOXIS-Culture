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
     * Fascinación por un cristal (gesto aditivo, compartido por todas las especies). La cabeza ya
     * mira el cristal (eso lo hace el servidor con su LookControl); acá se ladea con ternura, sube
     * un poquito el mentón ("¡qué lindo!"), las orejitas se paran hacia adelante y dan golpecitos.
     */
    public static void applyFascination(ModelPart head, ModelPart rightEar, ModelPart leftEar,
                                        float amount, float side, float twitch, float hop, float age) {
        if (amount <= 0.0F) return;
        // Cabecita ladeada con ternura, con un vaivén suave de felicidad (nunca rígida).
        head.zRot += (0.34F + 0.05F * Mth.sin(age * 0.18F)) * amount * side;
        head.xRot -= 0.1F * amount;
        // Rebote cartoon de la cabecita: se hunde al agacharse/aterrizar y se estira en el aire.
        head.xRot += 0.18F * Math.min(0.0F, hop) * -1.0F;
        // Orejitas bien paradas y hacia adelante, atentas.
        rightEar.xRot -= 0.35F * amount;
        leftEar.xRot -= 0.35F * amount;
        rightEar.zRot += 0.2F * amount;
        leftEar.zRot -= 0.2F * amount;
        // Golpecito alternado (+ derecha, - izquierda).
        if (twitch > 0.0F) rightEar.zRot -= 0.35F * twitch;
        if (twitch < 0.0F) leftEar.zRot -= 0.35F * twitch;
        // Orejitas expresivas: se mueven suave, cada una a su ritmo, mientras dura la fascinación.
        rightEar.zRot += Mth.sin(age * 0.5F) * 0.1F * amount;
        leftEar.zRot += Mth.sin(age * 0.5F + 2.1F) * 0.1F * amount;
    }

    /**
     * Festejo de la fascinación: bracitos arriba y abiertos, sacudiéndose de alegría y la colita
     * moviéndose rápido de lado a lado (solo de costado: nunca sube hacia la nuca).
     * Los ángulos de entrada son los que ya calculó el modelo; {@code cheer} los mezcla.
     */
    public static void applyCheer(ModelPart rightArm, ModelPart leftArm, ModelPart tail, float cheer, float age) {
        if (cheer <= 0.0F) return;
        float shake = Mth.sin(age * 1.1F) * 0.32F;            // sacudón rápido, como un bailecito
        float bob = Mth.sin(age * 0.55F) * 0.15F;
        // Bien abiertos y hacia adelante (si suben más, la cabezota los tapa).
        rightArm.zRot = Mth.lerp(cheer, rightArm.zRot, 1.55F + shake);
        leftArm.zRot = Mth.lerp(cheer, leftArm.zRot, -1.55F + shake);
        rightArm.xRot = Mth.lerp(cheer, rightArm.xRot, -0.75F + bob);
        leftArm.xRot = Mth.lerp(cheer, leftArm.xRot, -0.75F - bob);
        tail.yRot = Mth.lerp(cheer, tail.yRot, Mth.sin(age * 0.9F) * 0.55F);
    }

    /**
     * Interacciones con la naturaleza (gesto aditivo, compartido por todas las especies): mira
     * alrededor eligiendo flor, se agacha a recogerla, contempla la flor ladeando la cabecita,
     * la olfatea moviendo la naricita y mueve las orejitas suave.
     * La dirección de la mirada la pone el servidor (mira la flor elegida).
     */
    public static void applyNature(ModelPart head, ModelPart body, ModelPart rightEar, ModelPart leftEar, float age,
                                   float survey, float lean, float hold, float raise, float sniff,
                                   float ears, float tilt, float side) {
        // Duda entre varias flores: pequeños giros de cabeza.
        head.yRot += Mth.sin(age * 0.22F) * 0.16F * survey;
        head.zRot += Mth.sin(age * 0.13F) * 0.08F * survey;
        // Se agacha con cuidado (recoger, plantar, olfatear).
        body.xRot += 0.35F * lean;
        head.xRot += 0.45F * lean;
        head.z -= 1.6F * lean;
        head.y += 1.0F * lean;
        // Contempla la flor que tiene en la mano: cabecita un poco baja y ladeada con ternura.
        float look = hold * (1.0F - lean);
        head.xRot += 0.25F * look - 0.12F * raise * look;
        head.zRot += 0.24F * tilt * side;
        // Olfatea: la naricita sube y baja rapidito.
        head.xRot += Mth.sin(age * 1.7F) * 0.045F * sniff;
        // Orejitas que se mueven suave, cada una a su ritmo.
        rightEar.zRot += Mth.sin(age * 0.7F) * 0.16F * ears;
        leftEar.zRot += Mth.sin(age * 0.7F + 1.9F) * 0.16F * ears;
        rightEar.xRot -= 0.15F * ears;
        leftEar.xRot -= 0.15F * ears;
    }

    /**
     * Brazos con la flor: estirarse hacia la flor al recogerla/plantarla, sostenerla a la altura
     * del pecho y acercarla a la carita para olerla (la otra manito acompaña, con cariño).
     */
    public static void applyFlowerArms(ModelPart rightArm, ModelPart leftArm, float lean, float hold, float raise,
                                       float offer) {
        rightArm.xRot = Mth.lerp(lean, rightArm.xRot, -1.2F);
        rightArm.zRot = Mth.lerp(lean, rightArm.zRot, 0.05F);
        float carry = hold * (1.0F - lean);
        rightArm.xRot = Mth.lerp(carry, rightArm.xRot, Mth.lerp(raise, -0.55F, -1.5F));
        rightArm.zRot = Mth.lerp(carry, rightArm.zRot, Mth.lerp(raise, 0.08F, -0.5F));
        float cup = carry * raise * 0.8F;
        leftArm.xRot = Mth.lerp(cup, leftArm.xRot, -1.1F);
        leftArm.zRot = Mth.lerp(cup, leftArm.zRot, 0.45F);
        // Regalo: estira el bracito hacia adelante (y un poquito arriba) ofreciendo la flor.
        rightArm.xRot = Mth.lerp(offer, rightArm.xRot, -1.75F);
        rightArm.zRot = Mth.lerp(offer, rightArm.zRot, -0.1F);
    }

    /**
     * Saludo y descanso en compañía, cabeza y cuerpo (gesto aditivo, compartido por todas las
     * especies). Ladeo de cabecita, orejas que se paran y se mueven, asentir, cabeceo de sueño,
     * bostezo y apoyarse en el compañero sentado al lado ({@code lean} + = está a su derecha).
     */
    public static void applySocialHead(ModelPart head, ModelPart body, ModelPart rightEar, ModelPart leftEar,
                                       float age, float tilt, float side, float ears, float nod,
                                       float doze, float yawn, float lean, float sit) {
        head.zRot += 0.34F * tilt * side;
        head.xRot -= 0.06F * tilt;
        rightEar.xRot -= 0.25F * ears;
        leftEar.xRot -= 0.25F * ears;
        rightEar.zRot += Mth.sin(age * 0.6F) * 0.12F * ears;
        leftEar.zRot += Mth.sin(age * 0.6F + 2.0F) * 0.12F * ears;
        head.xRot += Mth.sin(age * 0.9F) * 0.16F * nod;
        head.xRot += doze * (0.12F + 0.08F * Mth.sin(age * 0.25F));
        head.xRot -= 0.38F * yawn * (1.0F - 0.5F * sit);
        body.zRot -= 0.2F * lean;
        head.zRot -= 0.24F * lean;
        head.x += 1.2F * lean;
    }

    /** Saludo con la patita (la izquierda) y bracitos que se estiran al bostezar de pie. */
    public static void applySocialArms(ModelPart rightArm, ModelPart leftArm, float age, float wave,
                                       float yawn, float sit) {
        leftArm.zRot = Mth.lerp(wave, leftArm.zRot, -(1.75F + 0.3F * Mth.sin(age * 0.9F)));
        leftArm.xRot = Mth.lerp(wave, leftArm.xRot, -0.4F);
        float stretch = yawn * (1.0F - sit);
        rightArm.zRot += 0.7F * stretch;
        leftArm.zRot -= 0.7F * stretch;
        rightArm.xRot -= 0.5F * stretch;
        leftArm.xRot -= 0.5F * stretch;
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
