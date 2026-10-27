package com.noxisculture.entity.ai;

import com.noxisculture.config.NoxisConfig;
import com.noxisculture.entity.idle.NoxisAmbientAnimation;
import com.noxisculture.entity.personality.NoxisPersonality;
import com.noxisculture.entity.social.NoxisFriendship;
import com.noxisculture.entity.social.NoxisSocialLink;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Compartir un momento de tranquilidad (servidor). Un Noxis se detiene en un lugar lindo cerca y otro
 * lo acompaña: se ubican uno al lado del otro (a una distancia natural, sin tocarse ni superponerse),
 * miran el paisaje para el mismo lado, a veces sentados, y de vez en cuando uno gira la cabeza hacia
 * el otro. No hablan ni hacen nada especial: solo se acompañan. Sin gestos románticos.
 *
 * <ul>
 *   <li>Con quién: casi siempre su amigo especial (si tiene y está cerca); a veces, otro Noxis cercano
 *   con el que todavía no son amigos. Usa el sistema de amistad que ya existe (no crea otro).</li>
 *   <li>Personalidad: los cariñosos y los tranquilos lo buscan más; los traviesos, menos (ninguno queda
 *   afuera).</li>
 *   <li>Cada uno tiene su propio tiempo: uno puede levantarse antes; entonces mira al otro un momento y
 *   los dos siguen con lo suyo. Nadie se queda esperando.</li>
 *   <li>Lugar: a pocos pasos, dentro de su zona; si no hay lugar, no pasa. Poco frecuente (espera de 6 a
 *   12 min, escalada por la opción "Momentos compartidos"). Prioridad baja: sueño, descanso, objetos,
 *   regalos, lectura, pintura, peligro y demás lo cortan.</li>
 * </ul>
 */
public class NoxisQuietMomentGoal<T extends PathfinderMob & NoxisAmbient> extends Goal {
    private static final double SEARCH = 10.0D;
    private static final double SIDE = 1.15D;          // distancia entre los dos (bloques)
    private static final int APPROACH_TIMEOUT = 240;
    private static final int COOLDOWN = 7200;
    private static final int COOLDOWN_RANDOM = 7200;
    private static final double SPEED = 0.38D;

    private enum Phase { APPROACH, TOGETHER, PARTING }

    private final T mob;
    private int nextCheckTick;
    private long nextAllowedTime = -1L;
    private @Nullable PathfinderMob partner;
    private Phase phase = Phase.APPROACH;
    private int ticks;
    private @Nullable Vec3 mySpot;
    private @Nullable Vec3 partnerSpot;
    private float viewYaw;
    private boolean partnerOnRight;
    private boolean sit;
    private int myLength;
    private int partnerLength;
    private boolean partnerUp;

