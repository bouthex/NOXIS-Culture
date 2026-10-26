package com.noxisculture.item;

import com.noxisculture.block.ModBlocks;
import com.noxisculture.block.entity.NoxisHatBlockEntity;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Interacciones del sombrero con las manos (funcionan igual en supervivencia y en creativo):
 * <ul>
 *   <li><b>Teñir el lazo</b>: un tinte en una mano y el sombrero en la otra, clic derecho
 *   (al aire). Solo cambia el lazo; la copa y el ala quedan como estaban.</li>
 *   <li><b>Lavar</b>: clic derecho con el sombrero teñido sobre un caldero con agua, como las
 *   armaduras de cuero: vuelve a sus colores originales y el caldero pierde un nivel de agua.</li>
 * </ul>
 */
public final class NoxisHatInteractions {
    private NoxisHatInteractions() {
    }

    public static void register() {
        UseItemCallback.EVENT.register(NoxisHatInteractions::dyeBand);
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> wash(player, level, hand, hit.getBlockPos()));
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> dyePlacedBand(player, level, hand, hit.getBlockPos()));
    }

    private static boolean isHat(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModBlocks.NOXIS_HAT.asItem());
    }

    /** Tinte en una mano + sombrero en la otra: tiñe el lazo (mezcla como el cuero). */
    private static InteractionResult dyeBand(Player player, Level level, InteractionHand hand) {
        ItemStack used = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        ItemStack hat;
        ItemStack dyeStack;
        if (isHat(used) && NoxisHatColors.dyeOf(other) != null) {
            hat = used;
            dyeStack = other;
        } else if (isHat(other) && NoxisHatColors.dyeOf(used) != null) {
            hat = other;
            dyeStack = used;
        } else {
            return InteractionResult.PASS;
        }
        DyeColor dye = NoxisHatColors.dyeOf(dyeStack);
        if (!level.isClientSide() && dye != null) {
            NoxisHatColors.dyeBand(hat, dye);
            if (!player.getAbilities().instabuild) dyeStack.shrink(1);
            level.playSound(null, player.blockPosition(), SoundEvents.DYE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.getInventory().setChanged();
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Tinte sobre un sombrero apoyado en el piso: tiñe solo el lazo de ese sombrero (la copa, el
     * ala y la gema no cambian). No lo levanta, no lo rompe ni lo equipa.
     */
    private static InteractionResult dyePlacedBand(Player player, Level level, InteractionHand hand, BlockPos pos) {
        ItemStack dyeStack = player.getItemInHand(hand);
        DyeColor dye = NoxisHatColors.dyeOf(dyeStack);
        if (dye == null || !level.getBlockState(pos).is(ModBlocks.NOXIS_HAT)) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof NoxisHatBlockEntity be) {
            ItemStack hat = be.getHat().copy();
            NoxisHatColors.dyeBand(hat, dye);
            be.setHat(hat);
            if (!player.getAbilities().instabuild) dyeStack.shrink(1);
            level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    /** Sombrero teñido sobre un caldero con agua: lo lava (copa, ala y lazo) y gasta un nivel. */
    private static InteractionResult wash(Player player, Level level, InteractionHand hand, BlockPos pos) {
        ItemStack hat = player.getItemInHand(hand);
        if (!isHat(hat)) return InteractionResult.PASS;
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.WATER_CAULDRON)) return InteractionResult.PASS;
        if (!NoxisHatColors.hasCrownColor(hat) && !NoxisHatColors.hasBandColor(hat)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            NoxisHatColors.wash(hat);
            LayeredCauldronBlock.lowerFillLevel(state, level, pos);
            level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.6F, 1.2F);
            player.getInventory().setChanged();
        }
        return InteractionResult.SUCCESS;
    }
}
