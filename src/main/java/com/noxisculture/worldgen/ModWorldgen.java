package com.noxisculture.worldgen;

import com.noxisculture.NoxisCulture;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Generación en el mundo. Las vetas se definen en JSON
 * (data/noxis_culture/worldgen/...) para poder ajustar rareza sin tocar Java.
 */
public final class ModWorldgen {
    private ModWorldgen() {}

    public static final ResourceKey<PlacedFeature> NOXUS_ORE_PLACED =
            ResourceKey.create(Registries.PLACED_FEATURE, NoxisCulture.id("ore_noxus"));

    public static void initialize() {
        // Por ahora en todo el Overworld profundo; cuando exista el bioma Crying
        // le daremos vetas extra ahí.
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                NOXUS_ORE_PLACED);
    }
}
