package com.noxisculture.item;

import com.noxisculture.tag.ModTags;
import net.minecraft.world.item.ToolMaterial;

/**
 * Materiales de herramientas del mod.
 * Referencia vanilla: DIAMOND = 1561 dur / 8.0 vel / 3.0 daño / 10 encant.
 *                     NETHERITE = 2031 dur / 9.0 vel / 4.0 daño / 15 encant.
 * Cosmos se ubica entre ambos: mejor que diamante, sin llegar a netherite.
 */
public final class ModToolMaterials {
    private ModToolMaterials() {}

    public static final ToolMaterial COSMOS = new ToolMaterial(
            ModTags.Blocks.INCORRECT_FOR_COSMOS_TOOL, // mismo nivel de cosecha que diamante
            1800,  // durabilidad
            8.5F,  // velocidad de minado
            3.0F,  // bonus de daño
            18,    // encantabilidad (alta: los Noxis "afinan" sus herramientas)
            ModTags.Items.REPAIRS_COSMOS_TOOL
    );
}
