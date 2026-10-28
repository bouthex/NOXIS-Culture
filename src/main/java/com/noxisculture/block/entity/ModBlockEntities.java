package com.noxisculture.block.entity;

import com.noxisculture.NoxisCulture;
import com.noxisculture.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static final BlockEntityType<NoyuxCauldronBlockEntity> NOYUX_CAULDRON =
            register("noyux_cauldron", NoyuxCauldronBlockEntity::new, ModBlocks.NOYUX_CAULDRON);

    /** El sombrero apoyado en el piso: guarda el sombrero exacto (colores, desgaste...). */
    public static final BlockEntityType<NoxisHatBlockEntity> NOXIS_HAT =
            register("noxis_hat", NoxisHatBlockEntity::new, ModBlocks.NOXIS_HAT);

    /** Documento apoyado: guarda el documento exacto (identidad y datos). */
    public static final BlockEntityType<NoxisDocumentBlockEntity> NOXIS_DOCUMENT =
            register("noxis_document", NoxisDocumentBlockEntity::new, ModBlocks.NOXIS_DOCUMENT);

    /** Atril: quién pinta la obra, cuánto avanzó y cuándo pintó por última vez. */
    public static final BlockEntityType<NoxisEaselBlockEntity> NOXIS_EASEL =
            register("noxis_easel", NoxisEaselBlockEntity::new, ModBlocks.NOXIS_EASEL);

    private static <T extends BlockEntity> BlockEntityType<T> register(
            String name, FabricBlockEntityTypeBuilder.Factory<? extends T> factory, Block... blocks) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, NoxisCulture.id(name),
                FabricBlockEntityTypeBuilder.<T>create(factory, blocks).build());
    }

    public static void initialize() {}
}
