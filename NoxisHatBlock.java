package com.noxisculture.block.custom;

import com.mojang.serialization.MapCodec;
import com.noxisculture.block.ModBlocks;
import com.noxisculture.block.entity.NoxisHatBlockEntity;
import com.noxisculture.item.NoxisHatColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * El sombrero de un Noxis apoyado en el suelo (lo dejó al lado de su pecera, o lo puso un
 * jugador). Es un bloque con la forma del sombrero (mismo tamaño y alto) que guarda el sombrero
 * EXACTO que se apoyó ({@link NoxisHatBlockEntity}): colores de copa y lazo, desgaste, etc.
 * Al romperlo se recupera ese mismo sombrero; si lo levanta un Noxis, se lo pone tal cual.
 */
public class NoxisHatBlock extends Block implements EntityBlock {
    public static final MapCodec<NoxisHatBlock> CODEC = simpleCodec(NoxisHatBlock::new);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 13, 14);
    /**
     * Copa/ala o lazo teñidos de blanco: el modelo usa una máscara más clara para que el blanco
     * se vea blanco y no gris. Lo actualiza el propio bloque según el sombrero que guarda.
     */
    public static final BooleanProperty WHITE_CROWN = BooleanProperty.create("white_crown");
    public static final BooleanProperty WHITE_BAND = BooleanProperty.create("white_band");

    public NoxisHatBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(WHITE_CROWN, false).setValue(WHITE_BAND, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WHITE_CROWN, WHITE_BAND);
    }

    /** El estado que corresponde a los colores de ese sombrero (blanco o no, en copa y lazo). */
    public static BlockState withColors(BlockState state, ItemStack hat) {
        if (!state.hasProperty(WHITE_CROWN)) return state;
        return state.setValue(WHITE_CROWN, NoxisHatColors.isWhite(NoxisHatColors.crown(hat)))
                .setValue(WHITE_BAND, NoxisHatColors.isWhite(NoxisHatColors.band(hat)));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NoxisHatBlockEntity(pos, state);
    }

    /** Lo apoyó un jugador: el bloque guarda ese sombrero exacto (con sus colores). */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof NoxisHatBlockEntity hat) {
            hat.setHat(stack);
        }
    }

    // ------------------------------------------------------------------ para los Noxis

    /** Apoya ese sombrero exacto en el piso (servidor). */
    public static void placeHat(Level level, BlockPos pos, ItemStack hat) {
        level.setBlock(pos, ModBlocks.NOXIS_HAT.defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof NoxisHatBlockEntity be) {
            be.setHat(hat.isEmpty() ? NoxisHatColors.newHat() : hat);
        }
    }

    /** Levanta el sombrero apoyado (servidor): devuelve ese sombrero exacto y quita el bloque. */
    public static ItemStack takeHat(Level level, BlockPos pos) {
        ItemStack hat = level.getBlockEntity(pos) instanceof NoxisHatBlockEntity be ? be.takeHat() : NoxisHatColors.newHat();
        level.removeBlock(pos, false);
        return hat;
    }

    /** El sombrero apoyado en esa posición (sin levantarlo), para elegir entre varios. */
    public static ItemStack peekHat(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof NoxisHatBlockEntity be ? be.getHat() : NoxisHatColors.newHat();
    }
}
