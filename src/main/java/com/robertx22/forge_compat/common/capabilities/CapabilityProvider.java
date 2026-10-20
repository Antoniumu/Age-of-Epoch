package com.robertx22.forge_compat.common.capabilities;

import com.robertx22.forge_compat.common.capabilities.CapabilityProvider;

import com.robertx22.forge_compat.common.util.LazyOptional;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.nbt.Tag;
/**
 * Runtime store for capability instances, attached lazily on first access
 * (equivalent to Forge attaching them at object construction).
 */
public class CapabilityProvider {

    private static final Map<Object, Map<String, ICapabilityProvider>> STORE = new WeakHashMap<>();

    @SuppressWarnings("unchecked")
    public static synchronized <T> LazyOptional<T> get(Object owner, Capability<T> cap) {
        if (owner == null) return LazyOptional.empty();
        Map<String, ICapabilityProvider> map = STORE.get(owner);
        if (map == null) {
            map = new java.util.HashMap<>();
            STORE.put(owner, map);
            AttachCapabilitiesEvent<Object> event = new AttachCapabilitiesEvent<>(owner);
            com.robertx22.forge_compat.common.MinecraftForge.EVENT_BUS.post(event);
            map.putAll(event.getProviders());
        }
        LazyOptional<T> result = LazyOptional.empty();
        for (ICapabilityProvider p : map.values()) {
            LazyOptional<T> r = CapabilityProvider.get(p, cap, null);
            if (r.isPresent()) {
                result = r;
                break;
            }
        }
        return result;
    }

    public static synchronized void saveAll(Object owner, CompoundTag tag) {
        Map<String, ICapabilityProvider> map = STORE.get(owner);
        if (map == null) return;
        for (Map.Entry<String, ICapabilityProvider> e : map.entrySet()) {
            if (e.getValue() instanceof ICapabilitySerializable) {
                tag.put(e.getKey(), ((ICapabilitySerializable<?>) e.getValue()).serializeNBT());
            }
        }
    }

    public static synchronized void loadAll(Object owner, CompoundTag tag) {
        Map<String, ICapabilityProvider> map = STORE.get(owner);
        if (map == null) return;
        for (Map.Entry<String, ICapabilityProvider> e : map.entrySet()) {
            if (e.getValue() instanceof ICapabilitySerializable && tag.contains(e.getKey())) {
                ((ICapabilitySerializable<net.minecraft.nbt.Tag>) e.getValue()).deserializeNBT(tag.get(e.getKey()));
            }
        }
    }
}
