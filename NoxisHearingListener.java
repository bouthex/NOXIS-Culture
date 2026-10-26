package com.noxisculture.entity.hearing;

import com.noxisculture.entity.ai.NoxisListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;

/**
 * Los "oídos" de un Noxis: un oyente de eventos de juego que viaja con él (igual que el del
 * allay que escucha tocadiscos). Minecraft solo le avisa de los eventos que pasan dentro de su
 * radio, así que no hay que revisar nada cada tick. Lo único que hace es anotar el sonido; el
 * objetivo {@code NoxisListenGoal} decide después si se para a escuchar.
 */
public class NoxisHearingListener implements GameEventListener {
    /** Radio de escucha (bloques). */
    public static final int RADIUS = 16;

    private final PathfinderMob owner;
    private final PositionSource source;

    public <T extends PathfinderMob & NoxisListener> NoxisHearingListener(T owner) {
        this.owner = owner;
        this.source = new EntityPositionSource(owner, owner.getEyeHeight());
    }

    @Override
    public PositionSource getListenerSource() {
        return this.source;
    }

    @Override
    public int getListenerRadius() {
        return RADIUS;
    }

    @Override
    public boolean handleGameEvent(ServerLevel level, Holder<GameEvent> event, GameEvent.Context context, Vec3 pos) {
        Entity cause = context.sourceEntity();
        if (!this.owner.isAlive()) return false;
        // Tocadiscos: se anota cuándo empieza y cuándo termina la música (para bailar).
        if (event.is(GameEvent.JUKEBOX_PLAY.key())) {
            NoxisJukeboxes.onPlay(level, BlockPos.containing(pos));
        } else if (event.is(GameEvent.JUKEBOX_STOP_PLAY.key())) {
            NoxisJukeboxes.onStop(level, BlockPos.containing(pos));
            return false;
        }
        if (cause == this.owner) return false;
        byte kind = NoxisSounds.classify(level, event, context, pos);
        if (kind == NoxisSounds.NONE) return false;
        ((NoxisListener) this.owner).hearSound(pos, kind, cause);
        return true;
    }
}
