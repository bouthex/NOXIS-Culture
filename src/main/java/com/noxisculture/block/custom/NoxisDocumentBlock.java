package com.noxisculture.block.custom;

import com.mojang.serialization.MapCodec;
import com.noxisculture.block.entity.NoxisDocumentBlockEntity;
import com.noxisculture.document.NoxisDocuments;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Documentos Noxis apoyados sobre una superficie: un libro cerrado, un cuaderno o una pila de hojas.
 *
 * <ul>
 *   <li>{@link #KIND}: libro, cuaderno u hoja.</li>
 *   <li>{@link #LAYERS}: cuántas hojas hay en la pila (1 a 8, como las capas de nieve; un libro o un
 *   cuaderno siempre es 1). Cada hoja suma un píxel de alto; la colisión y la selección siguen la altura
 *   real.</li>
 *   <li>La entidad del bloque guarda cada documento EXACTO (identidad y datos), en orden.</li>
 * </ul>
 * <p>Clic derecho: leerlo (en una pila, la hoja de arriba). Agachado con la mano vacía: recogerlo (en
 * una pila, la hoja de arriba). Agachado con otra hoja en la mano sobre una pila: sumar la hoja.</p>
 */
public class NoxisDocumentBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<NoxisDocumentBlock> CODEC = simpleCodec(NoxisDocumentBlock::new);
    public static final int MAX_LAYERS = 8;
    public static final IntegerProperty KIND = IntegerProperty.create("kind", 0, NoxisDocuments.KINDS - 1);
    public static final IntegerProperty LAYERS = IntegerProperty.create("layers", 1, MAX_LAYERS);

    /** Formas con el frente al norte (el lomo del libro a la izquierda, x bajo). */
    private static final Map<Direction, VoxelShape> BOOK = Shapes.rotateHorizontal(Block.box(3.5, 0, 2, 12.5, 3, 14));
    private static final Map<Direction, VoxelShape> NOTEBOOK = Shapes.rotateHorizontal(Block.box(3.5, 0, 3, 12.5, 2, 13));
    private static final VoxelShape[] SHEETS = new VoxelShape[MAX_LAYERS + 1];
    static {
        for (int i = 1; i <= MAX_LAYERS; i++) SHEETS[i] = Block.box(2.5, 0, 2.5, 13.5, i, 13.5);
    }

    public NoxisDocumentBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(KIND, NoxisDocuments.BOOK).setValue(LAYERS, 1));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, KIND, LAYERS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(KIND)) {
            case NoxisDocuments.NOTEBOOK -> NOTEBOOK.get(state.getValue(FACING));
            case NoxisDocuments.SHEET -> SHEETS[state.getValue(LAYERS)];
            default -> BOOK.get(state.getValue(FACING));
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.getShape(state, level, pos, context);
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    // ------------------------------------------------------------------ colocar

    /** Otra hoja sobre una pila de hojas (como la nieve): se suma una capa. */
    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        if (state.getValue(KIND) != NoxisDocuments.SHEET || state.getValue(LAYERS) >= MAX_LAYERS) return false;
        if (!NoxisDocuments.isDocument(stack) || NoxisDocuments.kindOf(stack) != NoxisDocuments.SHEET) return false;
        return !context.replacingClickedOnBlock() || context.getClickedFace() == Direction.UP;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState here = context.getLevel().getBlockState(context.getClickedPos());
        if (here.is(this)) return here.setValue(LAYERS, Math.min(MAX_LAYERS, here.getValue(LAYERS) + 1));
        BlockState state = this.defaultBlockState().setValue(KIND, NoxisDocuments.kindOf(context.getItemInHand()))
                .setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(LAYERS, 1);
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
        return direction == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NoxisDocumentBlockEntity(pos, state);
    }

    /** Lo apoyó un jugador: el bloque guarda ese documento exacto (en una pila, arriba de todo). */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof NoxisDocumentBlockEntity be) be.push(stack);
    }

    // ------------------------------------------------------------------ leer y recoger

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) {
            // Agachado con la mano vacía: recogerlo (de una pila, la hoja de arriba).
            if (level instanceof ServerLevel server && server.getBlockEntity(pos) instanceof NoxisDocumentBlockEntity be) {
                ItemStack taken = be.pop(state);
                int layers = state.getValue(LAYERS);
                if (layers > 1) {
                    server.setBlock(pos, state.setValue(LAYERS, layers - 1), Block.UPDATE_ALL);
                } else {
                    server.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
                if (!player.getInventory().add(taken)) player.drop(taken, false);
                level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8F, 1.1F);
            }
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide()) {
            ItemStack top = level.getBlockEntity(pos) instanceof NoxisDocumentBlockEntity be ? be.top() : ItemStack.EMPTY;
            NoxisDocuments.openReader(top.isEmpty() ? NoxisDocuments.create(state.getValue(KIND), "", -1L) : top);
        }
        return InteractionResult.SUCCESS;
    }

    /** Cae cada documento exacto (lo suelta la entidad del bloque; no hay tabla de botín). */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    /** Lo apoya un Noxis (servidor): ese documento exacto, mirando hacia {@code facing}. */
    public static boolean place(Level level, BlockPos pos, ItemStack document, Direction facing) {
        BlockState state = com.noxisculture.block.ModBlocks.NOXIS_DOCUMENT.defaultBlockState()
                .setValue(KIND, NoxisDocuments.kindOf(document)).setValue(FACING, facing).setValue(LAYERS, 1);
        if (!state.canSurvive(level, pos) || !level.getBlockState(pos).canBeReplaced()) return false;
        level.setBlock(pos, state, Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof NoxisDocumentBlockEntity be) be.push(document);
        return true;
    }
}
