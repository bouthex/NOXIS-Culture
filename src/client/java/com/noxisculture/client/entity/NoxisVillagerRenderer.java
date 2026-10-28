package com.noxisculture.client.entity;

import com.noxisculture.NoxisCulture;
import com.noxisculture.entity.custom.NoxisVillager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class NoxisVillagerRenderer extends MobRenderer<NoxisVillager, NoxisVillagerRenderState, NoxisVillagerModel> {
    private static final Identifier TEXTURE = NoxisCulture.id("textures/entity/noxis_villager.png");

    public NoxisVillagerRenderer(EntityRendererProvider.Context context) {
        super(context, new NoxisVillagerModel(context.bakeLayer(ModModelLayers.NOXIS_VILLAGER)), 0.45F);
        this.addLayer(new NoxisVillagerEyesLayer(this));
        // Brillo extra de los ojos al fascinarse con un cristal (copia del modelo que solo dibuja los ojitos).
        // Globito de sueño mientras duerme (copia del modelo que solo dibuja el globito).
        this.addLayer(new NoxisSleepBubbleLayer(this,
                new NoxisVillagerModel(context.bakeLayer(ModModelLayers.NOXIS_VILLAGER), NoxisVillagerModel.PASS_SLEEP_BUBBLE)));
        // La flor en la mano (interacciones con la naturaleza).
        this.addLayer(new NoxisHeldFlowerLayer(this));
        // El sombrero con sus colores (copa/ala, lazo y lo fijo: ramita y gema).
        this.addLayer(new NoxisHatLayer(this,
                new NoxisVillagerModel(context.bakeLayer(ModModelLayers.NOXIS_VILLAGER), NoxisVillagerModel.PASS_HAT_CROWN),
                new NoxisVillagerModel(context.bakeLayer(ModModelLayers.NOXIS_VILLAGER), NoxisVillagerModel.PASS_HAT_BAND),
                new NoxisVillagerModel(context.bakeLayer(ModModelLayers.NOXIS_VILLAGER), NoxisVillagerModel.PASS_HAT_FIXED)));
        // Libro abierto y lápiz de los lectores / escritores (copia del modelo con su propia textura).
        this.addLayer(new NoxisPropsLayer(this,
                new NoxisVillagerModel(context.bakeLayer(ModModelLayers.NOXIS_VILLAGER), NoxisVillagerModel.PASS_PROPS)));
        this.addLayer(new NoxisCrystalGlowLayer(this,
                new NoxisVillagerModel(context.bakeLayer(ModModelLayers.NOXIS_VILLAGER), NoxisVillagerModel.PASS_GLOW_EYES)));
        // Resonancia con el disco amarillo: cristales dorados y ojitos más cálidos (solo mientras suena cerca).
        this.addLayer(new NoxisResonanceLayer(this,
                new NoxisVillagerModel(context.bakeLayer(ModModelLayers.NOXIS_VILLAGER), NoxisVillagerModel.PASS_NORMAL),
                new NoxisVillagerModel(context.bakeLayer(ModModelLayers.NOXIS_VILLAGER), NoxisVillagerModel.PASS_GLOW_EYES)));
    }

    @Override
    public NoxisVillagerRenderState createRenderState() {
        return new NoxisVillagerRenderState();
    }

    @Override
    public void extractRenderState(NoxisVillager entity, NoxisVillagerRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.happyAnim = entity.getHappyAnim();
        state.scaredAnim = entity.getScaredAnim();
        state.torchAnim = entity.getTorchAnim();
        state.restAnim = entity.getRestAnim();
        state.umbrellaAnim = entity.getUmbrellaAnim();
        state.variant = entity.getVariant();
        state.capeSwing = entity.getCapePhysics().getSwing(partialTick);
        state.capeTurn = entity.getCapePhysics().getTurn(partialTick);
        state.curious = entity.getCuriosity().getAmount(partialTick);
        state.curiousSide = entity.getCuriosity().getSide();
        state.curiousFlick = entity.getCuriosity().getFlick(partialTick);
        state.crystalAmount = entity.getCrystalFascination().getAmount(partialTick);
        state.crystalGlow = entity.getCrystalFascination().getGlow(partialTick);
        state.crystalCheer = entity.getCrystalFascination().getCheer(partialTick);
        state.crystalHop = entity.getCrystalFascination().getHop(partialTick);
        state.crystalTwitch = entity.getCrystalFascination().getTwitch(partialTick);
        state.crystalSide = entity.getCrystalFascination().getSide();
        state.natureSurvey = entity.getNatureAnimation().getSurvey(partialTick);
        state.natureLean = entity.getNatureAnimation().getLean(partialTick);
        state.natureHold = entity.getNatureAnimation().getHold(partialTick);
        state.natureRaise = entity.getNatureAnimation().getRaise(partialTick);
        state.natureSniff = entity.getNatureAnimation().getSniff(partialTick);
        state.natureEyesClosed = entity.getNatureAnimation().getEyesClosed(partialTick);
        state.natureEars = entity.getNatureAnimation().getEars(partialTick);
        state.natureTilt = entity.getNatureAnimation().getTilt(partialTick);
        state.naturePupils = entity.getNatureAnimation().getPupils(partialTick);
        state.natureSit = entity.getNatureAnimation().getSit(partialTick);
        state.natureOffer = entity.getNatureAnimation().getOffer(partialTick);
        state.natureSide = entity.getNatureAnimation().getSide();
        state.socialTilt = entity.getSocialAnimation().getTilt(partialTick);
        state.socialSide = entity.getSocialAnimation().getSide();
        state.socialEars = entity.getSocialAnimation().getEars(partialTick);
        state.socialWave = entity.getSocialAnimation().getWave(partialTick);
        state.socialNod = entity.getSocialAnimation().getNod(partialTick);
        state.socialHop = entity.getSocialAnimation().getHop(partialTick);
        state.socialDoze = entity.getSocialAnimation().getDoze(partialTick);
        state.socialYawn = entity.getSocialAnimation().getYawn(partialTick);
        state.socialLean = entity.getSocialAnimation().getLean(partialTick);
        state.inBowl = entity.isInBowl();
        // Pecera tapada por otra encima: se acurruca un poquito más (nada toca la base de arriba).
        net.minecraft.world.level.block.state.BlockState bowlState = entity.level().getBlockState(entity.blockPosition());
        state.bowlCovered = state.inBowl && bowlState.is(com.noxisculture.block.ModBlocks.NOXIS_BOWL)
                && bowlState.getValue(com.noxisculture.block.custom.NoxisBowlBlock.COVERED);
        state.hasHat = entity.hasHat();
        ItemStack hatItem = entity.getHatItem();
        state.hatCrownColor = com.noxisculture.item.NoxisHatColors.crown(hatItem);
        state.hatBandColor = com.noxisculture.item.NoxisHatColors.band(hatItem);
        state.hatAnim = entity.getHatAnimation().getAnim();
        state.hatAnimTime = entity.getHatAnimation().getTime(partialTick);
        state.bowlHop = entity.getBowlHopAnimation().getAnim();
        state.bowlHopTime = entity.getBowlHopAnimation().getTime(partialTick);
        com.noxisculture.entity.idle.NoxisBoredAnimation bored = entity.getBoredAnimation();
        state.boredType = bored.getType();
        state.boredMirrored = bored.isMirrored();
        state.boredTime = bored.getTime(partialTick);
        state.boredWeight = bored.isBusy() ? bored.getWeight(partialTick) : 0.0F;
        boolean boredShown = state.boredWeight > 0.5F;
        state.boredEyesClosed = boredShown && com.noxisculture.entity.idle.NoxisBoredAnimation.eyesClosed(state.boredType, state.boredTime);
        state.boredPupils = boredShown && com.noxisculture.entity.idle.NoxisBoredAnimation.bigPupils(state.boredType, state.boredTime);
        state.boredTired = boredShown && com.noxisculture.entity.idle.NoxisBoredAnimation.tiredEyes(state.boredType, state.boredTime);
        com.noxisculture.entity.idle.NoxisKeepsakeAnimation keep = entity.getKeepsakeAnimation();
        state.keepsake = keep.getAnim();
        state.keepsakeTime = keep.getTime(partialTick);
        state.keepsakeWeight = keep.isBusy() ? keep.getWeight(partialTick) : 0.0F;
        boolean keepShown = state.keepsakeWeight > 0.5F;
        state.keepsakePupils = keepShown && com.noxisculture.entity.idle.NoxisKeepsakeAnimation.bigPupils(state.keepsake, state.keepsakeTime);
        state.keepsakeClosed = keepShown && com.noxisculture.entity.idle.NoxisKeepsakeAnimation.eyesClosed(state.keepsake, state.keepsakeTime);
        state.scholarSit = state.keepsakeWeight * com.noxisculture.entity.idle.NoxisKeepsakeAnimation.sit(state.keepsake, state.keepsakeTime);
        state.scholarFocused = keepShown && com.noxisculture.entity.idle.NoxisKeepsakeAnimation.focused(state.keepsake, state.keepsakeTime);
        com.noxisculture.entity.idle.NoxisPersonaAnimation persona = entity.getPersonaAnimation();
        state.persona = persona.getAnim();
        state.personaTime = persona.getTime(partialTick);
        state.personaWeight = persona.isBusy() ? persona.getWeight(partialTick) : 0.0F;
        boolean personaShown = state.personaWeight > 0.5F;
        state.personaPupils = personaShown && com.noxisculture.entity.idle.NoxisPersonaAnimation.bigPupils(state.persona, state.personaTime);
        state.personaHalf = personaShown && com.noxisculture.entity.idle.NoxisPersonaAnimation.halfClosed(state.persona, state.personaTime);
        state.personaClosed = personaShown && com.noxisculture.entity.idle.NoxisPersonaAnimation.closed(state.persona, state.personaTime);
        com.noxisculture.entity.idle.NoxisPlayAnimation play = entity.getPlayAnimation();
        for (byte i = 1; i < com.noxisculture.entity.idle.NoxisPlayAnimation.COUNT; i++) {
            state.playWeight[i] = play.getWeight(i, partialTick);
            state.playTime[i] = play.getTime(i, partialTick);
        }
        state.watchSit = state.playWeight[com.noxisculture.entity.idle.NoxisPlayAnimation.SIT_WATCH];
        com.noxisculture.entity.idle.NoxisAmbientAnimation amb = entity.getAmbientAnimation();
        boolean ambBusy = amb.isBusy();
        for (int i = 1; i < com.noxisculture.entity.idle.NoxisAmbientAnimation.COUNT; i++) {
            state.ambientWeight[i] = ambBusy ? amb.getWeight(i, partialTick) : 0.0F;
            state.ambientTime[i] = amb.getTime(i, partialTick);
        }
        state.ambientPartnerRight = amb.isPartnerRight();
        state.ambientPhase = (entity.getId() % 7) * 0.9F;
        state.ambientSit = ambBusy ? amb.getSit(partialTick) : 0.0F;
        state.easelScale = 0.0F;   // el atril ahora es un bloque real del mundo (NoxisEaselBlock): ya no se dibuja en el Noxis
        state.canvasStage = amb.getCanvasStage();
        state.brushShown = amb.toolsInHand();
        state.paletteShown = state.brushShown;
        com.noxisculture.entity.idle.NoxisHumAnimation humAnim = entity.getHumAnimation();
        state.hum = humAnim.getState();
        state.humWeight = humAnim.isBusy() ? humAnim.getWeight(partialTick) : 0.0F;
        state.humBeatTime = entity.level().getGameTime() + partialTick;
        state.humEyesHalf = state.humWeight > 0.5F && state.hum >= 1 && state.hum <= 3;   // tarareando: ojitos entornados
        state.whistleMouth = state.humWeight > 0.35F && com.noxisculture.entity.idle.NoxisHumAnimation.isWhistle(state.hum);
        byte playState = play.getState();
        boolean playShown = playState != 0 && state.playWeight[playState] > 0.5F;
        state.playSly = playShown && com.noxisculture.entity.idle.NoxisPlayAnimation.slyEyes(playState);
        state.playPupils = playShown && com.noxisculture.entity.idle.NoxisPlayAnimation.bigPupils(playState);
        state.playLaugh = playShown && com.noxisculture.entity.idle.NoxisPlayAnimation.laughing(playState, state.playTime[playState]);
        com.noxisculture.entity.idle.NoxisListenAnimation listen = entity.getListenAnimation();
        for (byte i = 1; i < 4; i++) {
            state.listenWeight[i] = listen.getWeight(i, partialTick);
            state.listenTime[i] = listen.getTime(i, partialTick);
        }
        state.listenDir = listen.getDir(partialTick);
        state.listenSide = listen.getSide();
        byte listenState = listen.getState();
        boolean listenShown = listenState != 0 && state.listenWeight[listenState] > 0.5F;
        state.listenPupils = listenShown && com.noxisculture.entity.idle.NoxisListenAnimation.bigPupils(listenState, state.listenTime[listenState]);
        state.listenClosed = listenShown && com.noxisculture.entity.idle.NoxisListenAnimation.closed(listenState, state.listenTime[listenState]);
        state.listenNoBlink = listenShown && com.noxisculture.entity.idle.NoxisListenAnimation.noBlink(listenState, state.listenTime[listenState]);
        state.resonance = entity.getResonanceAmount(partialTick);
        state.resonanceBeat = ((int) entity.level().getGameTime() - entity.getResonanceOrigin() + partialTick)
                / com.noxisculture.entity.hearing.NoxisDanceTempo.beatTicksOf(com.noxisculture.sound.ModJukeboxSongs.GOLDEN_SIESTA);
        com.noxisculture.entity.idle.NoxisDanceAnimation dance = entity.getDanceAnimation();
        state.danceWeight = dance.getWeight(com.noxisculture.entity.idle.NoxisDanceAnimation.DANCE, partialTick);
        state.danceFallWeight = dance.getWeight(com.noxisculture.entity.idle.NoxisDanceAnimation.FALL, partialTick);
        state.danceFallTime = dance.getTime(com.noxisculture.entity.idle.NoxisDanceAnimation.FALL, partialTick);
        state.dancePairFallWeight = dance.getWeight(com.noxisculture.entity.idle.NoxisDanceAnimation.PAIR_FALL, partialTick);
        state.dancePairFallTime = dance.getTime(com.noxisculture.entity.idle.NoxisDanceAnimation.PAIR_FALL, partialTick);
        state.danceStyle = dance.getStyle();
        state.dancePrevStyle = dance.getPrevStyle();
        state.danceStyleBlend = dance.getStyleBlend(partialTick);
        state.danceMirror = com.noxisculture.entity.idle.NoxisDanceAnimation.mirrored(state.danceStyle) ? -1.0F : 1.0F;
        state.dancePairRole = com.noxisculture.entity.idle.NoxisDanceAnimation.mirrored(state.danceStyle) ? 1 : 0;
        state.danceSeed = entity.getId();
        // Pulso: tiempo de la canción con el reloj del mundo (el mismo para todos los que bailan).
        int clock = (int) entity.level().getGameTime();
        state.danceBeatTicks = entity.getDanceBeat();
        state.danceBeatRaw = ((clock - entity.getDanceOrigin()) + partialTick) / state.danceBeatTicks;
        state.danceClosed = state.danceWeight > 0.5F
                && com.noxisculture.entity.idle.NoxisDanceAnimation.danceClosed(state.danceBeatRaw, state.danceStyle);
        boolean fallShown = state.danceFallWeight > 0.5F;
        boolean pairFallShown = state.dancePairFallWeight > 0.5F;
        float ft = state.danceFallTime;
        float pt = state.dancePairFallTime;
        state.danceClosed |= (fallShown && com.noxisculture.entity.idle.NoxisDanceAnimation.fallClosed(ft))
                || (pairFallShown && com.noxisculture.entity.idle.NoxisDanceAnimation.pairFallClosed(pt));
        state.danceHalf = (fallShown && com.noxisculture.entity.idle.NoxisDanceAnimation.fallHalf(ft))
                || (pairFallShown && com.noxisculture.entity.idle.NoxisDanceAnimation.pairFallHalf(pt));
        state.danceBig = (fallShown && com.noxisculture.entity.idle.NoxisDanceAnimation.fallBig(ft))
                || (pairFallShown && com.noxisculture.entity.idle.NoxisDanceAnimation.pairFallBig(pt));
        com.noxisculture.entity.idle.NoxisAffectionAnimation aff = entity.getAffectionAnimation();
        state.affection = aff.getAnim();
        state.affectionTime = aff.getTime(partialTick);
        state.affectionWeight = aff.isBusy() ? aff.getWeight(partialTick) : 0.0F;
        boolean affShown = state.affectionWeight > 0.5F;
        state.affectionEyesClosed = affShown && com.noxisculture.entity.idle.NoxisAffectionAnimation.eyesClosed(state.affection, state.affectionTime);
        state.affectionPupils = affShown && com.noxisculture.entity.idle.NoxisAffectionAnimation.bigPupils(state.affection, state.affectionTime);
        com.noxisculture.entity.idle.NoxisRestGesture rg = entity.getRestGesture();
        state.restGesture = rg.getGesture();
        state.restGestTime = rg.getTime(partialTick);
        state.restGestWeight = rg.isBusy() ? rg.getWeight(partialTick) : 0.0F;
        state.restEyes = state.restGestWeight > 0.5F
                ? com.noxisculture.entity.idle.NoxisRestGesture.eyes(state.restGesture, state.restGestTime) : 0;
        byte rgType = com.noxisculture.entity.idle.NoxisRestGesture.typeOf(state.restGesture);
        float awake = 0.0F;
        if (rgType == com.noxisculture.entity.idle.NoxisRestGesture.STRETCH_SEATED) {
            awake = com.noxisculture.entity.idle.NoxisBoredAnimation.smooth(
                    (state.restGestTime - com.noxisculture.entity.idle.NoxisRestGesture.lead(state.restGesture)) / 10.0F);
        } else if (rgType == com.noxisculture.entity.idle.NoxisRestGesture.STRETCH_STAND) {
            awake = 1.0F;
        }
        state.restAwake = awake * state.restGestWeight;
        state.bubbleRest = rg.bubbleRest(partialTick);
        ItemStack flower = entity.getHeldFlower();
        // Contemplando un tesoro: se ve en sus manitos (solo una imagen; sigue guardado en su inventario).
        ItemStack keepsakeItem = entity.getKeepsakeItem();
        // Leyendo / escribiendo: el libro se dibuja con su modelo 3D (no con la imagen del ítem).
        boolean bookAnim = com.noxisculture.entity.idle.NoxisKeepsakeAnimation.isBook(state.keepsake);
        state.bookShown = bookAnim && !keepsakeItem.isEmpty();
        state.pencilShown = state.bookShown && com.noxisculture.entity.idle.NoxisKeepsakeAnimation.isWriting(state.keepsake);
        state.keepsakeHolding = flower.isEmpty() && !keepsakeItem.isEmpty();
        if (state.keepsakeHolding && !bookAnim) flower = keepsakeItem;
        if (flower.isEmpty()) {
            state.heldFlower.clear();
        } else {
            Minecraft.getInstance().getItemModelResolver()
                    .updateForLiving(state.heldFlower, flower, ItemDisplayContext.FIXED, entity);
        }
        state.restRising = !entity.isResting();
    }

    @Override
    public Identifier getTextureLocation(NoxisVillagerRenderState state) {
        return TEXTURE;
    }
}
