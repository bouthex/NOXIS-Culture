package com.noxisculture.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Ayudas compartidas por los momentos tranquilos (servidor; cálculos livianos). */
public final class NoxisAmbientUtil {
    private NoxisAmbientUtil() {}

    /** ¿Ese punto queda dentro de la zona de paseo de su pecera? (sin pecera: no hay límite). */
    public static boolean insideHome(PathfinderMob mob, Vec3 p) {
        if (!(mob instanceof NoxisRoamer roamer)) return true;
        BlockPos home = roamer.getHomeAnchor();
        if (home == null) return true;
        return NoxisSpacing.horizontal2(p, Vec3.atBottomCenterOf(home)) <= NoxisWanderGoal.ROAM_RADIUS * NoxisWanderGoal.ROAM_RADIUS;
    }

    /** Lugar donde un Noxis puede pararse (aire para él y piso firme). */
    public static boolean standable(Level level, BlockPos pos) {
        return level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                && level.getFluidState(pos).isEmpty();
    }

    /** ¿Tiene el cielo encima (sin techo)? */
    public static boolean openSky(Level level, BlockPos feet) {
        return level.canSeeSky(feet.above());
    }

    /** Hacia dónde mira un yaw (grados de Minecraft) como vector horizontal. */
    public static Vec3 forward(float yaw) {
        float r = yaw * net.minecraft.util.Mth.DEG_TO_RAD;
        return new Vec3(-net.minecraft.util.Mth.sin(r), 0.0D, net.minecraft.util.Mth.cos(r));
    }

    /** El costado derecho de un yaw. */
    public static Vec3 right(float yaw) {
        float r = yaw * net.minecraft.util.Mth.DEG_TO_RAD;
        return new Vec3(-net.minecraft.util.Mth.cos(r), 0.0D, -net.minecraft.util.Mth.sin(r));
    }

    public static float yawTowards(Vec3 from, Vec3 to) {
        return (float) (net.minecraft.util.Mth.atan2(to.z - from.z, to.x - from.x) * net.minecraft.util.Mth.RAD_TO_DEG) - 90.0F;
    }

    public static void face(PathfinderMob mob, float yaw) {
        mob.setYRot(yaw);
        mob.yBodyRot = yaw;
        mob.yHeadRot = yaw;
    }
}
