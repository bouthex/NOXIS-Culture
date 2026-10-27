package com.noxisculture.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.noxisculture.NoxisCulture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Libro abierto y lápiz de los Noxis lectores / escritores, y caballete, lienzo, pincel y paleta de los
 * artistas: modelitos 3D (tapas, lomo, páginas
 * con renglones; lápiz de madera pintada con punta y terminación oscura) con su propia textura
 * {@code noxis_props.png}. Usa una copia del modelo que solo muestra esas piezas, así siguen
 * exactamente las patitas en cada pose; no toca las texturas del Noxis.
 */
public class NoxisPropsLayer extends RenderLayer<NoxisVillagerRenderState, NoxisVillagerModel> {
    private static final net.minecraft.resources.Identifier TEXTURE = NoxisCulture.id("textures/entity/noxis_props.png");

    private final NoxisVillagerModel propsModel;
    private final RenderType type;

    public NoxisPropsLayer(RenderLayerParent<NoxisVillagerRenderState, NoxisVillagerModel> parent,
                           NoxisVillagerModel propsModel) {
        super(parent);
        this.propsModel = propsModel;
        this.type = propsModel.renderType(TEXTURE);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       NoxisVillagerRenderState state, float yRot, float xRot) {
        if (!state.bookShown && state.easelScale <= 0.01F && !state.brushShown && !state.paletteShown) return;
        collector.order(0).submitModel(this.propsModel, state, poseStack, this.type, light, OverlayTexture.NO_OVERLAY,
                -1, null, 0, null);
    }
}
