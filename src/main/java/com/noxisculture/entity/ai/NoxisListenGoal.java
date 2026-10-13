package com.noxisculture.entity.ai;

import com.noxisculture.entity.hearing.NoxisHeardSound;
import com.noxisculture.entity.hearing.NoxisSounds;
import com.noxisculture.entity.idle.NoxisListenAnimation;
import com.noxisculture.entity.personality.NoxisPersonality;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * Reaccionar a un sonido del mundo (servidor). Cuando un Noxis tranquilo oye algo interesante
 * cerca (campana, bloque musical, tocadiscos, cuerno, una criatura), a veces:
 *
 * <ol>
 *   <li>para las orejitas hacia el sonido (el cliente las orienta con la dirección que manda acá);</li>
 *   <li>gira la cabeza para encontrarlo y se queda escuchando un momento;</li>
 *   <li>después vuelve a lo suyo o, si le da curiosidad, camina unos pasos a investigar y mira
 *   qué era.</li>
 * </ol>
 *
 * <p>Nunca reacciona a cada sonido: solo a los que valen la pena ({@link NoxisSounds}), con una
 * chance según el sonido y su personalidad, y con una espera de 30 a 60 s entre reacciones. Como
 * mucho dos Noxis cercanos reaccionan a la vez, y cada uno arranca con un desfase propio (nunca
 * todos sincronizados). No interrumpe nada: comerciar, dormir, descansar, usar objetos y el resto
 * de las actividades lo cortan o le impiden empezar.</p>
 *
 * <p>Personalidad: todos pueden escuchar e investigar; los curiosos investigan mucho más seguido y
 * los tranquilos casi siempre se quedan escuchando.</p>
 */
public class NoxisListenGoal<T extends PathfinderMob & NoxisListener> extends Goal {
    private static final int CHECK_INTERVAL = 4;
    /** Un sonido "viejo" (no lo atendió enseguida) ya no interesa. */
    private static final int MAX_AGE = 30;
    private static final int COOLDOWN = 600;            // 30 s ...
    private static final int COOLDOWN_RANDOM = 600;     // ... a 60 s
    /** Si decidió no hacerle caso, un rato corto sin volver a considerarlo. */
    private static final int IGNORE_COOLDOWN = 100;
    private static final double CROWD_ZONE = 12.0D;
    private static final int MAX_CROWD = 2;
    /** Desde qué momento la cabeza va a buscar el sonido (primero se mueven las orejitas). */
    private static final int LOOK_FROM = 8;
    /** En qué momento de la escucha decide si va a investigar. */
    private static final int DECIDE_AT = 48;
    private static final double MIN_INVESTIGATE = 3.5D;
    private static final double MAX_INVESTIGATE = 14.0D;
    /** Se detiene a esta distancia (horizontal) de lo que sonó: mira sin pegarse. */
    private static final double ARRIVE = 2.2D;
    private static final double SPEED = 0.4D;
    private static final int WALK_TIMEOUT = 200;
    private static final int MAX_PATH_FAILS = 3;

    private enum Phase { LISTEN, WALK, PEEK }

    private final T mob;
    private int nextCheckTick;
    private long nextAllowedTime = -1L;
    private Phase phase = Phase.LISTEN;
    private Vec3 target = Vec3.ZERO;
    private byte kind;
    private int sourceId = -1;
    private int ticks;
    private int pathFails;
    private boolean done;
    private byte lastDir;

