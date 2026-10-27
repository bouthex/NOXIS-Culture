package com.noxisculture.entity.idle;

import net.minecraft.util.Mth;

/**
 * Melodías espontáneas (solo visual, cliente; el servidor dice el estado con un byte sincronizado).
 *
 * <ul>
 *   <li>{@code 1..6} = está tarareando (1-3) o silbando (4-6) esa melodía: cabecita que marca el
 *   pulso, orejitas que se mueven a tiempo y un balanceo suave; tarareando entrecierra los ojitos,
 *   silbando levanta un poquito la cabeza.</li>
 *   <li>{@link #SWAY_BASE}{@code + n} = otro Noxis que se mece al compás de esa melodía (baile suave en
 *   el lugar, nada que ver con los bailes de los discos).</li>
 *   <li>{@link #LISTEN_BASE}{@code + n} = otro Noxis que escucha con atención (cabeza ladeada, orejas
 *   paradas, patita en el pecho).</li>
 * </ul>
 *
 * <p>Todas las melodías van a 100 BPM: un pulso cada {@link #BEAT} ticks. El servidor las empieza
 * siempre en un tick múltiplo del pulso, así el compás se calcula con la hora del mundo y quienes se
 * mecen van sincronizados con quien tararea.</p>
 */
public final class NoxisHumAnimation {
    public static final byte NONE = 0;
    public static final byte SWAY_BASE = 10;
    public static final byte LISTEN_BASE = 20;
    public static final int MELODIES = 6;
    public static final int BEAT = 12;
    /** Largo de cada melodía en ticks (el sonido dura lo mismo). */
    private static final int[] LENGTH = {79, 79, 85, 73, 73, 79};
    private static final float FADE = 0.12F;

    private byte state = NONE;
    private float weight;
    private float weightO;
    private int time;

    public static int length(int melody) {
        return melody >= 1 && melody <= MELODIES ? LENGTH[melody - 1] : 0;
    }

    public static boolean isHumming(byte s) {
        return s >= 1 && s <= MELODIES;
    }

    public static boolean isSway(byte s) {
        return s > SWAY_BASE && s <= SWAY_BASE + MELODIES;
    }

    public static boolean isListen(byte s) {
        return s > LISTEN_BASE && s <= LISTEN_BASE + MELODIES;
    }

    public static boolean isWhistle(byte s) {
        return isHumming(s) && s >= 4;
    }

    public void tick(byte serverState) {
        this.weightO = this.weight;
        if (serverState != NONE) {
            if (serverState != this.state) {
                this.state = serverState;
                this.time = 0;
            }
            this.weight = Math.min(1.0F, this.weight + FADE);
            this.time++;
        } else {
            this.weight = Math.max(0.0F, this.weight - FADE);
            if (this.weight <= 0.0F) {
                this.state = NONE;
                this.time = 0;
            } else {
                this.time++;
            }
        }
    }

    public byte getState() {
        return this.state;
    }

    public float getWeight(float pt) {
        return Mth.lerp(pt, this.weightO, this.weight);
    }

    public float getTime(float pt) {
        return this.time + pt;
    }

    public boolean isBusy() {
        return this.state != NONE;
    }
}
