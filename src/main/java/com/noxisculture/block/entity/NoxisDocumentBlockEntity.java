package com.noxisculture.block.entity;

import com.noxisculture.document.NoxisDocuments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Guarda el documento EXACTO que está apoyado (con su identidad y sus datos). Al quitar el bloque
 * suelta ese mismo documento, una sola vez: nunca se pierde ni se duplica.
 */
public class NoxisDocumentBlockEntity extends BlockEntity {
    private ItemStack document = ItemStack.EMPTY;
    private boolean dropped;

    public NoxisDocumentBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NOXIS_DOCUMENT, pos, state);
    }

    public ItemStack getDocument() {
        return this.document;
    }

    public void setDocument(ItemStack stack) {
        this.document = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        this.dropped = false;
        this.setChanged();
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.document.isEmpty()) output.store("document", ItemStack.CODEC, this.document);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.document = input.read("document", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (!this.dropped && this.level instanceof ServerLevel server) {
            ItemStack out = this.document.isEmpty()
                    ? NoxisDocuments.create(state.getValue(com.noxisculture.block.custom.NoxisDocumentBlock.KIND), "", -1L)
                    : this.document.copy();
            Block.popResource(server, pos, out);
            this.dropped = true;
        }
    }
}
