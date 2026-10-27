package com.noxisculture.entity.ai;

import com.noxisculture.entity.idle.NoxisAffectionAnimation;
import com.noxisculture.entity.social.NoxisFriendship;
import com.noxisculture.entity.social.NoxisSocialLink;
import com.noxisculture.sound.ModSounds;
import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Gestos de cariño entre amigos (servidor). De vez en cuando, si su amigo especial está cerca y
 * los dos están tranquilos, este Noxis va hasta su lado (los dos mirando para el mismo lado,
 * nunca cara a cara) y hacen juntos uno de tres gestos ({@link NoxisAffectionAnimation}):
 * agarrarse de las manitos, apoyar las cabecitas de costado o un abrazo de amiguitos. Después
 * cada uno sigue con lo suyo.
 *
 * <p>Coordinado: este guía y el amigo lo acompaña ({@link NoxisSocialFollowGoal}). Los dos
 * reciben el gesto en el mismo tick (una sola coreografía compartida). Espera propia de los dos
 * (2 a 4 min), una sola pareja con cariño a la vez por zona, y cualquier cosa importante
 * (comercio, susto, descanso, sueño, objetos) lo cancela: el gesto se desvanece suave.</p>
 *
 * <p><b>Caminar de la mano</b>: después de darse las manitos, a veces no se sueltan y salen a caminar
 * juntos un rato (de unos segundos a más de un minuto, según cómo son los dos). Es un paseo de verdad:
 * el que guía camina con la navegación normal hacia lugares de su zona y el amigo va a su lado (a la
 * misma distancia del gesto), adaptándose al terreno. Si se separan demasiado, si uno no puede seguir
 * (obstáculo, agua, sin camino) o si aparece algo más importante, se sueltan con naturalidad y cada uno
 * sigue con lo suyo. Ninguna personalidad queda afuera: solo cambian las ganas y cuánto dura.</p>
 */
public class NoxisAffectionGoal<T extends PathfinderMob & NoxisFriend> extends Goal {
    private static final double MAX_DISTANCE = 10.0D;
    private static final double ZONE = 8.0D;
    private static final int APPROACH_TIMEOUT = 200;
    private static final int COOLDOWN = 2400;          // 2 min ...
    private static final int COOLDOWN_RANDOM = 2400;   // ... a 4 min
    private static final double SPEED = 0.4D;

    private enum Phase { APPROACH, AFFECTION, WALK }

    // ---- Caminar de la mano
    private static final double WALK_SPEED = 0.32D;
    private static final double WALK_LEG = 9.0D;
    private static final int WALK_MAX_APART_TICKS = 30;
    private static final int WALK_MAX_PATH_FAILS = 3;
    private int walkLength;
    private int apartTicks;
    private int pathFails;
    private int pauseTicks;
    private @Nullable Vec3 walkTarget;

    private final T mob;
    private int nextCheckTick;
    private @Nullable PathfinderMob friend;
    private Phase phase = Phase.APPROACH;
    private int ticks;
    private @Nullable Vec3 spot;
    /** Gesto elegido (manitos, cabecitas, abrazo) y el último, para no repetir siempre el mismo. */
    private byte gesture = NoxisAffectionAnimation.HANDS;
    private byte lastGesture = NoxisAffectionAnimation.NONE;
    /** Para dónde miran los dos (el que recibe se queda mirando como estaba). */
    private float baseYaw;
    /** ¿Se pone a la DERECHA del amigo? (si no, a su izquierda). */
    private boolean onFriendsRight;

