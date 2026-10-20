package com.robertx22.forge_compat.fml;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public class RegistryObject<T> implements Supplier<T> {

    private final ResourceLocation id;
    private final T value;

    public RegistryObject(ResourceLocation id, T value) {
        this.id = id;
        this.value = value;
    }

    @Override
    public T get() { return value; }

    public ResourceLocation getId() { return id; }

    public boolean isPresent() { return value != null; }
}
