package com.noxisculture.entity.ai;

import com.noxisculture.entity.discovery.NoxisDiscoveries;

/**
 * Noxis con los momentos tranquilos de la etapa 2: contemplar el cielo, compartir un rato con un
 * amigo, descubrir cosas, preocuparse por un compañero, pintar y dejar obras o documentos
 * (todas las especies, actuales y futuras).
 */
public interface NoxisAmbient extends NoxisCurious, NoxisKeeper, NoxisFriend, NoxisRoamer {
    /** ¿Libre para un momento tranquilo? (sin otra actividad, sin objetos en la mano, tranquilo). */
    boolean canDoAmbient();

    /** Estado del momento ({@code NoxisAmbientAnimation}, sincronizado). */
    byte getAmbientAnim();

    void setAmbientAnim(byte anim);

    /** Sus recuerdos de descubrimientos (se guardan con él). */
    NoxisDiscoveries getDiscoveries();

    /**
     * Última vez que le pasó algo feo de verdad (lo lastimaron o se cayó bailando), en ticks del
     * mundo; -1 = nunca. Es la única "señal de malestar" que usa la preocupación: no se inventan
     * estados nuevos.
     */
    long getLastDistressTime();

    /** Familia de estilo de un artista (0..5); se elige una vez y se guarda. */
    int getArtStyle();
}
