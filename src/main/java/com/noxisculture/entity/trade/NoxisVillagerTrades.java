package com.noxisculture.entity.trade;

import com.noxisculture.item.ModItems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
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
 * Tabla de comercio del Aldeano Noxis.
 * Cada Noxis genera: 2 compras + 3 ventas al azar, y con 35% de chance
 * ofrece además el Pico del Cosmos. Así cada aldea "se siente" distinta.
 */
public final class NoxisVillagerTrades {
    private NoxisVillagerTrades() {}

    @FunctionalInterface
    public interface TradeFactory {
        MerchantOffer create(ServerLevel level, RandomSource random);
    }

    private static final int BUY_COUNT = 2;
    private static final int SELL_COUNT = 3;
    private static final float COSMOS_CHANCE = 0.35F;

    /** Lo que el Noxis le COMPRA al jugador (fuente de esmeraldas). */
    private static final List<TradeFactory> BUYS = List.of(
            (l, r) -> buy(new ItemCost(Items.CRYING_OBSIDIAN, 4), 1, 12),
            (l, r) -> buy(new ItemCost(Items.AMETHYST_SHARD, 16), 1, 12),
            (l, r) -> buy(new ItemCost(Items.SCULK, 12), 1, 12),
            (l, r) -> buy(new ItemCost(Items.ECHO_SHARD, 1), 4, 4)
    );

    /** Lo que el Noxis le VENDE al jugador. Precios pensados para inicio/mid-game. */
    private static final List<TradeFactory> SELLS = List.of(
            (l, r) -> sell(new ItemCost(Items.EMERALD, 10), null, new ItemStack(Items.DIAMOND), 4, 10),
            (l, r) -> sell(new ItemCost(Items.EMERALD, 22), new ItemCost(Items.DIAMOND, 1),
                    enchant(l, new ItemStack(Items.DIAMOND_PICKAXE), Enchantments.EFFICIENCY, 3, Enchantments.UNBREAKING, 2), 3, 15),
            (l, r) -> sell(new ItemCost(Items.EMERALD, 22), new ItemCost(Items.DIAMOND, 1),
                    enchant(l, new ItemStack(Items.DIAMOND_AXE), Enchantments.EFFICIENCY, 3, Enchantments.UNBREAKING, 2), 3, 15),
            (l, r) -> sell(new ItemCost(Items.EMERALD, 20), new ItemCost(Items.DIAMOND, 1),
                    enchant(l, new ItemStack(Items.DIAMOND_SWORD), Enchantments.SHARPNESS, 3, Enchantments.LOOTING, 1), 3, 15),
            (l, r) -> sell(new ItemCost(Items.EMERALD, 28), new ItemCost(Items.DIAMOND, 2),
                    enchant(l, new ItemStack(Items.DIAMOND_CHESTPLATE), Enchantments.PROTECTION, 3, Enchantments.UNBREAKING, 1), 2, 20),
            (l, r) -> sell(new ItemCost(Items.EMERALD, 18), new ItemCost(Items.DIAMOND, 1),
                    enchant(l, new ItemStack(Items.DIAMOND_BOOTS), Enchantments.FEATHER_FALLING, 3, Enchantments.PROTECTION, 2), 3, 15)
    );

    /** Oferta estrella: 1 sola unidad por reposición. */
    private static final TradeFactory COSMOS_PICKAXE = (l, r) ->
            sell(new ItemCost(Items.EMERALD, 40), new ItemCost(Items.DIAMOND, 2),
                    new ItemStack(ModItems.COSMOS_PICKAXE), 1, 30);

    public static void fill(MerchantOffers offers, ServerLevel level, RandomSource random) {
        addRandom(offers, BUYS, BUY_COUNT, level, random);
        addRandom(offers, SELLS, SELL_COUNT, level, random);
        if (random.nextFloat() < COSMOS_CHANCE) {
            offers.add(COSMOS_PICKAXE.create(level, random));
        }
    }

    // ---------------- Helpers ----------------

    private static void addRandom(MerchantOffers offers, List<TradeFactory> pool, int amount,
                                  ServerLevel level, RandomSource random) {
        List<TradeFactory> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, new Random(random.nextLong()));
        shuffled.stream().limit(amount).forEach(f -> offers.add(f.create(level, random)));
    }

    private static MerchantOffer buy(ItemCost cost, int emeralds, int maxUses) {
        return new MerchantOffer(cost, new ItemStack(Items.EMERALD, emeralds), maxUses, 2, 0.05F);
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
