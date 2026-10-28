package com.noxisculture.block.entity;

import com.noxisculture.block.custom.NoxisDocumentBlock;
import com.noxisculture.document.NoxisDocuments;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
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
 * Guarda cada documento EXACTO que está apoyado (identidad y datos), en orden: en una pila de hojas, el
 * último es el de arriba. Al quitar el bloque suelta esos mismos documentos, una sola vez: nunca se
 * pierden ni se duplican. Se sincroniza con el cliente para poder leerlos.
 */
public class NoxisDocumentBlockEntity extends BlockEntity {
    private final List<ItemStack> documents = new ArrayList<>();

    public NoxisDocumentBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NOXIS_DOCUMENT, pos, state);
    }

    /** El de arriba (o vacío). */
    public ItemStack top() {
        return this.documents.isEmpty() ? ItemStack.EMPTY : this.documents.get(this.documents.size() - 1);
    }

    public int count() {
        return this.documents.size();
    }

    /** Suma un documento arriba (una unidad, copiada tal cual). */
    public void push(ItemStack stack) {
        if (stack.isEmpty()) return;
        this.documents.add(stack.copyWithCount(1));
        this.changed();
    }

    /** Saca el de arriba. Si por algún motivo no hay ninguno guardado, devuelve uno en blanco del mismo tipo. */
    public ItemStack pop(BlockState state) {
        ItemStack out = this.documents.isEmpty()
                ? NoxisDocuments.create(state.getValue(NoxisDocumentBlock.KIND), "", -1L)
                : this.documents.remove(this.documents.size() - 1);
        this.changed();
        return out;
    }

    private void changed() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.documents.isEmpty()) output.store("documents", ItemStack.CODEC.listOf(), List.copyOf(this.documents));
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.documents.clear();
        input.read("documents", ItemStack.CODEC.listOf()).ifPresent(this.documents::addAll);
        // Formato de la versión anterior: un solo documento.
        input.read("document", ItemStack.CODEC).ifPresent(st -> { if (this.documents.isEmpty()) this.documents.add(st); });
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    /** Al quitar el bloque (romperlo, o si desaparece el soporte): suelta cada documento guardado. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level instanceof ServerLevel server) {
            for (ItemStack st : this.documents) Block.popResource(server, pos, st.copy());
            this.documents.clear();
        }
    }
}
