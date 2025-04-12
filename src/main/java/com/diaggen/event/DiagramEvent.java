package com.diaggen.event;

/**
 * Classe de base pour tous les événements du diagramme.
 */
public abstract class DiagramEvent {
    private final String diagramId;

    public DiagramEvent(String diagramId) {
        this.diagramId = diagramId;
    }

    public String getDiagramId() {
        return diagramId;
    }
}
