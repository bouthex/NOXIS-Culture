package com.noxisculture.entity.light;

import com.noxisculture.block.ModBlocks;
import com.noxisculture.block.custom.NoxisLightBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Luz dinámica sin shaders ni mods externos: coloca un bloque de luz invisible
 * en el aire a la altura de la cabeza y lo mueve con la entidad.
 * Se usa por composición en cada especie Noxis.
 *
 * Rendimiento: solo toca el mundo cuando la entidad cambia de bloque o cambia
 * el nivel de luz, y usa UPDATE_CLIENTS (sin avisar a los bloques vecinos).
 */
public final class NoxisLightController {
    private @Nullable BlockPos current;

    public void update(Mob mob, int lightLevel) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos target = lightLevel > 0 && mob.isAlive() ? findSpot(level, mob) : null;
        if (this.current != null && !this.current.equals(target)) {
            clearAt(level, this.current);
            this.current = null;
        }
        if (target != null) {
            BlockState wanted = ModBlocks.NOXIS_LIGHT.defaultBlockState()
                    .setValue(NoxisLightBlock.LEVEL, Math.min(15, lightLevel));
            if (level.getBlockState(target) != wanted) {
                level.setBlock(target, wanted, Block.UPDATE_CLIENTS);
            }
            this.current = target;
        }
    }

    /** Cabeza primero; si está ocupada, a la altura de los pies. Solo en aire (nunca en agua ni bloques). */
    private static @Nullable BlockPos findSpot(ServerLevel level, Mob mob) {
        BlockPos feet = mob.blockPosition();
        for (BlockPos pos : new BlockPos[] {feet.above(), feet}) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.is(ModBlocks.NOXIS_LIGHT)) {
                return pos;
            }
        }
        return null;
    }

    /** Llamar cuando la entidad se elimina (muerte, descarga, etc.). */
    public void clear(Level level) {
        if (this.current != null) {
            clearAt(level, this.current);
            this.current = null;
        }
    }

    private static void clearAt(Level level, BlockPos pos) {
        if (level.getBlockState(pos).is(ModBlocks.NOXIS_LIGHT)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
