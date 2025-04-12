package com.diaggen.view.diagram.canvas;

import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

DiagramCanvaspublic class GridRenderer {
    private final Canvas canvas;
    private final int minorGridSize;
    private final int majorGridSize;
    private double scale = 1.0;
    private double translateX = 0;
    private double translateY = 0;
    private boolean showGrid = true;
    private boolean showCoordinates = true;
DiagramCanvas    private final Color minorLineColor = Color.rgb(240, 240, 240);
    private final Color majorLineColor = Color.rgb(220, 220, 220);
    private final Color originLineColor = Color.rgb(200, 200, 200, 0.7);
    private final Color textColor = Color.rgb(150, 150, 150);

    public GridRenderer(Canvas canvas, int minorGridSize, int majorGridSize) {
        this.canvas = canvas;
        this.minorGridSize = minorGridSize;
        this.majorGridSize = majorGridSize;

        canvas.widthProperty().addListener((obs, oldVal, newVal) -> drawGrid());
        canvas.heightProperty().addListener((obs, oldVal, newVal) -> drawGrid());
    }

    public void setTransform(double scale, double translateX, double translateY) {
        this.scale = scale;
        this.translateX = translateX;
        this.translateY = translateY;
        drawGrid();
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
        drawGrid();
    }

    public void setShowCoordinates(boolean showCoordinates) {
        this.showCoordinates = showCoordinates;
        drawGrid();
    }

    public void drawGrid() {
        int width = (int) canvas.getWidth();
        int height = (int) canvas.getHeight();

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        if (!showGrid) {
            return;
        }
DiagramCanvas        Point2D topLeft = viewportToContent(new Point2D(0, 0));
        Point2D bottomRight = viewportToContent(new Point2D(width, height));
DiagramCanvas        double adjustedMinorSize = Math.max(minorGridSize * scale, 5);
        double adjustedMajorSize = Math.max(majorGridSize * scale, 50);
DiagramCanvas        int visibleMinorGridSize = minorGridSize;
        int visibleMajorGridSize = majorGridSize;

        if (scale < 0.5) {
            visibleMinorGridSize = majorGridSize;
            visibleMajorGridSize = majorGridSize * 5;
        } else if (scale > 2.0) {
            visibleMinorGridSize = minorGridSize / 2;
        }
DiagramCanvas        double minorOffsetX = (translateX % (visibleMinorGridSize * scale)) / scale;
        double minorOffsetY = (translateY % (visibleMinorGridSize * scale)) / scale;
        double majorOffsetX = (translateX % (visibleMajorGridSize * scale)) / scale;
        double majorOffsetY = (translateY % (visibleMajorGridSize * scale)) / scale;
DiagramCanvas        int startX = (int)(topLeft.getX() / visibleMinorGridSize) * visibleMinorGridSize;
        int startY = (int)(topLeft.getY() / visibleMinorGridSize) * visibleMinorGridSize;
        int endX = (int)(bottomRight.getX() / visibleMinorGridSize + 1) * visibleMinorGridSize;
        int endY = (int)(bottomRight.getY() / visibleMinorGridSize + 1) * visibleMinorGridSize;

        int startMajorX = (int)(topLeft.getX() / visibleMajorGridSize) * visibleMajorGridSize;
        int startMajorY = (int)(topLeft.getY() / visibleMajorGridSize) * visibleMajorGridSize;
        int endMajorX = (int)(bottomRight.getX() / visibleMajorGridSize + 1) * visibleMajorGridSize;
        int endMajorY = (int)(bottomRight.getY() / visibleMajorGridSize + 1) * visibleMajorGridSize;
DiagramCanvas        gc.setStroke(minorLineColor);
        gc.setLineWidth(0.5);

        for (int x = startX; x <= endX; x += visibleMinorGridSize) {
            Point2D p1 = contentToViewport(new Point2D(x, topLeft.getY()));
            Point2D p2 = contentToViewport(new Point2D(x, bottomRight.getY()));
            gc.strokeLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
        }

        for (int y = startY; y <= endY; y += visibleMinorGridSize) {
            Point2D p1 = contentToViewport(new Point2D(topLeft.getX(), y));
            Point2D p2 = contentToViewport(new Point2D(bottomRight.getX(), y));
            gc.strokeLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
        }
DiagramCanvas        gc.setStroke(majorLineColor);
        gc.setLineWidth(1.0);

        for (int x = startMajorX; x <= endMajorX; x += visibleMajorGridSize) {
            Point2D p1 = contentToViewport(new Point2D(x, topLeft.getY()));
            Point2D p2 = contentToViewport(new Point2D(x, bottomRight.getY()));
            gc.strokeLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
DiagramCanvas            if (showCoordinates && scale > 0.4) {
                gc.setFill(textColor);
                gc.setTextAlign(TextAlignment.CENTER);
                gc.setFont(new Font("Arial", 10));
                gc.fillText(String.valueOf(x), p1.getX(), height - 5);
            }
        }

        for (int y = startMajorY; y <= endMajorY; y += visibleMajorGridSize) {
            Point2D p1 = contentToViewport(new Point2D(topLeft.getX(), y));
            Point2D p2 = contentToViewport(new Point2D(bottomRight.getX(), y));
            gc.strokeLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
DiagramCanvas            if (showCoordinates && scale > 0.4) {
                gc.setFill(textColor);
                gc.setTextAlign(TextAlignment.RIGHT);
                gc.setFont(new Font("Arial", 10));
                gc.fillText(String.valueOf(y), 15, p1.getY() + 4);
            }
        }
DiagramCanvas        Point2D origin = contentToViewport(new Point2D(0, 0));
        if (origin.getX() >= 0 && origin.getX() <= width &&
                origin.getY() >= 0 && origin.getY() <= height) {

            gc.setStroke(originLineColor);
            gc.setLineWidth(1.5);
DiagramCanvas            gc.strokeLine(origin.getX(), 0, origin.getX(), height);
DiagramCanvas            gc.strokeLine(0, origin.getY(), width, origin.getY());
DiagramCanvas            gc.setFill(originLineColor);
            gc.fillOval(origin.getX() - 3, origin.getY() - 3, 6, 6);
        }
    }

    private Point2D viewportToContent(Point2D viewportPoint) {
        return new Point2D(
                (viewportPoint.getX() - translateX) / scale,
                (viewportPoint.getY() - translateY) / scale
        );
    }

    private Point2D contentToViewport(Point2D contentPoint) {
        return new Point2D(
                contentPoint.getX() * scale + translateX,
                contentPoint.getY() * scale + translateY
        );
    }
}