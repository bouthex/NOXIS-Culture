package com.noxisculture.fluid.custom;

import com.noxisculture.block.ModBlocks;
import com.noxisculture.fluid.ModFluidTags;
import com.noxisculture.fluid.ModFluids;
import com.noxisculture.item.ModItems;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

/**
 * Noyux: líquido violeta corrosivo.
 *  - Fluye MUY lento (más lento que la lava) y no se expande mucho.
 *  - No genera fuentes infinitas.
 *  - Tocarlo quema 5 corazones por segundo.
 *  - Corroe de a poco la piedra que toca: piedra -> adoquín -> grava.
 *    (Se detiene en la grava para no "comerse" el mundo.)
 */
public abstract class NoyuxFluid extends FlowingFluid {
    private static final int TICK_DELAY = 40;          // lava en el Overworld: 30
    private static final float BURN_DAMAGE = 10.0F;    // 5 corazones
    private static final int CORRODE_CHANCE = 2;       // 1 de cada N ticks aleatorios corroe algo

    @Override
    public Fluid getFlowing() {
        return ModFluids.FLOWING_NOYUX;
    }

    @Override
    public Fluid getSource() {
        return ModFluids.NOYUX;
    }

    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == ModFluids.NOYUX || fluid == ModFluids.FLOWING_NOYUX;
    }

    @Override
    public Item getBucket() {
        return ModItems.NOYUX_BUCKET;
    }

    @Override
    public void animateTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
        if (random.nextInt(12) == 0) {
            level.addParticle(ParticleTypes.PORTAL, pos.getX() + random.nextDouble(), pos.getY() + 0.9D,
                    pos.getZ() + random.nextDouble(), 0.0D, 0.04D, 0.0D);
        }
        if (random.nextInt(120) == 0) {
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, SoundSource.BLOCKS,
                    0.4F, 0.6F + random.nextFloat() * 0.2F, false);
        }
    }

    @Override
    public @Nullable ParticleOptions getDripParticle() {
        return ParticleTypes.DRIPPING_OBSIDIAN_TEAR; // gotita violeta
    }

    @Override
    protected boolean canConvertToSource(ServerLevel level) {
        return false;
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        Block.dropResources(state, level, pos, blockEntity);
    }

    @Override
    protected void entityInside(Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier handler) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof LivingEntity living)) {
            return;
        }
        if (living instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        if (level.getGameTime() % 20 == 0) {
            living.hurtServer(serverLevel, level.damageSources().magic(), BURN_DAMAGE);
        }
    }

    // ---------------- Corrosión lenta de la piedra ----------------

    @Override
    protected boolean isRandomlyTicking() {
        return true;
    }

    @Override
    protected void randomTick(ServerLevel level, BlockPos pos, FluidState state, RandomSource random) {
        if (random.nextInt(CORRODE_CHANCE) != 0) {
            return;
        }
        Direction dir = Direction.getRandom(random);
        BlockPos target = pos.relative(dir);
        BlockState current = level.getBlockState(target);
        BlockState corroded = corrode(current);
        if (corroded != null) {
            level.setBlockAndUpdate(target, corroded);
            level.playSound(null, target, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.6F);
            level.sendParticles(ParticleTypes.SMOKE, target.getX() + 0.5D, target.getY() + 0.5D,
                    target.getZ() + 0.5D, 6, 0.3D, 0.3D, 0.3D, 0.01D);
        }
    }

    private static @Nullable BlockState corrode(BlockState state) {
        if (state.is(Blocks.DEEPSLATE)) return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
        if (state.is(BlockTags.BASE_STONE_OVERWORLD)) return Blocks.COBBLESTONE.defaultBlockState();
        if (state.is(Blocks.COBBLESTONE) || state.is(Blocks.COBBLED_DEEPSLATE)) return Blocks.GRAVEL.defaultBlockState();
        return null;
    }

    // ---------------- Comportamiento de flujo ----------------

    @Override
    protected int getSlopeFindDistance(LevelReader level) {
        return 2;
    }

    @Override
    public int getDropOff(LevelReader level) {
        return 2; // se esparce poco, como la lava
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return TICK_DELAY;
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return ModBlocks.NOYUX.defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    @Override
    public boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) {
        return direction == Direction.DOWN && !fluid.is(ModFluidTags.NOYUX);
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return Optional.of(SoundEvents.BUCKET_FILL_LAVA);
    }

    public static class Flowing extends NoyuxFluid {
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }

    public static class Source extends NoyuxFluid {
        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }
}