    public NoxisQuietMomentGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        this.nextCheckTick = mob.getRandom().nextInt(300);
    }

    private static float wish(NoxisPersonality p) {
        return switch (p) {
            case AFFECTIONATE -> 0.35F;
            case CALM -> 0.3F;
            case ARTIST -> 0.25F;
            case CURIOUS -> 0.2F;
            case MISCHIEVOUS -> 0.12F;
        };
    }

    @Override
    public boolean canUse() {
        if (this.mob.tickCount < this.nextCheckTick) return false;
        RandomSource random = this.mob.getRandom();
        this.nextCheckTick = this.mob.tickCount + 100 + random.nextInt(100);
        int freq = NoxisConfig.get().quietMoments;
        if (!NoxisConfig.enabled(freq)) return false;
        long now = this.mob.level().getGameTime();
        if (this.nextAllowedTime < 0L) this.nextAllowedTime = now + NoxisConfig.scaleCooldown(3600 + random.nextInt(6000), freq);
        if (now < this.nextAllowedTime || !this.mob.canDoAmbient() || !this.mob.canSocialize() || this.mob.getSocialLink().isBusy()) return false;
        if (!this.mob.onGround() || this.mob.isInWater() || random.nextFloat() >= wish(this.mob.getPersonality())) return false;
        if (NoxisSocialLink.zoneBusy(this.mob, 8.0D, NoxisSocialLink.Kind.QUIET)) return false;
        // Su amigo especial (si está cerca y libre) o, a veces, otro Noxis cercano.
        PathfinderMob other = NoxisFriendship.findFriend(this.mob);
        if (other == null || other.distanceToSqr(this.mob) > SEARCH * SEARCH || !free(other)) {
            other = random.nextInt(3) == 0 ? NoxisSocialLink.findPartner(this.mob, SEARCH, NoxisQuietMomentGoal::free) : null;
        }
        if (other == null) return false;
        if (!this.planSpots(other)) return false;
        this.partner = other;
        return true;
    }

    private static boolean free(PathfinderMob e) {
        return e instanceof NoxisAmbient a && a.canDoAmbient() && a.canSocialize() && !a.getSocialLink().isBusy()
                && e.onGround() && !e.isInWater();
    }

    /** Dos lugares uno al lado del otro, cerca, mirando a un lado abierto (sin paredes adelante). */
    private boolean planSpots(PathfinderMob other) {
        Level level = this.mob.level();
        RandomSource random = this.mob.getRandom();
        Vec3 mid = this.mob.position().add(other.position()).scale(0.5D);
        for (int i = 0; i < 8; i++) {
            BlockPos base = BlockPos.containing(mid.x + random.nextInt(7) - 3, mid.y, mid.z + random.nextInt(7) - 3);
            for (int dy = -1; dy <= 1; dy++) {
                BlockPos p = base.above(dy);
                if (!NoxisAmbientUtil.standable(level, p)) continue;
                float yaw = random.nextFloat() * 360.0F;
                Vec3 a = Vec3.atBottomCenterOf(p);
                Vec3 b = a.add(NoxisAmbientUtil.right(yaw).scale(SIDE));
                BlockPos pb = BlockPos.containing(b);
                if (!NoxisAmbientUtil.standable(level, pb)) continue;
                // Adelante, unos bloques libres (que haya algo para mirar).
                BlockPos ahead = BlockPos.containing(a.add(NoxisAmbientUtil.forward(yaw).scale(2.5D))).above();
                if (!level.getBlockState(ahead).isAir()) continue;
                if (!NoxisAmbientUtil.insideHome(this.mob, a) || !NoxisAmbientUtil.insideHome(this.mob, b)) continue;
                this.mySpot = a;
                this.partnerSpot = b;
                this.viewYaw = yaw;
                this.partnerOnRight = true;          // el amigo queda a SU derecha
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        PathfinderMob other = this.partner;
        if (other == null) return;
        this.mob.getSocialLink().link(other, NoxisSocialLink.Kind.QUIET, true);
        ((NoxisSocial) other).getSocialLink().link(this.mob, NoxisSocialLink.Kind.QUIET, false);
        RandomSource random = this.mob.getRandom();
        this.phase = Phase.APPROACH;
        this.ticks = 0;
        this.partnerUp = false;
        float k = this.mob.getPersonality() == NoxisPersonality.CALM || this.mob.getPersonality() == NoxisPersonality.AFFECTIONATE ? 1.4F : 1.0F;
        this.myLength = (int) ((300 + random.nextInt(600)) * k);         // 15 a 45 s (o más)
        this.partnerLength = Math.max(160, this.myLength + random.nextInt(201) - 100);   // casi lo mismo, no igual
        this.sit = random.nextFloat() < 0.6F;
    }

    @Override
    public boolean canContinueToUse() {
        PathfinderMob other = this.partner;
        if (other == null || !other.isAlive() || !(other instanceof NoxisAmbient a)) return false;
        if (!a.getSocialLink().isLinkedWith(this.mob) || !this.mob.getSocialLink().isLinkedWith(other)) return false;
        if (!this.mob.isSafeForSocial() || !a.isSafeForSocial() || this.mob.hurtTime > 0 || other.hurtTime > 0) return false;
        if (this.phase == Phase.APPROACH) return this.ticks < APPROACH_TIMEOUT;
        if (this.phase == Phase.PARTING) return this.ticks < 30;
        return this.mob.distanceToSqr(other) < 3.0D * 3.0D;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        PathfinderMob other = this.partner;
        Vec3 a = this.mySpot;
        Vec3 b = this.partnerSpot;
        if (other == null || a == null || b == null) return;
        this.ticks++;
        NoxisAmbient pa = (NoxisAmbient) other;
        switch (this.phase) {
            case APPROACH -> {
                boolean meThere = NoxisSpacing.horizontal2(this.mob.position(), a) < 0.35D * 0.35D;
                boolean otherThere = NoxisSpacing.horizontal2(other.position(), b) < 0.35D * 0.35D;
                if (this.ticks % 10 == 1) {
                    if (!meThere) this.mob.getNavigation().moveTo(a.x, a.y, a.z, SPEED);
                    if (!otherThere) other.getNavigation().moveTo(b.x, b.y, b.z, SPEED);
                }
                if (meThere) this.mob.getNavigation().stop();
                if (otherThere) other.getNavigation().stop();
                if ((meThere && otherThere) || (this.ticks > 160 && this.mob.distanceToSqr(other) < 2.2D * 2.2D)) {
                    this.mob.getNavigation().stop();
                    other.getNavigation().stop();
                    this.phase = Phase.TOGETHER;
                    this.ticks = 0;
                    byte state = this.sit ? NoxisAmbientAnimation.QUIET_SIT : NoxisAmbientAnimation.QUIET_STAND;
                    this.mob.setAmbientAnim(NoxisAmbientAnimation.encode(state, this.partnerOnRight));
                    pa.setAmbientAnim(NoxisAmbientAnimation.encode(state, !this.partnerOnRight));
                }
            }
            case TOGETHER -> {
                this.mob.getNavigation().stop();
                other.getNavigation().stop();
                NoxisAmbientUtil.face(this.mob, this.viewYaw);
                NoxisAmbientUtil.face(other, this.viewYaw);
                // Uno se levanta antes que el otro: se para, mira al compañero un momento y terminan.
                if (!this.partnerUp && this.ticks >= this.partnerLength) {
                    this.partnerUp = true;
                    pa.setAmbientAnim(NoxisAmbientAnimation.NONE);
                }
                if (this.ticks >= Math.max(this.myLength, this.partnerLength) || (this.partnerUp && this.ticks >= this.myLength)) {
                    this.mob.setAmbientAnim(NoxisAmbientAnimation.NONE);
                    pa.setAmbientAnim(NoxisAmbientAnimation.NONE);
                    this.phase = Phase.PARTING;
                    this.ticks = 0;
                }
            }
            case PARTING -> {
                this.mob.getLookControl().setLookAt(other, 20.0F, 20.0F);
                other.getLookControl().setLookAt(this.mob, 20.0F, 20.0F);
            }
        }
    }

    @Override
    public void stop() {
        PathfinderMob other = this.partner;
        this.mob.setAmbientAnim(NoxisAmbientAnimation.NONE);
        this.mob.getSocialLink().clear();
        this.mob.getNavigation().stop();
        if (other instanceof NoxisAmbient pa) {
            if (pa.getSocialLink().isLinkedWith(this.mob)) pa.getSocialLink().clear();
            pa.setAmbientAnim(NoxisAmbientAnimation.NONE);
            other.getNavigation().stop();
        }
        this.partner = null;
        this.mySpot = null;
        this.partnerSpot = null;
        RandomSource random = this.mob.getRandom();
        this.nextAllowedTime = this.mob.level().getGameTime()
                + NoxisConfig.scaleCooldown(COOLDOWN + random.nextInt(COOLDOWN_RANDOM), NoxisConfig.get().quietMoments);
    }
}
