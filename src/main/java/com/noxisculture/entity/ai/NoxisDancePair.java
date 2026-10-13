package com.noxisculture.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
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
        this.leaderSpot = mid.subtract(axis.scale(HALF_DISTANCE));
        this.partnerSpot = mid.add(axis.scale(HALF_DISTANCE));
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
