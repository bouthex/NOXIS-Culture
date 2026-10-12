package com.noxisculture.entity.nature;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * Reservas de flores tiradas en el piso (servidor), para que dos Noxis nunca vayan por la misma:
 *
 * <ul>
 *   <li><b>Regalo</b>: la flor que un Noxis le lanzó a otro queda reservada para ESE destinatario.
 *   Ningún otro Noxis la toca hasta que él la recoja o el regalo se cancele.</li>
 *   <li><b>Recogida</b>: un Noxis que va a buscar una flor libre la aparta mientras camina.</li>
 * </ul>
 *
 * <p>Nada queda reservado para siempre: quien la reservó la renueva mientras la persigue, y si
 * deja de hacerlo (murió, se descargó, se alejó, no puede llegar...) la reserva vence sola y la
 * flor queda libre en el piso. Nunca se borra ni se duplica una flor desde acá.</p>
 */
public final class NoxisItemClaims {
    private NoxisItemClaims() {}

    /** Un regalo espera a su destinatario como mucho 1 minuto sin que él lo renueve. */
    public static final long GIFT_TICKS = 20L * 60L;
    /** Una flor libre apartada, como mucho 30 s sin renovar. */
    public static final long COLLECT_TICKS = 20L * 30L;

    private record Claim(UUID owner, boolean gift, long expires) {}

    private static final Map<UUID, Claim> CLAIMS = new HashMap<>();

    private static long now(Entity e) {
        return e.level().getGameTime();
    }

    private static @org.jspecify.annotations.Nullable Claim active(ItemEntity item) {
        Claim c = CLAIMS.get(item.getUUID());
        if (c == null) return null;
        if (c.expires <= now(item) || item.isRemoved()) {
            CLAIMS.remove(item.getUUID());
            return null;
        }
        return c;
    }

    /** Reserva la flor como regalo para ese destinatario (pisa cualquier otra reserva). */
    public static void claimGift(ItemEntity item, Entity recipient) {
        cleanup(now(item));
        CLAIMS.put(item.getUUID(), new Claim(recipient.getUUID(), true, now(item) + GIFT_TICKS));
    }

    /** Intenta apartar una flor libre para ir a buscarla. false si ya es de otro (o es un regalo ajeno). */
    public static boolean claim(ItemEntity item, Entity collector) {
        cleanup(now(item));
        Claim c = active(item);
        if (c != null && !c.owner.equals(collector.getUUID())) return false;
        CLAIMS.put(item.getUUID(), new Claim(collector.getUUID(), c != null && c.gift,
                now(item) + (c != null && c.gift ? GIFT_TICKS : COLLECT_TICKS)));
        return true;
    }

    /** Renueva la reserva mientras la sigue buscando (solo si es suya). */
    public static void refresh(ItemEntity item, Entity owner) {
        Claim c = active(item);
        if (c != null && c.owner.equals(owner.getUUID())) {
            CLAIMS.put(item.getUUID(), new Claim(c.owner, c.gift, now(item) + (c.gift ? GIFT_TICKS : COLLECT_TICKS)));
        }
    }

    /** ¿Está reservada por este Noxis? */
    public static boolean isClaimedBy(ItemEntity item, Entity owner) {
        Claim c = active(item);
        return c != null && c.owner.equals(owner.getUUID());
    }

    /** ¿La reservó OTRO (como regalo o porque va a buscarla)? Entonces hay que ignorarla. */
    public static boolean isClaimedByOther(ItemEntity item, Entity self) {
        Claim c = active(item);
        return c != null && !c.owner.equals(self.getUUID());
    }

    /** Libera la reserva (solo si es suya): la flor queda libre en el piso. */
    public static void release(ItemEntity item, Entity owner) {
        Claim c = CLAIMS.get(item.getUUID());
        if (c != null && c.owner.equals(owner.getUUID())) CLAIMS.remove(item.getUUID());
    }

    /** Libera todo lo que tenía reservado (al morir o desaparecer). */
    public static void releaseAll(Entity owner) {
        CLAIMS.values().removeIf(c -> c.owner.equals(owner.getUUID()));
    }

    private static void cleanup(long now) {
        Iterator<Map.Entry<UUID, Claim>> it = CLAIMS.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().expires <= now) it.remove();
        }
    }
}
