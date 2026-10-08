package com.noxisculture.entity.idle;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * "Curiosidad repentina" (solo visual, se calcula en el cliente): cuando un Noxis lleva un rato
 * quieto y tranquilo, de vez en cuando ladea la cabecita y para una orejita como si hubiera
 * escuchado algo, la mantiene un instante y vuelve suave a su postura.
 *
 * <p>Reutilizable por todas las especies Noxis: cada entidad decide si está "tranquila"
 * y llama a {@link #tick}; el modelo lee {@link #getAmount}, {@link #getSide} y {@link #getFlick}.</p>
 */
public final class NoxisCuriosity {
    /** Tiempo mínimo tranquilo antes de que pueda pasar (5 s). */
    private static final int MIN_IDLE_TICKS = 100;
    /** Probabilidad por tick una vez tranquilo: en promedio, cada ~25 s. */
    private static final int CHANCE = 500;
    private static final int RISE = 8;
    private static final int HOLD = 26;
    private static final int FALL = 12;

    private int idleTicks;
    private int phase = -1;
    private float side = 1.0F;
    private float amount;
    private float amountO;
    private float flick;
    private float flickO;

    public void tick(boolean calm, RandomSource random) {
        this.amountO = this.amount;
        this.flickO = this.flick;
        if (!calm) {
            // Cualquier otra cosa (comercio, emoción, antorcha, caminar...) tiene prioridad:
            // se corta y la cabeza vuelve rápido a su lugar.
            this.idleTicks = 0;
            this.phase = -1;
            this.amount = Math.max(0.0F, this.amount - 0.25F);
            this.flick = 0.0F;
            return;
        }
        if (this.phase < 0) {
            this.idleTicks++;
            this.amount = Math.max(0.0F, this.amount - 0.25F);
            this.flick = 0.0F;
            if (this.idleTicks > MIN_IDLE_TICKS && random.nextInt(CHANCE) == 0) {
                this.phase = 0;
                this.side = random.nextBoolean() ? 1.0F : -1.0F;
            }
            return;
        }
        this.phase++;
        float target;
        if (this.phase < RISE) {
            float x = this.phase / (float) RISE;
            target = x * x * (3.0F - 2.0F * x);
        } else if (this.phase < RISE + HOLD) {
            target = 1.0F;
        } else if (this.phase < RISE + HOLD + FALL) {
            float x = 1.0F - (this.phase - RISE - HOLD) / (float) FALL;
            target = x * x * (3.0F - 2.0F * x);
        } else {
            target = 0.0F;
            this.phase = -1;
            this.idleTicks = 0;      // vuelve a esperar un rato tranquilo antes de repetir
        }
        this.amount = target;
        // Dos golpecitos de oreja al "escuchar algo", al principio de la pausa.
        int p = this.phase - RISE;
        this.flick = (p == 2 || p == 3 || p == 7) ? 1.0F : 0.0F;
    }

    public float getAmount(float partialTick) {
        return Mth.lerp(partialTick, this.amountO, this.amount);
    }

    public float getFlick(float partialTick) {
        return Mth.lerp(partialTick, this.flickO, this.flick);
    }

    /** +1 o -1: hacia qué lado ladea la cabecita (y qué orejita escucha). */
    public float getSide() {
        return this.side;
    }
}
