package com.robertx22.forge_compat.registries;

import com.robertx22.forge_compat.eventbus.api.IEventBus;
import com.robertx22.forge_compat.fml.RegistryObject;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class DeferredRegister<T> {

    private static final List<DeferredRegister<?>> ALL_REGISTERS = Collections.synchronizedList(new ArrayList<>());

    private final Registry<Object> registry;
    private final String modid;
    private final List<RegistryObject<?>> entries = new ArrayList<>();

    private DeferredRegister(Registry<Object> registry, String modid) {
        this.registry = registry;
        this.modid = modid;
        ALL_REGISTERS.add(this);
    }

    @SuppressWarnings("unchecked")
    public static <T> DeferredRegister<T> create(Registry<Object> registry, String modid) {
        return new DeferredRegister<>(registry, modid);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> DeferredRegister<T> create(Object registryOrKey, String modid) {
        if (registryOrKey instanceof Registry<?> reg) {
            return new DeferredRegister<>((Registry<Object>) reg, modid);
        }
        if (registryOrKey instanceof ResourceKey<?> rk) {
            Registry<?> resolved = BuiltInRegistries.REGISTRY.getValue(rk.identifier());
            return new DeferredRegister<>((Registry<Object>) resolved, modid);
        }
        return new DeferredRegister<>(null, modid);
    }

    public void register(IEventBus bus) {
    }

    @SuppressWarnings("unchecked")
    public synchronized <I extends T> RegistryObject<I> register(String name, Supplier<I> supplier) {
        Identifier id = Identifier.fromNamespaceAndPath(modid, name);
        RegistryObject<I> obj = new RegistryObject<>(id, () -> {
            if (registry != null && registry.containsKey(id)) {
                return (I) registry.getValue(id);
            }
            PropertiesIdHelper.CURRENT_ID.set(id);
            I value;
            try {
                value = supplier.get();
            } finally {
                PropertiesIdHelper.CURRENT_ID.remove();
            }
            PropertiesIdHelper.fixRegisteredDescriptionId(value, id);
            if (registry != null) {
                Registry.register((Registry<I>) registry, id, value);
            }
            return value;
        }, true);
        entries.add(obj);
        return obj;
    }

    public synchronized void flush() {
        for (RegistryObject<?> obj : new ArrayList<>(entries)) {
            obj.get();
        }
    }

    public static void flushAll() {
        List<DeferredRegister<?>> snapshot;
        synchronized (ALL_REGISTERS) {
            snapshot = new ArrayList<>(ALL_REGISTERS);
        }
        for (DeferredRegister<?> dr : snapshot) {
            dr.flush();
        }
    }
}
