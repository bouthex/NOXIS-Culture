package com.noxisculture.entity.ai;

import com.noxisculture.entity.nature.NoxisFlowerCarry;
import com.noxisculture.entity.nature.NoxisFlowerReservations;
import com.noxisculture.entity.nature.NoxisFlowers;
import com.noxisculture.entity.nature.NoxisNatureAction;
import com.noxisculture.sound.ModSounds;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Pequeñas interacciones con la naturaleza (servidor). De vez en cuando, por iniciativa propia,
 * un Noxis tranquilo elige UNA de estas actividades:
 * <ol>
 *   <li><b>Elegir y admirar una flor:</b> mira varias flores, elige una (prefiere amarillas y
 *       violetas), se acerca, la recoge con cuidado, la contempla, la huele y la vuelve a plantar
 *       en el mismo lugar y con el mismo estado. Después la mira una última vez.</li>
 *   <li>A veces (30%), en vez de devolverla, <b>se la regala</b> a alguien cercano: un jugador,
 *       otro Noxis o un animal pacífico. Se acerca, se la ofrece y se la lanza suavecito como
 *       objeto real. Si no hay nadie cerca, la devuelve a su lugar como siempre.</li>
 *   <li><b>Olfatear una flor</b> sin recogerla.</li>
 *   <li><b>Sentarse a contemplar</b> un lugar lindo con flores o plantas.</li>
 * </ol>
 *
 * <p>Cada Noxis tiene sus propios tiempos (primer intento al azar, decisiones escalonadas y
 * cooldown propio), aparta la flor elegida para que nadie más la tome, no repite siempre lo
 * mismo y, si hay otros Noxis ocupados cerca, es menos probable que se sume.</p>
 */
public class NoxisNatureGoal<T extends PathfinderMob & NoxisNatureLover> extends Goal {
    private static final int RADIUS = 7;
    private static final int RADIUS_Y = 2;
    private static final double SPEED = 0.4D;
    private static final double REACH = 1.4D;          // distancia (bloques) para tocar la flor
    private static final int WALK_TIMEOUT = 240;       // 12 s para llegar; si no, desiste
    private static final float GIFT_CHANCE = 0.3F;     // 3 de cada 10 flores recogidas se regalan
    private static final double GIFT_RADIUS = 8.0D;
    private static final double GIFT_DISTANCE = 2.2D;  // distancia (bloques) desde la que la lanza
    private static final int GIFT_TOSS_TICK = 14;

    private enum Activity { PICK, SNIFF, SIT }

    private enum Step { SURVEY, APPROACH, PICK, ADMIRE, RETURN, REPLANT, LAST_LOOK, SNIFF, SIT,
        GIFT_APPROACH, GIFT, DONE }

    private final T mob;
    private int nextDecisionTick;
    private long nextAllowedTime;
    private @Nullable Activity lastActivity;

    private Activity activity = Activity.PICK;
    private Step step = Step.DONE;
    private int stepTicks;
    private int stepLength;
    private @Nullable BlockPos target;
    private final List<BlockPos> candidates = new ArrayList<>();
    private @Nullable Vec3 lookAt;
    private boolean reserved;
    private boolean wantsToGift;
    /** Tick del paso actual en el que suena el olfateo (una sola vez por animación); -1 = no. */
    private int sniffSoundAt = -1;
    private @Nullable LivingEntity recipient;

