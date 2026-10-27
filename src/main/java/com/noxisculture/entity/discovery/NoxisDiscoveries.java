package com.noxisculture.entity.discovery;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Recuerdos de descubrimientos de UN Noxis (servidor; se guardan con él). Compactos: como mucho
 * {@link #MAX} cosas, cada una con un número (el tipo de bloque, objeto o bioma) y unos pocos bits:
 *
 * <ul>
 *   <li>{@link #SEEN} — lo vio (aunque sea de lejos).</li>
 *   <li>{@link #KNOWN} — se acercó y lo conoció bien.</li>
 *   <li>{@link #COLLECTED} — alguna vez lo tuvo en su inventario.</li>
 * </ul>
 * Cuando se llena, se olvida lo que hace más tiempo que no le llama la atención. No es conocimiento
 * de la aldea: cada Noxis tiene los suyos.
 */
public final class NoxisDiscoveries {
    public static final byte SEEN = 1;
    public static final byte KNOWN = 2;
    public static final byte COLLECTED = 4;
    public static final int MAX = 96;

    /** Espacios de nombres de las claves (para que un bloque y un objeto con el mismo número no choquen). */
    public static final int BLOCK = 1 << 28;
    public static final int ITEM = 2 << 28;
    public static final int BIOME = 3 << 28;

    private final LinkedHashMap<Integer, Byte> memory = new LinkedHashMap<>(MAX, 0.75F, true);

    public static int key(int space, int id) {
        return space | (id & 0x0FFFFFFF);
    }

    public byte get(int key) {
        Byte b = this.memory.get(key);
        return b == null ? 0 : b;
    }

    public boolean has(int key, byte flag) {
        return (this.get(key) & flag) != 0;
    }

    public void mark(int key, byte flag) {
        this.memory.put(key, (byte) (this.get(key) | flag));
        if (this.memory.size() > MAX) {
            Iterator<Map.Entry<Integer, Byte>> it = this.memory.entrySet().iterator();
            it.next();
            it.remove();
        }
    }

    public int size() {
        return this.memory.size();
    }

    /** Para guardar: pares (clave, bits) en un solo arreglo de enteros. */
    public int[] save() {
        int[] out = new int[this.memory.size() * 2];
        int i = 0;
        for (Map.Entry<Integer, Byte> e : this.memory.entrySet()) {
            out[i++] = e.getKey();
            out[i++] = e.getValue();
        }
        return out;
    }

    public void load(int[] data) {
        this.memory.clear();
        for (int i = 0; i + 1 < data.length; i += 2) {
            this.memory.put(data[i], (byte) data[i + 1]);
        }
    }
}
