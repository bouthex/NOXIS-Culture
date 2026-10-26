package com.noxisculture.block.custom;

import com.mojang.serialization.MapCodec;
import com.noxisculture.entity.ai.NoxisBowlSleeper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Pecera Noxis: donde duermen los Noxis (para ellos reemplaza a las camas; las camas de los
 * aldeanos vanilla no cambian). Base de madera y vidrio cuadrado (casi un bloque entero) con
 * cuellito y un borde ancho arriba, como una pecera.
 *
 * <p>{@link #OCCUPIED} guarda si está ocupada (o reservada por un Noxis que va en camino): una
 * pecera admite UN solo Noxis. Se guarda con el mundo. Si quedara marcada como ocupada sin
 * nadie que la use (por ejemplo, tras un cierre inesperado), se libera sola.</p>
 */
public class NoxisBowlBlock extends Block {
    public static final MapCodec<NoxisBowlBlock> CODEC = simpleCodec(NoxisBowlBlock::new);
    public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;
    /**
     * Tapada: hay otra pecera (u otro bloque) justo encima. La abertura queda cerrada (se ve tapada por la base
     * de la de arriba) y ningún Noxis puede entrar ni salir hasta que la destapen.
     */
    public static final BooleanProperty COVERED = BooleanProperty.create("covered");

    /**
     * Contorno vacía (como un caldero): el vidrio con su forma real y el hueco del medio libre,
     * así quien está adentro puede mirar y apuntar hacia afuera.
     */
    private static final VoxelShape OUTLINE_HOLLOW = Shapes.or(
            Block.box(0, 0, 0, 16, 2, 16),
            ring(2, 18, 0, 16, 1.25),
            ring(18, 18.5, 0, 16, 3),
            ring(18.5, 19.5, 3, 13, 1),
            ring(19.5, 21, 0.5, 15.5, 2.5));
    /**
     * Contorno llena: con un Noxis durmiendo (o reservada), la pecera entera se apunta como un
     * bloque, así desde afuera se apunta al vidrio y nunca se golpea al Noxis sin querer.
     */
    private static final VoxelShape OUTLINE = Shapes.or(
            Block.box(0, 0, 0, 16, 18, 16),
            Block.box(3, 18, 3, 13, 19.5, 13),
            Block.box(0.5, 19.5, 0.5, 15.5, 21, 15.5));
    /**
     * Choque, igual para todos (como un caldero): base de madera que sostiene lo que entra + las
     * cuatro paredes de vidrio. Arriba queda abierto: el jugador (o cualquier cosa chica que
     * quepa) puede saltar adentro y quedarse parado en el fondo, y nadie atraviesa las paredes.
     * Las paredes llegan a 18 px (lo que mide el cuerpo de la pecera), así un jugador puede
     * saltar adentro desde el piso. El cuellito y el borde de arriba son solo decorativos.
     */
    private static final VoxelShape WALLS = Shapes.or(
            Block.box(0, 0, 0, 16, 2, 16),
            ring(2, 18, 0, 16, 1.25));
    /** Tapada por otra pecera: el vidrio llega solo hasta la base de la de arriba. */
    private static final VoxelShape WALLS_COVERED = Shapes.or(
            Block.box(0, 0, 0, 16, 2, 16),
            ring(2, 16, 0, 16, 1.25));
    private static final VoxelShape OUTLINE_COVERED = Block.box(0, 0, 0, 16, 16, 16);

    /** Marco cuadrado de vidrio: cuatro paredes de espesor {@code t} entre {@code o0} y {@code o1}. */
    private static VoxelShape ring(double y0, double y1, double o0, double o1, double t) {
        return Shapes.or(
                Block.box(o0, y0, o0, o1, y1, o0 + t),
                Block.box(o0, y0, o1 - t, o1, y1, o1),
                Block.box(o0, y0, o0 + t, o0 + t, y1, o1 - t),
                Block.box(o1 - t, y0, o0 + t, o1, y1, o1 - t));
    }

    public NoxisBowlBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(OCCUPIED, false).setValue(COVERED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OCCUPIED, COVERED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(COVERED)) return OUTLINE_COVERED;
        return state.getValue(OCCUPIED) ? OUTLINE : OUTLINE_HOLLOW;
    }

    // ------------------------------------------------------------------ peceras apiladas

    /** Al colocarla: ¿ya tiene otra pecera encima? */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(COVERED, coversBowl(context.getLevel().getBlockState(context.getClickedPos().above())));
    }

    /**
     * ¿El bloque de arriba tapa la abertura? Otra pecera o cualquier bloque de verdad sí; aire,
     * pasto, flores, nieve finita y demás cosas que se reemplazan al construir, no.
     */
    private static boolean coversBowl(BlockState above) {
        return !above.isAir() && !above.canBeReplaced();
    }

    /** Cuando cambia el bloque de arriba: se tapa o se destapa. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState,
                                     RandomSource random) {
        if (direction == Direction.UP) {
            boolean covered = coversBowl(neighborState);
            if (covered != state.getValue(COVERED)) state = state.setValue(COVERED, covered);
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(COVERED) ? WALLS_COVERED : WALLS;
    }

    // ------------------------------------------------------------------ llevarse la pecera con el Noxis

    /**
     * Romper una pecera ocupada (como una colmena con abejas): el Noxis que duerme adentro se
     * guarda en el ítem de la pecera, con todos sus datos, y desaparece del mundo (no se duplica).
     * En creativo también sale el ítem con el Noxis, para no perderlo. Si estaba solo reservada
     * (el Noxis venía en camino), sale la pecera vacía normal.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel server && state.getValue(OCCUPIED)) {
            ItemStack withNoxis = NoxisBowlStorage.capture(server, pos);
            if (withNoxis != null) {
                popResource(level, pos, withNoxis);
            } else if (!player.isCreative()) {
                popResource(level, pos, new ItemStack(this));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Al colocar una pecera que trae un Noxis guardado, el Noxis aparece adentro, durmiendo. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel server) {
            NoxisBowlStorage.release(server, pos, stack);
        }
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
        if (!owned && NoxisBowlClaims.owner(level, pos) == null) {
            level.setBlock(pos, state.setValue(OCCUPIED, false), Block.UPDATE_ALL);
        }
    }
}
