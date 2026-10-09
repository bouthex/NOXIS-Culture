package com.noxisculture.entity.ai;

import com.noxisculture.entity.social.NoxisSocialAction;
import com.noxisculture.entity.social.NoxisSocialLink;
import java.util.EnumSet;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * El Noxis que ACOMPAÑA una interacción social (saludo o descanso en compañía). No decide
 * nada: se queda disponible mientras el que la guía lo mueve y lo hace mirar. Reserva el
 * movimiento y la mirada para que ninguna otra actividad se meta en el medio. Si algo
 * importante lo interrumpe, suelta el enlace y el otro cancela la interacción.
 */
public class NoxisSocialFollowGoal<T extends PathfinderMob & NoxisSocial> extends Goal {
    private final T mob;

    public NoxisSocialFollowGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        NoxisSocialLink link = this.mob.getSocialLink();
        return link.isBusy() && !link.isLeader() && link.getInviter() == null;
    }

    @Override
    public boolean canContinueToUse() {
        NoxisSocialLink link = this.mob.getSocialLink();
        PathfinderMob partner = link.getPartner();
        return link.isBusy() && !link.isLeader() && link.getInviter() == null && partner != null && partner.isAlive()
                && partner instanceof NoxisSocial s && s.getSocialLink().isLinkedWith(this.mob)
                && this.mob.isSafeForSocial();
    }

    @Override
    public void stop() {
        // Lo invitaron a dormir al lado: no suelta nada, sigue su propio descanso en compañía.
        if (this.mob.getSocialLink().getInviter() != null) return;
        // Lo interrumpieron (o terminó): suelta el enlace; el que guía se entera y termina.
        this.mob.getSocialLink().clear();
        this.mob.setSocialAnim(NoxisSocialAction.NONE);
    }
}
