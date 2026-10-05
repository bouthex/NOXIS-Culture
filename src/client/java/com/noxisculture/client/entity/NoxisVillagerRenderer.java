package com.noxisculture.client.entity;

import com.noxisculture.NoxisCulture;
import com.noxisculture.entity.custom.NoxisVillager;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class NoxisVillagerRenderer extends MobRenderer<NoxisVillager, NoxisVillagerRenderState, NoxisVillagerModel> {
    private static final Identifier TEXTURE = NoxisCulture.id("textures/entity/noxis_villager.png");

    public NoxisVillagerRenderer(EntityRendererProvider.Context context) {
        super(context, new NoxisVillagerModel(context.bakeLayer(ModModelLayers.NOXIS_VILLAGER)), 0.35F);
    }

    @Override
    public NoxisVillagerRenderState createRenderState() {
        return new NoxisVillagerRenderState();
    }

    @Override
    public Identifier getTextureLocation(NoxisVillagerRenderState state) {
        return TEXTURE;
    }
}
