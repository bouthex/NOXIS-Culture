package com.noxisculture.entity.ai;

import com.noxisculture.entity.idle.NoxisPlayAnimation;
import com.noxisculture.entity.social.NoxisSocialLink;
import com.noxisculture.sound.ModSounds;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Seguir a un compañero por curiosidad (servidor), y la travesura secreta (muy rara).
 *
 * <p><b>Seguir</b>: de vez en cuando, un Noxis curioso ve a otro caminando u ocupado en algo
 * interesante y lo sigue un rato a poca distancia (navegación normal), mirando de vez en cuando
 * qué hace. Si el otro se frena, se frena también y lo observa ladeando la cabeza. Después de
 * 15 a 30 s (o si el otro se aleja mucho, se va a descansar o no puede seguirlo) pierde el
 * interés y vuelve a lo suyo. El seguido no se entera ni cambia nada de lo que hace.</p>
 *
 * <p><b>Travesura</b> (muy rara: solo los traviesos, pocas veces, con espera de 20-30 min): si el
 * otro se queda quieto, se acerca agachadito por detrás o por un costado, espera un instante
 * moviendo la colita y ¡salta! El otro pega un saltito de susto (el miedo de siempre, apenas un
 * segundo) y retrocede un poquito; el travieso festeja, el otro se recupera enseguida y le
 * devuelve un manotazo juguetón. Nadie se lastima ni cambia ninguna amistad.</p>
 *
 * <p>Límites: espera propia (1,5 a 4 min), no repite con el mismo compañero enseguida, nadie
 * es seguido por dos a la vez y como mucho dos Noxis siguiendo a otros en la misma zona.</p>
 */
public class NoxisCuriousFollowGoal<T extends PathfinderMob & NoxisCurious> extends Goal {
    private static final double SEE_RADIUS = 8.0D;
    private static final double LOSE_DISTANCE = 14.0D;
    private static final double ZONE = 12.0D;
    private static final int MAX_FOLLOWERS_IN_ZONE = 2;
    private static final double NEAR = 2.2D;        // más cerca que esto, se frena
    private static final double FAR = 3.2D;        // más lejos que esto, camina
    private static final double SPEED = 0.38D;
    private static final double SNEAK_SPEED = 0.2D;
    private static final int MIN_FOLLOW = 300;      // 15 s ...
    private static final int EXTRA_FOLLOW = 300;    // ... a 30 s
    private static final int COOLDOWN = 1800;       // 1,5 min ...
    private static final int COOLDOWN_RANDOM = 3000;// ... a 4 min
    private static final int SAME_TARGET_COOLDOWN = 6000;
    private static final int MAX_PATH_FAILS = 4;
    // Travesura
    private static final int PRANK_CHANCE = 6;      // 1 de cada 6 seguimientos de un travieso (si ya pasó su espera)
    private static final int PRANK_COOLDOWN = 24000;     // 20 min ...
    private static final int PRANK_COOLDOWN_RANDOM = 12000; // ... a 30 min
    private static final int STILL_BEFORE_PRANK = 40;
    private static final int SNEAK_TIMEOUT = 200;
    private static final int CROUCH_TICKS = 18;
    private static final int SCENE_LENGTH = 66;

    private enum Phase { FOLLOW, SNEAK, CROUCH, SCENE }

    private final T mob;
    private int nextCheckTick;
    private long nextAllowedTime = -1L;
    private long nextPrankTime = -1L;
    private int lastTargetId = -1;
    private long lastTargetUntil;
    private @Nullable PathfinderMob target;
    private Phase phase = Phase.FOLLOW;
    private int ticks;
    private int followLength;
    private int pathFails;
    private int targetStill;
    private boolean prankPlanned;
    private @Nullable Vec3 sneakSpot;
    private boolean done;

