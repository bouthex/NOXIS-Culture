package com.noxisculture.sound;

import com.noxisculture.NoxisCulture;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.JukeboxSong;

/**
 * Canciones de tocadiscos de Noxis Culture. Cada canción se define con datos (como las de
 * Minecraft) en {@code data/noxis_culture/jukebox_song/}; acá solo están sus claves.
 */
public final class ModJukeboxSongs {
    private ModJukeboxSongs() {}

    /** «Siesta dorada»: el disco de cristal amarillo (lo-fi tranquilo, 1 tiempo = 14 ticks). */
    public static final ResourceKey<JukeboxSong> GOLDEN_SIESTA =
            ResourceKey.create(Registries.JUKEBOX_SONG, NoxisCulture.id("golden_siesta"));

    /** «Grieta nocturna»: el disco de cristal violeta agrietado (boom-bap lento, 1 tiempo = 15 ticks). */
    public static final ResourceKey<JukeboxSong> NIGHT_RIFT =
            ResourceKey.create(Registries.JUKEBOX_SONG, NoxisCulture.id("night_rift"));
}
