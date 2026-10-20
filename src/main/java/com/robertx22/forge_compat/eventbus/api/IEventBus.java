package com.robertx22.forge_compat.eventbus.api;

import java.util.function.Consumer;

public interface IEventBus {
    <T extends Event> void addListener(Consumer<T> consumer);

    <T extends Event> void addListener(EventPriority priority, Consumer<T> consumer);

    void register(Object target);

    boolean post(Event event);
}
