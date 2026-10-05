package com.noxisculture.tag;

import com.noxisculture.NoxisCulture;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Referencias a tags del mod. El contenido real vive en data/noxis_culture/tags. */
public final class ModTags {
    private ModTags() {}

    public static final class Blocks {
        /** Bloques que el material Cosmos NO puede cosechar (hereda del diamante). */
        public static final TagKey<Block> INCORRECT_FOR_COSMOS_TOOL = create("incorrect_for_cosmos_tool");

        private static TagKey<Block> create(String name) {
            return TagKey.create(Registries.BLOCK, NoxisCulture.id(name));
        }
    }

    public static final class Items {
        /** Ítems que reparan herramientas Cosmos en el yunque. */
        public static final TagKey<Item> REPAIRS_COSMOS_TOOL = create("repairs_cosmos_tool");

        private static TagKey<Item> create(String name) {
            return TagKey.create(Registries.ITEM, NoxisCulture.id(name));
        }
    }
}
