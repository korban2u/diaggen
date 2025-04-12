package com.diaggen.event;

/**
 * Événement signalant une sélection d'élément dans le diagramme.
 */
public class ElementSelectedEvent extends DiagramEvent {
    private final String elementId;
    private final boolean isClass; // true si c'est une classe, false si c'est une relation

    public ElementSelectedEvent(String diagramId, String elementId, boolean isClass) {
        super(diagramId);
        this.elementId = elementId;
        this.isClass = isClass;
    }

    public String getElementId() {
        return elementId;
    }

    public boolean isClass() {
        return isClass;
    }
}
