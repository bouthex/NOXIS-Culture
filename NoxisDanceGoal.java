package com.noxisculture.entity.ai;

import com.noxisculture.entity.hearing.NoxisJukeboxes;
import com.noxisculture.entity.idle.NoxisDanceAnimation;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Bailar cuando suena un disco (servidor). Si un Noxis tranquilo tiene cerca un tocadiscos que está
 * sonando ({@link NoxisJukeboxes}: lo anotan los oídos de los Noxis, sin buscar por el mundo), a
 * veces se acerca, se pone a su alrededor y baila al pulso de la canción mientras suene.
 *
 * <ul>
 *   <li>Cualquier disco: vanilla, de otros mods y los futuros discos Noxis (lo confirma el tocadiscos).</li>
 *   <li>Solo: elige al azar uno de los dos bailes solos, con su estilo y un pequeño desfase.</li>
 *   <li>En pareja: dos que bailan solos y cerca pueden juntarse (lo propone el de id menor y la
 *   reserva queda puesta en los dos a la vez). Máximo dos por pareja; un emparejado no acepta a
 *   otro; un tercero sigue bailando solo hasta encontrar a otro que baile solo. El que propuso
 *   decide todo y se lo pone a los dos en el mismo tick (nunca se desincronizan).</li>
 *   <li>Caídas sin daño: 5 % al empezar un baile solo (resbalón) y 2 % al formar una pareja (los
 *   dos juntos). Al formar pareja se anula la caída individual pendiente: nunca se suman.</li>
 *   <li>Como mucho 4 bailarines por tocadiscos; mínimo 15 s de baile (salvo que termine la
 *   música) y después una espera de 1 a 2 minutos. Se detiene suave si la música termina o queda
 *   lejos. No interrumpe comercio, sueño, descanso ni actividades prioritarias (lo cortan, y la
 *   pareja se libera bien: el otro sigue bailando solo, si todavía suena).</li>
 * </ul>
 */
public class NoxisDanceGoal<T extends PathfinderMob & NoxisDancer> extends Goal {
    private static final int CHECK_INTERVAL = 20;
    private static final double HEAR_RANGE = 16.0D;
    private static final double MAX_DISTANCE = 6.0D;
    private static final int MAX_DANCERS = 4;
    private static final double CROWD_ZONE = 8.0D;
    private static final double SPEED = 0.45D;
    private static final int APPROACH_TIMEOUT = 240;
    private static final int MIN_DANCE = 300;            // 15 s
    private static final int MAX_DANCE = 1800;           // 90 s ...
    private static final int MAX_DANCE_RANDOM = 1200;    // ... a 150 s
    private static final int COOLDOWN = 1200;            // 1 min ...
    private static final int COOLDOWN_RANDOM = 1200;     // ... a 2 min
    private static final int FALL_COOLDOWN = 3600;
    private static final float FALL_CHANCE = 0.05F;
    private static final float PAIR_FALL_CHANCE = 0.02F;
    private static final int LEAVE_TIMEOUT = 120;
    // Parejas
    private static final int PAIR_CHECK = 60;
    private static final double PAIR_RANGE = 4.0D;
    private static final float PAIR_CHANCE = 0.35F;
    private static final int PAIR_GATHER_TIMEOUT = 60;
    private static final int MIN_SOLO_BEFORE_PAIR = 60;
    /** Espacio cómodo entre bailarines (bloques). */
    private static final double DANCE_ROOM = 1.8D;

    private enum Phase { APPROACH, SOLO, PAIR, FALL, PAIR_FALL, LEAVE }

    private final T mob;
    private int nextCheckTick;
    private long nextAllowedTime = -1L;
    private long decidedOrigin = Long.MIN_VALUE;
    private @Nullable BlockPos decidedJukebox;
    private Phase phase = Phase.APPROACH;
    private NoxisJukeboxes.@Nullable Music music;
    private Vec3 spot = Vec3.ZERO;
    private int ticks;
    private int danceTicks;
    private int danceLength;
    private int fallAt;
    private int pathFails;
    private boolean done;
    private boolean fell;
    private byte soloStyle;

