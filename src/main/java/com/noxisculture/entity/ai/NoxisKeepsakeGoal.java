package com.noxisculture.entity.ai;

import com.noxisculture.entity.idle.NoxisKeepsakeAnimation;
import com.noxisculture.sound.ModSounds;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

/**
 * Recordar y contemplar sus tesoros (servidor): muy de vez en cuando, un Noxis tranquilo saca un
 * objeto querido de su inventario privado (si se lo regalaron, lo prefiere), lo sostiene, lo mira
 * con ternura y lo vuelve a guardar. Si es un regalo, aparecen corazoncitos, como si recordara el
 * gesto. Los artistas y cariñosos lo hacen más seguido; los traviesos, menos.
 *
 * <p>El objeto NUNCA sale del inventario: lo que se ve en sus manitos es solo una imagen. Así no
 * se puede perder ni duplicar aunque algo lo interrumpa (se corta suave y listo).</p>
 */
public class NoxisKeepsakeGoal<T extends PathfinderMob & NoxisKeeper> extends Goal {
    private static final int COOLDOWN = 3600;          // 3 min ...
    private static final int COOLDOWN_RANDOM = 4800;   // ... a 7 min

    private final T mob;
    private int nextCheckTick;
    private long nextAllowedTime = -1L;
    private int slot = -1;
    private ItemStack shown = ItemStack.EMPTY;
    private boolean gift;
    private int ticks;

    public NoxisKeepsakeGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.nextCheckTick = mob.getRandom().nextInt(200);
    }

    @Override
    public boolean canUse() {
        if (this.mob.tickCount < this.nextCheckTick) return false;
        RandomSource random = this.mob.getRandom();
        this.nextCheckTick = this.mob.tickCount + 80 + random.nextInt(60);
        long now = this.mob.level().getGameTime();
        if (this.nextAllowedTime < 0L) this.nextAllowedTime = now + 2400 + random.nextInt(3600);   // el primero: 2 a 5 min
        if (now < this.nextAllowedTime || !this.mob.canHandleKeepsakes() || !this.mob.getNavigation().isDone()) return false;
        if (random.nextFloat() >= 0.25F * this.mob.getPersonality().keepsakeWeight()) return false;
        // Elige un tesoro: casi siempre prefiere un regalo (si tiene).
        SimpleContainer inv = this.mob.getPrivateInventory();
        List<Integer> gifts = new ArrayList<>();
        List<Integer> others = new ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || !this.mob.appreciates(s)) continue;
            (this.mob.isGiftedItem(s.getItem()) ? gifts : others).add(i);
        }
        List<Integer> pool = !gifts.isEmpty() && (others.isEmpty() || random.nextFloat() < 0.7F) ? gifts : others;
        if (pool.isEmpty()) return false;
        this.slot = pool.get(random.nextInt(pool.size()));
        this.gift = pool == gifts;
        return true;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.shown = this.mob.getPrivateInventory().getItem(this.slot).copyWithCount(1);
        this.mob.getNavigation().stop();
        this.mob.setKeepsakeAnim(NoxisKeepsakeAnimation.CONTEMPLATE);
    }

    @Override
    public boolean canContinueToUse() {
        if (this.ticks >= NoxisKeepsakeAnimation.CONTEMPLATE_LENGTH || this.mob.hurtTime > 0) return false;
        if (!this.mob.canHandleKeepsakes()) return false;
        // Si el tesoro ya no está en su lugar (no debería pasar), termina sin más.
        ItemStack now = this.mob.getPrivateInventory().getItem(this.slot);
        return !now.isEmpty() && ItemStack.isSameItem(now, this.shown);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.ticks++;
        this.mob.getNavigation().stop();
        if (this.ticks == NoxisKeepsakeAnimation.TAKE_OUT_TICK) this.mob.showKeepsake(this.shown);
        if (this.ticks == NoxisKeepsakeAnimation.LOVE_TICK) {
            this.mob.playSound(ModSounds.NOXIS_HAPPY, 0.4F, 1.3F);
            if (this.gift && this.mob.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.HEART, this.mob.getX(), this.mob.getEyeY() + 0.4D, this.mob.getZ(),
                        2, 0.2D, 0.1D, 0.2D, 0.0D);
            }
        }
        if (this.ticks == NoxisKeepsakeAnimation.PUT_AWAY_TICK) this.mob.showKeepsake(ItemStack.EMPTY);
    }

    @Override
    public void stop() {
        this.mob.showKeepsake(ItemStack.EMPTY);
        this.mob.setKeepsakeAnim(NoxisKeepsakeAnimation.NONE);
        this.nextAllowedTime = this.mob.level().getGameTime() + COOLDOWN + this.mob.getRandom().nextInt(COOLDOWN_RANDOM);
        this.shown = ItemStack.EMPTY;
        this.slot = -1;
    }
}