    public NoxisCuriousFollowGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        this.nextCheckTick = mob.getRandom().nextInt(200);
    }

    // ------------------------------------------------------------------ empezar

    @Override
    public boolean canUse() {
        if (this.mob.tickCount < this.nextCheckTick) return false;
        RandomSource random = this.mob.getRandom();
        this.nextCheckTick = this.mob.tickCount + 40 + random.nextInt(60);       // se fija cada 2-5 s
        long now = this.mob.level().getGameTime();
        if (this.nextAllowedTime < 0L) this.nextAllowedTime = now + 600 + random.nextInt(1800);
        if (this.nextPrankTime < 0L) this.nextPrankTime = now + 3600 + random.nextInt(6000);   // 3 a 8 min
        if (now < this.nextAllowedTime || !this.mob.isCuriousType() || !this.mob.canFollowCuriously()) return false;
        if (random.nextInt(4) != 0) return false;
        // Que no haya ya muchos siguiendo a otros por la zona.
        if (countFollowersNear(this.mob, ZONE) >= MAX_FOLLOWERS_IN_ZONE) return false;
        List<PathfinderMob> found = this.mob.level().getEntitiesOfClass(PathfinderMob.class,
                this.mob.getBoundingBox().inflate(SEE_RADIUS, 3.0D, SEE_RADIUS),
                e -> e != this.mob && e.isAlive() && e instanceof NoxisCurious c
                        && c.isDoingSomethingInteresting() && !c.isFollowedByOther(this.mob.getId(), now)
                        && !(e.getId() == this.lastTargetId && now < this.lastTargetUntil)
                        && this.mob.hasLineOfSight(e));
        if (found.isEmpty()) return false;
        PathfinderMob pick = found.get(random.nextInt(found.size()));
        if (this.mob.getNavigation().createPath(pick, 2) == null) return false;
        this.target = pick;
        return true;
    }

    private static int countFollowersNear(PathfinderMob self, double radius) {
        return self.level().getEntitiesOfClass(PathfinderMob.class, self.getBoundingBox().inflate(radius, 4.0D, radius),
                e -> e != self && e instanceof NoxisCurious c && isFollowerState(c.getPlayAnim())).size();
    }

    private static boolean isFollowerState(byte s) {
        return s == NoxisPlayAnimation.FOLLOW || s == NoxisPlayAnimation.WATCH || s == NoxisPlayAnimation.SNEAK
                || s == NoxisPlayAnimation.CROUCH || s == NoxisPlayAnimation.POUNCE || s == NoxisPlayAnimation.CHEER;
    }

    @Override
    public void start() {
        RandomSource random = this.mob.getRandom();
        long now = this.mob.level().getGameTime();
        this.phase = Phase.FOLLOW;
        this.ticks = 0;
        this.pathFails = 0;
        this.targetStill = 0;
        this.done = false;
        this.followLength = MIN_FOLLOW + random.nextInt(EXTRA_FOLLOW);
        // Travesura: rarísima (solo traviesos, pasada su larga espera, y aun así pocas veces).
        this.prankPlanned = this.mob.isPrankster() && now >= this.nextPrankTime && random.nextInt(PRANK_CHANCE) == 0;
        this.mob.setPlayAnim(NoxisPlayAnimation.FOLLOW);
    }

    // ------------------------------------------------------------------ seguir

    @Override
    public boolean canContinueToUse() {
        PathfinderMob other = this.target;
        if (this.done || other == null || !other.isAlive() || this.mob.hurtTime > 0) return false;
        if (this.phase == Phase.SCENE) return this.ticks < SCENE_LENGTH;      // la escenita termina sola
        if (this.phase == Phase.FOLLOW) {
            if (!this.mob.canFollowCuriously()) return false;
        } else if (!this.mob.isSafeForSocial()
                || !(other instanceof NoxisCurious c) || !c.getSocialLink().isLinkedWith(this.mob)) {
            return false;                                     // el otro se ocupó: se cancela la broma
        }
        if (this.mob.distanceToSqr(other) > LOSE_DISTANCE * LOSE_DISTANCE || this.pathFails >= MAX_PATH_FAILS) return false;
        if (other instanceof NoxisCurious c && this.phase == Phase.FOLLOW && !c.isDoingSomethingInteresting()
                && !c.canBePranked()) {
            return false;                                     // se fue a descansar, a comerciar...: pierde interés
        }
        return this.phase != Phase.FOLLOW || this.ticks < this.followLength;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        PathfinderMob other = this.target;
        if (other == null) return;
        this.ticks++;
        long now = this.mob.level().getGameTime();
        if (other instanceof NoxisCurious c) c.markFollowedBy(this.mob.getId(), now + 20);
        switch (this.phase) {
            case FOLLOW -> this.tickFollow(other);
            case SNEAK -> this.tickSneak(other);
            case CROUCH -> this.tickCrouch(other);
            case SCENE -> this.tickScene(other);
        }
    }

    private void tickFollow(PathfinderMob other) {
        double d = this.mob.distanceTo(other);
        boolean otherMoving = other.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D || !other.getNavigation().isDone();
        this.targetStill = otherMoving ? 0 : this.targetStill + 1;
        // Mira qué hace: casi siempre al otro; a veces, un ratito para adelante.
        if ((this.ticks / 30) % 4 != 3) this.mob.getLookControl().setLookAt(other, 20.0F, 20.0F);
        if (d > FAR) {
            if (this.ticks % 10 == 1 || this.mob.getNavigation().isDone()) {
                if (!this.mob.getNavigation().moveTo(other, SPEED)) this.pathFails++;
            }
            this.mob.setPlayAnim(NoxisPlayAnimation.FOLLOW);
        } else if (d < NEAR || !otherMoving) {
            this.mob.getNavigation().stop();
            // El otro se frenó: lo observa con curiosidad.
            this.mob.setPlayAnim(otherMoving ? NoxisPlayAnimation.FOLLOW : NoxisPlayAnimation.WATCH);
        }
        // Travesura: el otro lleva un rato quieto y tranquilo.
        if (this.prankPlanned && this.targetStill >= STILL_BEFORE_PRANK && other instanceof NoxisCurious c
                && c.canBePranked() && !c.getSocialLink().isBusy() && this.mob.canFollowCuriously() && d < 6.0D) {
            this.startSneak(other, c);
        }
    }

    // ------------------------------------------------------------------ travesura

    private void startSneak(PathfinderMob other, NoxisCurious c) {
        // Por detrás o por un costado (donde no mira).
        float yaw = other.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
        Vec3 right = new Vec3(-Mth.cos(yaw), 0.0D, -Mth.sin(yaw));
        int pick = this.mob.getRandom().nextInt(3);
        Vec3 offset = pick == 0 ? forward.scale(-0.95D)
                : forward.scale(-0.35D).add(right.scale(pick == 1 ? 0.9D : -0.9D));
        Vec3 spot = other.position().add(offset);
        if (this.mob.getNavigation().createPath(spot.x, spot.y, spot.z, 0) == null) {
            this.prankPlanned = false;                        // no hay cómo llegar: solo lo sigue
            return;
        }
        // Los dos quedan reservados: el otro se queda quieto, sin enterarse.
        this.mob.getSocialLink().link(other, NoxisSocialLink.Kind.PLAY, true);
        c.getSocialLink().link(this.mob, NoxisSocialLink.Kind.PLAY, false);
        this.sneakSpot = spot;
        this.phase = Phase.SNEAK;
        this.ticks = 0;
        this.mob.setPlayAnim(NoxisPlayAnimation.SNEAK);
    }

    private void tickSneak(PathfinderMob other) {
        Vec3 spot = this.sneakSpot;
        if (spot == null || this.ticks > SNEAK_TIMEOUT) {
            this.cancelPrank();
            return;
        }
        other.getNavigation().stop();
        this.mob.getLookControl().setLookAt(other, 10.0F, 10.0F);
        double dx = spot.x - this.mob.getX();
        double dz = spot.z - this.mob.getZ();
        if (dx * dx + dz * dz < 0.25D * 0.25D || (this.ticks > 40 && dx * dx + dz * dz < 0.45D * 0.45D)) {
            this.mob.getNavigation().stop();
            this.phase = Phase.CROUCH;
            this.ticks = 0;
            this.mob.setPlayAnim(NoxisPlayAnimation.CROUCH);
        } else if (this.ticks % 10 == 1 || this.mob.getNavigation().isDone()) {
            if (!this.mob.getNavigation().moveTo(spot.x, spot.y, spot.z, SNEAK_SPEED)) this.pathFails++;
        }
    }

    private void tickCrouch(PathfinderMob other) {
        other.getNavigation().stop();
        this.mob.getNavigation().stop();
        faceTowards(this.mob, other);
        if (this.ticks >= CROUCH_TICKS) {
            // ¡Salta! Un saltito de verdad hacia el otro.
            this.phase = Phase.SCENE;
            this.ticks = 0;
            this.mob.setPlayAnim(NoxisPlayAnimation.POUNCE);
            Vec3 dir = other.position().subtract(this.mob.position());
            Vec3 flat = new Vec3(dir.x, 0.0D, dir.z);
            if (flat.lengthSqr() > 1.0E-4D) flat = flat.normalize();
            this.mob.setDeltaMovement(flat.x * 0.14D, 0.32D, flat.z * 0.14D);
            this.mob.playSound(ModSounds.NOXIS_HAPPY, 0.7F, 1.55F);
        }
    }

    /** La escenita después del salto: susto breve, festejo, manotazo juguetón y cada uno a lo suyo. */
    private void tickScene(PathfinderMob other) {
        this.mob.getNavigation().stop();
        if (this.ticks > 6) this.mob.getLookControl().setLookAt(other, 30.0F, 30.0F);
        NoxisCurious c = other instanceof NoxisCurious nc ? nc : null;
        if (this.ticks == 4 && c != null) {
            // El otro pega un saltito de susto y retrocede un poquito (el miedo de siempre, breve).
            c.setPlayAnim(NoxisPlayAnimation.STARTLED);
            c.startle(22);
            Vec3 away = other.position().subtract(this.mob.position());
            Vec3 flat = new Vec3(away.x, 0.0D, away.z);
            if (flat.lengthSqr() > 1.0E-4D) flat = flat.normalize();
            other.getNavigation().stop();
            other.setDeltaMovement(flat.x * 0.22D, 0.28D, flat.z * 0.22D);
            faceTowards(other, this.mob);
            other.playSound(ModSounds.NOXIS_SCARED, 0.55F, 1.35F);
        }
        if (this.ticks == NoxisPlayAnimation.POUNCE_LENGTH) {
            this.mob.setPlayAnim(NoxisPlayAnimation.CHEER);       // festeja la travesura
            this.mob.cheerUp(36);
            this.mob.playSound(ModSounds.NOXIS_CELEBRATE, 0.45F, 1.3F);
        }
        if (this.ticks > 8 && this.ticks < 30) other.getLookControl().setLookAt(this.mob, 30.0F, 30.0F);
        if (this.ticks == 30 && c != null) {
            // Se recupera enseguida y le devuelve un gesto juguetón.
            c.setPlayAnim(NoxisPlayAnimation.PLAYFUL);
            faceTowards(other, this.mob);
            other.playSound(ModSounds.NOXIS_HAPPY, 0.5F, 1.25F);
        }
        if (this.ticks > 30) other.getLookControl().setLookAt(this.mob, 30.0F, 30.0F);
        if (this.ticks == 30 + NoxisPlayAnimation.PLAYFUL_LENGTH && c != null) c.setPlayAnim(NoxisPlayAnimation.NONE);
        if (this.ticks == NoxisPlayAnimation.POUNCE_LENGTH + NoxisPlayAnimation.CHEER_LENGTH) {
            this.mob.setPlayAnim(NoxisPlayAnimation.NONE);
        }
    }

    private void cancelPrank() {
        this.prankPlanned = false;
        this.releaseTarget();
        this.phase = Phase.FOLLOW;
        this.ticks = Math.max(this.ticks, this.followLength - 60);   // ya no tiene gracia: sigue un ratito y se va
        this.mob.setPlayAnim(NoxisPlayAnimation.FOLLOW);
    }

    private void releaseTarget() {
        PathfinderMob other = this.target;
        if (other instanceof NoxisCurious c) {
            if (c.getSocialLink().isLinkedWith(this.mob)) c.getSocialLink().clear();
            byte s = c.getPlayAnim();
            if (s == NoxisPlayAnimation.STARTLED || s == NoxisPlayAnimation.PLAYFUL) c.setPlayAnim(NoxisPlayAnimation.NONE);
        }
        if (this.mob.getSocialLink().isBusy()) this.mob.getSocialLink().clear();
    }

    private static void faceTowards(PathfinderMob mob, PathfinderMob other) {
        double dx = other.getX() - mob.getX();
        double dz = other.getZ() - mob.getZ();
        if (dx * dx + dz * dz < 1.0E-4D) return;
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        mob.setYRot(yaw);
        mob.yBodyRot = yaw;
        mob.yHeadRot = yaw;
    }

    @Override
    public void stop() {
        long now = this.mob.level().getGameTime();
        RandomSource random = this.mob.getRandom();
        boolean pranked = this.phase == Phase.SCENE;
        this.releaseTarget();
        this.mob.getNavigation().stop();
        this.mob.setPlayAnim(NoxisPlayAnimation.NONE);
        if (this.target != null) {
            this.lastTargetId = this.target.getId();
            this.lastTargetUntil = now + SAME_TARGET_COOLDOWN;
        }
        this.nextAllowedTime = now + COOLDOWN + random.nextInt(COOLDOWN_RANDOM);
        if (pranked) this.nextPrankTime = now + PRANK_COOLDOWN + random.nextInt(PRANK_COOLDOWN_RANDOM);
        this.target = null;
        this.sneakSpot = null;
        this.prankPlanned = false;
        this.phase = Phase.FOLLOW;
    }
}
