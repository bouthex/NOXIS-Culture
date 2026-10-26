package com.noxisculture.entity.personality;

/** Noxis con personalidad propia (todas las especies, actuales y futuras). */
public interface NoxisPersonable {
    NoxisPersonality getPersonality();

    /** ¿Está libre para su gesto propio? (tranquilo, quieto, sin ninguna otra actividad en curso). */
    boolean canDoPersonaGesture();

    /** Gesto propio en curso ({@code NoxisPersonaAnimation}, sincronizado; NONE = ninguno). */
    byte getPersonaGesture();

    void setPersonaGesture(byte gesture);
}
