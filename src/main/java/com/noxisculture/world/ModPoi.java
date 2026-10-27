package com.noxisculture.world;

import com.noxisculture.NoxisCulture;
import com.noxisculture.block.ModBlocks;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PoiHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;

/**
 * Puntos de interés (el mismo sistema que usan los aldeanos para camas y mesas de trabajo): las
 * pinturas y los documentos colocados quedan registrados por el juego y se guardan con el mundo.
 * Así un Noxis encuentra obras cercanas sin revisar bloque por bloque, y se puede limitar cuántas
 * hay en una zona.
 */
public final class ModPoi {
    private ModPoi() {}

    public static final ResourceKey<PoiType> PAINTING_KEY = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, NoxisCulture.id("noxis_painting"));
    public static final ResourceKey<PoiType> DOCUMENT_KEY = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, NoxisCulture.id("noxis_document"));

    public static final PoiType PAINTING = PoiHelper.register(NoxisCulture.id("noxis_painting"), 0, 1, ModBlocks.NOXIS_PAINTING);
    public static final PoiType DOCUMENT = PoiHelper.register(NoxisCulture.id("noxis_document"), 0, 1, ModBlocks.NOXIS_DOCUMENT);

    /** Pinturas colocadas a menos de {@code radius} bloques. */
    public static Stream<BlockPos> paintingsNear(ServerLevel level, BlockPos center, int radius) {
        return level.getPoiManager().getInRange(h -> h.is(PAINTING_KEY), center, radius, PoiManager.Occupancy.ANY)
                .map(PoiRecord::getPos);
    }

    /** Documentos colocados a menos de {@code radius} bloques. */
    public static Stream<BlockPos> documentsNear(ServerLevel level, BlockPos center, int radius) {
        return level.getPoiManager().getInRange(h -> h.is(DOCUMENT_KEY), center, radius, PoiManager.Occupancy.ANY)
                .map(PoiRecord::getPos);
    }

    public static void initialize() {}
}
