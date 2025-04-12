package com.diaggen.controller;

import com.diaggen.controller.command.CommandManager;
import com.diaggen.event.DiagramChangedEvent;
import com.diaggen.event.EventBus;
import com.diaggen.model.*;
import com.diaggen.view.diagram.DiagramCanvas;
import java.util.Optional;


public abstract class BaseController {
    protected final DiagramStore diagramStore;
    protected final CommandManager commandManager;
    protected final EventBus eventBus;

    protected BaseController(DiagramStore diagramStore, CommandManager commandManager) {
        this.diagramStore = diagramStore;
        this.commandManager = commandManager;
        this.eventBus = EventBus.getInstance();
    }

    protected void refreshDiagram() {
        String diagramId = diagramStore.getActiveDiagram() != null ?
                diagramStore.getActiveDiagram().getId() : null;

        if (diagramId != null) {
            eventBus.publish(new DiagramChangedEvent(diagramId,
                    DiagramChangedEvent.ChangeType.DIAGRAM_RENAMED, null));
        }
    }

    protected ClassDiagram getActiveDiagram() {
        return diagramStore.getActiveDiagram();
    }

    protected Optional<DiagramClass> findClassById(String id) {
        return diagramStore.findClassById(id);
    }

    protected Optional<DiagramRelation> findRelationById(String id) {
        return diagramStore.findRelationById(id);
    }
}
