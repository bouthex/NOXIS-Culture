package com.noxisculture.entity.nature;

/** Qué está haciendo un Noxis con la naturaleza (se sincroniza al cliente como un byte para animar). */
public final class NoxisNatureAction {
    private NoxisNatureAction() {}

    public static final byte NONE = 0;
    /** Mira varias flores, decidiendo cuál le gusta más. */
    public static final byte SURVEY = 1;
    /** Se agacha para recoger la flor con cuidado. */
    public static final byte PICK = 2;
    /** La contempla con amor y la huele. */
    public static final byte ADMIRE = 3;
    /** Se agacha para volver a plantarla. */
    public static final byte REPLANT = 4;
    /** Una última mirada a la flor ya plantada. */
    public static final byte LAST_LOOK = 5;
    /** Olfatea una flor sin recogerla. */
    public static final byte SNIFF = 6;
    /** Sentado, contemplando el paisaje. */
    public static final byte SIT = 7;
    /** Le regala la flor a alguien: se la acerca y se la lanza suavecito. */
    public static final byte GIFT = 8;
}
