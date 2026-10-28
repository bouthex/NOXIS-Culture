package com.noxisculture.item;

import com.noxisculture.NoxisCulture;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;

/**
 * Componentes de datos propios del mod.
 *
 * <p>{@link #PAINTING_ART}: la obra de una pintura Noxis. Si el objeto NO lo tiene, la pintura todavía
 * no fue descubierta: todas las pinturas sin descubrir son idénticas y se apilan con el comportamiento
 * normal de Minecraft. La obra se asigna una sola vez (al colocarla por primera vez, o cuando la termina
 * un Noxis artista) y desde ahí nunca cambia; las pinturas con obra se apilan solo con la misma obra.</p>
 */
public final class ModDataComponents {
    private ModDataComponents() {}

    public static final DataComponentType<Integer> PAINTING_ART = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE, NoxisCulture.id("painting_art"),
            DataComponentType.<Integer>builder()
                    .persistent(ExtraCodecs.NON_NEGATIVE_INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static void initialize() {}
}
