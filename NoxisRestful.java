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

    /**
     * ¿Puede dormirse solo ya? Las especies que buscan compañía antes de dormir devuelven false
     * hasta haber buscado sin encontrar a nadie.
     */
    default boolean mayRestAlone() {
        return true;
    }

    /** Ya buscó compañía y no encontró: durante este rato puede dormirse solo. */
    default void allowRestAlone(int ticks) {
    }

    void onRestFinished();

    /** Empieza a acomodarse para descansar (mira el lugar, se sienta y se acomoda con un balanceo). */
    default void startSettle() {
    }

    /**
     * Se despertó (terminó su descanso normalmente): se estira. {@code seated}: sigue sentado al
     * empezar (descanso solo o en compañía); false: ya está de pie (al salir de la pecera).
     */
    default void startWakeStretch(boolean seated) {
    }

    /** ¿Se está estirando? (mientras tanto se queda quieto). */
    default boolean isStretching() {
        return false;
    }
}
