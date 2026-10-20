package com.robertx22.forge_compat.fml.javafmlmod;

import com.robertx22.forge_compat.eventbus.api.EventBusImpl;
import com.robertx22.forge_compat.eventbus.api.IEventBus;

public class FMLJavaModLoadingContext {

    private static final FMLJavaModLoadingContext INSTANCE = new FMLJavaModLoadingContext();
    private final IEventBus modBus = new EventBusImpl();

    public static FMLJavaModLoadingContext get() { return INSTANCE; }

    public IEventBus getModEventBus() { return modBus; }
}
