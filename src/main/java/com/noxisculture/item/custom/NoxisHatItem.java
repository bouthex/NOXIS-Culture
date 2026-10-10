package com.noxisculture.item.custom;

import com.noxisculture.item.NoxisHatColors;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * El Sombrero de Noxis como objeto: se equipa en la ranura de casco (protege como un casco de
 * oro), se puede apoyar en el piso como bloque (clic derecho sobre un bloque) y se tiñe.
 *
 * <p><b>Teñir desde el inventario</b> (con un tinte agarrado con el mouse, como el que se usa
 * para apilar):</p>
 * <ul>
 *   <li><b>Clic izquierdo</b> sobre el sombrero: tiñe la <b>copa y el ala</b>.</li>
 *   <li><b>Clic derecho</b> sobre el sombrero: tiñe el <b>lazo</b>.</li>
 * </ul>
 * <p>Cada clic usa un tinte y lo mezcla con el color que ya tenía (igual que el cuero), así se
 * pueden ir armando tonos. La copa y el ala también se tiñen en la mesa de crafteo con la receta
 * vanilla de las armaduras de cuero (sombrero + tintes). La gema nunca cambia.</p>
 */
public class NoxisHatItem extends BlockItem {
    public NoxisHatItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack hat, ItemStack other, Slot slot, ClickAction action,
                                            Player player, SlotAccess access) {
        DyeColor dye = NoxisHatColors.dyeOf(other);
        if (dye == null || hat.getCount() != 1) return false;
        if (action == ClickAction.SECONDARY) {
            NoxisHatColors.dyeBand(hat, dye);
        } else {
            NoxisHatColors.dyeCrown(hat, dye);
        }
        other.shrink(1);
        slot.setChanged();
        player.level().playSound(player, player.blockPosition(), SoundEvents.DYE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }
}
