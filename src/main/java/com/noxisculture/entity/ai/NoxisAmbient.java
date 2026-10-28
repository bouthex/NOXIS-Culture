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

    /** ¿Libre para mirar el cielo? (como {@link #canDoAmbient()}, pero la antorcha de la noche no lo impide). */
    default boolean canGazeSky() {
        return this.canDoAmbient();
    }

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

    /** El atril con la obra que dejó a medio pintar (para volver a continuarla); null = ninguno. Se guarda. */
    net.minecraft.core.@org.jspecify.annotations.Nullable BlockPos getWorkEasel();

    void setWorkEasel(net.minecraft.core.@org.jspecify.annotations.Nullable BlockPos pos);
}
