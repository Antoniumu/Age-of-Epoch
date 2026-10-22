package com.robertx22.forge_compat.common.capabilities;

import com.robertx22.forge_compat.eventbus.api.Event;
import com.robertx22.forge_compat.fml.event.lifecycle.IModBusEvent;

public class RegisterCapabilitiesEvent extends Event implements IModBusEvent {
    public <T> void register(Class<T> type) {
        CapabilityManager.INSTANCE.register(type, null, () -> null);
    }
}
