package com.diaggen.view.diagram.canvas;

import com.diaggen.controller.command.CommandManager;
import com.diaggen.controller.command.MoveClassCommand;
import com.diaggen.event.ClassMovedEvent;
import com.diaggen.event.EventBus;
import com.diaggen.model.DiagramClass;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;

import java.util.HashMap;
import java.util.Map;

public class NodeManager {

    private final Pane container;
    private final Map<String, ClassNode> classNodes = new HashMap<>();
    private ClassNode selectedNode;

    private double dragStartX;
    private double dragStartY;
    private Point2D dragStartPoint;
    private boolean isDragging = false;

    private NodeSelectionListener selectionListener;
    private RelationManager relationManager;
    private CommandManager commandManager;
    private final EventBus eventBus;
    private Runnable changeListener;

    // Ajout d'une référence au ViewportTransform pour gérer correctement le zoom
    private ViewportTransform viewportTransform;

    public NodeManager(Pane container) {
        this.container = container;
        this.eventBus = EventBus.getInstance();
    }

    // Méthode pour définir le ViewportTransform
    public void setViewportTransform(ViewportTransform viewportTransform) {
        this.viewportTransform = viewportTransform;
    }

    public void setRelationManager(RelationManager relationManager) {
        this.relationManager = relationManager;
    }

    public void setCommandManager(CommandManager commandManager) {
        this.commandManager = commandManager;
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

    public ClassNode createClassNode(DiagramClass diagramClass) {
        ClassNode classNode = new ClassNode(diagramClass);
        classNode.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                selectNode(classNode);
                dragStartX = e.getSceneX();
                dragStartY = e.getSceneY();
                dragStartPoint = new Point2D(classNode.getLayoutX(), classNode.getLayoutY());
                isDragging = false;
                e.consume();
            }
        });

        classNode.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                isDragging = true;

                // Ajuster le déplacement en fonction du facteur de zoom
                double scale = viewportTransform != null ? viewportTransform.getScale() : 1.0;
                double offsetX = (e.getSceneX() - dragStartX) / scale;
                double offsetY = (e.getSceneY() - dragStartY) / scale;

                double newX = dragStartPoint.getX() + offsetX;
                double newY = dragStartPoint.getY() + offsetY;

                classNode.setLayoutX(newX);
                classNode.setLayoutY(newY);
                if (relationManager != null) {
                    relationManager.updateAllRelationsLater();
                }

                e.consume();
            }
        });

        classNode.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                if (isDragging) {
                    double oldX = dragStartPoint.getX();
                    double oldY = dragStartPoint.getY();
                    double newX = classNode.getLayoutX();
                    double newY = classNode.getLayoutY();
                    if (Math.abs(oldX - newX) > 2 || Math.abs(oldY - newY) > 2) {
                        if (commandManager != null) {
                            MoveClassCommand command = new MoveClassCommand(diagramClass, oldX, oldY, newX, newY);
                            commandManager.executeCommand(command);
                            String diagramId = diagramClass.getDiagramId();
                            if (diagramId != null) {
                                eventBus.publish(new ClassMovedEvent(diagramId, diagramClass.getId(), oldX, oldY, newX, newY));
                            }
                        } else {
                            diagramClass.setX(newX);
                            diagramClass.setY(newY);
                        }
                        if (relationManager != null) {
                            relationManager.updateAllRelations();
                            notifyChange();
                        }
                    }
                }
                isDragging = false;
            }
        });
        container.getChildren().add(classNode);
        classNodes.put(diagramClass.getId(), classNode);
        if (relationManager != null) {
            classNode.setPositionChangeListener(() -> relationManager.updateAllRelationsLater());
        }

        notifyChange();

        return classNode;
    }

    public void removeClassNode(DiagramClass diagramClass) {
        ClassNode node = classNodes.get(diagramClass.getId());
        if (node != null) {
            container.getChildren().remove(node);
            classNodes.remove(diagramClass.getId());

            if (selectedNode == node) {
                selectedNode = null;
                if (selectionListener != null) {
                    selectionListener.onNodeSelected(null);
                }
            }

            notifyChange();
        }
    }

    public void clear() {
        container.getChildren().removeIf(node -> node instanceof ClassNode);
        classNodes.clear();
        selectedNode = null;
        if (selectionListener != null) {
            selectionListener.onNodeSelected(null);
        }

        notifyChange();
    }

    public void selectNode(ClassNode node) {
        if (selectedNode != null) {
            selectedNode.setSelected(false);
        }

        selectedNode = node;

        if (node != null) {
            node.setSelected(true);
            node.toFront();
        }
        if (selectionListener != null) {
            selectionListener.onNodeSelected(node);
        }

        notifyChange();
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