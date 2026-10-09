package com.noxisculture.entity.ai;

import com.noxisculture.entity.nature.NoxisNatureAction;
import com.noxisculture.sound.ModSounds;
import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Recibir una flor de regalo de otro Noxis (servidor). El que regala lanza la flor como objeto
 * real; este Noxis la espera mirándolo, la atrapa cuando llega (con corazones y carita feliz),
 * la sostiene unos segundos mirándola con ternura y la guarda en su inventario.
 *
 * <p>Sin duplicados ni pérdidas: al atraparla, el objeto del mundo desaparece y la MISMA flor
 * entra en el inventario (que Minecraft ya guarda con el Noxis). Lo que se ve en la mano es solo
 * una imagen de esa flor. Si alguien la levanta antes, o no llega, el Noxis no recibe nada y la
 * flor sigue en el piso para quien la quiera.</p>
 */
public class NoxisReceiveGiftGoal<T extends PathfinderMob & NoxisNatureLover> extends Goal {
    private static final int MAX_WAIT = 60;     // 3 s esperando que llegue
    private static final int HOLD_TICKS = 90;   // 4,5 s mirándola con ternura

    private final T mob;
    private int ticks;
    private boolean caught;

    public NoxisReceiveGiftGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.mob.getIncomingGift() != null;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.caught = false;
        this.mob.getNavigation().stop();
        this.mob.setNatureAction(NoxisNatureAction.RECEIVE);
    }

    @Override
    public boolean canContinueToUse() {
        if (this.mob.hurtTime > 0) return false;
        return this.caught ? this.ticks < HOLD_TICKS : this.ticks < MAX_WAIT && this.mob.getIncomingGift() != null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.ticks++;
        this.mob.getNavigation().stop();
        if (!this.caught) {
            ItemEntity gift = this.mob.getIncomingGift();
            LivingEntity giver = this.mob.getGiftGiver();
            if (gift == null || gift.isRemoved() || gift.getItem().isEmpty()) {
                this.mob.clearIncomingGift();               // alguien la levantó: nada que recibir
                return;
            }
            // Mira la flor que viene volando (o a quien se la regala, si todavía no salió).
            if (this.ticks < 6 && giver != null) this.mob.getLookControl().setLookAt(giver, 30.0F, 30.0F);
            else this.mob.getLookControl().setLookAt(gift, 30.0F, 30.0F);
            double d2 = gift.distanceToSqr(this.mob);
            boolean arrived = d2 < 1.2D * 1.2D || (gift.onGround() && d2 < 2.8D * 2.8D)
                    || (this.ticks > 30 && d2 < 4.0D * 4.0D);
            if (arrived) this.catchGift(gift);
        } else {
            // La sostiene mirándola con ternura (delante suyo, un poco abajo).
            Vec3 forward = Vec3.directionFromRotation(0.0F, this.mob.yBodyRot);
            this.mob.getLookControl().setLookAt(this.mob.getX() + forward.x, this.mob.getEyeY() - 0.15D,
                    this.mob.getZ() + forward.z, 10.0F, 30.0F);
            if (this.ticks == 40 && this.mob.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.HEART, this.mob.getX(), this.mob.getEyeY() + 0.45D,
                        this.mob.getZ(), 1, 0.1D, 0.05D, 0.1D, 0.0D);
            }
        }
    }

    private void catchGift(ItemEntity gift) {
        ItemStack flower = gift.getItem().copy();
        gift.discard();                                  // el objeto del mundo deja de existir...
        this.mob.storeGift(flower);                      // ...y esa misma flor queda guardada
        this.mob.showGiftInHand(flower);                 // en la mano: solo una imagen de ella
        this.mob.clearIncomingGift();
        this.caught = true;
        this.ticks = 0;
        this.mob.playSound(ModSounds.NOXIS_HAPPY, 0.6F, 1.2F);
        if (this.mob.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HEART, this.mob.getX(), this.mob.getEyeY() + 0.4D,
                    this.mob.getZ(), 3, 0.3D, 0.15D, 0.3D, 0.0D);
        }
    }

    @Override
    public void stop() {
        this.mob.clearIncomingGift();
        this.mob.showGiftInHand(ItemStack.EMPTY);
        this.mob.setNatureAction(NoxisNatureAction.NONE);
        this.caught = false;
    }
}
