package com.noxisculture.art;

import com.noxisculture.block.ModBlocks;
import com.noxisculture.item.ModDataComponents;
import com.noxisculture.world.ModPoi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

/**
 * La colección de pinturas de los Noxis artistas.
 *
 * <p>Cada obra es un diseño original del mod (pixel art de 14x14 dentro de su marco, en
 * {@code textures/block/noxis_painting/art_NN.png}). La identidad de una obra es su número: viaja en
 * el objeto (componente {@code noxis_culture:painting_art}) y en el bloque colgado o en el atril
 * (propiedad {@code art}). En la mano y en el inventario todas las pinturas se ven iguales (ícono
 * genérico): la obra se ve al colocarla.</p>
 * <p>Pinturas sin descubrir: sin obra todavía; se apilan normalmente y la obra se sortea al colocarlas
 * por primera vez ({@link #discover}). Las pinturas que termina un Noxis nacen con su obra. Una obra
 * asignada no se vuelve a sortear nunca.</p>
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

    /** Una pintura sin descubrir (todavía sin obra): se apila con las demás sin descubrir. */
    public static ItemStack blank() {
        return new ItemStack(ModBlocks.NOXIS_PAINTING.asItem());
    }

    /** Una pintura con su obra ya asignada (la termina un Noxis, se retira de una pared o de un atril). */
    public static ItemStack stack(int art) {
        ItemStack stack = blank();
        setArt(stack, art);
        return stack;
    }

    /** Asigna la obra. Solo se usa con pinturas que todavía no tienen una: nunca se vuelve a sortear. */
    public static void setArt(ItemStack stack, int art) {
        stack.set(ModDataComponents.PAINTING_ART, Mth.clamp(art, 0, COUNT - 1));
    }

    /** ¿Ya tiene obra? (también reconoce las pinturas de la versión anterior, que la guardaban en custom_model_data). */
    public static boolean hasArt(ItemStack stack) {
        return stack.has(ModDataComponents.PAINTING_ART) || legacyArt(stack) >= 0;
    }

    /** El número de la obra, o -1 si la pintura todavía no fue descubierta. */
    public static int artOf(ItemStack stack) {
        Integer art = stack.get(ModDataComponents.PAINTING_ART);
        if (art != null) return Mth.clamp(art, 0, COUNT - 1);
        return legacyArt(stack);
    }

    private static int legacyArt(ItemStack stack) {
        CustomModelData data = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (data != null && !data.floats().isEmpty()) return Mth.clamp(Math.round(data.floats().get(0)), 0, COUNT - 1);
        return -1;
    }

    /**
     * Pasa una pintura de la versión anterior al formato actual (misma obra), para que se apile con las
     * demás de su obra. No cambia nada si ya está en el formato actual o no tiene obra.
     */
    public static void migrateLegacy(ItemStack stack) {
        if (stack.has(ModDataComponents.PAINTING_ART)) return;
        int art = legacyArt(stack);
        if (art < 0) return;
        stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        stack.remove(DataComponents.CUSTOM_DATA);
        setArt(stack, art);
    }

    /**
     * Sortea la obra de una pintura sin descubrir que se coloca por primera vez: cualquiera de la
     * colección, evitando (si se puede) las que ya cuelgan cerca, para que descubrir sea más lindo.
     */
    public static int discover(ServerLevel level, BlockPos pos, RandomSource random) {
        java.util.Set<Integer> near = new java.util.HashSet<>();
        ModPoi.artworksNear(level, pos, 16).forEach(p -> {
            int a = com.noxisculture.block.custom.NoxisPaintingBlock.artAt(level.getBlockState(p));
            if (a >= 0) near.add(a);
        });
        int[] options = new int[COUNT];
        int n = 0;
        for (int i = 0; i < COUNT; i++) if (!near.contains(i)) options[n++] = i;
        return n == 0 ? random.nextInt(COUNT) : options[random.nextInt(n)];
    }

    public static boolean isPainting(ItemStack stack) {
        return stack.is(ModBlocks.NOXIS_PAINTING.asItem());
    }
}
