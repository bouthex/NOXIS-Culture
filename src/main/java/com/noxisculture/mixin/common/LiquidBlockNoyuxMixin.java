package com.noxisculture.mixin.common;

import com.noxisculture.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * El Noyux está en la etiqueta de fluidos {@code minecraft:lava} para que las criaturas y el jugador se
 * muevan en él como en la lava (flotar, subir, salir). Esa etiqueta también haría que, al tocar agua,
 * Minecraft lo convierta en obsidiana o roca como a la lava. El Noyux no hace eso: sigue esparciéndose
 * normalmente. Es opcional: si algún día el método cambia, el enganche simplemente no se aplica.
 */
@Mixin(LiquidBlock.class)
public abstract class LiquidBlockNoyuxMixin {
    @Inject(method = "shouldSpreadLiquid(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void noxisCulture$noyuxKeepsSpreading(Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this == ModBlocks.NOYUX) {
            cir.setReturnValue(true);
        }
    }
}
