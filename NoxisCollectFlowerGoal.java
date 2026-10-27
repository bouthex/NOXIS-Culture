package com.noxisculture.entity.ai;

import com.noxisculture.entity.nature.NoxisFlowers;
import com.noxisculture.entity.nature.NoxisItemClaims;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

/**
 * Recoger flores tiradas (servidor): de vez en cuando, un Noxis tranquilo y con las manos libres
 * ve una flor suelta en el piso, camina hasta ella y la guarda en su inventario.
 *
 * <ul>
 *   <li>Ocasional: busca cada 2 s, y después de recoger (o desistir) espera de 40 a 90 s.</li>
 *   <li>Cada flor la busca un solo Noxis: la aparta al elegirla ({@link NoxisItemClaims}).</li>
 *   <li>Las flores reservadas como regalo para otro Noxis se ignoran: son de su destinatario.</li>
 *   <li>Solo levanta lo que entra en su inventario; lo que no entra queda en el piso. Nunca
 *   duplica ni pierde nada (el objeto del mundo se achica o desaparece por lo que guardó).</li>
 * </ul>
 */
public class NoxisCollectFlowerGoal<T extends PathfinderMob & NoxisNatureLover> extends Goal {
    private static final double RADIUS = 10.0D;
    private static final int SCAN_INTERVAL = 40;
    private static final int COOLDOWN = 800;           // 40 s ...
    private static final int COOLDOWN_RANDOM = 1000;   // ... a 90 s
    private static final int TIMEOUT = 300;            // 15 s para llegar
    private static final int MAX_PATH_FAILS = 3;
    private static final double REACH = 1.2D;
    private static final double SPEED = 0.4D;

    private final T mob;
    private int nextScanTick;
    private long nextAllowedTime = -1L;
    private @Nullable ItemEntity target;
    private int ticks;
    private int pathFails;
    private boolean done;

    public NoxisCollectFlowerGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.nextScanTick = mob.getRandom().nextInt(SCAN_INTERVAL);
    }

    /** ¿Es una flor (vanilla o de otro mod) que un Noxis guardaría? (la rosa marchita, ni se toca). */
    public static boolean isFlowerItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem block
                && block.getBlock() != Blocks.WITHER_ROSE
                && NoxisFlowers.isFlower(block.getBlock().defaultBlockState());
    }

    @Override
    public boolean canUse() {
        long now = this.mob.level().getGameTime();
        if (this.nextAllowedTime < 0L) this.nextAllowedTime = now + 200 + this.mob.getRandom().nextInt(600);
        if (now < this.nextAllowedTime || this.mob.tickCount < this.nextScanTick) return false;
        this.nextScanTick = this.mob.tickCount + SCAN_INTERVAL;
        if (!this.mob.canCollectFlowers() || !this.mob.onGround() || this.mob.isInWater()) return false;
        List<ItemEntity> found = this.mob.level().getEntitiesOfClass(ItemEntity.class,
                this.mob.getBoundingBox().inflate(RADIUS, 3.0D, RADIUS),
                e -> e.isAlive() && e.onGround() && !e.hasPickUpDelay() && isFlowerItem(e.getItem())
                        && !NoxisItemClaims.isClaimedByOther(e, this.mob)       // regalos ajenos y flores ya elegidas
                        && this.mob.hasRoomFor(e.getItem()));
        if (found.isEmpty()) return false;
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(this.mob)));
        for (ItemEntity e : found) {
            if (this.mob.getNavigation().createPath(e, 0) == null) continue;     // no llega: otra
            if (NoxisItemClaims.claim(e, this.mob)) {
                this.target = e;
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.pathFails = 0;
        this.done = false;
        if (this.target != null) this.mob.getNavigation().moveTo(this.target, SPEED);
    }

    @Override
    public boolean canContinueToUse() {
        ItemEntity item = this.target;
        return !this.done && item != null && item.isAlive() && !item.getItem().isEmpty()
                && NoxisItemClaims.isClaimedBy(item, this.mob) && this.mob.canCollectFlowers()
                && this.mob.hurtTime == 0 && this.ticks < TIMEOUT && this.pathFails < MAX_PATH_FAILS;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        ItemEntity item = this.target;
        if (item == null) return;
        this.ticks++;
        NoxisItemClaims.refresh(item, this.mob);
        this.mob.getLookControl().setLookAt(item, 30.0F, 30.0F);
        double dx = item.getX() - this.mob.getX();
        double dz = item.getZ() - this.mob.getZ();
        if (item.distanceToSqr(this.mob) < REACH * REACH
                || (dx * dx + dz * dz < 0.8D * 0.8D && Math.abs(item.getY() - this.mob.getY()) < 1.0D)) {
            this.collect(item);
            return;
        }
        if (this.ticks % 10 == 1 || this.mob.getNavigation().isDone()) {
            if (!this.mob.getNavigation().moveTo(item, SPEED)) this.pathFails++;
        }
    }

    /** La guarda en el inventario: solo lo que entra; el resto queda en el piso. */
    private void collect(ItemEntity item) {
        this.mob.getNavigation().stop();
        ItemStack stack = item.getItem();
        ItemStack rest = this.mob.storeCollected(stack.copy());
        int taken = stack.getCount() - rest.getCount();
        if (taken > 0) {
            this.mob.take(item, taken);                      // el objeto "vuela" hacia él (vanilla)
            if (rest.isEmpty()) {
                item.discard();
            } else {
                item.setItem(rest);
            }
            this.mob.playSound(SoundEvents.ITEM_PICKUP, 0.3F, 1.4F);
        }
        this.done = true;
    }

    @Override
    public void stop() {
        if (this.target != null) NoxisItemClaims.release(this.target, this.mob);
        this.target = null;
        this.mob.getNavigation().stop();
        this.nextAllowedTime = this.mob.level().getGameTime() + COOLDOWN + this.mob.getRandom().nextInt(COOLDOWN_RANDOM);
    }
}
