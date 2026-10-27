package com.noxisculture.entity.ai;

import com.noxisculture.entity.nature.NoxisItemClaims;
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
 * real, reservada para ESTE Noxis ({@link NoxisItemClaims}); este la espera mirándolo y la
 * atrapa cuando llega (con corazones y carita feliz), la sostiene unos segundos mirándola con
 * ternura y la guarda en su inventario.
 *
 * <p>Si la flor cae lejos, cuando toca el piso camina hasta ella (navegación normal, sin
 * atravesar nada) y la recoge con la misma animación de siempre. Si algo más importante lo
 * interrumpe (comercio, susto...), la reserva sigue y vuelve a buscarla después. Se cancela de
 * forma segura (la flor queda libre en el piso para cualquiera) si no puede llegar, se aleja
 * demasiado, tarda mucho, muere o alguien más la levanta.</p>
 *
 * <p>Sin duplicados ni pérdidas: al atraparla, el objeto del mundo desaparece y la MISMA flor
 * entra en el inventario. Lo que se ve en la mano es solo una imagen de esa flor.</p>
 */
public class NoxisReceiveGiftGoal<T extends PathfinderMob & NoxisNatureLover> extends Goal {
    private static final int MAX_WAIT = 60;          // 3 s esperando que llegue volando
    private static final int HOLD_TICKS = 90;        // 4,5 s mirándola con ternura
    private static final int FETCH_TIMEOUT = 400;    // 20 s caminando hasta ella, como mucho
    private static final int MAX_PATH_FAILS = 4;     // sin camino posible: se cancela
    private static final double MAX_DISTANCE = 24.0D;
    private static final double REACH = 1.1D;
    private static final double FETCH_SPEED = 0.45D;

    private final T mob;
    private int ticks;
    private boolean caught;
    private boolean fetching;
    private int fetchTicks;
    private int pathFails;
    /** true: el regalo terminó (recibido o cancelado); false: solo lo interrumpieron. */
    private boolean finished;

