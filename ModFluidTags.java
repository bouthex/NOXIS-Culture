package com.noxisculture.fluid;

import com.noxisculture.NoxisCulture;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public final class ModFluidTags {
    private ModFluidTags() {}

    public static final TagKey<Fluid> NOYUX = TagKey.create(Registries.FLUID, NoxisCulture.id("noyux"));
}
