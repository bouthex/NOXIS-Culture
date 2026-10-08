package com.noxisculture.client.entity;

import com.noxisculture.NoxisCulture;
import com.noxisculture.entity.custom.NoxisVillager;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class NoxisVillagerRenderer extends MobRenderer<NoxisVillager, NoxisVillagerRenderState, NoxisVillagerModel> {
    private static final Identifier TEXTURE = NoxisCulture.id("textures/entity/noxis_villager.png");

    public NoxisVillagerRenderer(EntityRendererProvider.Context context) {
        super(context, new NoxisVillagerModel(context.bakeLayer(ModModelLayers.NOXIS_VILLAGER)), 0.45F);
        this.addLayer(new NoxisVillagerEyesLayer(this));
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
        state.restRising = !entity.isResting();
    }

    @Override
    public Identifier getTextureLocation(NoxisVillagerRenderState state) {
        return TEXTURE;
    }
}
