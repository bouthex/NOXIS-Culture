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

    private static SoundEvent register(String name) {
        Identifier id = NoxisCulture.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void initialize() {}
}
