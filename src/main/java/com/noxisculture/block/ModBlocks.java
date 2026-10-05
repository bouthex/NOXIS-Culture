package com.noxisculture.block;

import com.noxisculture.block.custom.NoyuxCauldronBlock;
import com.noxisculture.fluid.ModFluids;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.references.BlockItemId;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
    private ModBlocks() {}

    /** Luz de los ladrillos agrietados: tenue, de ambiente (antorcha = 14). */
    private static final int GLOWING_BRICKS_LIGHT = 7;

    // ---------------- Construcción ----------------

    /** Ladrillos de Noxita: lisos, sin vetas de luz. */
    public static final Block NOXITE_BRICKS = register(
            ModBlockItemIds.NOXITE_BRICKS, Block::new, noxiteBrickProperties());

    /** Ladrillos de Noxita Resplandecientes: grietas violetas que emiten luz suave. */
    public static final Block GLOWING_NOXITE_BRICKS = register(
            ModBlockItemIds.GLOWING_NOXITE_BRICKS, Block::new,
            noxiteBrickProperties().lightLevel(state -> GLOWING_BRICKS_LIGHT));

    // ---------------- Mineral Noxus ----------------
    // Igual de duro que el diamante y da 4-8 de experiencia al minarlo.

    public static final Block NOXUS_ORE = register(
            ModBlockItemIds.NOXUS_ORE,
            props -> new DropExperienceBlock(UniformInt.of(4, 8), props),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 3.0F));

    public static final Block DEEPSLATE_NOXUS_ORE = register(
            ModBlockItemIds.DEEPSLATE_NOXUS_ORE,
            props -> new DropExperienceBlock(UniformInt.of(4, 8), props),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(4.5F, 3.0F)
                    .sound(SoundType.DEEPSLATE));

    /** Bloque de Noxus: 9 gemas compactadas; piedra fraccionada con un ojo de luz corrupta. */
    public static final Block NOXUS_BLOCK = register(
            ModBlockItemIds.NOXUS_BLOCK, Block::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 5)); // el ojo corrupto ilumina un poco

    // ---------------- Técnicos (sin ítem) ----------------

    /** Caldero con Noyux: brilla violeta y transmuta el Noxus en bruto. */
    public static final Block NOYUX_CAULDRON = register(
            ModBlockIds.NOYUX_CAULDRON, NoyuxCauldronBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(2.0F)
                    .noOcclusion()
                    .lightLevel(state -> 6));

    /** El líquido Noyux colocado en el mundo. */
    public static final Block NOYUX = register(
            ModBlockIds.NOYUX,
            props -> new LiquidBlock(ModFluids.NOYUX, props),
            BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).lightLevel(state -> 6));

    private static BlockBehaviour.Properties noxiteBrickProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(3.0F, 6.0F)
                .sound(SoundType.DEEPSLATE_BRICKS);
    }

    /** Registra un bloque CON ítem. */
    private static Block register(BlockItemId id, Function<BlockBehaviour.Properties, Block> factory,
                                  BlockBehaviour.Properties properties) {
        Block block = register(id.block(), factory, properties);
        BlockItem blockItem = new BlockItem(block,
                new Item.Properties().useBlockDescriptionPrefix().setId(id.item()));
        Registry.register(BuiltInRegistries.ITEM, id.item(), blockItem);
        return block;
    }

    /** Registra un bloque SIN ítem (para bloques técnicos, como la pecera). */
    private static Block register(ResourceKey<Block> key, Function<BlockBehaviour.Properties, Block> factory,
                                  BlockBehaviour.Properties properties) {
        Block block = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.BLOCK, key, block);
    }

    public static void initialize() {}
}
