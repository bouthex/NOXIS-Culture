package com.noxisculture.entity.ai;

import com.noxisculture.entity.social.NoxisSocialLink;

/** Entidades Noxis que interactúan entre sí (todas las especies, actuales y futuras). */
public interface NoxisSocial {
    /** Con quién comparte una interacción y sus tiempos. */
    NoxisSocialLink getSocialLink();

    /** ¿Está tranquilo y libre para empezar una interacción? (sin comercio, miedo, objetos ni otra actividad). */
    boolean canSocialize();

    /** ¿Puede seguir en una interacción ya empezada? (como {@link #canSocialize} pero puede estar descansando). */
    boolean isSafeForSocial();

    /** Gesto social a mostrar (ver {@code NoxisSocialAction}). */
    void setSocialAnim(byte anim);

    /** Bosteza (se ve y se escucha). */
    void playYawn();
}
