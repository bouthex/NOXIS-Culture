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
 * Tabla de comercio del Aldeano Noxis, por NIVELES (como los aldeanos vanilla).
 *  Nivel 1: 2 compras + 3 ventas básicas.
 *  Nivel 2: +2 ofertas de Noxus.
 *  Nivel 3: +Pico del Cosmos garantizado.
 * Las ofertas agotadas se reemplazan por otras nuevas al terminar de comerciar.
 */
public final class NoxisVillagerTrades {
    private NoxisVillagerTrades() {}

    @FunctionalInterface
    public interface TradeFactory {
        MerchantOffer create(ServerLevel level, RandomSource random);
    }

    public static final int MAX_LEVEL = 3;

    /** XP total necesaria para pasar del nivel N al N+1 (mismos umbrales que vanilla). */
    public static int xpToLevelUp(int level) {
        return switch (level) {
            case 1 -> 10;
            case 2 -> 70;
            default -> Integer.MAX_VALUE;
        };
    }

    /** Lo que el Noxis le COMPRA al jugador (fuente de esmeraldas). */
    private static final List<TradeFactory> BUYS = List.of(
            (l, r) -> buy(new ItemCost(Items.CRYING_OBSIDIAN, 4), 1, 12),
            (l, r) -> buy(new ItemCost(Items.AMETHYST_SHARD, 16), 1, 12),
            (l, r) -> buy(new ItemCost(Items.SCULK, 12), 1, 12),
            (l, r) -> buy(new ItemCost(Items.ECHO_SHARD, 1), 4, 4),
            (l, r) -> buy(new ItemCost(ModItems.NIXIL, 2), 3, 8)
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

    /** Nivel 2: el Noxus (lo valoran mucho, así que lo pagan bien y lo venden caro). */
    private static final List<TradeFactory> NOXUS_TRADES = List.of(
            (l, r) -> buy(new ItemCost(ModItems.RAW_NOXUS, 1), 8, 6),
            (l, r) -> sell(new ItemCost(Items.EMERALD, 24), null, new ItemStack(ModItems.RAW_NOXUS), 2, 20),
            (l, r) -> sell(new ItemCost(Items.EMERALD, 36), new ItemCost(Items.DIAMOND, 1),
                    new ItemStack(ModItems.NOXUS_INGOT), 1, 25)
    );

    /** Nivel 3: oferta estrella, 1 sola unidad por reposición. */
    private static final TradeFactory COSMOS_PICKAXE = (l, r) ->
            sell(new ItemCost(Items.EMERALD, 40), new ItemCost(Items.DIAMOND, 2),
                    new ItemStack(ModItems.COSMOS_PICKAXE), 1, 30);

    /** Agrega las ofertas que desbloquea un nivel. */
    public static void addLevelOffers(int level, MerchantOffers offers, ServerLevel serverLevel, RandomSource random) {
        switch (level) {
            case 1 -> {
                addRandom(offers, BUYS, 2, serverLevel, random);
                addRandom(offers, SELLS, 3, serverLevel, random);
            }
            case 2 -> addRandom(offers, NOXUS_TRADES, 2, serverLevel, random);
            case 3 -> offers.add(COSMOS_PICKAXE.create(serverLevel, random));
            default -> { }
        }
    }

    /**
     * Rotación: cambia cada oferta AGOTADA por una nueva del mismo tipo.
     * Se llama al cerrar el menú de comercio, así nunca cambia algo mientras lo estás usando.
     */
    public static boolean replaceExhausted(MerchantOffers offers, ServerLevel level, RandomSource random) {
        boolean changed = false;
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer old = offers.get(i);
            if (!old.isOutOfStock() || old.getResult().is(ModItems.COSMOS_PICKAXE)) {
                continue; // el Pico del Cosmos solo vuelve con la reposición normal
            }
            boolean isBuy = old.getResult().is(Items.EMERALD);
            List<TradeFactory> pool = isBuy ? BUYS : SELLS;
            offers.set(i, pool.get(random.nextInt(pool.size())).create(level, random));
            changed = true;
        }
        return changed;
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
