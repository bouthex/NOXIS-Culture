package com.noxisculture.client.entity;

import com.noxisculture.NoxisCulture;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;

public final class ModModelLayers {
    private ModModelLayers() {}

    public static final ModelLayerLocation NOXIS_VILLAGER = createMain("noxis_villager");

    private static ModelLayerLocation createMain(String name) {
        return new ModelLayerLocation(NoxisCulture.id(name), "main");
    }

    public static void initialize() {
        ModelLayerRegistry.registerModelLayer(NOXIS_VILLAGER, NoxisVillagerModel::createBodyLayer);
    }
}
