package com.robertx22.forge_compat.fml;

import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

public class RegistryObject<T> implements Supplier<T> {

    private final Identifier id;
    private final T value;

    public RegistryObject(Identifier id, T value) {
        this.id = id;
        this.value = value;
    }

    @Override
    public T get() { return value; }

    public Identifier getId() { return id; }

    public boolean isPresent() { return value != null; }
}
