package com.noxisculture.client;

import com.noxisculture.NoxisCulture;
import net.minecraft.resources.Identifier;

/**
 * La visión Noxis: un efecto de pantalla (post-procesado, como el del creeper o la araña) que se
 * activa solo al observar a un Noxis en modo Espectador. Tiñe el mundo de un amarillo cálido y
 * suave y hace que los violetas y púrpuras resalten muchísimo. Se calcula sobre la imagen final,
 * así que funciona con todo (vanilla, otros mods y lo que agreguemos), sin tocar texturas ni la
 * iluminación, y no afecta al HUD ni a los menús.
 *
 * <p>Archivos: {@code assets/noxis_culture/post_effect/noxis_vision.json} y
 * {@code assets/noxis_culture/shaders/post/noxis_vision.fsh} (ahí se ajustan la intensidad del
 * amarillo y del violeta).</p>
 */
public final class NoxisVision {
    public static final Identifier POST_EFFECT = NoxisCulture.id("noxis_vision");

    private NoxisVision() {
    }
}
