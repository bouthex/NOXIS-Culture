package com.noxisculture.entity.ai;

import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/** Entidades Noxis que duermen de noche en su pecera (todas las especies, actuales y futuras). */
public interface NoxisBowlSleeper {
    /** ¿Está dentro de una pecera durmiendo? */
    boolean isInBowl();

    void setInBowl(boolean inBowl);

    /** La pecera que eligió (reservada) o en la que duerme; null si ninguna. */
    @Nullable BlockPos getBowlPos();

    void setBowlPos(@Nullable BlockPos pos);

    /** ¿Tiene puesto su sombrero? */
    boolean hasHat();

    void setHasHat(boolean hat);

    /** El sombrero exacto que lleva puesto (con sus colores), o vacío. */
    net.minecraft.world.item.ItemStack getHatItem();

    /** Se pone ese sombrero exacto (vacío = sin sombrero). */
    void setHatItem(net.minecraft.world.item.ItemStack hat);

    /** Dónde dejó su sombrero al acostarse (o null). */
    @Nullable BlockPos getHatPos();

    void setHatPos(@Nullable BlockPos pos);

    /** Animación de sacarse/ponerse el sombrero ({@code NoxisHatAnimation.NONE/OFF/ON}). */
    void setHatAnim(byte anim);

    /**
     * Saltito para entrar o salir de la pecera ({@code NoxisBowlHop.NONE/PREP/IN/OUT}): mientras
     * salta (IN/OUT) la entidad no se mueve sola (ni gravedad ni choques); la lleva el objetivo
     * de dormir, tick a tick. El cliente lo usa para la pose. No se guarda.
     */
    void setBowlHop(byte anim);

    /**
     * Empieza la búsqueda temporal de sombrero (al despertarse sin el suyo): busca y recorre la
     * zona sin alejarse de {@code origin}, por un tiempo limitado.
     */
    void startHatSearch(BlockPos origin);

    /** Centro de la búsqueda de sombrero en curso, o null si no está buscando. */
    @Nullable BlockPos getHatSearchOrigin();

    void stopHatSearch();

    /** ¿Ya le dio sueño esta noche? (cada Noxis tiene su propio horario). */
    boolean isBedtime();

    /** ¿Ya es hora de levantarse? (cada uno a su ritmo después del amanecer). */
    boolean isWakeTime();

    /** ¿Puede ir a dormir ahora? (sin comercio, sin miedo, sin otra actividad importante). */
    boolean canGoToBowl();
}
