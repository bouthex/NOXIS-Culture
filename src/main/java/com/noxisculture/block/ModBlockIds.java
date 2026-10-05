package com.noxisculture.block;

import com.noxisculture.NoxisCulture;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;

/** IDs de bloques SIN ítem propio (bloques técnicos). */
public final class ModBlockIds {
    private ModBlockIds() {}

    public static final ResourceKey<Block> NOYUX_CAULDRON = create("noyux_cauldron");
    public static final ResourceKey<Block> NOYUX = create("noyux");

    private static ResourceKey<Block> create(String name) {
        return ResourceKey.create(Registries.BLOCK, NoxisCulture.id(name));
    }
}
