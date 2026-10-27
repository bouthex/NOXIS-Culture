package com.noxisculture.block.custom;

import com.mojang.serialization.MapCodec;
import com.noxisculture.art.NoxisPaintings;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Una pintura Noxis colocada en el mundo: colgada en una pared o apoyada en un caballetito sobre el
 * piso. Es un bloque (no una entidad): no pesa, se guarda solo con el mundo y no se duplica.
 *
 * <ul>
 *   <li>{@link #FACING}: hacia dónde mira la obra.</li>
 *   <li>{@link #STANDING}: en caballete (piso) o colgada (pared).</li>
 *   <li>{@link #ART}: el número de la obra (su identidad; ver {@link NoxisPaintings}).</li>
 * </ul>
 * <p>Al romperla (o si se rompe la pared o el piso) cae la MISMA obra como objeto. No tiene colisión:
 * no traba el paso de nadie.</p>
 */
public class NoxisPaintingBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<NoxisPaintingBlock> CODEC = simpleCodec(NoxisPaintingBlock::new);
    public static final BooleanProperty STANDING = BooleanProperty.create("standing");
    public static final IntegerProperty ART = IntegerProperty.create("art", 0, NoxisPaintings.COUNT - 1);

    private static final VoxelShape WALL_NORTH = Block.box(0, 0, 15, 16, 16, 16);
    private static final VoxelShape WALL_SOUTH = Block.box(0, 0, 0, 16, 16, 1);
    private static final VoxelShape WALL_EAST = Block.box(0, 0, 0, 1, 16, 16);
    private static final VoxelShape WALL_WEST = Block.box(15, 0, 0, 16, 16, 16);
    private static final VoxelShape EASEL = Block.box(2, 0, 4, 14, 16, 12);

    public NoxisPaintingBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(STANDING, false).setValue(ART, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STANDING, ART);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(STANDING)) return EASEL;
        return switch (state.getValue(FACING)) {
            case SOUTH -> WALL_SOUTH;
            case EAST -> WALL_EAST;
            case WEST -> WALL_WEST;
            default -> WALL_NORTH;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** Estado para colgar o apoyar la obra {@code art} mirando hacia {@code facing}. */
    public BlockState placed(int art, Direction facing, boolean standing) {
        return this.defaultBlockState().setValue(ART, Math.max(0, Math.min(NoxisPaintings.COUNT - 1, art)))
                .setValue(FACING, facing).setValue(STANDING, standing);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        int art = NoxisPaintings.artOf(context.getItemInHand());
        Direction face = context.getClickedFace();
        BlockState state;
        if (face.getAxis().isHorizontal()) {
            state = this.placed(art, face, false);                                  // en la pared, mirando afuera
        } else if (face == Direction.UP) {
            state = this.placed(art, context.getHorizontalDirection().getOpposite(), true);   // en caballete, mirando al jugador
        } else {
            return null;
        }
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(STANDING)) {
            BlockPos below = pos.below();
            return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
        }
        Direction facing = state.getValue(FACING);
        BlockPos wall = pos.relative(facing.getOpposite());
        return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return state.canSurvive(level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    /** Cae la misma obra (no hay tabla de botín: la identidad está en el bloque). */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(NoxisPaintings.stack(state.getValue(ART), "", -1L));
    }
}
