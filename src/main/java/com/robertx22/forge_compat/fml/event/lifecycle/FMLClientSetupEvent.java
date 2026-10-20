package com.robertx22.forge_compat.fml.event.lifecycle;

import com.robertx22.forge_compat.eventbus.api.Event;

public class FMLClientSetupEvent extends Event implements IModBusEvent {
    public FMLClientSetupEvent() {
    }

    public void enqueueWork(Runnable runnable) {
        runnable.run();
    }
}
