package com.noxisculture.entity.mood;

/**
 * Estados de ánimo compartidos por TODAS las especies Noxis
 * (aldeanos, guerreros, sabios). Se sincronizan al cliente como un byte.
 */
public enum NoxisMood {
    NEUTRAL,
    HAPPY,
    SCARED;

    private static final NoxisMood[] VALUES = values();

    public byte id() {
        return (byte) this.ordinal();
    }

    public static NoxisMood byId(byte id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : NEUTRAL;
    }
}
