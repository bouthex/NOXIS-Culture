package com.noxisculture.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.noxisculture.client.NoxisVision;
import com.noxisculture.entity.NoxisSpecies;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Visión Noxis: al observar a un Noxis en modo Espectador (la cámara "se mete" en él), el
 * juego usa nuestro efecto de pantalla en vez de ninguno, igual que hace vanilla con el
 * creeper, la araña o el enderman. Para cualquier otra entidad se deja el comportamiento
 * vanilla tal cual (así el efecto del enderman sigue igual) y, al dejar de observarlo, vanilla
 * lo quita como siempre.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    protected abstract void setPostEffect(Identifier id);

    @WrapMethod(method = "checkEntityPostEffect")
    private void noxisCulture$noxisVision(@Nullable Entity cameraEntity, Operation<Void> original) {
        if (cameraEntity instanceof NoxisSpecies) {
            this.setPostEffect(NoxisVision.POST_EFFECT);
        } else {
            original.call(cameraEntity);
        }
    }
}
