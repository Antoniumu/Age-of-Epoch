package com.robertx22.forge_compat.eventbus.api;

import java.util.function.Consumer;

public interface IEventBus {
    <T extends Event> void addListener(Consumer<T> consumer);

    <T extends Event> void addListener(EventPriority priority, Consumer<T> consumer);

    default <T extends Event> void addListener(EventPriority priority, Class<T> eventType, Consumer<T> consumer) {
        addListener(priority, consumer);
    }

    void register(Object target);

    boolean post(Event event);

    default <T extends Event, F> void addGenericListener(Class<F> filter, java.util.function.Consumer<T> consumer) {
        addListener(consumer);
    }
}
