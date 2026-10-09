package com.noxisculture.entity.social;

import com.noxisculture.entity.ai.NoxisSocial;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import org.jspecify.annotations.Nullable;

/**
 * Con quién está compartiendo una interacción social un Noxis (servidor), y sus tiempos de
 * espera. Los dos participantes quedan enlazados entre sí: uno "guía" la interacción y el otro
 * la acompaña, así nunca arrancan dos interacciones distintas a la vez.
 */
public final class NoxisSocialLink {
    public enum Kind { GREET, REST }

    private @Nullable PathfinderMob partner;
    private @Nullable Kind kind;
    private boolean leader;
    /** Descanso en compañía: el compañero que lo invitó a acostarse a su lado (o null). */
    private @Nullable PathfinderMob inviter;

    /** Desde cuándo puede volver a saludar (reloj del mundo). 0 = todavía no se sorteó. */
    private long nextGreetTime;
    /** A quién saludó la última vez y hasta cuándo no lo repite. */
    private int lastPartnerId = -1;
    private long lastPartnerUntil;

    public boolean isBusy() {
        return this.partner != null;
    }

    public @Nullable PathfinderMob getPartner() {
        return this.partner;
    }

    public @Nullable Kind getKind() {
        return this.kind;
    }

    public boolean isLeader() {
        return this.leader;
    }

    public void link(PathfinderMob partner, Kind kind, boolean leader) {
        this.partner = partner;
        this.kind = kind;
        this.leader = leader;
    }

    /** El compañero (que ya se acostó) lo invita a dormir a su lado. */
    public void invite(PathfinderMob from) {
        this.inviter = from;
    }

    public @Nullable PathfinderMob getInviter() {
        return this.inviter;
    }

    public void clearInvite() {
        this.inviter = null;
    }

    public void clear() {
        this.inviter = null;
        this.partner = null;
        this.kind = null;
        this.leader = false;
    }

    /** ¿Sigue enlazado con ese compañero? */
    public boolean isLinkedWith(Entity other) {
        return this.partner == other;
    }

    // ---------------- saludo: tiempos propios ----------------

    public boolean canGreetNow(long now, RandomSource random) {
        if (this.nextGreetTime == 0L) {
            // Primer saludo: cada Noxis tiene su propio momento (30 s a 2,5 min).
            this.nextGreetTime = now + 600 + random.nextInt(2400);
        }
        return now >= this.nextGreetTime;
    }

    public void greeted(Entity partner, long now, RandomSource random) {
        this.nextGreetTime = now + 3600 + random.nextInt(4800);   // 3 a 7 min
        this.lastPartnerId = partner.getId();
        this.lastPartnerUntil = now + 12000;                      // al mismo, no antes de 10 min
    }

    public boolean recentlyGreeted(Entity partner, long now) {
        return partner.getId() == this.lastPartnerId && now < this.lastPartnerUntil;
    }

    // ---------------- búsqueda de compañeros ----------------

    /** Un compañero cercano, tranquilo y libre (elegido al azar, no siempre el más cercano). */
    public static @Nullable PathfinderMob findPartner(PathfinderMob self, double radius,
                                                      Predicate<PathfinderMob> extra) {
        List<PathfinderMob> found = new ArrayList<>(self.level().getEntitiesOfClass(PathfinderMob.class,
                self.getBoundingBox().inflate(radius, 2.0D, radius),
                e -> e != self && e.isAlive() && e instanceof NoxisSocial s && s.canSocialize()
                        && !s.getSocialLink().isBusy() && self.hasLineOfSight(e) && extra.test(e)));
        if (found.isEmpty()) return null;
        return found.get(self.getRandom().nextInt(found.size()));
    }

    /** ¿Hay otra interacción social en curso en esta zona? (así no se saludan todos a la vez). */
    public static boolean zoneBusy(PathfinderMob self, double radius, @Nullable Kind kind) {
        return !self.level().getEntitiesOfClass(PathfinderMob.class,
                self.getBoundingBox().inflate(radius, 4.0D, radius),
                e -> e != self && e instanceof NoxisSocial s && s.getSocialLink().isBusy()
                        && (kind == null || s.getSocialLink().getKind() == kind)).isEmpty();
    }
}
