package com.diaggen.view.diagram.canvas;

import com.diaggen.model.DiagramClass;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;


public class NavigationManager {
    private final Pane targetPane;
    private final ViewportTransform transform;

    private final BooleanProperty panningProperty = new SimpleBooleanProperty(false);
    private double mouseAnchorX;
    private double mouseAnchorY;
    private double lastTranslateX;
    private double lastTranslateY;

    private static final double ZOOM_FACTOR = 1.2;
    private static final double MIN_SCALE = 0.1;
    private static final double MAX_SCALE = 5.0;

    private Cursor previousCursor;

    public NavigationManager(Pane targetPane, ViewportTransform transform) {
        this.targetPane = targetPane;
        this.transform = transform;

        transform.setMinScale(MIN_SCALE);
        transform.setMaxScale(MAX_SCALE);

        setupEventHandlers();
    }

    private void setupEventHandlers() {
        targetPane.addEventFilter(ScrollEvent.SCROLL, this::handleScroll);
        targetPane.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.MIDDLE) {
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
            if (event.getButton() == MouseButton.MIDDLE && isPanning()) {
                stopPanning();
                event.consume();
            }
        });
        targetPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.SPACE && !isPanning()) {
                previousCursor = targetPane.getCursor();
                startPanning(targetPane.getWidth() / 2, targetPane.getHeight() / 2);
                targetPane.setCursor(Cursor.MOVE);
                event.consume();
            }
        });

        targetPane.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.SPACE && isPanning()) {
                stopPanning();
                targetPane.setCursor(previousCursor);
                event.consume();
            }
        });
    }

    private void handleScroll(ScrollEvent event) {
        if (event.isControlDown()) {
            return;
        }

        double scaleFactor = event.getDeltaY() > 0 ? ZOOM_FACTOR : 1/ZOOM_FACTOR;

        Point2D mousePoint = new Point2D(event.getX(), event.getY());
        Point2D contentPoint = viewportToContent(mousePoint);

        double oldScale = transform.getScale();
        double newScale = oldScale * scaleFactor;
        transform.setScale(newScale);
        if (newScale != oldScale) {
            Point2D newMouse = contentToViewport(contentPoint);
            transform.setTranslateX(transform.getTranslateX() + (mousePoint.getX() - newMouse.getX()));
            transform.setTranslateY(transform.getTranslateY() + (mousePoint.getY() - newMouse.getY()));
        }

        event.consume();
    }

    private void startPanning(double x, double y) {
        mouseAnchorX = x;
        mouseAnchorY = y;
        lastTranslateX = transform.getTranslateX();
        lastTranslateY = transform.getTranslateY();
        panningProperty.set(true);
    }

    private void stopPanning() {
        panningProperty.set(false);
    }

    private boolean isPanning() {
        return panningProperty.get();
    }

    private Point2D viewportToContent(Point2D viewportPoint) {
        return transform.transformPoint(viewportPoint);
    }

    private Point2D contentToViewport(Point2D contentPoint) {
        return transform.inverseTransformPoint(contentPoint);
    }

    public void resetView() {
        transform.setScale(1.0);
        transform.setTranslateX(0);
        transform.setTranslateY(0);
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
            double nodeWidth = 200;
            double nodeHeight = 150;

            minX = Math.min(minX, nodeX);
            minY = Math.min(minY, nodeY);
            maxX = Math.max(maxX, nodeX + nodeWidth);
            maxY = Math.max(maxY, nodeY + nodeHeight);
        }

        if (!hasClasses) {
            resetView();
            return;
        }
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

        transform.setScale(scale);
        double contentCenterX = (minX + maxX) / 2;
        double contentCenterY = (minY + maxY) / 2;
        double viewportCenterX = targetPane.getWidth() / 2;
        double viewportCenterY = targetPane.getHeight() / 2;

        transform.setTranslateX(viewportCenterX - contentCenterX * scale);
        transform.setTranslateY(viewportCenterY - contentCenterY * scale);
    }

    public void zoomIn() {
        double currentScale = transform.getScale();
        double newScale = currentScale * ZOOM_FACTOR;
        double centerX = targetPane.getWidth() / 2;
        double centerY = targetPane.getHeight() / 2;
        Point2D contentPoint = viewportToContent(new Point2D(centerX, centerY));

        transform.setScale(newScale);

        Point2D newCenter = contentToViewport(contentPoint);
        transform.setTranslateX(transform.getTranslateX() + (centerX - newCenter.getX()));
        transform.setTranslateY(transform.getTranslateY() + (centerY - newCenter.getY()));
    }

    public void zoomOut() {
        double currentScale = transform.getScale();
        double newScale = currentScale / ZOOM_FACTOR;
        double centerX = targetPane.getWidth() / 2;
        double centerY = targetPane.getHeight() / 2;
        Point2D contentPoint = viewportToContent(new Point2D(centerX, centerY));

        transform.setScale(newScale);

        Point2D newCenter = contentToViewport(contentPoint);
        transform.setTranslateX(transform.getTranslateX() + (centerX - newCenter.getX()));
        transform.setTranslateY(transform.getTranslateY() + (centerY - newCenter.getY()));
    }
}