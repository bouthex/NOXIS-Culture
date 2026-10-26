package com.noxisculture.entity;

import com.noxisculture.entity.custom.NoxisVillager;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntityTypes {
    private ModEntityTypes() {}

    /**
     * Hitbox: 0.7 de ancho x 1.7 de alto (incluye el sombrero de copa alto).
     * Un aldeano vanilla mide 0.6 x 1.95, así que el Noxis es claramente "petiso".
     */
    public static final EntityType<NoxisVillager> NOXIS_VILLAGER = register(
            ModEntityTypeIds.NOXIS_VILLAGER,
            EntityType.Builder.<NoxisVillager>of(NoxisVillager::new, MobCategory.CREATURE)
                    .sized(0.7F, 1.7F)
                    .eyeHeight(0.72F)
                    .clientTrackingRange(10)
    );

    private static <T extends Entity> EntityType<T> register(ResourceKey<EntityType<?>> key, EntityType.Builder<T> builder) {
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(NOXIS_VILLAGER, NoxisVillager.createAttributes());
    }
}
