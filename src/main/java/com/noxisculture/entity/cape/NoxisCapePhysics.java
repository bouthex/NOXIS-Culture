package com.noxisculture.entity.cape;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Física visual liviana de la capa Noxis (solo cliente), al estilo de la capa del jugador:
 * la capa sigue al cuerpo con un poco de retraso/inercia.
 *  - swing: cuánto "vuela" según la velocidad (quieto 0, caminando ~0.5, corriendo 1).
 *  - turn: al girar, la capa queda un instante hacia el lado contrario y se acomoda.
 * Reutilizable por cualquier especie Noxis: basta con llamar a {@link #tick} en el tick de cliente.
 */
public final class NoxisCapePhysics {
    private float swing;
    private float swingO;
    private float turn;
    private float turnO;

    public void tick(LivingEntity entity) {
        this.swingO = this.swing;
        this.turnO = this.turn;
        double dx = entity.getX() - entity.xo;
        double dz = entity.getZ() - entity.zo;
        float speed = (float) Math.sqrt(dx * dx + dz * dz);
        float swingTarget = Mth.clamp(speed * 7.0F, 0.0F, 1.0F);
        // Sube rápido con el "viento" y, al frenar, tarda un instante en volver a caer.
        float rate = swingTarget > this.swing ? 0.22F : 0.07F;
        this.swing += (swingTarget - this.swing) * rate;
        float yawDelta = Mth.wrapDegrees(entity.yBodyRot - entity.yBodyRotO);
        float turnTarget = Mth.clamp(-yawDelta / 15.0F, -1.0F, 1.0F);
        this.turn += (turnTarget - this.turn) * 0.18F;            // se acomoda despacito al dejar de girar
    }

    public float getSwing(float partialTick) {
        return Mth.lerp(partialTick, this.swingO, this.swing);
    }

    public float getTurn(float partialTick) {
        return Mth.lerp(partialTick, this.turnO, this.turn);
    }
}
