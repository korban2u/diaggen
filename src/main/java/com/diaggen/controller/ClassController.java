package com.diaggen.controller;

import com.diaggen.controller.command.AddClassCommand;
import com.diaggen.controller.command.CommandManager;
import com.diaggen.controller.command.MoveClassCommand;
import com.diaggen.controller.command.RemoveClassCommand;
import com.diaggen.event.ClassMovedEvent;
import com.diaggen.event.DiagramChangedEvent;
import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramClass;
import com.diaggen.model.DiagramStore;
import com.diaggen.service.LayoutService;
import com.diaggen.view.dialog.DialogFactory;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClassController extends BaseController {

    private static final Logger LOGGER = Logger.getLogger(ClassController.class.getName());
    private final DialogFactory dialogFactory;
    private LayoutService layoutService;

    public ClassController(DiagramStore diagramStore, CommandManager commandManager, DialogFactory dialogFactory) {
        super(diagramStore, commandManager);
        this.dialogFactory = dialogFactory;
    }
    public void setLayoutService(LayoutService layoutService) {
        this.layoutService = layoutService;
    }

    public void createClass() {
        ClassDiagram currentDiagram = getActiveDiagram();
        if (currentDiagram == null) return;

        var dialog = dialogFactory.createClassEditorDialog(null);
        dialog.showAndWait().ifPresent(diagramClass -> {
            AddClassCommand command = new AddClassCommand(currentDiagram, diagramClass);
            commandManager.executeCommand(command);

            eventBus.publish(new DiagramChangedEvent(currentDiagram.getId(),
                    DiagramChangedEvent.ChangeType.CLASS_ADDED, diagramClass.getId()));
        });
    }

    public void editClass(DiagramClass diagramClass) {
        if (diagramClass == null) return;

        var dialog = dialogFactory.createClassEditorDialog(diagramClass);
        dialog.showAndWait().ifPresent(updatedClass -> {
            eventBus.publish(new DiagramChangedEvent(getActiveDiagram().getId(),
                    DiagramChangedEvent.ChangeType.CLASS_MODIFIED, diagramClass.getId()));
        });
    }

    public void removeClass(DiagramClass diagramClass) {
        ClassDiagram currentDiagram = getActiveDiagram();
        if (currentDiagram == null || diagramClass == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer la classe");
        alert.setHeaderText("Supprimer \"" + diagramClass.getName() + "\"");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cette classe ? " +
                "Cette action supprimera également toutes les relations associées.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            RemoveClassCommand command = new RemoveClassCommand(currentDiagram, diagramClass);
            commandManager.executeCommand(command);

            eventBus.publish(new DiagramChangedEvent(currentDiagram.getId(),
                    DiagramChangedEvent.ChangeType.CLASS_REMOVED, diagramClass.getId()));
        }
    }

    public void moveClass(DiagramClass diagramClass, double oldX, double oldY, double newX, double newY) {
        ClassDiagram currentDiagram = getActiveDiagram();
        if (currentDiagram == null || diagramClass == null) return;

        MoveClassCommand command = new MoveClassCommand(diagramClass, oldX, oldY, newX, newY);
        commandManager.executeCommand(command);

        eventBus.publish(new ClassMovedEvent(currentDiagram.getId(),
                diagramClass.getId(), oldX, oldY, newX, newY));
    }

    public void arrangeClassesAutomatically() {
        ClassDiagram diagram = getActiveDiagram();
        if (diagram == null || diagram.getClasses().isEmpty()) return;

        LOGGER.log(Level.INFO, "Starting automatic class arrangement");
        if (layoutService != null) {
            layoutService.arrangeClasses(diagram, commandManager);
            eventBus.publish(new DiagramChangedEvent(diagram.getId(),
                    DiagramChangedEvent.ChangeType.DIAGRAM_RENAMED, null));

            return;
        }
        final int GRID_WIDTH = 250;
        final int GRID_HEIGHT = 200;
        final int MAX_COLUMNS = 4;

        int row = 0;
        int col = 0;

        commandManager.startCommandGroup("Arrangement automatique des classes");

        for (DiagramClass diagramClass : diagram.getClasses()) {
            double oldX = diagramClass.getX();
            double oldY = diagramClass.getY();
            double newX = 50 + col * GRID_WIDTH;
            double newY = 50 + row * GRID_HEIGHT;

            MoveClassCommand command = new MoveClassCommand(diagramClass, oldX, oldY, newX, newY);
            commandManager.executeCommand(command);

            col++;
            if (col >= MAX_COLUMNS) {
                col = 0;
                row++;
            }
        }

        commandManager.endCommandGroup();

        eventBus.publish(new DiagramChangedEvent(diagram.getId(),
                DiagramChangedEvent.ChangeType.DIAGRAM_RENAMED, null));

        LOGGER.log(Level.INFO, "Automatic class arrangement completed");
    }
}