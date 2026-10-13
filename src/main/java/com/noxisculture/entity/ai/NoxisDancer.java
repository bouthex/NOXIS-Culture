package com.noxisculture.entity.ai;

/**
 * Noxis que bailan cuando suena un disco cerca (todas las especies, con capa o sin ella). La
 * coreografía la dibuja el cliente con lo que manda el servidor: la etapa (bailar / caída torpe),
 * el estilo propio de este bailarín, el tick del mundo en que empezó la canción y cuántos ticks
 * dura cada tiempo (así todos los que bailan con el mismo disco van al mismo pulso).
 */
public interface NoxisDancer extends NoxisListener {
    /** Etapa del baile ({@code NoxisDanceAnimation}, sincronizada; NONE = no baila). */
    byte getDanceState();

    void setDance(byte state, byte style, int origin, byte beatTicks);

    /** Pareja de baile actual (servidor; null = baila solo o no baila). */
    @org.jspecify.annotations.Nullable NoxisDancePair getDancePair();

    void setDancePair(@org.jspecify.annotations.Nullable NoxisDancePair pair);

    /** Estilo de baile actual (para saber si baila solo o en pareja). */
    byte getDanceStyleByte();

    /** ¿Libre para bailar? (las mismas condiciones que para pararse a escuchar). */
    default boolean canDance() {
        return this.canListen() && this.getListenAnim() == 0;
    }
}
