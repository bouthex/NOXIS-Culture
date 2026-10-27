package com.noxisculture.entity.ai;

/**
 * Noxis que se aburren (todas las especies, actuales y futuras): cuando llevan un rato quietos
 * sin hacer nada, a veces hacen un gesto de aburrimiento ({@link NoxisBoredGoal}).
 */
public interface NoxisBored {
    /**
     * ¿Está tranquilo, con las manos libres y sin nada importante en curso? (sin comercio,
     * descanso, sueño, miedo, felicidad, objetos en la mano ni otras interacciones).
     */
    boolean canBeBored();

    /** Cuántos ticks lleva quieto y tranquilo sin hacer nada. */
    int getIdleTicks();

    /** Vuelve a contar el rato sin hacer nada desde cero. */
    void resetIdleTicks();

    /** ¿Lleva puesto su sombrero? (para acomodárselo). */
    boolean hasHat();

    /** Gesto de aburrimiento en curso ({@code NoxisBoredAnimation}, sincronizado; NONE = ninguno). */
    byte getBoredAnim();

    void setBoredAnim(byte anim);
}
