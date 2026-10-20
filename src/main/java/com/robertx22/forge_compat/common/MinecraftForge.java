package com.robertx22.forge_compat.common;

import com.robertx22.forge_compat.eventbus.api.EventBusImpl;
import com.robertx22.forge_compat.eventbus.api.IEventBus;

public class MinecraftForge {
    public static final IEventBus EVENT_BUS = new EventBusImpl();
}
