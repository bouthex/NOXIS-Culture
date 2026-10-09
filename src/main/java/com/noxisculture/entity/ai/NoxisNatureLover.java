package com.noxisculture.entity.ai;

import com.noxisculture.entity.nature.NoxisFlowerCarry;

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
}
