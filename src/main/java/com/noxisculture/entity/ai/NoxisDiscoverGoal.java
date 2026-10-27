package com.noxisculture.entity.ai;

import com.noxisculture.config.NoxisConfig;
import com.noxisculture.entity.discovery.NoxisDiscoveries;
import com.noxisculture.entity.idle.NoxisAmbientAnimation;
import com.noxisculture.entity.personality.NoxisPersonality;
import com.noxisculture.tag.ModTags;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Descubrir cosas interesantes (servidor). La curiosidad nace de lo que hay en el mundo: cada tanto
 * un Noxis echa un vistazo a su alrededor (unos pocos lugares al azar, nunca todo el entorno) y, si
 * ve algo de la etiqueta {@code noxis_culture:discoverable} que todavía no conoce (una flor, un
 * cristal, una mena, una luz, una rareza), se detiene a mirarlo. También nota cuando llega por primera
 * vez a un bioma nuevo y mira a su alrededor.
 *
 * <ul>
 *   <li>Recuerdos compactos de cada Noxis ({@link NoxisDiscoveries}): "lo vi" (de lejos), "lo conozco"
 *   (se acercó a mirarlo bien) y "lo tuve" (estuvo en su inventario). Lo que ya conoce no lo sorprende
 *   cada vez: solo de vez en cuando le echa otra mirada corta.</li>
 *   <li>Reacción: se frena, gira la cabeza, a veces un respingo de sorpresa la primera vez; se acerca
 *   unos pasos y lo mira de cerca (agachado si es chiquito o está abajo, inclinado si está a su altura).</li>
 *   <li>Personalidad (probabilidades, no reglas): los curiosos se detienen más y se acercan más; los
 *   artistas, sobre todo por flores y cristales; los tranquilos miran un momento desde donde están.</li>
 *   <li>No recoge todo lo que encuentra: los objetos del suelo ya los atiende otra actividad, con sus
 *   reglas (no roba ni junta cosas que no le interesan).</li>
 *   <li>Sin aglomerarse: un mismo lugar lo mira un solo Noxis a la vez. Revisa cada 6 a 12 s, espera
 *   propia de 1 a 3 min (escalada por la opción "Descubrimientos"), prioridad baja.</li>
 * </ul>
 */
public class NoxisDiscoverGoal<T extends PathfinderMob & NoxisAmbient> extends Goal {
    private static final int RADIUS = 7;
    private static final int SAMPLES = 20;
    private static final int COOLDOWN = 1200;
    private static final int COOLDOWN_RANDOM = 2400;
    private static final double SPEED = 0.35D;
    /** Lugares que alguien está mirando ahora (para no amontonarse): posición → hasta cuándo. */
    private static final Map<BlockPos, Long> CLAIMED = new HashMap<>();

    private enum Phase { NOTICE, APPROACH, INSPECT, SCAN }

    private final T mob;
    private int nextCheckTick;
    private long nextAllowedTime = -1L;
    private int nextBiomeCheck;
    private @Nullable BlockPos target;
    private int key;
    private boolean firstTime;
    private boolean approach;
    private Phase phase = Phase.NOTICE;
    private int ticks;
    private int inspectLength;

