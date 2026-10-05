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
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS)
                .register(tab -> tab.accept(ModBlocks.NOXITE_BRICKS.asItem()));

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(tab -> tab.accept(ModItems.COSMOS_PICKAXE));
    }
}
