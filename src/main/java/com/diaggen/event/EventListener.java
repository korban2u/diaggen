package com.diaggen.event;

/**
 * Interface définissant un écouteur d'événements.
 */
public interface EventListener<T extends DiagramEvent> {
    void onEvent(T event);
}
