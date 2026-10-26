package com.noxisculture.item.custom;

import com.noxisculture.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;

/**
 * Cubo de Noyux: se vuelca en el piso como cualquier líquido (lo maneja BucketItem)
 * o, si apuntás a un caldero vacío, lo llena de Noyux.
 */
public class NoyuxBucketItem extends BucketItem {
    public NoyuxBucketItem(Fluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(Blocks.CAULDRON)) {
            return super.useOn(context);
        }
        if (!level.isClientSide()) {
            level.setBlockAndUpdate(pos, ModBlocks.NOYUX_CAULDRON.defaultBlockState());
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 1.0F, 0.8F);
            Player player = context.getPlayer();
            if (player != null) {
                ItemStack stack = context.getItemInHand();
                player.setItemInHand(context.getHand(),
                        ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
