package com.noxisculture.block.custom;

import com.noxisculture.block.ModBlocks;
import com.noxisculture.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

/**
 * Reacciones del Noyux con el agua y la lava (servidor). Se resuelven con la lógica de bloques y
 * fluidos del juego, igual que la lava con el agua: el bloque de Noyux que toca el otro líquido se
 * transforma, una sola vez, en un bloque sólido (que ya no reacciona).
 *
 * <ul>
 *   <li><b>Fuente de Noyux + agua → Niris.</b></li>
 *   <li><b>Fuente de Noyux + lava → Ignyx</b> (cristal; nombre pendiente de aprobación).</li>
 *   <li><b>Noyux que fluye (no fuente) + agua o lava → adoquín.</b> Así no se puede armar un generador
 *   infinito de Niris ni de cristal: cada fuente da un solo bloque.</li>
 * </ul>
 * El agua y la lava vanilla no cambian.
 */
public final class NoyuxReactions {
    private NoyuxReactions() {}

    /** Si el Noyux en {@code pos} toca agua o lava, lo transforma y devuelve true. */
    public static boolean react(ServerLevel level, BlockPos pos, BlockState noyuxState) {
        FluidState self = noyuxState.getFluidState();
        boolean source = self.isSource();
        BlockState result = null;
        for (Direction dir : Direction.values()) {
            FluidState other = level.getFluidState(pos.relative(dir));
            if (other.is(FluidTags.WATER)) {
                result = source ? ModBlocks.NIRIS.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
                break;                                         // el agua tiene prioridad
            }
            if (isLava(other) && result == null) {
                result = source ? ModBlocks.IGNYX.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
            }
        }
        if (result == null) return false;
        level.setBlockAndUpdate(pos, result);
        fizz(level, pos);
        return true;
    }

    /** Lava vanilla de verdad (el Noyux también está en la etiqueta de la lava, por eso se compara el tipo). */
    public static boolean isLava(FluidState fluid) {
        return Fluids.LAVA.isSame(fluid.getType());
    }

    /** ¿Ese fluido reacciona con el Noyux? (agua o lava vanilla). */
    public static boolean reactsWith(FluidState fluid) {
        return fluid.is(FluidTags.WATER) || isLava(fluid);
    }

    public static void fizz(LevelAccessor level, BlockPos pos) {
        if (level instanceof ServerLevel server) {
            server.playSound(null, pos, ModSounds.NOYUX_FIRE_FIZZ, SoundSource.BLOCKS, 0.8F, 0.75F + server.getRandom().nextFloat() * 0.2F);
            NoyuxBurning.burst(server, pos, server.getRandom(), 6, 2);
        }
    }

    /** Lo que deja el Noyux que fluye al caer sobre agua o lava (null = no reacciona). */
    public static @Nullable BlockState flowingResult(FluidState target) {
        return reactsWith(target) ? Blocks.COBBLESTONE.defaultBlockState() : null;
    }
}
