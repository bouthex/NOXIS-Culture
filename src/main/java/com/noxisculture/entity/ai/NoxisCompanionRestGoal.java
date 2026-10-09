package com.noxisculture.entity.ai;

import com.noxisculture.entity.social.NoxisSocialAction;
import com.noxisculture.entity.social.NoxisSocialLink;
import java.util.EnumSet;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Descanso en compañía (servidor). Cuando a un Noxis le toca descansar y tiene un compañero
 * tranquilo cerca, a veces (en vez de descansar solo) le contagia el sueño:
 * <ol>
 *   <li>Cabecea y bosteza; el otro lo mira y, tras una pausa, bosteza también.</li>
 *   <li>El compañero se acerca y se pone a su lado.</li>
 *   <li>Se sientan juntos (el mismo descanso de siempre) y se apoyan uno en el otro.</li>
 *   <li>Bostezan casi a la vez, descansan un rato y se levantan con calma.</li>
 * </ol>
 *
 * <p>Usa el descanso individual que ya existe ({@link NoxisRestful}): el mismo estado de
 * descanso, la misma animación y el mismo tiempo de espera; no lo reemplaza ni lo duplica.
 * Solo una pareja por zona. Si a cualquiera de los dos lo interrumpe algo importante, se
 * cancela para ambos y vuelven a lo normal.</p>
 */
public class NoxisCompanionRestGoal<T extends PathfinderMob & NoxisSocial & NoxisRestful> extends Goal {
    private static final double RADIUS = 5.0D;
    private static final double ZONE = 16.0D;
    private static final double SIDE_DISTANCE = 0.78D;   // bloques entre los dos al sentarse
    private static final int DROWSY_TICKS = 56;
    private static final int APPROACH_TIMEOUT = 160;
    private static final int MIN_REST = 400;
    private static final int EXTRA_REST = 300;
    private static final int WAKE_TICKS = 24;

    private enum Phase { DROWSY, APPROACH, REST, WAKE, DONE }

    private final T mob;
    private @Nullable PathfinderMob partner;
    private Phase phase = Phase.DONE;
    private int ticks;
    private int restLength;
    private boolean partnerOnRight;
    private @Nullable Vec3 spot;

