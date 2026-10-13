package com.noxisculture.block.custom;

import com.mojang.serialization.MapCodec;
import com.noxisculture.fluid.custom.NoyuxFluid;
import com.noxisculture.particle.ModParticles;
import com.noxisculture.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Fuego violeta del Noyux: arde sobre la piedra (mecánica propia, ver {@link NoyuxBurning}).
 *
 * <ul>
 *   <li>Se revisa cada 1,5 a 2,5 s (tick programado, nunca cada tick).</li>
 *   <li>Envejece ({@link #AGE}) y se consume solo; al consumirse a veces "quema" la piedra de abajo
 *   un paso (piedra → adoquín → grava, igual que la corrosión del Noyux).</li>
 *   <li>Mientras arde puede encender un lugar vecino con piedra quemable, hasta la generación
 *   {@link NoyuxBurning#MAX_SPREAD} ({@link #SPREAD}): la propagación siempre termina.</li>
 *   <li>Se apaga con agua al lado o si ya no hay piedra quemable debajo.</li>
 *   <li>Quema a las criaturas que están adentro (daño mágico, como el Noyux).</li>
 * </ul>
 */
public class NoyuxFireBlock extends Block {
    public static final MapCodec<NoyuxFireBlock> CODEC = simpleCodec(NoyuxFireBlock::new);
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 7);
    public static final IntegerProperty SPREAD = IntegerProperty.create("spread", 0, NoyuxBurning.MAX_SPREAD);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);
    private static final float SPREAD_CHANCE = 0.45F;
    private static final float SCORCH_CHANCE = 0.35F;
    private static final float DAMAGE = 2.0F;

    public NoyuxFireBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0).setValue(SPREAD, 0));
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, SPREAD);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    private static int delay(RandomSource random) {
        return 30 + random.nextInt(20);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, delay(level.getRandom()));
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Sin piedra quemable debajo (la rompieron, se volvió grava...): se apaga.
        if (!NoyuxBurning.isBurnable(level.getBlockState(pos.below()))) {
            this.extinguish(level, pos, random);
            return;
        }
        // Agua al lado: se apaga.
        for (Direction dir : Direction.values()) {
            if (level.getFluidState(pos.relative(dir)).is(FluidTags.WATER)) {
                this.extinguish(level, pos, random);
                return;
            }
        }
        // Quema a quien esté adentro.
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos))) {
            living.hurtServer(level, level.damageSources().magic(), DAMAGE);
        }
        int age = state.getValue(AGE) + 1 + random.nextInt(2);
        if (age > 7) {
            // Se consumió: a veces deja la piedra quemada (un paso de corrosión).
            if (random.nextFloat() < SCORCH_CHANCE) {
                BlockState scorched = NoyuxFluid.corrode(level.getBlockState(pos.below()));
                if (scorched != null) level.setBlockAndUpdate(pos.below(), scorched);
            }
            this.extinguish(level, pos, random);
            return;
        }
        // Propagación controlada a un lugar vecino con piedra quemable.
        int spread = state.getValue(SPREAD);
        if (spread < NoyuxBurning.MAX_SPREAD && random.nextFloat() < SPREAD_CHANCE) {
            BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1);
            if (!target.equals(pos)) NoyuxBurning.ignite(level, target, spread + 1);
        }
        level.setBlockAndUpdate(pos, state.setValue(AGE, age));
        level.scheduleTick(pos, this, delay(random));
    }

    private void extinguish(ServerLevel level, BlockPos pos, RandomSource random) {
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.playSound(null, pos, ModSounds.NOYUX_FIRE_FIZZ, SoundSource.BLOCKS, 0.5F, 0.9F + random.nextFloat() * 0.2F);
        NoyuxBurning.burst(level, pos, random, 5, 1);
    }

    /** Humo violeta, destellos y chispas que suben; y el crepitar, de vez en cuando (solo en el cliente). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX(), y = pos.getY(), z = pos.getZ();
        if (random.nextInt(18) == 0) {
            level.playLocalSound(x + 0.5D, y + 0.5D, z + 0.5D, ModSounds.NOYUX_FIRE_CRACKLE, SoundSource.BLOCKS,
                    0.6F + random.nextFloat() * 0.3F, 0.85F + random.nextFloat() * 0.3F, false);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ModParticles.NOYUX_SMOKE, x + 0.2D + random.nextDouble() * 0.6D, y + 0.5D + random.nextDouble() * 0.4D,
                    z + 0.2D + random.nextDouble() * 0.6D, (random.nextDouble() - 0.5D) * 0.01D, 0.03D + random.nextDouble() * 0.02D,
                    (random.nextDouble() - 0.5D) * 0.01D);
        }
        if (random.nextInt(5) == 0) {
            level.addParticle(ModParticles.NOYUX_SPARK, x + random.nextDouble(), y + 0.3D + random.nextDouble() * 0.8D,
                    z + random.nextDouble(), 0.0D, 0.015D, 0.0D);
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ModParticles.NOYUX_MOTE, x + 0.15D + random.nextDouble() * 0.7D, y + 0.2D,
                    z + 0.15D + random.nextDouble() * 0.7D, 0.0D, 0.04D + random.nextDouble() * 0.03D, 0.0D);
        }
    }
}
