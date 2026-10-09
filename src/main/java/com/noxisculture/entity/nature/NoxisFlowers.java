package com.noxisculture.entity.nature;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * Qué plantas le interesan a un Noxis. Usa solo bloques y tags vanilla que ya existen
 * (no agrega contenido).
 */
public final class NoxisFlowers {
    private NoxisFlowers() {}

    /** Sus favoritas: amarillas y violetas. */
    private static final Set<Block> FAVORITES = Set.of(
            Blocks.DANDELION, Blocks.SUNFLOWER,      // amarillas
            Blocks.ALLIUM, Blocks.LILAC);            // violetas
    /** Plantas (no flores) que hacen "lindo" un lugar para sentarse a mirar. */
    private static final Set<Block> PLANTS = Set.of(
            Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN,
            Blocks.OAK_SAPLING, Blocks.SPRUCE_SAPLING, Blocks.BIRCH_SAPLING, Blocks.JUNGLE_SAPLING,
            Blocks.ACACIA_SAPLING, Blocks.DARK_OAK_SAPLING, Blocks.CHERRY_SAPLING);

    /** Cualquier flor vanilla plantada (para mirar u olfatear). */
    public static boolean isFlower(BlockState state) {
        return state.is(BlockTags.FLOWERS);
    }

    /**
     * Flores que puede recoger y volver a plantar tal cual: las de un solo bloque.
     * Las altas (dos bloques) solo se miran u olfatean; la rosa marchita, ni se toca.
     */
    public static boolean isPickable(BlockState state) {
        return state.is(BlockTags.SMALL_FLOWERS) && !state.is(Blocks.WITHER_ROSE)
                && state.getBlock().asItem() != net.minecraft.world.item.Items.AIR;
    }

    public static boolean isFavorite(BlockState state) {
        return FAVORITES.contains(state.getBlock());
    }

    /** Flores, retoños o pastito: lo que hace que un lugar sea lindo para contemplar. */
    public static boolean isNature(BlockState state) {
        return isFlower(state) || PLANTS.contains(state.getBlock());
    }
}
