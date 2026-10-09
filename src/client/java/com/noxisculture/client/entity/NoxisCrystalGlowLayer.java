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
 * Brillo mágico de los ojos durante la "fascinación por los cristales".
 *
 * <p>Reutiliza la MISMA textura emisiva de los ojos ({@code noxis_villager_eyes.png}, sin
 * cambios) y la vuelve a dibujar encima en modo aditivo (suma luz, como el brillo del creeper
 * cargado, pero quieto), con una intensidad que sube, late suave y baja:</p>
 * <p>Los ojitos conservan su forma y su pupila. Usa una copia del modelo que solo muestra los
 * ojos, así no se ilumina nada más (ni la gema ni la capa). Las chispitas doradas que lo
 * acompañan las larga la entidad. No toca {@link NoxisVillagerEyesLayer}; con brillo 0 no dibuja nada.</p>
 */
public class NoxisCrystalGlowLayer extends RenderLayer<NoxisVillagerRenderState, NoxisVillagerModel> {
    /** energySwirl con desplazamiento 0 = textura quieta, emisiva y aditiva. */
    private static final RenderType GLOW = RenderTypes.energySwirl(
            NoxisCulture.id("textures/entity/noxis_villager_eyes.png"), 0.0F, 0.0F);
    /** Luz máxima (cielo 15, bloque 15): el brillo no depende de la oscuridad. */
    private static final int FULL_BRIGHT = 0xF000F0;
    /** Dorado cálido e intenso (1 = duplica la luz de los ojos). */
    private static final float[] COLOR = {1.0F, 0.9F, 0.6F};

    private final NoxisVillagerModel eyesModel;

    public NoxisCrystalGlowLayer(RenderLayerParent<NoxisVillagerRenderState, NoxisVillagerModel> parent,
                                 NoxisVillagerModel eyesModel) {
        super(parent);
        this.eyesModel = eyesModel;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       NoxisVillagerRenderState state, float yRot, float xRot) {
        float glow = state.crystalGlow;
        if (glow <= 0.01F) return;
        collector.order(2).submitModel(this.eyesModel, state, poseStack, GLOW,
                FULL_BRIGHT, OverlayTexture.NO_OVERLAY, tint(COLOR, glow), null, 0, null);
    }

    private static int tint(float[] color, float glow) {
        return 0xFF000000
                | (channel(color[0] * glow) << 16)
                | (channel(color[1] * glow) << 8)
                | channel(color[2] * glow);
    }

    private static int channel(float value) {
        return Math.round(Math.max(0.0F, Math.min(1.0F, value)) * 255.0F);
    }
}
