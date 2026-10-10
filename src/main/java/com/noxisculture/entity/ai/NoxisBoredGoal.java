package com.noxisculture.entity.ai;

import com.noxisculture.entity.idle.NoxisBoredAnimation;
import com.noxisculture.sound.ModSounds;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Aburrimiento (servidor): cuando un Noxis lleva un rato quieto, tranquilo y con las manos
 * libres, de vez en cuando hace uno de sus tres gestos de aburrimiento, elegido al azar entre
 * los que puede hacer en ese momento. La animación la arma el cliente; acá se decide cuál, se
 * lo mantiene quieto mientras dura y se ponen los soniditos.
 *
 * <ul>
 *   <li>Espera propia de 30 a 75 s entre un gesto y otro, y nunca repite el último si hay otro.</li>
 *   <li>Si cerca ya hay Noxis aburridos, elige un gesto distinto al de ellos; si ya hay dos, espera.</li>
 *   <li>Cualquier cosa importante (comercio, golpe, miedo, sueño, regalo...) lo corta en el acto:
 *   el cliente vuelve suave a la postura normal.</li>
 * </ul>
 */
public class NoxisBoredGoal<T extends PathfinderMob & NoxisBored> extends Goal {
    /** Rato quieto sin hacer nada antes de aburrirse (5 s). */
    private static final int MIN_IDLE_TICKS = 100;
    /** Una vez aburrido, probabilidad por consulta (cada 2 ticks): ~3 s en promedio. */
    private static final int CHANCE = 30;
    private static final int COOLDOWN = 600;           // 30 s ...
    private static final int COOLDOWN_RANDOM = 900;    // ... a 75 s
    /** Si no pudo elegir (vecinos aburridos), vuelve a probar en 5 s. */
    private static final int RETRY = 100;
    private static final double NEARBY_RADIUS = 10.0D;
    private static final int MAX_NEARBY_BORED = 2;

    private static final int WEIGHT_TAP = 4;
    private static final int WEIGHT_HAT = 3;
    private static final int WEIGHT_PAWS = 3;

    private final T mob;
    private long nextAllowedTime = -1L;
    private byte lastType = NoxisBoredAnimation.NONE;
    private byte type = NoxisBoredAnimation.NONE;
    private boolean mirrored;
    private int t;
    private int length;

