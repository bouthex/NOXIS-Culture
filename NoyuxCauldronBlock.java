package com.noxisculture.block.custom;

import com.mojang.serialization.MapCodec;
import com.noxisculture.block.entity.ModBlockEntities;
import com.noxisculture.block.entity.NoyuxCauldronBlockEntity;
import com.noxisculture.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Caldero con Noyux. Nivel 1-3: cada Noxus en bruto que cae adentro se transmuta
 * en un Lingote de Noxus y gasta un nivel. Tocar el líquido quema 5 corazones.
 */
public class NoyuxCauldronBlock extends BaseEntityBlock {
    public static final MapCodec<NoyuxCauldronBlock> CODEC = simpleCodec(NoyuxCauldronBlock::new);
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL_CAULDRON;

    /** Misma forma hueca que el caldero vanilla (para que se pueda tirar cosas adentro). */
    private static final VoxelShape SHAPE = Shapes.join(
            Shapes.block(),
            Shapes.or(Block.box(0, 0, 4, 16, 3, 12), Block.box(4, 0, 0, 12, 3, 16),
                    Block.box(2, 0, 2, 14, 3, 14), Block.box(2, 4, 2, 14, 16, 14)),
            BooleanOp.ONLY_FIRST);

    public NoyuxCauldronBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 3));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NoyuxCauldronBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.NOYUX_CAULDRON, NoyuxCauldronBlockEntity::serverTick);
    }

    /** Clic derecho: con Noxus en bruto lo transmuta; con un cubo vacío (caldero lleno) recupera el Noyux. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(ModItems.RAW_NOXUS)) {
            if (level instanceof ServerLevel serverLevel) {
                stack.consume(1, player);
                transmute(serverLevel, pos, state);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.BUCKET) && state.getValue(LEVEL) == 3) {
            if (!level.isClientSide()) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(ModItems.NOYUX_BUCKET)));
                level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
                level.playSound(null, pos, SoundEvents.BUCKET_FILL_LAVA, SoundSource.BLOCKS, 1.0F, 0.8F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** Convierte un Noxus en bruto en un lingote y baja un nivel. */
    public static void transmute(ServerLevel level, BlockPos pos, BlockState state) {
        ItemEntity ingot = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 1.05D, pos.getZ() + 0.5D,
                new ItemStack(ModItems.NOXUS_INGOT));
        ingot.setDeltaMovement(0.0D, 0.25D, 0.0D);
        level.addFreshEntity(ingot);

        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.2F, 0.7F);
        level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.4F);
        level.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5D, pos.getY() + 0.9D, pos.getZ() + 0.5D,
                20, 0.25D, 0.2D, 0.25D, 0.05D);

        int current = state.getValue(LEVEL);
        level.setBlockAndUpdate(pos, current > 1
                ? state.setValue(LEVEL, current - 1)
                : Blocks.CAULDRON.defaultBlockState());
    }

    /** Burbujitas violetas subiendo (solo visual, en el cliente). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            double y = pos.getY() + 0.3D + 0.2D * state.getValue(LEVEL);
            level.addParticle(ParticleTypes.PORTAL,
                    pos.getX() + 0.25D + random.nextDouble() * 0.5D, y,
                    pos.getZ() + 0.25D + random.nextDouble() * 0.5D, 0.0D, 0.05D, 0.0D);
        }
    }
}
