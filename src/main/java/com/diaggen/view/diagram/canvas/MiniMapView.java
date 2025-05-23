package com.diaggen.view.diagram.canvas;

import com.diaggen.model.ClassType;
import com.diaggen.model.DiagramClass;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class MiniMapView extends VBox {
    private final Pane diagramCanvas;
    private final ViewportTransform transform;
    private final Pane contentRepresentation = new Pane();
    private final Rectangle viewportRect = new Rectangle();
    private final Label titleLabel = new Label("Aperçu");
    private final AtomicBoolean isUpdating = new AtomicBoolean(false);
    private final Map<String, Rectangle> classRectangles = new HashMap<>();
    private final Map<String, Line> relationLines = new HashMap<>();
    private final double originalRightAnchor = 10.0;
    private final double originalTopAnchor = 10.0;
    private boolean isRepositioning = false;
    private double miniMapScale = 0.1;
    private boolean isDragging = false;
    private boolean isExpanded = false;
    private double minX = 0;
    private double minY = 0;
    private double maxX = 0;
    private double maxY = 0;

    public MiniMapView(Pane diagramCanvas, ViewportTransform transform) {
        this.diagramCanvas = diagramCanvas;
        this.transform = transform;
        setPrefSize(180, 150);
        setMaxSize(180, 150);
        setSpacing(5);
        setPadding(new Insets(5));
        setStyle("-fx-background-color: rgba(255, 255, 255, 0.85); -fx-background-radius: 8; -fx-z-index: 1000;");
        setViewOrder(-10);
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(6.0);
        dropShadow.setOffsetX(2.0);
        dropShadow.setOffsetY(2.0);
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.3));
        setEffect(dropShadow);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        titleLabel.setStyle("-fx-text-fill: #4a89dc;");
        contentRepresentation.setPrefSize(170, 100);
        contentRepresentation.setMinSize(170, 100);
        contentRepresentation.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 4;");
        viewportRect.setFill(Color.rgb(74, 137, 220, 0.2));
        viewportRect.setStroke(Color.rgb(74, 137, 220, 0.8));
        viewportRect.setStrokeWidth(1.5);
        viewportRect.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 2, 0, 0, 0);");
        contentRepresentation.getChildren().add(viewportRect);
        getChildren().addAll(titleLabel, contentRepresentation);
        Tooltip tooltip = new Tooltip("Mini-carte: cliquez pour naviguer\nDrag & drop pour déplacer la vue\nDouble-clic pour agrandir/réduire");
        Tooltip.install(this, tooltip);
        setOnMousePressed(this::handleMousePress);
        setOnMouseDragged(this::handleMouseDrag);
        setOnMouseReleased(this::handleMouseRelease);
        contentRepresentation.setOnMousePressed(this::handleMousePress);
        contentRepresentation.setOnMouseDragged(this::handleMouseDrag);
        contentRepresentation.setOnMouseReleased(this::handleMouseRelease);
        setupAnimations();
        transform.scaleProperty().addListener((obs, oldVal, newVal) -> updateViewportRect());
        transform.translateXProperty().addListener((obs, oldVal, newVal) -> updateViewportRect());
        transform.translateYProperty().addListener((obs, oldVal, newVal) -> updateViewportRect());
    }

    private void setupAnimations() {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), this);
        fadeIn.setFromValue(0.7);
        fadeIn.setToValue(1.0);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), this);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.7);

        setOpacity(0.7);
        setOnMouseEntered(e -> fadeIn.play());
        setOnMouseExited(e -> fadeOut.play());
        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                toggleExpanded();
                e.consume();
            }
        });
    }

    private void toggleExpanded() {
        isExpanded = !isExpanded;

        if (isExpanded) {
            setPrefSize(240, 200);
            setMaxSize(240, 200);
            contentRepresentation.setPrefSize(230, 160);
            contentRepresentation.setMinSize(230, 160);
            titleLabel.setText("Aperçu (cliquez pour réduire)");
        } else {
            setPrefSize(180, 150);
            setMaxSize(180, 150);
            contentRepresentation.setPrefSize(170, 100);
            contentRepresentation.setMinSize(170, 100);
            titleLabel.setText("Aperçu");
        }
        updateContent(null);
    }

    public void repositionForEditorPanel(boolean isEditorVisible, double editorWidth) {
        if (isRepositioning) {
            return;
        }

        isRepositioning = true;

        try {
            TranslateTransition transition = new TranslateTransition(Duration.millis(250), this);

            if (isEditorVisible) {
                double offset = editorWidth + 10;
                transition.setByX(-offset);
            } else {
                transition.setByX(0);
                transition.setToX(0);
            }

            transition.setOnFinished(e -> isRepositioning = false);
            transition.play();
        } catch (Exception e) {
            isRepositioning = false;
            if (isEditorVisible) {
                double offset = editorWidth + 10;
                setTranslateX(-offset);
            } else {
                setTranslateX(0);
            }
        }
    }

    public boolean isFullyVisible() {
        if (getParent() == null) {
            return true;
        }

        Bounds parentBounds = getParent().getBoundsInLocal();
        Bounds bounds = getBoundsInParent();
        return parentBounds.contains(bounds);
    }

    public void updateContent(Iterable<DiagramClass> classes) {
        if (isUpdating.getAndSet(true)) {
            return;
        }

        try {
            Platform.runLater(() -> {
                try {
                    contentRepresentation.getChildren().clear();
                    classRectangles.clear();
                    relationLines.clear();
                    if (classes == null) {
                        viewportRect.setX(10);
                        viewportRect.setY(10);
                        viewportRect.setWidth(contentRepresentation.getPrefWidth() - 20);
                        viewportRect.setHeight(contentRepresentation.getPrefHeight() - 20);
                        contentRepresentation.getChildren().add(viewportRect);
                        return;
                    }
                    calculateBounds(classes);
                    double contentWidth = maxX - minX;
                    double contentHeight = maxY - minY;

                    if (contentWidth <= 0 || contentHeight <= 0) {
                        contentWidth = 800;
                        contentHeight = 600;
                    }

                    double scaleX = (contentRepresentation.getPrefWidth() - 20) / contentWidth;
                    double scaleY = (contentRepresentation.getPrefHeight() - 20) / contentHeight;
                    miniMapScale = Math.min(scaleX, scaleY);
                    for (DiagramClass diagramClass : classes) {
                        createClassRectangle(diagramClass);
                    }
                    contentRepresentation.getChildren().add(viewportRect);
                    updateViewportRect();
                } finally {
                    isUpdating.set(false);
                }
            });
        } catch (Exception e) {
            isUpdating.set(false);
            throw e;
        }
    }

    private void calculateBounds(Iterable<DiagramClass> classes) {
        minX = Double.MAX_VALUE;
        minY = Double.MAX_VALUE;
        maxX = Double.MIN_VALUE;
        maxY = Double.MIN_VALUE;

        boolean hasClasses = false;

        for (DiagramClass diagramClass : classes) {
            hasClasses = true;
            double x = diagramClass.getX();
            double y = diagramClass.getY();
            double width = 200;
            double height = 120 + diagramClass.getAttributes().size() * 20 + diagramClass.getMethods().size() * 20;

            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x + width);
            maxY = Math.max(maxY, y + height);
        }

        if (!hasClasses) {
            minX = 0;
            minY = 0;
            maxX = 800;
            maxY = 600;
        } else {
            double margin = 100;
            minX -= margin;
            minY -= margin;
            maxX += margin;
            maxY += margin;
        }
    }

    private void createClassRectangle(DiagramClass diagramClass) {
        double x = diagramClass.getX();
        double y = diagramClass.getY();
        double width = 200;
        double height = 120 + diagramClass.getAttributes().size() * 20 + diagramClass.getMethods().size() * 20;

        Rectangle rect = new Rectangle(
                (x - minX) * miniMapScale + 10,
                (y - minY) * miniMapScale + 10,
                width * miniMapScale,
                height * miniMapScale
        );
        if (diagramClass.getClassType() == ClassType.INTERFACE) {
            rect.setFill(Color.rgb(230, 230, 255));
            rect.setStroke(Color.rgb(100, 100, 200));
        } else if (diagramClass.getClassType() == ClassType.ABSTRACT_CLASS) {
            rect.setFill(Color.rgb(255, 230, 230));
            rect.setStroke(Color.rgb(200, 100, 100));
        } else if (diagramClass.getClassType() == ClassType.ENUM) {
            rect.setFill(Color.rgb(230, 255, 230));
            rect.setStroke(Color.rgb(100, 200, 100));
        } else {
            rect.setFill(Color.rgb(245, 245, 245));
            rect.setStroke(Color.rgb(150, 150, 150));
        }

        rect.setStrokeWidth(1);
        rect.setOnMouseEntered(e -> {
            rect.setStrokeWidth(2);
            rect.setStroke(Color.rgb(74, 137, 220));
            Tooltip.install(rect, new Tooltip(diagramClass.getName()));
        });

        rect.setOnMouseExited(e -> {
            rect.setStrokeWidth(1);
            if (diagramClass.getClassType() == ClassType.INTERFACE) {
                rect.setStroke(Color.rgb(100, 100, 200));
            } else if (diagramClass.getClassType() == ClassType.ABSTRACT_CLASS) {
                rect.setStroke(Color.rgb(200, 100, 100));
            } else if (diagramClass.getClassType() == ClassType.ENUM) {
                rect.setStroke(Color.rgb(100, 200, 100));
            } else {
                rect.setStroke(Color.rgb(150, 150, 150));
            }
        });

        contentRepresentation.getChildren().add(rect);
        classRectangles.put(diagramClass.getId(), rect);
    }

    private void updateViewportRect() {
        double contentWidth = diagramCanvas.getWidth() / transform.getScale();
        double contentHeight = diagramCanvas.getHeight() / transform.getScale();
        double viewportX = -transform.getTranslateX() / transform.getScale();
        double viewportY = -transform.getTranslateY() / transform.getScale();

        viewportRect.setX((viewportX - minX) * miniMapScale + 10);
        viewportRect.setY((viewportY - minY) * miniMapScale + 10);
        viewportRect.setWidth(contentWidth * miniMapScale);
        viewportRect.setHeight(contentHeight * miniMapScale);
        double maxX = contentRepresentation.getPrefWidth() - 20;
        double maxY = contentRepresentation.getPrefHeight() - 20;

        if (viewportRect.getX() < 10) viewportRect.setX(10);
        if (viewportRect.getY() < 10) viewportRect.setY(10);
        if (viewportRect.getX() + viewportRect.getWidth() > maxX) {
            viewportRect.setWidth(maxX - viewportRect.getX());
        }
        if (viewportRect.getY() + viewportRect.getHeight() > maxY) {
            viewportRect.setHeight(maxY - viewportRect.getY());
        }
    }

    private void handleMousePress(MouseEvent event) {
        if (viewportRect.contains(event.getX(), event.getY())) {
            isDragging = true;
        } else {
            navigateToPoint(event.getX(), event.getY());
        }
        event.consume();
    }

    private void handleMouseDrag(MouseEvent event) {
        if (isDragging) {
            double deltaX = event.getX() - (viewportRect.getX() + viewportRect.getWidth() / 2);
            double deltaY = event.getY() - (viewportRect.getY() + viewportRect.getHeight() / 2);
            navigateToPoint(viewportRect.getX() + deltaX, viewportRect.getY() + deltaY);
        }
        event.consume();
    }

    private void handleMouseRelease(MouseEvent event) {
        isDragging = false;
        event.consume();
    }

    private void navigateToPoint(double x, double y) {
        double contentX = (x - 10) / miniMapScale + minX;
        double contentY = (y - 10) / miniMapScale + minY;

        double viewportWidth = diagramCanvas.getWidth() / transform.getScale();
        double viewportHeight = diagramCanvas.getHeight() / transform.getScale();

        double translateX = -(contentX - viewportWidth / 2) * transform.getScale();
        double translateY = -(contentY - viewportHeight / 2) * transform.getScale();
        animateTransform(translateX, translateY);
    }

    private void animateTransform(double targetX, double targetY) {
        javafx.animation.Timeline timeline = new javafx.animation.Timeline();

        javafx.animation.KeyValue kvX = new javafx.animation.KeyValue(transform.translateXProperty(), targetX);
        javafx.animation.KeyValue kvY = new javafx.animation.KeyValue(transform.translateYProperty(), targetY);

        javafx.animation.KeyFrame kf = new javafx.animation.KeyFrame(Duration.millis(200), kvX, kvY);

        timeline.getKeyFrames().add(kf);
        timeline.play();
    }

    public void highlightClass(String classId) {
        Rectangle rect = classRectangles.get(classId);
        if (rect != null) {
            rect.setStroke(Color.rgb(74, 137, 220));
            rect.setStrokeWidth(2);
            rect.setEffect(new DropShadow(4, Color.rgb(74, 137, 220, 0.7)));
        }
    }

    public void unhighlightClass(String classId) {
        Rectangle rect = classRectangles.get(classId);
        if (rect != null) {
            rect.setStroke(Color.rgb(150, 150, 150));
            rect.setStrokeWidth(1);
            rect.setEffect(null);
        }
    }
}