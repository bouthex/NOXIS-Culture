package com.noxisculture.entity.ai;

import com.noxisculture.entity.idle.NoxisCrystals;
import com.noxisculture.entity.idle.NoxisKeepsakeAnimation;
import com.noxisculture.entity.nature.NoxisItemClaims;
import com.noxisculture.entity.nature.NoxisTreasures;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Objetos que llaman la atención (servidor): de vez en cuando, un Noxis tranquilo ve un objeto
 * en el piso o en la mano de un jugador, se acerca, lo mira de cerca, lo olfatea y ladea la
 * cabeza. Los cristales y lo de Noxus le despiertan la fascinación de siempre; las flores, mucho
 * interés. Si es un tesoro suelto en el piso, a veces se lo queda (va a su inventario privado).
 *
 * <ul>
 *   <li>Nunca toma nada de la mano de un jugador ni de cofres: solo mira.</li>
 *   <li>No toca regalos reservados para otro Noxis ni objetos que otro ya eligió.</li>
 *   <li>Ocasional (espera de 1 a 2,5 min); los curiosos más, los tranquilos menos.</li>
 * </ul>
 */
public class NoxisInspectItemGoal<T extends PathfinderMob & NoxisKeeper> extends Goal {
    private static final double RADIUS = 7.0D;
    private static final int COOLDOWN = 1200;
    private static final int COOLDOWN_RANDOM = 1800;
    private static final int APPROACH_TIMEOUT = 160;
    private static final double SPEED = 0.4D;

    private final T mob;
    private int nextCheckTick;
    private long nextAllowedTime = -1L;
    private @Nullable Entity target;
    private boolean inspecting;
    private boolean crystal;
    private int ticks;
    private int lookTicks;
    private int pathFails;

