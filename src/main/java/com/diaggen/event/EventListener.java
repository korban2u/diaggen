package com.diaggen.event;


public interface EventListener<T extends DiagramEvent> {
    void onEvent(T event);
}
