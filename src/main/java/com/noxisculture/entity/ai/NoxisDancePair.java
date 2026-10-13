package com.noxisculture.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import com.noxisculture.entity.idle.NoxisDanceAnimation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Una pareja de baile (servidor, no se guarda): exactamente dos Noxis. La crea el que propone
 * ({@link #leader}) y la reserva queda puesta en los DOS en el mismo tick, así ningún otro puede
 * elegir a ninguno de los dos. El que propuso toma las decisiones (cuándo empiezan, la coreografía,
 * la caída en pareja, cuándo termina) y se las pone a los dos a la vez: nunca se desincronizan.
 */
public final class NoxisDancePair {
    /** Mitad de la distancia entre los dos (bloques): bien cerquita para tomarse de las manitos. */
    public static final double HALF_DISTANCE = 0.425D;
    /** En los saltitos (giran con los bracitos abiertos) quedan un poco más separados: no se atraviesan. */
    public static final double HOPS_HALF_DISTANCE = 0.6D;
    /** Lugar libre alrededor de la pareja: si hay otro Noxis más cerca del centro, la pareja se corre un poquito. */
    private static final double CLEARANCE = 1.7D;
    private static final double MAX_SHIFT = 1.0D;

    public final PathfinderMob leader;
    public final PathfinderMob partner;
    public final BlockPos jukebox;
    /** Coreografía de pareja (NoxisDanceAnimation.PAIR_RING / PAIR_HOPS). */
    public final int routine;
    /** En qué tick del baile en pareja se caen los dos (-1 = no se caen). */
    public final int fallAt;
    private final Vec3 leaderSpot;
    private final Vec3 partnerSpot;
    public boolean started;
    public boolean falling;
    public boolean finished;
    public int ticks;
    private boolean dissolved;

    public NoxisDancePair(PathfinderMob leader, PathfinderMob partner, BlockPos jukebox, int routine, int fallAt) {
        this.leader = leader;
        this.partner = partner;
        this.jukebox = jukebox.immutable();
        this.routine = routine;
        this.fallAt = fallAt;
        // Frente a frente, en el medio de donde estaban.
        Vec3 a = leader.position();
        Vec3 b = partner.position();
        Vec3 mid = a.add(b).scale(0.5D);
        Vec3 axis = new Vec3(b.x - a.x, 0.0D, b.z - a.z);
        axis = axis.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : axis.normalize();
        mid = makeRoom(leader, partner, mid);
        double half = routine == NoxisDanceAnimation.PAIR_HOPS ? HOPS_HALF_DISTANCE : HALF_DISTANCE;
        this.leaderSpot = mid.subtract(axis.scale(half));
        this.partnerSpot = mid.add(axis.scale(half));
    }

    /**
     * Si otro Noxis (por ejemplo, uno que baila solo al lado) queda pegado al centro de la pareja,
     * el centro se corre un poquito para el otro lado (una sola vez, al formarse la pareja).
     */
    private static Vec3 makeRoom(PathfinderMob leader, PathfinderMob partner, Vec3 mid) {
        double pushX = 0.0D, pushZ = 0.0D;
        for (PathfinderMob e : leader.level().getEntitiesOfClass(PathfinderMob.class,
                new AABB(mid, mid).inflate(CLEARANCE, 1.5D, CLEARANCE),
                e -> e != leader && e != partner && e instanceof NoxisListener && e.isAlive())) {
            double dx = mid.x - e.getX();
            double dz = mid.z - e.getZ();
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d >= CLEARANCE) continue;
            if (d < 1.0E-3D) {
                dx = 1.0D;
                dz = 0.0D;
                d = 1.0D;
            }
            double k = (CLEARANCE - d) / d;
            pushX += dx * k;
            pushZ += dz * k;
        }
        double len = Math.sqrt(pushX * pushX + pushZ * pushZ);
        if (len < 1.0E-3D) return mid;
        double s = Math.min(MAX_SHIFT, len) / len;
        Vec3 moved = new Vec3(mid.x + pushX * s, mid.y, mid.z + pushZ * s);
        // Solo si ahí se puede estar parado (si no, se quedan donde estaban).
        return leader.getNavigation().createPath(BlockPos.containing(moved), 0) != null ? moved : mid;
    }

    public boolean isLeader(PathfinderMob mob) {
        return mob == this.leader;
    }

    public PathfinderMob other(PathfinderMob mob) {
        return mob == this.leader ? this.partner : this.leader;
    }

    public Vec3 spotFor(PathfinderMob mob) {
        return mob == this.leader ? this.leaderSpot : this.partnerSpot;
    }

    /** Papel en la coreografía: 0 = el que propuso, 1 = el compañero. */
    public int role(PathfinderMob mob) {
        return mob == this.leader ? 0 : 1;
    }

    public boolean isDissolved() {
        return this.dissolved;
    }

    /** Libera la pareja en los dos (los dos quedan libres para su vida normal o para bailar solos). */
    public void dissolve() {
        if (this.dissolved) return;
        this.dissolved = true;
        if (this.leader instanceof NoxisDancer d && d.getDancePair() == this) d.setDancePair(null);
        if (this.partner instanceof NoxisDancer d && d.getDancePair() == this) d.setDancePair(null);
    }
}
