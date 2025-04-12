package com.diaggen.controller;

import com.diaggen.controller.command.CommandManager;
import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramClass;
import com.diaggen.model.DiagramRelation;
import com.diaggen.model.DiagramStore;
import com.diaggen.model.RelationType;
import com.diaggen.view.diagram.DiagramCanvas;

public class MainController {

    private final DiagramStore diagramStore;
    private final CommandManager commandManager;
    private final ClassController classController;
    private final RelationController relationController;
    private final DiagramController diagramController;
    private final ExportController exportController;
    private final DiagramCanvas diagramCanvas;

    public MainController(DiagramStore diagramStore,
                          CommandManager commandManager,
                          ClassController classController,
                          RelationController relationController,
                          DiagramController diagramController,
                          ExportController exportController,
                          DiagramCanvas diagramCanvas) {
        this.diagramStore = diagramStore;
        this.commandManager = commandManager;
        this.classController = classController;
        this.relationController = relationController;
        this.diagramController = diagramController;
        this.exportController = exportController;
        this.diagramCanvas = diagramCanvas;
    }

    public void handleNewDiagram() {

        diagramController.createNewDiagramWithDialog();
    }

    public void handleSelectDiagram(ClassDiagram diagram) {
        diagramController.activateDiagram(diagram);
    }

    public void handleEditDiagram() {
        if (diagramStore.getActiveDiagram() != null) {

            diagramController.renameDiagram(diagramStore.getActiveDiagram(), null);
        }
    }

    public void handleDeleteDiagram() {
        if (diagramStore.getActiveDiagram() != null) {
            diagramController.deleteDiagram(diagramStore.getActiveDiagram());
        }
    }

    public void handleSave() {
        diagramController.saveDiagram();
    }

    public void handleSaveAs() {
        diagramController.saveAsDiagram();
    }

    public void handleOpen() {
        diagramController.openDiagram();
    }

    public void handleAddClass() {
        classController.createClass();
    }

    public void handleDeleteClass() {

        DiagramClass selectedClass = diagramCanvas.getSelectedClass();
        if (selectedClass != null) {
            classController.removeClass(selectedClass);
        }
    }

    public void handleEditClass(DiagramClass diagramClass) {
        classController.editClass(diagramClass);
    }

    public void handleMoveClass(DiagramClass diagramClass, double oldX, double oldY, double newX, double newY) {
        classController.moveClass(diagramClass, oldX, oldY, newX, newY);
    }

    public void handleAddRelation() {
        relationController.createRelation();
    }

    public void handleDeleteRelation() {

        DiagramRelation selectedRelation = diagramCanvas.getSelectedRelation();
        if (selectedRelation != null) {
            relationController.removeRelation(selectedRelation);
        }
    }

    public void handleEditRelation(DiagramRelation relation) {
        relationController.editRelation(relation);
    }

    public void changeRelationType(DiagramRelation relation, RelationType newType) {
        relationController.changeRelationType(relation, newType);
    }

    public void handleExportImage() {
        exportController.exportToPNG();
    }

    public void handleExportSVG() {
        exportController.exportToSVG();
    }

    public void handleExportPlantUML() {
        exportController.exportToPlantUML();
    }

    public void handleExportJavaCode() {
        exportController.exportToJavaCode();
    }

    public void handleImportJavaCode() {
        exportController.importJavaCode();
    }

    public void handleUndo() {
        if (commandManager.canUndo()) {
            commandManager.undo();
        }
    }

    public void handleRedo() {
        if (commandManager.canRedo()) {
            commandManager.redo();
        }
    }

    public DiagramStore getDiagramStore() {
        return diagramStore;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }
}