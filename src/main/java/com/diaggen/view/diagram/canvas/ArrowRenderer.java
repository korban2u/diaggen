package com.diaggen.view.diagram.canvas;

import com.diaggen.model.RelationType;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

public class ArrowRenderer {

    private final Group arrowGroup = new Group();
    private final Line line = new Line();
    private final Polygon arrowHead = new Polygon();

    private static final Color DEFAULT_COLOR = Color.BLACK;
    private static final Color SELECTED_COLOR = Color.web("#4a89dc");

    private double zoomScale = 1.0;
    private static final double BASE_LINE_WIDTH = 1.8;

    public ArrowRenderer() {
        line.getStyleClass().add("line");
        arrowHead.getStyleClass().add("arrow-head");

        line.setStrokeWidth(BASE_LINE_WIDTH);
        arrowHead.setStroke(DEFAULT_COLOR);

        arrowGroup.getChildren().addAll(line, arrowHead);
    }
    public void setZoomScale(double scale) {
        this.zoomScale = scale;
        updateLineThickness();
    }
    private void updateLineThickness() {
        double adjustedWidth = BASE_LINE_WIDTH / Math.sqrt(zoomScale);
        adjustedWidth = Math.min(adjustedWidth, 4.0);

        if (line.getStroke() == SELECTED_COLOR) {
            line.setStrokeWidth(adjustedWidth + 0.7);
            arrowHead.setStrokeWidth(adjustedWidth + 0.7);
        } else {
            line.setStrokeWidth(adjustedWidth);
            arrowHead.setStrokeWidth(adjustedWidth);
        }
    }

    public Group getArrowGroup() {
        return arrowGroup;
    }

    public Line getLine() {
        return line;
    }

    public Polygon getArrowHead() {
        return arrowHead;
    }

    public void updateArrow(Point2D start, Point2D end, RelationType relationType) {
        line.getStrokeDashArray().clear();
        if (relationType == RelationType.IMPLEMENTATION ||
                relationType == RelationType.DEPENDENCY) {
            line.getStrokeDashArray().addAll(10.0, 5.0);
        }

        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length == 0) {
            return;
        }

        double ux = dx / length;
        double uy = dy / length;

        line.setStartX(start.getX());
        line.setStartY(start.getY());

        arrowHead.getPoints().clear();
        switch (relationType) {
            case INHERITANCE:
            case IMPLEMENTATION:
                createInheritanceArrow(end, ux, uy);
                break;
            case ASSOCIATION:
            case DEPENDENCY:
                createAssociationArrow(end, ux, uy);
                break;
            case AGGREGATION:
                createAggregationArrow(end, ux, uy);
                break;
            case COMPOSITION:
                createCompositionArrow(end, ux, uy);
                break;
        }

        double arrowLength = getArrowLength(relationType);
        Point2D lineEnd = new Point2D(
                end.getX() - ux * arrowLength,
                end.getY() - uy * arrowLength
        );

        line.setEndX(lineEnd.getX());
        line.setEndY(lineEnd.getY());
    }

    private void createInheritanceArrow(Point2D end, double ux, double uy) {
        double perpX = -uy;
        double perpY = ux;
        double arrowSize = 14.0 / Math.sqrt(zoomScale);

        double baseX = end.getX() - ux * arrowSize;
        double baseY = end.getY() - uy * arrowSize;

        double leftX = baseX + perpX * arrowSize;
        double leftY = baseY + perpY * arrowSize;

        double rightX = baseX - perpX * arrowSize;
        double rightY = baseY - perpY * arrowSize;

        arrowHead.getPoints().addAll(
                end.getX(), end.getY(),
                leftX, leftY,
                baseX, baseY,
                rightX, rightY,
                end.getX(), end.getY()
        );

        arrowHead.setFill(Color.WHITE);
        arrowHead.setStroke(line.getStroke());
        arrowHead.setStrokeWidth(line.getStrokeWidth());
    }

    private void createAssociationArrow(Point2D end, double ux, double uy) {
        double perpX = -uy;
        double perpY = ux;
        double arrowSize = 12.0 / Math.sqrt(zoomScale);
        double arrowWidth = 6.0 / Math.sqrt(zoomScale);

        double leftX = end.getX() - ux * arrowSize + perpX * arrowWidth;
        double leftY = end.getY() - uy * arrowSize + perpY * arrowWidth;

        double rightX = end.getX() - ux * arrowSize - perpX * arrowWidth;
        double rightY = end.getY() - uy * arrowSize - perpY * arrowWidth;

        arrowHead.getPoints().addAll(
                end.getX(), end.getY(),
                leftX, leftY,
                rightX, rightY
        );

        arrowHead.setFill(line.getStroke());
        arrowHead.setStroke(line.getStroke());
    }

    private void createAggregationArrow(Point2D end, double ux, double uy) {
        createDiamondArrow(end, ux, uy, Color.WHITE);
    }

    private void createCompositionArrow(Point2D end, double ux, double uy) {
        createDiamondArrow(end, ux, uy, line.getStroke() == DEFAULT_COLOR ? Color.BLACK : (Color)line.getStroke());
    }

    private void createDiamondArrow(Point2D end, double ux, double uy, Color fillColor) {
        double perpX = -uy;
        double perpY = ux;
        double diamondLength = 16.0 / Math.sqrt(zoomScale);
        double diamondWidth = 10.0 / Math.sqrt(zoomScale);

        double midX = end.getX() - ux * diamondLength;
        double midY = end.getY() - uy * diamondLength;

        double backX = midX - ux * diamondLength;
        double backY = midY - uy * diamondLength;

        double leftX = midX + perpX * diamondWidth;
        double leftY = midY + perpY * diamondWidth;

        double rightX = midX - perpX * diamondWidth;
        double rightY = midY - perpY * diamondWidth;

        arrowHead.getPoints().addAll(
                end.getX(), end.getY(),
                leftX, leftY,
                backX, backY,
                rightX, rightY,
                end.getX(), end.getY()
        );

        arrowHead.setFill(fillColor);
        arrowHead.setStroke(line.getStroke());
        arrowHead.setStrokeWidth(line.getStrokeWidth());
    }

    private double getArrowLength(RelationType relationType) {
        double scaleFactor = 1.0 / Math.sqrt(zoomScale);

        switch (relationType) {
            case AGGREGATION:
            case COMPOSITION:
                return 32.0 * scaleFactor;
            case INHERITANCE:
            case IMPLEMENTATION:
                return 14.0 * scaleFactor;
            default:
                return 0.0;
        }
    }

    public void setSelected(boolean selected) {
        Color color = selected ? SELECTED_COLOR : DEFAULT_COLOR;
        line.setStroke(color);
        arrowHead.setStroke(color);

        if (arrowHead.getFill() != Color.WHITE) {
            arrowHead.setFill(color);
        }
        if (selected) {
            line.setStrokeWidth(BASE_LINE_WIDTH / Math.sqrt(zoomScale) + 0.7);
            arrowHead.setStrokeWidth(BASE_LINE_WIDTH / Math.sqrt(zoomScale) + 0.7);
        } else {
            line.setStrokeWidth(BASE_LINE_WIDTH / Math.sqrt(zoomScale));
            arrowHead.setStrokeWidth(BASE_LINE_WIDTH / Math.sqrt(zoomScale));
        }
    }
}