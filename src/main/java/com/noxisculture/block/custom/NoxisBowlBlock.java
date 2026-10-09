package com.noxisculture.block.custom;

import com.mojang.serialization.MapCodec;
import com.noxisculture.entity.ai.NoxisBowlSleeper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Pecera Noxis: donde duermen los Noxis (para ellos reemplaza a las camas; las camas de los
 * aldeanos vanilla no cambian). Base de madera y vidrio con forma de pecera.
 *
 * <p>{@link #OCCUPIED} guarda si está ocupada (o reservada por un Noxis que va en camino): una
 * pecera admite UN solo Noxis. Se guarda con el mundo. Si quedara marcada como ocupada sin
 * nadie que la use (por ejemplo, tras un cierre inesperado), se libera sola.</p>
 */
public class NoxisBowlBlock extends Block {
    public static final MapCodec<NoxisBowlBlock> CODEC = simpleCodec(NoxisBowlBlock::new);
    public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;

    /** Contorno: la pecera entera (para apuntarla y romperla). */
    private static final VoxelShape OUTLINE = Shapes.or(
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(1, 2, 1, 15, 18, 15),
            Block.box(2, 18, 2, 14, 22, 14));
    /** Choque para todos: base + paredes de vidrio (no se puede caminar adentro). */
    private static final VoxelShape WALLS = Shapes.or(
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(0, 2, 0, 16, 16, 1), Block.box(0, 2, 15, 16, 16, 16),
            Block.box(0, 2, 0, 1, 16, 16), Block.box(15, 2, 0, 16, 16, 16));
    /** Choque para el Noxis que duerme adentro: solo el piso de madera. */
    private static final VoxelShape FLOOR = Block.box(0, 0, 0, 16, 2, 16);

    public NoxisBowlBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(OCCUPIED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OCCUPIED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext ecc && ecc.getEntity() instanceof NoxisBowlSleeper) {
            return FLOOR;
        }
        return WALLS;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(OCCUPIED);
    }

    /** Autolimpieza: ocupada pero sin ningún Noxis que la tenga como suya cerca → se libera. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(OCCUPIED)) return;
        boolean owned = !level.getEntities((Entity) null, new AABB(pos).inflate(32.0D),
                e -> e instanceof NoxisBowlSleeper s && pos.equals(s.getBowlPos())).isEmpty();
        if (!owned) {
            level.setBlock(pos, state.setValue(OCCUPIED, false), Block.UPDATE_ALL);
        }
    }
}
