package com.diaggen.view.diagram.canvas;

import com.diaggen.model.DiagramClass;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;


public class MiniMapView extends Pane {
    private final Pane diagramCanvas;
    private final ViewportTransform transform;
    private final Pane contentRepresentation = new Pane();
    private final Rectangle viewportRect = new Rectangle();

    private double miniMapScale = 0.1;
    private boolean isDragging = false;

    public MiniMapView(Pane diagramCanvas, ViewportTransform transform) {
        this.diagramCanvas = diagramCanvas;
        this.transform = transform;
        setPrefSize(150, 120);
        setMaxSize(150, 120);
        setBackground(new Background(new BackgroundFill(
                Color.rgb(250, 250, 250, 0.9), new CornerRadii(5), Insets.EMPTY)));
        setBorder(new Border(new BorderStroke(
                Color.rgb(180, 180, 180), BorderStrokeStyle.SOLID, new CornerRadii(5), BorderWidths.DEFAULT)));
        viewportRect.setFill(Color.rgb(100, 150, 255, 0.2));
        viewportRect.setStroke(Color.rgb(70, 130, 220));
        viewportRect.setStrokeWidth(1);
        contentRepresentation.setPadding(new Insets(5));

        getChildren().addAll(contentRepresentation, viewportRect);
        Tooltip.install(this, new Tooltip("Mini-map: Click to navigate"));
        getStyleClass().add("mini-map");
        viewportRect.getStyleClass().add("viewport-rect");
        setOnMousePressed(this::handleMousePress);
        setOnMouseDragged(this::handleMouseDrag);
        setOnMouseReleased(this::handleMouseRelease);
        transform.scaleProperty().addListener((obs, oldVal, newVal) -> updateViewportRect());
        transform.translateXProperty().addListener((obs, oldVal, newVal) -> updateViewportRect());
        transform.translateYProperty().addListener((obs, oldVal, newVal) -> updateViewportRect());
    }

    public void updateContent(Iterable<DiagramClass> classes) {
        contentRepresentation.getChildren().clear();
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        boolean hasClasses = false;

        for (DiagramClass diagramClass : classes) {
            hasClasses = true;
            double x = diagramClass.getX();
            double y = diagramClass.getY();
            double width = 150;
            double height = 100;

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
        }
        double padding = 50;
        minX -= padding;
        minY -= padding;
        maxX += padding;
        maxY += padding;
        double contentWidth = maxX - minX;
        double contentHeight = maxY - minY;

        double scaleX = (getWidth() - 10) / contentWidth;
        double scaleY = (getHeight() - 10) / contentHeight;
        miniMapScale = Math.min(scaleX, scaleY);
        for (DiagramClass diagramClass : classes) {
            double x = diagramClass.getX();
            double y = diagramClass.getY();
            double width = 150;
            double height = 100;

            Rectangle rect = new Rectangle(
                    (x - minX) * miniMapScale + 5,
                    (y - minY) * miniMapScale + 5,
                    width * miniMapScale,
                    height * miniMapScale
            );

            rect.setFill(Color.rgb(220, 220, 220));
            rect.setStroke(Color.rgb(150, 150, 150));
            rect.setStrokeWidth(0.5);
            contentRepresentation.getChildren().add(rect);
        }

        updateViewportRect();
    }

    private void updateViewportRect() {
        double contentWidth = diagramCanvas.getWidth() / transform.getScale();
        double contentHeight = diagramCanvas.getHeight() / transform.getScale();
        double viewportX = -transform.getTranslateX() / transform.getScale();
        double viewportY = -transform.getTranslateY() / transform.getScale();
        viewportRect.setX((viewportX - minX()) * miniMapScale + 5);
        viewportRect.setY((viewportY - minY()) * miniMapScale + 5);
        viewportRect.setWidth(contentWidth * miniMapScale);
        viewportRect.setHeight(contentHeight * miniMapScale);
    }

    private double minX() {
        if (contentRepresentation.getChildren().isEmpty()) {
            return 0;
        }

        double minX = Double.MAX_VALUE;
        for (Node node : contentRepresentation.getChildren()) {
            if (node instanceof Rectangle) {
                Rectangle rect = (Rectangle) node;
                minX = Math.min(minX, rect.getX() - 5);
            }
        }

        return minX / miniMapScale;
    }

    private double minY() {
        if (contentRepresentation.getChildren().isEmpty()) {
            return 0;
        }

        double minY = Double.MAX_VALUE;
        for (Node node : contentRepresentation.getChildren()) {
            if (node instanceof Rectangle) {
                Rectangle rect = (Rectangle) node;
                minY = Math.min(minY, rect.getY() - 5);
            }
        }

        return minY / miniMapScale;
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
        double contentX = (x - 5) / miniMapScale + minX();
        double contentY = (y - 5) / miniMapScale + minY();
        double viewportWidth = diagramCanvas.getWidth() / transform.getScale();
        double viewportHeight = diagramCanvas.getHeight() / transform.getScale();

        double translateX = -(contentX - viewportWidth / 2) * transform.getScale();
        double translateY = -(contentY - viewportHeight / 2) * transform.getScale();

        transform.setTranslateX(translateX);
        transform.setTranslateY(translateY);
    }
}