package com.noxisculture.entity.ai;

import com.noxisculture.art.NoxisPaintings;
import com.noxisculture.block.ModBlocks;
import com.noxisculture.block.custom.NoxisDocumentBlock;
import com.noxisculture.block.custom.NoxisPaintingBlock;
import com.noxisculture.document.NoxisDocuments;
import com.noxisculture.entity.idle.NoxisAmbientAnimation;
import com.noxisculture.world.ModPoi;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Dejar una obra o un documento en el mundo (servidor). Un Noxis que lleva una pintura terminada (o un
 * documento escrito) busca, de a ratos, un lugar apropiado dentro de su zona y la deja:
 *
 * <ul>
 *   <li><b>Pinturas</b>: colgadas en una pared, a la altura de los ojos, o, si no hay paredes cerca,
 *   exhibidas en un atril vacío cercano. Si no hay lugar, la guarda y prueba otro día.</li>
 *   <li><b>Documentos</b>: apoyados encima de un mueble de la etiqueta
 *   {@code noxis_culture:document_surfaces} (mesas de trabajo, barriles, cofres, atriles...).</li>
 * </ul>
 * <p>Límites: nunca dos obras pegadas (al menos 4 bloques entre pinturas), como mucho 6 pinturas y 3
 * documentos en 24 bloques (los encuentra por puntos de interés, sin revisar bloque por bloque). Si no
 * hay lugar, la guarda y prueba otro día. Al colocarla, la saca de su inventario: nunca se duplica.</p>
 */
public class NoxisPlaceWorkGoal<T extends PathfinderMob & NoxisAmbient> extends Goal {
    private static final int SEARCH = 9;
    private static final int AREA = 24;
    private static final int MAX_PAINTINGS = 6;
    private static final int MAX_DOCUMENTS = 3;
    private static final double SPEED = 0.38D;

    private final T mob;
    private int nextCheckTick;
    private int slot = -1;
    private boolean painting;
    private @Nullable BlockPos standAt;
    private @Nullable BlockPos placeAt;
    private Direction facing = Direction.NORTH;
    private boolean standing;
    private int ticks;
    private boolean placing;
    private boolean done;