    public NoxisInspectItemGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.nextCheckTick = mob.getRandom().nextInt(200);
    }

    private static ItemStack stackOf(Entity e) {
        if (e instanceof ItemEntity item) return item.getItem();
        if (e instanceof Player p) return p.getMainHandItem();
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse() {
        if (this.mob.tickCount < this.nextCheckTick) return false;
        RandomSource random = this.mob.getRandom();
        this.nextCheckTick = this.mob.tickCount + 60 + random.nextInt(40);
        long now = this.mob.level().getGameTime();
        if (this.nextAllowedTime < 0L) this.nextAllowedTime = now + 600 + random.nextInt(1200);
        if (now < this.nextAllowedTime || !this.mob.canHandleKeepsakes()) return false;
        if (random.nextFloat() >= 0.3F * this.mob.getPersonality().inspectWeight()) return false;
        List<Entity> special = new ArrayList<>();
        List<Entity> plain = new ArrayList<>();
        for (ItemEntity item : this.mob.level().getEntitiesOfClass(ItemEntity.class,
                this.mob.getBoundingBox().inflate(RADIUS, 3.0D, RADIUS),
                e -> e.isAlive() && e.onGround() && !NoxisItemClaims.isClaimedByOther(e, this.mob))) {
            (NoxisTreasures.isSpecial(item.getItem()) ? special : plain).add(item);
        }
        for (Player p : this.mob.level().getEntitiesOfClass(Player.class, this.mob.getBoundingBox().inflate(RADIUS - 1.0D, 3.0D, RADIUS - 1.0D),
                p -> p.isAlive() && !p.isSpectator() && !p.getMainHandItem().isEmpty())) {
            (NoxisTreasures.isSpecial(p.getMainHandItem()) ? special : plain).add(p);
        }
        // Lo especial (cristales, Noxus, flores) casi siempre gana; lo común, a veces.
        List<Entity> pool = !special.isEmpty() && (plain.isEmpty() || random.nextFloat() < 0.8F) ? special : plain;
        if (pool.isEmpty() || (pool == plain && random.nextInt(3) != 0)) return false;
        Entity pick = pool.get(random.nextInt(pool.size()));
        if (this.mob.getNavigation().createPath(pick, 1) == null) return false;
        if (pick instanceof ItemEntity item && !NoxisItemClaims.claim(item, this.mob)) return false;
        this.target = pick;
        return true;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.lookTicks = 0;
        this.pathFails = 0;
        this.inspecting = false;
        this.crystal = this.target != null && NoxisCrystals.isCrystal(stackOf(this.target));
    }

    @Override
    public boolean canContinueToUse() {
        Entity t = this.target;
        if (t == null || !t.isAlive() || stackOf(t).isEmpty() || this.mob.hurtTime > 0) return false;
        if (t instanceof ItemEntity item && !NoxisItemClaims.isClaimedBy(item, this.mob)) return false;
        if (!this.inspecting) {
            return this.ticks < APPROACH_TIMEOUT && this.pathFails < 3 && this.mob.canHandleKeepsakes();
        }
        // Mirándolo: sigue mientras nada importante pase (la fascinación de los cristales no lo corta).
        return this.lookTicks < NoxisKeepsakeAnimation.INSPECT_LENGTH && this.mob.distanceToSqr(t) < 5.0D * 5.0D
                && this.mob.canHandleKeepsakesWhileFascinated();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        Entity t = this.target;
        if (t == null) return;
        this.ticks++;
        if (t instanceof ItemEntity item) NoxisItemClaims.refresh(item, this.mob);
        double near = t instanceof Player ? 2.0D : 1.3D;
        this.mob.getLookControl().setLookAt(t.getX(), t.getY() + (t instanceof Player ? t.getBbHeight() * 0.5D : 0.15D),
                t.getZ(), 30.0F, 30.0F);
        if (!this.inspecting) {
            if (this.mob.distanceToSqr(t) < near * near) {
                this.mob.getNavigation().stop();
                this.inspecting = true;
                this.mob.setKeepsakeAnim(NoxisKeepsakeAnimation.INSPECT);
                if (this.crystal) this.mob.setFascinated(true);           // la fascinación de siempre
            } else if (this.ticks % 10 == 1 || this.mob.getNavigation().isDone()) {
                if (!this.mob.getNavigation().moveTo(t, SPEED)) this.pathFails++;
            }
            return;
        }
        this.mob.getNavigation().stop();
        this.lookTicks++;
        if (this.lookTicks == 20) this.mob.playSound(com.noxisculture.sound.ModSounds.NOXIS_SNIFF, 0.4F, 1.15F);
        // Al final: si es un tesoro suelto en el piso, a veces se lo queda.
        if (this.lookTicks == NoxisKeepsakeAnimation.INSPECT_LENGTH - 6 && t instanceof ItemEntity item
                && this.mob.appreciates(item.getItem())
                && this.mob.getRandom().nextFloat() < this.mob.getPersonality().keepChance()) {
            this.keep(item);
        }
    }

    /** Lo guarda en su inventario privado (solo lo que entra; el resto queda en el piso). */
    private void keep(ItemEntity item) {
        ItemStack stack = item.getItem();
        ItemStack rest = this.mob.getPrivateInventory().addItem(stack.copy());
        int taken = stack.getCount() - rest.getCount();
        if (taken <= 0) return;
        this.mob.take(item, taken);
        if (rest.isEmpty()) item.discard();
        else item.setItem(rest);
        this.mob.playSound(SoundEvents.ITEM_PICKUP, 0.3F, 1.4F);
    }

    @Override
    public void stop() {
        if (this.target instanceof ItemEntity item) NoxisItemClaims.release(item, this.mob);
        if (this.crystal) this.mob.setFascinated(false);
        this.mob.setKeepsakeAnim(NoxisKeepsakeAnimation.NONE);
        this.mob.getNavigation().stop();
        this.target = null;
        this.inspecting = false;
        this.nextAllowedTime = this.mob.level().getGameTime() + COOLDOWN + this.mob.getRandom().nextInt(COOLDOWN_RANDOM);
    }
}
