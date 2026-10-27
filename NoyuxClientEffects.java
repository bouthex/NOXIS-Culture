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

    /** Distancia de las muestras alrededor de la cámara (cubre el plano cercano, como hace el juego). */
    private static final double NEAR = 0.15D;

    /**
     * ¿La cámara está (aunque sea en parte) dentro del Noyux? El juego decide la niebla de lava mirando
     * las esquinas del plano cercano de la cámara, no solo su centro: por eso, al entrar despacio,
     * había un instante rojo. Acá se mira el centro y puntos alrededor; si alguno queda debajo de la
     * superficie de un Noyux y ninguno toca lava de verdad, la niebla es violeta. Se calcula en cada
     * cuadro, así que nunca queda trabado.
     */
    public static boolean isCameraInNoyux(Camera camera, Level level) {
        net.minecraft.world.phys.Vec3 c = camera.position();
        boolean noyux = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < SAMPLES.length; i += 3) {
            double x = c.x + SAMPLES[i] * NEAR, y = c.y + SAMPLES[i + 1] * NEAR, z = c.z + SAMPLES[i + 2] * NEAR;
            pos.set(Math.floor(x), Math.floor(y), Math.floor(z));
            FluidState fluid = level.getFluidState(pos);
            if (fluid.isEmpty() || y >= pos.getY() + fluid.getHeight(level, pos)) continue;
            if (com.noxisculture.block.custom.NoyuxReactions.isLava(fluid)) return false;   // lava de verdad: la de siempre
            if (fluid.is(ModFluidTags.NOYUX)) noyux = true;
        }
        return noyux;
    }

    /** Centro y puntos alrededor (x, y, z) en unidades de {@link #NEAR}. */
    private static final double[] SAMPLES = {
            0, 0, 0,
            1, 1, 0, -1, 1, 0, 0, 1, 1, 0, 1, -1,
            1, -1, 0, -1, -1, 0, 0, -1, 1, 0, -1, -1,
            0, 1, 0, 0, -1, 0
    };

    // ------------------------------------------------------------------ llamas en pantalla

    /** Cuánto siguen las llamas violetas después de salir (igual que la quemadura del servidor: 1,5 s). */
    private static final int RESIDUAL_TICKS = 30;
    private static @Nullable LocalPlayer residualPlayer;
    private static int residualUntil = -1;
    private static boolean hurtWhileTouching;

    /**
     * ¿Se dibujan las llamas violetas en primera persona? Mientras toca Noyux o su fuego, sí. Al salir,
     * siguen un instante solo si el Noyux llegó a lastimarlo (así el waterdrop rápido no deja llamas);
     * el agua las apaga y la lava vanilla pone las suyas.
     */
    public static boolean isPlayerBurningInNoyux() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isCreative() || player.isSpectator()) return false;
        if (player != residualPlayer) {
            residualPlayer = player;
            residualUntil = -1;
            hurtWhileTouching = false;
        }
        int now = player.tickCount;
        if (touchesNoyux(player)) {
            if (player.hurtTime > 0) hurtWhileTouching = true;
            residualUntil = hurtWhileTouching ? now + RESIDUAL_TICKS : -1;
            return true;
        }
        hurtWhileTouching = false;
        if (player.isInWater() || touchesVanillaLava(player)) {
            residualUntil = -1;
            return false;
        }
        return now <= residualUntil;
    }

    private static boolean touchesNoyux(LocalPlayer player) {
        Level level = player.level();
        AABB box = player.getBoundingBox().deflate(0.001D);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = (int) Math.floor(box.minX); x <= (int) Math.floor(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y <= (int) Math.floor(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z <= (int) Math.floor(box.maxZ); z++) {
                    pos.set(x, y, z);
                    if (level.getBlockState(pos).is(ModBlocks.NOYUX_FIRE)) return true;
                    FluidState fluid = level.getFluidState(pos);
                    if (fluid.is(ModFluidTags.NOYUX) && box.minY < y + fluid.getHeight(level, pos)) return true;
                }
            }
        }
        return false;
    }

    private static boolean touchesVanillaLava(LocalPlayer player) {
        Level level = player.level();
        AABB box = player.getBoundingBox().deflate(0.001D);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = (int) Math.floor(box.minX); x <= (int) Math.floor(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y <= (int) Math.floor(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z <= (int) Math.floor(box.maxZ); z++) {
                    pos.set(x, y, z);
                    if (com.noxisculture.block.custom.NoyuxReactions.isLava(level.getFluidState(pos))) return true;
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
