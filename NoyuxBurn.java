package com.noxisculture.block.custom;

import com.noxisculture.block.ModBlocks;
import com.noxisculture.fluid.ModFluidTags;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * Quemadura residual del Noyux (servidor).
 *
 * <p>Cuando el Noyux (líquido o fuego violeta) le hace daño de verdad a una criatura, queda
 * "quemándose" un instante después de salir: <b>{@link #RESIDUAL_TICKS} ticks (1,5 s)</b> y un único
 * golpe de <b>{@link #RESIDUAL_DAMAGE} de daño mágico</b> (1 corazón) un segundo después del último
 * contacto. Antes no había quemadura residual (0 s, 0 de daño).</p>
 *
 * <ul>
 *   <li>Si sigue en contacto, el daño lo pone la fuente (líquido o fuego); el residual no se suma.</li>
 *   <li>El agua la apaga al instante, sin daño.</li>
 *   <li>La lava vanilla la reemplaza: se borra el residual violeta y la lava quema con sus reglas de
 *   siempre (no se toca el fuego vanilla ni se da inmunidad).</li>
 *   <li>El waterdrop rápido sigue funcionando: si el Noyux no llegó a hacer daño, no hay residual.</li>
 * </ul>
 */
public final class NoyuxBurn {
    private NoyuxBurn() {}

    public static final int RESIDUAL_TICKS = 30;
    public static final float RESIDUAL_DAMAGE = 2.0F;
    private static final int RESIDUAL_HIT_AT = 20;

    /** Última vez que el Noyux le hizo daño (tiempo del mundo). Claves débiles: se limpian solas. */
    private static final Map<LivingEntity, Long> LAST_HIT = new WeakHashMap<>();

    /** Lo llaman el líquido y el fuego cuando su daño se aplicó. */
    public static void touched(LivingEntity living, ServerLevel level) {
        LAST_HIT.put(living, level.getGameTime());
    }

    /** Fin de cada tick del mundo (registrado en NoxisCulture). Solo recorre a los que se quemaron. */
    public static void tick(ServerLevel level) {
        if (LAST_HIT.isEmpty()) return;
        long now = level.getGameTime();
        Iterator<Map.Entry<LivingEntity, Long>> it = LAST_HIT.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<LivingEntity, Long> entry = it.next();
            LivingEntity e = entry.getKey();
            if (e == null || e.isRemoved() || !e.isAlive()) {
                it.remove();
                continue;
            }
            if (e.level() != level) continue;
            long since = now - entry.getValue();
            if (since > RESIDUAL_TICKS || since < 0) {
                it.remove();
                continue;
            }
            // El agua la apaga; la lava vanilla toma el control con sus propias reglas.
            if (e.isInWater() || touchesVanillaLava(level, e)) {
                it.remove();
                continue;
            }
            if (since == RESIDUAL_HIT_AT && !inNoyux(level, e)) {
                e.hurtServer(level, level.damageSources().magic(), RESIDUAL_DAMAGE);
            }
        }
    }

    /** ¿Sigue tocando Noyux o fuego violeta? (entonces el daño lo pone la fuente). */
    public static boolean inNoyux(ServerLevel level, LivingEntity e) {
        AABB box = e.getBoundingBox().deflate(1.0E-3D);
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            if (level.getFluidState(p).is(ModFluidTags.NOYUX) || level.getBlockState(p).is(ModBlocks.NOYUX_FIRE)) return true;
        }
        return false;
    }

    private static boolean touchesVanillaLava(ServerLevel level, LivingEntity e) {
        AABB box = e.getBoundingBox().deflate(1.0E-3D);
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            if (NoyuxReactions.isLava(level.getFluidState(p))) return true;
        }
        return false;
    }
}
