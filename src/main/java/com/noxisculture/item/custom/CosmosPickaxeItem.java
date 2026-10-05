package com.noxisculture.item.custom;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Pico del Cosmos.
 * Habilidad "Pulso Estelar" (clic derecho): detecta menas en un radio de 8 bloques,
 * muestra cuántas hay en la barra de acción y traza una estela de partículas
 * apuntando a la más cercana. Cuesta durabilidad y tiene enfriamiento.
 */
public class CosmosPickaxeItem extends Item {
    // Valores de balance centralizados: tocar aquí, no dentro de la lógica.
    public static final int PULSE_RADIUS = 8;
    public static final int PULSE_COOLDOWN_TICKS = 20 * 30; // 30 s
    public static final int PULSE_DURABILITY_COST = 5;
    private static final double TRAIL_LENGTH = 3.0D;
    private static final double TRAIL_STEP = 0.35D;

    public CosmosPickaxeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            PulseResult result = scanForOres(serverLevel, player.blockPosition());

            if (result.nearest() != null) {
                drawTrail(serverLevel, player.getEyePosition(), Vec3.atCenterOf(result.nearest()));
            }

            // Tono agudo si encontró algo, grave si no: feedback sin mirar la pantalla.
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                    1.0F, result.count() > 0 ? 1.5F : 0.6F);

            serverPlayer.sendSystemMessage(
                    Component.translatable("item.noxis_culture.cosmos_pickaxe.pulse", result.count()), true);

            stack.hurtAndBreak(PULSE_DURABILITY_COST, player, hand);
            player.getCooldowns().addCooldown(stack, PULSE_COOLDOWN_TICKS);
        }
        return InteractionResult.SUCCESS;
    }

    private static PulseResult scanForOres(ServerLevel level, BlockPos center) {
        int count = 0;
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-PULSE_RADIUS, -PULSE_RADIUS, -PULSE_RADIUS),
                center.offset(PULSE_RADIUS, PULSE_RADIUS, PULSE_RADIUS))) {
            if (level.getBlockState(pos).is(ConventionalBlockTags.ORES)) {
                count++;
                double dist = pos.distSqr(center);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = pos.immutable(); // betweenClosed reutiliza el objeto mutable
                }
            }
        }
        return new PulseResult(count, nearest);
    }

    /** Estela corta en el aire (las partículas dentro de piedra no se verían). */
    private static void drawTrail(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 dir = to.subtract(from).normalize();
        double max = Math.min(TRAIL_LENGTH, from.distanceTo(to));
        for (double d = 0.6D; d <= max; d += TRAIL_STEP) {
            Vec3 p = from.add(dir.scale(d));
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private record PulseResult(int count, BlockPos nearest) {}
}
