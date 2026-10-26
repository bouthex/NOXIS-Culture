package com.noxisculture.entity.nature;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * La flor que un Noxis tiene en la mano (servidor), con su lugar y estado originales para
 * devolverla EXACTAMENTE como estaba. Nunca la duplica ni la pierde:
 * <ul>
 *   <li>Al recogerla, el bloque desaparece y la flor pasa a la mano (una sola copia).</li>
 *   <li>Al devolverla, solo la planta si el lugar sigue vacío (aire) y el suelo la sostiene;
 *       nunca pisa un bloque que alguien haya puesto mientras tanto.</li>
 *   <li>Si no se puede devolver de forma segura, la deja como objeto en el piso.</li>
 *   <li>Si la regala, esa misma flor pasa a ser el objeto que lanza (no se replanta).</li>
 *   <li>Se guarda en el NBT del Noxis: si el chunk se descarga con la flor en la mano, al
 *       volver la devuelve (o la suelta como objeto).</li>
 * </ul>
 */
public final class NoxisFlowerCarry {
    private ItemStack stack = ItemStack.EMPTY;
    private @Nullable BlockPos origin;
    private @Nullable BlockState originalState;
    /** true si se cargó del disco con una flor en la mano (hay que resolverla). */
    private boolean orphan;

    public boolean isHolding() {
        return !this.stack.isEmpty();
    }

    public ItemStack getStack() {
        return this.stack;
    }

    public @Nullable BlockPos getOrigin() {
        return this.origin;
    }

    public boolean isOrphan() {
        return this.orphan;
    }

    /** Recoge la flor de ese lugar. Devuelve false si ahí ya no hay una flor recogible. */
    public boolean pick(Level level, BlockPos pos) {
        if (this.isHolding()) return false;
        BlockState state = level.getBlockState(pos);
        if (!NoxisFlowers.isPickable(state)) return false;
        ItemStack item = new ItemStack(state.getBlock().asItem());
        if (item.isEmpty()) return false;
        if (!level.removeBlock(pos, false)) return false;
        level.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.NEUTRAL, 0.5F, 1.3F);
        this.stack = item;
        this.origin = pos.immutable();
        this.originalState = state;
        this.orphan = false;
        return true;
    }

    /** ¿Se puede volver a plantar ahí sin pisar nada? */
    public boolean canReplant(Level level) {
        if (!this.isHolding() || this.origin == null || this.originalState == null) return false;
        if (!level.isLoaded(this.origin)) return false;
        return level.getBlockState(this.origin).isAir() && this.originalState.canSurvive(level, this.origin);
    }

    /** La vuelve a plantar en su lugar, tal como estaba. Devuelve false si no fue seguro. */
    public boolean replant(Level level) {
        if (!this.canReplant(level)) return false;
        BlockPos pos = this.origin;
        BlockState state = this.originalState;
        level.setBlock(pos, state, Block.UPDATE_ALL);
        level.playSound(null, pos, state.getSoundType().getPlaceSound(), SoundSource.NEUTRAL, 0.5F, 1.25F);
        this.clear();
        return true;
    }

    /** La devuelve si se puede; si no, la deja como objeto en el piso (nunca se pierde). */
    public void returnOrDrop(Level level, Entity holder) {
        if (!this.isHolding()) return;
        if (!this.replant(level)) this.drop(level, holder);
    }

    /**
     * La entrega como regalo: devuelve LA flor (la misma, una sola) y la mano queda vacía.
     * Quien la recibe la convierte en objeto; el lugar original queda libre.
     */
    public ItemStack giveAway() {
        ItemStack gift = this.stack;
        this.clear();
        return gift;
    }

    /** La deja como objeto en el piso, al lado del Noxis. */
    public void drop(Level level, Entity holder) {
        if (!this.isHolding()) return;
        if (level instanceof ServerLevel) {
            Block.popResource(level, holder.blockPosition(), this.stack.copy());
        }
        this.clear();
    }

    private void clear() {
        this.stack = ItemStack.EMPTY;
        this.origin = null;
        this.originalState = null;
        this.orphan = false;
    }

    public void save(ValueOutput output) {
        if (!this.isHolding() || this.origin == null || this.originalState == null) return;
        output.store("noxis_flower", ItemStack.CODEC, this.stack);
        output.store("noxis_flower_pos", BlockPos.CODEC, this.origin);
        output.store("noxis_flower_state", BlockState.CODEC, this.originalState);
    }

    public void load(ValueInput input) {
        this.clear();
        ItemStack loaded = input.read("noxis_flower", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        if (loaded.isEmpty()) return;
        this.stack = loaded;
        this.origin = input.read("noxis_flower_pos", BlockPos.CODEC).orElse(null);
        this.originalState = input.read("noxis_flower_state", BlockState.CODEC).orElse(null);
        this.orphan = true;
    }
}
