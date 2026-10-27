package com.noxisculture.entity.ai;

import com.noxisculture.config.NoxisConfig;
import com.noxisculture.entity.idle.NoxisAmbientAnimation;
import com.noxisculture.entity.personality.NoxisPersonality;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Contemplar el cielo (servidor). De vez en cuando, un Noxis tranquilo con el cielo abierto encima se
 * detiene, levanta despacio la cabeza y mira hacia arriba un rato: las estrellas de noche, el sol
 * bajando o subiendo en el atardecer y el amanecer, y (muy pocas veces) las nubes de día. Después baja
 * la cabeza y sigue con lo suyo.
 *
 * <ul>
 *   <li>Respeta el mundo: no lo hace con techo encima, bajo la lluvia ni en el agua. De noche mira el
 *   cielo estrellado en general (sin calcular la luna); en el atardecer mira al oeste (por donde se va
 *   el sol) y en el amanecer al este: se distingue por si el cielo se oscurece o se aclara.</li>
 *   <li>Variantes: parado o sentado (si el piso es firme), con la mirada que va cambiando despacio,
 *   orejitas que se mueven apenas y la cabeza un poco ladeada (lo dibuja el cliente).</li>
 *   <li>Personalidad: los tranquilos miran más tiempo; los curiosos a veces caminan unos pasos a un
 *   lugar con mejor vista; los artistas se detienen más en el atardecer; los demás también pueden.</li>
 *   <li>Termina solo: cuando se cumple el rato, si empieza a llover, si aparece un techo, un peligro o
 *   algo más importante (prioridad baja: 5). Nunca sale de su zona. Espera propia de 4 a 9 minutos
 *   (escalada por la opción "Contemplar el cielo").</li>
 * </ul>
 */
public class NoxisSkyGazeGoal<T extends PathfinderMob & NoxisAmbient> extends Goal {
    private static final int COOLDOWN = 4800;
    private static final int COOLDOWN_RANDOM = 6000;
    private static final double SPEED = 0.35D;

    private enum Sky { NIGHT, SUNSET, SUNRISE, DAY }
    private enum Phase { WALK, GAZE }

    private final T mob;
    private int nextCheckTick;
    private long nextAllowedTime = -1L;
    private Sky sky = Sky.NIGHT;
    private Phase phase = Phase.GAZE;
    private @Nullable Vec3 spot;
    private int ticks;
    private int length;
    private boolean sitting;
    private float baseYaw;
    private float gazeYaw;
    private float gazePitch;
    private int nextGazeShift;
    /** Brillo del cielo la última vez que miró (para saber si oscurece o aclara). */
    private int lastBrightness = -1;
    private long lastBrightnessTime;

