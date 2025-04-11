package com.diaggen.view.diagram;

import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramClass;
import com.diaggen.model.DiagramRelation;
import com.diaggen.view.diagram.canvas.ClassNode;
import com.diaggen.view.diagram.canvas.GridRenderer;
import com.diaggen.view.diagram.canvas.NodeManager;
import com.diaggen.view.diagram.canvas.RelationLine;
import com.diaggen.view.diagram.canvas.RelationManager;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Pane;


public class DiagramCanvas extends Pane {

    private ClassDiagram diagram;
    private final Canvas gridCanvas;
    private final GridRenderer gridRenderer;
    private final NodeManager nodeManager;
    private final RelationManager relationManager;

    private Runnable onAddClassRequest;

    public DiagramCanvas() {
        getStyleClass().add("diagram-canvas");
        setStyle("-fx-background-color: white;");

        gridCanvas = new Canvas();
        gridCanvas.widthProperty().bind(widthProperty());
        gridCanvas.heightProperty().bind(heightProperty());
        getChildren().add(gridCanvas);

        gridRenderer = new GridRenderer(gridCanvas, 20);
        nodeManager = new NodeManager(this);
        relationManager = new RelationManager(this, nodeManager);

        nodeManager.setRelationManager(relationManager);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem addClassItem = new MenuItem("Ajouter une classe");
        addClassItem.setOnAction(e -> {
            if (onAddClassRequest != null) {
                onAddClassRequest.run();
            }
        });
        contextMenu.getItems().add(addClassItem);

        setOnContextMenuRequested(e -> {
            contextMenu.show(this, e.getScreenX(), e.getScreenY());
        });

        widthProperty().addListener((obs, oldVal, newVal) -> gridRenderer.drawGrid());
        heightProperty().addListener((obs, oldVal, newVal) -> gridRenderer.drawGrid());
        gridRenderer.drawGrid();

        nodeManager.setNodeSelectionListener(node -> {
            relationManager.selectRelation(null);
        });

        relationManager.setRelationSelectionListener(line -> {
            nodeManager.selectNode(null);
        });
    }

    public void loadDiagram(ClassDiagram diagram) {
        this.diagram = diagram;

        clear();

        for (DiagramClass diagramClass : diagram.getClasses()) {
            nodeManager.createClassNode(diagramClass);
        }

        for (DiagramRelation relation : diagram.getRelations()) {
            relationManager.createRelationLine(relation);
        }

        relationManager.updateAllRelationsLater();
    }

    /**
     * Rafraîchit l'affichage du diagramme
     */
    public void refresh() {
        if (diagram != null) {
            clear();

            for (DiagramClass diagramClass : diagram.getClasses()) {
                nodeManager.createClassNode(diagramClass);
            }

            for (DiagramRelation relation : diagram.getRelations()) {
                relationManager.createRelationLine(relation);
            }

            relationManager.updateAllRelationsLater();
        }
    }

    public void clear() {
        nodeManager.clear();
        relationManager.clear();
    }

    public DiagramClass getSelectedClass() {
        return nodeManager.getSelectedClass();
    }

    public DiagramRelation getSelectedRelation() {
        return relationManager.getSelectedRelation();
    }

    public void setOnAddClassRequest(Runnable handler) {
        this.onAddClassRequest = handler;
    }

    public void selectClass(DiagramClass diagramClass) {
        if (diagramClass == null) {
            nodeManager.selectNode(null);
            return;
        }

        ClassNode node = nodeManager.getNodeById(diagramClass.getId());
        if (node != null) {
            nodeManager.selectNode(node);
        }
    }

    public void selectRelation(DiagramRelation relation) {
        if (relation == null) {
            relationManager.selectRelation(null);
            return;
        }

        RelationLine line = relationManager.getLineById(relation.getId());
        if (line != null) {
            relationManager.selectRelation(line);
        }
    }
}