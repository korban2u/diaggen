package com.diaggen.view.diagram;

import com.diaggen.event.DiagramChangedEvent;
import com.diaggen.event.ElementSelectedEvent;
import com.diaggen.event.EventBus;
import com.diaggen.event.EventListener;
import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramClass;
import com.diaggen.model.DiagramRelation;
import com.diaggen.view.diagram.canvas.*;
import javafx.application.Platform;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class DiagramCanvas extends AnchorPane {

    private ClassDiagram diagram;
    private final Canvas gridCanvas;
    private final GridRenderer gridRenderer;
    private final NodeManager nodeManager;
    private final RelationManager relationManager;
    private final EventBus eventBus = EventBus.getInstance();
    private final ViewportTransform viewportTransform;
    private final NavigationManager navigationManager;
DiagramCanvas    private final StackPane canvasContainer = new StackPane();
    private final Pane contentPane = new Pane();
    private final NavigationControls navigationControls;
    private final MiniMapView miniMapView;

    private Runnable onAddClassRequest;
    private Runnable onDeleteRequest;
    private Consumer<DiagramClass> classSelectionListener;
    private Consumer<DiagramRelation> relationSelectionListener;

    public DiagramCanvas() {
        getStyleClass().add("diagram-canvas");
        setStyle("-fx-background-color: white;");
        setPrefSize(800, 600);
DiagramCanvas        canvasContainer.getStyleClass().add("canvas-container");
        canvasContainer.setStyle("-fx-background-color: white;");
DiagramCanvas        gridCanvas = new Canvas();
        gridCanvas.widthProperty().bind(canvasContainer.widthProperty());
        gridCanvas.heightProperty().bind(canvasContainer.heightProperty());
DiagramCanvas        canvasContainer.getChildren().addAll(gridCanvas, contentPane);
DiagramCanvas        AnchorPane.setTopAnchor(canvasContainer, 0.0);
        AnchorPane.setRightAnchor(canvasContainer, 0.0);
        AnchorPane.setBottomAnchor(canvasContainer, 0.0);
        AnchorPane.setLeftAnchor(canvasContainer, 0.0);
        getChildren().add(canvasContainer);
DiagramCanvas        viewportTransform = new ViewportTransform();
DiagramCanvas        gridRenderer = new GridRenderer(gridCanvas, 10, 50);
DiagramCanvas        nodeManager = new NodeManager(contentPane);
        relationManager = new RelationManager(contentPane, nodeManager);
        nodeManager.setRelationManager(relationManager);
DiagramCanvas        navigationManager = new NavigationManager(canvasContainer, viewportTransform);
DiagramCanvas        navigationControls = new NavigationControls(
                viewportTransform,
                navigationManager,
                gridRenderer
        );
        navigationControls.getStyleClass().add("navigation-controls");
        navigationControls.setPrefHeight(40);
DiagramCanvas        AnchorPane.setBottomAnchor(navigationControls, 10.0);
        AnchorPane.setLeftAnchor(navigationControls, 10.0);
        getChildren().add(navigationControls);
DiagramCanvas        miniMapView = new MiniMapView(canvasContainer, viewportTransform);
        miniMapView.getStyleClass().add("mini-map");
        miniMapView.setPrefSize(150, 120);
DiagramCanvas        AnchorPane.setTopAnchor(miniMapView, 10.0);
        AnchorPane.setRightAnchor(miniMapView, 10.0);
        getChildren().add(miniMapView);
DiagramCanvas        setupContextMenu();
        setupKeyHandlers();
        setupSelectionListeners();
        setupEventBusListeners();
DiagramCanvas        viewportTransform.scaleProperty().addListener((obs, oldVal, newVal) -> updateTransform());
        viewportTransform.translateXProperty().addListener((obs, oldVal, newVal) -> updateTransform());
        viewportTransform.translateYProperty().addListener((obs, oldVal, newVal) -> updateTransform());
DiagramCanvas        canvasContainer.widthProperty().addListener((obs, oldVal, newVal) -> gridRenderer.drawGrid());
        canvasContainer.heightProperty().addListener((obs, oldVal, newVal) -> gridRenderer.drawGrid());

        Platform.runLater(gridRenderer::drawGrid);
DiagramCanvas        canvasContainer.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                if (e.getTarget() == canvasContainer || e.getTarget() == gridCanvas) {
                    deselectAll();
                    e.consume();
                }
            }
        });

        gridCanvas.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                deselectAll();
                e.consume();
            }
        });