    public NoxisBoredGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        long now = this.mob.level().getGameTime();
        if (this.nextAllowedTime < 0L) {
            // El primero llega pronto (10-40 s), y cada Noxis a su tiempo.
            this.nextAllowedTime = now + 200 + this.mob.getRandom().nextInt(600);
        }
        if (now < this.nextAllowedTime) return false;
        if (this.mob.getIdleTicks() < MIN_IDLE_TICKS || !this.mob.canBeBored()) return false;
        if (this.mob.getRandom().nextInt(CHANCE) != 0) return false;
        this.type = this.choose();
        if (this.type == NoxisBoredAnimation.NONE) {
            this.nextAllowedTime = now + RETRY;
            return false;
        }
        return true;
    }

    /** Elige un gesto posible ahora, distinto del último y de los que hacen los vecinos. */
    private byte choose() {
        List<Byte> nearby = new ArrayList<>();
        AABB area = this.mob.getBoundingBox().inflate(NEARBY_RADIUS, 4.0D, NEARBY_RADIUS);
        for (PathfinderMob other : this.mob.level().getEntitiesOfClass(PathfinderMob.class, area,
                e -> e != this.mob && e instanceof NoxisBored b && b.getBoredAnim() != NoxisBoredAnimation.NONE)) {
            nearby.add(NoxisBoredAnimation.typeOf(((NoxisBored) other).getBoredAnim()));
        }
        if (nearby.size() >= MAX_NEARBY_BORED) return NoxisBoredAnimation.NONE;   // ya hay bastantes aburridos

        List<Byte> options = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        add(options, weights, NoxisBoredAnimation.TAP, WEIGHT_TAP, nearby);
        if (this.mob.hasHat()) add(options, weights, NoxisBoredAnimation.HAT, WEIGHT_HAT, nearby);
        add(options, weights, NoxisBoredAnimation.PAWS, WEIGHT_PAWS, nearby);
        if (options.size() > 1) {
            int i = options.indexOf(this.lastType);
            if (i >= 0) {
                options.remove(i);
                weights.remove(i);
            }
        }
        if (options.isEmpty()) return NoxisBoredAnimation.NONE;
        int total = 0;
        for (int w : weights) total += w;
        int roll = this.mob.getRandom().nextInt(total);
        for (int i = 0; i < options.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) return options.get(i);
        }
        return options.get(0);
    }

    private static void add(List<Byte> options, List<Integer> weights, byte type, int weight, List<Byte> nearby) {
        if (!nearby.contains(type)) {
            options.add(type);
            weights.add(weight);
        }
    }

    @Override
    public void start() {
        this.t = 0;
        this.length = NoxisBoredAnimation.length(this.type);
        this.mirrored = this.mob.getRandom().nextBoolean();
        this.mob.getNavigation().stop();
        this.mob.setBoredAnim(NoxisBoredAnimation.encode(this.type, this.mirrored));
    }

    @Override
    public boolean canContinueToUse() {
        return this.t < this.length && this.mob.canBeBored() && this.mob.hurtTime == 0
                && (this.type != NoxisBoredAnimation.HAT || this.mob.hasHat());
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.t++;
        this.mob.getNavigation().stop();
        switch (this.type) {
            case NoxisBoredAnimation.TAP -> {
                for (int hit : NoxisBoredAnimation.TAP_HITS) {
                    if (this.t == hit) this.tapGround();
                }
            }
            case NoxisBoredAnimation.HAT -> {
                if (this.t == NoxisBoredAnimation.HAT_GRIP_TICK) {
                    this.mob.playSound(SoundEvents.WOOL_STEP, 0.35F, 1.45F);
                } else if (this.t == NoxisBoredAnimation.HAT_PROUD_TICK) {
                    this.mob.playSound(ModSounds.NOXIS_HAPPY, 0.45F, 1.25F);
                }
            }
            case NoxisBoredAnimation.PAWS -> {
                if (this.t == NoxisBoredAnimation.PAWS_PLAY_TICK) {
                    this.mob.playSound(ModSounds.NOXIS_AMBIENT, 0.4F, 1.35F);
                }
            }
            default -> { }
        }
    }

    /** Golpecito de la patita: el sonido de pisada del bloque de abajo y un poquito de polvo. */
    private void tapGround() {
        BlockPos below = this.mob.getOnPos();
        BlockState ground = this.mob.level().getBlockState(below);
        if (ground.isAir()) return;
        SoundType sound = ground.getSoundType();
        this.mob.playSound(sound.getStepSound(), sound.getVolume() * 0.35F, sound.getPitch() * 1.2F);
        if (this.mob.level() instanceof ServerLevel server && ground.getRenderShape() != RenderShape.INVISIBLE) {
            float yaw = this.mob.yBodyRot * Mth.DEG_TO_RAD;
            double side = this.mirrored ? -1.0D : 1.0D;           // pata derecha (o izquierda)
            double rx = -Mth.cos(yaw) * side * 0.1D;
            double rz = -Mth.sin(yaw) * side * 0.1D;
            double fx = -Mth.sin(yaw) * 0.12D;
            double fz = Mth.cos(yaw) * 0.12D;
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                    this.mob.getX() + rx + fx, this.mob.getY() + 0.05D, this.mob.getZ() + rz + fz,
                    3, 0.04D, 0.0D, 0.04D, 0.02D);
        }
    }

    @Override
    public void stop() {
        this.mob.setBoredAnim(NoxisBoredAnimation.NONE);
        this.mob.resetIdleTicks();
        this.lastType = this.type;
        this.type = NoxisBoredAnimation.NONE;
        this.nextAllowedTime = this.mob.level().getGameTime() + COOLDOWN + this.mob.getRandom().nextInt(COOLDOWN_RANDOM);
    }
}
