package com.noxisculture.entity.custom;

import com.noxisculture.entity.idle.NoxisCrystalFascination;
import com.noxisculture.entity.idle.NoxisCuriosity;
import com.noxisculture.entity.cape.NoxisCapePhysics;
import com.noxisculture.entity.ai.NoxisCrystalFascinationGoal;
import com.noxisculture.entity.ai.NoxisFascinatable;
import com.noxisculture.entity.ai.NoxisNatureGoal;
import com.noxisculture.entity.ai.NoxisNatureLover;
import com.noxisculture.entity.ai.NoxisReceiveGiftGoal;
import com.noxisculture.entity.ai.NoxisBowlSleepGoal;
import com.noxisculture.entity.ai.NoxisBowlSleeper;
import com.noxisculture.entity.ai.NoxisCompanionRestGoal;
import com.noxisculture.entity.ai.NoxisGreetGoal;
import com.noxisculture.entity.ai.NoxisSocial;
import com.noxisculture.entity.ai.NoxisSocialFollowGoal;
import com.noxisculture.entity.idle.NoxisSocialAnimation;
import com.noxisculture.entity.social.NoxisSocialAction;
import com.noxisculture.entity.social.NoxisSocialLink;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import com.noxisculture.entity.idle.NoxisNatureAnimation;
import com.noxisculture.entity.nature.NoxisFlowerCarry;
import com.noxisculture.entity.nature.NoxisNatureAction;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
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
public class NoxisVillager extends AbstractVillager implements com.noxisculture.entity.NoxisSpecies, NoxisLightSource, NoxisRestful, NoxisWorker, NoxisFascinatable, NoxisNatureLover, NoxisSocial, NoxisBowlSleeper, com.noxisculture.entity.ai.NoxisBored, com.noxisculture.entity.ai.NoxisFriend, com.noxisculture.entity.ai.NoxisCurious, com.noxisculture.entity.ai.NoxisKeeper {
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
    /** Variante de ropa: 0 = sin capa, 1..3 = capa de espalda con su estilo (se elige una vez y se guarda en NBT). */
    private static final EntityDataAccessor<Byte> VARIANT =
            SynchedEntityData.defineId(NoxisVillager.class, EntityDataSerializers.BYTE);
    public static final byte VARIANT_PLAIN = 0;
    public static final byte VARIANT_CAPE = 1;
    /** ~3 de cada 10 Noxis tienen capa. */
    private static final float CAPE_CHANCE = 0.3F;
    private static final int CAPE_STYLES = 3;
    private static final EntityDataAccessor<Boolean> RESTING =
            SynchedEntityData.defineId(NoxisVillager.class, EntityDataSerializers.BOOLEAN);
    /** true mientras mira fascinado un cristal (lo decide el servidor; el cliente anima). */
    private static final EntityDataAccessor<Boolean> FASCINATED =
            SynchedEntityData.defineId(NoxisVillager.class, EntityDataSerializers.BOOLEAN);
    /** Interacciones con la naturaleza: qué está haciendo y la flor que tiene en la mano. */
    private static final EntityDataAccessor<Byte> NATURE_ACTION =
            SynchedEntityData.defineId(NoxisVillager.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<ItemStack> HELD_FLOWER =
            SynchedEntityData.defineId(NoxisVillager.class, EntityDataSerializers.ITEM_STACK);
    /** Gesto social en curso (saludo, descanso en compañía): lo decide el servidor, el cliente lo anima. */
    private static final EntityDataAccessor<Byte> SOCIAL_ANIM =
            SynchedEntityData.defineId(NoxisVillager.class, EntityDataSerializers.BYTE);
    /** Duerme dentro de una pecera / tiene puesto el sombrero (se ven en el cliente y se guardan). */
    private static final EntityDataAccessor<Boolean> IN_BOWL =
            SynchedEntityData.defineId(NoxisVillager.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HAS_HAT =
            SynchedEntityData.defineId(NoxisVillager.class, EntityDataSerializers.BOOLEAN);
    /** El sombrero EXACTO que lleva puesto (con sus colores de copa y lazo); vacío si no tiene. */
    private static final EntityDataAccessor<ItemStack> HAT_ITEM =
            SynchedEntityData.defineId(NoxisVillager.class, EntityDataSerializers.ITEM_STACK);
    /** Animación de sacarse/ponerse el sombrero (la decide el servidor; no se guarda). */
    private static final EntityDataAccessor<Byte> HAT_ANIM =
            SynchedEntityData.defineId(NoxisVillager.class, EntityDataSerializers.BYTE);
    /** Saltito de la pecera (prepararse / entrar / salir): lo decide el servidor; no se guarda. */
    private static final EntityDataAccessor<Byte> BOWL_HOP =
            SynchedEntityData.defineId(NoxisVillager.class, EntityDataSerializers.BYTE);
    /** Gesto de aburrimiento en curso (lo decide el servidor; el cliente lo anima; no se guarda). */
    private static final EntityDataAccessor<Byte> BORED_ANIM =
            SynchedEntityData.defineId(NoxisVillager.class, EntityDataSerializers.BYTE);
    /** Acomodarse para descansar / estirarse al despertar (lo decide el servidor; no se guarda). */
    private static final EntityDataAccessor<Byte> REST_GESTURE =
            SynchedEntityData.defineId(NoxisVillager.class, EntityDataSerializers.BYTE);
    /** Gesto de cariño con su amigo especial (lo decide el servidor; no se guarda). */
    private static final EntityDataAccessor<Byte> AFFECTION_ANIM =
            SynchedEntityData.defineId(NoxisVillager.class, EntityDataSerializers.BYTE);
    /** Curiosidad / travesura en curso (lo decide el servidor; no se guarda). */
    private static final EntityDataAccessor<Byte> PLAY_ANIM =
            SynchedEntityData.defineId(NoxisVillager.class, EntityDataSerializers.BYTE);
    /** Gesto propio de la personalidad (lo decide el servidor; no se guarda). */
    private static final EntityDataAccessor<Byte> PERSONA_GESTURE =
            SynchedEntityData.defineId(NoxisVillager.class, EntityDataSerializers.BYTE);
    /** Objetos personales: gesto en curso y lo que se ve en sus manitos al contemplar (no se guardan). */
    private static final EntityDataAccessor<Byte> KEEPSAKE_ANIM =
            SynchedEntityData.defineId(NoxisVillager.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<ItemStack> KEEPSAKE_ITEM =
            SynchedEntityData.defineId(NoxisVillager.class, EntityDataSerializers.ITEM_STACK);
    /** Cuántos tipos de regalo recuerda como mucho. */
    private static final int GIFT_MEMORY_SIZE = 16;
    /** Cada Noxis se duerme a su hora: entre 0 y 2 minutos después de que oscurece. */
    private static final int BEDTIME_RANDOM = 2400;
    /** ...y se despierta a su ritmo: entre 0 y 40 s después del amanecer. */
    private static final int WAKE_RANDOM = 800;
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
    private boolean variantChosen;
    private final NoxisCapePhysics capePhysics = new NoxisCapePhysics();
    private final NoxisCuriosity curiosity = new NoxisCuriosity();
    private final NoxisCrystalFascination crystalFascination = new NoxisCrystalFascination();
    private final NoxisNatureAnimation natureAnimation = new NoxisNatureAnimation();
    private final NoxisFlowerCarry flowerCarry = new NoxisFlowerCarry();
    /** Flor de regalo que otro Noxis le lanzó y todavía viene en camino (servidor, no se guarda). */
    private final NoxisSocialLink socialLink = new NoxisSocialLink();
    // Pecera y sombrero (servidor; se guardan con el Noxis).
    private @Nullable BlockPos bowlPos;
    private @Nullable BlockPos hatPos;
    private final com.noxisculture.entity.idle.NoxisBowlHop bowlHopAnimation = new com.noxisculture.entity.idle.NoxisBowlHop();
    /** Búsqueda temporal de sombrero (al despertarse sin el suyo; no se guarda). */
    private @Nullable BlockPos hatSearchOrigin;
    private long hatSearchUntil = -1L;
    /** Cuánto dura como máximo la búsqueda de sombrero al despertarse (45 s). */
    private static final int HAT_SEARCH_TICKS = 900;
    private long bedtime = -1L;
    private long wakeTime = -1L;
    private final NoxisSocialAnimation socialAnimation = new NoxisSocialAnimation();
    private final com.noxisculture.entity.idle.NoxisHatAnimation hatAnimation =
            new com.noxisculture.entity.idle.NoxisHatAnimation();
    private final com.noxisculture.entity.idle.NoxisBoredAnimation boredAnimation =
            new com.noxisculture.entity.idle.NoxisBoredAnimation();
    private final com.noxisculture.entity.idle.NoxisRestGesture restGesture =
            new com.noxisculture.entity.idle.NoxisRestGesture();
    /** Servidor: ticks del gesto de descanso en curso y su largo total. */
    private int restGestureTick;
    private int restGestureLength;
    /** Cliente: el globito ya hizo ¡plop! al abrir los ojos (no se repite al pararse). */
    private boolean bubblePoppedOnWake;
    // Amistad especial (servidor; se guarda con el Noxis).
    private java.util.@Nullable UUID friendId;
    /** 0 = sin decidir, 1 = sociable, 2 = independiente (nunca forma vínculos). */
    private byte friendTrait;
    private long friendLastSeen;
    private long nextAffectionTime;
    private final com.noxisculture.entity.idle.NoxisAffectionAnimation affectionAnimation =
            new com.noxisculture.entity.idle.NoxisAffectionAnimation();
    /** Personalidad (servidor; se guarda). null = todavía sin elegir. */
    private com.noxisculture.entity.personality.@Nullable NoxisPersonality personality;
    private final com.noxisculture.entity.idle.NoxisPersonaAnimation personaAnimation =
            new com.noxisculture.entity.idle.NoxisPersonaAnimation();
    /** Qué cosas le regalaron (las recuerda con cariño y las guarda siempre). Se guarda. */
    private final java.util.LinkedHashSet<net.minecraft.world.item.Item> giftMemory = new java.util.LinkedHashSet<>();
    private final com.noxisculture.entity.idle.NoxisKeepsakeAnimation keepsakeAnimation =
            new com.noxisculture.entity.idle.NoxisKeepsakeAnimation();
    // Curiosidad (servidor): quién lo está siguiendo y hasta cuándo.
    private int followerId = -1;
    private long followedUntil;
    private final com.noxisculture.entity.idle.NoxisPlayAnimation playAnimation =
            new com.noxisculture.entity.idle.NoxisPlayAnimation();
    /** Cuánto lleva quieto y tranquilo sin hacer nada (servidor; para aburrirse). */
    private int idleTicks;
    private @Nullable ItemEntity incomingGift;
    private @Nullable LivingEntity giftGiver;
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
        builder.define(VARIANT, VARIANT_PLAIN);
        builder.define(FASCINATED, false);
        builder.define(NATURE_ACTION, NoxisNatureAction.NONE);
        builder.define(HELD_FLOWER, ItemStack.EMPTY);
        builder.define(SOCIAL_ANIM, NoxisSocialAction.NONE);
        builder.define(IN_BOWL, false);
        builder.define(HAS_HAT, true);
        builder.define(HAT_ITEM, com.noxisculture.item.NoxisHatColors.newHat());
        builder.define(HAT_ANIM, com.noxisculture.entity.idle.NoxisHatAnimation.NONE);
        builder.define(BOWL_HOP, com.noxisculture.entity.idle.NoxisBowlHop.NONE);
        builder.define(BORED_ANIM, com.noxisculture.entity.idle.NoxisBoredAnimation.NONE);
        builder.define(REST_GESTURE, com.noxisculture.entity.idle.NoxisRestGesture.NONE);
        builder.define(AFFECTION_ANIM, com.noxisculture.entity.idle.NoxisAffectionAnimation.NONE);
        builder.define(PLAY_ANIM, com.noxisculture.entity.idle.NoxisPlayAnimation.NONE);
        builder.define(PERSONA_GESTURE, com.noxisculture.entity.idle.NoxisPersonaAnimation.NONE);
        builder.define(KEEPSAKE_ANIM, com.noxisculture.entity.idle.NoxisKeepsakeAnimation.NONE);
        builder.define(KEEPSAKE_ITEM, ItemStack.EMPTY);
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

    public byte getVariant() {
        return this.entityData.get(VARIANT);
    }

    public boolean hasCape() {
        return this.getVariant() >= VARIANT_CAPE;
    }

    /** Gesto de curiosidad (solo visual, cliente). */
    public NoxisCuriosity getCuriosity() {
        return this.curiosity;
    }

    /** Animación de la fascinación por los cristales (solo visual, cliente). */
    public NoxisCrystalFascination getCrystalFascination() {
        return this.crystalFascination;
    }

    // ---------------- Fascinación por los cristales (NoxisFascinatable) ----------------

    public boolean isFascinated() {
        return this.entityData.get(FASCINATED);
    }

    @Override
    public boolean canBeFascinated() {
        return !this.isInBowl() && this.getMood() == NoxisMood.NEUTRAL && !this.isTrading() && !this.isResting()
                && !this.isHoldingTorch() && !this.isHoldingUmbrella()
                && this.getNatureAction() == NoxisNatureAction.NONE && !this.flowerCarry.isHolding()
                && !this.socialLink.isBusy();
    }

    // ---------------- Naturaleza (NoxisNatureLover) ----------------

    @Override
    public boolean canEnjoyNature() {
        return !this.isInBowl() && this.getMood() == NoxisMood.NEUTRAL && !this.isTrading() && !this.isResting()
                && !this.isHoldingTorch() && !this.isHoldingUmbrella() && !this.isFascinated()
                && this.incomingGift == null && this.getNatureAction() != NoxisNatureAction.RECEIVE
                && !this.socialLink.isBusy();
    }

    // ---------------- Interacciones entre compañeros (NoxisSocial) ----------------

    @Override
    public NoxisSocialLink getSocialLink() {
        return this.socialLink;
    }

    @Override
    public boolean canSocialize() {
        return this.isSafeForSocial() && !this.isResting() && !this.socialLink.isBusy()
                && this.getNatureAction() == NoxisNatureAction.NONE && !this.flowerCarry.isHolding()
                && this.incomingGift == null && this.onGround() && !this.isInWater();
    }

    @Override
    public boolean isSafeForSocial() {
        return !this.isInBowl() && this.getMood() == NoxisMood.NEUTRAL && !this.isTrading() && !this.isHoldingTorch()
                && !this.isHoldingUmbrella() && !this.isFascinated() && this.hurtTime == 0;
    }

    @Override
    public void setSocialAnim(byte anim) {
        this.entityData.set(SOCIAL_ANIM, anim);
    }

    public byte getSocialAnim() {
        return this.entityData.get(SOCIAL_ANIM);
    }

    @Override
    public void playYawn() {
        this.playSound(ModSounds.NOXIS_YAWN, 0.6F, 0.95F + this.random.nextFloat() * 0.15F);
        this.level().broadcastEntityEvent(this, NoxisSocialAction.YAWN_EVENT);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == NoxisSocialAction.YAWN_EVENT) {
            this.socialAnimation.startYawn();
        } else {
            super.handleEntityEvent(id);
        }
    }

    // ---------------- Pecera y sombrero (NoxisBowlSleeper) ----------------

    @Override
    public boolean isInBowl() {
        return this.entityData.get(IN_BOWL);
    }

    @Override
    public void setInBowl(boolean inBowl) {
        this.entityData.set(IN_BOWL, inBowl);
    }

    @Override
    public @Nullable BlockPos getBowlPos() {
        return this.bowlPos;
    }

    @Override
    public void setBowlPos(@Nullable BlockPos pos) {
        this.bowlPos = pos == null ? null : pos.immutable();
    }

    @Override
    public boolean hasHat() {
        return this.entityData.get(HAS_HAT);
    }

    @Override
    public void setHasHat(boolean hat) {
        // Sin decir cuál: si se lo pone, uno con los colores originales (solo si no tenía ya uno).
        if (hat && this.getHatItem().isEmpty()) {
            this.setHatItem(com.noxisculture.item.NoxisHatColors.newHat());
        } else if (!hat) {
            this.setHatItem(ItemStack.EMPTY);
        }
    }

    @Override
    public ItemStack getHatItem() {
        return this.entityData.get(HAT_ITEM);
    }

    @Override
    public void setHatItem(ItemStack hat) {
        this.entityData.set(HAT_ITEM, hat.isEmpty() ? ItemStack.EMPTY : hat.copyWithCount(1));
        this.entityData.set(HAS_HAT, !hat.isEmpty());
    }

    @Override
    public void setBowlHop(byte anim) {
        this.entityData.set(BOWL_HOP, anim);
        if (anim != com.noxisculture.entity.idle.NoxisBowlHop.NONE) this.entityData.set(TORCH, false);
    }

    /** ¿Está en el aire, saltando para entrar o salir de la pecera? */
    private boolean isBowlHopping() {
        byte a = this.entityData.get(BOWL_HOP);
        return a == com.noxisculture.entity.idle.NoxisBowlHop.IN || a == com.noxisculture.entity.idle.NoxisBowlHop.OUT;
    }

    /** Saltito de la pecera (solo visual, cliente). */
    public com.noxisculture.entity.idle.NoxisBowlHop getBowlHopAnimation() {
        return this.bowlHopAnimation;
    }

    @Override
    public void startHatSearch(BlockPos origin) {
        this.hatSearchOrigin = origin.immutable();
        this.hatSearchUntil = this.level().getGameTime() + HAT_SEARCH_TICKS;
    }

    @Override
    public @Nullable BlockPos getHatSearchOrigin() {
        if (this.hatSearchOrigin != null && (this.hasHat() || this.level().getGameTime() > this.hatSearchUntil)) {
            this.stopHatSearch();                      // ya lo tiene, o se acabó el tiempo
        }
        return this.hatSearchOrigin;
    }

    @Override
    public void stopHatSearch() {
        this.hatSearchOrigin = null;
        this.hatSearchUntil = -1L;
    }

    /** Durante el saltito de la pecera lo mueve el objetivo de dormir: acá no se mueve solo. */
    @Override
    public void travel(Vec3 input) {
        if (this.isBowlHopping()) {
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }
        super.travel(input);
    }

    /**
     * Tamaño real del Noxis (la caja con la que choca y donde se lo golpea):
     * con sombrero, la altura completa; sin sombrero, solo hasta las orejitas;
     * durmiendo en la pecera, acurrucado y bien adentro del vidrio.
     */
    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions base = super.getDefaultDimensions(pose);
        if (this.isInBowl()) {
            // Acurrucado y bien adentro: la caja termina por debajo del borde del bloque (15,6 px),
            // así se puede colocar otra pecera (u otro bloque) encima aunque esté durmiendo.
            return base.scale(1.07F, 0.5F).withEyeHeight(base.eyeHeight() * 0.75F);
        }
        if (!this.hasHat()) {
            return base.scale(1.0F, 0.68F).withEyeHeight(base.eyeHeight());
        }
        return base;
    }

    /** Durmiendo en la pecera (o saltando para entrar/salir) nadie lo empuja. */
    @Override
    public boolean isPushable() {
        return !this.isInBowl() && !this.isBowlHopping() && super.isPushable();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (IN_BOWL.equals(key) || HAS_HAT.equals(key)) {
            this.refreshDimensions();
        }
    }

    @Override
    public void setHatAnim(byte anim) {
        this.entityData.set(HAT_ANIM, anim);
        // Necesita los dos bracitos: guarda la antorcha enseguida (vuelve sola cuando termina).
        if (anim != com.noxisculture.entity.idle.NoxisHatAnimation.NONE) this.entityData.set(TORCH, false);
    }

    /** Animación de sacarse/ponerse el sombrero (solo visual, cliente). */
    public com.noxisculture.entity.idle.NoxisHatAnimation getHatAnimation() {
        return this.hatAnimation;
    }

    @Override
    public @Nullable BlockPos getHatPos() {
        return this.hatPos;
    }

    @Override
    public void setHatPos(@Nullable BlockPos pos) {
        this.hatPos = pos == null ? null : pos.immutable();
    }

    @Override
    public boolean isBedtime() {
        return this.bedtime >= 0L && this.level().getGameTime() >= this.bedtime;
    }

    @Override
    public boolean isWakeTime() {
        return this.wakeTime >= 0L && this.level().getGameTime() >= this.wakeTime;
    }

    @Override
    public boolean canGoToBowl() {
        return this.getMood() != NoxisMood.SCARED && !this.isTrading() && !this.isFascinated()
                && this.getNatureAction() == NoxisNatureAction.NONE && !this.flowerCarry.isHolding()
                && this.incomingGift == null && !this.socialLink.isBusy() && !this.isBaby();
    }

    // ---------------- Aburrimiento (NoxisBored) ----------------

    @Override
    public boolean canBeBored() {
        return !this.isInBowl() && !this.isBowlHopping() && this.getMood() == NoxisMood.NEUTRAL
                && !this.isTrading() && !this.isResting() && !this.isFascinated()
                && !this.isHoldingTorch() && !this.isHoldingUmbrella()
                && this.getNatureAction() == NoxisNatureAction.NONE && !this.flowerCarry.isHolding()
                && this.getHeldFlower().isEmpty() && this.incomingGift == null
                && !this.socialLink.isBusy() && this.getSocialAnim() == NoxisSocialAction.NONE
                && this.entityData.get(HAT_ANIM) == com.noxisculture.entity.idle.NoxisHatAnimation.NONE
                && this.entityData.get(BOWL_HOP) == com.noxisculture.entity.idle.NoxisBowlHop.NONE
                && this.entityData.get(REST_GESTURE) == com.noxisculture.entity.idle.NoxisRestGesture.NONE
                && this.entityData.get(PLAY_ANIM) == com.noxisculture.entity.idle.NoxisPlayAnimation.NONE
                && this.entityData.get(PERSONA_GESTURE) == com.noxisculture.entity.idle.NoxisPersonaAnimation.NONE
                && this.entityData.get(KEEPSAKE_ANIM) == com.noxisculture.entity.idle.NoxisKeepsakeAnimation.NONE
                && this.hatSearchOrigin == null && this.hurtTime == 0
                && this.onGround() && !this.isInWater() && !this.isPassenger();
    }

    @Override
    public int getIdleTicks() {
        return this.idleTicks;
    }

    @Override
    public void resetIdleTicks() {
        this.idleTicks = 0;
    }

    @Override
    public byte getBoredAnim() {
        return this.entityData.get(BORED_ANIM);
    }

    @Override
    public void setBoredAnim(byte anim) {
        this.entityData.set(BORED_ANIM, anim);
    }

    /** Gestos de aburrimiento (solo visual, cliente). */
    public com.noxisculture.entity.idle.NoxisBoredAnimation getBoredAnimation() {
        return this.boredAnimation;
    }

    /** Animación de saludos y descansos en compañía (solo visual, cliente). */
    public NoxisSocialAnimation getSocialAnimation() {
        return this.socialAnimation;
    }

    // ---- Recibir flores de regalo ----

    @Override
    public boolean canReceiveGift() {
        return this.canEnjoyNature() && !this.flowerCarry.isHolding()
                && this.getNatureAction() == NoxisNatureAction.NONE && !this.isBaby();
    }

    @Override
    public void expectGift(ItemEntity gift, LivingEntity giver) {
        if (this.incomingGift != null && this.incomingGift != gift) {
            com.noxisculture.entity.nature.NoxisItemClaims.release(this.incomingGift, this);
        }
        this.incomingGift = gift;
        this.giftGiver = giver;
        // La flor queda reservada para él: ningún otro Noxis la levanta.
        com.noxisculture.entity.nature.NoxisItemClaims.claimGift(gift, this);
    }

    @Override
    public @Nullable ItemEntity getIncomingGift() {
        return this.incomingGift;
    }

    @Override
    public @Nullable LivingEntity getGiftGiver() {
        return this.giftGiver;
    }

    @Override
    public void clearIncomingGift() {
        // La reserva se termina: si la flor sigue en el piso, queda libre para cualquiera.
        if (this.incomingGift != null) com.noxisculture.entity.nature.NoxisItemClaims.release(this.incomingGift, this);
        this.incomingGift = null;
        this.giftGiver = null;
    }

    @Override
    public void showGiftInHand(ItemStack stack) {
        if (stack.isEmpty() || this.flowerCarry.isHolding()) {
            this.syncHeldFlower();                       // vuelve a mostrar lo de siempre (nada)
        } else {
            this.entityData.set(HELD_FLOWER, stack.copy());
        }
    }

    @Override
    public boolean hasRoomFor(ItemStack stack) {
        return this.getInventory().canAddItem(stack);
    }

    @Override
    public ItemStack storeCollected(ItemStack stack) {
        return this.getInventory().addItem(stack);
    }

    @Override
    public boolean canCollectFlowers() {
        return com.noxisculture.entity.ai.NoxisNatureLover.super.canCollectFlowers()
                && this.getNatureAction() == NoxisNatureAction.NONE && this.getHeldFlower().isEmpty()
                && this.getBoredAnim() == com.noxisculture.entity.idle.NoxisBoredAnimation.NONE
                && !this.isStretching() && !this.isInBowl()
                && this.getPlayAnim() == com.noxisculture.entity.idle.NoxisPlayAnimation.NONE
                && this.getPersonaGesture() == com.noxisculture.entity.idle.NoxisPersonaAnimation.NONE
                && this.getKeepsakeAnim() == com.noxisculture.entity.idle.NoxisKeepsakeAnimation.NONE;
    }

    @Override
    public void storeGift(ItemStack stack) {
        if (!stack.isEmpty()) this.rememberGift(stack.getItem());   // un regalo: lo recuerda con cariño
        // Reutiliza el inventario que ya trae AbstractVillager (Minecraft lo guarda con el Noxis).
        ItemStack rest = this.getInventory().addItem(stack);
        if (!rest.isEmpty()) {
            Block.popResource(this.level(), this.blockPosition(), rest);   // inventario lleno: queda en el piso
        }
    }

    @Override
    public void setNatureAction(byte action) {
        this.entityData.set(NATURE_ACTION, action);
    }

    public byte getNatureAction() {
        return this.entityData.get(NATURE_ACTION);
    }

    @Override
    public NoxisFlowerCarry getFlowerCarry() {
        return this.flowerCarry;
    }

    @Override
    public void syncHeldFlower() {
        this.entityData.set(HELD_FLOWER, this.flowerCarry.getStack().copy());
    }

    /** La flor que se ve en su mano (sincronizada; en el cliente es solo para dibujarla). */
    public ItemStack getHeldFlower() {
        return this.entityData.get(HELD_FLOWER);
    }

    /** Animación de las interacciones con la naturaleza (solo visual, cliente). */
    public NoxisNatureAnimation getNatureAnimation() {
        return this.natureAnimation;
    }

    @Override
    public void setFascinated(boolean fascinated) {
        this.entityData.set(FASCINATED, fascinated);
    }

    /** Física visual de la capa (solo se usa en el cliente). */
    public NoxisCapePhysics getCapePhysics() {
        return this.capePhysics;
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
        return !this.isInBowl() && this.getMood() == NoxisMood.NEUTRAL && !this.isTrading() && !this.isHoldingTorch()
                && !this.isHoldingUmbrella();
    }

    @Override
    public void setResting(boolean resting) {
        this.entityData.set(RESTING, resting);
        if (!resting && this.socialLink.isBusy() && this.socialLink.getKind() == NoxisSocialLink.Kind.REST) {
            // Se despierta: libera a su compañero de siesta (que sigue durmiendo, si quiere).
            net.minecraft.world.entity.PathfinderMob p = this.socialLink.getPartner();
            if (p instanceof NoxisSocial s && s.getSocialLink().isLinkedWith(this)) {
                s.getSocialLink().clear();
                s.setSocialAnim(NoxisSocialAction.NONE);
            }
            this.socialLink.clear();
            this.setSocialAnim(NoxisSocialAction.NONE);
        }
    }

    // ---------------- Acomodarse y estirarse ----------------

    @Override
    public void startSettle() {
        // Con un dato al azar (para qué lado se balancea), así no se acomodan todos iguales.
        this.setRestGesture(com.noxisculture.entity.idle.NoxisRestGesture.encode(
                com.noxisculture.entity.idle.NoxisRestGesture.SETTLE, false, this.random.nextInt(8)));
        if (this.isInBowl()) {
            this.restGestureTick = com.noxisculture.entity.idle.NoxisRestGesture.SETTLE_BOWL_START;
        }
    }

    @Override
    public void startWakeStretch(boolean seated) {
        byte type = seated ? com.noxisculture.entity.idle.NoxisRestGesture.STRETCH_SEATED
                : com.noxisculture.entity.idle.NoxisRestGesture.STRETCH_STAND;
        // Desfase propio: cada uno abre los ojos a su ritmo (los compañeros nunca van sincronizados).
        this.setRestGesture(com.noxisculture.entity.idle.NoxisRestGesture.encode(type, this.hasHat(), this.random.nextInt(8)));
    }

    @Override
    public boolean isStretching() {
        byte type = com.noxisculture.entity.idle.NoxisRestGesture.typeOf(this.entityData.get(REST_GESTURE));
        return type == com.noxisculture.entity.idle.NoxisRestGesture.STRETCH_SEATED
                || type == com.noxisculture.entity.idle.NoxisRestGesture.STRETCH_STAND;
    }

    private void setRestGesture(byte gesture) {
        this.entityData.set(REST_GESTURE, gesture);
        this.restGestureTick = 0;
        this.restGestureLength = com.noxisculture.entity.idle.NoxisRestGesture.length(gesture);
    }

    /** Servidor: avanza el gesto de descanso y lo corta si pasa algo más importante. */
    private void tickRestGesture() {
        byte gesture = this.entityData.get(REST_GESTURE);
        if (gesture == com.noxisculture.entity.idle.NoxisRestGesture.NONE) return;
        byte type = com.noxisculture.entity.idle.NoxisRestGesture.typeOf(gesture);
        boolean keep;
        if (type == com.noxisculture.entity.idle.NoxisRestGesture.SETTLE) {
            keep = this.isResting();                   // si se levanta antes, deja de acomodarse
        } else {
            keep = !this.isResting() && this.getMood() == NoxisMood.NEUTRAL && !this.isTrading()
                    && this.hurtTime == 0 && !this.isHoldingTorch() && !this.isHoldingUmbrella()
                    && !this.isInBowl() && !this.flowerCarry.isHolding()
                    && this.getNatureAction() == NoxisNatureAction.NONE && this.incomingGift == null
                    && this.entityData.get(HAT_ANIM) == com.noxisculture.entity.idle.NoxisHatAnimation.NONE;
        }
        if (!keep || ++this.restGestureTick >= this.restGestureLength) {
            this.entityData.set(REST_GESTURE, com.noxisculture.entity.idle.NoxisRestGesture.NONE);
            return;
        }
        if (this.restGestureTick == com.noxisculture.entity.idle.NoxisRestGesture.yawnTick(gesture)) {
            this.playSound(ModSounds.NOXIS_YAWN, 0.55F, 1.08F + this.random.nextFloat() * 0.1F);
        }
    }

    /** Acomodarse / estirarse (solo visual, cliente). */
    public com.noxisculture.entity.idle.NoxisRestGesture getRestGesture() {
        return this.restGesture;
    }

    // ---------------- Amistad especial (NoxisFriend) ----------------

    @Override
    public java.util.@Nullable UUID getFriendId() {
        return this.friendId;
    }

    @Override
    public void setFriendId(java.util.@Nullable UUID friend) {
        this.friendId = friend;
        this.friendLastSeen = this.level().getGameTime();
    }

    @Override
    public boolean isIndependent() {
        if (this.friendTrait == 0) {
            this.friendTrait = this.random.nextFloat() < com.noxisculture.entity.social.NoxisFriendship.INDEPENDENT_CHANCE
                    ? (byte) 2 : (byte) 1;
        }
        return this.friendTrait == 2;
    }

    @Override
    public void setAffectionAnim(byte anim) {
        this.entityData.set(AFFECTION_ANIM, anim);
    }

    @Override
    public long getNextAffectionTime() {
        return this.nextAffectionTime;
    }

    @Override
    public void setNextAffectionTime(long time) {
        this.nextAffectionTime = time;
    }

    @Override
    public boolean canShowAffection() {
        return this.canSocialize() && !this.isStretching()
                && this.getBoredAnim() == com.noxisculture.entity.idle.NoxisBoredAnimation.NONE
                && this.entityData.get(REST_GESTURE) == com.noxisculture.entity.idle.NoxisRestGesture.NONE
                && this.entityData.get(PLAY_ANIM) == com.noxisculture.entity.idle.NoxisPlayAnimation.NONE
                && this.entityData.get(PERSONA_GESTURE) == com.noxisculture.entity.idle.NoxisPersonaAnimation.NONE
                && this.entityData.get(KEEPSAKE_ANIM) == com.noxisculture.entity.idle.NoxisKeepsakeAnimation.NONE
                && !this.isBaby();
    }

    // ---------------- Curiosidad y travesuras (NoxisCurious) ----------------

    // ---------------- Personalidad (NoxisPersonable) ----------------

    /** Se elige una sola vez (al aparecer, o al cargar un Noxis de antes) y se guarda. */
    @Override
    public com.noxisculture.entity.personality.NoxisPersonality getPersonality() {
        if (this.personality == null) {
            this.personality = com.noxisculture.entity.personality.NoxisPersonality.random(this.random);
        }
        return this.personality;
    }

    @Override
    public boolean canDoPersonaGesture() {
        return this.isFreeForPlay() && this.getPlayAnim() == com.noxisculture.entity.idle.NoxisPlayAnimation.NONE
                && this.noKeepsake() && !this.isBaby();
    }

    @Override
    public byte getPersonaGesture() {
        return this.entityData.get(PERSONA_GESTURE);
    }

    @Override
    public void setPersonaGesture(byte gesture) {
        this.entityData.set(PERSONA_GESTURE, gesture);
    }

    // ---------------- Inventario privado y objetos favoritos (NoxisKeeper) ----------------

    @Override
    public net.minecraft.world.SimpleContainer getPrivateInventory() {
        return this.getInventory();
    }

    @Override
    public boolean isGiftedItem(net.minecraft.world.item.Item item) {
        return this.giftMemory.contains(item);
    }

    private void rememberGift(net.minecraft.world.item.Item item) {
        this.giftMemory.remove(item);
        this.giftMemory.add(item);                           // el más reciente queda al final
        while (this.giftMemory.size() > GIFT_MEMORY_SIZE) {
            this.giftMemory.remove(this.giftMemory.iterator().next());
        }
    }

    @Override
    public boolean appreciates(ItemStack stack) {
        return com.noxisculture.entity.nature.NoxisTreasures.isTreasure(stack, this.giftMemory);
    }

    /** Sin el gesto de objetos en curso (para las demás actividades). */
    private boolean noKeepsake() {
        return this.entityData.get(KEEPSAKE_ANIM) == com.noxisculture.entity.idle.NoxisKeepsakeAnimation.NONE;
    }

    @Override
    public boolean canHandleKeepsakes() {
        return this.isFreeForPlay() && this.noPersonaGesture()
                && this.getPlayAnim() == com.noxisculture.entity.idle.NoxisPlayAnimation.NONE;
    }

    @Override
    public boolean canHandleKeepsakesWhileFascinated() {
        return !this.isInBowl() && !this.isTrading() && !this.isResting() && this.getMood() != NoxisMood.SCARED
                && this.hurtTime == 0 && !this.socialLink.isBusy() && !this.isHoldingTorch() && !this.isHoldingUmbrella()
                && this.getNatureAction() == NoxisNatureAction.NONE && this.incomingGift == null;
    }

    @Override
    public void setKeepsakeAnim(byte anim) {
        this.entityData.set(KEEPSAKE_ANIM, anim);
    }

    @Override
    public byte getKeepsakeAnim() {
        return this.entityData.get(KEEPSAKE_ANIM);
    }

    @Override
    public void showKeepsake(ItemStack stack) {
        this.entityData.set(KEEPSAKE_ITEM, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
    }

    /** Lo que se ve en sus manitos al contemplar un tesoro (solo para dibujarlo). */
    public ItemStack getKeepsakeItem() {
        return this.entityData.get(KEEPSAKE_ITEM);
    }

    /** Gesto de objetos (solo visual, cliente). */
    public com.noxisculture.entity.idle.NoxisKeepsakeAnimation getKeepsakeAnimation() {
        return this.keepsakeAnimation;
    }

    /** Si muere, sus tesoros quedan en el piso (no se pierden). */
    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel && !this.isRemoved()) {
            net.minecraft.world.SimpleContainer inv = this.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.removeItemNoUpdate(i);
                if (!s.isEmpty()) this.spawnAtLocation((ServerLevel) this.level(), s);
            }
        }
        super.die(source);
    }

    /** Gesto de la personalidad (solo visual, cliente). */
    public com.noxisculture.entity.idle.NoxisPersonaAnimation getPersonaAnimation() {
        return this.personaAnimation;
    }

    /** Sin ninguna otra actividad ni gesto en curso (para seguir a alguien o que le hagan una broma). */
    private boolean isFreeForPlay() {
        return this.canSocialize() && !this.isStretching() && !this.isInBowl()
                && this.getBoredAnim() == com.noxisculture.entity.idle.NoxisBoredAnimation.NONE
                && this.entityData.get(REST_GESTURE) == com.noxisculture.entity.idle.NoxisRestGesture.NONE
                && this.entityData.get(AFFECTION_ANIM) == com.noxisculture.entity.idle.NoxisAffectionAnimation.NONE
                && this.hatSearchOrigin == null;
    }

    /** Libre y además sin su gesto de personalidad en curso. */
    private boolean noPersonaGesture() {
        return this.entityData.get(PERSONA_GESTURE) == com.noxisculture.entity.idle.NoxisPersonaAnimation.NONE;
    }

    @Override
    public boolean canFollowCuriously() {
        return this.isFreeForPlay() && this.noPersonaGesture() && this.noKeepsake();
    }

    @Override
    public boolean isDoingSomethingInteresting() {
        if (this.isResting() || this.isInBowl() || this.isTrading() || this.getMood() == NoxisMood.SCARED) return false;
        boolean moving = !this.getNavigation().isDone() || this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D;
        return moving || this.getNatureAction() != NoxisNatureAction.NONE || this.isFascinated()
                || this.flowerCarry.isHolding()
                || this.getBoredAnim() != com.noxisculture.entity.idle.NoxisBoredAnimation.NONE;
    }

    @Override
    public boolean canBePranked() {
        return this.isFreeForPlay() && this.noPersonaGesture() && this.noKeepsake()
                && this.getPlayAnim() == com.noxisculture.entity.idle.NoxisPlayAnimation.NONE
                && this.getNavigation().isDone() && this.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4D;
    }

    @Override
    public byte getPlayAnim() {
        return this.entityData.get(PLAY_ANIM);
    }

    @Override
    public void setPlayAnim(byte anim) {
        this.entityData.set(PLAY_ANIM, anim);
    }

    @Override
    public void markFollowedBy(int follower, long until) {
        this.followerId = follower;
        this.followedUntil = until;
    }

    @Override
    public boolean isFollowedByOther(int follower, long now) {
        return this.followerId != follower && now < this.followedUntil;
    }

    @Override
    public void startle(int ticks) {
        this.moodController.makeScared(ticks);       // el miedo de siempre, apenas un momento
    }

    @Override
    public void cheerUp(int ticks) {
        this.moodController.makeHappy(ticks);
    }

    /** Curiosidad y travesuras (solo visual, cliente). */
    public com.noxisculture.entity.idle.NoxisPlayAnimation getPlayAnimation() {
        return this.playAnimation;
    }

    /** Gesto de cariño (solo visual, cliente). */
    public com.noxisculture.entity.idle.NoxisAffectionAnimation getAffectionAnimation() {
        return this.affectionAnimation;
    }

    /** Servidor: nace (a veces) un vínculo, y se olvida el de un amigo que ya no existe. */
    private void tickFriendship() {
        if ((this.tickCount + this.getId()) % com.noxisculture.entity.social.NoxisFriendship.BOND_INTERVAL != 0) return;
        if (this.friendId == null) {
            com.noxisculture.entity.social.NoxisFriendship.tryBond(this);
            return;
        }
        long now = this.level().getGameTime();
        if (this.friendLastSeen <= 0L) this.friendLastSeen = now;      // recién cargado: le da tiempo
        if (com.noxisculture.entity.social.NoxisFriendship.findFriend(this) != null) {
            this.friendLastSeen = now;
        } else if (this.level() instanceof ServerLevel server) {
            Entity e = server.getEntity(this.friendId);
            boolean notMutual = e instanceof com.noxisculture.entity.ai.NoxisFriend f && !this.getUUID().equals(f.getFriendId());
            if (notMutual || (e != null && !e.isAlive())
                    || now - this.friendLastSeen > com.noxisculture.entity.social.NoxisFriendship.FORGET_TICKS) {
                this.friendId = null;          // ya no existe (o ya no es su amigo): queda libre
            }
        }
    }

    /** Hasta cuándo puede dormirse solo (después de buscar compañía sin encontrar). */
    private long restAloneUntil;

    @Override
    public boolean mayRestAlone() {
        return this.level().getGameTime() < this.restAloneUntil;
    }

    @Override
    public void allowRestAlone(int ticks) {
        this.restAloneUntil = this.level().getGameTime() + ticks;
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
        // De noche: a dormir a la pecera. Misma prioridad que huir/asustarse: un monstruo cerca no la
        // saca de la pecera (solo la despierta un golpe), pero si ya estaba huyendo no se acuesta.
        this.goalSelector.addGoal(1, new NoxisBowlSleepGoal<>(this));
        // Al despertarse se queda quieto mientras se estira (lo cortan comercio, miedo y sueño).
        this.goalSelector.addGoal(2, new com.noxisculture.entity.ai.NoxisStretchGoal<>(this));
        // Descanso en compañía: va antes que el individual (a veces, si hay compañero, descansan juntos).
        this.goalSelector.addGoal(3, new NoxisCompanionRestGoal<>(this));
        this.goalSelector.addGoal(3, new NoxisSocialFollowGoal<>(this));
        this.goalSelector.addGoal(3, new NoxisRestGoal<>(this));
        this.goalSelector.addGoal(3, new NoxisReceiveGiftGoal<>(this));
        // Sin sombrero: busca uno cerca (el suyo u otro, bloque o ítem) y se lo pone.
        this.goalSelector.addGoal(3, new com.noxisculture.entity.ai.NoxisFindHatGoal<>(this));
        this.goalSelector.addGoal(4, new NoxisWorkAtTableGoal<>(this, 0.45D));
        this.goalSelector.addGoal(4, new NoxisCrystalFascinationGoal<>(this));
        this.goalSelector.addGoal(4, new NoxisNatureGoal<>(this));
        // De vez en cuando, levanta una flor suelta del piso (nunca un regalo reservado para otro).
        this.goalSelector.addGoal(4, new com.noxisculture.entity.ai.NoxisCollectFlowerGoal<>(this));
        // Objetos: mirar lo que le llama la atención, contemplar sus tesoros y ordenar lo que no le interesa.
        this.goalSelector.addGoal(4, new com.noxisculture.entity.ai.NoxisTidyInventoryGoal<>(this));
        this.goalSelector.addGoal(4, new com.noxisculture.entity.ai.NoxisInspectItemGoal<>(this));
        this.goalSelector.addGoal(4, new com.noxisculture.entity.ai.NoxisKeepsakeGoal<>(this));
        // Seguir a un compañero por curiosidad (y, rarísima vez, una travesura).
        this.goalSelector.addGoal(4, new com.noxisculture.entity.ai.NoxisCuriousFollowGoal<>(this));
        this.goalSelector.addGoal(4, new NoxisGreetGoal<>(this));
        // Amigo especial: gesto de cariño compartido (el amigo lo acompaña con NoxisSocialFollowGoal).
        this.goalSelector.addGoal(4, new com.noxisculture.entity.ai.NoxisAffectionGoal<>(this));
        // Aburrimiento: solo después de un rato quieto sin hacer nada (cualquier objetivo de
        // prioridad 1-3 lo corta: comercio, miedo, sueño, descanso, regalos, buscar el sombrero).
        this.goalSelector.addGoal(4, new com.noxisculture.entity.ai.NoxisBoredGoal<>(this));
        // Gesto propio de su personalidad, con su vocalización (de vez en cuando, estando tranquilo).
        this.goalSelector.addGoal(5, new com.noxisculture.entity.ai.NoxisPersonaGestureGoal<>(this));
        // A veces, en vez de pasear al azar, va hacia donde está su amigo (sin pegarse).
        this.goalSelector.addGoal(5, new com.noxisculture.entity.ai.NoxisSeekFriendGoal<>(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.35D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    // ---------------- Comercio ----------------

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.isInBowl()) {
            return InteractionResult.PASS;                  // durmiendo en su pecera: no comercia
        }
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
        // Enlace social suelto (el compañero desapareció o ya no está enlazado): se libera.
        if (this.tickCount % 20 == 0 && this.socialLink.isBusy()) {
            net.minecraft.world.entity.PathfinderMob p = this.socialLink.getPartner();
            if (p == null || !p.isAlive() || p.isRemoved()
                    || !(p instanceof NoxisSocial s && s.getSocialLink().isLinkedWith(this))) {
                this.socialLink.clear();
                this.setSocialAnim(NoxisSocialAction.NONE);
            }
        }
        // Regalo pendiente que ya no es suyo (se lo llevaron, venció la reserva): lo suelta.
        if (this.incomingGift != null && this.tickCount % 20 == 0 && (this.incomingGift.isRemoved()
                || !com.noxisculture.entity.nature.NoxisItemClaims.isClaimedBy(this.incomingGift, this))) {
            this.clearIncomingGift();
        }
        // Si se guardó el mundo con una flor en la mano, la devuelve a su lugar (o la suelta).
        if (this.flowerCarry.isOrphan() && this.tickCount > 20) {
            this.flowerCarry.returnOrDrop(this.level(), this);
            this.syncHeldFlower();
            this.setNatureAction(NoxisNatureAction.NONE);
        }
        this.tickRestGesture();
        this.tickFriendship();
        // Rato sin hacer nada (para aburrirse): quieto, sin camino y tranquilo.
        boolean still = this.getNavigation().isDone() && this.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4D;
        if (still && this.canBeBored()) {
            this.idleTicks++;
        } else {
            this.idleTicks = 0;
        }
        // Variante de ropa: se elige una sola vez, al aparecer.
        if (!this.variantChosen) {
            this.variantChosen = true;
            this.entityData.set(VARIANT, this.random.nextFloat() < CAPE_CHANCE
                    ? (byte) (VARIANT_CAPE + this.random.nextInt(CAPE_STYLES)) : VARIANT_PLAIN);
        }
        NoxisMood mood = this.moodController.tick(this);
        if (mood != this.getMood()) {
            this.entityData.set(MOOD, mood.id());
        }
        // De noche (o en cuevas oscuras) sacan la antorcha. Se revisa cada segundo.
        if (this.tickCount % 20 == 0) {
            boolean dark = this.level().isDarkOutside()
                    || this.level().getBrightness(LightLayer.SKY, this.blockPosition()) < CAVE_SKY_LIGHT;
            // Sin antorcha dentro de la pecera ni mientras se saca o se pone el sombrero (usa los bracitos).
            boolean torch = dark && !this.isInBowl()
                    && this.entityData.get(HAT_ANIM) == com.noxisculture.entity.idle.NoxisHatAnimation.NONE
                    && this.entityData.get(BOWL_HOP) == com.noxisculture.entity.idle.NoxisBowlHop.NONE;
            if (torch != this.isHoldingTorch()) {
                this.entityData.set(TORCH, torch);
            }
            // Horario de sueño propio: a cada uno le da sueño a su hora y se despierta a su ritmo.
            long now = this.level().getGameTime();
            if (this.level().isDarkOutside()) {
                if (this.bedtime < 0L) this.bedtime = now + this.random.nextInt(BEDTIME_RANDOM);
                this.wakeTime = -1L;
            } else {
                this.bedtime = -1L;
                if (this.wakeTime < 0L) this.wakeTime = now + this.random.nextInt(WAKE_RANDOM);
            }
            // Si llueve (o nieva) y tiene el cielo encima, saca el paraguas con la otra mano.
            boolean wet = this.level().isRaining() && this.level().canSeeSky(this.blockPosition().above())
                    && !this.isInBowl();
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
        if (this.hasCape()) {
            this.capePhysics.tick(this);
        }
        // Curiosidad: solo quieto y tranquilo, sin ninguna otra animación en curso.
        double dx = this.getX() - this.xo;
        double dz = this.getZ() - this.zo;
        boolean calm = this.getMood() == NoxisMood.NEUTRAL
                && this.happyAnim < 0.02F && this.scaredAnim < 0.02F
                && !this.isTrading() && !this.isResting() && this.restAnim < 0.01F
                && !this.isHoldingTorch() && this.torchAnim < 0.01F
                && !this.isHoldingUmbrella() && this.umbrellaAnim < 0.01F
                && this.hatAnimation.getAnim() == com.noxisculture.entity.idle.NoxisHatAnimation.NONE
                && this.bowlHopAnimation.getAnim() == com.noxisculture.entity.idle.NoxisBowlHop.NONE
                && !this.boredAnimation.isBusy() && !this.restGesture.isBusy() && !this.affectionAnimation.isBusy()
                && !this.playAnimation.isBusy() && !this.personaAnimation.isBusy()
                && !this.keepsakeAnimation.isBusy()
                && dx * dx + dz * dz < 1.0E-4D;
        // Los dos gestos tranquilos nunca se pisan: el que empezó primero termina antes que el otro.
        boolean fascinated = this.isFascinated() || this.crystalFascination.isActive()
                || this.crystalFascination.getAmount(1.0F) > 0.01F;
        // Naturaleza: la acción llega del servidor; acá solo se anima.
        this.natureAnimation.tick(this.getNatureAction(), !this.getHeldFlower().isEmpty(), this.random);
        boolean withNature = this.natureAnimation.isBusy();
        // Saludos y descanso en compañía: la señal llega del servidor; acá solo se anima.
        this.socialAnimation.tick(this.getSocialAnim(), this.random);
        this.hatAnimation.tick(this.entityData.get(HAT_ANIM));
        this.bowlHopAnimation.tick(this.entityData.get(BOWL_HOP));
        this.boredAnimation.tick(this.entityData.get(BORED_ANIM));
        this.affectionAnimation.tick(this.entityData.get(AFFECTION_ANIM));
        this.playAnimation.tick(this.entityData.get(PLAY_ANIM));
        this.personaAnimation.tick(this.entityData.get(PERSONA_GESTURE));
        this.keepsakeAnimation.tick(this.entityData.get(KEEPSAKE_ANIM));
        byte restG = this.entityData.get(REST_GESTURE);
        // En la pecera, acomodarse arranca directo del balanceo (el saltito ya lo hizo).
        this.restGesture.tick(restG, this.isInBowl()
                && com.noxisculture.entity.idle.NoxisRestGesture.typeOf(restG) == com.noxisculture.entity.idle.NoxisRestGesture.SETTLE
                ? com.noxisculture.entity.idle.NoxisRestGesture.SETTLE_BOWL_START : 0);
        boolean withSocial = this.socialAnimation.isBusy();
        this.curiosity.tick(calm && !fascinated && !withNature && !withSocial, this.random);
        // Fascinación: la señal llega del servidor; acá solo se corta si se ve algo prioritario.
        boolean free = this.getMood() == NoxisMood.NEUTRAL && this.scaredAnim < 0.02F
                && !this.isTrading() && !this.isResting() && !this.isHoldingTorch() && !this.isHoldingUmbrella();
        this.crystalFascination.tick(this.isFascinated(), free, this.random);
        // Chispitas doradas delante de la carita mientras le brillan los ojos (se ven de lejos).
        if (this.crystalFascination.isActive() && this.crystalFascination.getGlow(1.0F) > 0.5F
                && this.random.nextInt(5) == 0) {
            double yaw = this.yHeadRot * Mth.DEG_TO_RAD;
            double fx = -Math.sin(yaw) * 0.4D;
            double fz = Math.cos(yaw) * 0.4D;
            this.level().addParticle(ParticleTypes.WAX_ON,
                    this.getX() + fx + (this.random.nextDouble() - 0.5D) * 0.7D,
                    this.getY() + 0.55D + this.random.nextDouble() * 0.6D,
                    this.getZ() + fz + (this.random.nextDouble() - 0.5D) * 0.7D,
                    0.0D, 0.03D, 0.0D);
        }
        NoxisMood mood = this.getMood();
        this.happyAnim = approach(this.happyAnim, mood == NoxisMood.HAPPY ? 1.0F : 0.0F);
        this.scaredAnim = approach(this.scaredAnim, mood == NoxisMood.SCARED ? 1.0F : 0.0F);
        this.torchAnim = approach(this.torchAnim, this.isHoldingTorch() ? 1.0F : 0.0F);
        this.umbrellaAnim = approach(this.umbrellaAnim, this.isHoldingUmbrella() ? 1.0F : 0.0F);
        // Sentarse es más lento y pesado que el resto de las transiciones.
        float restTarget = this.isResting() ? 1.0F : 0.0F;
        // Acomodándose: primero se prepara de pie. Estirándose: sigue sentado hasta levantarse.
        if (this.restGesture.holdsStanding()) restTarget = 0.0F;
        if (this.restGesture.holdsSeated()) restTarget = 1.0F;
        if (this.isResting()) this.bubblePoppedOnWake = false;
        float restBefore = this.restAnim;
        this.restAnim += Mth.clamp(restTarget - this.restAnim, -0.06F, 0.06F);
        // Al despertarse, el globito de sueño hace ¡plop! (mismo momento en que desaparece del modelo):
        // al abrir los ojos si se estira sentado, o al levantarse.
        boolean popNow = this.restGesture.bubblePopsNow();
        if (popNow) this.bubblePoppedOnWake = true;
        if (popNow || (!this.isResting() && restBefore >= 0.72F && this.restAnim < 0.72F && !this.bubblePoppedOnWake)) {
            double yaw = this.yHeadRot * Mth.DEG_TO_RAD;
            this.level().addParticle(ParticleTypes.BUBBLE_POP,
                    this.getX() - Math.sin(yaw) * 0.45D, this.getY() + 0.55D, this.getZ() + Math.cos(yaw) * 0.45D,
                    0.0D, 0.02D, 0.0D);
        }

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
        output.putBoolean("variant_chosen", this.variantChosen);
        output.putInt("variant", this.getVariant());

        output.putInt("rest_cooldown", this.restCooldown);
        this.flowerCarry.save(output);
        output.putBoolean("has_hat", this.hasHat());
        if (!this.getHatItem().isEmpty()) output.store("hat_item", ItemStack.CODEC, this.getHatItem());
        output.putBoolean("in_bowl", this.isInBowl());
        if (this.bowlPos != null) output.store("bowl_pos", BlockPos.CODEC, this.bowlPos);
        if (this.hatPos != null) output.store("hat_pos", BlockPos.CODEC, this.hatPos);
        if (this.friendId != null) output.store("friend_id", net.minecraft.core.UUIDUtil.CODEC, this.friendId);
        output.putInt("friend_trait", this.friendTrait);
        output.putString("personality", this.getPersonality().id());
        output.store("gift_memory", net.minecraft.core.registries.BuiltInRegistries.ITEM.byNameCodec().listOf(),
                new java.util.ArrayList<>(this.giftMemory));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.lastRestockGameTime = input.getLong("last_restock").orElse(0L);
        this.merchantLevel = Math.max(1, input.getInt("merchant_level").orElse(1));
        this.merchantXp = input.getInt("merchant_xp").orElse(0);
        this.pendingLevelUp = input.getBooleanOr("pending_level_up", false);
        this.variantChosen = input.getBooleanOr("variant_chosen", false);
        this.entityData.set(VARIANT, (byte) Mth.clamp(input.getInt("variant").orElse(0), 0, VARIANT_CAPE + CAPE_STYLES - 1));

        this.restCooldown = input.getInt("rest_cooldown").orElse(this.restCooldown);
        this.flowerCarry.load(input);
        this.syncHeldFlower();
        // El sombrero exacto (con sus colores); los Noxis de antes tenían el original.
        ItemStack savedHat = input.read("hat_item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        if (!savedHat.isEmpty()) {
            this.setHatItem(savedHat);
        } else {
            this.setHatItem(input.getBooleanOr("has_hat", true) ? com.noxisculture.item.NoxisHatColors.newHat() : ItemStack.EMPTY);
        }
        this.setInBowl(input.getBooleanOr("in_bowl", false));
        this.bowlPos = input.read("bowl_pos", BlockPos.CODEC).orElse(null);
        this.hatPos = input.read("hat_pos", BlockPos.CODEC).orElse(null);
        if (this.bowlPos == null) this.setInBowl(false);
        this.friendId = input.read("friend_id", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
        this.friendTrait = (byte) Mth.clamp(input.getInt("friend_trait").orElse(0).intValue(), 0, 2);
        this.friendLastSeen = 0L;              // se pone al día al verlo (ver tickFriendship)
        // Personalidad guardada; los Noxis de antes reciben una ahora (y queda guardada).
        this.personality = com.noxisculture.entity.personality.NoxisPersonality.byId(input.getStringOr("personality", ""));
        this.giftMemory.clear();
        input.read("gift_memory", net.minecraft.core.registries.BuiltInRegistries.ITEM.byNameCodec().listOf())
                .ifPresent(this.giftMemory::addAll);
    }

    /** Al desaparecer (muerte, descarga del chunk...), se lleva su luz. */
    @Override
    public void remove(Entity.RemovalReason reason) {
        this.lightController.clear(this.level());
        // Si muere, su amigo queda libre para hacer otro amigo algún día.
        if (!this.level().isClientSide() && reason == Entity.RemovalReason.KILLED) {
            com.noxisculture.entity.social.NoxisFriendship.onDeath(this);
        }
        // Lo que tenía reservado (regalo pendiente, flor que iba a buscar) queda libre.
        if (!this.level().isClientSide()) com.noxisculture.entity.nature.NoxisItemClaims.releaseAll(this);
        // Si muere (o lo eliminan) con una pecera reservada o ocupada, la pecera queda libre.
        // Si solo se descarga con el chunk, la reserva sigue en el bloque y la retoma al volver.
        if (this.level() instanceof ServerLevel server && this.bowlPos != null) {
            if (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED) {
                com.noxisculture.block.custom.NoxisBowlClaims.release(server, this.bowlPos, this);
            } else {
                com.noxisculture.block.custom.NoxisBowlClaims.forget(server, this.bowlPos, this);
            }
        }
        // Si muere (o lo eliminan) con una flor en la mano, la flor queda en el piso.
        if (!this.level().isClientSide()
                && (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED)) {
            this.flowerCarry.drop(this.level(), this);
        }
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
