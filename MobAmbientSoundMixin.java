package com.noxisculture.mixin.common;

import com.noxisculture.entity.hearing.NoxisSounds;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cuando una criatura hace su sonido de siempre (el mugido de la vaca, el cacareo de la gallina...),
 * Minecraft no avisa con ningún evento de juego. Este enganche avisa solo en ese momento a los Noxis
 * cercanos (no se revisa nada cada tick). Es opcional: si algún día no encuentra el método, el juego
 * sigue funcionando y los Noxis siguen escuchando el resto de los sonidos.
 */
@Mixin(Mob.class)
public abstract class MobAmbientSoundMixin {
    @Inject(method = "playAmbientSound", at = @At("HEAD"), require = 0)
    private void noxisCulture$tellNoxis(CallbackInfo ci) {
        NoxisSounds.onCreatureCall((Mob) (Object) this);
    }
}
