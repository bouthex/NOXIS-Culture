package com.noxisculture.entity.custom;

import com.noxisculture.entity.mood.NoxisMood;
import com.noxisculture.entity.mood.NoxisMoodController;
import com.noxisculture.entity.trade.NoxisVillagerTrades;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
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
public class NoxisVillager extends AbstractVillager {
    /** Reposición de ofertas: dos veces por día de Minecraft. */
    private static final long RESTOCK_INTERVAL_TICKS = 12_000L;
    private static final float VOICE_PITCH_MULTIPLIER = 1.4F; // voz aguda = "cute"
    /** 1 de cada N ticks larga humito el sombrero (~cada 6 s en promedio). */
    private static final int HAT_SMOKE_CHANCE = 120;
    private static final double HAT_TOP_HEIGHT = 1.75D;

    private static final EntityDataAccessor<Byte> MOOD =
            SynchedEntityData.defineId(NoxisVillager.class, EntityDataSerializers.BYTE);
    private static final int HAPPY_AFTER_TRADE_TICKS = 60;
    private static final float MOOD_BLEND_SPEED = 0.15F;

    private final NoxisMoodController moodController = new NoxisMoodController();
    private long lastRestockGameTime;

    // Progresión de comercio (como el aldeano vanilla).
    private int merchantLevel = 1;
    private int merchantXp;
    private boolean pendingLevelUp;

    // Solo cliente: transición suave entre emociones (0 = nada, 1 = completo).
    private float happyAnim;
    private float scaredAnim;

    public NoxisVillager(EntityType<? extends NoxisVillager> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MOOD, NoxisMood.NEUTRAL.id());
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
     * Al cerrar el menú: sube de nivel si corresponde y renueva las ofertas agotadas.
     * (Arregla que el menú siguiera mostrando tradeos ya usados.)
     */
    @Override
    protected void stopTrading() {
        super.stopTrading();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.pendingLevelUp) {
            this.pendingLevelUp = false;
            this.merchantLevel++;
            NoxisVillagerTrades.addLevelOffers(this.merchantLevel, this.getOffers(), serverLevel, this.random);
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    this.getX(), this.getY() + 1.2D, this.getZ(), 12, 0.4D, 0.4D, 0.4D, 0.0D);
            this.moodController.makeHappy(HAPPY_AFTER_TRADE_TICKS * 2);
        }
        NoxisVillagerTrades.replaceExhausted(this.getOffers(), serverLevel, this.random);
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
        NoxisMood mood = this.moodController.tick(this);
        if (mood != this.getMood()) {
            this.entityData.set(MOOD, mood.id());
        }
        // Reposición de ofertas.
        if (!this.isTrading()) {
            long now = this.level().getGameTime();
            if (now - this.lastRestockGameTime >= RESTOCK_INTERVAL_TICKS) {
                this.getOffers().forEach(MerchantOffer::resetUses);
                this.lastRestockGameTime = now;
            }
        }
    }

    private void clientTick() {
        NoxisMood mood = this.getMood();
        this.happyAnim = approach(this.happyAnim, mood == NoxisMood.HAPPY ? 1.0F : 0.0F);
        this.scaredAnim = approach(this.scaredAnim, mood == NoxisMood.SCARED ? 1.0F : 0.0F);

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
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.lastRestockGameTime = input.getLong("last_restock").orElse(0L);
        this.merchantLevel = Math.max(1, input.getInt("merchant_level").orElse(1));
        this.merchantXp = input.getInt("merchant_xp").orElse(0);
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

    // ---------------- Sonidos (placeholders vanilla con tono agudo) ----------------
    // Reemplazar por SoundEvents propios en la etapa de "Sonidos".

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isTrading() ? SoundEvents.WANDERING_TRADER_TRADE : SoundEvents.WANDERING_TRADER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WANDERING_TRADER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WANDERING_TRADER_DEATH;
    }

    @Override
    protected SoundEvent getTradeUpdatedSound(boolean validTrade) {
        return validTrade ? SoundEvents.WANDERING_TRADER_YES : SoundEvents.WANDERING_TRADER_NO;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.WANDERING_TRADER_YES;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * VOICE_PITCH_MULTIPLIER;
    }
}
