package com.robertx22.forge_compat.registries;

import net.minecraft.resources.Identifier;

/** Marker retained for source compatibility with Forge registry generics. */
public interface IForgeRegistryEntry<T> {

    default Identifier getRegistryName() {
        return null;
    }
}
