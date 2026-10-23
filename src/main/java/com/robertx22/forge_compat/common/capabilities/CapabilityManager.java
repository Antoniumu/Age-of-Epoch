package com.robertx22.forge_compat.common.capabilities;

import com.robertx22.forge_compat.event.AttachCapabilitiesEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class CapabilityManager {

    public static final CapabilityManager INSTANCE = new CapabilityManager();

    private final Map<Class<?>, Capability<?>> byType = new ConcurrentHashMap<>();
    private final Map<Class<?>, Supplier<?>> factories = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public synchronized <T> Capability<T> register(Class<T> type, Capability.IStorage<T> storage, Supplier<? extends T> factory) {
        Capability<T> cap = new Capability<>(type, storage);
        byType.put(type, cap);
        factories.put(type, factory);
        // emulate Forge's @CapabilityInject field injection
        List<Class<?>> scan = new ArrayList<>();
        Class<?> root = type.getDeclaringClass() == null ? type : type.getDeclaringClass();
        scan.add(root);
        if (root != type) {
            scan.add(type);
        }
        for (int i = 0; i < scan.size(); i++) {
            Class<?> c = scan.get(i);
            if (c == null) continue;
            for (Class<?> inner : c.getDeclaredClasses()) {
                if (!scan.contains(inner)) {
                    scan.add(inner);
                }
            }
        }
        for (Class<?> c : scan) {
            if (c == null) continue;
            // auto-register @Mod.EventBusSubscriber inner classes (Forge did this implicitly)
            if (c.isAnnotationPresent(com.robertx22.forge_compat.fml.common.Mod.EventBusSubscriber.class)) {
                com.robertx22.forge_compat.common.MinecraftForge.EVENT_BUS.register(c);
            }
            for (Field f : c.getDeclaredFields()) {
                CapabilityInject inject = f.getAnnotation(CapabilityInject.class);
                if (inject == null) continue;
                if (!inject.value().equals(type)) continue;
                try {
                    f.setAccessible(true);
                    Field modifiers = Field.class.getDeclaredField("modifiers");
                    modifiers.setAccessible(true);
                    modifiers.setInt(f, f.getModifiers() & ~Modifier.FINAL);
                } catch (Exception ignored) {
                    // Java 12+: modifiers field removed; fall through to set() which
                    // works for non-final fields (port drops 'final' on these fields).
                }
                try {
                    f.setAccessible(true);
                    f.set(null, cap);
                } catch (Exception e) {
                    throw new RuntimeException("Cannot inject capability " + type, e);
                }
            }
        }
        return cap;
    }

    @SuppressWarnings("unchecked")
    public <T> Capability<T> getCapability(Class<T> type) {
        return (Capability<T>) byType.get(type);
    }

    @SuppressWarnings("unchecked")
    public <T> T createInstance(Capability<T> cap) {
        Supplier<?> s = factories.get(cap.getType());
        return s == null ? null : (T) s.get();
    }

    /** Fires Forge's AttachCapabilitiesEvent for an object exactly once. */
    public static void attachFor(Object object) {
        AttachCapabilitiesEvent<?> event = new AttachCapabilitiesEvent<>(object);
        com.robertx22.forge_compat.common.MinecraftForge.EVENT_BUS.post(event);
    }
}
