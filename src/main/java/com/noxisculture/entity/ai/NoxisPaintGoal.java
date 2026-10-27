package com.noxisculture.entity.ai;

import com.noxisculture.art.NoxisPaintings;
import com.noxisculture.config.NoxisConfig;
import com.noxisculture.entity.idle.NoxisAmbientAnimation;
import com.noxisculture.entity.personality.NoxisPersonality;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Pintar (servidor). Es un sistema real: al terminar, el artista tiene en su inventario una obra nueva
 * (un objeto con su número de obra, ver {@link NoxisPaintings}) que después puede dejar en el mundo
 * ({@link NoxisPlaceWorkGoal}).
 *
 * <p>Fases (cada una con su animación, ver {@code NoxisAmbientAnimation}):</p>
 * <ol>
 *   <li>Elegir un lugar tranquilo con algo de vista, dentro de su zona, y caminar hasta ahí.</li>
 *   <li>Observar el paisaje, encuadrándolo con las patitas.</li>
 *   <li>Preparar: arma el caballete con el lienzo en blanco y saca la paleta y el pincel.</li>
 *   <li>Pintar: pinceladas repetidas y variadas; pausas para alejarse un paso y mirar; retoques chicos.
 *   El lienzo se va llenando (se ve en el caballete).</li>
 *   <li>Terminar: mira la obra, contento; desarma el caballete y la guarda.</li>
 * </ol>
 *
 * <ul>
 *   <li>Quién: los Artistas (su inclinación propia). Muy de vez en cuando también un lector/escritor
 *   (los "creativos"); las demás personalidades no pintan. No se cambia la personalidad de nadie.</li>
 *   <li>Cada artista tiene su familia de estilo (se elige una vez y se guarda) y casi siempre pinta de
 *   ella: así sus obras se reconocen. Nunca repite la última obra.</li>
 *   <li>Límites: de día, sin lluvia encima, como mucho dos obras guardadas a la vez (si ya tiene dos,
 *   primero tiene que dejarlas). Espera propia de 6 a 12 min (opción "Pinturas"). Si algo lo
 *   interrumpe antes de terminar, la obra no se crea (no hay obras a medias ni duplicadas).</li>
 * </ul>
 */
public class NoxisPaintGoal<T extends PathfinderMob & NoxisAmbient> extends Goal {
    private static final int COOLDOWN = 7200;
    private static final int COOLDOWN_RANDOM = 7200;
    private static final int MAX_CARRIED = 2;
    private static final double SPEED = 0.35D;

    private enum Phase { WALK, LOOK, SETUP, STROKE, STEPBACK, RETOUCH, ADMIRE, PACK }

    private final T mob;
    private int nextCheckTick;
    private long nextAllowedTime = -1L;
    private @Nullable Vec3 spot;
    private float viewYaw;
    private Phase phase = Phase.WALK;
    private int ticks;
    private int phaseLength;
    private int cycles;
    private int art;
    private int lastArt = -1;
    private boolean finished;