    public NoxisAffectionGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        this.nextCheckTick = mob.getRandom().nextInt(200);
    }

    @Override
    public boolean canUse() {
        if (this.mob.tickCount < this.nextCheckTick) return false;
        RandomSource random = this.mob.getRandom();
        this.nextCheckTick = this.mob.tickCount + 60 + random.nextInt(60);    // se fija cada 3-6 s
        long now = this.mob.level().getGameTime();
        if (this.mob.getNextAffectionTime() == 0L) {
            this.mob.setNextAffectionTime(now + 600 + random.nextInt(1200)); // el primero: 30-90 s
        }
        if (now < this.mob.getNextAffectionTime() || this.mob.getSocialLink().isBusy()) return false;
        if (!this.mob.canShowAffection() || random.nextInt(3) != 0) return false;
        PathfinderMob other = NoxisFriendship.findFriend(this.mob);
        if (other == null || !(other instanceof NoxisFriend f)) return false;
        if (now < f.getNextAffectionTime() || f.getSocialLink().isBusy() || !f.canShowAffection()) return false;
        if (this.mob.distanceToSqr(other) > MAX_DISTANCE * MAX_DISTANCE) return false;
        if (NoxisSocialLink.zoneBusy(this.mob, ZONE, NoxisSocialLink.Kind.AFFECTION)) return false;
        if (this.mob.getNavigation().createPath(other, 0) == null) return false;
        this.friend = other;
        return true;
    }

    @Override
    public void start() {
        PathfinderMob other = this.friend;
        if (other == null) return;
        // Los dos quedan reservados entre sí: el amigo se queda quieto mirándolo venir.
        this.mob.getSocialLink().link(other, NoxisSocialLink.Kind.AFFECTION, true);
        ((NoxisFriend) other).getSocialLink().link(this.mob, NoxisSocialLink.Kind.AFFECTION, false);
        other.getNavigation().stop();
        this.phase = Phase.APPROACH;
        this.ticks = 0;
        // Uno de los tres gestos, al azar (distinto del último que hicieron).
        byte g;
        do {
            g = (byte) (1 + this.mob.getRandom().nextInt(3));
        } while (g == this.lastGesture);
        this.gesture = g;
        // Se pone al lado del amigo (del lado en que viene), mirando para el mismo lado.
        this.baseYaw = other.getYRot();
        Vec3 right = rightOf(this.baseYaw);
        Vec3 toMe = this.mob.position().subtract(other.position());
        this.onFriendsRight = toMe.x * right.x + toMe.z * right.z >= 0.0D;
        this.planSpot(other);
    }

    /** Al lado del amigo, a la distancia de este gesto. */
    private void planSpot(PathfinderMob other) {
        double d = NoxisAffectionAnimation.distance(this.gesture);
        this.spot = other.position().add(rightOf(this.baseYaw).scale(this.onFriendsRight ? d : -d));
    }

    private static Vec3 rightOf(float yaw) {
        float r = yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.cos(r), 0.0D, -Mth.sin(r));
    }

    @Override
    public boolean canContinueToUse() {
        PathfinderMob other = this.friend;
        if (other == null || !other.isAlive() || !(other instanceof NoxisFriend f)) return false;
        if (!f.getSocialLink().isLinkedWith(this.mob) || !this.mob.getSocialLink().isLinkedWith(other)) return false;
        if (!this.mob.isSafeForSocial() || !f.isSafeForSocial() || this.mob.hurtTime > 0 || other.hurtTime > 0) return false;
        if (this.phase == Phase.APPROACH) {
            return this.ticks < APPROACH_TIMEOUT && this.mob.distanceToSqr(other) < (MAX_DISTANCE + 4) * (MAX_DISTANCE + 4);
        }
        if (this.phase == Phase.WALK) {
            return this.ticks < this.walkLength && this.apartTicks < WALK_MAX_APART_TICKS && this.pathFails < WALK_MAX_PATH_FAILS
                    && !this.mob.isInWater() && !other.isInWater() && this.insideHome(this.mob.position());
        }
        return this.ticks < NoxisAffectionAnimation.length(NoxisAffectionAnimation.encode(
                NoxisAffectionAnimation.LEAD, this.gesture, false)) && this.mob.distanceToSqr(other) < 2.5D * 2.5D;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        PathfinderMob other = this.friend;
        if (other == null) return;
        this.ticks++;
        if (this.phase != Phase.WALK) other.getNavigation().stop();
        if (this.phase == Phase.APPROACH) {
            other.getLookControl().setLookAt(this.mob, 30.0F, 30.0F);
            this.mob.getLookControl().setLookAt(other, 30.0F, 30.0F);
            Vec3 target = this.spot;
            if (target == null) return;
            if (this.ticks % 20 == 0) this.planSpot(other);
            double dx = target.x - this.mob.getX();
            double dz = target.z - this.mob.getZ();
            double d2 = dx * dx + dz * dz;
            if (d2 < 0.2D * 0.2D || (this.ticks > 40 && d2 < 0.4D * 0.4D)) {
                this.beginAffection(other);
            } else if (this.ticks % 10 == 1 || this.mob.getNavigation().isDone()) {
                if (!this.mob.getNavigation().moveTo(target.x, target.y, target.z, SPEED)) {
                    this.mob.getMoveControl().setWantedPosition(target.x, target.y, target.z, SPEED);
                }
            }
            return;
        }
        if (this.phase == Phase.WALK) {
            this.tickWalk(other);
            return;
        }
        // Manitos: justo antes de soltarse, a veces deciden seguir caminando de la mano.
        if (this.gesture == NoxisAffectionAnimation.HANDS && this.ticks == HANDS_DECIDE_TICK && this.wantsToWalk(other)) {
            this.beginWalk(other);
            return;
        }
        // Gesto de cariño: quietos, uno al lado del otro, mirando para el mismo lado.
        this.mob.getNavigation().stop();
        face(this.mob, this.baseYaw);
        face(other, this.baseYaw);
        int heart = NoxisAffectionAnimation.heartTick(this.gesture);
        if (this.ticks == heart && this.mob.level() instanceof ServerLevel server) {
            Vec3 mid = this.mob.position().add(other.position()).scale(0.5D);
            server.sendParticles(ParticleTypes.HEART, mid.x, this.mob.getEyeY() + 0.75D, mid.z, 1, 0.05D, 0.05D, 0.05D, 0.0D);
        }
        if (this.ticks == heart + 6) other.playSound(ModSounds.NOXIS_HAPPY, 0.35F, 1.4F);
    }

    // ------------------------------------------------------------------ caminar de la mano

    /** En la coreografía de las manitos, el momento en que todavía están tomados (antes de soltarse). */
    private static final int HANDS_DECIDE_TICK = 52;

    private static float walkChance(PathfinderMob mob) {
        if (!(mob instanceof com.noxisculture.entity.personality.NoxisPersonable p)) return 0.35F;
        return switch (p.getPersonality()) {
            case AFFECTIONATE -> 0.55F;
            case CALM, ARTIST -> 0.4F;
            case CURIOUS -> 0.35F;
            case MISCHIEVOUS -> 0.3F;
        };
    }

    /** Cuánto duran las ganas de caminar juntos (multiplica): cariñosos y tranquilos, más. */
    private static float walkPatience(PathfinderMob mob) {
        if (!(mob instanceof com.noxisculture.entity.personality.NoxisPersonable p)) return 1.0F;
        return switch (p.getPersonality()) {
            case AFFECTIONATE -> 1.5F;
            case CALM -> 1.3F;
            case ARTIST -> 1.1F;
            case CURIOUS -> 0.9F;
            case MISCHIEVOUS -> 0.7F;
        };
    }

    private boolean wantsToWalk(PathfinderMob other) {
        if (this.mob.isInWater() || other.isInWater()) return false;
        float chance = (walkChance(this.mob) + walkChance(other)) * 0.5F;
        return this.mob.getRandom().nextFloat() < chance;
    }

    private void beginWalk(PathfinderMob other) {
        RandomSource random = this.mob.getRandom();
        float patience = (walkPatience(this.mob) + walkPatience(other)) * 0.5F;
        // Duración natural y variada: a veces un paseíto corto, a veces un buen rato.
        int base = random.nextInt(4) == 0 ? 100 + random.nextInt(140) : 300 + random.nextInt(900);
        this.walkLength = (int) (base * patience);
        this.apartTicks = 0;
        this.pathFails = 0;
        this.pauseTicks = 0;
        this.walkTarget = null;
        this.phase = Phase.WALK;
        this.ticks = 0;
        this.mob.setAffectionAnim(NoxisAffectionAnimation.withWalking(NoxisAffectionAnimation.encode(
                NoxisAffectionAnimation.LEAD, NoxisAffectionAnimation.HANDS, !this.onFriendsRight)));
        ((NoxisFriend) other).setAffectionAnim(NoxisAffectionAnimation.withWalking(NoxisAffectionAnimation.encode(
                NoxisAffectionAnimation.PARTNER, NoxisAffectionAnimation.HANDS, this.onFriendsRight)));
    }

    private void tickWalk(PathfinderMob other) {
        // El amigo va a su lado: del mismo lado que en el gesto, a la distancia de las manitos.
        double d = NoxisAffectionAnimation.distance(NoxisAffectionAnimation.HANDS);
        Vec3 side = rightOf(this.mob.yBodyRot).scale(this.onFriendsRight ? -d : d);
        Vec3 slot = this.mob.position().add(side);
        double err = Math.sqrt(NoxisSpacing.horizontal2(other.position(), slot));
        double apart = Math.sqrt(NoxisSpacing.horizontal2(other.position(), this.mob.position()));
        this.apartTicks = apart > 2.4D ? this.apartTicks + 1 : 0;

        // Pausitas para mirar el paisaje juntos (quietos, mirando para el mismo lado).
        if (this.pauseTicks > 0) {
            this.pauseTicks--;
            this.mob.getNavigation().stop();
            if (err > 0.25D) other.getNavigation().moveTo(slot.x, slot.y, slot.z, 0.3D);
            else other.getNavigation().stop();
            if (this.pauseTicks == 0) this.walkTarget = null;
            return;
        }
        Vec3 target = this.walkTarget;
        if (target == null || NoxisSpacing.horizontal2(this.mob.position(), target) < 1.5D * 1.5D) {
            if (target != null && this.mob.getRandom().nextInt(3) == 0) {
                this.pauseTicks = 40 + this.mob.getRandom().nextInt(60);
                this.walkTarget = this.mob.position();
                return;
            }
            target = this.pickWalkTarget();
            this.walkTarget = target;
            if (target == null) {
                this.pathFails++;
                return;
            }
        }
        // El que guía camina tranquilo; si el amigo se atrasa, lo espera un poquito (más lento).
        double leadSpeed = err > 1.2D ? WALK_SPEED * 0.6D : WALK_SPEED;
        if (this.ticks % 10 == 1 || this.mob.getNavigation().isDone()) {
            if (!this.mob.getNavigation().moveTo(target.x, target.y, target.z, leadSpeed)) {
                this.pathFails++;
                this.walkTarget = null;
            }
        }
        // El amigo apunta a su lugar al costado (un poco adelantado, así no queda atrás).
        if (this.ticks % 4 == 0) {
            Vec3 ahead = slot.add(this.mob.getDeltaMovement().multiply(6.0D, 0.0D, 6.0D));
            double speed = Math.min(0.55D, Math.max(0.28D, WALK_SPEED + err * 0.2D));
            other.getNavigation().moveTo(ahead.x, ahead.y, ahead.z, speed);
        }
        if (this.ticks % 60 == 0) {
            this.mob.getLookControl().setLookAt(other, 20.0F, 10.0F);   // de vez en cuando se miran
        }
    }

    /** Un lugar para seguir paseando juntos, dentro de la zona de su pecera. */
    private @Nullable Vec3 pickWalkTarget() {
        for (int i = 0; i < 5; i++) {
            Vec3 p = net.minecraft.world.entity.ai.util.LandRandomPos.getPos(this.mob, (int) WALK_LEG, 3);
            if (p == null || NoxisSpacing.horizontal2(p, this.mob.position()) < 3.0D * 3.0D) continue;
            if (!this.insideHome(p)) continue;
            return p;
        }
        return null;
    }

    private boolean insideHome(Vec3 p) {
        if (!(this.mob instanceof NoxisRoamer roamer)) return true;
        net.minecraft.core.BlockPos home = roamer.getHomeAnchor();
        if (home == null) return true;
        return NoxisSpacing.horizontal2(p, Vec3.atBottomCenterOf(home)) <= NoxisWanderGoal.ROAM_RADIUS * NoxisWanderGoal.ROAM_RADIUS;
    }

    /** Ya están uno al lado del otro: los dos empiezan el MISMO gesto en el mismo tick. */
    private void beginAffection(PathfinderMob other) {
        this.mob.getNavigation().stop();
        face(this.mob, this.baseYaw);
        face(other, this.baseYaw);
        // Si este está a la derecha del amigo, el amigo le queda a su izquierda (y al revés).
        this.mob.setAffectionAnim(NoxisAffectionAnimation.encode(NoxisAffectionAnimation.LEAD, this.gesture,
                !this.onFriendsRight));
        ((NoxisFriend) other).setAffectionAnim(NoxisAffectionAnimation.encode(NoxisAffectionAnimation.PARTNER,
                this.gesture, this.onFriendsRight));
        this.lastGesture = this.gesture;
        this.phase = Phase.AFFECTION;
        this.ticks = 0;
    }

    private static void face(PathfinderMob mob, float yaw) {
        mob.setYRot(yaw);
        mob.yBodyRot = yaw;
        mob.yHeadRot = yaw;
        mob.setXRot(0.0F);
    }

    /** Espera hasta el próximo gesto: los cariñosos, más cortita; los tranquilos, un poco más larga. */
    private static long cooldown(PathfinderMob mob) {
        float factor = mob instanceof com.noxisculture.entity.personality.NoxisPersonable p
                ? p.getPersonality().affectionCooldown() : 1.0F;
        return (long) ((COOLDOWN + mob.getRandom().nextInt(COOLDOWN_RANDOM)) * factor);
    }

    @Override
    public void stop() {
        long now = this.mob.level().getGameTime();
        PathfinderMob other = this.friend;
        this.mob.getSocialLink().clear();
        this.mob.setAffectionAnim(NoxisAffectionAnimation.NONE);
        // Cada uno vuelve a su vida; los dos esperan un rato antes de la próxima vez.
        this.mob.setNextAffectionTime(now + cooldown(this.mob));
        if (other instanceof NoxisFriend f) {
            if (f.getSocialLink().isLinkedWith(this.mob)) f.getSocialLink().clear();
            f.setAffectionAnim(NoxisAffectionAnimation.NONE);
            f.setNextAffectionTime(now + cooldown(other));
        }
        if (other != null && this.phase == Phase.WALK) other.getNavigation().stop();
        this.mob.getNavigation().stop();
        this.friend = null;
        this.spot = null;
        this.walkTarget = null;
        this.phase = Phase.APPROACH;
    }
}
