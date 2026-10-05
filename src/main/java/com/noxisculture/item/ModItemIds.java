package com.noxisculture.item;

import com.noxisculture.NoxisCulture;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/** IDs de ítems (sin instancias), patrón de vanilla ItemIds. */
public final class ModItemIds {
    private ModItemIds() {}

    public static final ResourceKey<Item> COSMOS_PICKAXE = create("cosmos_pickaxe");

    private static ResourceKey<Item> create(String name) {
        return ResourceKey.create(Registries.ITEM, NoxisCulture.id(name));
    }
}
