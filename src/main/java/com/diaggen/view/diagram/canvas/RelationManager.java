package com.diaggen.view.diagram.canvas;

import com.diaggen.model.DiagramClass;
import com.diaggen.model.DiagramRelation;
import javafx.application.Platform;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;

import java.util.HashMap;
import java.util.Map;

public class RelationManager {

    private final Pane container;
    private final NodeManager nodeManager;
    private final Map<String, RelationLine> relationLines = new HashMap<>();
    private RelationLine selectedRelation;

    private RelationSelectionListener selectionListener;

    public RelationManager(Pane container, NodeManager nodeManager) {
        this.container = container;
        this.nodeManager = nodeManager;
    }

    public RelationLine createRelationLine(DiagramRelation relation) {
        ClassNode sourceNode = nodeManager.getNodeById(relation.getSourceClass().getId());
        ClassNode targetNode = nodeManager.getNodeById(relation.getTargetClass().getId());

        if (sourceNode != null && targetNode != null) {
            RelationLine relationLine = new RelationLine(relation, sourceNode, targetNode);

            relationLine.setOnMousePressed(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    selectRelation(relationLine);
                    e.consume();
                }
            });

            container.getChildren().add(0, relationLine); // En dessous des nœuds
            relationLines.put(relation.getId(), relationLine);

            return relationLine;
        }

        return null;
    }

    public void removeRelationLine(DiagramRelation relation) {
        RelationLine line = relationLines.get(relation.getId());
        if (line != null) {
            container.getChildren().remove(line);
            relationLines.remove(relation.getId());

            if (selectedRelation == line) {
                selectedRelation = null;
            }
        }
    }

    public void updateAllRelations() {
        for (RelationLine line : relationLines.values()) {
            line.update();
        }
    }

    public void clear() {
        container.getChildren().removeIf(node -> node instanceof RelationLine);
        relationLines.clear();
        selectedRelation = null;
    }

    public void updateAllRelationsLater() {
        Platform.runLater(this::updateAllRelations);
    }

    public void selectRelation(RelationLine line) {
        // Désélectionner la relation précédente
        if (selectedRelation != null) {
            selectedRelation.setSelected(false);
        }

        nodeManager.selectNode(null);

        selectedRelation = line;

        if (line != null) {
            line.setSelected(true);
            line.toFront();

            // Notifier l'écouteur
            if (selectionListener != null) {
                selectionListener.onRelationSelected(line);
            }
        }
    }

    public RelationLine getSelectedRelationLine() {
        return selectedRelation;
    }

    public DiagramRelation getSelectedRelation() {
        return selectedRelation != null ? selectedRelation.getRelation() : null;
    }


    public RelationLine getLineById(String relationId) {
        return relationLines.get(relationId);
    }


    public void setRelationSelectionListener(RelationSelectionListener listener) {
        this.selectionListener = listener;
    }

    public interface RelationSelectionListener {
        void onRelationSelected(RelationLine line);
    }
}