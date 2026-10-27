package com.noxisculture.entity.ai;

import com.noxisculture.entity.social.NoxisSocialAction;
import com.noxisculture.entity.social.NoxisSocialLink;
import com.noxisculture.sound.ModSounds;
import java.util.EnumSet;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jspecify.annotations.Nullable;

/**
 * Saludo entre compañeros (servidor). De vez en cuando, un Noxis tranquilo invita a otro
 * cercano y tranquilo: se miran, ladean la cabecita, mueven las orejas y uno saluda con la
 * patita mientras el otro asiente (a veces con un saltito). Dura unos 3 segundos.
 *
 * <p>Coordinado: este Noxis guía y el otro lo acompaña ({@link NoxisSocialFollowGoal}). Cada uno
 * tiene sus tiempos de espera, no repite enseguida con el mismo compañero y, si hay otro saludo
 * en la zona, no arranca uno nuevo.</p>
 */
public class NoxisGreetGoal<T extends PathfinderMob & NoxisSocial> extends Goal {
    private static final double RADIUS = 6.0D;
    private static final double ZONE = 10.0D;
    private static final int DURATION = 62;

    private final T mob;
    private int nextCheckTick;
    private int ticks;
    private @Nullable PathfinderMob partner;

    public NoxisGreetGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        this.nextCheckTick = mob.getRandom().nextInt(200);
    }

    @Override
    public boolean canUse() {
        if (this.mob.tickCount < this.nextCheckTick) return false;
        RandomSource random = this.mob.getRandom();
        this.nextCheckTick = this.mob.tickCount + 60 + random.nextInt(100);   // se fija cada 3-8 s
        long now = this.mob.level().getGameTime();
        NoxisSocialLink link = this.mob.getSocialLink();
        if (link.isBusy() || !link.canGreetNow(now, random)) return false;
        if (!this.mob.canSocialize() || random.nextInt(3) != 0) return false;
        if (NoxisSocialLink.zoneBusy(this.mob, ZONE, null)) return false;
        this.partner = NoxisSocialLink.findPartner(this.mob, RADIUS, e -> {
            NoxisSocialLink other = ((NoxisSocial) e).getSocialLink();
            return other.canGreetNow(now, e.getRandom()) && !link.recentlyGreeted(e, now);
        });
        return this.partner != null;
    }

    @Override
    public void start() {
        PathfinderMob other = this.partner;
        if (other == null) return;
        RandomSource random = this.mob.getRandom();
        this.ticks = 0;
        this.mob.getSocialLink().link(other, NoxisSocialLink.Kind.GREET, true);
        ((NoxisSocial) other).getSocialLink().link(this.mob, NoxisSocialLink.Kind.GREET, false);
        this.mob.getNavigation().stop();
        other.getNavigation().stop();
        // Cada uno con su gesto: uno saluda con la patita y el otro asiente (a veces con saltito).
        boolean iWave = random.nextBoolean();
        byte nod = random.nextInt(3) == 0 ? NoxisSocialAction.GREET_NOD_HOP : NoxisSocialAction.GREET_NOD;
        this.mob.setSocialAnim(iWave ? NoxisSocialAction.GREET_WAVE : nod);
        ((NoxisSocial) other).setSocialAnim(iWave ? nod : NoxisSocialAction.GREET_WAVE);
    }

    @Override
    public boolean canContinueToUse() {
        PathfinderMob other = this.partner;
        return this.ticks < DURATION && other != null && other.isAlive()
                && other instanceof NoxisSocial s && s.getSocialLink().isLinkedWith(this.mob)
                && this.mob.isSafeForSocial() && s.isSafeForSocial()
                && this.mob.distanceToSqr(other) < 64.0D;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.ticks++;
        PathfinderMob other = this.partner;
        if (other == null) return;
        this.mob.getNavigation().stop();
        other.getNavigation().stop();
        this.mob.getLookControl().setLookAt(other, 30.0F, 30.0F);
        other.getLookControl().setLookAt(this.mob, 30.0F, 30.0F);
        if (this.ticks == 14) this.mob.playSound(ModSounds.NOXIS_YES, 0.35F, 1.2F);
        if (this.ticks == 20) other.playSound(ModSounds.NOXIS_YES, 0.35F, 1.3F);
    }

    @Override
    public void stop() {
        long now = this.mob.level().getGameTime();
        PathfinderMob other = this.partner;
        this.mob.getSocialLink().clear();
        this.mob.setSocialAnim(NoxisSocialAction.NONE);
        if (other != null) {
            this.mob.getSocialLink().greeted(other, now, this.mob.getRandom());
            if (other instanceof NoxisSocial s) {
                if (s.getSocialLink().isLinkedWith(this.mob)) {
                    s.getSocialLink().clear();
                    s.setSocialAnim(NoxisSocialAction.NONE);
                }
                s.getSocialLink().greeted(this.mob, now, other.getRandom());
            }
        }
        this.partner = null;
    }
}
