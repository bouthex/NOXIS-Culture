package com.noxisculture.mixin.client;

import com.noxisculture.client.NoyuxClientEffects;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Niebla del Noyux: como el Noyux está en la etiqueta de la lava (para moverse como en ella), el juego
 * le pone la niebla espesa de la lava. Se conserva lo espesa que es y solo se cambia el color a violeta,
 * únicamente con la cámara dentro del Noyux (la lava sigue igual). Si el método cambiara en otra
 * versión, el enganche simplemente no se aplica.
 */
@Mixin(FogRenderer.class)
public abstract class FogRendererNoyuxMixin {
    @Inject(method = "computeFogColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IFLorg/joml/Vector4f;)V",
            at = @At("TAIL"), require = 0)
    private void noxisCulture$violetFog(Camera camera, float partialTick, ClientLevel level, int renderDistance,
                                        float darkenWorldAmount, Vector4f dest, CallbackInfo ci) {
        if (NoyuxClientEffects.isCameraInNoyux(camera, level)) {
            dest.set(NoyuxClientEffects.FOG_R, NoyuxClientEffects.FOG_G, NoyuxClientEffects.FOG_B, 1.0F);
        }
    }
}
