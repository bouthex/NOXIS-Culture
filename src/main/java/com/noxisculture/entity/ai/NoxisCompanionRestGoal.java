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
 * <p><b>Sumarse a alguien que ya duerme solo:</b> si cerca hay un Noxis descansando solo, el
 * que tiene sueño se acerca, se acomoda a su lado y se duermen juntos. Nunca más de dos: el
 * que duerme queda reservado apenas lo eligen, así ningún tercero puede sumarse. Si todos los
 * que duermen ya tienen pareja, el nuevo duerme solo con su descanso de siempre. Si uno de
 * los dos se despierta, el otro sigue durmiendo solo.</p>
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

    private enum Phase { DROWSY, APPROACH, JOIN_APPROACH, REST, WAKE, DONE }
    private static final double JOIN_RADIUS = 8.0D;
    private static final int JOIN_TIMEOUT = 200;

    private final T mob;
    private @Nullable PathfinderMob partner;
    private Phase phase = Phase.DONE;
    private int ticks;
    private int restLength;
    private boolean partnerOnRight;
    private @Nullable Vec3 spot;
    /** true: se suma a alguien que ya dormía solo (no lo despierta al terminar). */
    private boolean joining;
    /** true si este Noxis llegó a acostarse (para el tiempo de espera del descanso). */
    private boolean slept;

    public NoxisCompanionRestGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        RandomSource random = this.mob.getRandom();
        // Mismas condiciones que el descanso de siempre, y solo a veces en compañía.
        if (!this.mob.wantsToRest() || !this.mob.onGround() || this.mob.isInWater()) return false;
        if (random.nextInt(20) != 0) return false;
        if (this.mob.getSocialLink().isBusy() || !this.mob.canSocialize()) return false;
        // 1) ¿Hay alguien durmiendo SOLO cerca? Se acerca y se duermen juntos.
        this.partner = this.findLonelySleeper();
        if (this.partner != null) {
            this.joining = true;
            return true;
        }
        // 2) Si no, a veces contagia el sueño a un compañero despierto (como antes).
        this.joining = false;
        if (random.nextInt(2) != 0) return false;
        if (NoxisSocialLink.zoneBusy(this.mob, ZONE, NoxisSocialLink.Kind.REST)) return false;
        this.partner = NoxisSocialLink.findPartner(this.mob, RADIUS,
                e -> e instanceof NoxisRestful r && r.isSafeToRest() && e.onGround() && !e.isInWater());
        return this.partner != null;
    }

    /** Un Noxis que ya duerme y está SOLO (sin pareja ni reserva), al que se pueda llegar. */
    private @Nullable PathfinderMob findLonelySleeper() {
        PathfinderMob best = null;
        double bestDist = Double.MAX_VALUE;
        for (PathfinderMob e : this.mob.level().getEntitiesOfClass(PathfinderMob.class,
                this.mob.getBoundingBox().inflate(JOIN_RADIUS, 2.0D, JOIN_RADIUS),
                e -> e != this.mob && e.isAlive() && e instanceof NoxisRestful r && r.isResting()
                        && e instanceof NoxisSocial s && !s.getSocialLink().isBusy() && s.isSafeForSocial())) {
            double d = e.distanceToSqr(this.mob);
            if (d < bestDist && this.mob.getNavigation().createPath(e, 1) != null) {
                best = e;
                bestDist = d;
            }
        }
        return best;
    }

    @Override
    public void start() {
        PathfinderMob other = this.partner;
        if (other == null) return;
        this.mob.getSocialLink().link(other, NoxisSocialLink.Kind.REST, true);
        ((NoxisSocial) other).getSocialLink().link(this.mob, NoxisSocialLink.Kind.REST, false);
        this.ticks = 0;
        this.slept = false;
        this.restLength = MIN_REST + this.mob.getRandom().nextInt(EXTRA_REST);
        if (this.joining) {
            // Reservado: desde ya nadie más puede sumarse a ese que duerme.
            Vec3 right = rightOf(other.getYRot());
            Vec3 toMe = this.mob.position().subtract(other.position());
            boolean meOnItsRight = toMe.x * right.x + toMe.z * right.z >= 0.0D;
            this.spot = other.position().add(right.scale(meOnItsRight ? SIDE_DISTANCE : -SIDE_DISTANCE));
            this.partnerOnRight = !meOnItsRight;           // al acostarme, él queda del otro lado
            this.phase = Phase.JOIN_APPROACH;
            return;
        }
        this.mob.getNavigation().stop();
        other.getNavigation().stop();
        this.phase = Phase.DROWSY;
        this.mob.setSocialAnim(NoxisSocialAction.REST_DROWSY);
        ((NoxisSocial) other).setSocialAnim(NoxisSocialAction.REST_WATCH);
    }

    @Override
    public boolean canContinueToUse() {
        PathfinderMob other = this.partner;
        if (this.joining) {
            // Si el compañero se despierta, este sigue durmiendo solo hasta terminar.
            return this.phase != Phase.DONE && this.mob.isSafeForSocial() && this.mob.hurtTime == 0
                    && (other != null || this.phase == Phase.REST || this.phase == Phase.WAKE);
        }
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
        if (this.joining) {
            this.tickJoin();
            return;
        }
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
                    this.slept = true;
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
            case JOIN_APPROACH -> { }
            case WAKE -> {
                // Se levantan con calma antes de volver a lo suyo.
                this.mob.getNavigation().stop();
                other.getNavigation().stop();
                if (this.ticks >= WAKE_TICKS) this.phase = Phase.DONE;
            }
            case DONE -> { }
        }
    }

    /** Sumarse a alguien que ya dormía solo. */
    private void tickJoin() {
        this.ticks++;
        PathfinderMob other = this.partner;
        // ¿Se despertó (o desapareció) el compañero? Se libera la pareja; este sigue solo.
        if (other != null && (!other.isAlive() || !(other instanceof NoxisRestful r) || !r.isResting()
                || !((NoxisSocial) other).getSocialLink().isLinkedWith(this.mob))) {
            this.releasePartner();
            other = null;
            if (this.phase == Phase.JOIN_APPROACH) {   // todavía no se había acostado: desiste
                this.phase = Phase.DONE;
                return;
            }
            this.mob.setSocialAnim(NoxisSocialAction.NONE);
        }
        switch (this.phase) {
            case JOIN_APPROACH -> {
                if (other == null || this.spot == null) { this.phase = Phase.DONE; return; }
                this.mob.getLookControl().setLookAt(other, 10.0F, 20.0F);
                Vec3 target = this.spot;
                double dx = target.x - this.mob.getX();
                double dz = target.z - this.mob.getZ();
                double d2 = dx * dx + dz * dz;
                if (d2 < 0.2D * 0.2D || (this.ticks > 40 && d2 < 0.45D * 0.45D)) {
                    this.mob.getNavigation().stop();
                    // Se acomoda a su lado, bosteza y se duerme apoyado en él.
                    face(this.mob, other.getYRot());
                    this.mob.setResting(true);
                    this.slept = true;
                    this.mob.playYawn();
                    this.mob.setSocialAnim(this.partnerOnRight ? NoxisSocialAction.REST_LEAN_RIGHT : NoxisSocialAction.REST_LEAN_LEFT);
                    ((NoxisSocial) other).setSocialAnim(this.partnerOnRight ? NoxisSocialAction.REST_LEAN_LEFT : NoxisSocialAction.REST_LEAN_RIGHT);
                    this.enter(Phase.REST);
                } else if (this.ticks >= JOIN_TIMEOUT) {
                    this.phase = Phase.DONE;                    // no llegó: duerme solo otro día
                } else if (this.ticks % 10 == 1 || this.mob.getNavigation().isDone()) {
                    if (!this.mob.getNavigation().moveTo(target.x, target.y, target.z, 0.3D)) {
                        this.mob.getMoveControl().setWantedPosition(target.x, target.y, target.z, 0.3D);
                    }
                }
            }
            case REST -> {
                this.mob.getNavigation().stop();
                if (other != null) face(this.mob, other.getYRot());
                if (this.ticks == 40 && other != null) ((NoxisSocial) other).playYawn();
                if (this.ticks > 60 && this.mob.getRandom().nextInt(400) == 0) this.mob.playYawn();
                if (this.ticks >= this.restLength) {
                    this.mob.setResting(false);
                    this.mob.setSocialAnim(NoxisSocialAction.NONE);
                    this.releasePartner();
                    this.enter(Phase.WAKE);
                }
            }
            case WAKE -> {
                this.mob.getNavigation().stop();
                if (this.ticks >= WAKE_TICKS) this.phase = Phase.DONE;
            }
            default -> this.phase = Phase.DONE;
        }
    }

    /** Suelta la pareja sin despertarla (sigue durmiendo sola con su descanso de siempre). */
    private void releasePartner() {
        PathfinderMob other = this.partner;
        if (other instanceof NoxisSocial s && s.getSocialLink().isLinkedWith(this.mob)) {
            s.getSocialLink().clear();
            s.setSocialAnim(NoxisSocialAction.NONE);
        }
        this.partner = null;
        this.mob.getSocialLink().clear();
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
        if (this.joining) {
            this.releasePartner();
            this.mob.setResting(false);
            this.mob.setSocialAnim(NoxisSocialAction.NONE);
            this.mob.getSocialLink().clear();
            if (this.slept) this.mob.onRestFinished();
            this.spot = null;
            this.phase = Phase.DONE;
            this.joining = false;
            return;
        }
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
