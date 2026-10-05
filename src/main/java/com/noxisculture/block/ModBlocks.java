package com.noxisculture.block;

import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockItemId;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
    private ModBlocks() {}

    /**
     * Ladrillo de Noxita: piedra de construcción de las aldeas Noxis.
     * Un poco más dura que los ladrillos de pizarra (3.0 vs 3.5 de deepslate bricks
     * -> la dejamos en 3.0 para que sea cómoda de minar al inicio del juego).
     */
    public static final Block NOXITE_BRICKS = register(
            ModBlockItemIds.NOXITE_BRICKS,
            Block::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.DEEPSLATE_BRICKS)
    );

    /** Registra un bloque CON ítem. */
    private static Block register(BlockItemId id, Function<BlockBehaviour.Properties, Block> factory,
                                  BlockBehaviour.Properties properties) {
        Block block = register(id.block(), factory, properties);
        BlockItem blockItem = new BlockItem(block,
                new Item.Properties().useBlockDescriptionPrefix().setId(id.item()));
        Registry.register(BuiltInRegistries.ITEM, id.item(), blockItem);
        return block;
    }

    /** Registra un bloque SIN ítem (lo usaremos para bloques técnicos más adelante). */
    private static Block register(ResourceKey<Block> key, Function<BlockBehaviour.Properties, Block> factory,
                                  BlockBehaviour.Properties properties) {
        Block block = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.BLOCK, key, block);
    }

    /** Fuerza la carga estática de la clase. */
    public static void initialize() {}
}
