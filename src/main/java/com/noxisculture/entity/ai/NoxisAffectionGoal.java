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
 */
public class NoxisAffectionGoal<T extends PathfinderMob & NoxisFriend> extends Goal {
    private static final double MAX_DISTANCE = 10.0D;
    private static final double ZONE = 8.0D;
    private static final int APPROACH_TIMEOUT = 200;
    private static final int COOLDOWN = 2400;          // 2 min ...
    private static final int COOLDOWN_RANDOM = 2400;   // ... a 4 min
    private static final double SPEED = 0.4D;

    private enum Phase { APPROACH, AFFECTION }

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
        other.getNavigation().stop();
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

    @Override
    public void stop() {
        long now = this.mob.level().getGameTime();
        PathfinderMob other = this.friend;
        this.mob.getSocialLink().clear();
        this.mob.setAffectionAnim(NoxisAffectionAnimation.NONE);
        // Cada uno vuelve a su vida; los dos esperan un rato antes de la próxima vez.
        this.mob.setNextAffectionTime(now + COOLDOWN + this.mob.getRandom().nextInt(COOLDOWN_RANDOM));
        if (other instanceof NoxisFriend f) {
            if (f.getSocialLink().isLinkedWith(this.mob)) f.getSocialLink().clear();
            f.setAffectionAnim(NoxisAffectionAnimation.NONE);
            f.setNextAffectionTime(now + COOLDOWN + other.getRandom().nextInt(COOLDOWN_RANDOM));
        }
        this.friend = null;
        this.spot = null;
    }
}
