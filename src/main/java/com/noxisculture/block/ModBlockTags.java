package com.noxisculture.block;

import com.noxisculture.NoxisCulture;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModBlockTags {
    private ModBlockTags() {}

    /**
     * Piedras que el Noyux puede quemar (data/noxis_culture/tags/block/noyux_burnable.json).
     * Otros mods (o un datapack) pueden sumar sus piedras agregándolas a esta etiqueta; también se
     * aceptan, si existen, las etiquetas comunes c:stones y c:cobblestones.
     */
    public static final TagKey<Block> NOYUX_BURNABLE = TagKey.create(Registries.BLOCK, NoxisCulture.id("noyux_burnable"));
}
