package com.noxisculture.client;

import com.noxisculture.client.entity.ModModelLayers;
import com.noxisculture.client.entity.NoxisVillagerRenderer;
import com.noxisculture.entity.ModEntityTypes;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

/** Todo lo visual vive en src/client: el servidor nunca carga estas clases. */
public final class NoxisCultureClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModModelLayers.initialize();
        EntityRenderers.register(ModEntityTypes.NOXIS_VILLAGER, NoxisVillagerRenderer::new);
    }
}
