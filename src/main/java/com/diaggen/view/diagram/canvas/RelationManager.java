package com.diaggen.view.diagram.canvas;

import com.diaggen.model.DiagramClass;
import com.diaggen.model.DiagramRelation;
import javafx.application.Platform;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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

            // Améliorer la détection des clics sur les relations
            container.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                // Vérifier si le clic est sur cette relation
                if (e.getButton() == MouseButton.PRIMARY) {
                    // Calculer la position du clic relatif au conteneur
                    double x = e.getX();
                    double y = e.getY();

                    // Vérifier si le clic est proche de la ligne
                    if (relationLine.isNearLine(x, y)) {
                        selectRelation(relationLine);
                        e.consume(); // Empêcher la propagation de l'événement
                    }
                }
            });

            // Ajouter la ligne au conteneur (en-dessous des nœuds)
            container.getChildren().add(0, relationLine);
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
                selectRelation(null);
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

        selectedRelation = line;

        if (line != null) {
            line.setSelected(true);
            line.toFront();

            // Notifier l'écouteur
            if (selectionListener != null) {
                selectionListener.onRelationSelected(line);
            }
        } else {
            if (selectionListener != null) {
                selectionListener.onRelationSelected(null);
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

    public Map<String, RelationLine> getRelationLines() {
        return relationLines;
    }

    public void setRelationSelectionListener(RelationSelectionListener listener) {
        this.selectionListener = listener;
    }

    public interface RelationSelectionListener {
        void onRelationSelected(RelationLine line);
    }
}