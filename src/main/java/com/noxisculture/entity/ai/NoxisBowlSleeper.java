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

    /** Dónde dejó su sombrero al acostarse (o null). */
    @Nullable BlockPos getHatPos();

    void setHatPos(@Nullable BlockPos pos);

    /** Animación de sacarse/ponerse el sombrero ({@code NoxisHatAnimation.NONE/OFF/ON}). */
    void setHatAnim(byte anim);

    /**
     * Saltito para entrar o salir de la pecera: mientras dura, la entidad no se mueve sola
     * (ni gravedad ni choques); la lleva el objetivo de dormir, tick a tick. No se guarda.
     */
    void setBowlHop(boolean hop);

    /** ¿Ya le dio sueño esta noche? (cada Noxis tiene su propio horario). */
    boolean isBedtime();

    /** ¿Ya es hora de levantarse? (cada uno a su ritmo después del amanecer). */
    boolean isWakeTime();

    /** ¿Puede ir a dormir ahora? (sin comercio, sin miedo, sin otra actividad importante). */
    boolean canGoToBowl();
}
