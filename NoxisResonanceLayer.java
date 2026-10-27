package com.noxisculture.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.noxisculture.NoxisCulture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * Resonancia amarilla (solo visual): mientras suena cerca el disco de cristal amarillo, los
 * cristales violetas del Noxis (la gema del pecho y el emblema de la capa) se vuelven dorados y
 * los ojitos toman un amarillo más intenso y cálido, con un resplandor que late con el pulso.
 *
 * <ul>
 *   <li>Cristales y ojos: {@code noxis_resonance.png} tiene SOLO esos píxeles, recoloreados con la
 *   misma luz relativa (se conservan las facetas y los detalles). Se dibuja encima con la misma
 *   geometría, brillando con luz propia y con transparencia según cuánto resuena (entra y sale suave).</li>
 *   <li>Resplandor: la textura emisiva de los ojos otra vez, en modo aditivo, con un dorado cálido que
 *   late en cada tiempo de la canción (misma técnica que el brillo de la fascinación).</li>
 * </ul>
 *
 * <p>No cambia ninguna textura ni dato guardado: con resonancia 0 no dibuja nada.</p>
 */
public class NoxisResonanceLayer extends RenderLayer<NoxisVillagerRenderState, NoxisVillagerModel> {
    private static final RenderType CRYSTALS = RenderTypes.entityTranslucent(
            NoxisCulture.id("textures/entity/noxis_resonance.png"));
    private static final RenderType GLOW = RenderTypes.energySwirl(
            NoxisCulture.id("textures/entity/noxis_villager_eyes.png"), 0.0F, 0.0F);
    private static final int FULL_BRIGHT = 0xF000F0;

    private final NoxisVillagerModel bodyModel;
    private final NoxisVillagerModel eyesModel;

    public NoxisResonanceLayer(RenderLayerParent<NoxisVillagerRenderState, NoxisVillagerModel> parent,
                               NoxisVillagerModel bodyModel, NoxisVillagerModel eyesModel) {
        super(parent);
        this.bodyModel = bodyModel;
        this.eyesModel = eyesModel;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       NoxisVillagerRenderState state, float yRot, float xRot) {
        float amount = state.resonance;
        if (amount <= 0.01F) return;
        // Cristales y ojos dorados (por encima de la capa emisiva de siempre).
        int alpha = Math.round(Mth.clamp(amount, 0.0F, 1.0F) * 255.0F);
        collector.order(3).submitModel(this.bodyModel, state, poseStack, CRYSTALS, FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, (alpha << 24) | 0xFFFFFF, null, 0, null);
        // Resplandor que acompaña la música: sube en cada tiempo y baja despacito.
        float f = state.resonanceBeat - Mth.floor(state.resonanceBeat);
        float pulse = (1.0F - f) * (1.0F - f);
        float glow = amount * (0.22F + 0.28F * pulse);
        collector.order(4).submitModel(this.eyesModel, state, poseStack, GLOW, FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, tint(1.0F * glow, 0.72F * glow, 0.22F * glow), null, 0, null);
    }

    private static int tint(float r, float g, float b) {
        return 0xFF000000 | (channel(r) << 16) | (channel(g) << 8) | channel(b);
    }

    private static int channel(float value) {
        return Math.round(Mth.clamp(value, 0.0F, 1.0F) * 255.0F);
    }
}
