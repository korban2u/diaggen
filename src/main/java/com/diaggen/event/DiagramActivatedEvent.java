package com.diaggen.event;

/**
 * Événement spécifique indiquant qu'un diagramme est devenu le diagramme actif.
 * Cet événement doit être écouté par les composants d'interface qui ont besoin
 * de mettre à jour leur état quand on change de diagramme actif.
 */
public class DiagramActivatedEvent extends DiagramEvent {

    public DiagramActivatedEvent(String diagramId) {
        super(diagramId);
    }
}