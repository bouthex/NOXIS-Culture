package com.noxisculture.entity.ai;

import com.noxisculture.entity.social.NoxisSocialAction;
import com.noxisculture.entity.social.NoxisSocialLink;
import java.util.EnumSet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Descanso en compañía (servidor). Cuando a un Noxis le toca descansar, ANTES de dormirse busca
 * compañía, en este orden:
 * <ol>
 *   <li><b>Alguien que ya duerme solo:</b> camina hasta su lado y se duerme apoyado en él.</li>
 *   <li><b>Si no hay:</b> un compañero tranquilo que pueda descansar. Cabecea y bosteza, el otro lo
 *       mira y se contagia; él se sienta y el compañero viene a acomodarse a su lado.</li>
 *   <li><b>Si no encuentra a nadie</b> tras unos segundos de buscar: duerme solo, con el descanso
 *       individual de siempre ({@link NoxisRestGoal}).</li>
 * </ol>
 *
 * <p>Parejas de a dos como máximo: apenas uno elige a otro, los dos quedan reservados entre sí y
 * nadie más puede elegirlos. Cada uno duerme con SU PROPIO tiempo de descanso y se despierta por
 * su cuenta: el que se despierta primero se levanta y sigue con su vida; el otro sigue durmiendo,
 * la pareja se libera y otro Noxis puede venir a acompañarlo.</p>
 *
 * <p>Usa el descanso que ya existe ({@link NoxisRestful}): el mismo estado, la misma animación,
 * la misma duración y el mismo tiempo de espera.</p>
 */
public class NoxisCompanionRestGoal<T extends PathfinderMob & NoxisSocial & NoxisRestful> extends Goal {
    private static final double SEARCH_RADIUS = 8.0D;    // hasta dónde busca compañía (bloques)
    private static final int SEARCH_TICKS = 100;          // 5 s buscando antes de dormir solo
    private static final int SOLO_WINDOW = 400;           // después, 20 s para dormirse solo
    private static final double SIDE_DISTANCE = 0.78D;    // bloques entre los dos al acostarse
    private static final int DROWSY_TICKS = 56;
    private static final int WALK_TIMEOUT = 200;          // 10 s para llegar al lado del otro
    private static final int MIN_REST = 400;
    private static final int EXTRA_REST = 300;
    private static final int WAKE_TICKS = 24;

    private enum Phase { SEARCH, DROWSY, WALK, REST, WAKE, DONE }

    private final T mob;
    private Phase phase = Phase.DONE;
    private int ticks;
    private int restLength;
    /** Su compañero de siesta (o null si duerme solo). */
    private @Nullable PathfinderMob partner;
    /** true: camina hasta el compañero, que ya está dormido (o se está por dormir). */
    private boolean goingToPartner;
    private @Nullable Vec3 spot;
    /** De qué lado le queda el compañero una vez acostados. */
    private boolean partnerOnRight;
    private boolean slept;

    public NoxisCompanionRestGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    // ------------------------------------------------------------------ empezar