DiagramCanvas        setFocusTraversable(true);
    }

    private void updateTransform() {
DiagramCanvas        contentPane.setScaleX(viewportTransform.getScale());
        contentPane.setScaleY(viewportTransform.getScale());
        contentPane.setTranslateX(viewportTransform.getTranslateX());
        contentPane.setTranslateY(viewportTransform.getTranslateY());
DiagramCanvas        gridRenderer.setTransform(
                viewportTransform.getScale(),
                viewportTransform.getTranslateX(),
                viewportTransform.getTranslateY()
        );
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem addClassItem = new MenuItem("Ajouter une classe");
        addClassItem.setOnAction(e -> {
            if (onAddClassRequest != null) {
                onAddClassRequest.run();
            }
        });

        MenuItem fitToViewItem = new MenuItem("Ajuster à la vue");
        fitToViewItem.setOnAction(e -> zoomToFit());

        contextMenu.getItems().addAll(addClassItem, fitToViewItem);

        canvasContainer.setOnContextMenuRequested(e -> {
            if (e.getTarget() == canvasContainer || e.getTarget() == gridCanvas) {
                contextMenu.show(canvasContainer, e.getScreenX(), e.getScreenY());
            }
        });
    }

    private void setupKeyHandlers() {
        setOnKeyPressed(this::handleKeyPress);
    }

    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
            if ((getSelectedClass() != null || getSelectedRelation() != null) && onDeleteRequest != null) {
                onDeleteRequest.run();
                event.consume();
            }
        } else if (event.getCode() == KeyCode.ESCAPE) {
            deselectAll();
            event.consume();
        } else if (event.isControlDown() && event.getCode() == KeyCode.EQUALS) {
DiagramCanvas            navigationManager.zoomIn();
            event.consume();
        } else if (event.isControlDown() && event.getCode() == KeyCode.MINUS) {
DiagramCanvas            navigationManager.zoomOut();
            event.consume();
        } else if (event.isControlDown() && event.getCode() == KeyCode.DIGIT0) {
DiagramCanvas            navigationManager.resetView();
            event.consume();
        } else if (event.isControlDown() && event.getCode() == KeyCode.F) {
DiagramCanvas            zoomToFit();
            event.consume();
        }
    }

    private void setupSelectionListeners() {
        nodeManager.setNodeSelectionListener(node -> {
            if (node != null) {
                relationManager.selectRelation(null);
                DiagramClass selectedClass = node.getDiagramClass();

                if (diagram != null) {
                    eventBus.publish(new ElementSelectedEvent(diagram.getId(), selectedClass.getId(), true));
                }

                if (classSelectionListener != null) {
                    classSelectionListener.accept(selectedClass);
                }
            } else {
                if (classSelectionListener != null) {
                    classSelectionListener.accept(null);
                }
            }
        });

        relationManager.setRelationSelectionListener(line -> {
            if (line != null) {
                nodeManager.selectNode(null);
                DiagramRelation selectedRelation = line.getRelation();

                if (diagram != null) {
                    eventBus.publish(new ElementSelectedEvent(diagram.getId(), selectedRelation.getId(), false));
                }

                if (relationSelectionListener != null) {
                    relationSelectionListener.accept(selectedRelation);
                }
            } else {
                if (relationSelectionListener != null) {
                    relationSelectionListener.accept(null);
                }
            }
        });
    }

    private void setupEventBusListeners() {
        eventBus.subscribe(DiagramChangedEvent.class, (EventListener<DiagramChangedEvent>) event -> {
            if (diagram != null && diagram.getId().equals(event.getDiagramId())) {
                Platform.runLater(this::refresh);
            }
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

        Platform.runLater(() -> {
            for (ClassNode node : nodeManager.getNodes().values()) {
                node.refresh();
            }

            relationManager.updateAllRelations();
            requestLayout();
DiagramCanvas            miniMapView.updateContent(diagram.getClasses());
DiagramCanvas            Platform.runLater(this::zoomToFit);
        });
    }

    public void refresh() {
        if (diagram != null) {
            DiagramClass selectedClass = getSelectedClass();
            DiagramRelation selectedRelation = getSelectedRelation();

            Map<String, DiagramRelation> existingRelations = new HashMap<>();
            Map<String, DiagramClass> existingClasses = new HashMap<>();

            for (RelationLine line : relationManager.getRelationLines().values()) {
                existingRelations.put(line.getRelation().getId(), line.getRelation());
            }

            for (ClassNode node : nodeManager.getNodes().values()) {
                existingClasses.put(node.getDiagramClass().getId(), node.getDiagramClass());
            }
DiagramCanvas            for (DiagramClass diagramClass : diagram.getClasses()) {
                if (!existingClasses.containsKey(diagramClass.getId())) {
                    nodeManager.createClassNode(diagramClass);
                } else {
                    ClassNode node = nodeManager.getNodeById(diagramClass.getId());
                    if (node != null) {
                        node.refresh();
                    }
                }
            }
DiagramCanvas            for (DiagramRelation relation : diagram.getRelations()) {
                if (!existingRelations.containsKey(relation.getId())) {
                    relationManager.createRelationLine(relation);
                }
            }
DiagramCanvas            List<String> classesToRemove = new ArrayList<>();
            for (String classId : existingClasses.keySet()) {
                boolean found = false;
                for (DiagramClass diagramClass : diagram.getClasses()) {
                    if (diagramClass.getId().equals(classId)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    classesToRemove.add(classId);
                }
            }

            for (String classId : classesToRemove) {
                nodeManager.removeClassNode(existingClasses.get(classId));
            }
DiagramCanvas            List<String> relationsToRemove = new ArrayList<>();
            for (String relationId : existingRelations.keySet()) {
                boolean found = false;
                for (DiagramRelation relation : diagram.getRelations()) {
                    if (relation.getId().equals(relationId)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    relationsToRemove.add(relationId);
                }
            }

            for (String relationId : relationsToRemove) {
                relationManager.removeRelationLine(existingRelations.get(relationId));
            }

            relationManager.updateAllRelationsLater();
DiagramCanvas            miniMapView.updateContent(diagram.getClasses());
DiagramCanvas            if (selectedClass != null && diagram.getClasses().contains(selectedClass)) {
                selectClass(selectedClass);
            } else if (selectedRelation != null && diagram.getRelations().contains(selectedRelation)) {
                selectRelation(selectedRelation);
            } else if (classesToRemove.contains(selectedClass) || relationsToRemove.contains(selectedRelation)) {
                deselectAll();
            }
        }
    }

    public Dimension2D calculateRequiredSize() {
        if (diagram == null || diagram.getClasses().isEmpty()) {
            return new Dimension2D(600, 400); // Default size
        }

        double maxX = 0;
        double maxY = 0;
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;

        for (DiagramClass diagramClass : diagram.getClasses()) {
            ClassNode node = nodeManager.getNodeById(diagramClass.getId());
            if (node != null) {
                double nodeRight = node.getLayoutX() + node.getWidth();
                double nodeBottom = node.getLayoutY() + node.getHeight();

                maxX = Math.max(maxX, nodeRight);
                maxY = Math.max(maxY, nodeBottom);
                minX = Math.min(minX, node.getLayoutX());
                minY = Math.min(minY, node.getLayoutY());
            }
        }

        double margin = 100;
        double width = Math.max(600, maxX - minX + 2 * margin);
        double height = Math.max(400, maxY - minY + 2 * margin);

        return new Dimension2D(width, height);
    }

    public void clear() {
        nodeManager.clear();
        relationManager.clear();
    }

    public void deselectAll() {
        nodeManager.selectNode(null);
        relationManager.selectRelation(null);
        requestFocus();
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

    public void setOnDeleteRequest(Runnable handler) {
        this.onDeleteRequest = handler;
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

    public boolean hasSelection() {
        return getSelectedClass() != null || getSelectedRelation() != null;
    }

    public void setClassSelectionListener(Consumer<DiagramClass> listener) {
        this.classSelectionListener = listener;
    }

    public void setRelationSelectionListener(Consumer<DiagramRelation> listener) {
        this.relationSelectionListener = listener;
    }

    public NodeManager getNodeManager() {
        return nodeManager;
    }

    public RelationManager getRelationManager() {
        return relationManager;
    }

    public ClassDiagram getDiagram() {
        return diagram;
    }

    public ViewportTransform getViewportTransform() {
        return viewportTransform;
    }

    public NavigationManager getNavigationManager() {
        return navigationManager;
    }

    public GridRenderer getGridRenderer() {
        return gridRenderer;
    }

    public void zoomToFit() {
        if (diagram == null || diagram.getClasses().isEmpty()) {
            navigationManager.resetView();
            return;
        }

        navigationManager.zoomToFit(diagram.getClasses(), 50);
    }

    public Point2D viewportToContent(Point2D viewportPoint) {
        return viewportTransform.transformPoint(viewportPoint);
    }

    public Point2D contentToViewport(Point2D contentPoint) {
        return viewportTransform.inverseTransformPoint(contentPoint);
    }
}