package com.noxisculture.client.entity;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Capa de espalda de los Noxis (variante estética reutilizable por Aldeano, Guerrero, Sabio...).
 *
 * <p>Pequeña capa amarilla que cuelga detrás del cuerpo, inspirada en la capa del jugador:
 * dos tramos (alto y bajo) para que se sienta tela. El tramo alto lleva el emblema bordado
 * del Nixil, cuyo "corazón" es el ojal real por donde sale la colita; el tramo bajo es el
 * que flamea con el movimiento.</p>
 *
 * <p>Uso desde cualquier modelo Noxis: {@code NoxisCape.addTo(body)} al definir la malla y
 * {@code NoxisCape.animate(body, ...)} en {@code setupAnim}. La física (inercia) la calcula
 * {@code NoxisCapePhysics} en la entidad.</p>
 */
public final class NoxisCape {
    private NoxisCape() {}

    public static final String CAPE = "cape";
    public static final String CAPE_LOWER = "cape_lower";

    /** Separación desde la espalda: queda por fuera del poncho de hojas y deja pasar la cola. */
    private static final float BACK_Z = 3.05F;
    private static final float TOP_Y = 0.3F;
    private static final float UPPER_LENGTH = 4.4F;

    /** Cantidad de estilos de capa (misma base amarilla; cambian ribete, costuras y detalles del Nixil). */
    public static final int STYLES = 3;

    private static String capeName(int style) {
        return CAPE + "_" + style;
    }

    /** Agrega las capas (una por estilo) como hijas del cuerpo. Medidas enteras de textura + CubeDeformation = tamaño exacto. */
    public static void addTo(PartDefinition body) {
        PartDefinition cape0 = body.addOrReplaceChild(capeName(0),
                CubeListBuilder.create()
                        .texOffs(64, 0).addBox(-4.0750F, 0.2000F, -0.5000F, 3.0F, 4.0F, 1.0F, new CubeDeformation(-0.0750F, 0.2000F, -0.3750F))
                        .texOffs(73, 0).addBox(1.0750F, 0.2000F, -0.5000F, 3.0F, 4.0F, 1.0F, new CubeDeformation(-0.0750F, 0.2000F, -0.3750F))
                        .texOffs(82, 0).addBox(-1.0000F, 0.0000F, -0.5000F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.1500F, 0.0000F, -0.3750F))
                        .texOffs(89, 0).addBox(-1.0000F, 3.7000F, -0.5000F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.1500F, -0.3000F, -0.3750F))
                        .texOffs(96, 0).addBox(-3.0000F, 0.0000F, 0.1500F, 6.0F, 4.0F, 0.0F),
                PartPose.offset(0.0F, TOP_Y, BACK_Z));
        cape0.addOrReplaceChild(CAPE_LOWER,
                CubeListBuilder.create()
                        .texOffs(109, 0).addBox(-4.0000F, 0.0000F, -0.5000F, 8.0F, 2.0F, 1.0F, new CubeDeformation(0.0000F, 0.0000F, -0.3750F)),
                PartPose.offset(0.0F, UPPER_LENGTH, 0.0F));
        PartDefinition cape1 = body.addOrReplaceChild(capeName(1),
                CubeListBuilder.create()
                        .texOffs(64, 6).addBox(-4.0750F, 0.2000F, -0.5000F, 3.0F, 4.0F, 1.0F, new CubeDeformation(-0.0750F, 0.2000F, -0.3750F))
                        .texOffs(73, 6).addBox(1.0750F, 0.2000F, -0.5000F, 3.0F, 4.0F, 1.0F, new CubeDeformation(-0.0750F, 0.2000F, -0.3750F))
                        .texOffs(82, 6).addBox(-1.0000F, 0.0000F, -0.5000F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.1500F, 0.0000F, -0.3750F))
                        .texOffs(89, 6).addBox(-1.0000F, 3.7000F, -0.5000F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.1500F, -0.3000F, -0.3750F))
                        .texOffs(96, 6).addBox(-3.0000F, 0.0000F, 0.1500F, 6.0F, 4.0F, 0.0F),
                PartPose.offset(0.0F, TOP_Y, BACK_Z));
        cape1.addOrReplaceChild(CAPE_LOWER,
                CubeListBuilder.create()
                        .texOffs(109, 6).addBox(-4.0000F, 0.0000F, -0.5000F, 8.0F, 2.0F, 1.0F, new CubeDeformation(0.0000F, 0.0000F, -0.3750F)),
                PartPose.offset(0.0F, UPPER_LENGTH, 0.0F));
        PartDefinition cape2 = body.addOrReplaceChild(capeName(2),
                CubeListBuilder.create()
                        .texOffs(64, 12).addBox(-4.0750F, 0.2000F, -0.5000F, 3.0F, 4.0F, 1.0F, new CubeDeformation(-0.0750F, 0.2000F, -0.3750F))
                        .texOffs(73, 12).addBox(1.0750F, 0.2000F, -0.5000F, 3.0F, 4.0F, 1.0F, new CubeDeformation(-0.0750F, 0.2000F, -0.3750F))
                        .texOffs(82, 12).addBox(-1.0000F, 0.0000F, -0.5000F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.1500F, 0.0000F, -0.3750F))
                        .texOffs(89, 12).addBox(-1.0000F, 3.7000F, -0.5000F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.1500F, -0.3000F, -0.3750F))
                        .texOffs(96, 12).addBox(-3.0000F, 0.0000F, 0.1500F, 6.0F, 4.0F, 0.0F),
                PartPose.offset(0.0F, TOP_Y, BACK_Z));
        cape2.addOrReplaceChild(CAPE_LOWER,
                CubeListBuilder.create()
                        .texOffs(109, 12).addBox(-4.0000F, 0.0000F, -0.5000F, 8.0F, 2.0F, 1.0F, new CubeDeformation(0.0000F, 0.0000F, -0.3750F)),
                PartPose.offset(0.0F, UPPER_LENGTH, 0.0F));
    }

    /**
     * Anima la capa.
     *
     * @param style estilo de capa (0..STYLES-1), o -1 si este Noxis no tiene capa
     * @param swing 0..1 cuánto se mueve el Noxis (con inercia): quieto 0, caminando ~0.5, corriendo 1
     * @param turn  -1..1 giro del cuerpo con retraso (la capa queda "atrás" al girar)
     * @param rest  0..1 cuánto está sentado (el tramo bajo se apoya hacia atrás en el piso)
     */
    public static void animate(ModelPart body, int style, float swing, float turn, float age, float rest) {
        for (int i = 0; i < STYLES; i++) {
            body.getChild(capeName(i)).visible = i == style;
        }
        if (style < 0 || style >= STYLES) {
            return;   // sin capa
        }
        ModelPart cape = body.getChild(capeName(style));
        ModelPart lower = cape.getChild(CAPE_LOWER);
        // Como la capa del jugador: toda la capa se despega de la espalda y se levanta hacia
        // atrás con el movimiento (quieto ~0, caminando ~30°, corriendo ~55°).
        float flutter = Mth.sin(age * 0.35F) * 0.06F * swing;
        cape.xRot = (0.04F + 0.9F * swing + flutter * 0.5F) * (1.0F - rest);
        cape.zRot = 0.15F * turn * (1.0F - rest);
        // Tramo bajo: se curva un poco más por el "viento", con un leve ondear.
        float flap = 0.05F + 0.35F * swing + flutter;
        lower.xRot = Mth.lerp(rest, flap, 1.15F);
        lower.zRot = 0.25F * turn * (1.0F - rest);
    }
}
