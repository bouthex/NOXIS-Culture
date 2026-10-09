package com.noxisculture.entity.social;

/** Gesto social que se está mostrando (se sincroniza al cliente como un byte para animarlo). */
public final class NoxisSocialAction {
    private NoxisSocialAction() {}

    public static final byte NONE = 0;
    /** Saludo: ladea la cabecita y saluda con la patita. */
    public static final byte GREET_WAVE = 1;
    /** Saludo: ladea la cabecita y asiente. */
    public static final byte GREET_NOD = 2;
    /** Saludo: asiente y da un saltito chiquito. */
    public static final byte GREET_NOD_HOP = 3;
    /** Descanso en compañía: le agarra sueño (cabecea). */
    public static final byte REST_DROWSY = 10;
    /** Descanso en compañía: mira al que tiene sueño y se contagia. */
    public static final byte REST_WATCH = 11;
    /** Descanso en compañía: sentado, apoyado en el compañero que tiene a su DERECHA. */
    public static final byte REST_LEAN_RIGHT = 12;
    /** Descanso en compañía: sentado, apoyado en el compañero que tiene a su IZQUIERDA. */
    public static final byte REST_LEAN_LEFT = 13;

    /** Evento de entidad: bostezo (para animarlo en el cliente). */
    public static final byte YAWN_EVENT = 71;
}
