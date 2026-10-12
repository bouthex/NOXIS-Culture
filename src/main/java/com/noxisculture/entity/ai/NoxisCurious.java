package com.noxisculture.entity.ai;

/**
 * Noxis que siguen a sus compañeros por curiosidad y, muy de vez en cuando, les hacen una
 * travesura (todas las especies, actuales y futuras: aldeano, guerrero, sabio...).
 */
public interface NoxisCurious extends NoxisSocial, com.noxisculture.entity.personality.NoxisPersonable {

    /** ¿Está libre para ponerse a seguir a alguien? (tranquilo, manos libres, sin otra actividad). */
    boolean canFollowCuriously();

    /** ¿Está haciendo algo que llame la atención (caminando, oliendo flores, mirando un cristal...)? */
    boolean isDoingSomethingInteresting();

    /** ¿Se le puede hacer una travesura ahora? (quieto, tranquilo, sin otra actividad). */
    boolean canBePranked();

    /** Estado de curiosidad / travesura ({@code NoxisPlayAnimation}, sincronizado). */
    byte getPlayAnim();

    void setPlayAnim(byte anim);

    /** Lo sigue alguien (lo renueva el que lo sigue, cada tick). */
    void markFollowedBy(int followerId, long until);

    /** ¿Lo está siguiendo OTRO Noxis ahora? */
    boolean isFollowedByOther(int followerId, long now);

    /** Susto breve (la reacción de miedo de siempre, unos pocos ticks). Nunca daña. */
    void startle(int ticks);

    /** Contento un ratito (la expresión feliz de siempre). */
    void cheerUp(int ticks);
}
