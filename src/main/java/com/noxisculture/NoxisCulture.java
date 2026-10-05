package com.noxisculture;

import com.noxisculture.block.ModBlocks;
import com.noxisculture.entity.ModEntityTypes;
import com.noxisculture.item.ModCreativeTabs;
import com.noxisculture.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Punto de entrada común (cliente + servidor) de Noxis Culture.
 * Cada sistema se inicializa desde su propia clase para que, al portar
 * de versión, los cambios queden aislados en un solo archivo.
 */
public final class NoxisCulture implements ModInitializer {
    public static final String MOD_ID = "noxis_culture";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // El orden importa: los bloques registran sus BlockItems antes que el resto de ítems.
        ModBlocks.initialize();
        ModItems.initialize();
        ModEntityTypes.initialize();
        ModCreativeTabs.initialize();
        LOGGER.info("Noxis Culture: los Noxis llegaron al mundo.");
    }

    /** Helper único para todos los IDs del mod. */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
