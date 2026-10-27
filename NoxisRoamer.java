package com.noxisculture.entity.ai;

import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * Noxis que pasean por su zona y se hacen a un lado cuando hay demasiados juntos (todas las
 * especies, con capa o sin ella).
 */
public interface NoxisRoamer extends NoxisListener {
    /**
     * Centro de su zona: su pecera o, si no tiene, el lugar donde apareció. Es provisorio: cuando
     * existan las aldeas, la zona va a ser la aldea.
     */
    @Nullable BlockPos getHomeAnchor();

    /** ¿Libre para pasear o correrse un poquito? (tranquilo y sin ninguna actividad). */
    boolean canRoam();
}