    public NoxisDanceGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.nextCheckTick = mob.getRandom().nextInt(CHECK_INTERVAL);
    }

    // ================================================================== empezar

    @Override
    public boolean canUse() {
        if (this.mob.tickCount < this.nextCheckTick) return false;
        this.nextCheckTick = this.mob.tickCount + CHECK_INTERVAL;
        if (!(this.mob.level() instanceof ServerLevel level)) return false;
        long now = level.getGameTime();
        if (now < this.nextAllowedTime || !this.mob.canDance() || this.mob.isBaby()
                || this.mob.getDancePair() != null) return false;
        NoxisJukeboxes.Music found = NoxisJukeboxes.findNear(level, this.mob.position(), HEAR_RANGE);
        if (found == null) return false;
        // Se decide una sola vez por canción (la personalidad cambia la chance, nunca la prohíbe).
        if (found.origin() == this.decidedOrigin && found.pos().equals(this.decidedJukebox)) return false;
        this.decidedOrigin = found.origin();
        this.decidedJukebox = found.pos();
        RandomSource random = this.mob.getRandom();
        if (random.nextFloat() >= this.mob.getPersonality().danceChance()) return false;
        Vec3 center = found.center();
        int dancers = level.getEntitiesOfClass(PathfinderMob.class, new AABB(center, center).inflate(CROWD_ZONE, 4.0D, CROWD_ZONE),
                e -> e != this.mob && e instanceof NoxisDancer d && d.getDanceState() != NoxisDanceAnimation.NONE).size();
        if (dancers >= MAX_DANCERS) return false;
        Vec3 place = this.pickSpot(center, random);
        if (place == null) return false;
        this.music = found;
        this.spot = place;
        return true;
    }

    /**
     * Un lugar alrededor del tocadiscos (2 a 3,5 bloques), cada uno con su ángulo. Entre unos pocos
     * candidatos se queda con el que más lejos queda de los otros Noxis (los que ya bailan y los que
     * están parados cerca): bailan cerca, pero cada uno con su espacio.
     */
    private @Nullable Vec3 pickSpot(Vec3 center, RandomSource random) {
        java.util.List<PathfinderMob> others = this.mob.level().getEntitiesOfClass(PathfinderMob.class,
                new AABB(center, center).inflate(5.0D, 3.0D, 5.0D),
                e -> e != this.mob && e instanceof NoxisListener && e.isAlive());
        Vec3 best = null;
        double bestRoom = -1.0D;
        for (int i = 0; i < 6; i++) {
            float angle = this.mob.getId() * 2.39996F + i * 1.05F + random.nextFloat() * 0.4F;
            double r = 2.0D + random.nextDouble() * 1.5D;
            Vec3 p = new Vec3(center.x + Mth.cos(angle) * r, this.mob.getY(), center.z + Mth.sin(angle) * r);
            double room = NoxisSpacing.room(others, p, DANCE_ROOM);
            if (room <= bestRoom) continue;
            if (this.mob.getNavigation().createPath(BlockPos.containing(p), 0) == null) continue;
            best = p;
            bestRoom = room;
            if (room >= DANCE_ROOM) break;                 // ya tiene todo su espacio
        }
        return best;
    }

    @Override
    public void start() {
        RandomSource random = this.mob.getRandom();
        this.phase = Phase.APPROACH;
        this.ticks = 0;
        this.danceTicks = 0;
        this.pathFails = 0;
        this.done = false;
        this.fell = false;
        this.danceLength = MAX_DANCE + random.nextInt(MAX_DANCE_RANDOM);
        // Baile solo elegido al azar, con su estilo propio y un pelito de retraso (0 a 3 ticks).
        int routine = random.nextBoolean() ? NoxisDanceAnimation.SOLO_A : NoxisDanceAnimation.SOLO_B;
        this.soloStyle = NoxisDanceAnimation.style(routine, random.nextInt(4), random.nextBoolean(), random.nextInt(4));
        // Caída individual: se sortea una sola vez, al empezar el baile solo.
        this.fallAt = random.nextFloat() < FALL_CHANCE ? 40 + random.nextInt(60) : -1;
        this.mob.getNavigation().moveTo(this.spot.x, this.spot.y, this.spot.z, SPEED);
    }

    @Override
    public boolean canContinueToUse() {
        if (this.done || this.mob.hurtTime > 0) return false;
        if (this.phase == Phase.FALL || this.phase == Phase.PAIR_FALL || this.phase == Phase.LEAVE) return this.mob.canListen();
        return this.mob.canListen() && this.mob.getListenAnim() == 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    // ================================================================== cada tick

    @Override
    public void tick() {
        this.ticks++;
        // Alguien lo eligió como pareja (o la pareja se deshizo): cambia de etapa.
        NoxisDancePair pair = this.mob.getDancePair();
        if (this.phase == Phase.SOLO && pair != null) {
            this.enterPair();
        } else if (this.phase == Phase.PAIR && pair == null) {
            this.backToSolo();
        }
        switch (this.phase) {
            case APPROACH -> this.tickApproach();
            case SOLO -> this.tickSolo();
            case PAIR -> this.tickPair();
            case FALL -> this.tickFall();
            case PAIR_FALL -> this.tickPairFall();
            case LEAVE -> this.tickLeave();
        }
    }

    private boolean musicStillPlaying() {
        if (this.music == null || !(this.mob.level() instanceof ServerLevel level)) return false;
        NoxisJukeboxes.Music now = NoxisJukeboxes.check(level, this.music.pos());
        if (now == null) return false;
        this.music = now;
        return true;
    }

    private void lookAtMusic(float speed) {
        if (this.music == null) return;
        Vec3 c = this.music.center();
        this.mob.getLookControl().setLookAt(c.x, c.y + 0.3D, c.z, speed, 30.0F);
    }

    private void tickApproach() {
        if (this.ticks % 20 == 0 && !this.musicStillPlaying()) {
            this.done = true;
            return;
        }
        double dx = this.spot.x - this.mob.getX();
        double dz = this.spot.z - this.mob.getZ();
        boolean near = dx * dx + dz * dz < 0.6D * 0.6D;
        if (near || (this.mob.getNavigation().isDone() && this.distanceToMusic2() < 4.0D * 4.0D)) {
            this.mob.getNavigation().stop();
            this.phase = Phase.SOLO;
            this.ticks = 0;
            this.sendDance(NoxisDanceAnimation.DANCE, this.soloStyle);
            return;
        }
        if (this.ticks > APPROACH_TIMEOUT || this.pathFails >= 3) {
            this.done = true;
            return;
        }
        if (this.mob.getNavigation().isDone() && !this.mob.getNavigation().moveTo(this.spot.x, this.spot.y, this.spot.z, SPEED)) {
            this.pathFails++;
        }
    }

    private void sendDance(byte state, byte style) {
        NoxisJukeboxes.Music m = this.music;
        int origin = m == null ? 0 : (int) m.origin();
        byte beat = (byte) Mth.clamp(m == null ? 10 : m.beatTicks(), 4, 40);
        this.mob.setDance(state, style, origin, beat);
    }

    /** ¿Hay que dejar de bailar? (la música terminó, quedó lejos, o ya bailó bastante). */
    private boolean shouldStop() {
        int beatBefore = this.music == null ? 10 : this.music.beatTicks();
        long originBefore = this.music == null ? 0L : this.music.origin();
        boolean playing = this.musicStillPlaying();
        if (!playing || this.distanceToMusic2() > MAX_DISTANCE * MAX_DISTANCE
                || (this.danceTicks > MIN_DANCE && this.danceTicks > this.danceLength)) return true;
        if (this.music != null && (this.music.origin() != originBefore || this.music.beatTicks() != beatBefore)) {
            this.resendCurrent();                         // otro disco: se acomoda al pulso nuevo
        }
        return false;
    }

    private void resendCurrent() {
        NoxisDancePair pair = this.mob.getDancePair();
        if (this.phase == Phase.PAIR && pair != null && pair.started && pair.isLeader(this.mob)) {
            this.sendPair(pair, NoxisDanceAnimation.DANCE);
        } else if (this.phase == Phase.SOLO) {
            this.sendDance(NoxisDanceAnimation.DANCE, this.soloStyle);
        }
    }

    // ---------------------------------------------------------------- solo

    private void tickSolo() {
        this.danceTicks++;
        this.mob.getNavigation().stop();
        this.lookAtMusic(10.0F);
        if (this.fallAt > 0 && this.ticks == this.fallAt) {
            this.phase = Phase.FALL;
            this.ticks = 0;
            this.fell = true;
            this.sendDance(NoxisDanceAnimation.FALL, this.soloStyle);
            return;
        }
        if (this.ticks % 10 == 0 && this.shouldStop()) {
            this.done = true;
            return;
        }
        if (this.ticks >= MIN_SOLO_BEFORE_PAIR && (this.ticks + this.mob.getId()) % PAIR_CHECK == 0) {
            this.tryFormPair();
        }
    }

    /**
     * Proponer bailar juntos: solo lo propone el de id menor (así nunca dos eligen al mismo a la vez),
     * y solo a otro que esté bailando SOLO, con el mismo tocadiscos, sin pareja y sin caerse.
     */
    private void tryFormPair() {
        if (this.mob.getDancePair() != null || this.music == null) return;
        RandomSource random = this.mob.getRandom();
        PathfinderMob best = null;
        double bestD = PAIR_RANGE * PAIR_RANGE;
        for (PathfinderMob e : this.mob.level().getEntitiesOfClass(PathfinderMob.class,
                this.mob.getBoundingBox().inflate(PAIR_RANGE, 2.0D, PAIR_RANGE))) {
            if (e == this.mob || e.getId() < this.mob.getId() || !(e instanceof NoxisDancer d)) continue;
            if (d.getDancePair() != null || d.getDanceState() != NoxisDanceAnimation.DANCE
                    || NoxisDanceAnimation.isPair(d.getDanceStyleByte()) || !d.canDance()) continue;
            double dist = e.distanceToSqr(this.mob);
            if (dist < bestD) {
                best = e;
                bestD = dist;
            }
        }
        if (best == null) return;
        NoxisDancer other = (NoxisDancer) best;
        float chance = PAIR_CHANCE * 0.5F * (this.mob.getPersonality().pairDanceWeight() + other.getPersonality().pairDanceWeight());
        if (random.nextFloat() >= chance) return;
        int routine = random.nextBoolean() ? NoxisDanceAnimation.PAIR_RING : NoxisDanceAnimation.PAIR_HOPS;
        // Caída en pareja: se sortea una sola vez, al formar la pareja (2 %).
        int pairFall = random.nextFloat() < PAIR_FALL_CHANCE ? 80 + random.nextInt(120) : -1;
        NoxisDancePair pair = new NoxisDancePair(this.mob, best, this.music.pos(), routine, pairFall);
        // La reserva queda puesta en los dos en el mismo tick.
        this.mob.setDancePair(pair);
        other.setDancePair(pair);
        this.enterPair();
    }

    // ---------------------------------------------------------------- pareja

    private void enterPair() {
        this.phase = Phase.PAIR;
        this.ticks = 0;
        this.fallAt = -1;                 // la caída individual pendiente se anula: no se suma a la de pareja
    }

    /** La pareja se deshizo (el otro tuvo que irse): sigue bailando solo, si todavía suena. */
    private void backToSolo() {
        this.phase = Phase.SOLO;
        this.ticks = 0;
        if (this.musicStillPlaying()) {
            this.sendDance(NoxisDanceAnimation.DANCE, this.soloStyle);
        } else {
            this.done = true;
        }
    }

    private void facePartner(PathfinderMob other) {
        double dx = other.getX() - this.mob.getX();
        double dz = other.getZ() - this.mob.getZ();
        if (dx * dx + dz * dz < 1.0E-4D) return;
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        this.mob.setYRot(yaw);
        this.mob.yBodyRot = yaw;
        this.mob.yHeadRot = yaw;
        this.mob.setXRot(0.0F);
    }

    private void sendPair(NoxisDancePair pair, byte state) {
        byte leaderStyle = NoxisDanceAnimation.style(pair.routine, 0, false, 0);
        byte partnerStyle = NoxisDanceAnimation.style(pair.routine, 0, true, 0);
        NoxisJukeboxes.Music m = this.music;
        int origin = m == null ? 0 : (int) m.origin();
        byte beat = (byte) Mth.clamp(m == null ? 10 : m.beatTicks(), 4, 40);
        ((NoxisDancer) pair.leader).setDance(state, leaderStyle, origin, beat);
        ((NoxisDancer) pair.partner).setDance(state, partnerStyle, origin, beat);
    }

    private void tickPair() {
        NoxisDancePair pair = this.mob.getDancePair();
        if (pair == null) return;
        this.danceTicks++;
        PathfinderMob other = pair.other(this.mob);
        if (pair.finished) {
            this.done = true;
            return;
        }
        if (pair.falling) {
            this.phase = Phase.PAIR_FALL;
            this.ticks = 0;
            this.fell = true;
            return;
        }
        boolean leader = pair.isLeader(this.mob);
        if (!pair.started) {
            // Se acomodan frente a frente (pasitos cortos) mientras siguen bailando solos.
            Vec3 target = pair.spotFor(this.mob);
            double dx = target.x - this.mob.getX();
            double dz = target.z - this.mob.getZ();
            if (dx * dx + dz * dz > 0.15D * 0.15D) {
                this.mob.getMoveControl().setWantedPosition(target.x, target.y, target.z, 0.35D);
            } else {
                this.mob.getNavigation().stop();
            }
            this.facePartner(other);
            if (leader) {
                Vec3 ot = pair.spotFor(other);
                boolean bothThere = dx * dx + dz * dz < 0.25D * 0.25D && other.distanceToSqr(ot.x, other.getY(), ot.z) < 0.25D * 0.25D;
                if (bothThere || this.ticks > PAIR_GATHER_TIMEOUT) {
                    pair.started = true;
                    pair.ticks = 0;
                    this.sendPair(pair, NoxisDanceAnimation.DANCE);  // los dos, en el mismo tick
                }
            }
            return;
        }
        // Bailando juntos: quietos en su lugar, mirándose.
        this.mob.getNavigation().stop();
        this.facePartner(other);
        if (!leader) return;
        pair.ticks++;
        if (pair.fallAt > 0 && pair.ticks == pair.fallAt) {
            pair.falling = true;
            this.sendPair(pair, NoxisDanceAnimation.PAIR_FALL);     // una sola secuencia, los dos a la vez
            this.phase = Phase.PAIR_FALL;
            this.ticks = 0;
            this.fell = true;
            return;
        }
        if (pair.ticks % 10 == 0 && this.shouldStop()) {
            pair.finished = true;          // terminó la música (o ya bailaron bastante): los dos terminan
            this.done = true;
        }
    }

    // ---------------------------------------------------------------- caídas

    private void tickFall() {
        this.mob.getNavigation().stop();
        if (this.ticks == NoxisDanceAnimation.FALL_LAND_TICK) {
            this.mob.playSound(SoundEvents.WOOL_FALL, 0.5F, 1.3F);          // "puf" suavecito, sin daño
        }
        if (this.ticks >= NoxisDanceAnimation.FALL_LENGTH) {
            // Como si nada: deja de bailar y se va caminando a otro lado.
            this.mob.setDance(NoxisDanceAnimation.NONE, this.soloStyle, 0, (byte) 10);
            this.phase = Phase.LEAVE;
            this.ticks = 0;
            Vec3 away = this.music == null ? null : LandRandomPos.getPosAway(this.mob, 8, 4, this.music.center());
            if (away == null || !this.mob.getNavigation().moveTo(away.x, away.y, away.z, 0.4D)) this.done = true;
        }
    }

    private void tickPairFall() {
        this.mob.getNavigation().stop();
        NoxisDancePair pair = this.mob.getDancePair();
        if (pair != null) this.facePartner(pair.other(this.mob));
        if (this.ticks == NoxisDanceAnimation.PAIR_FALL_LAND_TICK) {
            this.mob.playSound(SoundEvents.WOOL_FALL, 0.45F, 1.2F + this.mob.getRandom().nextFloat() * 0.2F);
        }
        if (this.ticks >= NoxisDanceAnimation.PAIR_FALL_LENGTH) {
            // Se levantaron y se recuperaron: los dos vuelven a su vida normal.
            if (pair != null) pair.finished = true;
            this.done = true;
        }
    }

    private void tickLeave() {
        if (this.ticks > LEAVE_TIMEOUT || this.mob.getNavigation().isDone()) this.done = true;
    }

    private double distanceToMusic2() {
        if (this.music == null) return Double.MAX_VALUE;
        Vec3 c = this.music.center();
        double dx = c.x - this.mob.getX();
        double dz = c.z - this.mob.getZ();
        return dx * dx + dz * dz;
    }

    @Override
    public void stop() {
        NoxisDancePair pair = this.mob.getDancePair();
        if (pair != null) {
            // Si la pareja terminó junta (música o caída), los dos terminan; si solo este tuvo que
            // irse, el otro queda libre y sigue bailando solo.
            pair.dissolve();
        }
        this.mob.setDance(NoxisDanceAnimation.NONE, this.soloStyle, 0, (byte) 10);
        this.mob.getNavigation().stop();
        long now = this.mob.level().getGameTime();
        this.nextAllowedTime = now + (this.fell ? FALL_COOLDOWN : COOLDOWN) + this.mob.getRandom().nextInt(COOLDOWN_RANDOM);
        this.music = null;
    }
}
