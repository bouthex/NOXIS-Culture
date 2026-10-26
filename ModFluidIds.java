package com.noxisculture.fluid;

import com.noxisculture.NoxisCulture;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.material.Fluid;

public final class ModFluidIds {
    private ModFluidIds() {}

    public static final ResourceKey<Fluid> NOYUX = create("noyux");
    public static final ResourceKey<Fluid> FLOWING_NOYUX = create("flowing_noyux");

    private static ResourceKey<Fluid> create(String name) {
        return ResourceKey.create(Registries.FLUID, NoxisCulture.id(name));
    }
}
