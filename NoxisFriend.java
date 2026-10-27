package com.noxisculture.entity.ai;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Noxis que pueden tener un amigo especial (todas las especies, actuales y futuras: aldeano,
 * guerrero, sabio...). Cada uno tiene como mucho UN amigo, el vínculo es mutuo, se guarda con
 * el mundo y no cambia salvo que el amigo deje de existir. Algunos son independientes y nunca
 * forman un vínculo (así no todos son amigos de todos).
 */
public interface NoxisFriend extends NoxisSocial {
    /** El amigo especial (o null si no tiene). */
    @Nullable UUID getFriendId();

    void setFriendId(@Nullable UUID friend);

    /** ¿Es de los que prefieren no atarse a nadie? (se decide una vez y se guarda). */
    boolean isIndependent();

    /** Gesto de cariño en curso ({@code NoxisAffectionAnimation}, sincronizado; NONE = ninguno). */
    void setAffectionAnim(byte anim);

    /** Desde cuándo puede volver a mostrar cariño (reloj del mundo). */
    long getNextAffectionTime();

    void setNextAffectionTime(long time);

    /** ¿Está libre para un gesto de cariño ahora? (por defecto, igual que para socializar). */
    default boolean canShowAffection() {
        return this.canSocialize();
    }
}
