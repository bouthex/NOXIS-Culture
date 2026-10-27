package com.noxisculture.entity.ai;

import com.noxisculture.entity.idle.NoxisAmbientAnimation;
import com.noxisculture.entity.mood.NoxisMood;
import com.noxisculture.entity.personality.NoxisPersonality;
import com.noxisculture.entity.social.NoxisFriendship;
import com.noxisculture.entity.social.NoxisSocialLink;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Preocuparse por un compañero (servidor). Cuando a un Noxis le pasa algo feo DE VERDAD (lo
 * lastimaron o se cayó bailando: las únicas señales fiables que existen; no se inventan tristezas,
 * enfermedades ni curaciones), y una vez que el peligro pasó, otro Noxis puede acercarse a acompañarlo.
 *
 * <ul>
 *   <li>Quién: su amigo especial, casi siempre; si no, a veces otro Noxis cercano, según cómo es
 *   (los cariñosos, más; los tranquilos se quedan mirando desde una distancia prudente).</li>
 *   <li>Qué hace: se detiene, lo mira, se acerca despacio, se queda a su lado con la cabeza ladeada y,
 *   si es su amigo, a veces se sienta junto a él; el otro gira la cabeza hacia él y las orejitas
 *   vuelven a levantarse. Un corazoncito si son amigos. Después cada uno sigue con lo suyo.</li>
 *   <li>El otro puede no querer: a veces sigue con lo suyo y el que vino solo lo mira un momento.</li>
 *   <li>Sin reacciones de grupo: uno solo acompaña a cada uno, y a la misma persona no vuelve a ir a
 *   verla enseguida. No persigue: si el otro se aleja, lo deja. Dormir, descansar, leer, escribir,
 *   pintar o mirar el cielo NO son malestar (solo cuentan los golpes y las caídas).</li>
 * </ul>
 */
public class NoxisConcernGoal<T extends PathfinderMob & NoxisAmbient> extends Goal {
    private static final double SEARCH = 14.0D;
    /** El malestar cuenta desde 2 hasta 30 s después de que pasó. */
    private static final long MIN_AFTER = 40L;
    private static final long MAX_AFTER = 600L;
    private static final int COOLDOWN = 1800;
    private static final int APPROACH_TIMEOUT = 200;
    private static final double SPEED = 0.42D;
    /** A quién ya fueron a acompañar (id de la entidad → hasta cuándo no se repite). */
    private static final Map<Integer, Long> COMFORTED = new HashMap<>();

    private enum Phase { WATCH, APPROACH, BESIDE }

    private final T mob;
    private int nextCheckTick;
    private long nextAllowedTime;
    private @Nullable PathfinderMob other;
    private boolean friend;
    private boolean onlyWatch;
    private Phase phase = Phase.WATCH;
    private int ticks;
    private int length;
    private @Nullable Vec3 spot;
    private boolean onRight;
    private float baseYaw;

