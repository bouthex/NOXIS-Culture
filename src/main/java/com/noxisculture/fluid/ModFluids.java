package com.noxisculture.fluid;

import com.noxisculture.fluid.custom.NoyuxFluid;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;

public final class ModFluids {
    private ModFluids() {}

    public static final FlowingFluid NOYUX = register(ModFluidIds.NOYUX, new NoyuxFluid.Source());
    public static final FlowingFluid FLOWING_NOYUX = register(ModFluidIds.FLOWING_NOYUX, new NoyuxFluid.Flowing());

    private static FlowingFluid register(ResourceKey<Fluid> key, FlowingFluid fluid) {
        return Registry.register(BuiltInRegistries.FLUID, key, fluid);
    }

    public static void initialize() {}
}
