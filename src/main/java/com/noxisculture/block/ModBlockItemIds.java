package com.noxisculture.block;

import com.noxisculture.NoxisCulture;
import net.minecraft.resources.Identifier;
import net.minecraft.references.BlockItemId;

/**
 * IDs de bloques que tienen ítem. Desde 26.2 Mojang separa los IDs de las
 * instancias (ver BlockItemIds en vanilla); el datagen trabaja con estas claves.
 */
public final class ModBlockItemIds {
    private ModBlockItemIds() {}

    public static final BlockItemId NOXITE_BRICKS = create("noxite_bricks");

    private static BlockItemId create(String name) {
        Identifier id = NoxisCulture.id(name);
        return BlockItemId.create(id, id);
    }
}
