package com.noxisculture.item;

import com.noxisculture.NoxisCulture;
import com.noxisculture.block.ModBlocks;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

/**
 * Pestaña propia "Noxis Culture" en el creativo, con TODO el contenido del mod.
 * Al agregar un ítem o bloque nuevo, sumalo acá en la sección que corresponda.
 */
public final class ModCreativeTabs {
    private ModCreativeTabs() {}

    public static final ResourceKey<CreativeModeTab> NOXIS_TAB_KEY =
            ResourceKey.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(), NoxisCulture.id("noxis_culture"));

    public static final CreativeModeTab NOXIS_TAB = FabricCreativeModeTab.builder()
            .icon(() -> new ItemStack(ModItems.NOXUS_INGOT))
            .title(Component.translatable("itemGroup.noxis_culture"))
            .displayItems((params, output) -> {
                // Criaturas
                output.accept(ModItems.NOXIS_VILLAGER_SPAWN_EGG);
                // Construcción
                output.accept(ModBlocks.NOXITE_BRICKS.asItem());
                output.accept(ModBlocks.GLOWING_NOXITE_BRICKS.asItem());
                // Noxus: de la mena a la gema
                output.accept(ModBlocks.NOXUS_ORE.asItem());
                output.accept(ModBlocks.DEEPSLATE_NOXUS_ORE.asItem());
                output.accept(ModItems.RAW_NOXUS);
                output.accept(ModItems.NOYUX_BUCKET);
                output.accept(ModItems.NOXUS_INGOT);
                output.accept(ModBlocks.NOXUS_BLOCK.asItem());
                // Materiales y herramientas
                output.accept(ModItems.NIXIL);
                output.accept(ModItems.COSMOS_PICKAXE);
            })
            .build();

    public static void initialize() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, NOXIS_TAB_KEY, NOXIS_TAB);

        // El huevo también va en la pestaña vanilla de huevos, donde la gente lo busca.
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
                .register(tab -> tab.accept(ModItems.NOXIS_VILLAGER_SPAWN_EGG));
    }
}
