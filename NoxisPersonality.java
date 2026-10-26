package com.noxisculture.entity.personality;

import com.noxisculture.sound.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;

/**
 * Personalidad de un Noxis (todas las especies: aldeano, guerrero, sabio...). Se elige una sola
 * vez al aparecer y se guarda con el Noxis. Solo CAMBIA PROBABILIDADES: todas las personalidades
 * pueden hacer todo (un tranquilo también puede hacer una travesura, solo que muy rara vez).
 *
 * <p>Cada una tiene su gesto propio ({@code NoxisPersonaAnimation}) y su vocalización.</p>
 */
public enum NoxisPersonality {
    /** Se interesa por todo: objetos, cristales y lo que hacen los demás. */
    CURIOUS("curious", 22, 2.0F, 40, 1.0F),
    /** Pillo: sigue a los demás a escondidas y les gasta bromas. */
    MISCHIEVOUS("mischievous", 16, 1.4F, 5, 1.0F),
    /** Artista y sensible: contempla flores y detalles, momentos tranquilos. */
    ARTIST("artist", 20, 0.8F, 80, 1.0F),
    /** Cariñoso y sociable: busca a sus compañeros y muestra más cariño. */
    AFFECTIONATE("affectionate", 22, 1.6F, 50, 0.6F),
    /** Tranquilo: calmo, disfruta de descansar. */
    CALM("calm", 20, 0.5F, 150, 1.2F);

    private final String id;
    /** Peso al elegir la personalidad (de cada 100). */
    private final int weight;
    private final float followWeight;
    private final int prankOneIn;
    private final float affectionCooldown;

    NoxisPersonality(String id, int weight, float followWeight, int prankOneIn, float affectionCooldown) {
        this.id = id;
        this.weight = weight;
        this.followWeight = followWeight;
        this.prankOneIn = prankOneIn;
        this.affectionCooldown = affectionCooldown;
    }

    public String id() {
        return this.id;
    }

    /** Cuánto le gusta seguir a otros por curiosidad (1 = normal). */
    public float followWeight() {
        return this.followWeight;
    }

    /** De cada cuántos seguimientos (con su espera cumplida) intenta una travesura. */
    public int prankOneIn() {
        return this.prankOneIn;
    }

    /** Multiplica la espera entre gestos de cariño con su amigo (menos = más seguido). */
    public float affectionCooldown() {
        return this.affectionCooldown;
    }

    /** Ganas de acercarse a mirar objetos (1 = normal). */
    public float inspectWeight() {
        return switch (this) {
            case CURIOUS -> 2.0F;
            case MISCHIEVOUS -> 1.3F;
            case CALM -> 0.6F;
            default -> 1.0F;
        };
    }

    /** Ganas de sacar y contemplar sus tesoros (1 = normal). */
    public float keepsakeWeight() {
        return switch (this) {
            case ARTIST -> 1.8F;
            case AFFECTIONATE -> 1.6F;
            case CALM -> 1.2F;
            case MISCHIEVOUS -> 0.7F;
            default -> 1.0F;
        };
    }

    /** Chance de quedarse un tesoro suelto que encontró en el piso (después de mirarlo). */
    public float keepChance() {
        return switch (this) {
            case MISCHIEVOUS -> 0.6F;
            case CURIOUS, ARTIST -> 0.5F;
            default -> 0.35F;
        };
    }

    /** Ganas de pararse a escuchar un sonido interesante (multiplica la chance; nunca es 0). */
    public float listenWeight() {
        return switch (this) {
            case CURIOUS -> 1.0F;
            case ARTIST, MISCHIEVOUS -> 0.9F;
            case AFFECTIONATE -> 0.85F;
            case CALM -> 0.8F;
        };
    }

    /** Chance de ir a investigar después de escuchar (los curiosos, mucho; los tranquilos casi nunca). */
    public float investigateChance(boolean music) {
        return switch (this) {
            case CURIOUS -> 0.6F;
            case MISCHIEVOUS -> 0.45F;
            case ARTIST -> music ? 0.5F : 0.25F;          // a los artistas los atrae la música
            case AFFECTIONATE -> 0.3F;
            case CALM -> 0.1F;
        };
    }

    /** Chance de ponerse a bailar cuando suena un disco cerca (todos pueden; ninguno está prohibido). */
    public float danceChance() {
        return switch (this) {
            case AFFECTIONATE -> 0.75F;
            case MISCHIEVOUS, ARTIST -> 0.7F;
            case CURIOUS -> 0.6F;
            case CALM -> 0.4F;
        };
    }

    /** Ganas de bailar en pareja con otro que baila cerca (multiplica la chance). */
    public float pairDanceWeight() {
        return switch (this) {
            case AFFECTIONATE -> 1.5F;
            case MISCHIEVOUS -> 1.2F;
            case CURIOUS, ARTIST -> 1.0F;
            case CALM -> 0.7F;
        };
    }

    /** Número del gesto propio (ver {@code NoxisPersonaAnimation}). */
    public byte gesture() {
        return (byte) (this.ordinal() + 1);
    }

    /** Su vocalización característica. */
    public SoundEvent voice() {
        return switch (this) {
            case CURIOUS -> ModSounds.NOXIS_PERSONA_CURIOUS;
            case MISCHIEVOUS -> ModSounds.NOXIS_PERSONA_MISCHIEF;
            case ARTIST -> ModSounds.NOXIS_PERSONA_ARTIST;
            case AFFECTIONATE -> ModSounds.NOXIS_PERSONA_AFFECTION;
            case CALM -> ModSounds.NOXIS_PERSONA_CALM;
        };
    }

    public static NoxisPersonality random(RandomSource random) {
        int total = 0;
        for (NoxisPersonality p : values()) total += p.weight;
        int roll = random.nextInt(total);
        for (NoxisPersonality p : values()) {
            roll -= p.weight;
            if (roll < 0) return p;
        }
        return CURIOUS;
    }

    public static NoxisPersonality byId(String id) {
        for (NoxisPersonality p : values()) {
            if (p.id.equals(id)) return p;
        }
        return null;
    }
}
