package com.noxisculture.entity.ai;

import net.minecraft.core.BlockPos;

/** Noxis que trabajan en una mesa para reponer sus tradeos (comerciantes). */
public interface NoxisWorker {
    /** ¿Tiene tradeos usados y ya puede volver a reponer? */
    boolean needsToWork();

    /** Llegó a la mesa: repone los tradeos. */
    void workAt(BlockPos table);
}
