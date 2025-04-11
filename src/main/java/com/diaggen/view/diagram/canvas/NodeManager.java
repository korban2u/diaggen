package com.diaggen.view.diagram.canvas;

import com.diaggen.model.DiagramClass;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class NodeManager {

    private final Pane container;
    private final Map<String, ClassNode> classNodes = new HashMap<>();
    private ClassNode selectedNode;

    private double dragStartX;
    private double dragStartY;
    private Point2D dragStartPoint;

    private NodeSelectionListener selectionListener;

    private RelationManager relationManager;

    public NodeManager(Pane container) {
        this.container = container;
    }

    public void setRelationManager(RelationManager relationManager) {
        this.relationManager = relationManager;
    }

    public ClassNode createClassNode(DiagramClass diagramClass) {
        ClassNode classNode = new ClassNode(diagramClass);

        // Gestion des événements de souris
        classNode.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                selectNode(classNode);
                dragStartX = e.getSceneX();
                dragStartY = e.getSceneY();
                dragStartPoint = new Point2D(classNode.getLayoutX(), classNode.getLayoutY());
                e.consume();
            }
        });

        classNode.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                double offsetX = e.getSceneX() - dragStartX;
                double offsetY = e.getSceneY() - dragStartY;

                classNode.setLayoutX(dragStartPoint.getX() + offsetX);
                classNode.setLayoutY(dragStartPoint.getY() + offsetY);

                // Mettre à jour les relations en temps réel pendant le déplacement
                if (relationManager != null) {
                    relationManager.updateAllRelations();
                }

                e.consume();
            }
        });

        classNode.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                diagramClass.setX(classNode.getLayoutX());
                diagramClass.setY(classNode.getLayoutY());

                // Mettre à jour toutes les relations à la fin du déplacement
                if (relationManager != null) {
                    relationManager.updateAllRelations();
                }
            }
        });

        // Ajouter au conteneur et à la map
        container.getChildren().add(classNode);
        classNodes.put(diagramClass.getId(), classNode);

        // Configurer l'écouteur de changement de position pour mettre à jour les relations
        if (relationManager != null) {
            classNode.setPositionChangeListener(() -> relationManager.updateAllRelations());
        }

        return classNode;
    }

    public void removeClassNode(DiagramClass diagramClass) {
        ClassNode node = classNodes.get(diagramClass.getId());
        if (node != null) {
            container.getChildren().remove(node);
            classNodes.remove(diagramClass.getId());

            if (selectedNode == node) {
                selectedNode = null;
            }
        }
    }

    public void clear() {
        container.getChildren().removeIf(node -> node instanceof ClassNode);
        classNodes.clear();
        selectedNode = null;
    }

    public void selectNode(ClassNode node) {
        if (selectedNode != null) {
            selectedNode.setSelected(false);
        }

        selectedNode = node;

        if (node != null) {
            node.setSelected(true);
            node.toFront();

            // Notifier l'écouteur
            if (selectionListener != null) {
                selectionListener.onNodeSelected(node);
            }
        }
    }

    public ClassNode getSelectedNode() {
        return selectedNode;
    }

    public DiagramClass getSelectedClass() {
        return selectedNode != null ? selectedNode.getDiagramClass() : null;
    }

    public ClassNode getNodeById(String classId) {
        return classNodes.get(classId);
    }

    public Map<String, ClassNode> getNodes() {
        return classNodes;
    }

    public void setNodeSelectionListener(NodeSelectionListener listener) {
        this.selectionListener = listener;
    }

    public interface NodeSelectionListener {
        void onNodeSelected(ClassNode node);
    }
}