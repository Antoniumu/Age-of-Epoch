package com.robertx22.forge_compat.fml.event.lifecycle;

import com.robertx22.forge_compat.eventbus.api.Event;

public class FMLCommonSetupEvent extends Event implements IModBusEvent {
    public FMLCommonSetupEvent() {
    }

    public void enqueueWork(Runnable runnable) {
        runnable.run();
    }
}
