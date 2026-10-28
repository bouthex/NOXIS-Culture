package com.noxisculture.item.custom;

import com.noxisculture.art.NoxisPaintings;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * La pintura Noxis como objeto. En la mano y en el inventario todas se ven igual (un cuadro envuelto,
 * sin revelar la imagen) y llevan el mismo nombre; la obra se ve al colgarla.
 *
 * <ul>
 *   <li>Sin descubrir (sin obra): se apilan normalmente (hasta 16). Al colgar UNA, solo esa unidad
 *   recibe una obra sorteada; el resto de la pila sigue sin descubrir.</li>
 *   <li>Con obra (la terminó un Noxis, o se retiró de una pared o de un atril): la conserva para
 *   siempre; se apila solo con pinturas de la misma obra (comportamiento normal de Minecraft).</li>
 * </ul>
 * <p>La obra se sortea solo en el servidor (el cliente no adivina una obra distinta).</p>
 */
public class NoxisPaintingItem extends BlockItem {
    public NoxisPaintingItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (NoxisPaintings.hasArt(stack)) return super.useOn(context);

        Level level = context.getLevel();
        BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(),
                context.getClickedPos(), context.isInside());
        BlockPlaceContext probe = new BlockPlaceContext(level, context.getPlayer(), context.getHand(), stack, hit);
        BlockState probeState = this.getBlock().getStateForPlacement(probe);
        if (!probe.canPlace() || probeState == null) return InteractionResult.FAIL;
        if (!(level instanceof ServerLevel server)) {
            // El cliente no coloca nada: espera la obra que decida el servidor.
            level.playLocalSound(probe.getClickedPos(), SoundEvents.PAINTING_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            return InteractionResult.SUCCESS;
        }
        BlockPos target = probe.getClickedPos();
        ItemStack one = stack.copyWithCount(1);
        NoxisPaintings.setArt(one, NoxisPaintings.discover(server, target, level.getRandom()));
        InteractionResult result = this.place(new BlockPlaceContext(level, context.getPlayer(), context.getHand(), one, hit));
        if (result.consumesAction() && one.isEmpty()) stack.shrink(1);     // en creativo no se gasta
        return result;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        NoxisPaintings.migrateLegacy(stack);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable(NoxisPaintings.hasArt(stack)
                ? "item.noxis_culture.noxis_painting.discovered" : "item.noxis_culture.noxis_painting.undiscovered")
                .withStyle(ChatFormatting.GRAY));
    }
}
