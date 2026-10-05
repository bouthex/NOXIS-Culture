package com.noxisculture.entity;

import com.noxisculture.NoxisCulture;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;

public final class ModEntityTypeIds {
    private ModEntityTypeIds() {}

    public static final ResourceKey<EntityType<?>> NOXIS_VILLAGER = create("noxis_villager");

    private static ResourceKey<EntityType<?>> create(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, NoxisCulture.id(name));
    }
}