    public NoxisPlaceWorkGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        this.nextCheckTick = 1200 + mob.getRandom().nextInt(1200);
    }

    @Override
    public boolean canUse() {
        if (this.mob.tickCount < this.nextCheckTick) return false;
        RandomSource random = this.mob.getRandom();
        this.nextCheckTick = this.mob.tickCount + 1200 + random.nextInt(2400);     // cada 1 a 3 min
        if (!(this.mob.level() instanceof ServerLevel level) || !this.mob.canDoAmbient() || !this.mob.onGround()) return false;
        SimpleContainer inv = this.mob.getPrivateInventory();
        this.slot = -1;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (NoxisPaintings.isPainting(st)) { this.slot = i; this.painting = true; break; }
            if (NoxisDocuments.isDocument(st)) { this.slot = i; this.painting = false; }
        }
        if (this.slot < 0) return false;
        BlockPos here = this.mob.blockPosition();
        if (this.painting) {
            if (ModPoi.artworksNear(level, here, AREA).count() >= MAX_PAINTINGS) return false;
            return this.findWall(level) || this.findEasel(level);
        }
        if (ModPoi.documentsNear(level, here, AREA).count() >= MAX_DOCUMENTS) return false;
        return this.findSurface(level);
    }

    private boolean freeOfPaintings(ServerLevel level, BlockPos p) {
        return ModPoi.artworksNear(level, p, 4).findAny().isEmpty();
    }

    /** Pared: parado en S, la obra queda en S+1 (a la altura de los ojos) colgada de la pared de al lado. */
    private boolean findWall(ServerLevel level) {
        RandomSource random = this.mob.getRandom();
        for (int i = 0; i < 16; i++) {
            BlockPos s = this.mob.blockPosition().offset(random.nextInt(SEARCH * 2 + 1) - SEARCH, random.nextInt(3) - 1, random.nextInt(SEARCH * 2 + 1) - SEARCH);
            if (!NoxisAmbientUtil.standable(level, s) || !NoxisAmbientUtil.insideHome(this.mob, Vec3.atBottomCenterOf(s))) continue;
            BlockPos p = s.above();
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockState state = ((NoxisPaintingBlock) ModBlocks.NOXIS_PAINTING).placed(0, d.getOpposite());
                if (!state.canSurvive(level, p) || !this.freeOfPaintings(level, p)) continue;
                if (this.mob.getNavigation().createPath(s, 0) == null) continue;
                this.standAt = s;
                this.placeAt = p;
                this.facing = d.getOpposite();
                this.standing = false;
                return true;
            }
        }
        return false;
    }

    /** Un atril vacío cercano (de un jugador o de otro Noxis): la deja exhibida ahí. */
    private boolean findEasel(ServerLevel level) {
        for (BlockPos e : ModPoi.easelsNear(level, this.mob.blockPosition(), SEARCH).toList()) {
            BlockState st = level.getBlockState(e);
            if (!st.is(ModBlocks.NOXIS_EASEL) || st.getValue(com.noxisculture.block.custom.NoxisEaselBlock.CANVAS) != 0) continue;
            Direction facing = st.getValue(com.noxisculture.block.custom.NoxisEaselBlock.FACING);
            BlockPos s = e.relative(facing);
            if (!NoxisAmbientUtil.standable(level, s) || !NoxisAmbientUtil.insideHome(this.mob, Vec3.atBottomCenterOf(s))) continue;
            if (this.mob.getNavigation().createPath(s, 0) == null) continue;
            this.standAt = s;
            this.placeAt = e;
            this.facing = facing;
            this.standing = true;
            return true;
        }
        return false;
    }

    /** Encima de un mueble apropiado (mesa, barril, cofre, atril...). */
    private boolean findSurface(ServerLevel level) {
        RandomSource random = this.mob.getRandom();
        for (int i = 0; i < 24; i++) {
            BlockPos b = this.mob.blockPosition().offset(random.nextInt(SEARCH * 2 + 1) - SEARCH, random.nextInt(3) - 1, random.nextInt(SEARCH * 2 + 1) - SEARCH);
            if (!level.getBlockState(b).is(com.noxisculture.tag.ModTags.Blocks.DOCUMENT_SURFACES)) continue;
            BlockPos p = b.above();
            if (!level.getBlockState(p).isAir() || ModPoi.documentsNear(level, p, 3).findAny().isPresent()) continue;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos s = b.relative(d);
                if (!NoxisAmbientUtil.standable(level, s) || !NoxisAmbientUtil.insideHome(this.mob, Vec3.atBottomCenterOf(s))) continue;
                if (this.mob.getNavigation().createPath(s, 0) == null) continue;
                this.standAt = s;
                this.placeAt = p;
                this.facing = d;
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.placing = false;
        this.done = false;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.done && this.ticks < 300 && this.mob.canDoAmbient() && this.mob.hurtTime == 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.ticks++;
        BlockPos s = this.standAt;
        BlockPos p = this.placeAt;
        if (s == null || p == null) {
            this.done = true;
            return;
        }
        Vec3 c = Vec3.atCenterOf(p);
        if (!this.placing) {
            if (NoxisSpacing.horizontal2(this.mob.position(), Vec3.atBottomCenterOf(s)) < 0.6D * 0.6D || this.mob.distanceToSqr(c) < 1.6D * 1.6D) {
                this.mob.getNavigation().stop();
                this.placing = true;
                this.ticks = 0;
                this.mob.setAmbientAnim(NoxisAmbientAnimation.PLACE_ITEM);
            } else if (this.ticks % 10 == 1) {
                this.mob.getNavigation().moveTo(s.getX() + 0.5D, s.getY(), s.getZ() + 0.5D, SPEED);
            }
            return;
        }
        this.mob.getNavigation().stop();
        this.mob.getLookControl().setLookAt(c.x, c.y, c.z, 30.0F, 30.0F);
        if (this.ticks == NoxisAmbientAnimation.PLACE_LENGTH / 2) this.place();
        if (this.ticks >= NoxisAmbientAnimation.PLACE_LENGTH) this.done = true;
    }

    /** La saca del inventario y la coloca (si el lugar sigue libre); si no, la sigue guardando. */
    private void place() {
        Level level = this.mob.level();
        BlockPos p = this.placeAt;
        SimpleContainer inv = this.mob.getPrivateInventory();
        if (p == null || this.slot < 0 || this.slot >= inv.getContainerSize()) return;
        ItemStack st = inv.getItem(this.slot);
        if (this.painting && NoxisPaintings.isPainting(st)) {
            NoxisPaintings.migrateLegacy(st);
            int art = NoxisPaintings.hasArt(st) ? NoxisPaintings.artOf(st)
                    : NoxisPaintings.discover((ServerLevel) level, p, this.mob.getRandom());
            if (this.standing) {
                // En el atril vacío (si sigue vacío).
                BlockState easel = level.getBlockState(p);
                if (!easel.is(ModBlocks.NOXIS_EASEL) || easel.getValue(com.noxisculture.block.custom.NoxisEaselBlock.CANVAS) != 0) return;
                level.setBlock(p, easel.setValue(com.noxisculture.block.custom.NoxisEaselBlock.CANVAS, com.noxisculture.block.custom.NoxisEaselBlock.CANVAS_DONE)
                        .setValue(com.noxisculture.block.custom.NoxisEaselBlock.ART, art), Block.UPDATE_ALL);
            } else {
                if (!level.getBlockState(p).canBeReplaced()) return;
                BlockState state = ((NoxisPaintingBlock) ModBlocks.NOXIS_PAINTING).placed(art, this.facing);
                if (!state.canSurvive(level, p)) return;
                level.setBlock(p, state, Block.UPDATE_ALL);
            }
            inv.removeItem(this.slot, 1);
            level.playSound(null, p, SoundEvents.PAINTING_PLACE, SoundSource.NEUTRAL, 0.7F, 1.0F);
        } else if (!level.getBlockState(p).canBeReplaced()) {
            return;
        } else if (!this.painting && NoxisDocuments.isDocument(st)) {
            ItemStack one = st.copyWithCount(1);
            if (NoxisDocumentBlock.place(level, p, one, this.facing)) {
                inv.removeItem(this.slot, 1);
                level.playSound(null, p, SoundEvents.BOOK_PUT, SoundSource.NEUTRAL, 0.7F, 1.0F);
            }
        }
    }

    @Override
    public void stop() {
        this.mob.setAmbientAnim(NoxisAmbientAnimation.NONE);
        this.mob.getNavigation().stop();
        this.standAt = null;
        this.placeAt = null;
    }
}
