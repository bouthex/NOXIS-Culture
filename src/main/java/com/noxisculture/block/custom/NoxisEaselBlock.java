package com.noxisculture.block.custom;

import com.mojang.serialization.MapCodec;
import com.noxisculture.art.NoxisPaintings;
import com.noxisculture.block.ModBlocks;
import com.noxisculture.block.entity.NoxisEaselBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
 * Atril de pintura. Lo pueden usar los jugadores (para exhibir una pintura) y los Noxis artistas (para
 * pintar: dejan la obra a medio hacer y vuelven más tarde a continuarla).
 *
 * <ul>
 *   <li>{@link #CANVAS}: 0 sin lienzo · 1 lienzo en blanco · 2 boceto · 3 color · 4 obra terminada.</li>
 *   <li>{@link #ART}: qué obra es (cuando hay lienzo con obra).</li>
 *   <li>La entidad del bloque guarda quién la está pintando, cuánto avanzó y cuándo pintó por última
 *   vez (ver {@link NoxisEaselBlockEntity}).</li>
 * </ul>
 * <p>Clic derecho con una pintura sobre un atril vacío: la apoya (si era una pintura sin descubrir, ahí
 * se descubre su obra). Clic derecho sobre una obra terminada: la retira (el atril queda). Una obra que
 * un Noxis todavía está pintando no se puede retirar, salvo que la haya abandonado (3 días sin volver).
 * Al romper el atril cae el atril y, si había una obra empezada, la pintura con esa misma obra.</p>
 */
public class NoxisEaselBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<NoxisEaselBlock> CODEC = simpleCodec(NoxisEaselBlock::new);
    public static final int CANVAS_NONE = 0;
    public static final int CANVAS_BLANK = 1;
    public static final int CANVAS_SKETCH = 2;
    public static final int CANVAS_COLOR = 3;
    public static final int CANVAS_DONE = 4;
    public static final IntegerProperty CANVAS = IntegerProperty.create("canvas", 0, 4);
    public static final IntegerProperty ART = IntegerProperty.create("art", 0, NoxisPaintings.COUNT - 1);

    /** Con el frente (el lado del lienzo) hacia el norte. */
    private static final Map<Direction, VoxelShape> OUTLINE = Shapes.rotateHorizontal(Shapes.or(
            Block.box(2, 4, 5, 14, 16, 9), Block.box(3, 0, 5, 13, 4, 14), Block.box(7, 4, 9, 9, 15, 14)));
    private static final Map<Direction, VoxelShape> COLLISION = Shapes.rotateHorizontal(Block.box(3, 0, 5, 13, 15, 13));

    public NoxisEaselBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(CANVAS, CANVAS_NONE).setValue(ART, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CANVAS, ART);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINE.get(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION.get(state.getValue(FACING));
    }

    /** El lienzo mira hacia quien lo coloca. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
        return new NoxisEaselBlockEntity(pos, state);
    }

    // ------------------------------------------------------------------ jugador

    /** Clic derecho con una pintura sobre un atril vacío: la apoya. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!NoxisPaintings.isPainting(stack) || state.getValue(CANVAS) != CANVAS_NONE) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        NoxisPaintings.migrateLegacy(stack);
        int art = NoxisPaintings.hasArt(stack) ? NoxisPaintings.artOf(stack) : NoxisPaintings.discover(server, pos, level.getRandom());
        server.setBlock(pos, state.setValue(CANVAS, CANVAS_DONE).setValue(ART, art), Block.UPDATE_ALL);
        if (server.getBlockEntity(pos) instanceof NoxisEaselBlockEntity be) be.clearWork();
        stack.consume(1, player);
        level.playSound(null, pos, SoundEvents.PAINTING_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    /** Clic derecho sobre la obra: la retira (el atril queda colocado). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        int canvas = state.getValue(CANVAS);
        if (canvas == CANVAS_NONE) return InteractionResult.PASS;
        boolean free = canvas == CANVAS_DONE;
        if (!free && level.getBlockEntity(pos) instanceof NoxisEaselBlockEntity be) free = be.isAbandoned(level.getGameTime());
        if (!free) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("block.noxis_culture.noxis_easel.in_progress"));
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        ItemStack painting = canvas == CANVAS_BLANK ? ItemStack.EMPTY : NoxisPaintings.stack(state.getValue(ART));
        server.setBlock(pos, state.setValue(CANVAS, CANVAS_NONE), Block.UPDATE_ALL);
        if (server.getBlockEntity(pos) instanceof NoxisEaselBlockEntity be) be.clearWork();
        if (!painting.isEmpty() && !player.getInventory().add(painting)) player.drop(painting, false);
        level.playSound(null, pos, SoundEvents.PAINTING_BREAK, SoundSource.BLOCKS, 0.8F, 1.1F);
        return InteractionResult.SUCCESS;
    }

    /** Cae el atril y, si había una obra empezada o terminada, la pintura con esa obra. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(this));
        if (state.getValue(CANVAS) >= CANVAS_SKETCH) drops.add(NoxisPaintings.stack(state.getValue(ART)));
        return drops;
    }

    // ------------------------------------------------------------------ Noxis

    /** Un Noxis arma su propio atril con un lienzo en blanco para la obra {@code art}. */
    public static boolean placeForNoxis(Level level, BlockPos pos, Direction facing, int art, java.util.UUID owner) {
        BlockState state = ModBlocks.NOXIS_EASEL.defaultBlockState().setValue(FACING, facing)
                .setValue(CANVAS, CANVAS_BLANK).setValue(ART, art);
        if (!level.getBlockState(pos).canBeReplaced() || !state.canSurvive(level, pos)) return false;
        level.setBlock(pos, state, Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof NoxisEaselBlockEntity be) be.startWork(owner, level.getGameTime(), true);
        return true;
    }

    /** Etapa del lienzo según el avance (0 a 100). */
    public static int canvasFor(int progress) {
        if (progress >= NoxisEaselBlockEntity.DONE) return CANVAS_DONE;
        if (progress >= 55) return CANVAS_COLOR;
        if (progress >= 15) return CANVAS_SKETCH;
        return CANVAS_BLANK;
    }
}
