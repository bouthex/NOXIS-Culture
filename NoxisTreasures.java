package com.noxisculture.entity.nature;

import com.noxisculture.entity.ai.NoxisCollectFlowerGoal;
import com.noxisculture.entity.idle.NoxisCrystals;
import java.util.Set;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Qué cosas aprecia un Noxis (lo único que guarda en su inventario privado): flores, cristales y
 * cosas de Noxus, y los regalos que recibió. Todo lo demás (espadas, herramientas, armas...) no
 * le interesa: si lo tuviera, lo guarda en un cofre cercano o lo deja en el piso.
 */
public final class NoxisTreasures {
    private NoxisTreasures() {}

    /** ¿Lo aprecia? (independiente de la personalidad: todas guardan lo mismo). */
    public static boolean isTreasure(ItemStack stack, Set<Item> giftMemory) {
        if (stack.isEmpty()) return false;
        return NoxisCollectFlowerGoal.isFlowerItem(stack) || NoxisCrystals.isCrystal(stack)
                || giftMemory.contains(stack.getItem());
    }

    /** ¿Despierta un interés especial al verlo? (cristales, Noxus y flores). */
    public static boolean isSpecial(ItemStack stack) {
        return NoxisCrystals.isCrystal(stack) || NoxisCollectFlowerGoal.isFlowerItem(stack);
    }
}
