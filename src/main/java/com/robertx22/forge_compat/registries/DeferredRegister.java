package com.robertx22.forge_compat.registries;

import com.robertx22.forge_compat.eventbus.api.IEventBus;
import com.robertx22.forge_compat.fml.RegistryObject;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Eager-registration DeferredRegister: entries are registered immediately,
 * which matches Fabric's register-during-init lifecycle.
 */
public class DeferredRegister<T> {

    private final Registry<Object> registry;
    private final String modid;
    private final List<Runnable> pending = new ArrayList<>();

    private DeferredRegister(Registry<Object> registry, String modid) {
        this.registry = registry;
        this.modid = modid;
    }

    @SuppressWarnings("unchecked")
    public static <T> DeferredRegister<T> create(Registry<Object> registry, String modid) {
        return new DeferredRegister<>(registry, modid);
    }

    public static <T> DeferredRegister<T> create(Object registry, String modid) {
        return new DeferredRegister<>((Registry<Object>) registry, modid);
    }

    public void register(IEventBus bus) {
        // entries are registered eagerly at register(id, supplier) time
    }

    @SuppressWarnings("unchecked")
    public synchronized <I extends T> RegistryObject<I> register(String name, Supplier<I> supplier) {
        I value = supplier.get();
        Identifier id = Identifier.fromNamespaceAndPath(modid, name);
        Registry.register((Registry<I>) registry, id, value);
        return new RegistryObject<>(id, value);
    }
}
