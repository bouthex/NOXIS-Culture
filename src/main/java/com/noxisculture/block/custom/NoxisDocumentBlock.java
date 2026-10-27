package com.noxisculture.block.custom;

import com.mojang.serialization.MapCodec;
import com.noxisculture.block.entity.NoxisDocumentBlockEntity;
import com.noxisculture.document.NoxisDocuments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Un documento Noxis apoyado (libro cerrado, cuaderno u hoja) sobre una superficie. Modelos 3D con
 * tapas, lomo y bloque de páginas; las páginas están en blanco (el idioma llega en otra etapa).
 * Guarda el objeto exacto en su {@link NoxisDocumentBlockEntity}: al romperlo cae el mismo documento.
 */
public class NoxisDocumentBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<NoxisDocumentBlock> CODEC = simpleCodec(NoxisDocumentBlock::new);
    public static final IntegerProperty KIND = IntegerProperty.create("kind", 0, NoxisDocuments.KINDS - 1);
    private static final VoxelShape BOOK = Block.box(3, 0, 3, 13, 3, 13);
    private static final VoxelShape NOTEBOOK = Block.box(4, 0, 3, 12, 2, 13);
    private static final VoxelShape SHEET = Block.box(3, 0, 3, 13, 1, 13);

    public NoxisDocumentBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(KIND, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, KIND);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(KIND)) {
            case NoxisDocuments.NOTEBOOK -> NOTEBOOK;
            case NoxisDocuments.SHEET -> SHEET;
            default -> BOOK;
        };
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState().setValue(KIND, NoxisDocuments.kindOf(context.getItemInHand()))
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return state.canSurvive(level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NoxisDocumentBlockEntity(pos, state);
    }

    /** Lo apoyó un jugador: el bloque guarda ese documento exacto. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof NoxisDocumentBlockEntity be) be.setDocument(stack);
    }

    /** Lo apoya un Noxis (servidor): ese documento exacto, mirando hacia {@code facing}. */
    public static boolean place(Level level, BlockPos pos, ItemStack document, Direction facing) {
        BlockState state = com.noxisculture.block.ModBlocks.NOXIS_DOCUMENT.defaultBlockState()
                .setValue(KIND, NoxisDocuments.kindOf(document)).setValue(FACING, facing);
        if (!state.canSurvive(level, pos) || !level.getBlockState(pos).canBeReplaced()) return false;
        level.setBlock(pos, state, Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof NoxisDocumentBlockEntity be) be.setDocument(document);
        return true;
    }
}
