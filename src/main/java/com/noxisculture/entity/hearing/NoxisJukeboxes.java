package com.noxisculture.entity.hearing;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Tocadiscos que están sonando (servidor). Cuando un disco empieza o termina, Minecraft avisa con
 * sus eventos de juego (los mismos que escucha el allay para bailar); los oídos de cualquier Noxis
 * cercano anotan acá el tocadiscos. Así cada Noxis solo mira esta listita (unos pocos lugares)
 * en vez de buscar tocadiscos por el mundo.
 *
 * <p>Que esté sonando lo confirma siempre el propio tocadiscos (su reproductor de canciones), así
 * que funciona con cualquier disco registrado como canción de tocadiscos: los de Minecraft, los
 * de otros mods y los futuros discos Noxis, sin nombrar ninguno.</p>
 */
public final class NoxisJukeboxes {
    private static final Map<ServerLevel, Map<BlockPos, Boolean>> PLAYING = new WeakHashMap<>();

    private NoxisJukeboxes() {}

    /** Un disco que suena: dónde está, desde qué tick del mundo suena y cuántos ticks dura cada tiempo. */
    public record Music(BlockPos pos, long origin, int beatTicks,
                        net.minecraft.resources.@Nullable ResourceKey<net.minecraft.world.item.JukeboxSong> song) {
        public Vec3 center() {
            return Vec3.atCenterOf(this.pos);
        }
    }

    public static void onPlay(ServerLevel level, BlockPos pos) {
        PLAYING.computeIfAbsent(level, l -> new HashMap<>()).put(pos.immutable(), Boolean.TRUE);
    }

    public static void onStop(ServerLevel level, BlockPos pos) {
        Map<BlockPos, Boolean> map = PLAYING.get(level);
        if (map != null) map.remove(pos);
    }

    /** ¿Ese tocadiscos sigue sonando? Devuelve su música, o null si ya no suena (o ya no está). */
    public static @Nullable Music check(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox)) return null;
        JukeboxSongPlayer player = jukebox.getSongPlayer();
        if (!player.isPlaying()) return null;
        long origin = level.getGameTime() - player.getTicksSinceSongStarted();
        net.minecraft.core.Holder<net.minecraft.world.item.JukeboxSong> song = player.getSong();
        return new Music(pos, origin, NoxisDanceTempo.beatTicks(song), song == null ? null : song.unwrapKey().orElse(null));
    }

    /** El tocadiscos más cercano que está sonando ESA canción (o null). */
    public static @Nullable Music findNearSong(ServerLevel level, Vec3 from, double radius,
                                               net.minecraft.resources.ResourceKey<net.minecraft.world.item.JukeboxSong> song) {
        Map<BlockPos, Boolean> map = PLAYING.get(level);
        if (map == null || map.isEmpty()) return null;
        for (BlockPos pos : map.keySet()) {
            if (Vec3.atCenterOf(pos).distanceToSqr(from) > radius * radius) continue;
            Music music = check(level, pos);
            if (music != null && song.equals(music.song())) return music;
        }
        return null;
    }

    /** El tocadiscos sonando más cercano dentro del radio (o null). Limpia los que ya no suenan. */
    public static @Nullable Music findNear(ServerLevel level, Vec3 from, double radius) {
        Map<BlockPos, Boolean> map = PLAYING.get(level);
        if (map == null || map.isEmpty()) return null;
        Music best = null;
        double bestD = radius * radius;
        Iterator<BlockPos> it = map.keySet().iterator();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            double d = Vec3.atCenterOf(pos).distanceToSqr(from);
            if (d > bestD) continue;
            Music music = check(level, pos);
            if (music == null) {
                if (level.isLoaded(pos)) it.remove();          // terminó (o lo rompieron): se olvida
                continue;
            }
            best = music;
            bestD = d;
        }
        return best;
    }
}
