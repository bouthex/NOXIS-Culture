package com.noxisculture.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;

/**
 * El Noxis camina hasta su mesa de trabajo para reponer los tradeos agotados,
 * igual que los aldeanos vanilla con su bloque de profesión.
 * TEMPORAL: usa la mesa de herrería vanilla hasta que diseñemos la mesa Noxis.
 */
public class NoxisWorkAtTableGoal<T extends PathfinderMob & NoxisWorker> extends MoveToBlockGoal {
    private final T worker;

    public NoxisWorkAtTableGoal(T worker, double speed) {
        super(worker, speed, 16, 4);
        this.worker = worker;
    }

    @Override
    public boolean canUse() {
        return this.worker.needsToWork() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return this.worker.needsToWork() && super.canContinueToUse();
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.SMITHING_TABLE);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isReachedTarget()) {
            this.worker.workAt(this.blockPos);
        }
    }
}
