package com.noxisculture.entity.idle;

import net.minecraft.util.Mth;

/**
 * Reaccionar a un sonido (solo visual, cliente; el servidor dice en qué etapa está y hacia dónde
 * está el sonido respecto de su cabeza). Coreografía propia, compartida por todas las especies:
 *
 * <ul>
 *   <li>{@link #PERK} — <b>escuchar</b>: un respingo chiquito ("¡eh!"), las orejitas se paran,
 *   se adelantan y giran hacia el sonido (la del lado del sonido gira más y la otra se echa
 *   atrás); la cabeza lo busca con un vaivén corto que se apaga (el giro real lo hace el
 *   servidor); después, la pausa de escucha: cabeza ladeada con la orejita del sonido arriba,
 *   quieto, la patita de ese lado levantada junto a la carita, la colita alerta y quieta, y
 *   unos tiquitos de oreja. Cierra con un cabeceo chiquito ("ajá").</li>
 *   <li>{@link #INVESTIGATE} — <b>ir a investigar</b>: camina con el cuello estirado, las dos
 *   orejitas bien adelante y las patitas un poco al frente; cada tanto para una orejita
 *   ("¿sigue ahí?").</li>
 *   <li>{@link #PEEK} — <b>¿y esto?</b>: al llegar se inclina a mirar, ladea la cabeza a un lado y
 *   al otro (con la orejita de cada lado), y termina con un saltito de "¡encontrado!".</li>
 * </ul>
 */
public final class NoxisListenAnimation {
    public static final byte NONE = 0;
    public static final byte PERK = 1;
    public static final byte INVESTIGATE = 2;
    public static final byte PEEK = 3;
    private static final int COUNT = 4;

    public static final int PERK_LENGTH = 56;
    public static final int PEEK_LENGTH = 42;

    private byte state = NONE;
    private final float[] weight = new float[COUNT];
    private final float[] weightO = new float[COUNT];
    private final int[] time = new int[COUNT];
    private float dir;
    private float dirO;
    /** De qué lado quedó el sonido al empezar a escuchar (+1 / -1): no cambia durante la escucha. */
    private float side = 1.0F;
    private boolean sideLatched;

    public void tick(byte serverState, byte serverDir) {
        if (serverState < 0 || serverState >= COUNT) serverState = NONE;
        if (serverState != this.state) {
            this.state = serverState;
            this.time[serverState] = 0;
            if (serverState == PERK) this.sideLatched = false;
        }
        for (int i = 1; i < COUNT; i++) {
            this.weightO[i] = this.weight[i];
            boolean on = i == this.state;
            this.weight[i] = on ? Math.min(1.0F, this.weight[i] + 0.25F) : Math.max(0.0F, this.weight[i] - 0.15F);
            if (on || this.weight[i] > 0.0F) this.time[i]++;
        }
        // La dirección se acomoda suave (las orejitas no saltan de golpe).
        this.dirO = this.dir;
        float target = Mth.clamp(serverDir / 100.0F, -1.0F, 1.0F);
        if (this.state != NONE) this.dir += Mth.clamp(target - this.dir, -0.2F, 0.2F);
        if (this.state == PERK && !this.sideLatched && Math.abs(target) > 0.08F) {
            this.side = Math.signum(target);
            this.sideLatched = true;
        }
    }

    public boolean isBusy() {
        if (this.state != NONE) return true;
        for (int i = 1; i < COUNT; i++) if (this.weight[i] > 0.0F) return true;
        return false;
    }

    public byte getState() {
        return this.state;
    }

    public float getWeight(byte s, float pt) {
        return Mth.lerp(pt, this.weightO[s], this.weight[s]);
    }

    public float getTime(byte s, float pt) {
        return this.time[s] + pt;
    }

    public float getDir(float pt) {
        return Mth.lerp(pt, this.dirO, this.dir);
    }

    /** Lado del sonido (+1 = su derecha): la orejita y la patita de ese lado hacen la escucha. */
    public float getSide() {
        return this.side;
    }

    // ------------------------------------------------------------------ ojitos

    /** Pupilas grandes: el "¡eh!" del principio y el "¿y esto?" al llegar. */
    public static boolean bigPupils(byte state, float t) {
        return (state == PERK && t >= 2.0F && t < 14.0F) || (state == PEEK && t >= 4.0F && t < 30.0F);
    }

    /** Mientras escucha no parpadea (atento). */
    public static boolean noBlink(byte state, float t) {
        return (state == PERK && t < 48.0F) || state == INVESTIGATE;
    }

    /** Ojitos cerrados de satisfacción en el saltito de "¡encontrado!". */
    public static boolean closed(byte state, float t) {
        return state == PEEK && t >= 33.0F && t < 37.0F;
    }
}
