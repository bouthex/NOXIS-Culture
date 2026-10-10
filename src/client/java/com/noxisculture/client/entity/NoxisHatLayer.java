package com.noxisculture.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.noxisculture.NoxisCulture;
import com.noxisculture.item.NoxisHatColors;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * El sombrero que lleva puesto el Noxis, con SUS colores. Usa la misma geometría y animación de
 * siempre (copias del modelo que solo muestran el sombrero) y la textura-máscara
 * {@code noxis_hat_mask.png}: la copa y el ala se pintan con su color, el lazo con el suyo, y la
 * ramita y la gema se dibujan con sus colores originales (nunca se tiñen). Sin teñir, se ve igual
 * que el sombrero negro y violeta original.
 */
public class NoxisHatLayer extends RenderLayer<NoxisVillagerRenderState, NoxisVillagerModel> {
    public static final Identifier MASK = NoxisCulture.id("textures/entity/noxis_hat_mask.png");
    /** Máscara más clara, solo para el blanco (para que se vea blanco y no gris). */
    public static final Identifier MASK_WHITE = NoxisCulture.id("textures/entity/noxis_hat_mask_white.png");
    /** El brillo de la gema (igual que antes: luz propia, como los ojitos). */
    public static final RenderType GEM_GLOW = RenderTypes.eyes(NoxisCulture.id("textures/entity/noxis_hat_gem_glow.png"));

    private final NoxisVillagerModel crown;
    private final NoxisVillagerModel band;
    private final NoxisVillagerModel fixed;
    private final RenderType type;
    private final RenderType typeWhite;

    public NoxisHatLayer(RenderLayerParent<NoxisVillagerRenderState, NoxisVillagerModel> parent,
                         NoxisVillagerModel crown, NoxisVillagerModel band, NoxisVillagerModel fixed) {
        super(parent);
        this.crown = crown;
        this.band = band;
        this.fixed = fixed;
        this.type = crown.renderType(MASK);
        this.typeWhite = crown.renderType(MASK_WHITE);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       NoxisVillagerRenderState state, float yRot, float xRot) {
        if (!state.hasHat) return;
        int overlay = OverlayTexture.NO_OVERLAY;
        collector.order(0).submitModel(this.crown, state, poseStack,
                NoxisHatColors.isWhite(state.hatCrownColor) ? this.typeWhite : this.type, light, overlay,
                0xFF000000 | state.hatCrownColor, null, 0, null);
        collector.order(0).submitModel(this.band, state, poseStack,
                NoxisHatColors.isWhite(state.hatBandColor) ? this.typeWhite : this.type, light, overlay,
                0xFF000000 | state.hatBandColor, null, 0, null);
        collector.order(0).submitModel(this.fixed, state, poseStack, this.type, light, overlay,
                -1, null, 0, null);
        collector.order(1).submitModel(this.fixed, state, poseStack, GEM_GLOW, 0xF000F0, overlay,
                -1, null, 0, null);
    }
}
