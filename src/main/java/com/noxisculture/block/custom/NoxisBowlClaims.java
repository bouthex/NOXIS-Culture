package com.noxisculture.block.custom;

import com.noxisculture.block.ModBlocks;
import com.noxisculture.entity.ai.NoxisBowlSleeper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Quién tiene cada pecera (servidor). Una pecera, UN Noxis: el que la reserva primero es su
 * dueño (por su UUID) hasta que la libera; cualquier otro que la quiera recibe un "no".
 *
 * <p>El servidor procesa a los Noxis uno por uno, así que aunque dos elijan la misma pecera en
 * el mismo tick, solo el primero la consigue ({@link #tryClaim}). La reserva también se marca en
 * el bloque ({@link NoxisBowlBlock#OCCUPIED}), que es lo que se guarda con el mundo; al cargar,
 * cada Noxis vuelve a reclamar la suya y, si dos dicen ser dueños de la misma, gana uno y el
 * otro sale (ver NoxisBowlSleepGoal).</p>
 *
 * <p>Una reserva de un Noxis que ya no existe, que ya no apunta a esa pecera, o de una pecera
 * que ya no está, se considera vencida y se descarta sola.</p>
 */
public final class NoxisBowlClaims {
    private static final Map<ServerLevel, Map<BlockPos, UUID>> CLAIMS = new WeakHashMap<>();

    private NoxisBowlClaims() {
    }

    private static Map<BlockPos, UUID> of(ServerLevel level) {
        return CLAIMS.computeIfAbsent(level, l -> new HashMap<>());
    }

    /** El dueño vigente de la pecera (o null si está libre o la reserva venció). */
    public static @Nullable UUID owner(ServerLevel level, BlockPos pos) {
        Map<BlockPos, UUID> map = of(level);
        UUID id = map.get(pos);
        if (id == null) return null;
        Entity e = level.getEntity(id);
        boolean valid = e != null && e.isAlive() && e instanceof NoxisBowlSleeper s && pos.equals(s.getBowlPos())
                && level.getBlockState(pos).is(ModBlocks.NOXIS_BOWL);
        if (!valid) {
            map.remove(pos);
            return null;
        }
        return id;
    }

    /**
     * Intenta reservar la pecera para {@code who}. Solo funciona si está destapada y nadie más
     * la tiene. Si la consigue, la marca como ocupada en el bloque.
     */
    public static boolean tryClaim(ServerLevel level, BlockPos pos, Entity who) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.NOXIS_BOWL)) return false;
        UUID current = owner(level, pos);
        if (current != null && !current.equals(who.getUUID())) return false;
        if (current == null && state.getValue(NoxisBowlBlock.COVERED)) return false;   // tapada: no se entra
        of(level).put(pos.immutable(), who.getUUID());
        if (!state.getValue(NoxisBowlBlock.OCCUPIED)) {
            level.setBlock(pos, state.setValue(NoxisBowlBlock.OCCUPIED, true), Block.UPDATE_ALL);
        }
        return true;
    }

    /**
     * Retoma una reserva guardada (al cargar el mundo): como {@link #tryClaim}, pero aunque la
     * pecera esté tapada (el Noxis ya estaba adentro, durmiendo).
     */
    public static boolean reclaim(ServerLevel level, BlockPos pos, Entity who) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.NOXIS_BOWL)) return false;
        UUID current = owner(level, pos);
        if (current != null && !current.equals(who.getUUID())) return false;
        of(level).put(pos.immutable(), who.getUUID());
        if (!state.getValue(NoxisBowlBlock.OCCUPIED)) {
            level.setBlock(pos, state.setValue(NoxisBowlBlock.OCCUPIED, true), Block.UPDATE_ALL);
        }
        return true;
    }

    /** ¿Es {@code who} el dueño de la pecera? */
    public static boolean isOwner(ServerLevel level, BlockPos pos, Entity who) {
        UUID current = owner(level, pos);
        return current != null && current.equals(who.getUUID());
    }

    /** Libera la pecera, solo si {@code who} es su dueño (nunca le saca la pecera a otro). */
    public static void release(ServerLevel level, BlockPos pos, Entity who) {
        Map<BlockPos, UUID> map = of(level);
        UUID current = map.get(pos);
        if (current != null && !current.equals(who.getUUID())) return;
        map.remove(pos);
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.NOXIS_BOWL) && state.getValue(NoxisBowlBlock.OCCUPIED)) {
            level.setBlock(pos, state.setValue(NoxisBowlBlock.OCCUPIED, false), Block.UPDATE_ALL);
        }
    }

    /** Olvida la reserva sin tocar el bloque (el Noxis se descargó con el chunk: la retoma al volver). */
    public static void forget(ServerLevel level, BlockPos pos, Entity who) {
        Map<BlockPos, UUID> map = of(level);
        UUID current = map.get(pos);
        if (current != null && current.equals(who.getUUID())) map.remove(pos);
    }
}
