package com.noxisculture.block.entity;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Lo que el atril recuerda de la obra que se está pintando (solo servidor; se guarda con el mundo):
 * quién la pinta, cuánto avanzó (0 a 100), cuándo pintó por última vez y si el atril lo armó el propio
 * Noxis (entonces puede llevárselo al terminar; un atril de un jugador nunca se lo lleva).
 */
public class NoxisEaselBlockEntity extends BlockEntity {
    public static final int DONE = 100;
    /** Tres días sin volver: la obra queda abandonada y cualquiera puede retirarla. */
    public static final long ABANDON_TICKS = 72000L;

    private @Nullable UUID owner;
    private int progress;
    private long lastWork;
    private boolean noxisMade;

    public NoxisEaselBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NOXIS_EASEL, pos, state);
    }

    public @Nullable UUID getOwner() {
        return this.owner;
    }

    public int getProgress() {
        return this.progress;
    }

    public boolean isNoxisMade() {
        return this.noxisMade;
    }

    public long getLastWork() {
        return this.lastWork;
    }

    public boolean isAbandoned(long now) {
        return this.owner == null || now - this.lastWork > ABANDON_TICKS;
    }

    public void startWork(UUID owner, long now, boolean noxisMade) {
        this.owner = owner;
        this.progress = 0;
        this.lastWork = now;
        this.noxisMade = noxisMade;
        this.setChanged();
    }

    /** Un Noxis retoma (o adopta) la obra. */
    public void claim(UUID owner, long now) {
        this.owner = owner;
        this.lastWork = now;
        this.setChanged();
    }

    public void addProgress(int amount, long now) {
        this.progress = Math.min(DONE, this.progress + amount);
        this.lastWork = now;
        this.setChanged();
    }

    /** Sin obra en curso (se retiró la pintura, se exhibe una terminada o se puso una de un jugador). */
    public void clearWork() {
        this.owner = null;
        this.progress = 0;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.owner != null) output.store("owner", UUIDUtil.CODEC, this.owner);
        output.putInt("progress", this.progress);
        output.putLong("last_work", this.lastWork);
        output.putBoolean("noxis_made", this.noxisMade);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        Optional<UUID> o = input.read("owner", UUIDUtil.CODEC);
        this.owner = o.orElse(null);
        this.progress = input.getIntOr("progress", 0);
        this.lastWork = input.getLongOr("last_work", 0L);
        this.noxisMade = input.getBooleanOr("noxis_made", false);
    }
}
