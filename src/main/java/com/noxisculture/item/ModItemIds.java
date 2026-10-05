package com.noxisculture.item;

import com.noxisculture.NoxisCulture;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/** IDs de ítems (sin instancias), patrón de vanilla ItemIds. */
public final class ModItemIds {
    private ModItemIds() {}

    public static final ResourceKey<Item> COSMOS_PICKAXE = create("cosmos_pickaxe");
    public static final ResourceKey<Item> RAW_NOXUS = create("raw_noxus");
    public static final ResourceKey<Item> NOXUS_INGOT = create("noxus_ingot");
    public static final ResourceKey<Item> NIXIL = create("nixil");
    public static final ResourceKey<Item> NOYUX_BUCKET = create("noyux_bucket");

    // Huevos de spawn (uno por cada mob del mod)
    public static final ResourceKey<Item> NOXIS_VILLAGER_SPAWN_EGG = create("noxis_villager_spawn_egg");

    private static ResourceKey<Item> create(String name) {
        return ResourceKey.create(Registries.ITEM, NoxisCulture.id(name));
    }
}
