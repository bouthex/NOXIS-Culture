package com.noxisculture.entity.nature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Reservas de flores (servidor): un Noxis "aparta" la flor que eligió para que ningún otro
 * intente recogerla u olfatearla al mismo tiempo. Cada reserva vence sola por si el Noxis
 * desaparece a mitad de camino (descarga del chunk, muerte...).
 */
public final class NoxisFlowerReservations {
    private NoxisFlowerReservations() {}

    /** Una interacción completa dura menos de esto; pasado este tiempo la reserva se libera sola. */
    private static final long MAX_TICKS = 20L * 90L;

    private record Reservation(int entityId, long expires) {}

    private static final Map<GlobalPos, Reservation> RESERVED = new HashMap<>();

    private static GlobalPos key(Entity entity, BlockPos pos) {
        return GlobalPos.of(entity.level().dimension(), pos.immutable());
    }

    /** ¿La reservó OTRO Noxis (y sigue vigente)? */
    public static boolean isTakenByOther(Entity entity, BlockPos pos) {
        Reservation r = RESERVED.get(key(entity, pos));
        return r != null && r.entityId != entity.getId() && r.expires > entity.level().getGameTime();
    }

    /** Intenta apartar la flor. Devuelve false si ya la tiene otro. */
    public static boolean reserve(Entity entity, BlockPos pos) {
        cleanup(entity.level().getGameTime());
        if (isTakenByOther(entity, pos)) return false;
        RESERVED.put(key(entity, pos), new Reservation(entity.getId(), entity.level().getGameTime() + MAX_TICKS));
        return true;
    }

    /** Libera la flor (solo si era de este Noxis). */
    public static void release(Entity entity, BlockPos pos) {
        GlobalPos k = key(entity, pos);
        Reservation r = RESERVED.get(k);
        if (r != null && r.entityId == entity.getId()) RESERVED.remove(k);
    }

    /** Cuántas flores tienen apartadas otros Noxis cerca (para no hacer todos lo mismo a la vez). */
    public static int busyNear(Entity entity, double radius) {
        long now = entity.level().getGameTime();
        double r2 = radius * radius;
        int count = 0;
        for (Map.Entry<GlobalPos, Reservation> e : RESERVED.entrySet()) {
            Reservation r = e.getValue();
            if (r.entityId == entity.getId() || r.expires <= now) continue;
            if (!e.getKey().dimension().equals(entity.level().dimension())) continue;
            if (e.getKey().pos().distToCenterSqr(entity.position()) < r2) count++;
        }
        return count;
    }

    private static void cleanup(long now) {
        Iterator<Map.Entry<GlobalPos, Reservation>> it = RESERVED.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().expires <= now) it.remove();
        }
    }
}
