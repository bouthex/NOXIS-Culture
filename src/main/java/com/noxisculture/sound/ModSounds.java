package com.noxisculture.sound;

import com.noxisculture.NoxisCulture;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Voz de los Noxis: gatito + extraterrestre, aguda y tierna.
 * TODAS las especies Noxis usan estos sonidos hasta que les demos voz propia.
 */
public final class ModSounds {
    private ModSounds() {}

    public static final SoundEvent NOXIS_AMBIENT = register("entity.noxis.ambient");
    public static final SoundEvent NOXIS_HAPPY = register("entity.noxis.happy");
    public static final SoundEvent NOXIS_SCARED = register("entity.noxis.scared");
    public static final SoundEvent NOXIS_HURT = register("entity.noxis.hurt");
    public static final SoundEvent NOXIS_DEATH = register("entity.noxis.death");
    public static final SoundEvent NOXIS_YES = register("entity.noxis.yes");
    public static final SoundEvent NOXIS_NO = register("entity.noxis.no");
    public static final SoundEvent NOXIS_TRADE = register("entity.noxis.trade");
    public static final SoundEvent NOXIS_YAWN = register("entity.noxis.yawn");
    /** Festejo al concretar un tradeo (y más fuerte al subir de nivel). */
    public static final SoundEvent NOXIS_CELEBRATE = register("entity.noxis.celebrate");
    /** Olfateo curioso al acercar una flor a la naricita. */
    public static final SoundEvent NOXIS_SNIFF = register("entity.noxis.sniff");
    /** Voces de cada personalidad (gesto propio): curioso "¿mrr?", travieso "jejeje", artista tarareo, cariñoso "¡eeey!", tranquilo ronroneo. */
    public static final SoundEvent NOXIS_PERSONA_CURIOUS = register("entity.noxis.persona_curious");
    public static final SoundEvent NOXIS_PERSONA_MISCHIEF = register("entity.noxis.persona_mischief");
    public static final SoundEvent NOXIS_PERSONA_ARTIST = register("entity.noxis.persona_artist");
    public static final SoundEvent NOXIS_PERSONA_AFFECTION = register("entity.noxis.persona_affection");
    public static final SoundEvent NOXIS_PERSONA_CALM = register("entity.noxis.persona_calm");

    /** Disco de cristal amarillo: «Siesta dorada». */
    public static final SoundEvent MUSIC_DISC_GOLDEN_SIESTA = register("music_disc.golden_siesta");
    /** Disco de cristal violeta agrietado: «Grieta nocturna». */
    public static final SoundEvent MUSIC_DISC_NIGHT_RIFT = register("music_disc.night_rift");

    /** Noyux: burbujeo grave y viscoso con un rumor energético de fondo. */
    public static final SoundEvent NOYUX_AMBIENT = register("block.noyux.ambient");
    /** Noyux fluyendo o cayendo: una masa pesada que corre. */
    public static final SoundEvent NOYUX_FLOW = register("block.noyux.flow");
    /** Fuego violeta: crepitar seco con un tintineo de cristal. */
    public static final SoundEvent NOYUX_FIRE_CRACKLE = register("block.noyux_fire.crackle");
    public static final SoundEvent NOYUX_FIRE_IGNITE = register("block.noyux_fire.ignite");
    public static final SoundEvent NOYUX_FIRE_FIZZ = register("block.noyux_fire.extinguish");

    private static SoundEvent register(String name) {
        Identifier id = NoxisCulture.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void initialize() {}
}
