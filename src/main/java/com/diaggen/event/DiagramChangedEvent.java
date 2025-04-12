package com.diaggen.event;

/**
 * Événement signalant un changement dans le diagramme.
 */
public class DiagramChangedEvent extends DiagramEvent {
    public enum ChangeType {
        CLASS_ADDED, CLASS_MODIFIED, CLASS_REMOVED, CLASS_SELECTED,
        RELATION_ADDED, RELATION_MODIFIED, RELATION_REMOVED, RELATION_SELECTED,
        DIAGRAM_RENAMED, DIAGRAM_CLEARED
    }

    private final ChangeType changeType;
    private final String elementId;

    public DiagramChangedEvent(String diagramId, ChangeType changeType, String elementId) {
        super(diagramId);
        this.changeType = changeType;
        this.elementId = elementId;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public String getElementId() {
        return elementId;
    }
}