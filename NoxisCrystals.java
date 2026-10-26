package com.noxisculture.entity.idle;

import com.noxisculture.block.ModBlocks;
import com.noxisculture.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Qué cosas cuentan como "cristal" para los Noxis y cómo encontrarlas cerca.
 * Solo usa bloques e ítems que ya existen (vanilla + Noxis Culture). Se usa en el SERVIDOR.
 */
public final class NoxisCrystals {
    private NoxisCrystals() {}

    /** Familia de la amatista + lo cristalino de Noxus. */
    private static final Set<Block> BLOCKS = Set.of(
            Blocks.AMETHYST_BLOCK, Blocks.BUDDING_AMETHYST, Blocks.AMETHYST_CLUSTER,
            Blocks.LARGE_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD, Blocks.SMALL_AMETHYST_BUD,
            ModBlocks.NOXUS_BLOCK, ModBlocks.NOXUS_ORE, ModBlocks.DEEPSLATE_NOXUS_ORE,
            ModBlocks.GLOWING_NOXITE_BRICKS);
    /** Ítems cristalinos (en el piso o en la mano de un jugador). Los bloques de arriba como ítem también cuentan. */
    private static final Set<Item> ITEMS = Set.of(
            Items.AMETHYST_SHARD, ModItems.NIXIL, ModItems.RAW_NOXUS, ModItems.NOXUS_INGOT);

    public static boolean isCrystal(BlockState state) {
        return BLOCKS.contains(state.getBlock());
    }

    public static boolean isCrystal(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (ITEMS.contains(stack.getItem())) return true;
        return stack.getItem() instanceof BlockItem blockItem && BLOCKS.contains(blockItem.getBlock());
    }

    /**
     * El cristal más cercano a la vista: primero lo que un jugador sostiene en la mano,
     * después ítems tirados en el piso y por último bloques. Null si no hay nada.
     */
    public static @Nullable Vec3 findNearest(Entity entity, int radius, int radiusY) {
        Level level = entity.level();
        Vec3 eye = entity.getEyePosition();
        AABB area = entity.getBoundingBox().inflate(radius, radiusY, radius);
        Vec3 best = null;
        double bestDist = Double.MAX_VALUE;

        for (Player player : level.getEntitiesOfClass(Player.class, area, p -> !p.isSpectator())) {
            if (isCrystal(player.getMainHandItem()) || isCrystal(player.getOffhandItem())) {
                Vec3 v = new Vec3(player.getX(), player.getY() + player.getBbHeight() * 0.55D, player.getZ());
                double d = v.distanceToSqr(eye);
                if (d < bestDist) { bestDist = d; best = v; }
            }
        }
        if (best != null) return best;   // un jugador mostrándole un cristal le gana a todo

        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area, i -> isCrystal(i.getItem()))) {
            Vec3 v = item.position().add(0.0D, 0.2D, 0.0D);
            double d = v.distanceToSqr(eye);
            if (d < bestDist) { bestDist = d; best = v; }
        }
        if (best != null) return best;

        BlockPos origin = entity.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -radiusY, -radius),
                origin.offset(radius, radiusY + 1, radius))) {
            if (isCrystal(level.getBlockState(pos))) {
                Vec3 v = Vec3.atCenterOf(pos);
                double d = v.distanceToSqr(eye);
                if (d < bestDist) { bestDist = d; best = v; }
            }
        }
        return best;
    }

    /** ¿Sigue habiendo un cristal en ese lugar? (lo minaron, lo levantaron, el jugador se fue...). */
    public static boolean stillThere(Level level, Vec3 target) {
        if (isCrystal(level.getBlockState(BlockPos.containing(target)))) return true;
        AABB box = new AABB(target, target).inflate(1.5D);
        if (!level.getEntitiesOfClass(ItemEntity.class, box, i -> isCrystal(i.getItem())).isEmpty()) return true;
        for (Player player : level.getEntitiesOfClass(Player.class, box.inflate(1.0D), p -> !p.isSpectator())) {
            if (isCrystal(player.getMainHandItem()) || isCrystal(player.getOffhandItem())) return true;
        }
        return false;
    }
}
