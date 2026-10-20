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

    public static synchronized <T> LazyOptional<T> get(Object owner, Capability<T> cap, Direction side) {
        if (owner instanceof ICapabilityProvider && !(owner instanceof net.minecraft.world.entity.Entity) && !(owner instanceof net.minecraft.world.level.Level) && !(owner instanceof net.minecraft.world.item.ItemStack)) {
            return ((ICapabilityProvider) owner).getCapability(cap, side);
        }
        return get(owner, cap);
    }

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
            LazyOptional<T> r = p.getCapability(cap, null);
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
                ((ICapabilitySerializable<Tag>) e.getValue()).deserializeNBT(tag.get(e.getKey()));
            }
        }
    }
}
