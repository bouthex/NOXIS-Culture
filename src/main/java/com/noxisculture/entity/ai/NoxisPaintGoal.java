package com.noxisculture.entity.ai;

import com.noxisculture.art.NoxisPaintings;
import com.noxisculture.block.ModBlocks;
import com.noxisculture.block.custom.NoxisEaselBlock;
import com.noxisculture.block.entity.NoxisEaselBlockEntity;
import com.noxisculture.config.NoxisConfig;
import com.noxisculture.entity.idle.NoxisAmbientAnimation;
import com.noxisculture.entity.personality.NoxisPersonality;
import com.noxisculture.world.ModPoi;
import java.util.EnumSet;
import java.util.List;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Pintar en un atril de verdad (servidor). La obra queda en el mundo mientras se pinta: el artista
 * puede trabajar un rato, irse y volver otro día a continuarla, hasta terminarla.
 *
 * <ol>
 *   <li>Elegir: si tiene una obra a medio pintar, vuelve a su atril; si no, usa un atril vacío cercano
 *   (por ejemplo, uno de un jugador) o elige un lugar tranquilo con vista y arma el suyo.</li>
 *   <li>Observar el paisaje (encuadrándolo con las patitas) y armar el atril con un lienzo en blanco.</li>
 *   <li>Pintar: pinceladas, pausas para mirar y retoques. Cada sesión avanza la obra (el lienzo pasa
 *   de blanco a boceto, a color y a terminado). Una sesión dura unas pocas pinceladas.</li>
 *   <li>Si no la terminó: se aleja un paso, la mira y se va (recuerda dónde quedó). Si la terminó: la
 *   mira contento y decide si la deja exhibida en el atril o se la lleva para colgarla en una pared
 *   (y entonces guarda su atril; un atril que no armó él, nunca se lo lleva).</li>
 * </ol>
 * <ul>
 *   <li>Quién: los Artistas; muy de vez en cuando un lector/escritor. No cambia ninguna personalidad.</li>
 *   <li>Cada obra nace con su número (familia de estilo del artista) y nunca cambia; no se crean obras a
 *   medias ni duplicadas: la obra existe UNA vez, en el atril, hasta que alguien la retira.</li>
 *   <li>Nadie más toca una obra en curso: ni otros Noxis ni los jugadores (salvo que quede abandonada 3
 *   días). Si al volver el atril ya no está o cambió, el artista simplemente lo olvida.</li>
 * </ul>
 */
public class NoxisPaintGoal<T extends PathfinderMob & NoxisAmbient> extends Goal {
    private static final int COOLDOWN = 7200;
    private static final int COOLDOWN_RANDOM = 7200;
    /** Espera para volver a una obra que dejó a medias. */
    private static final int RETURN_COOLDOWN = 2400;
    private static final int MAX_CARRIED = 2;
    private static final double SPEED = 0.35D;

    private enum Phase { WALK, LOOK, SETUP, STROKE, STEPBACK, RETOUCH, ADMIRE, PACK, LEAVE }

    private final T mob;
    private int nextCheckTick;
    private long nextAllowedTime = -1L;
    private @Nullable Vec3 spot;
    /** Dónde va (o está) el atril. */
    private @Nullable BlockPos easel;
    /** ¿Hay que armar un atril nuevo en {@link #easel}? */
    private boolean needsEasel;
    private float viewYaw;
    private Phase phase = Phase.WALK;
    private int ticks;
    private int phaseLength;
    private int cycles;
    private int art;
    private int lastArt = -1;
    private boolean finished;
    private boolean completed;

