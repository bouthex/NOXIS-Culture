package com.noxisculture.block.entity;

import com.noxisculture.block.custom.NoyuxCauldronBlock;
import com.noxisculture.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * "Cerebro" del caldero de Noyux: revisa lo que hay adentro cada pocos ticks.
 * Solo existe en calderos con Noyux, así que no afecta el rendimiento del resto.
 */
public class NoyuxCauldronBlockEntity extends BlockEntity {
    private static final int CHECK_INTERVAL_TICKS = 5;
    private static final int BURN_INTERVAL_TICKS = 20;
    private static final float BURN_DAMAGE = 10.0F; // 5 corazones

    private int ticks;

    public NoyuxCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NOYUX_CAULDRON, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, NoyuxCauldronBlockEntity be) {
        be.ticks++;
        if (be.ticks % CHECK_INTERVAL_TICKS != 0 || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        double liquidTop = pos.getY() + 0.25D + 0.25D * state.getValue(NoyuxCauldronBlock.LEVEL);
        AABB inside = new AABB(pos.getX() + 0.125D, pos.getY() + 0.25D, pos.getZ() + 0.125D,
                pos.getX() + 0.875D, liquidTop, pos.getZ() + 0.875D);

        for (Entity entity : level.getEntities((Entity) null, inside, e -> true)) {
            if (entity instanceof ItemEntity item && item.getItem().is(ModItems.RAW_NOXUS)) {
                item.getItem().shrink(1);
                if (item.getItem().isEmpty()) {
                    item.discard();
                }
                NoyuxCauldronBlock.transmute(serverLevel, pos, state);
                return; // el estado del bloque cambió: seguimos en el próximo chequeo
            }
            if (entity instanceof LivingEntity living && be.ticks % BURN_INTERVAL_TICKS == 0) {
                if (living instanceof Player player && (player.isCreative() || player.isSpectator())) {
                    continue;
                }
                living.hurtServer(serverLevel, level.damageSources().magic(), BURN_DAMAGE);
            }
        }
    }
}
