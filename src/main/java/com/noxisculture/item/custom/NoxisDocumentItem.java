package com.noxisculture.item.custom;

import com.noxisculture.document.NoxisDocuments;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Libro, cuaderno u hoja Noxis en la mano. Clic derecho: leerlo (pantalla de lectura; las páginas
 * todavía están en blanco). Agachado + clic derecho sobre una superficie: apoyarlo en el mundo (una hoja
 * sobre otra hoja suma una capa a la pila).
 */
public class NoxisDocumentItem extends BlockItem {
    public NoxisDocumentItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            NoxisDocuments.openReader(stack);
            player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.7F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isSecondaryUseActive()) return super.useOn(context);
        if (player == null) return InteractionResult.PASS;
        return this.use(context.getLevel(), player, context.getHand());
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.noxis_culture.noxis_document.hint_read").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("item.noxis_culture.noxis_document.hint_place").withStyle(ChatFormatting.GRAY));
    }
}