    public NoxisNatureGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        // Iniciativa propia: cada Noxis arranca con su propio retraso (10 a 70 s).
        this.nextDecisionTick = 200 + mob.getRandom().nextInt(1200);
    }

    // ------------------------------------------------------------------ cuándo empieza

    @Override
    public boolean canUse() {
        if (this.mob.tickCount < this.nextDecisionTick) return false;
        RandomSource random = this.mob.getRandom();
        this.nextDecisionTick = this.mob.tickCount + 60 + random.nextInt(100);   // decide cada 3-8 s
        Level level = this.mob.level();
        if (level.getGameTime() < this.nextAllowedTime) return false;
        if (this.mob.getFlowerCarry().isHolding()) return false;
        if (!this.mob.canEnjoyNature() || !this.mob.onGround() || this.mob.isInWater()) return false;
        if (random.nextInt(3) != 0) return false;                                 // no siempre tiene ganas
        // Si otros Noxis cerca ya están con flores, es menos probable que se sume.
        int busy = NoxisFlowerReservations.busyNear(this.mob, 10.0D);
        if (busy > 0 && random.nextInt(2 + busy * 2) != 0) return false;

        for (Activity choice : this.shuffledActivities(random)) {
            if (this.plan(choice)) {
                this.activity = choice;
                return true;
            }
        }
        // No encontró nada lindo: vuelve a intentarlo dentro de un rato.
        this.nextDecisionTick = this.mob.tickCount + 200 + random.nextInt(200);
        return false;
    }

    /** Orden aleatorio con pesos; lo que hizo la última vez queda con menos chances. */
    private List<Activity> shuffledActivities(RandomSource random) {
        List<Activity> pool = new ArrayList<>(List.of(Activity.PICK, Activity.SNIFF, Activity.SIT));
        List<Activity> order = new ArrayList<>();
        while (!pool.isEmpty()) {
            float total = 0.0F;
            for (Activity a : pool) total += this.weight(a);
            float r = random.nextFloat() * total;
            Activity chosen = pool.get(pool.size() - 1);
            for (Activity a : pool) {
                r -= this.weight(a);
                if (r <= 0.0F) { chosen = a; break; }
            }
            order.add(chosen);
            pool.remove(chosen);
        }
        return order;
    }

    private float weight(Activity a) {
        float w = switch (a) {
            case PICK -> 0.45F;
            case SNIFF -> 0.35F;
            case SIT -> 0.2F;
        };
        return a == this.lastActivity ? w * 0.3F : w;
    }

    private boolean plan(Activity choice) {
        this.candidates.clear();
        this.target = null;
        RandomSource random = this.mob.getRandom();
        Level level = this.mob.level();
        BlockPos origin = this.mob.blockPosition();

        if (choice == Activity.SIT) {
            int nature = 0;
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-4, -1, -4), origin.offset(4, 1, 4))) {
                if (NoxisFlowers.isNature(level.getBlockState(pos)) && ++nature >= 5) return true;
            }
            return false;
        }

        // Junta flores cercanas (no apartadas por otro) con un puntaje: favoritas, cercanía y azar.
        List<BlockPos> found = new ArrayList<>();
        List<Float> scores = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-RADIUS, -RADIUS_Y, -RADIUS),
                origin.offset(RADIUS, RADIUS_Y, RADIUS))) {
            BlockState state = level.getBlockState(pos);
            boolean ok = choice == Activity.PICK ? NoxisFlowers.isPickable(state) : NoxisFlowers.isFlower(state);
            if (!ok || NoxisFlowerReservations.isTakenByOther(this.mob, pos)) continue;
            double dist = Math.sqrt(pos.distToCenterSqr(this.mob.position()));
            float score = (NoxisFlowers.isFavorite(state) ? 3.0F : 1.0F) / (1.0F + (float) dist * 0.15F);
            found.add(pos.immutable());
            scores.add(score * (0.6F + random.nextFloat() * 0.8F));
        }
        if (found.isEmpty()) return false;

        // Unas pocas candidatas para "dudar" entre ellas, y la elegida por sorteo con pesos
        // (casi siempre una favorita, pero no siempre la misma ni la primera encontrada).
        for (int i = 0; i < 3 && !found.isEmpty(); i++) {
            int pick = weightedIndex(scores, random);
            this.candidates.add(found.remove(pick));
            scores.remove(pick);
        }
        List<Float> finalScores = new ArrayList<>();
        for (BlockPos pos : this.candidates) {
            BlockState state = level.getBlockState(pos);
            finalScores.add((NoxisFlowers.isFavorite(state) ? 4.0F : 1.0F) * (0.7F + random.nextFloat() * 0.6F));
        }
        BlockPos chosen = this.candidates.get(weightedIndex(finalScores, random));
        if (this.mob.getNavigation().createPath(chosen, 0) == null && !this.isNear(chosen)) return false;
        if (!NoxisFlowerReservations.reserve(this.mob, chosen)) return false;
        this.target = chosen;
        this.reserved = true;
        return true;
    }

    private static int weightedIndex(List<Float> scores, RandomSource random) {
        float total = 0.0F;
        for (float s : scores) total += s;
        float r = random.nextFloat() * total;
        for (int i = 0; i < scores.size(); i++) {
            r -= scores.get(i);
            if (r <= 0.0F) return i;
        }
        return scores.size() - 1;
    }

    // ------------------------------------------------------------------ la secuencia

    @Override
    public void start() {
        this.mob.getNavigation().stop();
        switch (this.activity) {
            case PICK -> this.enter(Step.SURVEY, 46 + this.mob.getRandom().nextInt(20), NoxisNatureAction.SURVEY);
            case SNIFF -> this.enter(Step.APPROACH, WALK_TIMEOUT, NoxisNatureAction.NONE);
            case SIT -> {
                this.enter(Step.SIT, 200 + this.mob.getRandom().nextInt(140), NoxisNatureAction.SIT);
                this.lookAt = null;
            }
        }
    }

    private void enter(Step next, int length, byte action) {
        this.sniffSoundAt = -1;
        this.step = next;
        this.stepTicks = 0;
        this.stepLength = length;
        this.mob.setNatureAction(action);
    }

    @Override
    public boolean canContinueToUse() {
        return this.step != Step.DONE && this.mob.canEnjoyNature() && this.mob.hurtTime == 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.stepTicks++;
        Level level = this.mob.level();
        if (this.stepTicks == this.sniffSoundAt) {
            this.mob.playSound(ModSounds.NOXIS_SNIFF, 0.7F, 0.95F + this.mob.getRandom().nextFloat() * 0.15F);
            this.sniffSoundAt = -1;                         // una sola vez por animación
        }
        NoxisFlowerCarry carry = this.mob.getFlowerCarry();
        switch (this.step) {
            case SURVEY -> {
                // Mira una candidata, después otra... decidiendo cuál le gusta más.
                this.mob.getNavigation().stop();
                int idx = Math.min(this.candidates.size() - 1, this.stepTicks / 16);
                BlockPos look = idx >= 0 && idx < this.candidates.size() ? this.candidates.get(idx) : this.target;
                if (this.stepTicks >= this.stepLength - 10 || look == null) look = this.target;
                this.lookAtBlock(look);
                if (this.stepTicks >= this.stepLength) this.enter(Step.APPROACH, WALK_TIMEOUT, NoxisNatureAction.NONE);
            }
            case APPROACH -> {
                if (this.target == null) { this.step = Step.DONE; return; }
                this.lookAtBlock(this.target);
                if (this.isNear(this.target)) {
                    this.mob.getNavigation().stop();
                    if (this.activity == Activity.PICK) this.enter(Step.PICK, 16, NoxisNatureAction.PICK);
                    else {
                        this.enter(Step.SNIFF, 64 + this.mob.getRandom().nextInt(20), NoxisNatureAction.SNIFF);
                        this.sniffSoundAt = 9;   // cuando acerca la naricita
                    }
                } else if (this.stepTicks >= this.stepLength) {
                    this.step = Step.DONE;                         // no pudo llegar: desiste
                } else if (this.stepTicks % 20 == 1 || this.mob.getNavigation().isDone()) {
                    this.mob.getNavigation().moveTo(this.target.getX() + 0.5D, this.target.getY(), this.target.getZ() + 0.5D, SPEED);
                }
            }
            case PICK -> {
                this.mob.getNavigation().stop();
                this.lookAtBlock(this.target);
                if (this.stepTicks == 9) {
                    if (this.target != null && carry.pick(level, this.target)) {
                        this.mob.syncHeldFlower();
                    } else {
                        this.step = Step.DONE;                     // la flor ya no estaba
                        return;
                    }
                }
                if (this.stepTicks >= this.stepLength) {
                    this.enter(Step.ADMIRE, 130 + this.mob.getRandom().nextInt(40), NoxisNatureAction.ADMIRE);
                    this.sniffSoundAt = 70;   // cuando acerca la flor a la carita
                    this.wantsToGift = this.mob.getRandom().nextFloat() < GIFT_CHANCE;
                }
            }
            case ADMIRE -> {
                this.mob.getNavigation().stop();
                // Mira la flor que tiene en la mano: delante suyo, un poco abajo.
                Vec3 forward = Vec3.directionFromRotation(0.0F, this.mob.yBodyRot);
                this.mob.getLookControl().setLookAt(this.mob.getX() + forward.x, this.mob.getEyeY() - 0.15D,
                        this.mob.getZ() + forward.z, 10.0F, 30.0F);
                if (this.stepTicks == 104) {
                    this.mob.playSound(ModSounds.NOXIS_HAPPY, 0.45F, 1.15F);
                }
                if (this.stepTicks == 98 && level instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.HEART, this.mob.getX(), this.mob.getEyeY() + 0.45D,
                            this.mob.getZ(), 1, 0.1D, 0.05D, 0.1D, 0.0D);
                }
                if (this.stepTicks >= this.stepLength) {
                    // ¿Se la regala a alguien? Si no hay nadie apropiado cerca, la devuelve a su lugar.
                    this.recipient = this.wantsToGift ? this.findRecipient() : null;
                    if (this.recipient != null) this.enter(Step.GIFT_APPROACH, WALK_TIMEOUT, NoxisNatureAction.NONE);
                    else this.enter(Step.RETURN, WALK_TIMEOUT, NoxisNatureAction.NONE);
                }
            }
            case GIFT_APPROACH -> {
                LivingEntity to = this.recipient;
                if (!this.isValidRecipient(to)) {                  // se fue: la devuelve a su lugar
                    this.recipient = null;
                    this.enter(Step.RETURN, WALK_TIMEOUT, NoxisNatureAction.NONE);
                    return;
                }
                this.mob.getLookControl().setLookAt(to, 30.0F, 30.0F);
                if (this.mob.distanceToSqr(to) < GIFT_DISTANCE * GIFT_DISTANCE) {
                    this.mob.getNavigation().stop();
                    this.enter(Step.GIFT, 34, NoxisNatureAction.GIFT);
                } else if (this.stepTicks >= this.stepLength) {
                    this.recipient = null;
                    this.enter(Step.RETURN, WALK_TIMEOUT, NoxisNatureAction.NONE);
                } else if (this.stepTicks % 20 == 1 || this.mob.getNavigation().isDone()) {
                    this.mob.getNavigation().moveTo(to, SPEED);
                }
            }
            case GIFT -> {
                this.mob.getNavigation().stop();
                LivingEntity to = this.recipient;
                if (to != null) this.mob.getLookControl().setLookAt(to, 30.0F, 30.0F);
                if (this.stepTicks == GIFT_TOSS_TICK) {
                    if (to != null && to.isAlive() && carry.isHolding()) {
                        this.tossGift(level, carry, to);
                    } else {
                        this.recipient = null;
                        this.enter(Step.RETURN, WALK_TIMEOUT, NoxisNatureAction.NONE);
                        return;
                    }
                }
                if (this.stepTicks >= this.stepLength) this.step = Step.DONE;
            }
            case RETURN -> {
                BlockPos home = carry.getOrigin();
                if (home == null) { this.step = Step.DONE; return; }
                this.lookAtBlock(home);
                if (this.isNear(home)) {
                    this.mob.getNavigation().stop();
                    this.enter(Step.REPLANT, 16, NoxisNatureAction.REPLANT);
                } else if (this.stepTicks >= this.stepLength) {
                    this.step = Step.DONE;                         // stop() la devuelve o la suelta
                } else if (this.stepTicks % 20 == 1 || this.mob.getNavigation().isDone()) {
                    this.mob.getNavigation().moveTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, SPEED);
                }
            }
            case REPLANT -> {
                this.mob.getNavigation().stop();
                this.lookAtBlock(carry.getOrigin() != null ? carry.getOrigin() : this.target);
                if (this.stepTicks == 9) {
                    carry.returnOrDrop(level, this.mob);
                    this.mob.syncHeldFlower();
                }
                if (this.stepTicks >= this.stepLength) this.enter(Step.LAST_LOOK, 30, NoxisNatureAction.LAST_LOOK);
            }
            case LAST_LOOK, SNIFF -> {
                this.mob.getNavigation().stop();
                this.lookAtBlock(this.target);
                if (this.stepTicks >= this.stepLength) this.step = Step.DONE;
            }
            case SIT -> {
                this.mob.getNavigation().stop();
                // Mira el paisaje con calma: cada 2-3 s, otra planta o flor cercana.
                if (this.lookAt == null || this.stepTicks % 50 == 0) this.lookAt = this.randomNatureSpot();
                if (this.lookAt != null) {
                    this.mob.getLookControl().setLookAt(this.lookAt.x, this.lookAt.y, this.lookAt.z, 4.0F, 20.0F);
                }
                if (this.stepTicks >= this.stepLength) this.step = Step.DONE;
            }
            case DONE -> { }
        }
    }

    // ------------------------------------------------------------------ regalo

    /** Alguien cercano a quien regalarle la flor: un jugador, otro Noxis o un animal pacífico. */
    private @Nullable LivingEntity findRecipient() {
        RandomSource random = this.mob.getRandom();
        AABB area = this.mob.getBoundingBox().inflate(GIFT_RADIUS, 3.0D, GIFT_RADIUS);
        List<LivingEntity> options = new ArrayList<>();
        List<Float> weights = new ArrayList<>();
        for (LivingEntity e : this.mob.level().getEntitiesOfClass(LivingEntity.class, area, this::isValidRecipient)) {
            float w = e instanceof Player ? 3.0F : e instanceof NoxisNatureLover ? 2.0F : 1.0F;
            if (this.mob.getNavigation().createPath(e, 1) == null) continue;
            options.add(e);
            weights.add(w * (0.5F + random.nextFloat()));
        }
        return options.isEmpty() ? null : options.get(weightedIndex(weights, random));
    }

    private boolean isValidRecipient(@Nullable LivingEntity e) {
        if (e == null || e == this.mob || !e.isAlive() || e.isRemoved()) return false;
        if (e.distanceToSqr(this.mob) > (GIFT_RADIUS + 4.0D) * (GIFT_RADIUS + 4.0D)) return false;
        if (e instanceof Player player) return !player.isSpectator();
        if (e instanceof NoxisNatureLover lover) return lover.canReceiveGift();
        return e instanceof Animal;
    }

    /** Convierte la flor en un objeto real y se la lanza suavecito al destinatario. */
    private void tossGift(Level level, NoxisFlowerCarry carry, LivingEntity to) {
        ItemStack gift = carry.giveAway();
        this.mob.syncHeldFlower();
        if (gift.isEmpty() || !(level instanceof ServerLevel server)) return;
        Vec3 from = this.mob.position().add(0.0D, this.mob.getBbHeight() * 0.55D, 0.0D);
        Vec3 dir = to.position().subtract(this.mob.position());
        Vec3 flat = new Vec3(dir.x, 0.0D, dir.z);
        double dist = Math.max(0.5D, flat.length());
        Vec3 push = flat.normalize().scale(Math.min(0.32D, 0.12D + dist * 0.07D)).add(0.0D, 0.22D, 0.0D);
        ItemEntity item = new ItemEntity(level, from.x + flat.normalize().x * 0.3D, from.y,
                from.z + flat.normalize().z * 0.3D, gift);
        item.setDeltaMovement(push);
        item.setPickUpDelay(10);
        level.addFreshEntity(item);
        if (to instanceof NoxisNatureLover lover && lover.canReceiveGift()) {
            // Otro Noxis: la espera y la atrapa. Mientras vuela, nadie más puede levantarla.
            item.setPickUpDelay(60);
            lover.expectGift(item, this.mob);
        }
        this.mob.playSound(ModSounds.NOXIS_HAPPY, 0.55F, 1.25F);
        server.sendParticles(ParticleTypes.HEART, this.mob.getX(), this.mob.getEyeY() + 0.4D, this.mob.getZ(),
                1, 0.1D, 0.05D, 0.1D, 0.0D);
    }

    private @Nullable Vec3 randomNatureSpot() {
        RandomSource random = this.mob.getRandom();
        BlockPos origin = this.mob.blockPosition();
        for (int i = 0; i < 12; i++) {
            BlockPos pos = origin.offset(random.nextInt(9) - 4, random.nextInt(3) - 1, random.nextInt(9) - 4);
            if (NoxisFlowers.isNature(this.mob.level().getBlockState(pos))) return Vec3.atCenterOf(pos);
        }
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.mob.yBodyRot + random.nextInt(120) - 60);
        return this.mob.getEyePosition().add(forward.scale(4.0D));
    }

    private boolean isNear(BlockPos pos) {
        double dx = pos.getX() + 0.5D - this.mob.getX();
        double dz = pos.getZ() + 0.5D - this.mob.getZ();
        return dx * dx + dz * dz < REACH * REACH && Math.abs(pos.getY() - this.mob.getY()) < 1.5D;
    }

    private void lookAtBlock(@Nullable BlockPos pos) {
        if (pos != null) {
            this.mob.getLookControl().setLookAt(pos.getX() + 0.5D, pos.getY() + 0.3D, pos.getZ() + 0.5D, 20.0F, 30.0F);
        }
    }

    @Override
    public void stop() {
        Level level = this.mob.level();
        NoxisFlowerCarry carry = this.mob.getFlowerCarry();
        if (carry.isHolding()) {
            // Interrumpido con la flor en la mano: si está cerca de su lugar la devuelve; si no,
            // o si el lugar ya no está libre, la deja como objeto. Nunca se pierde ni se duplica.
            BlockPos home = carry.getOrigin();
            if (home != null && home.distToCenterSqr(this.mob.position()) < 64.0D) {
                carry.returnOrDrop(level, this.mob);
            } else {
                carry.drop(level, this.mob);
            }
            this.mob.syncHeldFlower();
        }
        if (this.reserved && this.target != null) {
            NoxisFlowerReservations.release(this.mob, this.target);
        }
        this.reserved = false;
        this.wantsToGift = false;
        this.recipient = null;
        this.sniffSoundAt = -1;
        this.target = null;
        this.candidates.clear();
        this.step = Step.DONE;
        this.mob.getNavigation().stop();
        this.mob.setNatureAction(NoxisNatureAction.NONE);
        // Cooldown propio: cada Noxis vuelve a tener ganas a su tiempo.
        RandomSource random = this.mob.getRandom();
        int cooldown = switch (this.activity) {
            case PICK -> 1800 + random.nextInt(1800);   // 1,5 a 3 min
            case SNIFF -> 800 + random.nextInt(1000);   // 40 a 90 s
            case SIT -> 2400 + random.nextInt(2400);    // 2 a 4 min
        };
        this.nextAllowedTime = level.getGameTime() + cooldown;
        this.lastActivity = this.activity;
    }
}
