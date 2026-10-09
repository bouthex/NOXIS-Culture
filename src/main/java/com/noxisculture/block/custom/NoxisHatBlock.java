package com.noxisculture.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * El sombrero de un Noxis apoyado en el suelo mientras duerme en su pecera. Es un bloque con la
 * forma del sombrero (mismo tamaño y alto); se puede romper y recoger como objeto. Si sigue ahí
 * cuando el Noxis se despierta, se lo vuelve a poner.
 */
public class NoxisHatBlock extends Block {
    public static final MapCodec<NoxisHatBlock> CODEC = simpleCodec(NoxisHatBlock::new);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 13, 14);

    public NoxisHatBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
