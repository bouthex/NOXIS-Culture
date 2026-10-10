package com.noxisculture.block.entity;

import com.noxisculture.item.NoxisHatColors;
import net.fabricmc.fabric.api.blockgetter.v2.RenderDataBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Guarda el sombrero EXACTO que está apoyado en el piso (como objeto completo): sus colores de
 * copa y lazo, su desgaste y sus encantamientos. Así, al recogerlo (un jugador o un Noxis) se
 * recupera el mismo sombrero, nunca uno nuevo ni duplicado. El cliente recibe los colores para
 * pintar el bloque.
 */
public class NoxisHatBlockEntity extends BlockEntity implements RenderDataBlockEntity {
    private ItemStack hat = ItemStack.EMPTY;
    /** Ya lo levantaron (un Noxis): al quitar el bloque no se suelta como objeto. */
    private boolean taken;

    public NoxisHatBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NOXIS_HAT, pos, state);
    }

    /** El sombrero apoyado (si el bloque es de antes y no guardaba nada, uno con los colores originales). */
    public ItemStack getHat() {
        return this.hat.isEmpty() ? NoxisHatColors.newHat() : this.hat;
    }

    public void setHat(ItemStack stack) {
        this.hat = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        this.taken = false;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Lo levanta: devuelve el sombrero y marca que ya no está (el bloque se quita aparte). */
    public ItemStack takeHat() {
        ItemStack out = this.getHat().copy();
        this.hat = ItemStack.EMPTY;
        this.taken = true;
        return out;
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.hat.isEmpty()) output.store("hat", ItemStack.CODEC, this.hat);
        output.putBoolean("has", !this.hat.isEmpty());       // nunca vacío: así siempre se sincroniza
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.hat = input.read("hat", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        if (this.level != null && this.level.isClientSide()) {
            // Llegaron los colores: vuelve a pintar el bloque.
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 0);
        }
    }

    /**
     * Antes de quitar el bloque (lo rompió un jugador, una explosión, un pistón...): suelta el
     * sombrero tal cual estaba. Si lo levantó un Noxis, no suelta nada (ya lo tiene puesto).
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (!this.taken && this.level instanceof ServerLevel server) {
            Block.popResource(server, pos, this.getHat().copy());
            this.taken = true;
        }
    }

    /** Colores para pintar el bloque: {copa, lazo}. */
    @Override
    public @Nullable Object getRenderData() {
        ItemStack h = this.getHat();
        return new int[] {NoxisHatColors.crown(h), NoxisHatColors.band(h)};
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }
}
