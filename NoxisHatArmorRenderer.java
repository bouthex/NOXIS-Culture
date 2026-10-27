package com.noxisculture.client.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.noxisculture.NoxisCulture;
import com.noxisculture.client.entity.NoxisHatLayer;
import com.noxisculture.item.NoxisHatColors;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * El Sombrero de Noxis puesto en la cabeza de un jugador (o de un zombi, un soporte de
 * armadura...). Es el MISMO sombrero de copa del Noxis: misma geometría, proporciones y textura,
 * derecho y centrado sobre la cabeza. Se dibuja en tres partes con sus colores: copa y ala, lazo,
 * y lo fijo (ramita y gema, que nunca se tiñen). Sigue los movimientos de la cabeza.
 */
public class NoxisHatArmorRenderer implements ArmorRenderer {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(NoxisCulture.id("noxis_hat_worn"), "main");
    private static final String HAT = "noxis_hat";
    private static final String BRIM = "brim";
    private static final String CROWN = "crown";
    private static final String BAND = "band";
    private static final String SPRIG = "sprig";
    private static final String GEM = "gem";
    /** Tamaño del sombrero puesto en un jugador, respecto del sombrero del Noxis. */
    private static final float WORN_SCALE = 1.15F;
    /**
     * Altura de la base del ala respecto de la cabeza (la cabeza termina en -8; la capa del pelo de
     * la skin llega a -8.5): el ala se apoya justo sobre el pelo, sin hueco y sin hundirse.
     */
    private static final float BRIM_Y = -8.3F;

    private final HatModel crownModel;
    private final HatModel bandModel;
    private final HatModel fixedModel;

    public NoxisHatArmorRenderer(EntityRendererProvider.Context context) {
        this.crownModel = part(context, true, false, false);
        this.bandModel = part(context, false, true, false);
        this.fixedModel = part(context, false, false, true);
    }

    /**
     * El modelo del sombrero puesto: copia la posición de la cabeza de quien lo lleva y después
     * fija su propio tamaño y altura (la escala se aplica desde la base del ala, que queda
     * apoyada en la cabeza).
     */
    private static final class HatModel extends HumanoidModel<HumanoidRenderState> {
        final ModelPart hat;

        HatModel(ModelPart root) {
            super(root);
            this.hat = this.head.getChild(HAT);
        }

        @Override
        public void setupAnim(HumanoidRenderState state) {
            // La pose de la cabeza ya vino copiada del modelo de quien lo lleva: solo el sombrero.
            this.hat.y = BRIM_Y;
            this.hat.xScale = WORN_SCALE;
            this.hat.yScale = WORN_SCALE;
            this.hat.zScale = WORN_SCALE;
        }
    }

    /** Una copia del sombrero que solo muestra algunas de sus piezas. */
    private static HatModel part(EntityRendererProvider.Context context,
                                                          boolean crown, boolean band, boolean fixed) {
        HatModel model = new HatModel(context.bakeLayer(LAYER));
        ModelPart hat = model.hat;
        hat.getChild(BRIM).visible = crown;
        hat.getChild(CROWN).visible = crown;
        hat.getChild(BAND).visible = band;
        hat.getChild(SPRIG).visible = fixed;
        hat.getChild(GEM).visible = fixed;
        return model;
    }

    /** La geometría del sombrero del Noxis, apoyada sobre una cabeza de jugador. */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        // Solo la cabeza (con el sombrero); el resto del cuerpo queda vacío.
        PartDefinition head = root.addOrReplaceChild(PartNames.HEAD, CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild(PartNames.HAT, CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild(PartNames.BODY, CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild(PartNames.RIGHT_ARM, CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild(PartNames.LEFT_ARM, CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild(PartNames.RIGHT_LEG, CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild(PartNames.LEFT_LEG, CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));

        // Apoyado justo encima de la cabeza (y de la capa del pelo de la skin), derecho y centrado.
        // Un poco más grande que el original (x1.15) para acercarse a cómo le queda a un Noxis:
        // la base del ala sigue apoyada en el mismo lugar (escala desde el pie del sombrero).
        PartDefinition hat = head.addOrReplaceChild(HAT, CubeListBuilder.create(),
                PartPose.offset(0.0F, BRIM_Y, 0.0F));
        hat.addOrReplaceChild(BRIM,
                CubeListBuilder.create().texOffs(0, 46).addBox(-6.0F, -1.0F, -6.0F, 12.0F, 1.0F, 12.0F), PartPose.ZERO);
        hat.addOrReplaceChild(CROWN,
                CubeListBuilder.create().texOffs(36, 0).addBox(-3.5F, -13.0F, -3.5F, 7.0F, 12.0F, 7.0F), PartPose.ZERO);
        hat.addOrReplaceChild(BAND,
                CubeListBuilder.create().texOffs(36, 19)
                        .addBox(-3.5F, -3.0F, -3.5F, 7.0F, 2.0F, 7.0F, new CubeDeformation(0.25F)), PartPose.ZERO);
        hat.addOrReplaceChild(SPRIG,
                CubeListBuilder.create().texOffs(48, 30).addBox(0.0F, -4.0F, -1.0F, 1.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(3.2F, -2.0F, 1.0F, 0.0F, 0.0F, 0.35F));
        hat.addOrReplaceChild(GEM,
                CubeListBuilder.create().texOffs(62, 62).addBox(-0.5F, -3.0F, -3.8F, 1.0F, 1.0F, 0.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void render(PoseStack poseStack, SubmitNodeCollector collector, ItemStack stack,
                       HumanoidRenderState state, EquipmentSlot slot, int light,
                       HumanoidModel<HumanoidRenderState> contextModel) {
        if (slot != EquipmentSlot.HEAD) return;
        int crown = 0xFF000000 | NoxisHatColors.crown(stack);
        int band = 0xFF000000 | NoxisHatColors.band(stack);
        draw(contextModel, state, this.crownModel, collector, poseStack, light, crown);
        draw(contextModel, state, this.bandModel, collector, poseStack, light, band);
        // (dentro de draw: el blanco usa la máscara clara)
        draw(contextModel, state, this.fixedModel, collector, poseStack, light, -1);
        ArmorRenderer.submitTransformCopyingModel(contextModel, state, this.fixedModel, state, true, collector.order(1),
                poseStack, NoxisHatLayer.GEM_GLOW, 0xF000F0, OverlayTexture.NO_OVERLAY, -1, null, 0, null);
        if (stack.hasFoil()) {
            ArmorRenderer.submitTransformCopyingModel(contextModel, state, this.crownModel, state, true,
                    collector.order(1), poseStack, RenderTypes.armorEntityGlint(), light, OverlayTexture.NO_OVERLAY, 0, null);
        }
    }

    private static void draw(HumanoidModel<HumanoidRenderState> source, HumanoidRenderState state,
                             HatModel model, SubmitNodeCollector collector,
                             PoseStack poseStack, int light, int tint) {
        ArmorRenderer.submitTransformCopyingModel(source, state, model, state, true, collector.order(0),
                poseStack, model.renderType(tint != -1 && NoxisHatColors.isWhite(tint & 0xFFFFFF)
                        ? NoxisHatLayer.MASK_WHITE : NoxisHatLayer.MASK),
                light, OverlayTexture.NO_OVERLAY, tint, null, 0, null);
    }

    /** No dibujar además el objeto (el bloque) sobre la cabeza: el sombrero ya está puesto. */
    @Override
    public boolean shouldRenderDefaultHeadItem(LivingEntity entity, ItemStack stack) {
        return false;
    }
}
