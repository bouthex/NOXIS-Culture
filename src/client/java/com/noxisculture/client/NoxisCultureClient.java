package com.noxisculture.client;

import com.noxisculture.block.ModBlocks;
import com.noxisculture.client.armor.NoxisHatArmorRenderer;
import com.noxisculture.client.armor.NoxisHatTint;
import com.noxisculture.client.entity.ModModelLayers;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import com.noxisculture.client.entity.NoxisVillagerRenderer;
import com.noxisculture.entity.ModEntityTypes;
import com.noxisculture.NoxisCulture;
import com.noxisculture.fluid.ModFluids;
import net.fabricmc.api.ClientModInitializer;
import com.noxisculture.particle.ModParticles;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.particle.EndRodParticle;
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
        // Configuración accesible sin Mod Menu: botón en Opciones y comando /noxisculture config.
        com.noxisculture.client.config.NoxisConfigAccess.register();

        // Sombrero de Noxis puesto en la cabeza (jugadores, soportes de armadura...).
        ModelLayerRegistry.registerModelLayer(NoxisHatArmorRenderer.LAYER, NoxisHatArmorRenderer::createLayer);
        ArmorRenderer.register(context -> new NoxisHatArmorRenderer(context), ModBlocks.NOXIS_HAT);
        // Sombrero apoyado en el piso: se pinta con los colores guardados en su bloque.
        BlockColorRegistry.register(List.of(new NoxisHatTint(true), new NoxisHatTint(false)), ModBlocks.NOXIS_HAT);

        // Noyux: texturas propias, semitransparentes (la transparencia viene de la textura).
        // Quieto: copia de la textura del caldero, sin destellos blancos y más lenta (la del caldero
        // no se toca). Fluyendo y cayendo: textura de flujo que baja despacio, en bucle.
        // La tercera (las caras pegadas a vidrio, hojas, etc.) usa también la de flujo: así ningún
        // tramo del líquido cambia de aspecto de golpe.
        FluidRenderingRegistry.register(ModFluids.NOYUX, ModFluids.FLOWING_NOYUX,
                new FluidModel.Unbaked(
                        new Material(NoxisCulture.id("block/noyux_still_world")),
                        new Material(NoxisCulture.id("block/noyux_flow")),
                        new Material(NoxisCulture.id("block/noyux_flow")),
                        BlockTintSources.constant(ARGB.opaque(0xFFFFFF))));

        // Partículas del Noyux: dibujos propios (cuadros en assets/noxis_culture/particles), con el
        // movimiento de las partículas de la vara del End: flotan, brillan y se apagan de a poco.
        ParticleProviderRegistry.getInstance().register(ModParticles.NOYUX_SMOKE, EndRodParticle.Provider::new);
        ParticleProviderRegistry.getInstance().register(ModParticles.NOYUX_SPARK, EndRodParticle.Provider::new);
        ParticleProviderRegistry.getInstance().register(ModParticles.NOYUX_MOTE, EndRodParticle.Provider::new);
    }
}
