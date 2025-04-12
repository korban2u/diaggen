package com.diaggen.event;


public abstract class DiagramEvent {
    private final String diagramId;

    public DiagramEvent(String diagramId) {
        this.diagramId = diagramId;
    }

    public String getDiagramId() {
        return diagramId;
    }
}
