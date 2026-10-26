package com.noxisculture.block.custom;

import com.mojang.serialization.MapCodec;
import com.noxisculture.fluid.ModFluidTags;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Fuego violeta del Noyux: arde sobre la piedra (mecánica propia, ver {@link NoyuxBurning}).
 *
 * <ul>
 *   <li>Se revisa una vez por segundo (tick programado de 20 ticks, nunca cada tick).</li>
 *   <li>Quema a quien esté adentro: {@link #DAMAGE} de daño mágico por segundo. Antes eran 2,0 cada
 *   1,5 a 2,5 s (≈ 1 por segundo); ahora son 4,0 por segundo. No se suma al daño del líquido: quien
 *   ya está dentro del Noyux solo recibe el daño del Noyux. Tampoco enciende al jugador con fuego
 *   vanilla, así que el efecto termina apenas sale de las llamas.</li>
 *   <li>Envejece despacio ({@link #AGE}) y se consume solo; al consumirse a veces "quema" la piedra de
 *   abajo un paso (piedra → adoquín → grava, igual que la corrosión del Noyux).</li>
 *   <li>Avanza de a poco: de vez en cuando enciende un único lugar vecino con piedra quemable, hasta la
 *   generación {@link NoyuxBurning#MAX_SPREAD} ({@link #SPREAD}). La propagación siempre termina.</li>
 *   <li>Nunca frena al Noyux: no tiene colisión, el líquido lo puede pisar, y si llega Noyux que todavía
 *   corre, el fuego se retira solo (y no se vuelve a encender ahí, ver NoyuxBurning#nearSpreadingNoyux).</li>
 *   <li>Se apaga con agua o lava al lado (o si lo pisan). La lluvia NO lo apaga (el fuego vanilla no cambia).</li>
 * </ul>
 */
public class NoyuxFireBlock extends Block {
    public static final MapCodec<NoyuxFireBlock> CODEC = simpleCodec(NoyuxFireBlock::new);
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 7);
    public static final IntegerProperty SPREAD = IntegerProperty.create("spread", 0, NoyuxBurning.MAX_SPREAD);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);
    private static final int TICK = 20;                 // se revisa una vez por segundo
    private static final float AGE_CHANCE = 0.35F;      // por segundo: vive ~15 a 25 s
    private static final float SPREAD_CHANCE = 0.12F;   // por segundo: avanza de a un bloque, despacio
    private static final float SCORCH_CHANCE = 0.35F;
    /** Daño por segundo dentro del fuego violeta (antes: 2,0 cada 1,5-2,5 s). */
    public static final float DAMAGE = 4.0F;

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
        return TICK;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide() && !oldState.is(this)) {
            level.scheduleTick(pos, this, 10 + level.getRandom().nextInt(TICK));   // desfasados entre sí
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Sin piedra quemable debajo (la rompieron, se volvió grava...): se apaga.
        if (!NoyuxBurning.isBurnable(level.getBlockState(pos.below()))) {
            this.extinguish(level, pos, random);
            return;
        }
        // Llega Noyux que todavía corre: el fuego le deja paso (sin chispas ni sonido).
        if (NoyuxBurning.nearSpreadingNoyux(level, pos)) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            return;
        }
        // Agua o lava al lado: se apaga.
        for (Direction dir : Direction.values()) {
            FluidState near = level.getFluidState(pos.relative(dir));
            if (near.is(FluidTags.WATER) || NoyuxReactions.isLava(near)) {
                this.extinguish(level, pos, random);
                return;
            }
        }
        // Quema a quien esté adentro (una vez por segundo).
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos))) {
            if (living instanceof Player player && (player.isCreative() || player.isSpectator())) continue;
            if (level.getFluidState(living.blockPosition()).is(ModFluidTags.NOYUX)) continue; // ya lo quema el líquido
            living.hurtServer(level, level.damageSources().magic(), DAMAGE);
        }
        int age = state.getValue(AGE);
        if (random.nextFloat() < AGE_CHANCE) age++;
        if (age > 7) {
            // Se consumió: a veces deja la piedra quemada (un paso de corrosión).
            if (random.nextFloat() < SCORCH_CHANCE) {
                BlockState scorched = NoyuxFluid.corrode(level.getBlockState(pos.below()));
                if (scorched != null) level.setBlockAndUpdate(pos.below(), scorched);
            }
            this.extinguish(level, pos, random);
            return;
        }
        // Propagación lenta: a lo sumo un lugar vecino (al costado, o un escalón arriba o abajo).
        int spread = state.getValue(SPREAD);
        if (spread < NoyuxBurning.MAX_SPREAD && age >= 2 && random.nextFloat() < SPREAD_CHANCE) {
            Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            BlockPos target = pos.relative(dir).above(random.nextInt(3) - 1);
            NoyuxBurning.ignite(level, target, spread + 1);
        }
        if (age != state.getValue(AGE)) level.setBlockAndUpdate(pos, state.setValue(AGE, age));
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
