package com.noxisculture.entity.custom;

import com.noxisculture.entity.ai.NoxisRestGoal;
import com.noxisculture.entity.ai.NoxisRestful;
import com.noxisculture.entity.ai.NoxisWorkAtTableGoal;
import com.noxisculture.entity.ai.NoxisWorker;
import net.minecraft.core.BlockPos;
import com.noxisculture.entity.light.NoxisLightController;
import com.noxisculture.sound.ModSounds;
import com.noxisculture.entity.light.NoxisLightSource;
import com.noxisculture.entity.mood.NoxisMood;
import com.noxisculture.entity.mood.NoxisMoodController;
import com.noxisculture.entity.trade.NoxisVillagerTrades;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Aldeano Noxis (comerciante común).
 * Hereda de AbstractVillager, la misma base que Villager y WanderingTrader:
 * nos da gratis el menú de comercio vanilla, el guardado de ofertas y la
 * integración con TradeWithPlayerGoal, sin el sistema de profesiones/cerebro.
 */
public class NoxisVillager extends AbstractVillager implements NoxisLightSource, NoxisRestful, NoxisWorker {
    /** Puede reponer en su mesa como mucho cada medio día (~2 veces por día, como vanilla). */
    private static final long RESTOCK_INTERVAL_TICKS = 12_000L;
    /** Espera tras cerrar el menú antes de subir de nivel (vanilla: 40 ticks = 2 s). */
    private static final int LEVEL_UP_DELAY_TICKS = 40;
    /** 1 de cada N ticks larga humito el sombrero (~cada 6 s en promedio). */
    private static final int HAT_SMOKE_CHANCE = 120;
    private static final double HAT_TOP_HEIGHT = 1.75D;