    public NoxisSkyGazeGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        this.nextCheckTick = mob.getRandom().nextInt(200);
    }

    /** Brillo del cielo alto (lejos de antorchas y luces): 15 de día, bajo de noche, intermedio al atardecer. */
    private int skyBrightness() {
        Level level = this.mob.level();
        BlockPos up = this.mob.blockPosition().above(12);
        if (up.getY() >= level.getMaxY()) up = this.mob.blockPosition().above(2);
        return level.getMaxLocalRawBrightness(up);
    }

    private @Nullable Sky readSky() {
        Level level = this.mob.level();
        if (level.isRaining()) return null;                                // nubes de lluvia: nada que contemplar
        long now = level.getGameTime();
        int b = this.skyBrightness();
        Sky s;
        if (level.isDarkOutside()) {
            s = Sky.NIGHT;
        } else if (b < 14) {
            boolean darkening = this.lastBrightness >= 0 && now - this.lastBrightnessTime < 3600 ? b < this.lastBrightness : b < 10;
            s = darkening ? Sky.SUNSET : Sky.SUNRISE;
        } else {
            s = Sky.DAY;
        }
        if (b != this.lastBrightness) {
            this.lastBrightness = b;
            this.lastBrightnessTime = now;
        }
        return s;
    }

    private float wish(Sky s) {
        NoxisPersonality p = this.mob.getPersonality();
        float base = switch (s) {
            case NIGHT -> 0.22F;
            case SUNSET, SUNRISE -> 0.3F;
            case DAY -> 0.04F;
        };
        float k = switch (p) {
            case CALM -> 1.5F;
            case ARTIST -> s == Sky.SUNSET || s == Sky.SUNRISE ? 2.0F : 1.3F;
            case CURIOUS -> 1.1F;
            case AFFECTIONATE -> 1.0F;
            case MISCHIEVOUS -> 0.7F;
        };
        return base * k;
    }

    @Override
    public boolean canUse() {
        if (this.mob.tickCount < this.nextCheckTick) return false;
        RandomSource random = this.mob.getRandom();
        this.nextCheckTick = this.mob.tickCount + 80 + random.nextInt(80);
        int freq = NoxisConfig.get().skyGazing;
        if (!NoxisConfig.enabled(freq)) return false;
        long now = this.mob.level().getGameTime();
        if (this.nextAllowedTime < 0L) this.nextAllowedTime = now + NoxisConfig.scaleCooldown(2400 + random.nextInt(4800), freq);
        if (now < this.nextAllowedTime || !this.mob.canGazeSky() || !this.mob.onGround() || this.mob.isInWater()) return false;
        if (!NoxisAmbientUtil.openSky(this.mob.level(), this.mob.blockPosition())) return false;
        Sky s = this.readSky();
        if (s == null || random.nextFloat() >= this.wish(s)) return false;
        this.sky = s;
        this.spot = null;
        // Los curiosos a veces buscan un lugar con mejor vista (más abierto), cerca.
        if (this.mob.getPersonality() == NoxisPersonality.CURIOUS && random.nextBoolean()) this.spot = this.betterSpot();
        return true;
    }

    private @Nullable Vec3 betterSpot() {
        Level level = this.mob.level();
        RandomSource random = this.mob.getRandom();
        for (int i = 0; i < 6; i++) {
            BlockPos p = this.mob.blockPosition().offset(random.nextInt(11) - 5, random.nextInt(3) - 1, random.nextInt(11) - 5);
            if (!NoxisAmbientUtil.standable(level, p) || !NoxisAmbientUtil.openSky(level, p)) continue;
            Vec3 c = Vec3.atBottomCenterOf(p);
            if (!NoxisAmbientUtil.insideHome(this.mob, c) || this.mob.getNavigation().createPath(p, 0) == null) continue;
            return c;
        }
        return null;
    }

    @Override
    public void start() {
        RandomSource random = this.mob.getRandom();
        this.ticks = 0;
        int base = 200 + random.nextInt(240);                               // 10 a 22 s
        float k = switch (this.mob.getPersonality()) {
            case CALM -> 1.6F;
            case ARTIST -> 1.3F;
            case MISCHIEVOUS -> 0.7F;
            default -> 1.0F;
        };
        this.length = (int) (base * k);
        this.phase = this.spot != null ? Phase.WALK : Phase.GAZE;
        if (this.phase == Phase.GAZE) this.beginGaze();
    }

    private void beginGaze() {
        RandomSource random = this.mob.getRandom();
        this.phase = Phase.GAZE;
        this.ticks = 0;
        this.mob.getNavigation().stop();
        // Hacia dónde: el sol se pone por el oeste y sale por el este (en Minecraft, -X y +X).
        this.baseYaw = switch (this.sky) {
            case SUNSET -> 90.0F + (random.nextFloat() - 0.5F) * 40.0F;
            case SUNRISE -> -90.0F + (random.nextFloat() - 0.5F) * 40.0F;
            default -> this.mob.getYRot() + (random.nextFloat() - 0.5F) * 120.0F;
        };
        this.gazeYaw = this.baseYaw;
        this.gazePitch = this.sky == Sky.NIGHT ? 55.0F : this.sky == Sky.DAY ? 45.0F : 18.0F;
        this.nextGazeShift = 60 + random.nextInt(60);
        // Sentado (si el piso es firme y le da): los tranquilos y los artistas, más seguido.
        float sitChance = switch (this.mob.getPersonality()) {
            case CALM -> 0.7F;
            case ARTIST, AFFECTIONATE -> 0.5F;
            default -> 0.3F;
        };
        this.sitting = this.mob.onGround() && random.nextFloat() < sitChance;
        this.mob.setAmbientAnim(this.sitting ? NoxisAmbientAnimation.SKY_SIT : NoxisAmbientAnimation.SKY_STAND);
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.mob.canGazeSky() || this.mob.hurtTime > 0 || this.mob.isInWater()) return false;
        if (this.phase == Phase.WALK) return this.ticks < 160;
        if (this.mob.level().isRaining() || !NoxisAmbientUtil.openSky(this.mob.level(), this.mob.blockPosition())) return false;
        return this.ticks < this.length && NoxisAmbientUtil.insideHome(this.mob, this.mob.position());
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.ticks++;
        if (this.phase == Phase.WALK) {
            Vec3 s = this.spot;
            if (s == null || NoxisSpacing.horizontal2(this.mob.position(), s) < 0.5D * 0.5D || this.mob.getNavigation().isDone() && this.ticks > 20) {
                this.beginGaze();
            } else if (this.ticks % 10 == 1) {
                this.mob.getNavigation().moveTo(s.x, s.y, s.z, SPEED);
            }
            return;
        }
        this.mob.getNavigation().stop();
        // La mirada cambia despacio de lugar (busca otra estrella, sigue el color del cielo).
        if (this.ticks >= this.nextGazeShift) {
            RandomSource random = this.mob.getRandom();
            this.nextGazeShift = this.ticks + 60 + random.nextInt(80);
            this.gazeYaw = this.baseYaw + (random.nextFloat() - 0.5F) * (this.sky == Sky.NIGHT ? 80.0F : 30.0F);
        }
        float yaw = Mth.approachDegrees(this.mob.getYHeadRot(), this.gazeYaw, 1.5F);
        Vec3 eye = this.mob.getEyePosition();
        Vec3 dir = NoxisAmbientUtil.forward(yaw);
        double up = Math.tan(this.gazePitch * Mth.DEG_TO_RAD);
        this.mob.getLookControl().setLookAt(eye.x + dir.x * 4.0D, eye.y + up * 4.0D, eye.z + dir.z * 4.0D, 6.0F, 30.0F);
        if (this.sitting) NoxisAmbientUtil.face(this.mob, this.mob.yBodyRot);
    }

    @Override
    public void stop() {
        this.mob.setAmbientAnim(NoxisAmbientAnimation.NONE);
        this.mob.getNavigation().stop();
        this.spot = null;
        RandomSource random = this.mob.getRandom();
        this.nextAllowedTime = this.mob.level().getGameTime()
                + NoxisConfig.scaleCooldown(COOLDOWN + random.nextInt(COOLDOWN_RANDOM), NoxisConfig.get().skyGazing);
    }
}
