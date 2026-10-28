package com.noxisculture.block.custom;

import com.mojang.serialization.MapCodec;
import com.noxisculture.art.NoxisPaintings;
import com.noxisculture.block.ModBlocks;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Una pintura Noxis colgada en una pared. Es un bloque fino (no una entidad): se guarda con el mundo,
 * no pesa y no se duplica.
 *
 * <ul>
 *   <li>{@link #FACING}: hacia dónde mira la obra (la pared queda detrás).</li>
 *   <li>{@link #ART}: el número de la obra (su identidad; ver {@link NoxisPaintings}).</li>
 * </ul>
 * <p>Se cuelga haciendo clic derecho sobre la cara lateral de un bloque firme, como en Minecraft. Si
 * la pared desaparece, la pintura cae como objeto con LA MISMA obra; al romperla, también. Sin
 * colisión: no traba el paso.</p>
 */
public class NoxisPaintingBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<NoxisPaintingBlock> CODEC = simpleCodec(NoxisPaintingBlock::new);
    public static final IntegerProperty ART = IntegerProperty.create("art", 0, NoxisPaintings.COUNT - 1);

    private static final VoxelShape WALL_NORTH = Block.box(0, 0, 14, 16, 16, 16);
    private static final VoxelShape WALL_SOUTH = Block.box(0, 0, 0, 16, 16, 2);
    private static final VoxelShape WALL_EAST = Block.box(0, 0, 0, 2, 16, 16);
    private static final VoxelShape WALL_WEST = Block.box(14, 0, 0, 16, 16, 16);

    public NoxisPaintingBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ART, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ART);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
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

    /** Estado para colgar la obra {@code art} mirando hacia {@code facing}. */
    public BlockState placed(int art, Direction facing) {
        return this.defaultBlockState().setValue(ART, Math.max(0, Math.min(NoxisPaintings.COUNT - 1, art)))
                .setValue(FACING, facing);
    }

    /**
     * Solo en paredes (cara lateral de un bloque firme). La obra la decide {@code NoxisPaintingItem}
     * (la que ya tiene, o una sorteada si es nueva), así que acá se usa la del objeto.
     */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (!face.getAxis().isHorizontal()) return null;
        int art = NoxisPaintings.artOf(context.getItemInHand());
        BlockState state = this.placed(Math.max(0, art), face);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
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
        return List.of(NoxisPaintings.stack(state.getValue(ART)));
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return NoxisPaintings.stack(state.getValue(ART));
    }

    /** La obra que se ve en ese bloque (pintura colgada o terminada en un atril), o -1. */
    public static int artAt(BlockState state) {
        if (state.is(ModBlocks.NOXIS_PAINTING)) return state.getValue(ART);
        if (state.is(ModBlocks.NOXIS_EASEL) && state.getValue(NoxisEaselBlock.CANVAS) == NoxisEaselBlock.CANVAS_DONE) {
            return state.getValue(NoxisEaselBlock.ART);
        }
        return -1;
    }
}
