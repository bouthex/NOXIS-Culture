package com.noxisculture.entity.hearing;

import net.minecraft.world.phys.Vec3;

/**
 * El último sonido interesante que oyó un Noxis (servidor, no se guarda): dónde, qué clase de
 * sonido, cuándo, y quién lo hizo (id de la criatura, o -1 si fue un bloque).
 */
public record NoxisHeardSound(Vec3 pos, byte kind, long time, int sourceId) {
}
