package com.noxisculture.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.noxisculture.NoxisCulture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Globito de sueño de caricatura: mientras un Noxis duerme, le sale de la naricita un globito
 * celeste que se infla y desinfla con la respiración; al despertarse hace ¡plop!.
 *
 * <p>Usa una copia del modelo que solo muestra el globito (sigue la cabeza en todas sus poses)
 * y su propia texturita ({@code noxis_sleep_bubble.png}); no toca las texturas del Noxis.
 * Despierto no dibuja nada.</p>
 */
public class NoxisSleepBubbleLayer extends RenderLayer<NoxisVillagerRenderState, NoxisVillagerModel> {
    private static final RenderType BUBBLE = RenderTypes.entityCutoutNoCull(
            NoxisCulture.id("textures/entity/noxis_sleep_bubble.png"));
    /** Un poquito de luz propia (brillo sutil): nunca se ve más oscuro que esto. */
    private static final int MIN_BLOCK_LIGHT = 9;

    private final NoxisVillagerModel bubbleModel;

    public NoxisSleepBubbleLayer(RenderLayerParent<NoxisVillagerRenderState, NoxisVillagerModel> parent,
                                 NoxisVillagerModel bubbleModel) {
        super(parent);
        this.bubbleModel = bubbleModel;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       NoxisVillagerRenderState state, float yRot, float xRot) {
        if (NoxisVillagerModel.sleepBubbleScale(state.restAnim, state.restRising, state.ageInTicks) <= 0.02F) return;
        int block = Math.max((light & 0xFFFF) >> 4, MIN_BLOCK_LIGHT);
        int sky = (light >> 20) & 0xF;
        int glowLight = (sky << 20) | (block << 4);
        collector.order(1).submitModel(this.bubbleModel, state, poseStack, BUBBLE,
                glowLight, OverlayTexture.NO_OVERLAY, -1, null, 0, null);
    }
}