    public NoxisReceiveGiftGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        ItemEntity gift = this.mob.getIncomingGift();
        if (gift == null) return false;
        if (!this.isStillMine(gift)) {
            this.mob.clearIncomingGift();               // ya no se puede: queda libre en el piso
            return false;
        }
        return true;
    }

    /** ¿Sigue existiendo, reservada para él y a una distancia razonable? */
    private boolean isStillMine(ItemEntity gift) {
        return !gift.isRemoved() && !gift.getItem().isEmpty() && NoxisItemClaims.isClaimedBy(gift, this.mob)
                && gift.distanceToSqr(this.mob) < MAX_DISTANCE * MAX_DISTANCE;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.caught = false;
        this.fetching = false;
        this.fetchTicks = 0;
        this.pathFails = 0;
        this.finished = false;
        this.mob.getNavigation().stop();
        this.mob.setNatureAction(NoxisNatureAction.RECEIVE);
        // Si lo habían interrumpido y la flor ya está en el piso, va directo a buscarla.
        ItemEntity gift = this.mob.getIncomingGift();
        if (gift != null && gift.onGround() && gift.distanceToSqr(this.mob) > REACH * REACH) this.startFetch();
    }

    @Override
    public boolean canContinueToUse() {
        if (this.finished) return false;
        if (this.mob.hurtTime > 0) return false;
        if (this.caught) return this.ticks < HOLD_TICKS;
        ItemEntity gift = this.mob.getIncomingGift();
        if (gift == null || !this.isStillMine(gift)) return false;
        return !this.fetching || (this.fetchTicks < FETCH_TIMEOUT && this.pathFails < MAX_PATH_FAILS);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.ticks++;
        if (this.caught) {
            this.mob.getNavigation().stop();
            // La sostiene mirándola con ternura (delante suyo, un poco abajo).
            Vec3 forward = Vec3.directionFromRotation(0.0F, this.mob.yBodyRot);
            this.mob.getLookControl().setLookAt(this.mob.getX() + forward.x, this.mob.getEyeY() - 0.15D,
                    this.mob.getZ() + forward.z, 10.0F, 30.0F);
            if (this.ticks == 40 && this.mob.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.HEART, this.mob.getX(), this.mob.getEyeY() + 0.45D,
                        this.mob.getZ(), 1, 0.1D, 0.05D, 0.1D, 0.0D);
            }
            return;
        }
        ItemEntity gift = this.mob.getIncomingGift();
        if (gift == null || gift.isRemoved() || gift.getItem().isEmpty()) {
            this.finished = true;                        // alguien la levantó: nada que recibir
            return;
        }
        NoxisItemClaims.refresh(gift, this.mob);         // sigue siendo suya mientras la busca
        if (!this.fetching) {
            this.mob.getNavigation().stop();
            LivingEntity giver = this.mob.getGiftGiver();
            // Mira la flor que viene volando (o a quien se la regala, si todavía no salió).
            if (this.ticks < 6 && giver != null) this.mob.getLookControl().setLookAt(giver, 30.0F, 30.0F);
            else this.mob.getLookControl().setLookAt(gift, 30.0F, 30.0F);
            double d2 = gift.distanceToSqr(this.mob);
            boolean arrived = d2 < 1.2D * 1.2D || (gift.onGround() && d2 < 2.8D * 2.8D)
                    || (this.ticks > 30 && d2 < 4.0D * 4.0D);
            if (arrived) {
                this.catchGift(gift);
            } else if ((gift.onGround() && this.ticks > 12) || this.ticks >= MAX_WAIT) {
                this.startFetch();                       // cayó lejos: va a buscarla
            }
            return;
        }
        // Camina hasta la flor y, al llegar, la recoge con la animación de siempre.
        this.fetchTicks++;
        this.mob.getLookControl().setLookAt(gift, 30.0F, 30.0F);
        if (gift.distanceToSqr(this.mob) < REACH * REACH
                || (horizontal2(gift) < 0.8D * 0.8D && Math.abs(gift.getY() - this.mob.getY()) < 1.0D)) {
            this.mob.getNavigation().stop();
            this.mob.setNatureAction(NoxisNatureAction.RECEIVE);
            this.catchGift(gift);
            return;
        }
        if (this.fetchTicks % 10 == 1 || this.mob.getNavigation().isDone()) {
            if (!this.mob.getNavigation().moveTo(gift, FETCH_SPEED)) this.pathFails++;
        }
    }

    private double horizontal2(ItemEntity gift) {
        double dx = gift.getX() - this.mob.getX();
        double dz = gift.getZ() - this.mob.getZ();
        return dx * dx + dz * dz;
    }

    private void startFetch() {
        this.fetching = true;
        this.fetchTicks = 0;
        this.pathFails = 0;
        // Mientras camina, va normal (la carita de regalo vuelve al atraparla).
        this.mob.setNatureAction(NoxisNatureAction.NONE);
    }

    private void catchGift(ItemEntity gift) {
        ItemStack flower = gift.getItem().copy();
        gift.discard();                                  // el objeto del mundo deja de existir...
        this.mob.storeGift(flower);                      // ...y esa misma flor queda guardada
        this.mob.showGiftInHand(flower);                 // en la mano: solo una imagen de ella
        this.mob.clearIncomingGift();
        this.caught = true;
        this.fetching = false;
        this.ticks = 0;
        this.mob.playSound(ModSounds.NOXIS_HAPPY, 0.6F, 1.2F);
        if (this.mob.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HEART, this.mob.getX(), this.mob.getEyeY() + 0.4D,
                    this.mob.getZ(), 3, 0.3D, 0.15D, 0.3D, 0.0D);
        }
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
        ItemEntity gift = this.mob.getIncomingGift();
        boolean interrupted = !this.caught && !this.finished && gift != null && this.isStillMine(gift)
                && (!this.fetching || (this.fetchTicks < FETCH_TIMEOUT && this.pathFails < MAX_PATH_FAILS));
        if (!interrupted) {
            // Recibido, o cancelado de forma segura: la flor (si sigue ahí) queda libre en el piso.
            this.mob.clearIncomingGift();
        }
        this.mob.showGiftInHand(ItemStack.EMPTY);
        this.mob.setNatureAction(NoxisNatureAction.NONE);
        this.caught = false;
        this.fetching = false;
        this.finished = false;
    }
}
