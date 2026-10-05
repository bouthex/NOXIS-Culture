package com.noxisculture.entity.mood;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/**
 * Lógica de emociones (solo servidor). Se usa por composición: cada especie
 * Noxis tiene su propia instancia, sin importar de qué clase vanilla herede.
 *
 *  - MIEDO: al recibir daño o con un monstruo cerca. El miedo siempre gana.
 *  - FELIZ: al completar un tradeo o si un jugador cercano sostiene una esmeralda.
 */
public final class NoxisMoodController {
    private static final int SCAN_INTERVAL_TICKS = 10;
    private static final double DANGER_RADIUS = 6.0D;
    private static final double EMERALD_RADIUS = 4.0D;
    private static final int SCARED_ON_HURT_TICKS = 100;
    private static final int SCARED_ON_MONSTER_TICKS = 30;
    private static final int HAPPY_ON_EMERALD_TICKS = 20;

    private int happyTicks;
    private int scaredTicks;

    public void makeHappy(int ticks) {
        this.happyTicks = Math.max(this.happyTicks, ticks);
    }

    public void makeScared(int ticks) {
        this.scaredTicks = Math.max(this.scaredTicks, ticks);
    }

    /** Llamar una vez por tick en el servidor. Devuelve el ánimo actual. */
    public NoxisMood tick(Mob mob) {
        if (mob.hurtTime > 0) {
            this.makeScared(SCARED_ON_HURT_TICKS);
        }
        if (mob.tickCount % SCAN_INTERVAL_TICKS == 0) {
            if (!mob.level().getEntitiesOfClass(Monster.class, mob.getBoundingBox().inflate(DANGER_RADIUS)).isEmpty()) {
                this.makeScared(SCARED_ON_MONSTER_TICKS);
            }
            Player player = mob.level().getNearestPlayer(mob, EMERALD_RADIUS);
            if (player != null && player.isHolding(Items.EMERALD)) {
                this.makeHappy(HAPPY_ON_EMERALD_TICKS);
            }
        }
        if (this.happyTicks > 0) this.happyTicks--;
        if (this.scaredTicks > 0) this.scaredTicks--;

        if (this.scaredTicks > 0) return NoxisMood.SCARED;
        if (this.happyTicks > 0) return NoxisMood.HAPPY;
        return NoxisMood.NEUTRAL;
    }
}