    public NoxisCompanionRestGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        RandomSource random = this.mob.getRandom();
        // Mismas condiciones que el descanso de siempre, y solo a veces en compañía.
        if (!this.mob.wantsToRest() || !this.mob.onGround() || this.mob.isInWater()) return false;
        if (random.nextInt(20) != 0 || random.nextInt(2) != 0) return false;
        if (this.mob.getSocialLink().isBusy() || !this.mob.canSocialize()) return false;
        if (NoxisSocialLink.zoneBusy(this.mob, ZONE, NoxisSocialLink.Kind.REST)) return false;
        this.partner = NoxisSocialLink.findPartner(this.mob, RADIUS,
                e -> e instanceof NoxisRestful r && r.isSafeToRest() && e.onGround() && !e.isInWater());
        return this.partner != null;
    }

    @Override
    public void start() {
        PathfinderMob other = this.partner;
        if (other == null) return;
        this.mob.getSocialLink().link(other, NoxisSocialLink.Kind.REST, true);
        ((NoxisSocial) other).getSocialLink().link(this.mob, NoxisSocialLink.Kind.REST, false);
        this.mob.getNavigation().stop();
        other.getNavigation().stop();
        this.phase = Phase.DROWSY;
        this.ticks = 0;
        this.restLength = MIN_REST + this.mob.getRandom().nextInt(EXTRA_REST);
        this.mob.setSocialAnim(NoxisSocialAction.REST_DROWSY);
        ((NoxisSocial) other).setSocialAnim(NoxisSocialAction.REST_WATCH);
    }

    @Override
    public boolean canContinueToUse() {
        PathfinderMob other = this.partner;
        return this.phase != Phase.DONE && other != null && other.isAlive()
                && other instanceof NoxisSocial s && s.getSocialLink().isLinkedWith(this.mob)
                && this.mob.isSafeForSocial() && s.isSafeForSocial()
                && this.mob.hurtTime == 0 && other.hurtTime == 0
                && this.mob.distanceToSqr(other) < 100.0D;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        PathfinderMob other = this.partner;
        if (other == null) return;
        NoxisSocial otherSocial = (NoxisSocial) other;
        this.ticks++;
        switch (this.phase) {
            case DROWSY -> {
                // Le agarra sueño: cabecea y bosteza. El otro lo mira... y se contagia.
                this.mob.getNavigation().stop();
                other.getNavigation().stop();
                this.mob.getLookControl().setLookAt(other, 6.0F, 20.0F);
                other.getLookControl().setLookAt(this.mob, 20.0F, 30.0F);
                if (this.ticks == 8) this.mob.playYawn();
                if (this.ticks == 46) otherSocial.playYawn();
                if (this.ticks >= DROWSY_TICKS) {
                    // El compañero se pone al lado, del lado en el que ya estaba.
                    Vec3 right = rightOf(this.mob.getYRot());
                    Vec3 toOther = other.position().subtract(this.mob.position());
                    this.partnerOnRight = toOther.x * right.x + toOther.z * right.z >= 0.0D;
                    this.spot = this.mob.position().add(right.scale(this.partnerOnRight ? SIDE_DISTANCE : -SIDE_DISTANCE));
                    this.enter(Phase.APPROACH);
                }
            }
            case APPROACH -> {
                this.mob.getNavigation().stop();
                this.mob.getLookControl().setLookAt(other, 10.0F, 20.0F);
                Vec3 target = this.spot;
                if (target == null) { this.phase = Phase.DONE; return; }
                double dx = target.x - other.getX();
                double dz = target.z - other.getZ();
                if (dx * dx + dz * dz < 0.2D * 0.2D || (this.ticks > 30 && dx * dx + dz * dz < 0.45D * 0.45D)) {
                    other.getNavigation().stop();
                    // Se sientan juntos: el MISMO descanso de siempre para los dos.
                    this.mob.setResting(true);
                    ((NoxisRestful) other).setResting(true);
                    this.mob.setSocialAnim(this.partnerOnRight ? NoxisSocialAction.REST_LEAN_RIGHT : NoxisSocialAction.REST_LEAN_LEFT);
                    otherSocial.setSocialAnim(this.partnerOnRight ? NoxisSocialAction.REST_LEAN_LEFT : NoxisSocialAction.REST_LEAN_RIGHT);
                    this.enter(Phase.REST);
                } else if (this.ticks >= APPROACH_TIMEOUT) {
                    this.phase = Phase.DONE;                    // no pudo acomodarse: lo dejan
                } else if (this.ticks % 10 == 1 || other.getNavigation().isDone()) {
                    if (!other.getNavigation().moveTo(target.x, target.y, target.z, 0.3D)) {
                        // Muy cerquita: un pasito directo.
                        other.getMoveControl().setWantedPosition(target.x, target.y, target.z, 0.3D);
                    }
                }
            }
            case REST -> {
                this.mob.getNavigation().stop();
                other.getNavigation().stop();
                // Mirando para el mismo lado, uno al lado del otro.
                float yaw = this.mob.getYRot();
                face(this.mob, yaw);
                face(other, yaw);
                // Bostezan casi a la vez (con un pequeño desfase), y alguna vez más mientras descansan.
                if (this.ticks == 30) this.mob.playYawn();
                if (this.ticks == 38) otherSocial.playYawn();
                if (this.ticks > 60 && this.mob.getRandom().nextInt(400) == 0) {
                    (this.mob.getRandom().nextBoolean() ? (NoxisSocial) this.mob : otherSocial).playYawn();
                }
                if (this.ticks >= this.restLength) {
                    this.mob.setResting(false);
                    ((NoxisRestful) other).setResting(false);
                    this.mob.setSocialAnim(NoxisSocialAction.NONE);
                    otherSocial.setSocialAnim(NoxisSocialAction.NONE);
                    this.enter(Phase.WAKE);
                }
            }
            case WAKE -> {
                // Se levantan con calma antes de volver a lo suyo.
                this.mob.getNavigation().stop();
                other.getNavigation().stop();
                if (this.ticks >= WAKE_TICKS) this.phase = Phase.DONE;
            }
            case DONE -> { }
        }
    }

    private void enter(Phase next) {
        this.phase = next;
        this.ticks = 0;
    }

    private static Vec3 rightOf(float yaw) {
        float r = yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.cos(r), 0.0D, -Mth.sin(r));
    }

    private static void face(PathfinderMob mob, float yaw) {
        mob.setYRot(yaw);
        mob.yBodyRot = yaw;
        mob.yHeadRot = yaw;
    }

    @Override
    public void stop() {
        PathfinderMob other = this.partner;
        // Termina (o se cancela) para los dos, de forma segura.
        this.mob.setResting(false);
        this.mob.setSocialAnim(NoxisSocialAction.NONE);
        this.mob.getSocialLink().clear();
        this.mob.onRestFinished();
        if (other instanceof NoxisSocial s) {
            if (s.getSocialLink().isLinkedWith(this.mob)) {
                s.getSocialLink().clear();
                s.setSocialAnim(NoxisSocialAction.NONE);
            }
            if (other instanceof NoxisRestful r) {
                r.setResting(false);
                r.onRestFinished();
            }
        }
        this.partner = null;
        this.spot = null;
        this.phase = Phase.DONE;
    }
}
