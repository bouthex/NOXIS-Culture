package com.noxisculture.art;

import com.noxisculture.block.ModBlocks;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

/**
 * La colección de pinturas de los Noxis artistas.
 *
 * <p>Cada obra es un diseño original del mod (pixel art de 16x16 con su marco, en
 * {@code textures/block/noxis_painting/art_NN.png}). La identidad de una obra es su número: viaja en
 * el objeto ({@code custom_model_data}, que además elige el dibujo del ícono) y en el bloque colgado o
 * apoyado (propiedad {@code art}). Así nunca cambia al guardarla, llevarla, colgarla o recargar el
 * mundo, y no se guarda ninguna imagen repetida.</p>
 *
 * <p>Las obras se agrupan en {@link #FAMILIES} familias de estilo (solo para organizar: cada artista
 * tiene una familia preferida y un repertorio reconocible):</p>
 * <ol start="0">
 *   <li>recuerdos tiernos: flores, bichitos de luz, rincones cálidos, retratos;</li>
 *   <li>paisajes grises y gastados, con restos de luz;</li>
 *   <li>paisajes y viajes: colinas, montañas, mar, bosque, desierto, Nether, End, noches;</li>
 *   <li>arquitectura y lugares;</li>
 *   <li>formas inquietantes: sombras, figuras sin rasgos, grietas de luz;</li>
 *   <li>ceremonial: simetría, formas cristalinas, la gema en forma de lágrima.</li>
 * </ol>
 * <p>Para sumar obras: agregar la textura, sumar su familia al final de {@link #FAMILY_OF} y regenerar
 * los modelos (ver docs/PINTURAS.md). Los números existentes nunca se reordenan.</p>
 */
public final class NoxisPaintings {
    private NoxisPaintings() {}

    public static final int FAMILIES = 6;
    /** Familia de cada obra (el índice es el número de la obra). */
    private static final int[] FAMILY_OF = {
            0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2,
            3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5,
            0, 2, 2, 4, 3
    };
    public static final int COUNT = FAMILY_OF.length;

    public static int family(int art) {
        return FAMILY_OF[Mth.clamp(art, 0, COUNT - 1)];
    }

    /**
     * Elige qué va a pintar un artista: casi siempre de su familia (su estilo), a veces de otra;
     * nunca la misma obra que la última vez.
     */
    public static int pick(RandomSource random, int style, int last) {
        int family = random.nextInt(5) == 0 ? random.nextInt(FAMILIES) : Mth.clamp(style, 0, FAMILIES - 1);
        int[] options = new int[COUNT];
        int n = 0;
        for (int i = 0; i < COUNT; i++) if (FAMILY_OF[i] == family && i != last) options[n++] = i;
        if (n == 0) return random.nextInt(COUNT);
        return options[random.nextInt(n)];
    }

    /** Una pintura terminada como objeto (con su número y quién la pintó, para recordarlo). */
    public static ItemStack stack(int art, String painterId, long day) {
        ItemStack stack = new ItemStack(ModBlocks.NOXIS_PAINTING.asItem());
        setArt(stack, art);
        CompoundTag tag = new CompoundTag();
        tag.putInt("art", art);
        if (!painterId.isEmpty()) tag.putString("painter", painterId);
        if (day >= 0L) tag.putLong("day", day);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static void setArt(ItemStack stack, int art) {
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of((float) Mth.clamp(art, 0, COUNT - 1)), List.of(), List.of(), List.of()));
    }

    /** El número de la obra de ese objeto (0 si no tiene: por ejemplo, una sacada del creativo antigua). */
    public static int artOf(ItemStack stack) {
        CustomModelData data = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (data != null && !data.floats().isEmpty()) return Mth.clamp(Math.round(data.floats().get(0)), 0, COUNT - 1);
        return 0;
    }

    public static boolean isPainting(ItemStack stack) {
        return stack.is(ModBlocks.NOXIS_PAINTING.asItem());
    }
}
