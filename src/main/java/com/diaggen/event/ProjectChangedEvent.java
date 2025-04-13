package com.diaggen.event;

public class ProjectChangedEvent extends DiagramEvent {
    private final ChangeType changeType;
    private final String elementId;
    public ProjectChangedEvent(String projectId, ChangeType changeType, String elementId) {
        super(projectId);
        this.changeType = changeType;
        this.elementId = elementId;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public String getElementId() {
        return elementId;
    }

    public enum ChangeType {
        PROJECT_CREATED, PROJECT_RENAMED, PROJECT_DELETED, PROJECT_ACTIVATED,
        DIAGRAMS_IMPORTED, DIAGRAMS_EXPORTED
    }
}
