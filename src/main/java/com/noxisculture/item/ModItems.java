package com.noxisculture.item;

import com.noxisculture.item.custom.CosmosPickaxeItem;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemLore;

public final class ModItems {
    private ModItems() {}

    /** Pico del Cosmos: herramienta exclusiva que venden los Aldeanos Noxis. */
    public static final Item COSMOS_PICKAXE = register(
            ModItemIds.COSMOS_PICKAXE,
            CosmosPickaxeItem::new,
            new Item.Properties()
                    .pickaxe(ModToolMaterials.COSMOS, 1.0F, -2.8F) // mismos valores de ataque que el pico de diamante
                    .rarity(Rarity.RARE)
                    // La lore es un componente de datos: es la forma moderna (appendHoverText está deprecado).
                    .component(DataComponents.LORE, new ItemLore(List.of(
                            Component.translatable("item.noxis_culture.cosmos_pickaxe.lore"))))
    );

    public static Item register(ResourceKey<Item> key, Function<Item.Properties, Item> factory, Item.Properties properties) {
        Item item = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void initialize() {}
}
