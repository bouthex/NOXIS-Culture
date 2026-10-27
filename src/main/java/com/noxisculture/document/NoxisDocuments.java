package com.noxisculture.document;

import com.noxisculture.block.ModBlocks;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

/**
 * Documentos de los Noxis (libros, cuadernos y hojas): la infraestructura, SIN el idioma.
 *
 * <p>Cada documento es un objeto con su identidad guardada en {@code custom_data}:</p>
 * <pre>
 * {
 *   doc_id:   "uuid"        identidad única (nunca se repite; no se duplica al colocarlo y levantarlo)
 *   kind:     0 libro | 1 cuaderno | 2 hoja
 *   author:   "uuid"        el Noxis que lo escribió (vacío si no se sabe)
 *   created:  tick del mundo
 *   format:   1             versión del formato (para migrar cuando llegue el idioma)
 *   language: "pending"     todavía no hay idioma: las páginas quedan en blanco a propósito
 *   pages:    []            lista vacía: punto de extensión para el contenido futuro
 * }
 * </pre>
 * <p>Colocado en el mundo, el bloque guarda el objeto EXACTO (ver NoxisDocumentBlockEntity), así que
 * al levantarlo se recupera el mismo documento. El idioma, la escritura, el contenido de las páginas
 * y la autoría visible se agregan en una etapa futura: este formato ya deja el lugar.</p>
 */
public final class NoxisDocuments {
    private NoxisDocuments() {}

    public static final int BOOK = 0;
    public static final int NOTEBOOK = 1;
    public static final int SHEET = 2;
    public static final int KINDS = 3;
    public static final int FORMAT = 1;
    private static final String[] KIND_KEYS = {"book", "notebook", "sheet"};

    /** Un documento nuevo (páginas en blanco). */
    public static ItemStack create(int kind, String authorId, long created) {
        ItemStack stack = new ItemStack(ModBlocks.NOXIS_DOCUMENT.asItem());
        int k = Mth.clamp(kind, 0, KINDS - 1);
        CompoundTag tag = new CompoundTag();
        tag.putString("doc_id", UUID.randomUUID().toString());
        tag.putInt("kind", k);
        tag.putString("author", authorId);
        tag.putLong("created", created);
        tag.putInt("format", FORMAT);
        tag.putString("language", "pending");
        tag.put("pages", new ListTag());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        decorate(stack, k);
        return stack;
    }

    /** Nombre y dibujo según el tipo (libro, cuaderno u hoja). */
    public static void decorate(ItemStack stack, int kind) {
        int k = Mth.clamp(kind, 0, KINDS - 1);
        stack.set(DataComponents.ITEM_NAME, Component.translatable("item.noxis_culture.noxis_document." + KIND_KEYS[k]));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of((float) k), List.of(), List.of(), List.of()));
    }

    public static int kindOf(ItemStack stack) {
        CustomModelData data = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (data != null && !data.floats().isEmpty()) return Mth.clamp(Math.round(data.floats().get(0)), 0, KINDS - 1);
        return BOOK;
    }

    public static boolean isDocument(ItemStack stack) {
        return stack.is(ModBlocks.NOXIS_DOCUMENT.asItem());
    }
}
