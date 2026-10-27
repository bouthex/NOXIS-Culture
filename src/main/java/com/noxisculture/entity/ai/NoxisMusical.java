package com.noxisculture.entity.ai;

/**
 * Noxis que tararean o silban melodías espontáneas, y que reaccionan cuando otro lo hace
 * (todas las especies, actuales y futuras).
 */
public interface NoxisMusical extends com.noxisculture.entity.personality.NoxisPersonable {
    /** ¿Está libre para tararear o para reaccionar a una melodía? (tranquilo, sin otra actividad). */
    boolean canHum();

    /** Estado de melodía ({@code NoxisHumAnimation}, sincronizado). */
    byte getHum();

    void setHum(byte state);

    /** Los lectores y escritores (los "creativos") tararean más seguido. */
    boolean isScholar();
}
