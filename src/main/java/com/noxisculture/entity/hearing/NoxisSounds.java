package com.noxisculture.entity.hearing;

import com.noxisculture.entity.NoxisSpecies;
import com.noxisculture.entity.ai.NoxisListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Qué sonidos del mundo le interesan a un Noxis (servidor). No se revisa ningún sonido "a mano":
 * Minecraft ya avisa de los sonidos importantes con sus eventos de juego (los mismos que escuchan
 * los sensores de sculk y el allay), y cada Noxis tiene un oyente de esos eventos
 * ({@link NoxisHearingListener}). Acá solo se decide si el evento vale la pena:
 *
 * <ul>
 *   <li>{@link #NOTE}: un bloque musical que suena.</li>
 *   <li>{@link #RECORD}: un tocadiscos que empieza a tocar.</li>
 *   <li>{@link #BELL}: una campana (Minecraft avisa "cambio de bloque" en la posición de la campana).</li>
 *   <li>{@link #HORN}: un cuerno de cabra (instrumento).</li>
 *   <li>{@link #CREATURE}: una criatura cercana (que no sea un monstruo ni otro Noxis) que hace algo
 *   audible: el sonido de siempre de los animales, un lobo que se sacude, una oveja que come...</li>
 * </ul>
 *
 * <p>Todo lo demás (pasos, bloques, puertas, golpes...) se ignora: no reaccionan a cada ruidito.</p>
 */
public final class NoxisSounds {
    public static final byte NONE = 0;
    public static final byte NOTE = 1;
    public static final byte RECORD = 2;
    public static final byte BELL = 3;
    public static final byte HORN = 4;
    public static final byte CREATURE = 5;

    /** Hasta qué distancia (bloques) oye a las criaturas. */
    public static final double CREATURE_RANGE = 10.0D;

    private NoxisSounds() {}

    /** Cuánto le interesa (0..1): la chance base de pararse a escuchar. */
    public static float interest(byte kind) {
        return switch (kind) {
            case BELL -> 1.0F;
            case RECORD, HORN -> 0.9F;
            case NOTE -> 0.75F;
            case CREATURE -> 0.4F;
            default -> 0.0F;
        };
    }

    /** Música (los artistas la investigan más seguido). */
    public static boolean isMusic(byte kind) {
        return kind == NOTE || kind == RECORD || kind == HORN;
    }

    /** Clasifica un evento de juego que llegó al oyente del Noxis. */
    public static byte classify(ServerLevel level, Holder<GameEvent> event, GameEvent.Context context, Vec3 pos) {
        if (event.is(GameEvent.NOTE_BLOCK_PLAY.key())) return NOTE;
        if (event.is(GameEvent.JUKEBOX_PLAY.key())) return RECORD;
        if (event.is(GameEvent.INSTRUMENT_PLAY.key())) return HORN;
        if (event.is(GameEvent.BLOCK_CHANGE.key())) {
            // Muchos bloques avisan "cambio"; solo interesa si en ese lugar hay una campana.
            return level.getBlockState(BlockPos.containing(pos)).is(Blocks.BELL) ? BELL : NONE;
        }
        if (event.is(GameEvent.ENTITY_ACTION.key()) || event.is(GameEvent.EAT.key())) {
            return isCreature(context.sourceEntity()) ? CREATURE : NONE;
        }
        return NONE;
    }

    /** Una criatura que vale la pena escuchar: no un monstruo, ni otro Noxis, ni un jugador. */
    public static boolean isCreature(@Nullable Entity entity) {
        return entity instanceof Mob mob && !(entity instanceof NoxisSpecies) && !(entity instanceof Enemy)
                && !mob.isSilent() && mob.isAlive();
    }

    /**
     * Una criatura hizo su sonido de siempre (lo llama {@code MobAmbientSoundMixin} justo cuando
     * Minecraft lo reproduce, así que no se revisa nada cada tick). Les avisa solo a los Noxis
     * que están cerca.
     */
    public static void onCreatureCall(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level) || !isCreature(mob)) return;
        Vec3 pos = mob.getEyePosition();
        for (PathfinderMob noxis : level.getEntitiesOfClass(PathfinderMob.class,
                mob.getBoundingBox().inflate(CREATURE_RANGE, 4.0D, CREATURE_RANGE), e -> e instanceof NoxisListener)) {
            ((NoxisListener) noxis).hearSound(pos, CREATURE, mob);
        }
    }
}
