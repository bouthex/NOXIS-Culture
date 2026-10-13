package com.noxisculture.entity.ai;

import com.noxisculture.entity.hearing.NoxisHeardSound;
import com.noxisculture.entity.personality.NoxisPersonable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Noxis que reaccionan a los sonidos del mundo (todas las especies, actuales y futuras, con capa
 * o sin ella). Los oídos ({@code NoxisHearingListener}) anotan el sonido; el objetivo
 * {@link NoxisListenGoal} decide si se para a escuchar y si va a investigar.
 */
public interface NoxisListener extends NoxisPersonable {
    /** Oyó algo interesante (lo llaman sus oídos o una criatura cercana). */
    void hearSound(Vec3 pos, byte kind, @Nullable Entity source);

    /** El último sonido que oyó y todavía no atendió (null si no hay). */
    @Nullable NoxisHeardSound getHeardSound();

    void clearHeardSound();

    /** ¿Está tranquilo y libre para pararse a escuchar? (sin comerciar, dormir, descansar, usar objetos...). */
    boolean canListen();

    /** Reacción en curso ({@code NoxisListenAnimation}, sincronizada; NONE = ninguna). */
    byte getListenAnim();

    void setListenAnim(byte anim);

    /** Hacia dónde está el sonido respecto de su cabeza (-100..100; positivo = hacia su derecha). */
    void setListenDir(byte dir);
}
