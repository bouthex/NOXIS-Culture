package com.noxisculture.client.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Modelo "chibi" del Noxis: cabeza enorme (8x7x7) sobre un cuerpito de 6x5x4,
 * patitas cortas y un sombrero de copa que es hijo de la cabeza (gira con ella).
 * Textura: 64x64 -> textures/entity/noxis_villager.png
 *
 * Tip: cuando lo rediseñes en Blockbench, exportá con "Mojang mappings".
 */
public class NoxisVillagerModel extends EntityModel<NoxisVillagerRenderState> {
    private static final String HAT_BRIM = "hat_brim";
    private static final String HAT_CROWN = "hat_crown";

    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public NoxisVillagerModel(ModelPart root) {
        super(root);
        this.head = root.getChild(PartNames.HEAD);
        this.rightArm = root.getChild(PartNames.RIGHT_ARM);
        this.leftArm = root.getChild(PartNames.LEFT_ARM);
        this.rightLeg = root.getChild(PartNames.RIGHT_LEG);
        this.leftLeg = root.getChild(PartNames.LEFT_LEG);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild(PartNames.HEAD,
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -7.0F, -3.5F, 8, 7, 7),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        head.addOrReplaceChild(HAT_BRIM,
                CubeListBuilder.create().texOffs(0, 30).addBox(-5.0F, -8.0F, -5.0F, 10, 1, 10),
                PartPose.ZERO);
        head.addOrReplaceChild(HAT_CROWN,
                CubeListBuilder.create().texOffs(32, 0).addBox(-3.0F, -15.0F, -3.0F, 6, 7, 6),
                PartPose.ZERO);

        root.addOrReplaceChild(PartNames.BODY,
                CubeListBuilder.create().texOffs(0, 16).addBox(-3.0F, 0.0F, -2.0F, 6, 5, 4),
                PartPose.offset(0.0F, 16.0F, 0.0F));

        root.addOrReplaceChild(PartNames.RIGHT_ARM,
                CubeListBuilder.create().texOffs(24, 16).addBox(-1.0F, 0.0F, -1.0F, 2, 4, 2),
                PartPose.offset(-4.0F, 16.5F, 0.0F));
        root.addOrReplaceChild(PartNames.LEFT_ARM,
                CubeListBuilder.create().texOffs(24, 16).mirror().addBox(-1.0F, 0.0F, -1.0F, 2, 4, 2),
                PartPose.offset(4.0F, 16.5F, 0.0F));

        root.addOrReplaceChild(PartNames.RIGHT_LEG,
                CubeListBuilder.create().texOffs(32, 16).addBox(-1.0F, 0.0F, -1.0F, 2, 3, 2),
                PartPose.offset(-1.5F, 21.0F, 0.0F));
        root.addOrReplaceChild(PartNames.LEFT_LEG,
                CubeListBuilder.create().texOffs(32, 16).mirror().addBox(-1.0F, 0.0F, -1.0F, 2, 3, 2),
                PartPose.offset(1.5F, 21.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(NoxisVillagerRenderState state) {
        super.setupAnim(state);

        // Mirar hacia donde apunta la cabeza.
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        // Balanceo idle muy suave: le da "vida" y ternura aunque esté quieto.
        this.head.zRot = Mth.sin(state.ageInTicks * 0.08F) * 0.05F;

        // Pasitos cortos y rápidos (frecuencia alta, amplitud baja).
        float pos = state.walkAnimationPos;
        float amount = state.walkAnimationSpeed;
        this.rightLeg.xRot = Mth.cos(pos * 0.9F) * 1.2F * amount;
        this.leftLeg.xRot = Mth.cos(pos * 0.9F + Mth.PI) * 1.2F * amount;
        this.rightArm.xRot = Mth.cos(pos * 0.9F + Mth.PI) * 0.8F * amount;
        this.leftArm.xRot = Mth.cos(pos * 0.9F) * 0.8F * amount;
    }
}
