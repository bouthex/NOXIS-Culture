package com.noxisculture.entity.trade;

import com.noxisculture.block.ModBlocks;
import com.noxisculture.item.ModItems;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Comercio del Aldeano Noxis, IGUAL que los aldeanos vanilla:
 *  - 5 niveles: Novato, Aprendiz, Oficial, Experto y Maestro.
 *  - Cada nivel desbloquea 2 tradeos (1 compra + 1 venta) => 10 tradeos en Maestro.
 *  - Sube de nivel juntando experiencia de comercio (mismos umbrales que vanilla).
 *  - Las ofertas agotadas se bloquean y vuelven al reponer (2 veces por día).
 *
 * Cada nivel elige al azar de un grupito, así cada Noxis es un poco distinto;
 * el Maestro siempre ofrece lo mejor: la Gema de Noxus y el Pico del Cosmos.
 */
public final class NoxisVillagerTrades {
    private NoxisVillagerTrades() {}

    @FunctionalInterface
    public interface TradeFactory {
        MerchantOffer create(ServerLevel level, RandomSource random);
    }

    public static final int MAX_LEVEL = 5;

    /** XP TOTAL necesaria para alcanzar el siguiente nivel (mismos valores que vanilla). */
    public static int xpToLevelUp(int level) {
        return switch (level) {
            case 1 -> 10;
            case 2 -> 70;
            case 3 -> 150;
            case 4 -> 250;
            default -> Integer.MAX_VALUE;
        };
    }

    // XP que da cada tradeo según el nivel en que se desbloquea (como vanilla).
    private static final int XP_L1 = 2;
    private static final int XP_L2 = 5;
    private static final int XP_L3 = 10;
    private static final int XP_L4 = 15;
    private static final int XP_L5 = 30;

    // ---------------- Nivel 1: Novato ----------------
    private static final List<TradeFactory> L1_BUY = List.of(
            (l, r) -> buy(new ItemCost(Items.CRYING_OBSIDIAN, 4), 1, 12, XP_L1),
            (l, r) -> buy(new ItemCost(Items.AMETHYST_SHARD, 16), 1, 12, XP_L1),
            (l, r) -> buy(new ItemCost(Items.SCULK, 12), 1, 12, XP_L1));
    private static final List<TradeFactory> L1_SELL = List.of(
            (l, r) -> sell(new ItemCost(Items.EMERALD, 1), null, new ItemStack(ModBlocks.NOXITE_BRICKS, 4), 12, XP_L1),
            (l, r) -> sell(new ItemCost(Items.EMERALD, 3), null, new ItemStack(ModBlocks.GLOWING_NOXITE_BRICKS, 2), 12, XP_L1));

    // ---------------- Nivel 2: Aprendiz ----------------
    private static final List<TradeFactory> L2_BUY = List.of(
            (l, r) -> buy(new ItemCost(ModItems.NIXIL, 2), 3, 8, XP_L2),
            (l, r) -> buy(new ItemCost(Items.ECHO_SHARD, 1), 4, 4, XP_L2));
    private static final List<TradeFactory> L2_SELL = List.of(
            (l, r) -> sell(new ItemCost(Items.EMERALD, 10), null, new ItemStack(Items.DIAMOND), 4, XP_L2),
            (l, r) -> sell(new ItemCost(Items.EMERALD, 22), new ItemCost(Items.DIAMOND, 1),
                    enchant(l, new ItemStack(Items.DIAMOND_PICKAXE), Enchantments.EFFICIENCY, 3, Enchantments.UNBREAKING, 2), 3, XP_L2));

    // ---------------- Nivel 3: Oficial ----------------
    private static final List<TradeFactory> L3_BUY = List.of(
            (l, r) -> buy(new ItemCost(ModItems.RAW_NOXUS, 1), 8, 6, XP_L3));
    private static final List<TradeFactory> L3_SELL = List.of(
            (l, r) -> sell(new ItemCost(Items.EMERALD, 22), new ItemCost(Items.DIAMOND, 1),
                    enchant(l, new ItemStack(Items.DIAMOND_AXE), Enchantments.EFFICIENCY, 3, Enchantments.UNBREAKING, 2), 3, XP_L3),
            (l, r) -> sell(new ItemCost(Items.EMERALD, 20), new ItemCost(Items.DIAMOND, 1),
                    enchant(l, new ItemStack(Items.DIAMOND_SWORD), Enchantments.SHARPNESS, 3, Enchantments.LOOTING, 1), 3, XP_L3));

