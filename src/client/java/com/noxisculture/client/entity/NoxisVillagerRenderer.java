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
        // La flor en la mano (interacciones con la naturaleza).
        this.addLayer(new NoxisHeldFlowerLayer(this));
        this.addLayer(new NoxisCrystalGlowLayer(this,
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
        ItemStack flower = entity.getHeldFlower();
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