    public NoxisConcernGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        this.nextCheckTick = mob.getRandom().nextInt(60);
    }

    private static float care(NoxisPersonality p) {
        return switch (p) {
            case AFFECTIONATE -> 0.6F;
            case CURIOUS, ARTIST -> 0.35F;
            case CALM, MISCHIEVOUS -> 0.25F;
        };
    }

    private boolean distressed(PathfinderMob e, long now) {
        if (!(e instanceof NoxisAmbient a) || !e.isAlive() || e == this.mob) return false;
        long t = a.getLastDistressTime();
        if (t < 0L || now - t < MIN_AFTER || now - t > MAX_AFTER) return false;
        Long until = COMFORTED.get(e.getId());
        if (until != null && until > now) return false;
        // El peligro ya pasó (sin miedo y sin monstruos cerca) y no está ocupado con otra cosa.
        if (e instanceof com.noxisculture.entity.custom.NoxisVillager v && v.getMood() != NoxisMood.NEUTRAL) return false;
        if (!e.level().getEntitiesOfClass(Monster.class, e.getBoundingBox().inflate(8.0D)).isEmpty()) return false;
        return a.canSocialize() && !a.getSocialLink().isBusy();
    }

    @Override
    public boolean canUse() {
        if (this.mob.tickCount < this.nextCheckTick) return false;
        RandomSource random = this.mob.getRandom();
        this.nextCheckTick = this.mob.tickCount + 40 + random.nextInt(40);
        long now = this.mob.level().getGameTime();
        COMFORTED.values().removeIf(t -> t < now);
        if (now < this.nextAllowedTime || !this.mob.canDoAmbient() || !this.mob.canSocialize() || this.mob.getSocialLink().isBusy()) return false;
        PathfinderMob f = NoxisFriendship.findFriend(this.mob);
        PathfinderMob pick = null;
        boolean isFriend = false;
        if (f != null && f.distanceToSqr(this.mob) < SEARCH * SEARCH && this.distressed(f, now) && random.nextFloat() < 0.85F) {
            pick = f;
            isFriend = true;
        } else {
            List<PathfinderMob> near = this.mob.level().getEntitiesOfClass(PathfinderMob.class,
                    this.mob.getBoundingBox().inflate(10.0D, 3.0D, 10.0D), e -> this.distressed(e, now) && this.mob.hasLineOfSight(e));
            if (!near.isEmpty() && random.nextFloat() < care(this.mob.getPersonality())) {
                pick = near.get(random.nextInt(near.size()));
            }
        }
        if (pick == null) return false;
        this.other = pick;
        this.friend = isFriend;
        // Los tranquilos (y a veces los demás que no son amigos) miran desde una distancia prudente.
        boolean shy = this.mob.getPersonality() == NoxisPersonality.CALM || (!isFriend && random.nextInt(3) == 0);
        // Y el otro a veces prefiere seguir con lo suyo.
        boolean declines = random.nextFloat() < 0.2F;
        this.onlyWatch = shy || declines;
        COMFORTED.put(pick.getId(), now + 2400);
        return true;
    }

    @Override
    public void start() {
        PathfinderMob o = this.other;
        if (o == null) return;
        RandomSource random = this.mob.getRandom();
        this.ticks = 0;
        this.length = 100 + random.nextInt(140);
        if (this.onlyWatch) {
            this.phase = Phase.WATCH;
            this.mob.getNavigation().stop();
            this.mob.setAmbientAnim(NoxisAmbientAnimation.CONCERN_WATCH);
            return;
        }
        this.mob.getSocialLink().link(o, NoxisSocialLink.Kind.CONCERN, true);
        ((NoxisSocial) o).getSocialLink().link(this.mob, NoxisSocialLink.Kind.CONCERN, false);
        o.getNavigation().stop();
        this.phase = Phase.APPROACH;
        this.baseYaw = o.getYRot();
        Vec3 right = NoxisAmbientUtil.right(this.baseYaw);
        Vec3 toMe = this.mob.position().subtract(o.position());
        this.onRight = toMe.x * right.x + toMe.z * right.z >= 0.0D;
        this.spot = o.position().add(right.scale(this.onRight ? 0.95D : -0.95D));
        this.mob.setAmbientAnim(NoxisAmbientAnimation.CONCERN_WATCH);
    }

    @Override
    public boolean canContinueToUse() {
        PathfinderMob o = this.other;
        if (o == null || !o.isAlive() || this.mob.hurtTime > 0 || !this.mob.canDoAmbient()) return false;
        if (this.phase == Phase.WATCH) return this.ticks < 70 && this.mob.distanceToSqr(o) < 16.0D * 16.0D;
        if (!(o instanceof NoxisAmbient a) || !a.getSocialLink().isLinkedWith(this.mob) || !this.mob.getSocialLink().isLinkedWith(o)) return false;
        if (!a.isSafeForSocial() || o.hurtTime > 0) return false;
        if (this.phase == Phase.APPROACH) return this.ticks < APPROACH_TIMEOUT && this.mob.distanceToSqr(o) < 18.0D * 18.0D;
        return this.ticks < this.length && this.mob.distanceToSqr(o) < 2.5D * 2.5D;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        PathfinderMob o = this.other;
        if (o == null) return;
        this.ticks++;
        this.mob.getLookControl().setLookAt(o, 20.0F, 20.0F);
        switch (this.phase) {
            case WATCH -> this.mob.getNavigation().stop();
            case APPROACH -> {
                o.getNavigation().stop();
                o.getLookControl().setLookAt(this.mob, 20.0F, 20.0F);
                Vec3 s = this.spot;
                if (s == null) return;
                double d2 = NoxisSpacing.horizontal2(this.mob.position(), s);
                if (d2 < 0.3D * 0.3D || (this.ticks > 60 && d2 < 0.6D * 0.6D)) {
                    this.mob.getNavigation().stop();
                    this.phase = Phase.BESIDE;
                    this.ticks = 0;
                    // Al lado, mirando para el mismo lado; si son amigos, a veces se sientan juntos.
                    boolean sit = this.friend && this.mob.getRandom().nextBoolean() && this.mob.onGround();
                    byte me = sit ? NoxisAmbientAnimation.CONCERN_SIT : NoxisAmbientAnimation.CONCERN_STAND;
                    this.mob.setAmbientAnim(NoxisAmbientAnimation.encode(me, !this.onRight));
                    ((NoxisAmbient) o).setAmbientAnim(NoxisAmbientAnimation.encode(
                            sit ? NoxisAmbientAnimation.CONCERN_SIT : NoxisAmbientAnimation.CONCERN_RECEIVE, this.onRight));
                } else if (this.ticks % 10 == 1) {
                    this.mob.getNavigation().moveTo(s.x, s.y, s.z, SPEED);
                }
            }
            case BESIDE -> {
                this.mob.getNavigation().stop();
                o.getNavigation().stop();
                NoxisAmbientUtil.face(this.mob, this.baseYaw);
                if (this.ticks == 50 && this.friend && this.mob.level() instanceof ServerLevel server) {
                    Vec3 mid = this.mob.position().add(o.position()).scale(0.5D);
                    server.sendParticles(ParticleTypes.HEART, mid.x, this.mob.getEyeY() + 0.7D, mid.z, 1, 0.05D, 0.05D, 0.05D, 0.0D);
                }
            }
        }
    }

    @Override
    public void stop() {
        PathfinderMob o = this.other;
        this.mob.setAmbientAnim(NoxisAmbientAnimation.NONE);
        this.mob.getNavigation().stop();
        if (this.mob.getSocialLink().isLinkedWith(o == null ? this.mob : o)) this.mob.getSocialLink().clear();
        if (o instanceof NoxisAmbient a && !this.onlyWatch) {
            if (a.getSocialLink().isLinkedWith(this.mob)) a.getSocialLink().clear();
            a.setAmbientAnim(NoxisAmbientAnimation.NONE);
        }
        this.other = null;
        this.spot = null;
        this.nextAllowedTime = this.mob.level().getGameTime() + COOLDOWN + this.mob.getRandom().nextInt(COOLDOWN);
    }
}
