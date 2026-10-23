package com.robertx22.forge_compat.common.capabilities;

import com.robertx22.forge_compat.common.util.LazyOptional;
import com.robertx22.forge_compat.event.AttachCapabilitiesEvent;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Runtime store for capability instances, attached lazily on first access
 * (equivalent to Forge attaching them at object construction).
 */
public class CapabilityProvider {

    private static final Map<Object, Map<String, ICapabilityProvider>> STORE = new WeakHashMap<>();
    private static final Map<Object, Map<Capability<?>, LazyOptional<?>>> RESOLVED_CACHE = new WeakHashMap<>();

    private static Map<String, ICapabilityProvider> getOrAttach(Object owner) {
        Map<String, ICapabilityProvider> map = STORE.get(owner);
        if (map == null) {
            map = new java.util.HashMap<>();
            STORE.put(owner, map);
            AttachCapabilitiesEvent<Object> event = new AttachCapabilitiesEvent<>(owner);
            com.robertx22.forge_compat.common.MinecraftForge.EVENT_BUS.post(event);
            map.putAll(event.getProviders());
        }
        return map;
    }

    public static synchronized <T> LazyOptional<T> get(Object owner, Capability<T> cap, Direction side) {
        if (owner instanceof ICapabilityProvider && !(owner instanceof net.minecraft.world.entity.Entity) && !(owner instanceof net.minecraft.world.level.Level) && !(owner instanceof net.minecraft.world.item.ItemStack)) {
            return ((ICapabilityProvider) owner).getCapability(cap, side);
        }
        return get(owner, cap);
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T> LazyOptional<T> get(Object owner, Capability<T> cap) {
        if (owner == null || cap == null) return LazyOptional.empty();
        Map<Capability<?>, LazyOptional<?>> cached = RESOLVED_CACHE.get(owner);
        if (cached != null) {
            LazyOptional<?> hit = cached.get(cap);
            if (hit != null) {
                return (LazyOptional<T>) hit;
            }
        }
        Map<String, ICapabilityProvider> map = getOrAttach(owner);
        LazyOptional<T> result = LazyOptional.empty();
        for (ICapabilityProvider p : map.values()) {
            LazyOptional<T> r = p.getCapability(cap, null);
            if (r.isPresent()) {
                result = r;
                break;
            }
        }
        if (cached == null) {
            cached = new java.util.IdentityHashMap<>();
            RESOLVED_CACHE.put(owner, cached);
        }
        cached.put(cap, result);
        return result;
    }

    public static synchronized void saveAll(Object owner, CompoundTag tag) {
        if (owner == null || tag == null) return;
        Map<String, ICapabilityProvider> map = getOrAttach(owner);
        for (Map.Entry<String, ICapabilityProvider> e : map.entrySet()) {
            if (e.getValue() instanceof ICapabilitySerializable) {
                tag.put(e.getKey(), ((ICapabilitySerializable<?>) e.getValue()).serializeNBT());
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static synchronized void loadAll(Object owner, CompoundTag tag) {
        if (owner == null || tag == null) return;
        Map<String, ICapabilityProvider> map = getOrAttach(owner);
        for (Map.Entry<String, ICapabilityProvider> e : map.entrySet()) {
            if (e.getValue() instanceof ICapabilitySerializable && tag.contains(e.getKey())) {
                ((ICapabilitySerializable<Tag>) e.getValue()).deserializeNBT(tag.get(e.getKey()));
            }
        }
    }
}
