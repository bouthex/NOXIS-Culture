package com.noxisculture.item;

import com.noxisculture.entity.ModEntityTypes;
import com.noxisculture.fluid.ModFluids;
import com.noxisculture.item.custom.CosmosPickaxeItem;
import com.noxisculture.item.custom.NoyuxBucketItem;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
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

    /** Noxus en bruto: lo que suelta la mena. Solo se refina en un caldero de Noyux. */
    public static final Item RAW_NOXUS = register(ModItemIds.RAW_NOXUS, Item::new, new Item.Properties());

    /** Lingote de Noxus: material base de las futuras herramientas y armadura de Nox. */
    public static final Item NOXUS_INGOT = register(ModItemIds.NOXUS_INGOT, Item::new,
            new Item.Properties().rarity(Rarity.UNCOMMON));

    /** Nixil: cristal con núcleo de nebulosa, se obtiene fundiendo Ladrillos Resplandecientes. */
    public static final Item NIXIL = register(ModItemIds.NIXIL, Item::new,
            new Item.Properties().rarity(Rarity.UNCOMMON));

    /** Cubo de Noyux: lo vende el Sabio Noxis (anteúltimo tradeo). Se vuelca en el piso o en calderos. */
    public static final Item NOYUX_BUCKET = register(ModItemIds.NOYUX_BUCKET,
            props -> new NoyuxBucketItem(ModFluids.NOYUX, props),
            new Item.Properties()
                    .craftRemainder(Items.BUCKET)
                    .stacksTo(1)
                    .rarity(Rarity.RARE)
                    .component(DataComponents.LORE, new ItemLore(List.of(
                            Component.translatable("item.noxis_culture.noyux_bucket.lore")))));

    // ---------------- Huevos de spawn ----------------
    public static final Item NOXIS_VILLAGER_SPAWN_EGG = register(ModItemIds.NOXIS_VILLAGER_SPAWN_EGG,
            SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntityTypes.NOXIS_VILLAGER));

    public static Item register(ResourceKey<Item> key, Function<Item.Properties, Item> factory, Item.Properties properties) {
        Item item = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void initialize() {}
}
