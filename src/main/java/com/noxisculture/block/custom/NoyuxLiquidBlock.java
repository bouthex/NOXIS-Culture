package com.noxisculture.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * El bloque del Noyux en el mundo. Es el líquido de siempre ({@link LiquidBlock}); lo único que
 * agrega es que, al aparecer en un lugar (volcado con el balde o al fluir), revisa una sola vez,
 * un ratito después, si toca piedra quemable y la enciende. Después solo lo vuelve a intentar de a
 * ratos con los ticks al azar del propio Noyux (nada se revisa cada tick).
 */
public class NoyuxLiquidBlock extends LiquidBlock {
    private static final float CONTACT_CHANCE = 0.5F;
    private static final int MAX_PER_CHECK = 2;

    public NoyuxLiquidBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(this)) {
            level.scheduleTick(pos, this, 10 + level.getRandom().nextInt(30));
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        NoyuxBurning.igniteAround(level, pos, random, CONTACT_CHANCE, MAX_PER_CHECK);
    }
}
