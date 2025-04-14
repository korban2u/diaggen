package com.diaggen.view.diagram.canvas;

import com.diaggen.controller.command.CommandManager;
import com.diaggen.controller.command.MoveClassCommand;
import com.diaggen.event.ClassMovedEvent;
import com.diaggen.event.EventBus;
import com.diaggen.model.DiagramClass;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

import java.util.HashMap;
import java.util.Map;

public class NodeManager {

    private final Pane container;
    private final Map<String, ClassNode> classNodes = new HashMap<>();
    private final EventBus eventBus;
    private ClassNode selectedNode;
    private double dragStartX;
    private double dragStartY;
    private Point2D dragStartPoint;
    private boolean isDragging = false;
    private NodeSelectionListener selectionListener;
    private RelationManager relationManager;
    private CommandManager commandManager;
    private Runnable changeListener;
    private ViewportTransform viewportTransform;

    public NodeManager(Pane container) {
        this.container = container;
        this.eventBus = EventBus.getInstance();
    }

    public void setViewportTransform(ViewportTransform viewportTransform) {
        this.viewportTransform = viewportTransform;
    }

    public void setRelationManager(RelationManager relationManager) {
        this.relationManager = relationManager;
    }

    public void setCommandManager(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    public void setChangeListener(Runnable listener) {
        this.changeListener = listener;
    }

    private void notifyChange() {
        if (changeListener != null) {
            changeListener.run();
        }
    }

    public void createClassNode(DiagramClass diagramClass) {
        ClassNode classNode = new ClassNode(diagramClass);

        setupMouseHandlers(diagramClass, classNode);
        addNodeToContainer(classNode, diagramClass);

        notifyChange();
    }

    private void setupMouseHandlers(DiagramClass diagramClass, ClassNode classNode) {
        classNode.setOnMousePressed(e -> handleMousePressed(e, classNode));
        classNode.setOnMouseDragged(e -> handleMouseDragged(e, classNode));
        classNode.setOnMouseReleased(e -> handleMouseReleased(e, diagramClass, classNode));

        if (relationManager != null) {
            classNode.setPositionChangeListener(() -> relationManager.updateAllRelationsLater());
        }
    }

    private void handleMousePressed(MouseEvent e, ClassNode classNode) {
        if (e.getButton() != MouseButton.PRIMARY) {
            return;
        }

        selectNode(classNode);
        dragStartX = e.getSceneX();
        dragStartY = e.getSceneY();
        dragStartPoint = new Point2D(classNode.getLayoutX(), classNode.getLayoutY());
        isDragging = false;
        e.consume();
    }

    private void handleMouseDragged(MouseEvent e, ClassNode classNode) {
        if (e.getButton() != MouseButton.PRIMARY) {
            return;
        }

        isDragging = true;
        moveNodeToNewPosition(e, classNode);
        updateRelationsDuringDrag();
        e.consume();
    }

    private void moveNodeToNewPosition(MouseEvent e, ClassNode classNode) {
        double scale = viewportTransform != null ? viewportTransform.getScale() : 1.0;
        double offsetX = (e.getSceneX() - dragStartX) / scale;
        double offsetY = (e.getSceneY() - dragStartY) / scale;

        double newX = dragStartPoint.getX() + offsetX;
        double newY = dragStartPoint.getY() + offsetY;

        classNode.setLayoutX(newX);
        classNode.setLayoutY(newY);
    }

    private void updateRelationsDuringDrag() {
        if (relationManager != null) {
            relationManager.updateAllRelationsLater();
        }
    }

    private void handleMouseReleased(MouseEvent e, DiagramClass diagramClass, ClassNode classNode) {
        if (e.getButton() != MouseButton.PRIMARY || !isDragging) {
            isDragging = false;
            return;
        }

        processNodeMovement(diagramClass, classNode);
        isDragging = false;
    }

    private void processNodeMovement(DiagramClass diagramClass, ClassNode classNode) {
        double oldX = dragStartPoint.getX();
        double oldY = dragStartPoint.getY();
        double newX = classNode.getLayoutX();
        double newY = classNode.getLayoutY();

        if (!isSignificantMove(oldX, oldY, newX, newY)) {
            return;
        }

        updateModelPosition(diagramClass, oldX, oldY, newX, newY);
        updateRelationsAfterMove();
    }

    private boolean isSignificantMove(double oldX, double oldY, double newX, double newY) {
        return Math.abs(oldX - newX) > 2 || Math.abs(oldY - newY) > 2;
    }

    private void updateModelPosition(DiagramClass diagramClass, double oldX, double oldY, double newX, double newY) {
        if (commandManager != null) {
            executePositionCommand(diagramClass, oldX, oldY, newX, newY);
            publishMoveEvent(diagramClass, oldX, oldY, newX, newY);
        } else {
            diagramClass.setX(newX);
            diagramClass.setY(newY);
        }
    }

    private void executePositionCommand(DiagramClass diagramClass, double oldX, double oldY, double newX, double newY) {
        MoveClassCommand command = new MoveClassCommand(diagramClass, oldX, oldY, newX, newY);
        commandManager.executeCommand(command);
    }

    private void publishMoveEvent(DiagramClass diagramClass, double oldX, double oldY, double newX, double newY) {
        String diagramId = diagramClass.getDiagramId();
        if (diagramId != null) {
            eventBus.publish(new ClassMovedEvent(diagramId, diagramClass.getId(), oldX, oldY, newX, newY));
        }
    }

    private void updateRelationsAfterMove() {
        if (relationManager != null) {
            relationManager.updateAllRelations();
            notifyChange();
        }
    }

    private void addNodeToContainer(ClassNode classNode, DiagramClass diagramClass) {
        container.getChildren().add(classNode);
        classNodes.put(diagramClass.getId(), classNode);
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
        container.getChildren().removeIf(ClassNode.class::isInstance);
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