package com.robertx22.library_of_exile.main;

import com.robertx22.forge_compat.eventbus.api.Event;
import com.robertx22.forge_compat.eventbus.api.EventPriority;
import java.util.function.Consumer;

public class ForgeEvents {
    public static <T extends Event> void registerForgeEvent(Class<T> clazz, Consumer<T> event) {
        ApiForgeEvents.registerForgeEvent(clazz, event);
    }

    public static <T extends Event> void registerForgeEvent(Class<T> clazz, Consumer<T> event, EventPriority priority) {
        ApiForgeEvents.registerForgeEvent(clazz, event, priority);
    }
}
