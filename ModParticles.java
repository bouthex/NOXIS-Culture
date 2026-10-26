package com.noxisculture.particle;

import com.noxisculture.NoxisCulture;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Partículas del Noyux (sus dibujos están en assets/noxis_culture/textures/particle y la lista de
 * cuadros en assets/noxis_culture/particles). El cliente les asigna el movimiento en NoxisCultureClient.
 */
public final class ModParticles {
    private ModParticles() {}

    /** Voluta de humo violeta que nace chiquita, se enrosca, se abre en anillo y se deshace. */
    public static final SimpleParticleType NOYUX_SMOKE = register("noyux_smoke");
    /** Destello morado/magenta en forma de estrella de cuatro puntas. */
    public static final SimpleParticleType NOYUX_SPARK = register("noyux_spark");
    /** Chispa luminosa que sube y se apaga de a poco. */
    public static final SimpleParticleType NOYUX_MOTE = register("noyux_mote");

    private static SimpleParticleType register(String name) {
        return Registry.register(BuiltInRegistries.PARTICLE_TYPE, NoxisCulture.id(name), FabricParticleTypes.simple());
    }

    public static void initialize() {}
}
