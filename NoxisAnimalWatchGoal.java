package com.noxisculture.entity.ai;

import com.noxisculture.entity.idle.NoxisPlayAnimation;
import com.noxisculture.entity.personality.NoxisPersonality;
import com.noxisculture.tag.ModTags;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Observar animales con calma (servidor). De vez en cuando, un Noxis tranquilo y sin nada que hacer
 * ve un animal cerca y se queda mirándolo un rato. Nunca lo toca, no lo lastima y el animal no cambia
 * nada de lo que hace.
 *
 * <p>Reutiliza las animaciones de curiosidad que ya existen ({@link NoxisPlayAnimation}): FOLLOW para
 * acercarse despacio o seguirlo un ratito, WATCH para observarlo (patita en el pecho), SNEAK y CROUCH
 * para el acercamiento pícaro y PLAYFUL para el manotazo juguetón al aire. Sirven con y sin capa.</p>
 *
 * <p>Según la personalidad (solo cambian las ganas y la distancia; ninguna está prohibida):</p>
 * <ul>
 *   <li><b>Curioso:</b> se acerca bastante y, a veces, lo sigue un ratito si se mueve.</li>
 *   <li><b>Cariñoso:</b> se acerca despacio y lo mira de cerca; a veces lo sigue un poquito.</li>
 *   <li><b>Travieso:</b> se acerca agachadito, espera agazapado y hace un manotazo juguetón al aire
 *   (sin tocarlo) antes de seguir mirando.</li>
 *   <li><b>Artista:</b> lo contempla de más lejos y por más tiempo.</li>
 *   <li><b>Tranquilo:</b> casi no se mueve: lo mira desde donde está (o se acerca muy poco).</li>
 * </ul>
 * <p>Si el animal se le acerca demasiado, da un pasito atrás (tímido) y sigue mirando.</p>
 *
 * <p>Varios Noxis: como mucho dos miran al mismo animal; el segundo se ubica en otro ángulo, sin
 * amontonarse. Como mucho tres observadores en la zona.</p>
 *
 * <p>IA: prioridad baja (5). No empieza si está comiendo, durmiendo, descansando, con amigos, con
 * objetos, flores o regalos, leyendo, escuchando, bailando o con cualquier otra actividad (usa
 * {@link NoxisCurious#canFollowCuriously()}), y todas esas actividades lo cortan. Se fija cada 3 a 6 s
 * (no cada tick), tiene su propia espera de 2 a 5 minutos y limpia todo al terminar.</p>
 *
 * <p>Qué animales: todos los {@link Animal} (vanilla y de mods), más la etiqueta
 * {@code noxis_culture:watchable_animals}, menos la etiqueta {@code noxis_culture:not_watchable}.</p>
 */
public class NoxisAnimalWatchGoal<T extends PathfinderMob & NoxisCurious> extends Goal {
    private static final double SEE_RADIUS = 10.0D;
    private static final double LOSE_DISTANCE = 12.0D;
    private static final double ZONE = 16.0D;
    private static final int MAX_WATCHERS_PER_ANIMAL = 2;
    private static final int MAX_WATCHERS_IN_ZONE = 3;
    private static final double TOO_CLOSE = 1.1D;          // más cerca que esto: pasito atrás
    private static final double SLOW = 0.3D;
    private static final double SNEAK_SPEED = 0.2D;
    private static final double STEP_BACK_SPEED = 0.35D;
    private static final int COOLDOWN = 2400;              // 2 min ...
    private static final int COOLDOWN_RANDOM = 3600;       // ... a 5 min
    private static final int APPROACH_TIMEOUT = 200;
    private static final int BRIEF_FOLLOW = 100;           // seguirlo un ratito: 5 a 8 s
    private static final int BRIEF_FOLLOW_RANDOM = 60;
    private static final int STEP_BACK_TICKS = 24;
    private static final int CROUCH_TICKS = 18;
    private static final int SOUND_COOLDOWN = 1200;        // como mucho un sonidito por minuto

    /** Quién mira a cada animal (servidor; claves débiles: si el animal desaparece, se borra solo). */
    private static final Map<Entity, List<PathfinderMob>> WATCHERS = new WeakHashMap<>();

    private enum Phase { APPROACH, WATCH, FOLLOW, STEP_BACK, CROUCH, PLAYFUL }

    private final T mob;
    private int nextCheckTick;
    private long nextAllowedTime = -1L;
    private long nextSoundTime;
    private @Nullable LivingEntity animal;
    private @Nullable Vec3 spot;
    private Phase phase = Phase.APPROACH;
    private int ticks;
    private int total;
    private int watchLength;
    private double distance;
    private boolean followUsed;
    private boolean mischiefUsed;
    private long nextStepBackTime;
    private boolean done;

    public NoxisAnimalWatchGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        this.nextCheckTick = mob.getRandom().nextInt(200);
    }

    // ------------------------------------------------------------------ qué animales

    /** ¿Los Noxis se paran a mirar a esta criatura? */
    public static boolean isWatchable(Entity e) {
        if (!e.isAlive() || e.is(ModTags.EntityTypes.NOT_WATCHABLE)) return false;
        return e instanceof Animal || e.is(ModTags.EntityTypes.WATCHABLE_ANIMALS);
    }

    private static List<PathfinderMob> watchersOf(Entity animal) {
        List<PathfinderMob> list = WATCHERS.get(animal);
        if (list == null) return List.of();
        list.removeIf(w -> !w.isAlive());
        return list;
    }

    private static int watchersInZone(PathfinderMob self) {
        int n = 0;
        for (Map.Entry<Entity, List<PathfinderMob>> entry : WATCHERS.entrySet()) {
            for (PathfinderMob w : entry.getValue()) {
                if (w != self && w.isAlive() && w.level() == self.level() && w.distanceToSqr(self) < ZONE * ZONE) n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------ ganas según la personalidad

    private static float wish(NoxisPersonality p) {
        return switch (p) {
            case CURIOUS -> 0.45F;
            case AFFECTIONATE -> 0.35F;
            case MISCHIEVOUS, ARTIST -> 0.3F;
            case CALM -> 0.2F;
        };
    }

    private static double watchDistance(NoxisPersonality p, RandomSource random) {
        return switch (p) {
            case CURIOUS -> 2.3D + random.nextDouble() * 0.6D;
            case AFFECTIONATE -> 2.2D + random.nextDouble() * 0.6D;
            case MISCHIEVOUS -> 2.6D + random.nextDouble() * 0.6D;
            case ARTIST -> 3.8D + random.nextDouble() * 1.0D;
            case CALM -> 4.5D + random.nextDouble() * 1.5D;
        };
    }

    private static int watchTicks(NoxisPersonality p, RandomSource random) {
        return switch (p) {
            case ARTIST -> 300 + random.nextInt(300);   // 15 a 30 s
            case CALM -> 300 + random.nextInt(200);     // 15 a 25 s
            default -> 160 + random.nextInt(240);       // 8 a 20 s
        };
    }

    private static float followChance(NoxisPersonality p) {
        return switch (p) {
            case CURIOUS -> 0.5F;
            case AFFECTIONATE -> 0.4F;
            case MISCHIEVOUS -> 0.3F;
            case ARTIST -> 0.15F;
            case CALM -> 0.0F;
        };
    }

    // ------------------------------------------------------------------ empezar

    @Override
    public boolean canUse() {
        if (this.mob.tickCount < this.nextCheckTick) return false;
        RandomSource random = this.mob.getRandom();
        this.nextCheckTick = this.mob.tickCount + 60 + random.nextInt(60);       // se fija cada 3-6 s
        long now = this.mob.level().getGameTime();
        if (this.nextAllowedTime < 0L) this.nextAllowedTime = now + 1200 + random.nextInt(3600);   // el primero: 1 a 4 min
        if (now < this.nextAllowedTime || !this.mob.canFollowCuriously()) return false;
        if (this.mob.getPlayAnim() != NoxisPlayAnimation.NONE || !this.mob.onGround() || this.mob.isInWater()) return false;
        if (random.nextFloat() >= wish(this.mob.getPersonality())) return false;
        if (watchersInZone(this.mob) >= MAX_WATCHERS_IN_ZONE) return false;
        List<LivingEntity> found = this.mob.level().getEntitiesOfClass(LivingEntity.class,
                this.mob.getBoundingBox().inflate(SEE_RADIUS, 3.0D, SEE_RADIUS),
                e -> isWatchable(e) && watchersOf(e).size() < MAX_WATCHERS_PER_ANIMAL && this.mob.hasLineOfSight(e));
        if (found.isEmpty()) return false;
        LivingEntity pick = found.get(random.nextInt(found.size()));
        this.distance = watchDistance(this.mob.getPersonality(), random);
        this.spot = this.findSpot(pick);
        if (this.spot == null) return false;
        this.animal = pick;
        return true;
    }

    /**
     * Un lugar para mirarlo: a la distancia de su personalidad, del lado del Noxis (o, si ya hay otro
     * mirando, en otro ángulo) y con espacio alrededor. Null si no hay ninguno al que pueda llegar.
     */
    private @Nullable Vec3 findSpot(LivingEntity target) {
        Vec3 a = target.position();
        Vec3 here = this.mob.position();
        // Ya está a buena distancia y con espacio: se queda donde está.
        double d2 = NoxisSpacing.horizontal2(here, a);
        List<PathfinderMob> others = watchersOf(target);
        if (others.isEmpty() && d2 <= (this.distance + 1.2D) * (this.distance + 1.2D) && d2 >= 1.6D * 1.6D) return here;
        RandomSource random = this.mob.getRandom();
        double base = Math.atan2(here.z - a.z, here.x - a.x);
        if (!others.isEmpty()) {
            PathfinderMob first = others.get(0);
            double other = Math.atan2(first.getZ() - a.z, first.getX() - a.x);
            base = other + (random.nextBoolean() ? 1.0D : -1.0D) * Math.toRadians(100.0D + random.nextInt(40));
        }
        List<PathfinderMob> near = NoxisSpacing.around(this.mob, a, this.distance + 3.0D);
        for (int i = 0; i < 6; i++) {
            double ang = base + (i == 0 ? 0.0D : (random.nextDouble() - 0.5D) * Math.toRadians(70.0D));
            Vec3 p = new Vec3(a.x + Math.cos(ang) * this.distance, a.y, a.z + Math.sin(ang) * this.distance);
            if (NoxisSpacing.room(near, p, 2.0D) < 1.3D) continue;              // sin amontonarse
            if (this.mob.getNavigation().createPath(p.x, p.y, p.z, 0) == null) continue;
            return p;
        }
        return null;
    }

    @Override
    public void start() {
        RandomSource random = this.mob.getRandom();
        LivingEntity a = this.animal;
        if (a != null) WATCHERS.computeIfAbsent(a, k -> new ArrayList<>()).add(this.mob);
        this.done = false;
        this.total = 0;
        this.followUsed = false;
        this.mischiefUsed = this.mob.getPersonality() != NoxisPersonality.MISCHIEVOUS || random.nextBoolean();
        this.watchLength = watchTicks(this.mob.getPersonality(), random);
        this.enter(Phase.APPROACH);
    }

    private void enter(Phase next) {
        this.phase = next;
        this.ticks = 0;
        boolean sneaky = this.mob.getPersonality() == NoxisPersonality.MISCHIEVOUS;
        this.mob.setPlayAnim(switch (next) {
            case APPROACH -> sneaky ? NoxisPlayAnimation.SNEAK : NoxisPlayAnimation.FOLLOW;
            case FOLLOW -> NoxisPlayAnimation.FOLLOW;
            case CROUCH -> NoxisPlayAnimation.CROUCH;
            case PLAYFUL -> NoxisPlayAnimation.PLAYFUL;
            case WATCH, STEP_BACK -> NoxisPlayAnimation.WATCH;
        });
        if (next == Phase.WATCH) this.maybeVoice(4);
        if (next == Phase.PLAYFUL) this.maybeVoice(2);
    }

    /** Un sonidito propio, raro y con espera (1 de cada {@code oneIn} veces, como mucho uno por minuto). */
    private void maybeVoice(int oneIn) {
        long now = this.mob.level().getGameTime();
        if (now < this.nextSoundTime || this.mob.getRandom().nextInt(oneIn) != 0) return;
        this.nextSoundTime = now + SOUND_COOLDOWN;
        this.mob.playSound(this.mob.getPersonality().voice(), 0.45F, 0.95F + this.mob.getRandom().nextFloat() * 0.15F);
    }

    // ------------------------------------------------------------------ mirar

    @Override
    public boolean canContinueToUse() {
        LivingEntity a = this.animal;
        if (this.done || a == null || !isWatchable(a) || a.level() != this.mob.level()) return false;
        if (this.mob.hurtTime > 0 || !this.mob.canFollowCuriously()) return false;
        return this.mob.distanceToSqr(a) < LOSE_DISTANCE * LOSE_DISTANCE;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity a = this.animal;
        Vec3 s = this.spot;
        if (a == null) {
            this.done = true;
            return;
        }
        this.ticks++;
        this.total++;
        this.mob.getLookControl().setLookAt(a, 20.0F, 20.0F);
        double d = Math.sqrt(NoxisSpacing.horizontal2(this.mob.position(), a.position()));
        long now = this.mob.level().getGameTime();
        // Tímido: si el animal se le viene encima, un pasito atrás (en cualquier fase menos el manotazo).
        if (d < TOO_CLOSE && this.phase != Phase.STEP_BACK && this.phase != Phase.PLAYFUL && now >= this.nextStepBackTime) {
            this.nextStepBackTime = now + 60;
            Vec3 away = this.mob.position().subtract(a.position()).multiply(1.0D, 0.0D, 1.0D);
            if (away.lengthSqr() < 1.0E-4D) away = new Vec3(1.0D, 0.0D, 0.0D);
            Vec3 back = this.mob.position().add(away.normalize().scale(1.4D));
            this.mob.getNavigation().moveTo(back.x, back.y, back.z, STEP_BACK_SPEED);
            this.enter(Phase.STEP_BACK);
            return;
        }
        switch (this.phase) {
            case APPROACH -> {
                double speed = this.mob.getPersonality() == NoxisPersonality.MISCHIEVOUS ? SNEAK_SPEED : SLOW;
                boolean arrived = s == null || NoxisSpacing.horizontal2(this.mob.position(), s) < 0.6D * 0.6D
                        || d <= this.distance;
                if (arrived) {
                    this.mob.getNavigation().stop();
                    this.enter(Phase.WATCH);
                } else if (this.ticks > APPROACH_TIMEOUT) {
                    this.done = true;                                   // no pudo llegar: otra vez será
                } else if (this.ticks % 10 == 1 || this.mob.getNavigation().isDone()) {
                    this.mob.getNavigation().moveTo(s.x, s.y, s.z, speed);
                }
            }
            case WATCH -> {
                this.mob.getNavigation().stop();
                if (this.total >= this.watchLength) {
                    this.done = true;
                    return;
                }
                // El travieso: una vez, agazapado y manotazo juguetón al aire (nunca lo toca).
                if (!this.mischiefUsed && this.ticks > 60) {
                    this.mischiefUsed = true;
                    this.enter(Phase.CROUCH);
                    return;
                }
                // Se alejó: a veces lo sigue un ratito (una sola vez); si no, se queda mirando hasta perderlo.
                if (d > this.distance + 2.5D && !this.followUsed) {
                    this.followUsed = true;
                    if (this.mob.getRandom().nextFloat() < followChance(this.mob.getPersonality())) {
                        this.enter(Phase.FOLLOW);
                    }
                }
            }
            case FOLLOW -> {
                if (this.ticks > BRIEF_FOLLOW + (this.mob.getId() % BRIEF_FOLLOW_RANDOM)) {
                    this.mob.getNavigation().stop();
                    this.enter(Phase.WATCH);
                } else if (d <= this.distance) {
                    this.mob.getNavigation().stop();
                } else if (this.ticks % 10 == 1) {
                    this.mob.getNavigation().moveTo(a, SLOW);
                }
            }
            case STEP_BACK -> {
                if (this.ticks >= STEP_BACK_TICKS || this.mob.getNavigation().isDone()) {
                    this.mob.getNavigation().stop();
                    this.enter(Phase.WATCH);
                }
            }
            case CROUCH -> {
                this.mob.getNavigation().stop();
                if (this.ticks >= CROUCH_TICKS) this.enter(Phase.PLAYFUL);
            }
            case PLAYFUL -> {
                this.mob.getNavigation().stop();
                if (this.ticks >= NoxisPlayAnimation.PLAYFUL_LENGTH) this.enter(Phase.WATCH);
            }
        }
    }

    // ------------------------------------------------------------------ terminar

    @Override
    public void stop() {
        LivingEntity a = this.animal;
        if (a != null) {
            List<PathfinderMob> list = WATCHERS.get(a);
            if (list != null) {
                list.remove(this.mob);
                if (list.isEmpty()) WATCHERS.remove(a);
            }
        }
        if (this.mob.getPlayAnim() != NoxisPlayAnimation.NONE) this.mob.setPlayAnim(NoxisPlayAnimation.NONE);
        this.mob.getNavigation().stop();
        this.animal = null;
        this.spot = null;
        RandomSource random = this.mob.getRandom();
        this.nextAllowedTime = this.mob.level().getGameTime() + COOLDOWN + random.nextInt(COOLDOWN_RANDOM);
        this.nextCheckTick = this.mob.tickCount + 100 + random.nextInt(100);
    }
}
