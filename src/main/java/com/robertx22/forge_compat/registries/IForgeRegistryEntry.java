package com.robertx22.forge_compat.registries;

import net.minecraft.resources.ResourceLocation;

/** Marker retained for source compatibility with Forge registry generics. */
public interface IForgeRegistryEntry<T> {

    default ResourceLocation getRegistryName() {
        return null;
    }
}
