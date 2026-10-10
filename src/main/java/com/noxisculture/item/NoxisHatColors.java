package com.noxisculture.item;

import com.noxisculture.block.ModBlocks;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.DyedItemColor;
import org.jspecify.annotations.Nullable;

/**
 * Colores del sombrero Noxis. Cada sombrero guarda los suyos en el propio objeto:
 * <ul>
 *   <li><b>Copa y ala</b>: el componente vanilla {@code minecraft:dyed_color}, el mismo de las
 *   armaduras de cuero (así también funciona la receta vanilla de teñido en la mesa de crafteo).</li>
 *   <li><b>Lazo</b>: el primer color de {@code minecraft:custom_model_data}, independiente del
 *   de la copa.</li>
 * </ul>
 * La gema nunca se tiñe. Sin tinte, el sombrero se ve con sus colores originales (negro y violeta).
 *
 * <p>La mezcla de tintes es la misma que la de las armaduras de cuero de Java: se promedian los
 * colores (el que ya tenía más los tintes nuevos) y se corrige el brillo. Así se pueden lograr
 * miles de tonos combinando tintes, tanto en la copa como en el lazo.</p>
 */
public final class NoxisHatColors {
    /** Color base de la copa y el ala sin teñir (con la máscara da el negro original). */
    public static final int DEFAULT_CROWN = 0x231C2C;
    /** Color base del lazo sin teñir (con la máscara da el violeta original). */
    public static final int DEFAULT_BAND = 0xD146FF;

    /**
     * Cada tinte con su etiqueta convencional (c:white_dyes, c:red_dyes...): sirven los tintes
     * vanilla y también los de otros mods que usen esas etiquetas.
     */
    private static final Map<DyeColor, TagKey<Item>> DYES = new EnumMap<>(DyeColor.class);

    static {
        DYES.put(DyeColor.WHITE, ConventionalItemTags.WHITE_DYES);
        DYES.put(DyeColor.ORANGE, ConventionalItemTags.ORANGE_DYES);
        DYES.put(DyeColor.MAGENTA, ConventionalItemTags.MAGENTA_DYES);
        DYES.put(DyeColor.LIGHT_BLUE, ConventionalItemTags.LIGHT_BLUE_DYES);
        DYES.put(DyeColor.YELLOW, ConventionalItemTags.YELLOW_DYES);
        DYES.put(DyeColor.LIME, ConventionalItemTags.LIME_DYES);
        DYES.put(DyeColor.PINK, ConventionalItemTags.PINK_DYES);
        DYES.put(DyeColor.GRAY, ConventionalItemTags.GRAY_DYES);
        DYES.put(DyeColor.LIGHT_GRAY, ConventionalItemTags.LIGHT_GRAY_DYES);
        DYES.put(DyeColor.CYAN, ConventionalItemTags.CYAN_DYES);
        DYES.put(DyeColor.PURPLE, ConventionalItemTags.PURPLE_DYES);
        DYES.put(DyeColor.BLUE, ConventionalItemTags.BLUE_DYES);
        DYES.put(DyeColor.BROWN, ConventionalItemTags.BROWN_DYES);
        DYES.put(DyeColor.GREEN, ConventionalItemTags.GREEN_DYES);
        DYES.put(DyeColor.RED, ConventionalItemTags.RED_DYES);
        DYES.put(DyeColor.BLACK, ConventionalItemTags.BLACK_DYES);
    }

    private NoxisHatColors() {
    }

    /** Un sombrero nuevo, con sus colores originales. */
    public static ItemStack newHat() {
        return new ItemStack(ModBlocks.NOXIS_HAT);
    }

    /** El tinte vanilla que es este objeto, o null si no es un tinte. */
    public static @Nullable DyeColor dyeOf(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (Map.Entry<DyeColor, TagKey<Item>> e : DYES.entrySet()) {
            if (stack.is(e.getValue())) return e.getKey();
        }
        return null;
    }

    // ------------------------------------------------------------------ leer

    public static boolean hasCrownColor(ItemStack hat) {
        return hat.get(DataComponents.DYED_COLOR) != null;
    }

    public static boolean hasBandColor(ItemStack hat) {
        CustomModelData data = hat.get(DataComponents.CUSTOM_MODEL_DATA);
        return data != null && !data.colors().isEmpty();
    }

