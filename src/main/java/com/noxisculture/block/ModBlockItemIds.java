package com.noxisculture.block;

import com.noxisculture.NoxisCulture;
import net.minecraft.references.BlockItemId;
import net.minecraft.resources.Identifier;

/**
 * IDs de bloques que tienen ítem. Desde 26.2 Mojang separa los IDs de las
 * instancias (ver BlockItemIds en vanilla); el datagen trabaja con estas claves.
 */
public final class ModBlockItemIds {
    private ModBlockItemIds() {}

    // Construcción
    public static final BlockItemId NOXITE_BRICKS = create("noxite_bricks");
    public static final BlockItemId GLOWING_NOXITE_BRICKS = create("glowing_noxite_bricks");

    // Mineral Noxus
    public static final BlockItemId NOXUS_ORE = create("noxus_ore");
    public static final BlockItemId DEEPSLATE_NOXUS_ORE = create("deepslate_noxus_ore");
    public static final BlockItemId NOXUS_BLOCK = create("noxus_block");

    // Vida Noxis
    public static final BlockItemId NOXIS_BOWL = create("noxis_bowl");
    public static final BlockItemId NOXIS_HAT = create("noxis_hat");

    private static BlockItemId create(String name) {
        Identifier id = NoxisCulture.id(name);
        return BlockItemId.create(id, id);
    }
}
