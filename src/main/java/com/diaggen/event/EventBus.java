package com.diaggen.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {

    private static EventBus instance;

    private final Map<Class<? extends DiagramEvent>, CopyOnWriteArrayList<EventListener<? extends DiagramEvent>>> listeners;
    private final ThreadLocal<Boolean> isPublishing = new ThreadLocal<>();

    private EventBus() {
        listeners = new ConcurrentHashMap<>();
        isPublishing.set(false);
    }

    public static synchronized EventBus getInstance() {
        if (instance == null) {
            instance = new EventBus();
        }
        return instance;
    }

    public <T extends DiagramEvent> void subscribe(Class<T> eventType, EventListener<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(listener);
    }

    public <T extends DiagramEvent> void unsubscribe(Class<T> eventType, EventListener<T> listener) {
        if (listeners.containsKey(eventType)) {
            listeners.get(eventType).remove(listener);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends DiagramEvent> void publish(T event) {
        if (Boolean.TRUE.equals(isPublishing.get())) {
            return;
        }

        try {
            isPublishing.set(true);

            Class<? extends DiagramEvent> eventType = event.getClass();
            List<EventListener<?>> eventListeners = new ArrayList<>();
            if (listeners.containsKey(eventType)) {
                eventListeners.addAll(listeners.get(eventType));
            }
            for (Map.Entry<Class<? extends DiagramEvent>, CopyOnWriteArrayList<EventListener<? extends DiagramEvent>>> entry : listeners.entrySet()) {
                if (entry.getKey().isAssignableFrom(eventType) && !entry.getKey().equals(eventType)) {
                    eventListeners.addAll(entry.getValue());
                }
            }
            for (EventListener<?> listener : eventListeners) {
                ((EventListener<T>) listener).onEvent(event);
            }
        } finally {
            isPublishing.set(false);
        }
    }
}