package com.noxisculture.entity.ai;

/** Entidades Noxis que pueden sentarse a descansar (todas las especies). */
public interface NoxisRestful {
    /** ¿Ya pasó el tiempo desde el último descanso y se siente seguro? */
    boolean wantsToRest();

    /** ¿Sigue sintiéndose seguro? (sin miedo, sin comerciar, sin antorcha). */
    boolean isSafeToRest();

    void setResting(boolean resting);

    /** ¿Está descansando ahora? */
    boolean isResting();

    void onRestFinished();
}
