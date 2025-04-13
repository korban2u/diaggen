package com.diaggen.view.diagram.canvas;

import com.diaggen.model.DiagramClass;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class NavigationManager {
    private final Pane targetPane;
    private final ViewportTransform transform;

    private final BooleanProperty panningProperty = new SimpleBooleanProperty(false);
    private final DoubleProperty panSpeedProperty = new SimpleDoubleProperty(20.0);
    private double mouseAnchorX;
    private double mouseAnchorY;
    private double lastTranslateX;
    private double lastTranslateY;

    private static final double ZOOM_FACTOR = 1.2;
    private static final double MIN_SCALE = 0.1;
    private static final double MAX_SCALE = 10.0;
    private static final int KEYBOARD_PAN_STEP = 100;

    private Cursor previousCursor;
    private boolean spacePressed = false;
    private boolean altPressed = false;
    private boolean isBackgroundClick = false;

    public NavigationManager(Pane targetPane, ViewportTransform transform) {
        this.targetPane = targetPane;
        this.transform = transform;

        transform.setMinScale(MIN_SCALE);
        transform.setMaxScale(MAX_SCALE);

        setupEventHandlers();
    }

    private void setupEventHandlers() {
        // Gestion du zoom avec la molette
        targetPane.addEventFilter(ScrollEvent.SCROLL, this::handleScroll);

        // Gestion du panoramique avec le clic du milieu
        targetPane.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if ((event.getButton() == MouseButton.MIDDLE) ||
                    (event.getButton() == MouseButton.PRIMARY && (spacePressed || altPressed))) {
                startPanning(event.getX(), event.getY());
                event.consume();
            } else if (event.getButton() == MouseButton.PRIMARY &&
                    (event.getTarget() == targetPane || isClickOnBackground(event))) {
                isBackgroundClick = true;
                startPanning(event.getX(), event.getY());
                event.consume();
            }
        });

        targetPane.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (isPanning()) {
                double deltaX = event.getX() - mouseAnchorX;
                double deltaY = event.getY() - mouseAnchorY;
                transform.setTranslateX(lastTranslateX + deltaX);
                transform.setTranslateY(lastTranslateY + deltaY);
                event.consume();
            }
        });

        targetPane.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if ((event.getButton() == MouseButton.MIDDLE ||
                    (event.getButton() == MouseButton.PRIMARY && (spacePressed || altPressed || isBackgroundClick)))
                    && isPanning()) {
                stopPanning();
                isBackgroundClick = false;
                event.consume();
            }
        });

        // Touches pour activer le mode panoramique
        targetPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.SPACE && !spacePressed) {
                spacePressed = true;
                previousCursor = targetPane.getCursor();
                targetPane.setCursor(Cursor.MOVE);
                event.consume();
            } else if (event.getCode() == KeyCode.ALT && !altPressed) {
                altPressed = true;
                previousCursor = targetPane.getCursor();
                targetPane.setCursor(Cursor.MOVE);
                event.consume();
            } else {
                handleKeyNavigation(event);
            }
        });

        targetPane.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.SPACE && spacePressed) {
                spacePressed = false;
                targetPane.setCursor(previousCursor);
                if (!isPanning()) {
                    stopPanning();
                }
                event.consume();
            } else if (event.getCode() == KeyCode.ALT && altPressed) {
                altPressed = false;
                targetPane.setCursor(previousCursor);
                if (!isPanning()) {
                    stopPanning();
                }
                event.consume();
            }
        });
    }

    private boolean isClickOnBackground(MouseEvent event) {
        // Vérifie si le clic est sur l'arrière-plan et non sur un élément de diagramme
        Node target = (Node) event.getTarget();
        return target == targetPane ||
                (target.getClass().getSimpleName().equals("Canvas")) ||
                (target.getParent() == targetPane);
    }

    private void handleKeyNavigation(KeyEvent event) {
        double step = KEYBOARD_PAN_STEP / transform.getScale();

        if (event.isControlDown()) {
            step *= 3; // Mouvement plus rapide avec Ctrl enfoncé
        }

        switch (event.getCode()) {
            case UP:
            case W:
                panUp(step);
                event.consume();
                break;
            case DOWN:
            case S:
                panDown(step);
                event.consume();
                break;
            case LEFT:
            case A:
                panLeft(step);
                event.consume();
                break;
            case RIGHT:
            case D:
                panRight(step);
                event.consume();
                break;
            case HOME:
                resetView();
                event.consume();
                break;
        }
    }

    private void handleScroll(ScrollEvent event) {
        if (event.isControlDown()) {
            // Zoom centré sur la position de la souris
            double scaleFactor = event.getDeltaY() > 0 ? ZOOM_FACTOR : 1/ZOOM_FACTOR;
            zoomAt(event.getX(), event.getY(), scaleFactor);
            event.consume();
            return;
        }

        // Navigation horizontale avec Shift + molette
        if (event.isShiftDown()) {
            double panAmount = event.getDeltaY() * panSpeedProperty.get();
            transform.setTranslateX(transform.getTranslateX() + panAmount);
            event.consume();
            return;
        }

        // Navigation verticale standard avec la molette
        double panAmount = event.getDeltaY() * panSpeedProperty.get();
        transform.setTranslateY(transform.getTranslateY() + panAmount);
        event.consume();
    }

    private void startPanning(double x, double y) {
        mouseAnchorX = x;
        mouseAnchorY = y;
        lastTranslateX = transform.getTranslateX();
        lastTranslateY = transform.getTranslateY();
        panningProperty.set(true);

        if (isBackgroundClick) {
            targetPane.setCursor(Cursor.MOVE);
        }
    }

    private void stopPanning() {
        panningProperty.set(false);

        if (isBackgroundClick) {
            targetPane.setCursor(Cursor.DEFAULT);
        }
    }

    public boolean isPanning() {
        return panningProperty.get();
    }

    public BooleanProperty panningProperty() {
        return panningProperty;
    }

    public double getPanSpeed() {
        return panSpeedProperty.get();
    }

    public void setPanSpeed(double speed) {
        panSpeedProperty.set(speed);
    }

    public DoubleProperty panSpeedProperty() {
        return panSpeedProperty;
    }

    public void panLeft(double amount) {
        transform.setTranslateX(transform.getTranslateX() + amount * transform.getScale());
    }

    public void panRight(double amount) {
        transform.setTranslateX(transform.getTranslateX() - amount * transform.getScale());
    }

    public void panUp(double amount) {
        transform.setTranslateY(transform.getTranslateY() + amount * transform.getScale());
    }

    public void panDown(double amount) {
        transform.setTranslateY(transform.getTranslateY() - amount * transform.getScale());
    }

    private Point2D viewportToContent(Point2D viewportPoint) {
        return transform.transformPoint(viewportPoint);
    }

    private Point2D contentToViewport(Point2D contentPoint) {
        return transform.inverseTransformPoint(contentPoint);
    }

    public void resetView() {
        animateTransform(1.0, 0, 0);
    }

    private void animateTransform(double targetScale, double targetX, double targetY) {
        Timeline timeline = new Timeline();

        KeyValue kvScale = new KeyValue(transform.scaleProperty(), targetScale);
        KeyValue kvX = new KeyValue(transform.translateXProperty(), targetX);
        KeyValue kvY = new KeyValue(transform.translateYProperty(), targetY);

        KeyFrame kf = new KeyFrame(Duration.millis(300), kvScale, kvX, kvY);

        timeline.getKeyFrames().add(kf);
        timeline.play();
    }

    public void zoomToFit(Iterable<DiagramClass> classes, double padding) {
        if (classes == null) {
            resetView();
            return;
        }
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;
        boolean hasClasses = false;

        for (DiagramClass diagramClass : classes) {
            hasClasses = true;
            double nodeX = diagramClass.getX();
            double nodeY = diagramClass.getY();
            double nodeWidth = 200;  // Approximation
            double nodeHeight = 150; // Approximation

            minX = Math.min(minX, nodeX);
            minY = Math.min(minY, nodeY);
            maxX = Math.max(maxX, nodeX + nodeWidth);
            maxY = Math.max(maxY, nodeY + nodeHeight);
        }

        if (!hasClasses) {
            resetView();
            return;
        }

        // Ajouter de la marge
        minX -= padding;
        minY -= padding;
        maxX += padding;
        maxY += padding;

        double contentWidth = maxX - minX;
        double contentHeight = maxY - minY;

        if (contentWidth <= 0 || contentHeight <= 0) {
            resetView();
            return;
        }

        double scaleX = targetPane.getWidth() / contentWidth;
        double scaleY = targetPane.getHeight() / contentHeight;
        double scale = Math.min(scaleX, scaleY);
        scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));

        double contentCenterX = (minX + maxX) / 2;
        double contentCenterY = (minY + maxY) / 2;
        double viewportCenterX = targetPane.getWidth() / 2;
        double viewportCenterY = targetPane.getHeight() / 2;

        double translateX = viewportCenterX - contentCenterX * scale;
        double translateY = viewportCenterY - contentCenterY * scale;

        // Animation pour un effet plus fluide
        animateTransform(scale, translateX, translateY);
    }

    public void zoomAt(double x, double y, double scaleFactor) {
        Point2D mousePoint = new Point2D(x, y);
        Point2D contentPoint = viewportToContent(mousePoint);

        double oldScale = transform.getScale();
        double newScale = oldScale * scaleFactor;
        transform.setScale(newScale);

        if (newScale != oldScale) {
            Point2D newMouse = contentToViewport(contentPoint);
            transform.setTranslateX(transform.getTranslateX() + (mousePoint.getX() - newMouse.getX()));
            transform.setTranslateY(transform.getTranslateY() + (mousePoint.getY() - newMouse.getY()));
        }
    }

    public void zoomIn() {
        double centerX = targetPane.getWidth() / 2;
        double centerY = targetPane.getHeight() / 2;
        zoomAt(centerX, centerY, ZOOM_FACTOR);
    }

    public void zoomOut() {
        double centerX = targetPane.getWidth() / 2;
        double centerY = targetPane.getHeight() / 2;
        zoomAt(centerX, centerY, 1/ZOOM_FACTOR);
    }
}