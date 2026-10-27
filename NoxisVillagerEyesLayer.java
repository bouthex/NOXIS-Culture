package com.noxisculture.client.entity;

import com.noxisculture.NoxisCulture;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * Capa emisiva (como los ojos de araña o enderman): los ojos amarillos
 * y la gema brillan aunque esté oscuro. Solo dibuja los píxeles no
 * transparentes de noxis_villager_eyes.png.
 */
public class NoxisVillagerEyesLayer extends EyesLayer<NoxisVillagerRenderState, NoxisVillagerModel> {
    private static final RenderType EYES =
            RenderTypes.eyes(NoxisCulture.id("textures/entity/noxis_villager_eyes.png"));

    public NoxisVillagerEyesLayer(RenderLayerParent<NoxisVillagerRenderState, NoxisVillagerModel> parent) {
        super(parent);
    }

    @Override
    public RenderType renderType() {
        return EYES;
    }
}
