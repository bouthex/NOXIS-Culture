package com.noxisculture.entity.ai;

/** Entidades Noxis que se fascinan con los cristales (todas las especies, actuales y futuras). */
public interface NoxisFascinatable {
    /** ¿Está tranquilo y libre? (sin miedo, sin comerciar, sin descansar, con las manos libres). */
    boolean canBeFascinated();

    /** Avisa a los clientes que empieza/termina la reacción (para la animación y el brillo). */
    void setFascinated(boolean fascinated);
}
