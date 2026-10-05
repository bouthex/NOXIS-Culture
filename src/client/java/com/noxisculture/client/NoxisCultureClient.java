package com.noxisculture.client;

import com.noxisculture.client.entity.ModModelLayers;
import com.noxisculture.client.entity.NoxisVillagerRenderer;
import com.noxisculture.entity.ModEntityTypes;
import com.noxisculture.NoxisCulture;
import com.noxisculture.fluid.ModFluids;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.ARGB;
import net.minecraft.client.renderer.entity.EntityRenderers;

/** Todo lo visual vive en src/client: el servidor nunca carga estas clases. */
public final class NoxisCultureClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModModelLayers.initialize();
        EntityRenderers.register(ModEntityTypes.NOXIS_VILLAGER, NoxisVillagerRenderer::new);

        // Noyux: texturas propias, semitransparentes (la transparencia viene de la textura).
        FluidRenderingRegistry.register(ModFluids.NOYUX, ModFluids.FLOWING_NOYUX,
                new FluidModel.Unbaked(
                        new Material(NoxisCulture.id("block/noyux_still")),
                        new Material(NoxisCulture.id("block/noyux_flow")),
                        new Material(NoxisCulture.id("block/noyux_still")),
                        BlockTintSources.constant(ARGB.opaque(0xFFFFFF))));
    }
}
