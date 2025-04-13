package com.diaggen.controller;

import com.diaggen.controller.command.CommandManager;
import com.diaggen.event.DiagramChangedEvent;
import com.diaggen.layout.LayoutFactory;
import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramStore;
import com.diaggen.service.LayoutService;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contrôleur pour gérer les opérations de disposition dans les diagrammes de classe.
 * Ce contrôleur agit comme une façade entre la vue et le service de layout.
 */
public class LayoutController extends BaseController {

    private static final Logger LOGGER = Logger.getLogger(LayoutController.class.getName());
    private final LayoutService layoutService;

    public LayoutController(DiagramStore diagramStore, CommandManager commandManager, LayoutService layoutService) {
        super(diagramStore, commandManager);
        this.layoutService = layoutService;
    }

    /**
     * Ouvre une boîte de dialogue permettant à l'utilisateur de choisir un algorithme d'arrangement
     * et l'applique au diagramme actif.
     */
    public void arrangeClasses() {
        ClassDiagram diagram = getActiveDiagram();
        if (diagram == null || diagram.getClasses().isEmpty()) {
            return;
        }

        LOGGER.log(Level.INFO, "Starting class arrangement dialog");

        // Déléguer au service
        layoutService.arrangeClasses(diagram, commandManager);

        // Notifier les changements
        eventBus.publish(new DiagramChangedEvent(diagram.getId(),
                DiagramChangedEvent.ChangeType.DIAGRAM_RENAMED, null));
    }

    /**
     * Applique directement un type de layout spécifique au diagramme actif.
     */
    public void applyLayout(ClassDiagram diagram, LayoutFactory.LayoutType layoutType) {
        if (diagram == null || diagram.getClasses().isEmpty()) {
            return;
        }

        LOGGER.log(Level.INFO, "Applying {0} layout directly", layoutType);

        // Appliquer le layout avec génération de commandes
        layoutService.applyLayoutWithCommands(diagram, layoutType, commandManager);

        // Notifier les changements
        eventBus.publish(new DiagramChangedEvent(diagram.getId(),
                DiagramChangedEvent.ChangeType.DIAGRAM_RENAMED, null));
    }

    /**
     * Applique un layout sans génération de commandes (pour initialisation)
     */
    public void applyLayoutWithoutCommands(ClassDiagram diagram, LayoutFactory.LayoutType layoutType) {
        if (diagram == null || diagram.getClasses().isEmpty()) {
            return;
        }

        LOGGER.log(Level.INFO, "Applying {0} layout without commands", layoutType);

        // Appliquer le layout directement
        layoutService.applyLayout(diagram, layoutType);

        // Notifier les changements
        eventBus.publish(new DiagramChangedEvent(diagram.getId(),
                DiagramChangedEvent.ChangeType.DIAGRAM_RENAMED, null));
    }
}