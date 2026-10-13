package com.noxisculture.entity.hearing;

import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.JukeboxSong;
import org.jspecify.annotations.Nullable;

/**
 * Pulso del baile. Minecraft no dice el tempo de una canción (solo su duración y en qué tick va),
 * así que los Noxis bailan sobre una grilla simple de 120 pulsos por minuto (un tiempo cada 10
 * ticks) que arranca JUSTO cuando empezó el disco: todos los que bailan con el mismo tocadiscos
 * van al mismo pulso, y el pulso queda pegado al comienzo de la canción.
 *
 * <p>Sirve igual para cualquier disco (vanilla o de otros mods). Si algún día queremos un tempo
 * exacto para un disco (por ejemplo, los futuros discos Noxis), se agrega acá su clave y cuántos
 * ticks dura cada tiempo; los demás siguen usando el pulso por defecto.</p>
 */
public final class NoxisDanceTempo {
    /** 120 pulsos por minuto = 2 tiempos por segundo = 10 ticks por tiempo. */
    public static final int DEFAULT_BEAT_TICKS = 10;

    /** Tempos exactos de los discos que conocemos (ticks por tiempo); el resto usa el pulso por defecto. */
    private static final Map<ResourceKey<JukeboxSong>, Integer> KNOWN = Map.of(
            com.noxisculture.sound.ModJukeboxSongs.GOLDEN_SIESTA, 14,    // «Siesta dorada»: 85,7 pulsos por minuto
            com.noxisculture.sound.ModJukeboxSongs.NIGHT_RIFT, 15);      // «Grieta nocturna»: 80 pulsos por minuto

    private NoxisDanceTempo() {}

    /** Ticks por tiempo de una canción por su clave (si no se conoce, el pulso por defecto). */
    public static int beatTicksOf(ResourceKey<JukeboxSong> key) {
        return KNOWN.getOrDefault(key, DEFAULT_BEAT_TICKS);
    }

    public static int beatTicks(@Nullable Holder<JukeboxSong> song) {
        if (song == null) return DEFAULT_BEAT_TICKS;
        return song.unwrapKey().map(KNOWN::get).orElse(DEFAULT_BEAT_TICKS);
    }
}