    @Override
    public boolean canUse() {
        NoxisSocialLink link = this.mob.getSocialLink();
        // Lo invitó un compañero con sueño: va a acostarse a su lado.
        PathfinderMob inviter = link.getInviter();
        if (inviter != null) {
            this.partner = inviter;
            this.goingToPartner = true;
            return true;
        }
        // Le toca descansar: primero busca compañía (salvo que ya haya buscado y no encontró).
        if (!this.mob.wantsToRest() || this.mob.mayRestAlone()) return false;
        if (!this.mob.onGround() || this.mob.isInWater() || this.mob.getRandom().nextInt(20) != 0) return false;
        if (link.isBusy() || !this.mob.canSocialize()) return false;
        this.partner = null;
        this.goingToPartner = false;
        return true;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.slept = false;
        this.spot = null;
        this.restLength = MIN_REST + this.mob.getRandom().nextInt(EXTRA_REST);
        NoxisSocialLink link = this.mob.getSocialLink();
        if (this.goingToPartner && this.partner != null) {
            link.clearInvite();
            this.planSpotNextTo(this.partner);
            this.phase = Phase.WALK;
        } else {
            this.phase = Phase.SEARCH;
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (this.phase == Phase.DONE || this.mob.hurtTime != 0) return false;
        return this.mob.isSafeForSocial();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    // ------------------------------------------------------------------ cada tick

    @Override
    public void tick() {
        this.ticks++;
        this.checkPartner();
        switch (this.phase) {
            case SEARCH -> this.tickSearch();
            case DROWSY -> this.tickDrowsy();
            case WALK -> this.tickWalk();
            case REST -> this.tickRest();
            case WAKE -> {
                this.mob.getNavigation().stop();
                if (this.ticks >= WAKE_TICKS) this.phase = Phase.DONE;
            }
            case DONE -> { }
        }
    }

    /** 1) busca alguien dormido solo; 2) si no, un compañero tranquilo; 3) si no, dormirá solo. */
    private void tickSearch() {
        this.mob.getNavigation().stop();
        if (this.ticks % 20 == 1) {
            PathfinderMob sleeper = this.findLonelySleeper();
            if (sleeper != null) {
                this.reserve(sleeper);
                this.goingToPartner = true;
                this.planSpotNextTo(sleeper);
                this.enter(Phase.WALK);
                return;
            }
            PathfinderMob awake = NoxisSocialLink.findPartner(this.mob, SEARCH_RADIUS,
                    e -> e instanceof NoxisRestful r && r.isSafeToRest() && e.onGround() && !e.isInWater()
                            && ((NoxisSocial) e).getSocialLink().getInviter() == null
                            && this.mob.getNavigation().createPath(e, 1) != null);
            if (awake != null) {
                this.reserve(awake);
                awake.getNavigation().stop();
                this.mob.setSocialAnim(NoxisSocialAction.REST_DROWSY);
                ((NoxisSocial) awake).setSocialAnim(NoxisSocialAction.REST_WATCH);
                this.enter(Phase.DROWSY);
                return;
            }
        }
        if (this.ticks >= SEARCH_TICKS) {
            // Nadie disponible: duerme solo, con el descanso de siempre.
            this.mob.allowRestAlone(SOLO_WINDOW);
            this.phase = Phase.DONE;
        }
    }

    /** Le agarra sueño; el compañero lo mira, se contagia y después viene a acostarse a su lado. */
    private void tickDrowsy() {
        PathfinderMob other = this.partner;
        if (other == null) {                      // el compañero se fue: busca otra vez desde cero
            this.mob.setSocialAnim(NoxisSocialAction.NONE);
            this.enter(Phase.SEARCH);
            return;
        }
        this.mob.getNavigation().stop();
        other.getNavigation().stop();
        this.mob.getLookControl().setLookAt(other, 6.0F, 20.0F);
        other.getLookControl().setLookAt(this.mob, 20.0F, 30.0F);
        if (this.ticks == 8) this.mob.playYawn();
        if (this.ticks == 46) ((NoxisSocial) other).playYawn();
        if (this.ticks >= DROWSY_TICKS) {
            // Se acuesta acá mismo; el compañero (con SU propio descanso) viene a su lado.
            this.mob.setSocialAnim(NoxisSocialAction.NONE);
            ((NoxisSocial) other).setSocialAnim(NoxisSocialAction.NONE);
            ((NoxisSocial) other).getSocialLink().invite(this.mob);
            this.fallAsleep();
            this.enter(Phase.REST);
        }
    }

    /** Camina hasta el lado del compañero (con navegación, con límite de tiempo) y se acuesta. */
    private void tickWalk() {
        PathfinderMob other = this.partner;
        if (other == null || this.spot == null) {   // ya no está (se despertó / se fue): desiste
            this.phase = Phase.DONE;
            return;
        }
        this.mob.getLookControl().setLookAt(other, 10.0F, 20.0F);
        Vec3 target = this.spot;
        double dx = target.x - this.mob.getX();
        double dz = target.z - this.mob.getZ();
        double d2 = dx * dx + dz * dz;
        if (d2 < 0.2D * 0.2D || (this.ticks > 40 && d2 < 0.45D * 0.45D)) {
            this.mob.getNavigation().stop();
            face(this.mob, other.getYRot());
            this.mob.playYawn();
            this.fallAsleep();
            // Se apoyan uno en el otro.
            this.mob.setSocialAnim(this.partnerOnRight ? NoxisSocialAction.REST_LEAN_RIGHT : NoxisSocialAction.REST_LEAN_LEFT);
            ((NoxisSocial) other).setSocialAnim(this.partnerOnRight ? NoxisSocialAction.REST_LEAN_LEFT : NoxisSocialAction.REST_LEAN_RIGHT);
            this.enter(Phase.REST);
        } else if (this.ticks >= WALK_TIMEOUT) {
            this.phase = Phase.DONE;                                   // no llegó: sigue con lo suyo
        } else if (this.ticks % 10 == 1 || this.mob.getNavigation().isDone()) {
            if (!this.mob.getNavigation().moveTo(target.x, target.y, target.z, 0.3D)) {
                this.mob.getMoveControl().setWantedPosition(target.x, target.y, target.z, 0.3D);
            }
        }
    }

    /** Duerme con su propio tiempo; si el compañero se despierta antes, sigue durmiendo solo. */
    private void tickRest() {
        this.mob.getNavigation().stop();
        PathfinderMob other = this.partner;
        if (other != null && other instanceof NoxisRestful r && r.isResting()) {
            face(this.mob, other.getYRot());
            if (this.ticks == 40) ((NoxisSocial) other).playYawn();
        }
        if (this.ticks > 60 && this.mob.getRandom().nextInt(400) == 0) this.mob.playYawn();
        if (this.ticks >= this.restLength) {
            // Se despierta él solo: libera la pareja SIN despertar al otro.
            this.releasePartner();
            this.mob.setResting(false);
            this.mob.setSocialAnim(NoxisSocialAction.NONE);
            this.enter(Phase.WAKE);
        }
    }

    // ------------------------------------------------------------------ pareja

    /**
     * ¿Sigue la pareja? Si el compañero desapareció, se despertó (estando este ya acostado y el
     * otro ya acostado también) o soltó el enlace, se libera la pareja.
     */
    private void checkPartner() {
        PathfinderMob other = this.partner;
        if (other == null) return;
        boolean linked = other.isAlive() && other instanceof NoxisSocial s && s.getSocialLink().isLinkedWith(this.mob);
        // Si iba hacia uno que ya dormía, ese tiene que seguir durmiendo.
        boolean sleeperAwake = this.goingToPartner && this.phase == Phase.WALK
                && other instanceof NoxisRestful r && !r.isResting();
        if (!linked || sleeperAwake) {
            this.releasePartner();
            if (this.phase == Phase.REST) this.mob.setSocialAnim(NoxisSocialAction.NONE);
        }
    }

    /** Un Noxis que ya duerme y está SOLO (sin pareja ni reserva), al que se pueda llegar. */
    private @Nullable PathfinderMob findLonelySleeper() {
        PathfinderMob best = null;
        double bestDist = Double.MAX_VALUE;
        for (PathfinderMob e : this.mob.level().getEntitiesOfClass(PathfinderMob.class,
                this.mob.getBoundingBox().inflate(SEARCH_RADIUS, 2.0D, SEARCH_RADIUS),
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

    /** Los dos quedan reservados entre sí: nadie más puede elegirlos (máximo dos por pareja). */
    private void reserve(PathfinderMob other) {
        this.partner = other;
        this.mob.getSocialLink().link(other, NoxisSocialLink.Kind.REST, true);
        ((NoxisSocial) other).getSocialLink().link(this.mob, NoxisSocialLink.Kind.REST, false);
    }

    /** Suelta la pareja sin despertar a nadie: el otro sigue con lo suyo (o durmiendo solo). */
    private void releasePartner() {
        PathfinderMob other = this.partner;
        if (other instanceof NoxisSocial s && s.getSocialLink().isLinkedWith(this.mob)) {
            s.getSocialLink().clear();
            s.setSocialAnim(NoxisSocialAction.NONE);
        }
        this.partner = null;
        this.mob.getSocialLink().clear();
    }

    private void planSpotNextTo(PathfinderMob other) {
        Vec3 right = rightOf(other.getYRot());
        Vec3 toMe = this.mob.position().subtract(other.position());
        boolean meOnItsRight = toMe.x * right.x + toMe.z * right.z >= 0.0D;
        this.spot = other.position().add(right.scale(meOnItsRight ? SIDE_DISTANCE : -SIDE_DISTANCE));
        this.partnerOnRight = !meOnItsRight;        // al acostarme mirando igual, el otro queda del otro lado
    }

    private void fallAsleep() {
        this.mob.setResting(true);
        this.slept = true;
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
        // Si todavía estaba invitando a alguien que no llegó a acostarse, retira la invitación.
        if (other instanceof NoxisSocial s && s.getSocialLink().getInviter() == this.mob) {
            s.getSocialLink().clearInvite();
        }
        this.releasePartner();
        this.mob.setResting(false);
        this.mob.setSocialAnim(NoxisSocialAction.NONE);
        if (this.slept) this.mob.onRestFinished();     // mismo tiempo de espera que el descanso de siempre
        this.spot = null;
        this.phase = Phase.DONE;
        this.goingToPartner = false;
        this.slept = false;
    }
}
