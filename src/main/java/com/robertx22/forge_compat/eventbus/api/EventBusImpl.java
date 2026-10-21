package com.robertx22.forge_compat.eventbus.api;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class EventBusImpl implements IEventBus {

    private static final class Handler {
        final EventPriority priority;
        final Consumer<Event> consumer;
        final Class<?> eventType;
        final boolean receiveCanceled;

        Handler(EventPriority priority, Class<?> eventType, Consumer<Event> consumer, boolean receiveCanceled) {
            this.priority = priority;
            this.eventType = eventType;
            this.consumer = consumer;
            this.receiveCanceled = receiveCanceled;
        }
    }

    private final List<Handler> handlers = new CopyOnWriteArrayList<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Event> void addListener(Consumer<T> consumer) {
        addListener(EventPriority.NORMAL, consumer);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Event> void addListener(EventPriority priority, Consumer<T> consumer) {
        handlers.add(new Handler(priority, Event.class, (Event e) -> {
            try {
                consumer.accept((T) e);
            } catch (ClassCastException ignored) {
                // listener is for a different Event subtype
            }
        }, false));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Event> void addListener(EventPriority priority, Class<T> eventType, Consumer<T> consumer) {
        Class<?> targetType = eventType != null ? eventType : Event.class;
        handlers.add(new Handler(priority, targetType, (Event e) -> {
            try {
                consumer.accept((T) e);
            } catch (ClassCastException ignored) {
            }
        }, false));
    }

    @Override
    public void register(Object target) {
        Class<?> clazz = target instanceof Class ? (Class<?>) target : target.getClass();
        for (Method m : clazz.getDeclaredMethods()) {
            SubscribeEvent ann = m.getAnnotation(SubscribeEvent.class);
            if (ann == null) continue;
            if (m.getParameterCount() != 1) continue;
            Class<?> eventType = m.getParameterTypes()[0];
            if (!Event.class.isAssignableFrom(eventType)) continue;
            java.lang.reflect.Type genericParam = m.getGenericParameterTypes()[0];
            final Class<?> genericFilter;
            if (genericParam instanceof java.lang.reflect.ParameterizedType pt && pt.getActualTypeArguments().length == 1 && pt.getActualTypeArguments()[0] instanceof Class<?> gc) {
                genericFilter = gc;
            } else {
                genericFilter = null;
            }
            m.setAccessible(true);
            boolean isStatic = java.lang.reflect.Modifier.isStatic(m.getModifiers());
            Object instance = isStatic ? null : target;
            handlers.add(new Handler(ann.priority(), eventType, (Event e) -> {
                try {
                    if (genericFilter != null && e instanceof com.robertx22.forge_compat.event.AttachCapabilitiesEvent<?> ace) {
                        if (ace.getObject() != null && !genericFilter.isInstance(ace.getObject())) {
                            return;
                        }
                    }
                    m.invoke(instance, e);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }, ann.receiveCanceled()));
        }
    }

    @Override
    public boolean post(Event event) {
        List<Handler> sorted = new ArrayList<>(handlers);
        sorted.sort(Comparator.comparingInt(h -> h.priority.ordinal()));
        for (Handler h : sorted) {
            if (h.eventType.isInstance(event)) {
                if (event.isCanceled() && !h.receiveCanceled) continue;
                h.consumer.accept(event);
            }
        }
        return event.isCanceled();
    }
}
