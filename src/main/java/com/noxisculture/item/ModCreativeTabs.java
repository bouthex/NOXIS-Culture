package com.noxisculture.item;

import com.noxisculture.block.ModBlocks;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;

/**
 * Por ahora agregamos los ítems a pestañas vanilla.
 * Cuando haya más contenido, aquí mismo crearemos la pestaña propia "Noxis Culture".
 */
public final class ModCreativeTabs {
    private ModCreativeTabs() {}

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(tab -> {
            tab.accept(ModBlocks.NOXITE_BRICKS.asItem());
            tab.accept(ModBlocks.GLOWING_NOXITE_BRICKS.asItem());
            tab.accept(ModBlocks.NOXUS_BLOCK.asItem());
        });

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(tab -> {
            tab.accept(ModBlocks.NOXUS_ORE.asItem());
            tab.accept(ModBlocks.DEEPSLATE_NOXUS_ORE.asItem());
        });

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(tab -> {
            tab.accept(ModItems.RAW_NOXUS);
            tab.accept(ModItems.NOXUS_INGOT);
            tab.accept(ModItems.NIXIL);
            tab.accept(ModItems.NOYUX_BUCKET);
        });

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
                .register(tab -> tab.accept(ModItems.NOXIS_VILLAGER_SPAWN_EGG));

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(tab -> tab.accept(ModItems.COSMOS_PICKAXE));
    }
}
