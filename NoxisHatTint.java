package com.noxisculture.client.armor;

import com.noxisculture.item.NoxisHatColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Colores del sombrero apoyado en el piso (el bloque): la copa/ala (tinte 0) y el lazo
 * (tinte 1) salen del sombrero exacto que guarda el bloque. La ramita y la gema no se tiñen.
 */
public class NoxisHatTint implements BlockTintSource {
    private final boolean crown;

    public NoxisHatTint(boolean crown) {
        this.crown = crown;
    }

    @Override
    public int color(BlockState state) {
        return 0xFF000000 | (this.crown ? NoxisHatColors.DEFAULT_CROWN : NoxisHatColors.DEFAULT_BAND);
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        if (level.getBlockEntityRenderData(pos) instanceof int[] colors && colors.length >= 2) {
            return 0xFF000000 | (this.crown ? colors[0] : colors[1]);
        }
        return this.color(state);
    }
}
