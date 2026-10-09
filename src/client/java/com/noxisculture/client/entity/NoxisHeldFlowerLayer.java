package com.noxisculture.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Dibuja la flor REAL que el Noxis tiene en la mano (el mismo ítem vanilla de esa flor),
 * paradita, en la punta de su manito. Si no tiene flor, no dibuja nada.
 */
public class NoxisHeldFlowerLayer extends RenderLayer<NoxisVillagerRenderState, NoxisVillagerModel> {
    /** Tamaño de la flor respecto de un bloque (el Noxis es chiquito). */
    private static final float SCALE = 0.55F;

    public NoxisHeldFlowerLayer(RenderLayerParent<NoxisVillagerRenderState, NoxisVillagerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       NoxisVillagerRenderState state, float yRot, float xRot) {
        if (state.heldFlower.isEmpty()) return;
        NoxisVillagerModel model = this.getParentModel();
        model.setupAnim(state);                         // pose de ESTE Noxis en este frame
        poseStack.pushPose();
        model.root().translateAndRotate(poseStack);
        model.flowerAnchor().translateAndRotate(poseStack);
        // El modelo está "dado vuelta" (eje Y hacia abajo): se endereza la flor y se sube para
        // que el tallito quede en la mano y los pétalos arriba.
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(25.0F));
        poseStack.scale(SCALE, SCALE, SCALE);
        poseStack.translate(0.0F, 0.42F, 0.0F);
        state.heldFlower.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
