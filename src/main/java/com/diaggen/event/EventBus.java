package com.diaggen.event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bus d'événements central permettant le découplage des composants de l'application.
 * Implémente le pattern Observateur de manière générique.
 */
public class EventBus {

    private static EventBus instance;

    private final Map<Class<? extends DiagramEvent>, CopyOnWriteArrayList<EventListener<? extends DiagramEvent>>> listeners;

    private EventBus() {
        listeners = new ConcurrentHashMap<>();
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
        Class<? extends DiagramEvent> eventType = event.getClass();

        // Notifier les écouteurs qui s'abonnent exactement à ce type
        if (listeners.containsKey(eventType)) {
            for (EventListener<?> listener : listeners.get(eventType)) {
                ((EventListener<T>) listener).onEvent(event);
            }
        }

        // Notifier les écouteurs qui s'abonnent à des super-types
        for (Map.Entry<Class<? extends DiagramEvent>, CopyOnWriteArrayList<EventListener<? extends DiagramEvent>>> entry : listeners.entrySet()) {
            if (entry.getKey().isAssignableFrom(eventType) && !entry.getKey().equals(eventType)) {
                for (EventListener<?> listener : entry.getValue()) {
                    ((EventListener<T>) listener).onEvent(event);
                }
            }
        }
    }
}