    public NoxisPaintGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        this.nextCheckTick = mob.getRandom().nextInt(400);
    }

    /**
     * Solo para las pruebas automáticas (propiedad del sistema {@code noxisculture.test.fastArt=true}):
     * sin esperas largas, para poder ver una sesión completa en pocos minutos. Apagado en el juego normal.
     */
    static boolean fastForTests() {
        return Boolean.getBoolean("noxisculture.test.fastArt");
    }

    private float wish() {
        if (fastForTests() && this.mob.getPersonality() == NoxisPersonality.ARTIST) return 1.0F;
        if (this.mob.getPersonality() == NoxisPersonality.ARTIST) return 0.4F;
        if (this.mob.isScholar()) return 0.04F;                  // un creativo, muy de vez en cuando
        return 0.0F;
    }

    private static int carried(SimpleContainer inv) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) if (NoxisPaintings.isPainting(inv.getItem(i))) n++;
        return n;
    }

    @Override
    public boolean canUse() {
        boolean r = this.canUse0();
        if (fastForTests() && this.mob.tickCount >= this.lastDebug + 200) {
            this.lastDebug = this.mob.tickCount;
            com.noxisculture.NoxisCulture.LOGGER.info("[NoxisTest] pintar canUse={} motivo={}", r, this.why);
        }
        return r;
    }

    private int lastDebug;
    private String why = "";

    private boolean no(String reason) {
        this.why = reason;
        return false;
    }

    private boolean canUse0() {
        if (this.mob.tickCount < this.nextCheckTick) return no("espera");
        RandomSource random = this.mob.getRandom();
        this.nextCheckTick = this.mob.tickCount + 200 + random.nextInt(200);
        int freq = NoxisConfig.get().painting;
        if (!NoxisConfig.enabled(freq) || this.wish() <= 0.0F) return no("no pinta");
        long now = this.mob.level().getGameTime();
        if (this.nextAllowedTime < 0L) this.nextAllowedTime = now + NoxisConfig.scaleCooldown(3600 + random.nextInt(6000), freq);
        if (fastForTests()) this.nextAllowedTime = Math.min(this.nextAllowedTime, now);
        if (now < this.nextAllowedTime || !this.mob.canDoAmbient() || !this.mob.onGround() || this.mob.isInWater()) return no("ocupado");
        if (!(this.mob.level() instanceof ServerLevel level)) return false;
        if (level.isDarkOutside() || (level.isRaining() && NoxisAmbientUtil.openSky(level, this.mob.blockPosition()))) return no("oscuro");
        // 1) Su obra a medio pintar.
        BlockPos mine = this.mob.getWorkEasel();
        if (mine != null) {
            if (this.isMyWork(level, mine)) {
                return (fastForTests() || random.nextFloat() < 0.8F) && this.planAt(level, mine, false);
            }
            this.mob.setWorkEasel(null);                          // ya no está (o cambió): la olvida
        }
        if (random.nextFloat() >= this.wish()) return no("ganas");
        if (carried(this.mob.getPrivateInventory()) >= MAX_CARRIED) return false;
        // 2) Un atril vacío cercano. 3) Un lugar nuevo con su propio atril.
        return this.findEmptyEasel(level) || this.findSpot(level) || no("sin lugar");
    }

    private boolean isMyWork(ServerLevel level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        if (!st.is(ModBlocks.NOXIS_EASEL)) return false;
        int canvas = st.getValue(NoxisEaselBlock.CANVAS);
        if (canvas < NoxisEaselBlock.CANVAS_BLANK || canvas > NoxisEaselBlock.CANVAS_COLOR) return false;
        return level.getBlockEntity(pos) instanceof NoxisEaselBlockEntity be && this.mob.getUUID().equals(be.getOwner());
    }

    /** Se planta delante del lienzo de un atril que ya existe. */
    private boolean planAt(ServerLevel level, BlockPos easelPos, boolean fresh) {
        Direction facing = level.getBlockState(easelPos).getValue(NoxisEaselBlock.FACING);
        BlockPos stand = easelPos.relative(facing);
        if (!NoxisAmbientUtil.standable(level, stand) || !NoxisAmbientUtil.insideHome(this.mob, Vec3.atBottomCenterOf(stand))) return false;
        if (this.mob.getNavigation().createPath(stand, 0) == null) return false;
        this.easel = easelPos;
        this.spot = Vec3.atBottomCenterOf(stand);
        this.viewYaw = facing.getOpposite().toYRot();
        this.needsEasel = false;
        this.art = fresh ? NoxisPaintings.pick(this.mob.getRandom(), this.mob.getArtStyle(), this.lastArt)
                : level.getBlockState(easelPos).getValue(NoxisEaselBlock.ART);
        return true;
    }

    private boolean findEmptyEasel(ServerLevel level) {
        List<BlockPos> easels = ModPoi.easelsNear(level, this.mob.blockPosition(), 12)
                .filter(p -> level.getBlockState(p).is(ModBlocks.NOXIS_EASEL)
                        && level.getBlockState(p).getValue(NoxisEaselBlock.CANVAS) == NoxisEaselBlock.CANVAS_NONE)
                .toList();
        if (easels.isEmpty()) return false;
        BlockPos p = easels.get(this.mob.getRandom().nextInt(easels.size()));
        if (!this.nobodyNear(level, p)) return false;
        return this.planAt(level, p, true);
    }

    /** Un lugar tranquilo dentro de su zona: parado en S, el atril en el bloque de adelante, y vista libre. */
    private boolean findSpot(ServerLevel level) {
        RandomSource random = this.mob.getRandom();
        for (int i = 0; i < 10; i++) {
            BlockPos p = this.mob.blockPosition().offset(random.nextInt(13) - 6, random.nextInt(3) - 1, random.nextInt(13) - 6);
            if (!NoxisAmbientUtil.standable(level, p)) continue;
            Vec3 c = Vec3.atBottomCenterOf(p);
            if (!NoxisAmbientUtil.insideHome(this.mob, c)) continue;
            Direction look = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            BlockPos e = p.relative(look);
            if (!level.getBlockState(e).isAir() || !level.getBlockState(e.above()).isAir()) continue;
            if (!ModBlocks.NOXIS_EASEL.defaultBlockState().canSurvive(level, e)) continue;
            if (!level.getBlockState(e.relative(look).above()).isAir()) continue;
            if (!this.nobodyNear(level, p)) continue;
            if (ModPoi.easelsNear(level, e, 6).findAny().isPresent()) continue;      // sin atriles pegados
            if (this.mob.getNavigation().createPath(p, 0) == null) continue;
            this.spot = c;
            this.easel = e;
            this.needsEasel = true;
            this.viewYaw = look.toYRot();
            this.art = NoxisPaintings.pick(random, this.mob.getArtStyle(), this.lastArt);
            return true;
        }
        return false;
    }

    private boolean nobodyNear(Level level, BlockPos p) {
        return level.getEntitiesOfClass(PathfinderMob.class, new net.minecraft.world.phys.AABB(p).inflate(2.0D),
                e -> e != this.mob && e.isAlive()).isEmpty();
    }

    @Override
    public void start() {
        this.cycles = 2 + this.mob.getRandom().nextInt(2);
        this.finished = false;
        this.completed = false;
        this.enter(Phase.WALK, 240);
    }

    private void enter(Phase next, int length) {
        this.phase = next;
        this.ticks = 0;
        this.phaseLength = length;
        byte anim = switch (next) {
            case WALK -> NoxisAmbientAnimation.NONE;
            case LOOK -> NoxisAmbientAnimation.PAINT_LOOK;
            case SETUP -> NoxisAmbientAnimation.PAINT_SETUP;
            case STROKE -> NoxisAmbientAnimation.PAINT_STROKE;
            case STEPBACK, LEAVE -> NoxisAmbientAnimation.PAINT_STEPBACK;
            case RETOUCH -> NoxisAmbientAnimation.PAINT_RETOUCH;
            case ADMIRE -> NoxisAmbientAnimation.PAINT_ADMIRE;
            case PACK -> NoxisAmbientAnimation.PAINT_PACK;
        };
        this.mob.setAmbientAnim(anim);
    }

    @Override
    public boolean canContinueToUse() {
        if (this.finished || !this.mob.canDoAmbient() || this.mob.hurtTime > 0 || this.mob.isInWater()) return false;
        if (this.phase == Phase.WALK) return this.ticks < this.phaseLength;
        // El atril tiene que seguir ahí (salvo antes de armarlo).
        BlockPos e = this.easel;
        if (e == null) return false;
        if (!this.needsEasel && !this.mob.level().getBlockState(e).is(ModBlocks.NOXIS_EASEL)) return false;
        return !this.mob.level().isDarkOutside() || this.phase == Phase.PACK || this.phase == Phase.LEAVE;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.ticks++;
        RandomSource random = this.mob.getRandom();
        if (this.phase == Phase.WALK) {
            Vec3 s = this.spot;
            if (s == null) {
                this.finished = true;
                return;
            }
            if (NoxisSpacing.horizontal2(this.mob.position(), s) < 0.4D * 0.4D) {
                this.mob.getNavigation().stop();
                if (this.needsEasel) this.enter(Phase.LOOK, 60 + random.nextInt(40));
                else if (this.claimExisting()) this.enter(Phase.STROKE, 80 + random.nextInt(60));
                else this.finished = true;
            } else if (this.ticks % 10 == 1 || this.mob.getNavigation().isDone()) {
                this.mob.getNavigation().moveTo(s.x, s.y, s.z, SPEED);
            }
            return;
        }
        this.mob.getNavigation().stop();
        NoxisAmbientUtil.face(this.mob, this.viewYaw);
        Vec3 eye = this.mob.getEyePosition();
        Vec3 f = NoxisAmbientUtil.forward(this.viewYaw);
        if (this.phase == Phase.LOOK) {
            float side = 25.0F * net.minecraft.util.Mth.sin(this.ticks * 0.05F);
            Vec3 d = NoxisAmbientUtil.forward(this.viewYaw + side);
            this.mob.getLookControl().setLookAt(eye.x + d.x * 8.0D, eye.y + 0.5D, eye.z + d.z * 8.0D, 10.0F, 20.0F);
        } else {
            this.mob.getLookControl().setLookAt(eye.x + f.x, eye.y - 0.05D, eye.z + f.z, 20.0F, 20.0F);
        }
        if (this.phase == Phase.SETUP && this.ticks == NoxisAmbientAnimation.SETUP_LENGTH / 2) this.placeEasel();
        if ((this.phase == Phase.STROKE && this.ticks % 28 == 10) || (this.phase == Phase.RETOUCH && this.ticks % 20 == 6)) {
            this.mob.playSound(SoundEvents.BRUSH_GENERIC, 0.18F, 1.3F + random.nextFloat() * 0.3F);
        }
        if (this.ticks < this.phaseLength) return;
        switch (this.phase) {
            case LOOK -> this.enter(Phase.SETUP, NoxisAmbientAnimation.SETUP_LENGTH);
            case SETUP -> this.enter(Phase.STROKE, 80 + random.nextInt(60));
            case STROKE -> {
                if (this.paint(10 + random.nextInt(5))) this.enter(Phase.ADMIRE, 50);
                else this.enter(Phase.STEPBACK, 40 + random.nextInt(30));
            }
            case STEPBACK -> this.enter(Phase.RETOUCH, 40 + random.nextInt(40));
            case RETOUCH -> {
                if (this.paint(5 + random.nextInt(4))) this.enter(Phase.ADMIRE, 50);
                else if (--this.cycles > 0) this.enter(Phase.STROKE, 70 + random.nextInt(60));
                else this.enter(Phase.LEAVE, 40);                     // sigue otro día
            }
            case ADMIRE -> this.finish();
            case PACK, LEAVE -> this.finished = true;
            default -> { }
        }
    }

    /** Retoma una obra suya o empieza una nueva en un atril vacío. */
    private boolean claimExisting() {
        Level level = this.mob.level();
        BlockPos e = this.easel;
        if (e == null) return false;
        BlockState st = level.getBlockState(e);
        if (!st.is(ModBlocks.NOXIS_EASEL) || !(level.getBlockEntity(e) instanceof NoxisEaselBlockEntity be)) return false;
        int canvas = st.getValue(NoxisEaselBlock.CANVAS);
        if (canvas == NoxisEaselBlock.CANVAS_NONE) {
            level.setBlock(e, st.setValue(NoxisEaselBlock.CANVAS, NoxisEaselBlock.CANVAS_BLANK).setValue(NoxisEaselBlock.ART, this.art), Block.UPDATE_ALL);
            be.startWork(this.mob.getUUID(), level.getGameTime(), false);
        } else if (!this.mob.getUUID().equals(be.getOwner()) || canvas == NoxisEaselBlock.CANVAS_DONE) {
            return false;
        } else {
            be.claim(this.mob.getUUID(), level.getGameTime());
        }
        this.mob.setWorkEasel(e);
        return true;
    }

    private void placeEasel() {
        BlockPos e = this.easel;
        if (e == null) return;
        Direction facing = Direction.fromYRot(this.viewYaw).getOpposite();     // el lienzo mira al artista
        if (NoxisEaselBlock.placeForNoxis(this.mob.level(), e, facing, this.art, this.mob.getUUID())) {
            this.needsEasel = false;
            this.mob.setWorkEasel(e);
            this.mob.level().playSound(null, e, SoundEvents.WOOD_PLACE, SoundSource.NEUTRAL, 0.6F, 1.1F);
        } else {
            this.finished = true;                                                  // alguien ocupó el lugar
        }
    }

    /** Avanza la obra. Devuelve true si quedó terminada. */
    private boolean paint(int amount) {
        Level level = this.mob.level();
        BlockPos e = this.easel;
        if (e == null || !(level.getBlockEntity(e) instanceof NoxisEaselBlockEntity be)) return false;
        BlockState st = level.getBlockState(e);
        if (!st.is(ModBlocks.NOXIS_EASEL) || !this.mob.getUUID().equals(be.getOwner())) return false;
        be.addProgress(amount, level.getGameTime());
        int canvas = NoxisEaselBlock.canvasFor(be.getProgress());
        if (canvas != st.getValue(NoxisEaselBlock.CANVAS)) level.setBlock(e, st.setValue(NoxisEaselBlock.CANVAS, canvas), Block.UPDATE_ALL);
        return be.getProgress() >= NoxisEaselBlockEntity.DONE;
    }

    /** Terminada: la deja exhibida en el atril o se la lleva para colgarla en una pared. */
    private void finish() {
        Level level = this.mob.level();
        BlockPos e = this.easel;
        this.completed = true;
        this.lastArt = this.art;
        this.mob.setWorkEasel(null);
        this.mob.playSound(com.noxisculture.sound.ModSounds.NOXIS_HAPPY, 0.4F, 1.2F);
        if (e == null || !(level.getBlockEntity(e) instanceof NoxisEaselBlockEntity be)) {
            this.finished = true;
            return;
        }
        BlockState st = level.getBlockState(e);
        boolean ownEasel = be.isNoxisMade();
        boolean take = carried(this.mob.getPrivateInventory()) < MAX_CARRIED && this.mob.getRandom().nextFloat() < 0.5F;
        if (take) {
            ItemStack work = NoxisPaintings.stack(st.getValue(NoxisEaselBlock.ART));
            if (this.mob.getPrivateInventory().addItem(work).isEmpty()) {
                be.clearWork();
                if (ownEasel) level.setBlock(e, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);       // guarda su atril
                else level.setBlock(e, st.setValue(NoxisEaselBlock.CANVAS, NoxisEaselBlock.CANVAS_NONE), Block.UPDATE_ALL);
                this.enter(Phase.PACK, NoxisAmbientAnimation.PACK_LENGTH);
                return;
            }
        }
        be.clearWork();                                     // queda exhibida (cualquiera puede retirarla)
        this.enter(Phase.LEAVE, 30);
    }

    @Override
    public void stop() {
        this.mob.setAmbientAnim(NoxisAmbientAnimation.NONE);
        this.mob.getNavigation().stop();
        this.spot = null;
        this.easel = null;
        RandomSource random = this.mob.getRandom();
        // Si quedó una obra a medias, vuelve antes (y la recuerda en getWorkEasel()).
        int wait = this.mob.getWorkEasel() != null && !this.completed
                ? RETURN_COOLDOWN + random.nextInt(RETURN_COOLDOWN)
                : COOLDOWN + random.nextInt(COOLDOWN_RANDOM);
        this.nextAllowedTime = this.mob.level().getGameTime() + NoxisConfig.scaleCooldown(wait, NoxisConfig.get().painting);
    }
}