    // ---------------- Nivel 4: Experto ----------------
    private static final List<TradeFactory> L4_BUY = List.of(
            (l, r) -> buy(new ItemCost(Items.ECHO_SHARD, 2), 9, 4, XP_L4),
            (l, r) -> buy(new ItemCost(ModItems.NIXIL, 4), 7, 6, XP_L4));
    private static final List<TradeFactory> L4_SELL = List.of(
            (l, r) -> sell(new ItemCost(Items.EMERALD, 28), new ItemCost(Items.DIAMOND, 2),
                    enchant(l, new ItemStack(Items.DIAMOND_CHESTPLATE), Enchantments.PROTECTION, 3, Enchantments.UNBREAKING, 1), 2, XP_L4),
            (l, r) -> sell(new ItemCost(Items.EMERALD, 18), new ItemCost(Items.DIAMOND, 1),
                    enchant(l, new ItemStack(Items.DIAMOND_BOOTS), Enchantments.FEATHER_FALLING, 3, Enchantments.PROTECTION, 2), 3, XP_L4),
            (l, r) -> sell(new ItemCost(Items.EMERALD, 24), null, new ItemStack(ModItems.RAW_NOXUS), 2, XP_L4));

    // ---------------- Nivel 5: Maestro (siempre los mismos: lo mejor) ----------------
    private static final TradeFactory MASTER_GEM = (l, r) ->
            sell(new ItemCost(Items.EMERALD, 36), new ItemCost(Items.DIAMOND, 1), new ItemStack(ModItems.NOXUS_INGOT), 1, XP_L5);
    private static final TradeFactory MASTER_COSMOS = (l, r) ->
            sell(new ItemCost(Items.EMERALD, 40), new ItemCost(Items.DIAMOND, 2), new ItemStack(ModItems.COSMOS_PICKAXE), 1, XP_L5);

    /** Agrega los 2 tradeos que desbloquea un nivel. */
    public static void addLevelOffers(int level, MerchantOffers offers, ServerLevel serverLevel, RandomSource random) {
        switch (level) {
            case 1 -> addPair(offers, L1_BUY, L1_SELL, serverLevel, random);
            case 2 -> addPair(offers, L2_BUY, L2_SELL, serverLevel, random);
            case 3 -> addPair(offers, L3_BUY, L3_SELL, serverLevel, random);
            case 4 -> addPair(offers, L4_BUY, L4_SELL, serverLevel, random);
            case 5 -> {
                offers.add(MASTER_GEM.create(serverLevel, random));
                offers.add(MASTER_COSMOS.create(serverLevel, random));
            }
            default -> { }
        }
    }

    private static void addPair(MerchantOffers offers, List<TradeFactory> buys, List<TradeFactory> sells,
                                ServerLevel level, RandomSource random) {
        offers.add(buys.get(random.nextInt(buys.size())).create(level, random));
        offers.add(sells.get(random.nextInt(sells.size())).create(level, random));
    }

    // ---------------- Helpers ----------------

    private static MerchantOffer buy(ItemCost cost, int emeralds, int maxUses, int xp) {
        return new MerchantOffer(cost, new ItemStack(Items.EMERALD, emeralds), maxUses, xp, 0.05F);
    }

    private static MerchantOffer sell(ItemCost costA, ItemCost costB, ItemStack result, int maxUses, int xp) {
        return new MerchantOffer(costA, Optional.ofNullable(costB), result, maxUses, xp, 0.05F);
    }

    /** Aplica dos encantamientos (los encantamientos son datos dinámicos: se buscan en el registro del mundo). */
    private static ItemStack enchant(ServerLevel level, ItemStack stack,
                                     ResourceKey<Enchantment> first, int firstLevel,
                                     ResourceKey<Enchantment> second, int secondLevel) {
        var lookup = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> a = lookup.getOrThrow(first);
        Holder<Enchantment> b = lookup.getOrThrow(second);
        stack.enchant(a, firstLevel);
        stack.enchant(b, secondLevel);
        return stack;
    }
}
