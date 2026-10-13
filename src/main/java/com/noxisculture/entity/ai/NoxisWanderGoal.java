package com.noxisculture.entity.ai;

import com.noxisculture.entity.idle.NoxisCrystals;
import com.noxisculture.entity.nature.NoxisFlowers;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Pasear por su zona (servidor). Reemplaza al paseo al azar de Minecraft: cuando un Noxis está
 * tranquilo y sin nada que hacer, después de un ratito sale a dar una vuelta corta.
 *
 * <ul>
 *   <li>Cada salida tiene de 1 a 3 tramos (los curiosos y pillos encadenan más; los tranquilos,
 *   menos). Entre tramo y tramo se detiene a observar algo: una flor, un cristal, a otro Noxis o
 *   simplemente el paisaje, y después retoma la caminata.</li>
 *   <li>Entre salida y salida descansa unos segundos (según su personalidad): así también le
 *   queda tiempo para aburrirse, mirar sus cosas y el resto de sus actividades.</li>
 *   <li>Zona: alrededor de su pecera o, si no tiene, de donde apareció ({@link NoxisRoamer#getHomeAnchor()}).
 *   Si se alejó demasiado, el próximo tramo lo trae de vuelta hacia ahí. Es provisorio hasta tener aldeas.</li>
 *   <li>Elige destinos donde no haya otros Noxis parados (no van todos al mismo lugar).</li>
 * </ul>
 *
 * <p>Prioridad baja: cualquier otra actividad lo corta (comercio, miedo, sueño, descanso,
 * regalos, flores, cristales, amigos, bailar, escuchar...). No calcula nada cada tick: elige un
 * destino al empezar cada tramo y deja que la navegación haga el resto.</p>
 */
public class NoxisWanderGoal<T extends PathfinderMob & NoxisRoamer> extends Goal {
    /** Radio de su zona (bloques, en horizontal). */
    public static final double ROAM_RADIUS = 18.0D;
    /** Si por algo terminó muy lejos (y no tiene pecera), su zona pasa a ser donde está ahora. */
    public static final double REANCHOR_DISTANCE = 64.0D;
    private static final int LEG_RANGE = 9;
    private static final int LEG_VERTICAL = 5;
    private static final double MIN_LEG = 3.0D;
    private static final double SPEED = 0.36D;
    private static final int LEG_TIMEOUT = 300;
    /** Ratito sin hacer nada antes de salir a pasear (1,5 a 3 s): no arranca apenas termina otra cosa. */
    private static final int SETTLE = 30;
    private static final int SETTLE_RANDOM = 30;
    /** Espacio que busca en el destino (bloques hasta el Noxis más cercano). */
    private static final double DEST_ROOM = 2.0D;

    private enum Phase { WALK, OBSERVE }

    private final T mob;
    private long nextOutingTime = -1L;
    private long lastPolled = Long.MIN_VALUE;
    private long freeSince;
    private int settle;
    private Phase phase = Phase.WALK;
    private int legsLeft;
    private int ticks;
    private int observeLength;
    private boolean done;
    private @Nullable Vec3 walkTarget;
    private @Nullable Vec3 lookTarget;

    public NoxisWanderGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    // ------------------------------------------------------------------ empezar

    @Override
    public boolean canUse() {
        long now = this.mob.level().getGameTime();
        // Si hace rato que no lo consultan, estaba ocupado con otra cosa: recién ahora quedó libre.
        if (now - this.lastPolled > 5L) {
            this.freeSince = now;
            this.settle = SETTLE + this.mob.getRandom().nextInt(SETTLE_RANDOM);
        }
        this.lastPolled = now;
        boolean wet = this.mob.isInWater() && !this.mob.isPassenger();
        if (!wet) {
            if (!this.mob.canRoam()) {
                this.freeSince = now;
                return false;
            }
            if (now < this.nextOutingTime || now - this.freeSince < this.settle) return false;
        }
        if ((this.mob.tickCount + this.mob.getId()) % 10 != 0) return false;   // se fija de a ratos
        Vec3 target = this.pickTarget();
        if (target == null) {
            this.nextOutingTime = now + 60L;
            return false;
        }
        this.walkTarget = target;
        RandomSource random = this.mob.getRandom();
        int extra = switch (this.mob.getPersonality()) {
            case CURIOUS, MISCHIEVOUS -> random.nextInt(3);     // 1 a 3 tramos
            case CALM -> random.nextInt(4) == 0 ? 1 : 0;       // casi siempre uno solo
            default -> random.nextInt(2);
        };
        this.legsLeft = extra;
        return true;
    }

    @Override
    public void start() {
        this.phase = Phase.WALK;
        this.ticks = 0;
        this.done = false;
        this.lookTarget = null;
        this.walkTo(this.walkTarget);
    }

    @Override
    public boolean canContinueToUse() {
        if (this.done) return false;
        return this.mob.isInWater() || this.mob.canRoam();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return false;
    }

    // ------------------------------------------------------------------ cada tick

    @Override
    public void tick() {
        this.ticks++;
        if (this.phase == Phase.WALK) {
            if (this.mob.getNavigation().isDone()) {
                if (this.legsLeft <= 0) {
                    this.done = true;                          // fin del paseo
                    return;
                }
                this.legsLeft--;
                this.startObserving();
            } else if (this.ticks > LEG_TIMEOUT) {
                this.done = true;                              // no llega: lo deja para otro momento
            }
            return;
        }
        // Detenido observando algo.
        this.mob.getNavigation().stop();
        if (this.lookTarget != null) {
            this.mob.getLookControl().setLookAt(this.lookTarget.x, this.lookTarget.y, this.lookTarget.z, 10.0F, 30.0F);
        }
        if (this.ticks >= this.observeLength) {
            Vec3 next = this.pickTarget();
            if (next == null) {
                this.done = true;
                return;
            }
            this.phase = Phase.WALK;
            this.ticks = 0;
            this.walkTo(next);
        }
    }

    private void walkTo(@Nullable Vec3 target) {
        if (target == null || !this.mob.getNavigation().moveTo(target.x, target.y, target.z, SPEED)) {
            this.done = true;
        }
    }

    /** Se detiene a mirar algo: una flor o un cristal cerca, otro Noxis, o el paisaje. */
    private void startObserving() {
        RandomSource random = this.mob.getRandom();
        this.phase = Phase.OBSERVE;
        this.ticks = 0;
        this.observeLength = 30 + random.nextInt(50);
        this.mob.getNavigation().stop();
        this.lookTarget = this.findSomethingToWatch(random);
    }

    private @Nullable Vec3 findSomethingToWatch(RandomSource random) {
        BlockPos base = this.mob.blockPosition();
        // Unos pocos bloques al azar alrededor (nada de recorrer la zona entera).
        for (int i = 0; i < 10; i++) {
            BlockPos p = base.offset(random.nextInt(11) - 5, random.nextInt(4) - 1, random.nextInt(11) - 5);
            BlockState state = this.mob.level().getBlockState(p);
            if (NoxisFlowers.isFlower(state) || NoxisCrystals.isCrystal(state)) return Vec3.atCenterOf(p);
        }
        if (random.nextInt(3) == 0) {
            List<PathfinderMob> near = NoxisSpacing.around(this.mob, this.mob.position(), 8.0D);
            if (!near.isEmpty()) return near.get(random.nextInt(near.size())).getEyePosition();
        }
        float angle = random.nextFloat() * 6.2831855F;
        return this.mob.getEyePosition().add(Math.cos(angle) * 4.0D, random.nextFloat() * 1.5D - 0.5D, Math.sin(angle) * 4.0D);
    }

    // ------------------------------------------------------------------ destinos

    private @Nullable Vec3 pickTarget() {
        Vec3 here = this.mob.position();
        if (this.mob.isInWater()) return LandRandomPos.getPos(this.mob, LEG_RANGE, LEG_VERTICAL);
        BlockPos anchor = this.mob.getHomeAnchor();
        Vec3 home = anchor == null ? null : Vec3.atBottomCenterOf(anchor);
        if (home != null && NoxisSpacing.horizontal2(here, home) > ROAM_RADIUS * ROAM_RADIUS) {
            // Se alejó de su zona: el paseo lo lleva de vuelta (si no encuentra cómo, pasea igual cerca).
            Vec3 back = LandRandomPos.getPosTowards(this.mob, LEG_RANGE, LEG_VERTICAL, home);
            if (back != null) return back;
        }
        List<PathfinderMob> others = NoxisSpacing.around(this.mob, here, LEG_RANGE + DEST_ROOM);
        Vec3 best = null;
        double bestRoom = -1.0D;
        for (int i = 0; i < 4; i++) {
            Vec3 p = LandRandomPos.getPos(this.mob, LEG_RANGE, LEG_VERTICAL);
            if (p == null || NoxisSpacing.horizontal2(p, here) < MIN_LEG * MIN_LEG) continue;
            if (home != null && NoxisSpacing.horizontal2(p, home) > ROAM_RADIUS * ROAM_RADIUS) continue;
            double room = NoxisSpacing.room(others, p, DEST_ROOM);
            if (room > bestRoom) {
                best = p;
                bestRoom = room;
                if (room >= DEST_ROOM) break;
            }
        }
        return best;
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
        this.walkTarget = null;
        this.lookTarget = null;
        RandomSource random = this.mob.getRandom();
        // Descanso hasta la próxima salida (según cómo es).
        int pause = switch (this.mob.getPersonality()) {
            case CURIOUS, MISCHIEVOUS -> 80 + random.nextInt(140);
            case AFFECTIONATE -> 100 + random.nextInt(160);
            case ARTIST -> 120 + random.nextInt(180);
            case CALM -> 160 + random.nextInt(220);
        };
        this.nextOutingTime = this.mob.level().getGameTime() + pause;
    }
}
