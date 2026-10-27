package com.noxisculture.entity.ai;

import com.noxisculture.entity.nature.NoxisFlowerCarry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import org.jspecify.annotations.Nullable;

/** Entidades Noxis que disfrutan de la naturaleza (todas las especies, actuales y futuras). */
public interface NoxisNatureLover {
    /** ¿Está tranquilo y libre? (sin miedo, sin comerciar, sin descansar, con las manos libres, sin otra reacción). */
    boolean canEnjoyNature();

    /** Avisa a los clientes qué está haciendo (ver {@code NoxisNatureAction}) para animarlo. */
    void setNatureAction(byte action);

    /** La flor en la mano y su lugar original. */
    NoxisFlowerCarry getFlowerCarry();

    /** Llamar después de recoger o devolver la flor, para que los clientes la vean (o dejen de verla). */
    void syncHeldFlower();

    // ---- Recibir una flor de regalo de otro Noxis ----

    /** ¿Está libre para recibir un regalo ahora? (tranquilo, sin flor propia, sin otra actividad). */
    boolean canReceiveGift();

    /** Otro Noxis le lanzó esta flor (objeto real en el mundo): que la espere y la atrape. */
    void expectGift(ItemEntity gift, LivingEntity giver);

    /** El regalo que viene volando (o null). */
    @Nullable ItemEntity getIncomingGift();

    @Nullable LivingEntity getGiftGiver();

    /** Termina la espera del regalo (lo haya atrapado o no). */
    void clearIncomingGift();

    /** Muestra (o deja de mostrar) en la mano la flor recibida; solo visual, la real ya está guardada. */
    void showGiftInHand(net.minecraft.world.item.ItemStack stack);

    /** Guarda la flor recibida en su inventario (persistente). Lo que no entre queda en el piso. */
    void storeGift(net.minecraft.world.item.ItemStack stack);

    // ---- Recoger flores sueltas del piso ----

    /** ¿Puede ir a buscar una flor suelta ahora? (tranquilo, manos libres, sin regalo pendiente). */
    default boolean canCollectFlowers() {
        return this.canEnjoyNature() && !this.getFlowerCarry().isHolding() && this.getIncomingGift() == null;
    }

    /** ¿Le entra (al menos una) en el inventario? */
    boolean hasRoomFor(net.minecraft.world.item.ItemStack stack);

    /** Guarda en su inventario lo que entre y devuelve lo que no entró (no tira nada al piso). */
    net.minecraft.world.item.ItemStack storeCollected(net.minecraft.world.item.ItemStack stack);
}
