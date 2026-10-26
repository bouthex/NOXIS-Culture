package com.noxisculture.block.custom;

import com.noxisculture.block.ModBlockTags;
import com.noxisculture.block.ModBlocks;
import com.noxisculture.fluid.ModFluidTags;
import com.noxisculture.particle.ModParticles;
import com.noxisculture.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Combustión de la piedra por el Noyux (servidor). Mecánica propia: la piedra no arde en Minecraft,
 * así que el fuego violeta no usa el sistema de fuego vanilla.
 *
 * <ul>
 *   <li><b>Contacto:</b> el Noyux enciende fuego violeta sobre la piedra quemable que toca
 *   (al lado, o al lado y un bloque más abajo) cuando aparece en un lugar y, después, de a ratos.</li>
 *   <li><b>Qué arde:</b> solo los bloques de la etiqueta {@code noxis_culture:noyux_burnable}.</li>
 *   <li><b>Dónde:</b> el fuego ocupa un espacio de aire que tenga piedra quemable justo debajo.</li>
 *   <li><b>Propagación controlada:</b> cada fuego tiene una generación (0 = encendido por el Noyux);
 *   solo se propaga a lugares vecinos con piedra quemable y hasta la generación {@link #MAX_SPREAD}.
 *   Así nunca se extiende sin fin.</li>
 *   <li><b>Nunca frena al Noyux:</b> no se enciende donde el Noyux todavía puede correr, y si el
 *   líquido llega al lado, el fuego se retira solo (y el líquido, de todos modos, lo puede pisar).</li>
 *   <li><b>Extinción:</b> se apaga al consumirse, al quedar sin piedra debajo o con agua o lava al
 *   lado. La lluvia no lo apaga.</li>
 * </ul>
 */
public final class NoyuxBurning {
    private NoyuxBurning() {}

    /** Hasta dónde se propaga desde el Noyux (generaciones de fuego). */
    public static final int MAX_SPREAD = 3;

    public static boolean isBurnable(BlockState state) {
        return state.is(ModBlockTags.NOYUX_BURNABLE);
    }

    /** ¿Puede haber fuego violeta en {@code pos}? (aire, con piedra quemable debajo). */
    public static boolean canBurnAt(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).isAir() && isBurnable(level.getBlockState(pos.below()));
    }

    /**
     * ¿Hay Noyux al lado (o encima) de {@code pos} que todavía puede correr hasta ahí? En ese lugar no
     * se enciende fuego, así las llamas nunca se adelantan al líquido ni le cortan el paso.
     * Con caída 2, el Noyux que tiene 2 o menos ya no avanza de costado.
     */
    public static boolean nearSpreadingNoyux(BlockGetter level, BlockPos pos) {
        if (level.getFluidState(pos.above()).is(ModFluidTags.NOYUX)) return true;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            FluidState fluid = level.getFluidState(pos.relative(dir));
            if (fluid.is(ModFluidTags.NOYUX) && fluid.getAmount() > 2) return true;
        }
        return false;
    }

    /** Enciende fuego violeta en {@code pos} (si se puede). Devuelve si lo encendió. */
    public static boolean ignite(ServerLevel level, BlockPos pos, int spread) {
        if (!canBurnAt(level, pos) || nearSpreadingNoyux(level, pos)) return false;
        level.setBlockAndUpdate(pos, ModBlocks.NOYUX_FIRE.defaultBlockState()
                .setValue(NoyuxFireBlock.SPREAD, Math.min(spread, MAX_SPREAD)));
        RandomSource random = level.getRandom();
        level.playSound(null, pos, ModSounds.NOYUX_FIRE_IGNITE, SoundSource.BLOCKS, 0.7F, 0.85F + random.nextFloat() * 0.3F);
        burst(level, pos, random, 4, 3);
        return true;
    }

    /**
     * El Noyux en {@code fluidPos} intenta encender la piedra que toca: mira los cuatro costados (al
     * mismo nivel y uno más abajo, para las orillas) y enciende como mucho {@code max} fuegos.
     */
    public static void igniteAround(ServerLevel level, BlockPos fluidPos, RandomSource random, float chance, int max) {
        int lit = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (lit >= max) return;
            if (random.nextFloat() >= chance) continue;
            BlockPos side = fluidPos.relative(dir);
            // Piedra al lado (fuego encima de ella) o piedra al lado y abajo (fuego al costado del Noyux).
            if (isBurnable(level.getBlockState(side)) && ignite(level, side.above(), 0)) lit++;
            else if (isBurnable(level.getBlockState(side.below())) && ignite(level, side, 0)) lit++;
        }
    }

    /** Humo violeta, destellos y chispas que suben alrededor de un punto (para todos los jugadores cerca). */
    public static void burst(ServerLevel level, BlockPos pos, RandomSource random, int smoke, int sparks) {
        double x = pos.getX() + 0.5D, y = pos.getY() + 0.1D, z = pos.getZ() + 0.5D;
        for (int i = 0; i < smoke; i++) {
            level.sendParticles(ModParticles.NOYUX_SMOKE, x + (random.nextDouble() - 0.5D) * 0.8D, y + random.nextDouble() * 0.4D,
                    z + (random.nextDouble() - 0.5D) * 0.8D, 0, (random.nextDouble() - 0.5D) * 0.2D, 1.0D, (random.nextDouble() - 0.5D) * 0.2D,
                    0.03D + random.nextDouble() * 0.02D);
        }
        for (int i = 0; i < sparks; i++) {
            level.sendParticles(random.nextBoolean() ? ModParticles.NOYUX_SPARK : ModParticles.NOYUX_MOTE,
                    x + (random.nextDouble() - 0.5D) * 0.9D, y + random.nextDouble() * 0.6D, z + (random.nextDouble() - 0.5D) * 0.9D,
                    0, 0.0D, 1.0D, 0.0D, 0.02D + random.nextDouble() * 0.03D);
        }
    }
}
