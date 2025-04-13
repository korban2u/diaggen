package com.diaggen.view.diagram.canvas;

import com.diaggen.model.DiagramClass;
import com.diaggen.model.DiagramRelation;
import javafx.application.Platform;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class RelationManager {

    private final Pane container;
    private final NodeManager nodeManager;
    private final Map<String, RelationLine> relationLines = new HashMap<>();
    private RelationLine selectedRelation;

    private ViewportTransform viewportTransform;
    private double currentZoomScale = 1.0;

    private RelationSelectionListener selectionListener;
    private final AtomicBoolean updateScheduled = new AtomicBoolean(false);
    private Runnable changeListener;

    public RelationManager(Pane container, NodeManager nodeManager) {
        this.container = container;
        this.nodeManager = nodeManager;
    }

    // Méthode pour définir le ViewportTransform
    public void setViewportTransform(ViewportTransform viewportTransform) {
        this.viewportTransform = viewportTransform;

        // Ajouter un listener pour mettre à jour l'échelle de zoom
        this.viewportTransform.scaleProperty().addListener((obs, oldVal, newVal) -> {
            this.currentZoomScale = newVal.doubleValue();
            updateRelationsZoomScale();
        });
    }

    // Mettre à jour l'échelle de zoom pour toutes les relations
    private void updateRelationsZoomScale() {
        for (RelationLine line : relationLines.values()) {
            line.setZoomScale(currentZoomScale);
        }
    }

    // Méthode pour définir un écouteur de changements
    public void setChangeListener(Runnable listener) {
        this.changeListener = listener;
    }

    // Méthode pour notifier les changements
    private void notifyChange() {
        if (changeListener != null) {
            changeListener.run();
        }
    }

    public RelationLine createRelationLine(DiagramRelation relation) {
        ClassNode sourceNode = nodeManager.getNodeById(relation.getSourceClass().getId());
        ClassNode targetNode = nodeManager.getNodeById(relation.getTargetClass().getId());

        if (sourceNode != null && targetNode != null) {
            RelationLine relationLine = new RelationLine(relation, sourceNode, targetNode);

            // Définir l'échelle de zoom actuelle
            relationLine.setZoomScale(currentZoomScale);

            container.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    double x = e.getX();
                    double y = e.getY();
                    if (relationLine.isNearLine(x, y)) {
                        selectRelation(relationLine);
                        e.consume();
                    }
                }
            });
            container.getChildren().add(0, relationLine);
            relationLines.put(relation.getId(), relationLine);

            notifyChange();

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

            notifyChange();
        }
    }

    public void updateAllRelations() {
        for (RelationLine line : relationLines.values()) {
            line.update();
        }

        notifyChange();
    }

    public void clear() {
        container.getChildren().removeIf(node -> node instanceof RelationLine);
        relationLines.clear();
        selectedRelation = null;

        notifyChange();
    }

    public void updateAllRelationsLater() {
        if (updateScheduled.compareAndSet(false, true)) {
            Platform.runLater(() -> {
                try {
                    updateAllRelations();
                } finally {
                    updateScheduled.set(false);
                }
            });
        }
    }

    public void selectRelation(RelationLine line) {
        if (selectedRelation != null) {
            selectedRelation.setSelected(false);
        }

        selectedRelation = line;

        if (line != null) {
            line.setSelected(true);
            line.toFront();
            if (selectionListener != null) {
                selectionListener.onRelationSelected(line);
            }
        } else {
            if (selectionListener != null) {
                selectionListener.onRelationSelected(null);
            }
        }

        notifyChange();
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