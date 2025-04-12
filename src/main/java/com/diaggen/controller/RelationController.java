package com.diaggen.controller;

import com.diaggen.controller.command.AddRelationCommand;
import com.diaggen.controller.command.ChangeRelationTypeCommand;
import com.diaggen.controller.command.CommandManager;
import com.diaggen.controller.command.RemoveRelationCommand;
import com.diaggen.event.DiagramChangedEvent;
import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramRelation;
import com.diaggen.model.DiagramStore;
import com.diaggen.model.RelationType;
import com.diaggen.util.AlertHelper;
import com.diaggen.view.diagram.DiagramCanvas;
import com.diaggen.view.dialog.DialogFactory;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public class RelationController extends BaseController {

    private final DialogFactory dialogFactory;
    private final DiagramCanvas diagramCanvas;

    public RelationController(DiagramStore diagramStore, CommandManager commandManager,
                              DialogFactory dialogFactory, DiagramCanvas diagramCanvas) {
        super(diagramStore, commandManager);
        this.dialogFactory = dialogFactory;
        this.diagramCanvas = diagramCanvas;
    }

    public void createRelation() {
        ClassDiagram currentDiagram = getActiveDiagram();
        if (currentDiagram == null) return;

        var classes = currentDiagram.getClasses();
        if (classes.size() < 2) {
            AlertHelper.showWarning("Impossible d'ajouter une relation",
                    "Vous devez avoir au moins deux classes pour créer une relation.");
            return;
        }

        var dialog = dialogFactory.createRelationEditorDialog(null, classes);
        dialog.showAndWait().ifPresent(relation -> {
            AddRelationCommand command = new AddRelationCommand(currentDiagram, relation);
            commandManager.executeCommand(command);

            eventBus.publish(new DiagramChangedEvent(currentDiagram.getId(),
                    DiagramChangedEvent.ChangeType.RELATION_ADDED, relation.getId()));
        });
    }

    public void removeRelation(DiagramRelation relation) {
        ClassDiagram currentDiagram = getActiveDiagram();
        if (currentDiagram == null || relation == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer la relation");
        alert.setHeaderText("Supprimer la relation");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cette relation ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            RemoveRelationCommand command = new RemoveRelationCommand(currentDiagram, relation);
            commandManager.executeCommand(command);

            eventBus.publish(new DiagramChangedEvent(currentDiagram.getId(),
                    DiagramChangedEvent.ChangeType.RELATION_REMOVED, relation.getId()));
        }
    }

    public void changeRelationType(DiagramRelation relation, RelationType newType) {
        ClassDiagram currentDiagram = getActiveDiagram();
        if (currentDiagram == null || relation == null || relation.getRelationType() == newType) return;

        ChangeRelationTypeCommand command = new ChangeRelationTypeCommand(
                currentDiagram, relation, newType, diagramCanvas);
        commandManager.executeCommand(command);

        eventBus.publish(new DiagramChangedEvent(currentDiagram.getId(),
                DiagramChangedEvent.ChangeType.RELATION_MODIFIED, relation.getId()));
    }

    public void editRelation(DiagramRelation relation) {
        ClassDiagram currentDiagram = getActiveDiagram();
        if (currentDiagram == null || relation == null) return;

        var dialog = dialogFactory.createRelationEditorDialog(relation, currentDiagram.getClasses());
        dialog.showAndWait().ifPresent(updatedRelation -> {
            // Si le type a changé, utiliser ChangeRelationTypeCommand
            if (relation.getRelationType() != updatedRelation.getRelationType()) {
                changeRelationType(relation, updatedRelation.getRelationType());
            }

            eventBus.publish(new DiagramChangedEvent(currentDiagram.getId(),
                    DiagramChangedEvent.ChangeType.RELATION_MODIFIED, relation.getId()));
        });
    }
}