    /** Color de la copa y el ala (RGB), o el original si no está teñido. */
    public static int crown(ItemStack hat) {
        DyedItemColor dyed = hat.get(DataComponents.DYED_COLOR);
        return dyed != null ? dyed.rgb() & 0xFFFFFF : DEFAULT_CROWN;
    }

    /** Color del lazo (RGB), o el original si no está teñido. */
    public static int band(ItemStack hat) {
        CustomModelData data = hat.get(DataComponents.CUSTOM_MODEL_DATA);
        if (data != null && !data.colors().isEmpty()) return data.colors().get(0) & 0xFFFFFF;
        return DEFAULT_BAND;
    }

    // ------------------------------------------------------------------ teñir

    /** Tiñe la copa y el ala con un tinte más (mezcla como el cuero). */
    public static void dyeCrown(ItemStack hat, DyeColor dye) {
        Integer current = hasCrownColor(hat) ? crown(hat) : null;
        hat.set(DataComponents.DYED_COLOR, new DyedItemColor(mix(current, List.of(dye))));
    }

    /** Tiñe el lazo con un tinte más (mezcla como el cuero), sin tocar la copa. */
    public static void dyeBand(ItemStack hat, DyeColor dye) {
        Integer current = hasBandColor(hat) ? band(hat) : null;
        setBand(hat, mix(current, List.of(dye)));
    }

    private static void setBand(ItemStack hat, int rgb) {
        CustomModelData old = hat.get(DataComponents.CUSTOM_MODEL_DATA);
        List<Integer> colors = new ArrayList<>();
        colors.add(rgb);
        if (old != null && old.colors().size() > 1) colors.addAll(old.colors().subList(1, old.colors().size()));
        hat.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                old != null ? old.floats() : List.of(),
                old != null ? old.flags() : List.of(),
                old != null ? old.strings() : List.of(),
                colors));
    }

    /**
     * Mezcla de tintes de las armaduras de cuero de Java: promedia los colores (el actual, si
     * hay, más cada tinte) y escala el resultado para conservar el brillo promedio.
     */
    public static int mix(@Nullable Integer current, List<DyeColor> dyes) {
        int r = 0, g = 0, b = 0, maxSum = 0, count = 0;
        if (current != null) {
            int cr = (current >> 16) & 0xFF, cg = (current >> 8) & 0xFF, cb = current & 0xFF;
            r += cr; g += cg; b += cb;
            maxSum += Math.max(cr, Math.max(cg, cb));
            count++;
        }
        for (DyeColor dye : dyes) {
            int c = dye.getTextureDiffuseColor();
            int dr = (c >> 16) & 0xFF, dg = (c >> 8) & 0xFF, db = c & 0xFF;
            r += dr; g += dg; b += db;
            maxSum += Math.max(dr, Math.max(dg, db));
            count++;
        }
        if (count == 0) return current != null ? current : 0xFFFFFF;
        int ar = r / count, ag = g / count, ab = b / count;
        float avgMax = (float) maxSum / (float) count;
        float max = Math.max(ar, Math.max(ag, ab));
        if (max > 0.0F) {
            ar = (int) (ar * avgMax / max);
            ag = (int) (ag * avgMax / max);
            ab = (int) (ab * avgMax / max);
        }
        return (Math.min(255, ar) << 16) | (Math.min(255, ag) << 8) | Math.min(255, ab);
    }

    // ------------------------------------------------------------------ gustos de los Noxis

    /** ¿Tiene algo violeta (copa o lazo)? Los Noxis lo prefieren, pero nunca rechazan otro color. */
    public static boolean looksViolet(ItemStack hat) {
        return isViolet(crown(hat)) || isViolet(band(hat));
    }

    private static boolean isViolet(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255.0F, g = ((rgb >> 8) & 0xFF) / 255.0F, b = (rgb & 0xFF) / 255.0F;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        if (max < 0.25F || delta / max < 0.3F) return false;          // muy oscuro o grisáceo
        float hue;
        if (max == r) hue = 60.0F * (((g - b) / delta) % 6.0F);
        else if (max == g) hue = 60.0F * (((b - r) / delta) + 2.0F);
        else hue = 60.0F * (((r - g) / delta) + 4.0F);
        if (hue < 0.0F) hue += 360.0F;
        return hue >= 260.0F && hue <= 320.0F;
    }
}
