package com.robertx22.forge_compat.event;

import com.robertx22.forge_compat.common.capabilities.ICapabilityProvider;
import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public class AttachCapabilitiesEvent<T> extends Event {

    private final T object;
    private final Map<String, ICapabilityProvider> providers = new LinkedHashMap<>();

    public AttachCapabilitiesEvent(T object) {
        this.object = object;
    }

    public T getObject() { return object; }

    public void addCapability(Identifier key, ICapabilityProvider provider) {
        providers.put(key.toString(), provider);
    }

    public Map<String, ICapabilityProvider> getProviders() { return providers; }
}