    public NoxisDiscoverGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        this.nextCheckTick = mob.getRandom().nextInt(200);
    }

    private static float interest(NoxisPersonality p, BlockState state) {
        boolean pretty = state.is(net.minecraft.tags.BlockTags.FLOWERS)
                || state.is(net.minecraft.world.level.block.Blocks.AMETHYST_CLUSTER)
                || state.is(net.minecraft.world.level.block.Blocks.LARGE_AMETHYST_BUD);
        return switch (p) {
            case CURIOUS -> 0.75F;
            case ARTIST -> pretty ? 0.8F : 0.4F;
            case MISCHIEVOUS -> 0.45F;
            case AFFECTIONATE -> pretty ? 0.5F : 0.35F;
            case CALM -> 0.3F;
        };
    }

    @Override
    public boolean canUse() {
        if (this.mob.tickCount < this.nextCheckTick) return false;
        RandomSource random = this.mob.getRandom();
        this.nextCheckTick = this.mob.tickCount + 120 + random.nextInt(120);
        int freq = NoxisConfig.get().discoveries;
        if (!NoxisConfig.enabled(freq)) return false;
        long now = this.mob.level().getGameTime();
        if (this.nextAllowedTime < 0L) this.nextAllowedTime = now + NoxisConfig.scaleCooldown(600 + random.nextInt(1800), freq);
        if (now < this.nextAllowedTime || !this.mob.canDoAmbient() || !this.mob.onGround() || this.mob.isInWater()) return false;
        CLAIMED.values().removeIf(t -> t < now);
        Level level = this.mob.level();
        // ¿Un bioma nuevo? (pocas veces: es un cálculo liviano, pero no hace falta seguido)
        if (this.mob.tickCount >= this.nextBiomeCheck) {
            this.nextBiomeCheck = this.mob.tickCount + 400;
            int biome = NoxisDiscoveries.key(NoxisDiscoveries.BIOME, level.getBiome(this.mob.blockPosition()).getRegisteredName().hashCode());
            if (!this.mob.getDiscoveries().has(biome, NoxisDiscoveries.SEEN)) {
                this.mob.getDiscoveries().mark(biome, NoxisDiscoveries.SEEN);
                if (this.mob.getDiscoveries().size() > 1 && random.nextFloat() < 0.7F) {   // el primero (donde nació) no sorprende
                    this.key = biome;
                    this.target = null;
                    this.phase = Phase.SCAN;
                    return true;
                }
            }
        }
        // Unos pocos lugares al azar a su alrededor (nunca todo el entorno).
        BlockPos origin = this.mob.blockPosition();
        for (int i = 0; i < SAMPLES; i++) {
            BlockPos p = origin.offset(random.nextInt(RADIUS * 2 + 1) - RADIUS, random.nextInt(5) - 2, random.nextInt(RADIUS * 2 + 1) - RADIUS);
            BlockState state = level.getBlockState(p);
            if (!state.is(ModTags.Blocks.DISCOVERABLE) || CLAIMED.containsKey(p)) continue;
            int k = NoxisDiscoveries.key(NoxisDiscoveries.BLOCK, BuiltInRegistries.BLOCK.getId(state.getBlock()));
            byte known = this.mob.getDiscoveries().get(k);
            boolean isNew = (known & NoxisDiscoveries.SEEN) == 0;
            boolean isKnown = (known & NoxisDiscoveries.KNOWN) != 0;
            // Lo nuevo le interesa; lo que ya conoce, solo de vez en cuando (una mirada corta).
            float chance = isNew ? interest(this.mob.getPersonality(), state) : isKnown ? 0.03F : 0.25F;
            if (random.nextFloat() >= chance) continue;
            Vec3 c = Vec3.atCenterOf(p);
            if (!this.canSee(c)) continue;
            this.target = p.immutable();
            this.key = k;
            this.firstTime = isNew;
            this.approach = !isKnown && this.wantsToApproach(random) && NoxisAmbientUtil.insideHome(this.mob, c);
            this.phase = Phase.NOTICE;
            return true;
        }
        return false;
    }

    private boolean canSee(Vec3 c) {
        Vec3 eye = this.mob.getEyePosition();
        var hit = this.mob.level().clip(new net.minecraft.world.level.ClipContext(eye, c,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, this.mob));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS
                || hit.getBlockPos().equals(BlockPos.containing(c));
    }

    private boolean wantsToApproach(RandomSource random) {
        return switch (this.mob.getPersonality()) {
            case CURIOUS -> random.nextFloat() < 0.85F;
            case ARTIST, MISCHIEVOUS -> random.nextFloat() < 0.6F;
            case AFFECTIONATE -> random.nextFloat() < 0.5F;
            case CALM -> random.nextFloat() < 0.15F;             // casi siempre mira desde donde está
        };
    }

    @Override
    public void start() {
        this.ticks = 0;
        RandomSource random = this.mob.getRandom();
        this.inspectLength = 60 + random.nextInt(80);
        if (this.target != null) {
            CLAIMED.put(this.target, this.mob.level().getGameTime() + 400);
            this.mob.getDiscoveries().mark(this.key, NoxisDiscoveries.SEEN);
        }
        this.mob.getNavigation().stop();
        this.mob.setAmbientAnim(this.phase == Phase.SCAN ? NoxisAmbientAnimation.DISCOVER_SCAN
                : this.firstTime ? NoxisAmbientAnimation.DISCOVER_SURPRISE : NoxisAmbientAnimation.DISCOVER_LOOK);
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.mob.canDoAmbient() || this.mob.hurtTime > 0) return false;
        if (this.phase == Phase.SCAN) return this.ticks < 100;
        BlockPos t = this.target;
        if (t == null || !this.mob.level().getBlockState(t).is(ModTags.Blocks.DISCOVERABLE)) return false;   // ya no está
        return switch (this.phase) {
            case NOTICE -> this.ticks < 40;
            case APPROACH -> this.ticks < 160;
            default -> this.ticks < this.inspectLength;
        };
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.ticks++;
        if (this.phase == Phase.SCAN) {
            this.mob.getNavigation().stop();
            // Mira a su alrededor despacio, como quien llega a un lugar nuevo.
            float yaw = this.mob.yBodyRot + 70.0F * net.minecraft.util.Mth.sin(this.ticks * 0.06F);
            Vec3 e = this.mob.getEyePosition();
            Vec3 d = NoxisAmbientUtil.forward(yaw);
            this.mob.getLookControl().setLookAt(e.x + d.x * 5.0D, e.y + 0.6D, e.z + d.z * 5.0D, 10.0F, 20.0F);
            if (this.ticks == 90) this.mob.getDiscoveries().mark(this.key, NoxisDiscoveries.KNOWN);
            return;
        }
        BlockPos t = this.target;
        if (t == null) return;
        Vec3 c = Vec3.atCenterOf(t);
        this.mob.getLookControl().setLookAt(c.x, c.y, c.z, 20.0F, 30.0F);
        switch (this.phase) {
            case NOTICE -> {
                this.mob.getNavigation().stop();
                if (this.ticks == NoxisAmbientAnimation.SURPRISE_LENGTH + 2) {
                    this.mob.setAmbientAnim(NoxisAmbientAnimation.DISCOVER_LOOK);
                }
                if (this.ticks >= 36) {
                    if (this.approach) {
                        this.phase = Phase.APPROACH;
                    } else {
                        this.phase = Phase.INSPECT;
                        this.mob.setAmbientAnim(this.lowFor(t) ? NoxisAmbientAnimation.DISCOVER_LOW : NoxisAmbientAnimation.DISCOVER_LOOK);
                    }
                    this.ticks = 0;
                }
            }
            case APPROACH -> {
                double d2 = NoxisSpacing.horizontal2(this.mob.position(), c);
                if (d2 < 1.6D * 1.6D) {
                    this.mob.getNavigation().stop();
                    this.phase = Phase.INSPECT;
                    this.ticks = 0;
                    this.mob.setAmbientAnim(this.lowFor(t) ? NoxisAmbientAnimation.DISCOVER_LOW : NoxisAmbientAnimation.DISCOVER_LOOK);
                } else if (this.ticks % 10 == 1) {
                    this.mob.getNavigation().moveTo(c.x, t.getY(), c.z, SPEED);
                }
            }
            case INSPECT -> {
                this.mob.getNavigation().stop();
                if (this.ticks == this.inspectLength - 5) {
                    // Si se acercó a mirarlo bien, ahora lo "conoce"; si lo miró de lejos, solo lo "vio".
                    if (NoxisSpacing.horizontal2(this.mob.position(), c) < 2.5D * 2.5D) {
                        this.mob.getDiscoveries().mark(this.key, NoxisDiscoveries.KNOWN);
                    }
                }
            }
            default -> { }
        }
    }

    /** ¿Es algo chiquito o que está abajo? (se agacha para verlo). */
    private boolean lowFor(BlockPos t) {
        return t.getY() < this.mob.getBlockY() || this.mob.level().getBlockState(t).getShape(this.mob.level(), t).max(net.minecraft.core.Direction.Axis.Y) < 0.6D;
    }

    @Override
    public void stop() {
        if (this.target != null) CLAIMED.remove(this.target);
        this.mob.setAmbientAnim(NoxisAmbientAnimation.NONE);
        this.mob.getNavigation().stop();
        this.target = null;
        RandomSource random = this.mob.getRandom();
        this.nextAllowedTime = this.mob.level().getGameTime()
                + NoxisConfig.scaleCooldown(COOLDOWN + random.nextInt(COOLDOWN_RANDOM), NoxisConfig.get().discoveries);
    }
}