    public NoxisListenGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.nextCheckTick = mob.getRandom().nextInt(CHECK_INTERVAL);
    }

    @Override
    public boolean canUse() {
        if (this.mob.tickCount < this.nextCheckTick) return false;
        this.nextCheckTick = this.mob.tickCount + CHECK_INTERVAL;
        NoxisHeardSound heard = this.mob.getHeardSound();
        if (heard == null) return false;
        long now = this.mob.level().getGameTime();
        long age = now - heard.time();
        // Desfase propio (0 a 8 ticks): si varios oyen lo mismo, no reaccionan todos a la vez.
        int delay = Math.floorMod(this.mob.getId() * 7, 9);
        if (age < delay) return false;
        this.mob.clearHeardSound();
        if (age > MAX_AGE || now < this.nextAllowedTime || !this.mob.canListen()) return false;
        RandomSource random = this.mob.getRandom();
        float chance = NoxisSounds.interest(heard.kind()) * this.mob.getPersonality().listenWeight();
        if (random.nextFloat() >= chance) {
            this.nextAllowedTime = now + IGNORE_COOLDOWN;
            return false;
        }
        Vec3 eye = this.mob.getEyePosition();
        if (heard.pos().distanceToSqr(eye) > 20.0D * 20.0D) return false;
        // Como mucho dos Noxis cercanos reaccionando al mismo tiempo.
        int busy = this.mob.level().getEntitiesOfClass(PathfinderMob.class,
                this.mob.getBoundingBox().inflate(CROWD_ZONE, 4.0D, CROWD_ZONE),
                e -> e != this.mob && e instanceof NoxisListener l && l.getListenAnim() != NoxisListenAnimation.NONE).size();
        if (busy >= MAX_CROWD) return false;
        this.target = heard.pos();
        this.kind = heard.kind();
        this.sourceId = heard.sourceId();
        return true;
    }

    @Override
    public void start() {
        this.phase = Phase.LISTEN;
        this.ticks = 0;
        this.pathFails = 0;
        this.done = false;
        this.lastDir = 0;
        this.mob.getNavigation().stop();
        this.mob.setListenAnim(NoxisListenAnimation.PERK);
        this.updateDir();
    }

    @Override
    public boolean canContinueToUse() {
        return !this.done && this.mob.canListen() && this.mob.hurtTime == 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.ticks++;
        this.followSource();
        switch (this.phase) {
            case LISTEN -> this.tickListen();
            case WALK -> this.tickWalk();
            case PEEK -> this.tickPeek();
        }
        this.updateDir();
    }

    /** Si fue una criatura y se movió, el sonido "viene" de donde está ahora. */
    private void followSource() {
        if (this.sourceId < 0) return;
        Entity e = this.mob.level().getEntity(this.sourceId);
        if (e != null && e.isAlive() && e.distanceToSqr(this.mob) < 24.0D * 24.0D) {
            this.target = e.getEyePosition();
        }
    }

    private void tickListen() {
        this.mob.getNavigation().stop();
        // Primero se mueven las orejitas; recién después la cabeza va (despacito) a buscar el sonido.
        if (this.ticks >= LOOK_FROM) {
            this.mob.getLookControl().setLookAt(this.target.x, this.target.y, this.target.z, 12.0F, 30.0F);
        }
        if (this.ticks < DECIDE_AT) return;
        if (this.ticks == DECIDE_AT && this.wantsToInvestigate()) {
            this.startWalk();
            return;
        }
        if (this.ticks >= NoxisListenAnimation.PERK_LENGTH) this.done = true;   // ajá: vuelve a lo suyo
    }

    private boolean wantsToInvestigate() {
        // Un disco: no va a "investigarlo"; si quiere, va a bailar (NoxisDanceGoal).
        if (this.kind == NoxisSounds.RECORD) return false;
        double d2 = this.horizontal2(this.target);
        if (d2 < MIN_INVESTIGATE * MIN_INVESTIGATE || d2 > MAX_INVESTIGATE * MAX_INVESTIGATE) return false;
        NoxisPersonality personality = this.mob.getPersonality();
        float chance = personality.investigateChance(NoxisSounds.isMusic(this.kind));
        if (this.kind == NoxisSounds.CREATURE) chance *= 0.7F;
        if (this.mob.getRandom().nextFloat() >= chance) return false;
        Path path = this.mob.getNavigation().createPath(BlockPos.containing(this.target), 1);
        return path != null;
    }

    private void startWalk() {
        this.phase = Phase.WALK;
        this.ticks = 0;
        this.mob.setListenAnim(NoxisListenAnimation.INVESTIGATE);
        this.mob.getNavigation().moveTo(this.target.x, this.target.y, this.target.z, SPEED);
    }

    private void tickWalk() {
        this.mob.getLookControl().setLookAt(this.target.x, this.target.y, this.target.z, 20.0F, 30.0F);
        if (this.horizontal2(this.target) < ARRIVE * ARRIVE && Math.abs(this.target.y - this.mob.getY()) < 3.0D) {
            this.mob.getNavigation().stop();
            this.phase = Phase.PEEK;
            this.ticks = 0;
            this.mob.setListenAnim(NoxisListenAnimation.PEEK);
            return;
        }
        if (this.ticks > WALK_TIMEOUT || this.pathFails >= MAX_PATH_FAILS) {
            this.done = true;                              // no llega: se olvida tranquilo
            return;
        }
        if (this.ticks % 20 == 0 || this.mob.getNavigation().isDone()) {
            if (!this.mob.getNavigation().moveTo(this.target.x, this.target.y, this.target.z, SPEED)) this.pathFails++;
        }
    }

    private void tickPeek() {
        this.mob.getNavigation().stop();
        this.mob.getLookControl().setLookAt(this.target.x, this.target.y, this.target.z, 20.0F, 40.0F);
        if (this.ticks >= NoxisListenAnimation.PEEK_LENGTH) this.done = true;
    }

    private double horizontal2(Vec3 p) {
        double dx = p.x - this.mob.getX();
        double dz = p.z - this.mob.getZ();
        return dx * dx + dz * dz;
    }

    /** Hacia dónde está el sonido respecto de la cabeza (lo usan las orejitas en el cliente). */
    private void updateDir() {
        double dx = this.target.x - this.mob.getX();
        double dz = this.target.z - this.mob.getZ();
        if (dx * dx + dz * dz < 1.0E-4D) return;
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float rel = Mth.wrapDegrees(yaw - this.mob.getYHeadRot());
        byte dir = (byte) Math.round(Mth.clamp(rel / 90.0F, -1.0F, 1.0F) * 100.0F);
        if (Math.abs(dir - this.lastDir) >= 3) {
            this.lastDir = dir;
            this.mob.setListenDir(dir);
        }
    }

    @Override
    public void stop() {
        this.mob.setListenAnim(NoxisListenAnimation.NONE);
        this.mob.setListenDir((byte) 0);
        this.mob.getNavigation().stop();
        this.sourceId = -1;
        this.nextAllowedTime = this.mob.level().getGameTime() + COOLDOWN + this.mob.getRandom().nextInt(COOLDOWN_RANDOM);
    }
}
