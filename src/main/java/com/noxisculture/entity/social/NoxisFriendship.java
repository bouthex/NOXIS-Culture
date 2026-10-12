package com.noxisculture.entity.social;

import com.noxisculture.entity.ai.NoxisFriend;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import org.jspecify.annotations.Nullable;

/**
 * Amistades especiales entre Noxis (servidor), compartido por todas las especies.
 *
 * <ul>
 *   <li><b>Nacen de a poco</b>: dos Noxis sin amigo que pasan tiempo cerca y tranquilos tienen,
 *   cada tanto, una chance chiquita de hacerse amigos (los dos a la vez). Unos corazoncitos y
 *   un soniditito lo marcan.</li>
 *   <li><b>Uno solo y estable</b>: cada uno tiene como mucho un amigo; no cambia de compañero.
 *   Se guarda con el mundo.</li>
 *   <li><b>No todos</b>: algunos Noxis son independientes y no forman vínculos.</li>
 *   <li><b>Se olvida solo</b> si el amigo muere, o si pasan 3 días de juego sin verlo cargado
 *   cerca (desapareció del mundo).</li>
 * </ul>
 */
public final class NoxisFriendship {
    private NoxisFriendship() {}

    /** Proporción de Noxis independientes (nunca forman un vínculo). */
    public static final float INDEPENDENT_CHANCE = 0.35F;
    /** Cada cuánto se fija si puede hacerse amigo de alguien (5 s). */
    public static final int BOND_INTERVAL = 100;
    /** Chance por intento estando cerca de un candidato: en promedio, ~2,5 min juntos. */
    private static final int BOND_CHANCE = 30;
    private static final double BOND_RADIUS = 6.0D;
    /** 3 días de juego sin ver al amigo: lo olvida (ya no existe). */
    public static final long FORGET_TICKS = 72_000L;

    /** El amigo, si está cargado en este mundo (y sigue siendo su amigo). */
    public static <T extends PathfinderMob & NoxisFriend> @Nullable PathfinderMob findFriend(T mob) {
        UUID id = mob.getFriendId();
        if (id == null || !(mob.level() instanceof ServerLevel level)) return null;
        Entity e = level.getEntity(id);
        if (e instanceof PathfinderMob friend && friend.isAlive() && friend instanceof NoxisFriend f
                && mob.getUUID().equals(f.getFriendId())) {
            return friend;
        }
        return null;
    }

    /** Intento de hacerse amigo de alguien cercano (se llama cada {@link #BOND_INTERVAL} ticks). */
    public static <T extends PathfinderMob & NoxisFriend> void tryBond(T mob) {
        if (mob.getFriendId() != null || mob.isIndependent() || mob.isBaby() || !mob.canSocialize()) return;
        if (mob.getRandom().nextInt(BOND_CHANCE) != 0) return;
        List<PathfinderMob> found = mob.level().getEntitiesOfClass(PathfinderMob.class,
                mob.getBoundingBox().inflate(BOND_RADIUS, 2.0D, BOND_RADIUS),
                e -> e != mob && e.isAlive() && !e.isBaby() && e instanceof NoxisFriend f
                        && f.getFriendId() == null && !f.isIndependent() && f.canSocialize()
                        && mob.hasLineOfSight(e));
        if (found.isEmpty()) return;
        PathfinderMob other = found.get(mob.getRandom().nextInt(found.size()));
        mob.setFriendId(other.getUUID());
        ((NoxisFriend) other).setFriendId(mob.getUUID());
        if (mob.level() instanceof ServerLevel server) {
            for (PathfinderMob m : List.of(mob, other)) {
                server.sendParticles(ParticleTypes.HEART, m.getX(), m.getEyeY() + 0.5D, m.getZ(),
                        2, 0.2D, 0.1D, 0.2D, 0.0D);
            }
        }
        mob.playSound(com.noxisculture.sound.ModSounds.NOXIS_HAPPY, 0.45F, 1.3F);
    }

    /** Al morir: el amigo (si está cargado) queda libre para hacer otro amigo algún día. */
    public static <T extends PathfinderMob & NoxisFriend> void onDeath(T mob) {
        PathfinderMob friend = findFriend(mob);
        if (friend instanceof NoxisFriend f) f.setFriendId(null);
        mob.setFriendId(null);
    }
}
