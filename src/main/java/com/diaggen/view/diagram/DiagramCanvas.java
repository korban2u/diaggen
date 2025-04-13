package com.diaggen.view.diagram;

import com.diaggen.event.DiagramChangedEvent;
import com.diaggen.event.ElementSelectedEvent;
import com.diaggen.event.EventBus;
import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramClass;
import com.diaggen.model.DiagramRelation;
import com.diaggen.view.diagram.canvas.*;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class DiagramCanvas extends AnchorPane {

    private static final double DEFAULT_GRID_WIDTH = 50000;
    private static final double DEFAULT_GRID_HEIGHT = 50000;
    private final Canvas gridCanvas;
    private final GridRenderer gridRenderer;
    private final NodeManager nodeManager;
    private final RelationManager relationManager;
    private final EventBus eventBus = EventBus.getInstance();
    private final ViewportTransform viewportTransform;
    private final NavigationManager navigationManager;
    private final StackPane canvasContainer = new StackPane();
    private final Pane contentPane = new Pane();
    private final NavigationControls navigationControls;
    private final MiniMapView miniMapView;
    private final PositionIndicator positionIndicator;
    private final BooleanProperty editorPanelVisible = new SimpleBooleanProperty(false);
    private ClassDiagram diagram;
    private double editorPanelWidth = 300.0;
    private Runnable onAddClassRequest;
    private Runnable onDeleteRequest;
    private Consumer<DiagramClass> classSelectionListener;
    private Consumer<DiagramRelation> relationSelectionListener;

    public DiagramCanvas() {
        getStyleClass().add("diagram-canvas");
        setStyle("-fx-background-color: white;");
        setPrefSize(800, 600);
        canvasContainer.getStyleClass().add("canvas-container");
        canvasContainer.setStyle("-fx-background-color: white;");
        gridCanvas = new Canvas();
        gridCanvas.widthProperty().bind(canvasContainer.widthProperty());
        gridCanvas.heightProperty().bind(canvasContainer.heightProperty());
        canvasContainer.getChildren().addAll(gridCanvas, contentPane);
        AnchorPane.setTopAnchor(canvasContainer, 0.0);
        AnchorPane.setRightAnchor(canvasContainer, 0.0);
        AnchorPane.setBottomAnchor(canvasContainer, 0.0);
        AnchorPane.setLeftAnchor(canvasContainer, 0.0);
        getChildren().add(canvasContainer);
        viewportTransform = new ViewportTransform();
        gridRenderer = new GridRenderer(gridCanvas, 10, 50);
        nodeManager = new NodeManager(contentPane);
        nodeManager.setViewportTransform(viewportTransform);
        relationManager = new RelationManager(contentPane, nodeManager);
        relationManager.setViewportTransform(viewportTransform);
        nodeManager.setRelationManager(relationManager);
        navigationManager = new NavigationManager(canvasContainer, viewportTransform);
        navigationControls = new NavigationControls(
                viewportTransform,
                navigationManager,
                gridRenderer
        );
        AnchorPane.setBottomAnchor(navigationControls, 10.0);
        AnchorPane.setLeftAnchor(navigationControls, null);
        getChildren().add(navigationControls);
        widthProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                double controlWidth = navigationControls.getWidth();
                double newX = (newVal.doubleValue() - controlWidth) / 2;
                AnchorPane.setLeftAnchor(navigationControls, newX);
            });
        });
        miniMapView = new MiniMapView(canvasContainer, viewportTransform);
        AnchorPane.setTopAnchor(miniMapView, 10.0);
        AnchorPane.setRightAnchor(miniMapView, 10.0);
        getChildren().add(miniMapView);
        positionIndicator = new PositionIndicator(viewportTransform);
        AnchorPane.setBottomAnchor(positionIndicator, 60.0);
        AnchorPane.setLeftAnchor(positionIndicator, 10.0);
        getChildren().add(positionIndicator);
        setupContextMenu();
        setupKeyHandlers();
        setupMouseHandlers();
        setupSelectionListeners();
        setupEventBusListeners();
        setupMiniMapUpdates();
        editorPanelVisible.addListener((obs, wasVisible, isVisible) -> {
            adjustForEditorPanel(isVisible);
        });
        viewportTransform.scaleProperty().addListener((obs, oldVal, newVal) -> updateTransform());
        viewportTransform.translateXProperty().addListener((obs, oldVal, newVal) -> updateTransform());
        viewportTransform.translateYProperty().addListener((obs, oldVal, newVal) -> updateTransform());
        canvasContainer.widthProperty().addListener((obs, oldVal, newVal) -> gridRenderer.drawGrid());
        canvasContainer.heightProperty().addListener((obs, oldVal, newVal) -> gridRenderer.drawGrid());
        Platform.runLater(gridRenderer::drawGrid);

        setFocusTraversable(true);
        contentPane.setPrefSize(DEFAULT_GRID_WIDTH, DEFAULT_GRID_HEIGHT);
    }

    public void setEditorPanelState(boolean isVisible, double width) {
        if (width > 0) {
            this.editorPanelWidth = width;
        }
        if (editorPanelVisible.get() != isVisible) {
            editorPanelVisible.set(isVisible);
        } else if (isVisible) {
            adjustForEditorPanel(true);
        }
    }

    private void adjustForEditorPanel(boolean isEditorVisible) {
        adjustMiniMapPosition(isEditorVisible);
    }

    private void adjustMiniMapPosition(boolean isEditorVisible) {
        if (miniMapView == null) return;
        DoubleProperty topAnchor = new SimpleDoubleProperty();
        DoubleProperty rightAnchor = new SimpleDoubleProperty();
        Double currentTop = AnchorPane.getTopAnchor(miniMapView);
        Double currentRight = AnchorPane.getRightAnchor(miniMapView);
        topAnchor.set(currentTop != null ? currentTop : 10.0);
        rightAnchor.set(currentRight != null ? currentRight : 10.0);
        topAnchor.addListener((obs, oldVal, newVal) ->
                AnchorPane.setTopAnchor(miniMapView, newVal.doubleValue()));
        rightAnchor.addListener((obs, oldVal, newVal) ->
                AnchorPane.setRightAnchor(miniMapView, newVal.doubleValue()));

        if (isEditorVisible) {
            double requiredOffset = editorPanelWidth + 20.0;
            if (getWidth() - requiredOffset < miniMapView.getWidth() + 10) {
                Timeline timeline = new Timeline(
                        new KeyFrame(Duration.millis(250),
                                new KeyValue(topAnchor, getHeight() - miniMapView.getHeight() - 70),
                                new KeyValue(rightAnchor, getWidth() - editorPanelWidth - miniMapView.getWidth() - 20)
                        )
                );
                timeline.play();
            } else {
                Timeline timeline = new Timeline(
                        new KeyFrame(Duration.millis(250),
                                new KeyValue(rightAnchor, requiredOffset)
                        )
                );
                timeline.play();
            }
        } else {
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.millis(250),
                            new KeyValue(topAnchor, 10.0),
                            new KeyValue(rightAnchor, 10.0)
                    )
            );
            timeline.play();
        }
    }

    private void setupMiniMapUpdates() {
        nodeManager.setChangeListener(() -> {
            Platform.runLater(() -> {
                if (diagram != null) {
                    miniMapView.updateContent(diagram.getClasses());
                }
            });
        });

        relationManager.setChangeListener(() -> {
            Platform.runLater(() -> {
                if (diagram != null) {
                    miniMapView.updateContent(diagram.getClasses());
                }
            });
        });
    }

    private void updateTransform() {
        contentPane.setScaleX(viewportTransform.getScale());
        contentPane.setScaleY(viewportTransform.getScale());
        contentPane.setTranslateX(viewportTransform.getTranslateX());
        contentPane.setTranslateY(viewportTransform.getTranslateY());

        gridRenderer.setTransform(
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

        MenuItem resetViewItem = new MenuItem("Réinitialiser la vue");
        resetViewItem.setOnAction(e -> navigationManager.resetView());

        contextMenu.getItems().addAll(addClassItem, fitToViewItem, resetViewItem);

        canvasContainer.setOnContextMenuRequested(e -> {
            if (e.getTarget() == canvasContainer || e.getTarget() == gridCanvas) {
                contextMenu.show(canvasContainer, e.getScreenX(), e.getScreenY());
            }
        });
    }

    private void setupKeyHandlers() {
        setOnKeyPressed(this::handleKeyPress);
    }

    private void setupMouseHandlers() {
        canvasContainer.setOnMousePressed(e -> {
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
        canvasContainer.setOnMouseMoved(e -> {
            positionIndicator.updatePosition(e, canvasContainer);
        });
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
            navigationManager.zoomIn();
            event.consume();
        } else if (event.isControlDown() && event.getCode() == KeyCode.MINUS) {
            navigationManager.zoomOut();
            event.consume();
        } else if (event.isControlDown() && event.getCode() == KeyCode.DIGIT0) {
            navigationManager.resetView();
            event.consume();
        } else if (event.isControlDown() && event.getCode() == KeyCode.F) {
            zoomToFit();
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
                    miniMapView.highlightClass(selectedClass.getId());
                }

                if (classSelectionListener != null) {
                    classSelectionListener.accept(selectedClass);
                }
            } else {
                if (diagram != null) {
                    for (DiagramClass diagramClass : diagram.getClasses()) {
                        miniMapView.unhighlightClass(diagramClass.getId());
                    }
                }

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
        eventBus.subscribe(DiagramChangedEvent.class, event -> {
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
            Platform.runLater(() -> {
                miniMapView.updateContent(diagram.getClasses());
            });

            Platform.runLater(this::zoomToFit);
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
            for (DiagramClass diagramClass : diagram.getClasses()) {
                if (!existingClasses.containsKey(diagramClass.getId())) {
                    nodeManager.createClassNode(diagramClass);
                } else {
                    ClassNode node = nodeManager.getNodeById(diagramClass.getId());
                    if (node != null) {
                        node.refresh();
                    }
                }
            }
            for (DiagramRelation relation : diagram.getRelations()) {
                if (!existingRelations.containsKey(relation.getId())) {
                    relationManager.createRelationLine(relation);
                }
            }
            for (String classId : new HashMap<>(existingClasses).keySet()) {
                boolean found = false;
                for (DiagramClass diagramClass : diagram.getClasses()) {
                    if (diagramClass.getId().equals(classId)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    nodeManager.removeClassNode(existingClasses.get(classId));
                }
            }
            for (String relationId : new HashMap<>(existingRelations).keySet()) {
                boolean found = false;
                for (DiagramRelation relation : diagram.getRelations()) {
                    if (relation.getId().equals(relationId)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    relationManager.removeRelationLine(existingRelations.get(relationId));
                }
            }

            relationManager.updateAllRelationsLater();
            Platform.runLater(() -> {
                miniMapView.updateContent(diagram.getClasses());
            });
            if (selectedClass != null && diagram.getClasses().contains(selectedClass)) {
                selectClass(selectedClass);
            } else if (selectedRelation != null && diagram.getRelations().contains(selectedRelation)) {
                selectRelation(selectedRelation);
            } else if (selectedClass != null || selectedRelation != null) {
                deselectAll();
            }
        }
    }

    public Dimension2D calculateRequiredSize() {
        if (diagram == null || diagram.getClasses().isEmpty()) {
            return new Dimension2D(DEFAULT_GRID_WIDTH, DEFAULT_GRID_HEIGHT);
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

        double margin = 1000;
        double width = Math.max(DEFAULT_GRID_WIDTH, maxX - minX + 2 * margin);
        double height = Math.max(DEFAULT_GRID_HEIGHT, maxY - minY + 2 * margin);

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