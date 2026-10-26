package com.noxisculture.block.custom;

import com.mojang.serialization.MapCodec;
import com.noxisculture.entity.light.NoxisLightSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Bloque de luz invisible que acompaña a los Noxis.
 * No se ve, no tiene colisión, no se puede apuntar, se reemplaza al construir
 * y se AUTOLIMPIA: si no hay un Noxis cerca, desaparece solo en medio segundo.
 */
public class NoxisLightBlock extends Block {
    public static final MapCodec<NoxisLightBlock> CODEC = simpleCodec(NoxisLightBlock::new);
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 1, 15);
    private static final int CHECK_DELAY_TICKS = 10;

    public NoxisLightBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 6));
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, CHECK_DELAY_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean noxisNearby = !level.getEntities((Entity) null, new AABB(pos).inflate(1.5D),
                entity -> entity instanceof NoxisLightSource).isEmpty();
        if (noxisNearby) {
            level.scheduleTick(pos, this, CHECK_DELAY_TICKS);
        } else {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
