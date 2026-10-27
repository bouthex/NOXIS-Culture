package com.noxisculture.entity.ai;

import com.noxisculture.block.ModBlocks;
import com.noxisculture.block.custom.NoxisPaintingBlock;
import com.noxisculture.entity.discovery.NoxisDiscoveries;
import com.noxisculture.entity.idle.NoxisAmbientAnimation;
import com.noxisculture.entity.personality.NoxisPersonality;
import com.noxisculture.world.ModPoi;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Mirar una pintura (servidor). De vez en cuando, un Noxis encuentra una obra colgada o apoyada cerca
 * (la encuentra por puntos de interés, sin revisar bloque por bloque) y se para delante a mirarla.
 *
 * <ul>
 *   <li>Los artistas la examinan con más atención y por más tiempo (la patita en el mentón); los curiosos
 *   se acercan; los tranquilos se detienen un momento; otros siguen de largo.</li>
 *   <li>Las obras que nunca vio le interesan más; las que ya vio, solo de vez en cuando (recuerdos
 *   compactos, como los descubrimientos).</li>
 *   <li>Sin valoraciones, puntajes, ventas ni reputación. Un solo Noxis por obra a la vez. Espera de 2 a 5
 *   min; prioridad baja.</li>
 * </ul>
 */
public class NoxisGazeArtGoal<T extends PathfinderMob & NoxisAmbient> extends Goal {
    private static final int RADIUS = 10;
    private static final double SPEED = 0.35D;

    private final T mob;
    private int nextCheckTick;
    private long nextAllowedTime = -1L;
    private @Nullable BlockPos painting;
    private @Nullable Vec3 spot;
    private int key;
    private int ticks;
    private int length;
    private boolean watching;

    public NoxisGazeArtGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        this.nextCheckTick = mob.getRandom().nextInt(600);
    }

    private static float wish(NoxisPersonality p, boolean isNew) {
        float base = switch (p) {
            case ARTIST -> 0.6F;
            case CURIOUS -> 0.5F;
            case CALM -> 0.3F;
            case AFFECTIONATE -> 0.25F;
            case MISCHIEVOUS -> 0.2F;
        };
        return isNew ? base : base * 0.2F;
    }

    @Override
    public boolean canUse() {
        if (this.mob.tickCount < this.nextCheckTick) return false;
        RandomSource random = this.mob.getRandom();
        this.nextCheckTick = this.mob.tickCount + 400 + random.nextInt(400);
        long now = this.mob.level().getGameTime();
        if (this.nextAllowedTime < 0L) this.nextAllowedTime = now + 1200 + random.nextInt(2400);
        if (now < this.nextAllowedTime || !(this.mob.level() instanceof ServerLevel level) || !this.mob.canDoAmbient() || !this.mob.onGround()) return false;
        List<BlockPos> found = ModPoi.paintingsNear(level, this.mob.blockPosition(), RADIUS).toList();
        if (found.isEmpty()) return false;
        BlockPos pick = found.get(random.nextInt(found.size()));
        BlockState state = level.getBlockState(pick);
        if (!state.is(ModBlocks.NOXIS_PAINTING)) return false;
        // ¿Ya la está mirando otro?
        if (!level.getEntitiesOfClass(PathfinderMob.class, new net.minecraft.world.phys.AABB(pick).inflate(2.5D),
                e -> e != this.mob && e instanceof NoxisAmbient a
                        && com.noxisculture.entity.idle.NoxisAmbientAnimation.stateOf(a.getAmbientAnim()) == NoxisAmbientAnimation.GAZE_ART).isEmpty()) return false;
        int art = state.getValue(NoxisPaintingBlock.ART);
        int k = NoxisDiscoveries.key(NoxisDiscoveries.ITEM, 0x0800000 + art);
        boolean isNew = !this.mob.getDiscoveries().has(k, NoxisDiscoveries.KNOWN);
        if (random.nextFloat() >= wish(this.mob.getPersonality(), isNew)) return false;
        // Delante de la obra, a un par de bloques.
        Direction facing = state.getValue(NoxisPaintingBlock.FACING);
        BlockPos front = pick.relative(facing, 2);
        if (state.getValue(NoxisPaintingBlock.STANDING) || !com.noxisculture.entity.ai.NoxisAmbientUtil.standable(level, front)) front = front.below();
        if (!NoxisAmbientUtil.standable(level, front)) front = pick.relative(facing);
        if (!NoxisAmbientUtil.standable(level, front) || this.mob.getNavigation().createPath(front, 0) == null) return false;
        this.painting = pick;
        this.spot = Vec3.atBottomCenterOf(front);
        this.key = k;
        int base = 80 + random.nextInt(80);
        this.length = this.mob.getPersonality() == NoxisPersonality.ARTIST ? base * 2 : this.mob.getPersonality() == NoxisPersonality.CALM ? base / 2 + 30 : base;
        return true;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.watching = false;
    }

    @Override
    public boolean canContinueToUse() {
        BlockPos p = this.painting;
        if (p == null || !this.mob.level().getBlockState(p).is(ModBlocks.NOXIS_PAINTING)) return false;   // la sacaron
        if (!this.mob.canDoAmbient() || this.mob.hurtTime > 0) return false;
        return this.watching ? this.ticks < this.length : this.ticks < 200;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.ticks++;
        BlockPos p = this.painting;
        Vec3 s = this.spot;
        if (p == null || s == null) return;
        Vec3 c = Vec3.atCenterOf(p);
        this.mob.getLookControl().setLookAt(c.x, c.y, c.z, 20.0F, 30.0F);
        if (!this.watching) {
            if (NoxisSpacing.horizontal2(this.mob.position(), s) < 0.5D * 0.5D) {
                this.mob.getNavigation().stop();
                this.watching = true;
                this.ticks = 0;
                this.mob.setAmbientAnim(NoxisAmbientAnimation.GAZE_ART);
            } else if (this.ticks % 10 == 1) {
                this.mob.getNavigation().moveTo(s.x, s.y, s.z, SPEED);
            }
            return;
        }
        this.mob.getNavigation().stop();
        if (this.ticks == this.length - 5) this.mob.getDiscoveries().mark(this.key, (byte) (NoxisDiscoveries.SEEN | NoxisDiscoveries.KNOWN));
    }

    @Override
    public void stop() {
        this.mob.setAmbientAnim(NoxisAmbientAnimation.NONE);
        this.mob.getNavigation().stop();
        this.painting = null;
        this.spot = null;
        this.nextAllowedTime = this.mob.level().getGameTime() + 2400 + this.mob.getRandom().nextInt(3600);
    }
}
