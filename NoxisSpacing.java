package com.noxisculture.entity.ai;

import java.util.List;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Espacio personal entre Noxis (servidor). Cálculos livianos sobre una lista de vecinos que se pide
 * una sola vez: nada se recalcula cada tick.
 */
public final class NoxisSpacing {
    private NoxisSpacing() {}

    /** Los otros Noxis vivos alrededor de un punto (radio horizontal; 1,5 bloques para arriba y abajo). */
    public static List<PathfinderMob> around(PathfinderMob mob, Vec3 center, double radius) {
        return mob.level().getEntitiesOfClass(PathfinderMob.class,
                new AABB(center, center).inflate(radius, 1.5D, radius),
                e -> e != mob && e instanceof NoxisListener && e.isAlive() && !e.isPassenger());
    }

    /** Distancia horizontal al más cercano de la lista (como mucho {@code cap}). */
    public static double room(List<PathfinderMob> others, Vec3 p, double cap) {
        double best = cap * cap;
        for (PathfinderMob e : others) {
            if (Math.abs(e.getY() - p.y) > 2.0D) continue;
            double dx = e.getX() - p.x;
            double dz = e.getZ() - p.z;
            double d = dx * dx + dz * dz;
            if (d < best) best = d;
        }
        return Math.sqrt(best);
    }

    public static double horizontal2(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }
}
