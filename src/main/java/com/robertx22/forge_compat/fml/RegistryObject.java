package com.robertx22.forge_compat.fml;

import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

public class RegistryObject<T> implements Supplier<T> {

    private final Identifier id;
    private volatile T value;
    private Supplier<T> resolver;

    public RegistryObject(Identifier id, T value) {
        this.id = id;
        this.value = value;
        this.resolver = null;
    }

    public RegistryObject(Identifier id, Supplier<T> resolver, boolean lazy) {
        this.id = id;
        this.resolver = resolver;
    }

    @Override
    public T get() {
        if (value == null && resolver != null) {
            synchronized (this) {
                if (value == null && resolver != null) {
                    Supplier<T> r = resolver;
                    resolver = null;
                    value = r.get();
                }
            }
        }
        return value;
    }

    public Identifier getId() { return id; }

    public boolean isPresent() { return value != null || resolver != null; }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public net.minecraft.resources.ResourceKey<T> getKey() {
        return net.minecraft.resources.ResourceKey.create((net.minecraft.resources.ResourceKey) net.minecraft.core.registries.Registries.ITEM, this.id);
    }
}
