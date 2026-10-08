package com.noxisculture.entity.idle;

import com.noxisculture.block.ModBlocks;
import com.noxisculture.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * "Fascinación por los cristales" (solo visual, se calcula en el cliente): un Noxis tranquilo
 * que tiene cerca amatista o algo cristalino de Noxus, de vez en cuando se queda mirándolo,
 * ladea la cabecita, para las orejitas y sus ojos brillan más con un pulso suave. Muy de vez
 * en cuando da un saltito de entusiasmo. Al terminar, el brillo baja despacito.
 *
 * <p>Reutilizable por todas las especies Noxis (no depende de la ropa): la entidad decide si
 * está "tranquila", llama a {@link #tick} y el render lee los getters.</p>
 *
 * <p>Solo usa bloques e ítems que ya existen (vanilla + Noxis Culture); no agrega contenido.</p>
 */
public final class NoxisCrystalFascination {
    /** Bloques cristalinos: familia de la amatista + lo cristalino de Noxus. */
    private static final Set<Block> CRYSTAL_BLOCKS = Set.of(
            Blocks.AMETHYST_BLOCK, Blocks.BUDDING_AMETHYST, Blocks.AMETHYST_CLUSTER,
            Blocks.LARGE_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD, Blocks.SMALL_AMETHYST_BUD,
            ModBlocks.NOXUS_BLOCK, ModBlocks.NOXUS_ORE, ModBlocks.DEEPSLATE_NOXUS_ORE,
            ModBlocks.GLOWING_NOXITE_BRICKS);
    /** Ítems cristalinos (en el piso o en la mano de un jugador). Los bloques de arriba también cuentan. */
    private static final Set<Item> CRYSTAL_ITEMS = Set.of(
            Items.AMETHYST_SHARD, ModItems.NIXIL, ModItems.RAW_NOXUS, ModItems.NOXUS_INGOT);

    /** Radio de búsqueda (bloques) en horizontal y en vertical. */
    private static final int RADIUS = 5;
    private static final int RADIUS_Y = 2;
    /** Cada cuánto busca cristales (1 s): barato aunque haya muchos Noxis. */
    private static final int SCAN_INTERVAL = 20;
    /** Tiempo tranquilo mínimo antes de que pueda pasar (3 s). */
    private static final int MIN_IDLE_TICKS = 60;
    /** Probabilidad por tick con un cristal cerca: en promedio, cada ~15 s. */
    private static final int CHANCE = 300;
    /** El saltito de entusiasmo: 1 de cada 6 fascinaciones. */
    private static final int HOP_CHANCE = 6;

    private static final int RISE = 10;     // gira la cabeza hacia el cristal
    private static final int HOLD = 56;     // lo mira embelesado (~2,8 s)
    private static final int FALL = 14;     // vuelve a su postura
    private static final int GLOW_IN = 16;  // el brillo sube un poco más lento que la cabeza
    private static final int GLOW_OUT = 30; // y baja todavía más lento, al final
    private static final int HOP_AT = 22;   // en qué momento de la pausa salta (si salta)
    private static final int HOP_LEN = 8;

    private int idleTicks;
    private int scanCooldown;
    private int phase = -1;
    private boolean willHop;
    private Vec3 target;
    private float side = 1.0F;
    private float lookYaw;
    private float lookPitch;

    private float amount;
    private float amountO;
    private float glow;
    private float glowO;
    private float hop;
    private float hopO;
    private float twitch;
    private float twitchO;

    public void tick(LivingEntity entity, boolean calm, RandomSource random) {
        this.amountO = this.amount;
        this.glowO = this.glow;
        this.hopO = this.hop;
        this.twitchO = this.twitch;

        if (!calm) {
            // Comercio, emociones, antorcha, paraguas, descanso, caminar...: tienen prioridad.
            // La cabeza vuelve rápido; el brillo se apaga suave igual.
            this.idleTicks = 0;
            this.phase = -1;
            this.amount = Math.max(0.0F, this.amount - 0.2F);
            this.glow = Math.max(0.0F, this.glow - 1.0F / GLOW_OUT);
            this.hop = 0.0F;
            this.twitch = 0.0F;
            return;
        }

        if (this.phase < 0) {
            this.amount = Math.max(0.0F, this.amount - 0.2F);
            this.glow = Math.max(0.0F, this.glow - 1.0F / GLOW_OUT);
            this.hop = 0.0F;
            this.twitch = 0.0F;
            this.idleTicks++;
            if (--this.scanCooldown <= 0) {
                this.scanCooldown = SCAN_INTERVAL + random.nextInt(10);
                this.target = findCrystal(entity);
            }
            if (this.target != null && this.idleTicks > MIN_IDLE_TICKS && random.nextInt(CHANCE) == 0) {
                this.phase = 0;
                this.side = random.nextBoolean() ? 1.0F : -1.0F;
                this.willHop = random.nextInt(HOP_CHANCE) == 0;
            }
            return;
        }

        // Si el cristal desapareció (lo minaron, lo levantaron, el jugador se fue), termina antes.
        if (this.phase < RISE + HOLD && this.phase % 10 == 0 && !stillThere(entity, this.target)) {
            this.phase = RISE + HOLD;
        }
        this.updateLook(entity);

        this.phase++;
        int end = RISE + HOLD + FALL;
        if (this.phase < RISE) {
            this.amount = smooth(this.phase / (float) RISE);
        } else if (this.phase < RISE + HOLD) {
            this.amount = 1.0F;
        } else if (this.phase < end) {
            this.amount = smooth(1.0F - (this.phase - RISE - HOLD) / (float) FALL);
        } else {
            this.amount = 0.0F;
            this.phase = -1;
            this.idleTicks = 0;   // vuelve a esperar un rato tranquilo antes de repetir
        }

        // Brillo: sube suave, late despacito mientras lo mira y después se apaga lento.
        if (this.phase >= 0 && this.phase < RISE + HOLD) {
            float in = smooth(Math.min(1.0F, this.phase / (float) GLOW_IN));
            float pulse = 0.82F + 0.18F * Mth.sin((this.phase - GLOW_IN) * 0.16F);
            this.glow = in * (this.phase < GLOW_IN ? 1.0F : pulse);
        } else {
            this.glow = Math.max(0.0F, this.glow - 1.0F / GLOW_OUT);
        }

        // Orejitas: dos golpecitos alternados al principio y uno a mitad de la pausa.
        int p = this.phase - RISE;
        this.twitch = (p == 4 || p == 5) ? 1.0F : (p == 9 || p == 10) ? -1.0F : (p == 34) ? 1.0F : 0.0F;

        // Saltito de entusiasmo (raro): una parábola corta.
        if (this.willHop && p >= HOP_AT && p < HOP_AT + HOP_LEN) {
            float x = (p - HOP_AT + 1) / (float) HOP_LEN;
            this.hop = 4.0F * x * (1.0F - x);
        } else {
            this.hop = 0.0F;
        }
    }

    private void updateLook(LivingEntity entity) {
        if (this.target == null) return;
        double dx = this.target.x - entity.getX();
        double dz = this.target.z - entity.getZ();
        double dy = this.target.y - entity.getEyeY();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float rel = Mth.wrapDegrees(yaw - entity.yBodyRot);
        this.lookYaw = Mth.clamp(rel, -60.0F, 60.0F) * Mth.DEG_TO_RAD;
        float pitch = (float) (-Mth.atan2(dy, Math.max(horiz, 0.3D)) * Mth.RAD_TO_DEG);
        this.lookPitch = Mth.clamp(pitch, -35.0F, 45.0F) * Mth.DEG_TO_RAD;
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    // ------------------------------------------------------------------ búsqueda

    public static boolean isCrystal(BlockState state) {
        return CRYSTAL_BLOCKS.contains(state.getBlock());
    }

    public static boolean isCrystal(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (CRYSTAL_ITEMS.contains(stack.getItem())) return true;
        return stack.getItem() instanceof BlockItem blockItem && CRYSTAL_BLOCKS.contains(blockItem.getBlock());
    }

    /** El cristal más cercano: primero lo que muestra un jugador en la mano, después ítems y bloques. */
    private static Vec3 findCrystal(LivingEntity entity) {
        Level level = entity.level();
        AABB area = entity.getBoundingBox().inflate(RADIUS, RADIUS_Y, RADIUS);
        Vec3 best = null;
        double bestDist = Double.MAX_VALUE;

        for (Player player : level.getEntitiesOfClass(Player.class, area, p -> !p.isSpectator())) {
            for (InteractionHand hand : InteractionHand.values()) {
                if (isCrystal(player.getItemInHand(hand))) {
                    Vec3 v = new Vec3(player.getX(), player.getY() + player.getBbHeight() * 0.55D, player.getZ());
                    double d = v.distanceToSqr(entity.position());
                    if (d < bestDist) { bestDist = d; best = v; }
                }
            }
        }
        if (best != null) return best;   // un jugador mostrándole un cristal le gana a todo

        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area, i -> isCrystal(i.getItem()))) {
            Vec3 v = item.position().add(0.0D, 0.15D, 0.0D);
            double d = v.distanceToSqr(entity.position());
            if (d < bestDist) { bestDist = d; best = v; }
        }

        BlockPos origin = entity.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-RADIUS, -RADIUS_Y, -RADIUS),
                origin.offset(RADIUS, RADIUS_Y + 1, RADIUS))) {
            if (isCrystal(level.getBlockState(pos))) {
                Vec3 v = Vec3.atCenterOf(pos);
                double d = v.distanceToSqr(entity.position());
                if (d < bestDist) { bestDist = d; best = v; }
            }
        }
        return best;
    }

    private static boolean stillThere(LivingEntity entity, Vec3 target) {
        if (target == null) return false;
        Level level = entity.level();
        if (isCrystal(level.getBlockState(BlockPos.containing(target)))) return true;
        AABB box = new AABB(target, target).inflate(1.0D);
        if (!level.getEntitiesOfClass(ItemEntity.class, box, i -> isCrystal(i.getItem())).isEmpty()) return true;
        for (Player player : level.getEntitiesOfClass(Player.class, box.inflate(0.5D), p -> !p.isSpectator())) {
            if (isCrystal(player.getMainHandItem()) || isCrystal(player.getOffhandItem())) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ getters (render)

    public boolean isActive() {
        return this.phase >= 0;
    }

    public float getAmount(float partialTick) {
        return Mth.lerp(partialTick, this.amountO, this.amount);
    }

    /** 0..1: brillo extra de los ojos (con el pulso ya incluido). */
    public float getGlow(float partialTick) {
        return Mth.lerp(partialTick, this.glowO, this.glow);
    }

    /** 0..1: altura del saltito de entusiasmo. */
    public float getHop(float partialTick) {
        return Mth.lerp(partialTick, this.hopO, this.hop);
    }

    /** -1..1: golpecito de oreja (positivo derecha, negativo izquierda). */
    public float getTwitch(float partialTick) {
        return Mth.lerp(partialTick, this.twitchO, this.twitch);
    }

    public float getSide() {
        return this.side;
    }

    /** Giro de la cabeza hacia el cristal, relativo al cuerpo (radianes). */
    public float getLookYaw() {
        return this.lookYaw;
    }

    /** Inclinación de la cabeza hacia el cristal (radianes, positivo = mira abajo). */
    public float getLookPitch() {
        return this.lookPitch;
    }
}