    private static final EntityDataAccessor<Byte> MOOD =
            SynchedEntityData.defineId(NoxisVillager.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> TORCH =
            SynchedEntityData.defineId(NoxisVillager.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> UMBRELLA =
            SynchedEntityData.defineId(NoxisVillager.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RESTING =
            SynchedEntityData.defineId(NoxisVillager.class, EntityDataSerializers.BOOLEAN);
    private static final int HAPPY_AFTER_TRADE_TICKS = 60;
    /** ~1 vez por día de Minecraft (24000 ticks), con algo de azar. */
    private static final int REST_COOLDOWN_TICKS = 20_000;
    private static final int REST_COOLDOWN_RANDOM = 6_000;

    // Luz dinámica: gemas y ojos (siempre), más fuerte al estar feliz, máxima con la antorcha.
    private static final int LIGHT_BASE = 6;
    private static final int LIGHT_HAPPY = 9;
    private static final int LIGHT_TORCH = 13;
    /** Por debajo de esta luz del cielo (cuevas) también sacan la antorcha. */
    private static final int CAVE_SKY_LIGHT = 4;
    private static final float MOOD_BLEND_SPEED = 0.15F;

    private final NoxisMoodController moodController = new NoxisMoodController();
    private final NoxisLightController lightController = new NoxisLightController();
    private long lastRestockGameTime;

    // Progresión de comercio (como el aldeano vanilla).
    private int merchantLevel = 1;
    private int merchantXp;
    private boolean pendingLevelUp;
    private int levelUpTimer;

    // Solo cliente: transición suave entre emociones (0 = nada, 1 = completo).
    private float happyAnim;
    private float scaredAnim;
    private float torchAnim;
    private float restAnim;
    private float umbrellaAnim;
    /** El primer descanso llega pronto (1-5 min) para poder verlo; después, ~1 por día. */
    private int restCooldown = 1_200 + (int) (Math.random() * 4_800);

    public NoxisVillager(EntityType<? extends NoxisVillager> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MOOD, NoxisMood.NEUTRAL.id());
        builder.define(TORCH, false);
        builder.define(RESTING, false);
        builder.define(UMBRELLA, false);
    }

    public NoxisMood getMood() {
        return NoxisMood.byId(this.entityData.get(MOOD));
    }

    public float getHappyAnim() {
        return this.happyAnim;
    }

    public float getScaredAnim() {
        return this.scaredAnim;
    }

    public float getTorchAnim() {
        return this.torchAnim;
    }

    public boolean isHoldingTorch() {
        return this.entityData.get(TORCH);
    }

    public float getUmbrellaAnim() {
        return this.umbrellaAnim;
    }

    public boolean isHoldingUmbrella() {
        return this.entityData.get(UMBRELLA);
    }

    public float getRestAnim() {
        return this.restAnim;
    }

    // ---------------- Descanso (NoxisRestful) ----------------

    public boolean isResting() {
        return this.entityData.get(RESTING);
    }

    @Override
    public boolean wantsToRest() {
        return this.restCooldown <= 0 && this.isSafeToRest();
    }

    @Override
    public boolean isSafeToRest() {
        return this.getMood() == NoxisMood.NEUTRAL && !this.isTrading() && !this.isHoldingTorch()
                && !this.isHoldingUmbrella();
    }

    @Override
    public void setResting(boolean resting) {
        this.entityData.set(RESTING, resting);
    }

    @Override
    public void onRestFinished() {
        this.restCooldown = REST_COOLDOWN_TICKS + this.random.nextInt(REST_COOLDOWN_RANDOM);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)       // aldeano vanilla: 20
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Monster.class, 8.0F, 0.5D, 0.6D));
        this.goalSelector.addGoal(1, new PanicGoal(this, 0.6D));
        this.goalSelector.addGoal(2, new LookAtTradingPlayerGoal(this));
        this.goalSelector.addGoal(3, new NoxisRestGoal<>(this));
        this.goalSelector.addGoal(4, new NoxisWorkAtTableGoal<>(this, 0.45D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.35D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    // ---------------- Comercio ----------------

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.isAlive() && !this.isTrading() && !this.isBaby()) {
            if (hand == InteractionHand.MAIN_HAND) {
                player.awardStat(Stats.TALKED_TO_VILLAGER);
            }
            if (!this.level().isClientSide()) {
                if (this.getOffers().isEmpty()) {
                    return InteractionResult.CONSUME;
                }
                this.setTradingPlayer(player);
                this.openTradingScreen(player, this.getDisplayName(), this.merchantLevel);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected void updateTrades(ServerLevel level) {
        // Toda la tabla de tradeos vive en su propia clase: fácil de balancear y portar.
        // Al crearse: ofertas de todos los niveles ya alcanzados.
        for (int lvl = 1; lvl <= this.merchantLevel; lvl++) {
            NoxisVillagerTrades.addLevelOffers(lvl, this.getOffers(), level, this.random);
        }
    }

    /**
     * Sube de nivel (como vanilla): se llama desde tick() cuando ya NO está comerciando,
     * 2 segundos después de cerrar el menú. Puede subir más de un nivel si comerciaste mucho.
     * Desbloquea 2 tradeos nuevos por nivel y lo festeja.
     */
    private void levelUp(ServerLevel serverLevel) {
        this.pendingLevelUp = false;
        while (this.merchantLevel < NoxisVillagerTrades.MAX_LEVEL
                && this.merchantXp >= NoxisVillagerTrades.xpToLevelUp(this.merchantLevel)) {
            this.merchantLevel++;
            NoxisVillagerTrades.addLevelOffers(this.merchantLevel, this.getOffers(), serverLevel, this.random);
        }
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                this.getX(), this.getY() + 1.2D, this.getZ(), 16, 0.4D, 0.4D, 0.4D, 0.0D);
        this.playSound(ModSounds.NOXIS_CELEBRATE, 1.2F, 1.1F);
        this.moodController.makeHappy(HAPPY_AFTER_TRADE_TICKS * 2);
    }

    // ---------------- Trabajo en la mesa (NoxisWorker) ----------------

    @Override
    public boolean needsToWork() {
        if (this.isTrading() || this.isHoldingTorch() || this.isResting()) {
            return false;
        }
        if (this.level().getGameTime() - this.lastRestockGameTime < RESTOCK_INTERVAL_TICKS) {
            return false;
        }
        for (MerchantOffer offer : this.getOffers()) {
            if (offer.getUses() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void workAt(BlockPos table) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.getOffers().forEach(MerchantOffer::resetUses);
        this.lastRestockGameTime = serverLevel.getGameTime();
        serverLevel.playSound(null, table, SoundEvents.SMITHING_TABLE_USE, SoundSource.NEUTRAL, 0.8F, 1.2F);
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                table.getX() + 0.5D, table.getY() + 1.1D, table.getZ() + 0.5D, 6, 0.3D, 0.2D, 0.3D, 0.0D);
        this.moodController.makeHappy(HAPPY_AFTER_TRADE_TICKS);
    }

    @Override
    public int getVillagerXp() {
        return this.merchantXp;
    }

    @Override
    public boolean showProgressBar() {
        return true;
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        super.notifyTrade(offer);
        this.moodController.makeHappy(HAPPY_AFTER_TRADE_TICKS);
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        this.merchantXp += offer.getXp();
        if (this.merchantLevel < NoxisVillagerTrades.MAX_LEVEL
                && this.merchantXp >= NoxisVillagerTrades.xpToLevelUp(this.merchantLevel)) {
            this.pendingLevelUp = true;
            this.levelUpTimer = LEVEL_UP_DELAY_TICKS;
        }
        if (offer.shouldRewardExp()) {
            int xp = 3 + this.random.nextInt(4);
            this.level().addFreshEntity(new ExperienceOrb(this.level(), this.getX(), this.getY() + 0.5D, this.getZ(), xp));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.clientTick();
            return;
        }
        // Emociones (servidor -> se sincroniza solo al cambiar).
        if (this.restCooldown > 0) {
            this.restCooldown--;
        }
        NoxisMood mood = this.moodController.tick(this);
        if (mood != this.getMood()) {
            this.entityData.set(MOOD, mood.id());
        }
        // De noche (o en cuevas oscuras) sacan la antorcha. Se revisa cada segundo.
        if (this.tickCount % 20 == 0) {
            boolean dark = this.level().isDarkOutside()
                    || this.level().getBrightness(LightLayer.SKY, this.blockPosition()) < CAVE_SKY_LIGHT;
            if (dark != this.isHoldingTorch()) {
                this.entityData.set(TORCH, dark);
            }
            // Si llueve (o nieva) y tiene el cielo encima, saca el paraguas con la otra mano.
            boolean wet = this.level().isRaining() && this.level().canSeeSky(this.blockPosition().above());
            if (wet != this.isHoldingUmbrella()) {
                this.entityData.set(UMBRELLA, wet);
            }
        }
        // Luz dinámica (cada 2 ticks alcanza: solo escribe en el mundo si algo cambió).
        if (this.tickCount % 2 == 0) {
            int light = this.isHoldingTorch() ? LIGHT_TORCH : mood == NoxisMood.HAPPY ? LIGHT_HAPPY : LIGHT_BASE;
            this.lightController.update(this, light);
        }
        // Subida de nivel: cuando ya cerraste el menú (no depende de stopTrading,
        // que Minecraft no llama al cerrar el menú: ese era el error).
        if (this.pendingLevelUp && !this.isTrading() && --this.levelUpTimer <= 0
                && this.level() instanceof ServerLevel serverLevel) {
            this.levelUp(serverLevel);
        }
    }

    private void clientTick() {
        NoxisMood mood = this.getMood();
        this.happyAnim = approach(this.happyAnim, mood == NoxisMood.HAPPY ? 1.0F : 0.0F);
        this.scaredAnim = approach(this.scaredAnim, mood == NoxisMood.SCARED ? 1.0F : 0.0F);
        this.torchAnim = approach(this.torchAnim, this.isHoldingTorch() ? 1.0F : 0.0F);
        this.umbrellaAnim = approach(this.umbrellaAnim, this.isHoldingUmbrella() ? 1.0F : 0.0F);
        // Sentarse es más lento y pesado que el resto de las transiciones.
        float restTarget = this.isResting() ? 1.0F : 0.0F;
        this.restAnim += Mth.clamp(restTarget - this.restAnim, -0.06F, 0.06F);

        // Llamita en la punta de la antorcha.
        if (this.torchAnim > 0.9F && this.random.nextInt(5) == 0) {
            float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
            double fx = -Mth.sin(yaw), fz = Mth.cos(yaw);      // adelante
            double rx = -Mth.cos(yaw), rz = -Mth.sin(yaw);     // derecha
            this.level().addParticle(ParticleTypes.SMALL_FLAME,
                    this.getX() + rx * 0.27D + fx * 0.12D, this.getY() + 1.12D,
                    this.getZ() + rz * 0.27D + fz * 0.12D, 0.0D, 0.01D, 0.0D);
        }

        double headY = this.getY() + 1.0D;
        switch (mood) {
            case HAPPY -> {
                if (this.random.nextInt(12) == 0) {
                    this.level().addParticle(ParticleTypes.HAPPY_VILLAGER,
                            this.getRandomX(0.6D), headY + 0.3D, this.getRandomZ(0.6D), 0.0D, 0.0D, 0.0D);
                }
            }
            case SCARED -> {
                // Gotitas de sudor nervioso.
                if (this.random.nextInt(8) == 0) {
                    this.level().addParticle(ParticleTypes.SPLASH,
                            this.getRandomX(0.5D), headY, this.getRandomZ(0.5D), 0.0D, 0.0D, 0.0D);
                }
            }
            default -> {
                // Bocanada de humito desde la copa del sombrero.
                if (this.random.nextInt(HAT_SMOKE_CHANCE) == 0) {
                    this.level().addParticle(ParticleTypes.SMOKE,
                            this.getX(), this.getY() + HAT_TOP_HEIGHT, this.getZ(), 0.0D, 0.03D, 0.0D);
                }
            }
        }
    }

    private static float approach(float current, float target) {
        if (current < target) return Math.min(target, current + MOOD_BLEND_SPEED);
        return Math.max(target, current - MOOD_BLEND_SPEED);
    }

    // ---------------- Persistencia ----------------

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output); // AbstractVillager ya guarda las ofertas
        output.putLong("last_restock", this.lastRestockGameTime);
        output.putInt("merchant_level", this.merchantLevel);
        output.putInt("merchant_xp", this.merchantXp);
        output.putBoolean("pending_level_up", this.pendingLevelUp);
        output.putInt("rest_cooldown", this.restCooldown);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.lastRestockGameTime = input.getLong("last_restock").orElse(0L);
        this.merchantLevel = Math.max(1, input.getInt("merchant_level").orElse(1));
        this.merchantXp = input.getInt("merchant_xp").orElse(0);
        this.pendingLevelUp = input.getBooleanOr("pending_level_up", false);
        this.restCooldown = input.getInt("rest_cooldown").orElse(this.restCooldown);
    }

    /** Al desaparecer (muerte, descarga del chunk...), se lleva su luz. */
    @Override
    public void remove(Entity.RemovalReason reason) {
        this.lightController.clear(this.level());
        super.remove(reason);
    }

    /** Viven en aldeas: no deben desaparecer al alejarse el jugador. */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    /** Sin reproducción por ahora (se diseñará junto con las aldeas). */
    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    // ---------------- Voz Noxis (gato + extraterrestre), según su emoción ----------------

    @Override
    protected SoundEvent getAmbientSound() {
        if (this.isResting()) return ModSounds.NOXIS_YAWN;
        if (this.isTrading()) return ModSounds.NOXIS_TRADE;
        return switch (this.getMood()) {
            case HAPPY -> ModSounds.NOXIS_HAPPY;
            case SCARED -> ModSounds.NOXIS_SCARED;
            default -> ModSounds.NOXIS_AMBIENT;
        };
    }

    /** Asustados "hablan" más seguido; descansando, casi nada. */
    @Override
    public int getAmbientSoundInterval() {
        if (this.isResting()) return 400;
        return this.getMood() == NoxisMood.SCARED ? 40 : 120;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.NOXIS_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.NOXIS_DEATH;
    }

    @Override
    protected SoundEvent getTradeUpdatedSound(boolean validTrade) {
        return validTrade ? ModSounds.NOXIS_YES : ModSounds.NOXIS_NO;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return ModSounds.NOXIS_CELEBRATE; // festejo original al concretar cada tradeo
    }

}
