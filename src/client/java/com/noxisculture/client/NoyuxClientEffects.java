package com.noxisculture.client;

import com.noxisculture.block.ModBlocks;
import com.noxisculture.fluid.ModFluidTags;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Efectos de pantalla del Noyux (solo cliente): niebla violeta con la cámara sumergida y llamas
 * violetas en primera persona mientras el jugador se quema con Noyux o con su fuego.
 * No cambian nada de la lava, del agua ni del fuego normales.
 */
public final class NoyuxClientEffects {
    private NoyuxClientEffects() {}

    /** Color de la niebla dentro del Noyux (rojo, verde, azul): violeta oscuro y espeso. */
    public static final float FOG_R = 0.30F, FOG_G = 0.06F, FOG_B = 0.52F;

    /** ¿La cámara está debajo de la superficie de un Noyux? (mismo criterio que usa el juego para el agua y la lava). */
    public static boolean isCameraInNoyux(Camera camera, Level level) {
        BlockPos pos = camera.blockPosition();
        FluidState fluid = level.getFluidState(pos);
        return fluid.is(ModFluidTags.NOYUX) && camera.position().y < pos.getY() + fluid.getHeight(level, pos);
    }

    /** ¿El jugador local toca Noyux o fuego de Noyux? (solo cuando de verdad lo quema: ni creativo ni espectador). */
    public static boolean isPlayerBurningInNoyux() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isCreative() || player.isSpectator()) return false;
        Level level = player.level();
        AABB box = player.getBoundingBox().deflate(0.001D);
        int x0 = (int) Math.floor(box.minX), x1 = (int) Math.floor(box.maxX);
        int y0 = (int) Math.floor(box.minY), y1 = (int) Math.floor(box.maxY);
        int z0 = (int) Math.floor(box.minZ), z1 = (int) Math.floor(box.maxZ);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    pos.set(x, y, z);
                    if (level.getBlockState(pos).is(ModBlocks.NOYUX_FIRE)) return true;
                    FluidState fluid = level.getFluidState(pos);
                    if (fluid.is(ModFluidTags.NOYUX) && box.minY < y + fluid.getHeight(level, pos)) return true;
                }
            }
        }
        return false;
    }

    /** La textura animada del fuego violeta (la misma del bloque, así se mueve igual). */
    public static @Nullable TextureAtlasSprite fireSprite() {
        try {
            return Minecraft.getInstance().getModelManager().getBlockStateModelSet()
                    .getParticleMaterial(ModBlocks.NOYUX_FIRE.defaultBlockState()).sprite();
        } catch (RuntimeException e) {
            return null;                       // si algo no está listo todavía, queda el fuego de siempre
        }
    }
}
