package com.diaggen.controller;

import com.diaggen.controller.command.CommandManager;
import com.diaggen.event.DiagramChangedEvent;
import com.diaggen.layout.LayoutFactory;
import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramStore;
import com.diaggen.service.LayoutService;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LayoutController extends BaseController {

    private static final Logger LOGGER = Logger.getLogger(LayoutController.class.getName());
    private final LayoutService layoutService;

    public LayoutController(DiagramStore diagramStore, CommandManager commandManager, LayoutService layoutService) {
        super(diagramStore, commandManager);
        this.layoutService = layoutService;
    }

        public void arrangeClasses() {
        ClassDiagram diagram = getActiveDiagram();
        if (diagram == null || diagram.getClasses().isEmpty()) {
            return;
        }

        LOGGER.log(Level.INFO, "Starting class arrangement dialog");
        layoutService.arrangeClasses(diagram, commandManager);
        eventBus.publish(new DiagramChangedEvent(diagram.getId(),
                DiagramChangedEvent.ChangeType.DIAGRAM_RENAMED, null));
    }

        public void applyLayout(ClassDiagram diagram, LayoutFactory.LayoutType layoutType) {
        if (diagram == null || diagram.getClasses().isEmpty()) {
            return;
        }

        LOGGER.log(Level.INFO, "Applying {0} layout directly", layoutType);
        layoutService.applyLayoutWithCommands(diagram, layoutType, commandManager);
        eventBus.publish(new DiagramChangedEvent(diagram.getId(),
                DiagramChangedEvent.ChangeType.DIAGRAM_RENAMED, null));
    }

        public void applyLayoutWithoutCommands(ClassDiagram diagram, LayoutFactory.LayoutType layoutType) {
        if (diagram == null || diagram.getClasses().isEmpty()) {
            return;
        }

        LOGGER.log(Level.INFO, "Applying {0} layout without commands", layoutType);
        layoutService.applyLayout(diagram, layoutType);
        eventBus.publish(new DiagramChangedEvent(diagram.getId(),
                DiagramChangedEvent.ChangeType.DIAGRAM_RENAMED, null));
    }
}