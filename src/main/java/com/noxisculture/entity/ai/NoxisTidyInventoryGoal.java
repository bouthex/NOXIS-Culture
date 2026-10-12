package com.noxisculture.entity.ai;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Ordenar lo que no le interesa (servidor). El inventario privado es para lo que aprecia; si por
 * algún motivo tiene algo que no le interesa (una espada, una herramienta...), lo lleva a un
 * cofre o barril cercano y lo deja ahí, en un lugar libre o completando una pila igual: NUNCA
 * saca, mueve ni cambia lo que ya había. Si no hay cofre a mano (o está lleno, o no llega), lo
 * deja en el piso y sigue con su vida. Nunca saca nada de cofres para sí.
 */
public class NoxisTidyInventoryGoal<T extends PathfinderMob & NoxisKeeper> extends Goal {
    private static final int RADIUS = 10;
    private static final int RADIUS_Y = 3;
    private static final int CHECK_INTERVAL = 100;
    private static final int WALK_TIMEOUT = 300;
    private static final double SPEED = 0.42D;

    private enum Phase { WALK, OPEN }

    private final T mob;
    private int nextCheckTick;
    private @Nullable BlockPos chest;
    private Phase phase = Phase.WALK;
    private int ticks;
    private int pathFails;
    private boolean done;

    public NoxisTidyInventoryGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.nextCheckTick = mob.getRandom().nextInt(CHECK_INTERVAL);
    }

    @Override
    public boolean canUse() {
        if (this.mob.tickCount < this.nextCheckTick) return false;
        this.nextCheckTick = this.mob.tickCount + CHECK_INTERVAL;
        if (!this.hasUnwanted() || !this.mob.canHandleKeepsakes()) return false;
        this.chest = this.findChest();
        if (this.chest == null) {
            this.dropUnwanted();                     // no hay cofre a mano: lo deja en el piso
            return false;
        }
        return true;
    }

    private boolean hasUnwanted() {
        SimpleContainer inv = this.mob.getPrivateInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && !this.mob.appreciates(s)) return true;
        }
        return false;
    }

    /** El cofre o barril cercano más próximo, con lugar para algo de lo que lleva, al que pueda llegar. */
    private @Nullable BlockPos findChest() {
        BlockPos origin = this.mob.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-RADIUS, -RADIUS_Y, -RADIUS), origin.offset(RADIUS, RADIUS_Y, RADIUS))) {
            BlockEntity be = this.mob.level().getBlockEntity(pos);
            if (!(be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity) || !(be instanceof Container c)) continue;
            double d = pos.distSqr(origin);
            if (d >= bestDist || !this.hasRoom(c)) continue;
            if (this.mob.getNavigation().createPath(pos, 1) == null) continue;
            best = pos.immutable();
            bestDist = d;
        }
        return best;
    }

    private boolean hasRoom(Container c) {
        SimpleContainer inv = this.mob.getPrivateInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && !this.mob.appreciates(s) && fits(c, s)) return true;
        }
        return false;
    }

    private static boolean fits(Container c, ItemStack stack) {
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack in = c.getItem(i);
            if (!c.canPlaceItem(i, stack)) continue;
            if (in.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(in, stack) && in.getCount() < in.getMaxStackSize()) return true;
        }
        return false;
    }

    @Override
    public void start() {
        this.phase = Phase.WALK;
        this.ticks = 0;
        this.pathFails = 0;
        this.done = false;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.done && this.chest != null && this.mob.hurtTime == 0 && this.mob.canHandleKeepsakes();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        BlockPos pos = this.chest;
        if (pos == null) return;
        this.ticks++;
        Vec3 center = Vec3.atCenterOf(pos);
        this.mob.getLookControl().setLookAt(center.x, center.y, center.z, 30.0F, 30.0F);
        if (this.phase == Phase.WALK) {
            if (this.mob.distanceToSqr(center) < 2.2D * 2.2D) {
                this.mob.getNavigation().stop();
                this.phase = Phase.OPEN;
                this.ticks = 0;
                this.openLid(pos, true);
            } else if (this.ticks > WALK_TIMEOUT || this.pathFails >= 3) {
                this.dropUnwanted();                 // no llegó: lo deja en el piso
                this.done = true;
            } else if (this.ticks % 10 == 1 || this.mob.getNavigation().isDone()) {
                if (!this.mob.getNavigation().moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, SPEED)) this.pathFails++;
            }
            return;
        }
        this.mob.getNavigation().stop();
        if (this.ticks == 8) this.storeUnwanted(pos);
        if (this.ticks >= 16) {
            this.openLid(pos, false);
            this.done = true;
        }
    }

    /** Abre o cierra la tapa (animación y sonido del cofre / barril). */
    private void openLid(BlockPos pos, boolean open) {
        if (!(this.mob.level() instanceof ServerLevel level)) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ChestBlockEntity) {
            level.blockEvent(pos, level.getBlockState(pos).getBlock(), 1, open ? 1 : 0);
            level.playSound(null, pos, open ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, 1.1F);
        } else if (be instanceof BarrelBlockEntity) {
            level.playSound(null, pos, open ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 0.5F, 1.1F);
        }
    }

    /** Guarda en el cofre lo que no le interesa (sin tocar lo que había). Lo que no entra, al piso. */
    private void storeUnwanted(BlockPos pos) {
        BlockEntity be = this.mob.level().getBlockEntity(pos);
        if (!(be instanceof Container c)) {
            this.dropUnwanted();
            return;
        }
        SimpleContainer inv = this.mob.getPrivateInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || this.mob.appreciates(s)) continue;
            ItemStack rest = insert(c, s.copy());
            inv.setItem(i, rest);
        }
        c.setChanged();
        inv.setChanged();
        this.dropUnwanted();                         // lo que no entró
    }

    /** Solo en lugares libres o completando pilas iguales; nunca reemplaza ni mueve nada. */
    private static ItemStack insert(Container c, ItemStack stack) {
        for (int i = 0; i < c.getContainerSize() && !stack.isEmpty(); i++) {
            ItemStack in = c.getItem(i);
            if (!in.isEmpty() && ItemStack.isSameItemSameComponents(in, stack) && c.canPlaceItem(i, stack)) {
                int move = Math.min(stack.getCount(), Math.min(in.getMaxStackSize(), c.getMaxStackSize()) - in.getCount());
                if (move > 0) {
                    in.grow(move);
                    stack.shrink(move);
                }
            }
        }
        for (int i = 0; i < c.getContainerSize() && !stack.isEmpty(); i++) {
            if (c.getItem(i).isEmpty() && c.canPlaceItem(i, stack)) {
                c.setItem(i, stack.copy());
                stack.setCount(0);
            }
        }
        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    /** Deja en el piso todo lo que no le interesa (nunca se pierde nada). */
    private void dropUnwanted() {
        SimpleContainer inv = this.mob.getPrivateInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || this.mob.appreciates(s)) continue;
            inv.setItem(i, ItemStack.EMPTY);
            ItemEntity drop = new ItemEntity(this.mob.level(), this.mob.getX(), this.mob.getY() + 0.4D, this.mob.getZ(), s);
            drop.setDefaultPickUpDelay();
            this.mob.level().addFreshEntity(drop);
        }
        inv.setChanged();
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
        if (this.phase == Phase.OPEN && !this.done && this.chest != null) {
            // Lo interrumpieron con el cofre abierto: lo cierra.
            this.openLid(this.chest, false);
        }
        this.chest = null;
    }
}