    public NoxisPaintGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        this.nextCheckTick = mob.getRandom().nextInt(400);
    }

    private float wish() {
        if (this.mob.getPersonality() == NoxisPersonality.ARTIST) return 0.4F;
        if (this.mob.isScholar()) return 0.04F;                  // un creativo, muy de vez en cuando
        return 0.0F;
    }

    private static int carried(SimpleContainer inv) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) if (NoxisPaintings.isPainting(inv.getItem(i))) n++;
        return n;
    }

    private static boolean hasRoom(SimpleContainer inv) {
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).isEmpty()) return true;
        return false;
    }

    @Override
    public boolean canUse() {
        if (this.mob.tickCount < this.nextCheckTick) return false;
        RandomSource random = this.mob.getRandom();
        this.nextCheckTick = this.mob.tickCount + 200 + random.nextInt(200);
        int freq = NoxisConfig.get().painting;
        if (!NoxisConfig.enabled(freq) || this.wish() <= 0.0F) return false;
        long now = this.mob.level().getGameTime();
        if (this.nextAllowedTime < 0L) this.nextAllowedTime = now + NoxisConfig.scaleCooldown(3600 + random.nextInt(6000), freq);
        if (now < this.nextAllowedTime || !this.mob.canDoAmbient() || !this.mob.onGround() || this.mob.isInWater()) return false;
        Level level = this.mob.level();
        if (level.isDarkOutside() || (level.isRaining() && NoxisAmbientUtil.openSky(level, this.mob.blockPosition()))) return false;
        SimpleContainer inv = this.mob.getPrivateInventory();
        if (carried(inv) >= MAX_CARRIED || !hasRoom(inv)) return false;
        if (random.nextFloat() >= this.wish()) return false;
        return this.findSpot();
    }

    /** Un lugar tranquilo dentro de su zona, con dos bloques libres adelante (para el caballete y la vista). */
    private boolean findSpot() {
        Level level = this.mob.level();
        RandomSource random = this.mob.getRandom();
        for (int i = 0; i < 10; i++) {
            BlockPos p = this.mob.blockPosition().offset(random.nextInt(13) - 6, random.nextInt(3) - 1, random.nextInt(13) - 6);
            if (!NoxisAmbientUtil.standable(level, p)) continue;
            Vec3 c = Vec3.atBottomCenterOf(p);
            if (!NoxisAmbientUtil.insideHome(this.mob, c)) continue;
            float yaw = random.nextFloat() * 360.0F;
            Vec3 f = NoxisAmbientUtil.forward(yaw);
            BlockPos a1 = BlockPos.containing(c.add(f));
            BlockPos a2 = BlockPos.containing(c.add(f.scale(2.0D)));
            if (!level.getBlockState(a1).isAir() || !level.getBlockState(a1.above()).isAir() || !level.getBlockState(a2.above()).isAir()) continue;
            // Tranquilo: nadie pegado.
            if (!this.mob.level().getEntitiesOfClass(PathfinderMob.class, new net.minecraft.world.phys.AABB(p).inflate(2.0D),
                    e -> e != this.mob && e.isAlive()).isEmpty()) continue;
            if (this.mob.getNavigation().createPath(p, 0) == null) continue;
            this.spot = c;
            this.viewYaw = yaw;
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        RandomSource random = this.mob.getRandom();
        this.art = NoxisPaintings.pick(random, this.mob.getArtStyle(), this.lastArt);
        this.cycles = 2 + random.nextInt(2);
        this.finished = false;
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
            case STEPBACK -> NoxisAmbientAnimation.PAINT_STEPBACK;
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
        return !this.mob.level().isDarkOutside() || this.phase == Phase.PACK;
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
                this.enter(Phase.LOOK, 60 + random.nextInt(40));
            } else if (this.ticks % 10 == 1 || this.mob.getNavigation().isDone()) {
                this.mob.getNavigation().moveTo(s.x, s.y, s.z, SPEED);
            }
            return;
        }
        this.mob.getNavigation().stop();
        NoxisAmbientUtil.face(this.mob, this.viewYaw);
        Vec3 e = this.mob.getEyePosition();
        Vec3 f = NoxisAmbientUtil.forward(this.viewYaw);
        if (this.phase == Phase.LOOK) {
            // Mira el paisaje (más lejos y un poco a los costados).
            float side = 25.0F * net.minecraft.util.Mth.sin(this.ticks * 0.05F);
            Vec3 d = NoxisAmbientUtil.forward(this.viewYaw + side);
            this.mob.getLookControl().setLookAt(e.x + d.x * 8.0D, e.y + 0.5D, e.z + d.z * 8.0D, 10.0F, 20.0F);
        } else {
            // Al lienzo (adelante, a la altura de sus ojos).
            this.mob.getLookControl().setLookAt(e.x + f.x * 1.0D, e.y - 0.1D, e.z + f.z * 1.0D, 20.0F, 20.0F);
        }
        if (this.phase == Phase.STROKE && this.ticks % 28 == 10) {
            this.mob.playSound(SoundEvents.BRUSH_GENERIC, 0.18F, 1.3F + random.nextFloat() * 0.3F);
        }
        if (this.ticks < this.phaseLength) return;
        switch (this.phase) {
            case LOOK -> this.enter(Phase.SETUP, NoxisAmbientAnimation.SETUP_LENGTH);
            case SETUP -> this.enter(Phase.STROKE, 80 + random.nextInt(60));
            case STROKE -> this.enter(Phase.STEPBACK, 40 + random.nextInt(30));
            case STEPBACK -> this.enter(Phase.RETOUCH, 40 + random.nextInt(40));
            case RETOUCH -> {
                this.cycles--;
                if (this.cycles > 0) this.enter(Phase.STROKE, 70 + random.nextInt(60));
                else this.enter(Phase.ADMIRE, 50);
            }
            case ADMIRE -> {
                // ¡Terminada! La obra pasa a su inventario (si no entra, no se crea: nunca se tira al piso).
                ItemStack work = NoxisPaintings.stack(this.art, this.mob.getUUID().toString(), this.mob.level().getGameTime());
                ItemStack rest = this.mob.getPrivateInventory().addItem(work);
                if (rest.isEmpty()) {
                    this.lastArt = this.art;
                    this.mob.playSound(com.noxisculture.sound.ModSounds.NOXIS_HAPPY, 0.4F, 1.2F);
                }
                this.enter(Phase.PACK, NoxisAmbientAnimation.PACK_LENGTH);
            }
            case PACK -> this.finished = true;
            default -> { }
        }
    }

    @Override
    public void stop() {
        this.mob.setAmbientAnim(NoxisAmbientAnimation.NONE);
        this.mob.getNavigation().stop();
        this.spot = null;
        RandomSource random = this.mob.getRandom();
        this.nextAllowedTime = this.mob.level().getGameTime()
                + NoxisConfig.scaleCooldown(COOLDOWN + random.nextInt(COOLDOWN_RANDOM), NoxisConfig.get().painting);
    }
}
