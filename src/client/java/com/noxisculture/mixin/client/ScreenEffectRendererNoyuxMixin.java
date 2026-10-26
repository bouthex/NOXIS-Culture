package com.noxisculture.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.noxisculture.client.NoyuxClientEffects;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Llamas en primera persona al quemarse con Noyux: el juego dibuja las llamas cuando el jugador está
 * prendido fuego; acá también mientras toca Noyux o su fuego, y con la textura del fuego violeta en
 * vez de la del fuego común. Al salir, desaparecen solas (se revisa en cada cuadro). El fuego normal
 * no cambia. Opcional: si algo cambiara en otra versión, el enganche no se aplica.
 */
@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererNoyuxMixin {
    @ModifyExpressionValue(method = "submit",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isOnFire()Z"), require = 0)
    private boolean noxisCulture$burningInNoyux(boolean onFire) {
        return onFire || NoyuxClientEffects.isPlayerBurningInNoyux();
    }

    @WrapOperation(method = "submit",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;submitFire(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"),
            require = 0)
    private void noxisCulture$violetFlames(PoseStack poseStack, SubmitNodeCollector collector, TextureAtlasSprite sprite,
                                           Operation<Void> original) {
        TextureAtlasSprite violet = NoyuxClientEffects.isPlayerBurningInNoyux() ? NoyuxClientEffects.fireSprite() : null;
        original.call(poseStack, collector, violet != null ? violet : sprite);
    }
}
