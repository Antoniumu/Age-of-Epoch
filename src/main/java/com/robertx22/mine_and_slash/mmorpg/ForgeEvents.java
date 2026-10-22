package com.robertx22.mine_and_slash.mmorpg;

import com.robertx22.forge_compat.common.MinecraftForge;
import com.robertx22.forge_compat.eventbus.api.Event;
import com.robertx22.forge_compat.eventbus.api.EventPriority;
import com.robertx22.forge_compat.fml.event.lifecycle.IModBusEvent;
import com.robertx22.forge_compat.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.function.Consumer;

public class ForgeEvents {
    public static <T extends Event> void registerForgeEvent(Class<T> clazz, Consumer<T> event, EventPriority priority) {
        if (IModBusEvent.class.isAssignableFrom(clazz) || clazz.isAssignableFrom(IModBusEvent.class)) {
            FMLJavaModLoadingContext.get()
                    .getModEventBus()
                    .addListener(priority, event);
        } else {
            MinecraftForge.EVENT_BUS.addListener(priority, event);
        }
    }

    public static <T extends Event> void registerForgeEvent(Class<T> clazz, Consumer<T> event) {
        registerForgeEvent(clazz, event, EventPriority.NORMAL);
    }

}
