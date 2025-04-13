package com.diaggen.view.diagram.canvas;

import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public class GridRenderer {
    private final Canvas canvas;
    private final int minorGridSize;
    private final int majorGridSize;
    private double scale = 1.0;
    private double translateX = 0;
    private double translateY = 0;
    private boolean showGrid = true;
    private boolean showCoordinates = true;
    private boolean showOriginMarker = true;

    private final Color backgroundColor = Color.rgb(252, 252, 252);
    private final Color minorLineColor = Color.rgb(240, 240, 240);
    private final Color majorLineColor = Color.rgb(225, 225, 225);
    private final Color originLineColor = Color.rgb(100, 149, 237, 0.5);
    private final Color textColor = Color.rgb(150, 150, 150);

    private int gridSpacing = 1; // Facteur d'espacement adaptatif

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

        // Optimisation de l'espacement de la grille en fonction du niveau de zoom
        if (scale < 0.3) {
            gridSpacing = 5;
        } else if (scale < 0.7) {
            gridSpacing = 2;
        } else {
            gridSpacing = 1;
        }

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

    public void setShowOriginMarker(boolean showOriginMarker) {
        this.showOriginMarker = showOriginMarker;
        drawGrid();
    }

    public void drawGrid() {
        int width = (int) canvas.getWidth();
        int height = (int) canvas.getHeight();

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);
        gc.setFill(backgroundColor);
        gc.fillRect(0, 0, width, height);

        if (!showGrid) {
            return;
        }

        Point2D topLeft = viewportToContent(new Point2D(0, 0));
        Point2D bottomRight = viewportToContent(new Point2D(width, height));

        // Taille de grille adaptée au niveau de zoom
        int visibleMinorGridSize = minorGridSize * gridSpacing;
        int visibleMajorGridSize = majorGridSize * gridSpacing;

        // Calcul des limites de la grille visible
        int startX = (int)(Math.floor(topLeft.getX() / visibleMinorGridSize) * visibleMinorGridSize);
        int startY = (int)(Math.floor(topLeft.getY() / visibleMinorGridSize) * visibleMinorGridSize);
        int endX = (int)(Math.ceil(bottomRight.getX() / visibleMinorGridSize) * visibleMinorGridSize);
        int endY = (int)(Math.ceil(bottomRight.getY() / visibleMinorGridSize) * visibleMinorGridSize);

        int startMajorX = (int)(Math.floor(topLeft.getX() / visibleMajorGridSize) * visibleMajorGridSize);
        int startMajorY = (int)(Math.floor(topLeft.getY() / visibleMajorGridSize) * visibleMajorGridSize);
        int endMajorX = (int)(Math.ceil(bottomRight.getX() / visibleMajorGridSize) * visibleMajorGridSize);
        int endMajorY = (int)(Math.ceil(bottomRight.getY() / visibleMajorGridSize) * visibleMajorGridSize);

        // Dessiner les lignes mineures
        gc.setStroke(minorLineColor);
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

        // Dessiner les lignes majeures
        gc.setStroke(majorLineColor);
        gc.setLineWidth(1.0);

        for (int x = startMajorX; x <= endMajorX; x += visibleMajorGridSize) {
            Point2D p1 = contentToViewport(new Point2D(x, topLeft.getY()));
            Point2D p2 = contentToViewport(new Point2D(x, bottomRight.getY()));
            gc.strokeLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());

            // Afficher les coordonnées X
            if (showCoordinates && scale > 0.4) {
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

            // Afficher les coordonnées Y
            if (showCoordinates && scale > 0.4) {
                gc.setFill(textColor);
                gc.setTextAlign(TextAlignment.RIGHT);
                gc.setFont(new Font("Arial", 10));
                gc.fillText(String.valueOf(y), 15, p1.getY() + 4);
            }
        }

        // Dessiner les lignes d'origine (axes X et Y)
        if (showOriginMarker) {
            Point2D origin = contentToViewport(new Point2D(0, 0));
            if (origin.getX() >= -100 && origin.getX() <= width + 100 &&
                    origin.getY() >= -100 && origin.getY() <= height + 100) {

                gc.setStroke(originLineColor);
                gc.setLineWidth(1.5);

                // Lignes d'origine avec dégradé pour effet visuel
                LinearGradient gradientX = new LinearGradient(
                        0, 0, 1, 0, true, null,
                        new Stop(0, Color.rgb(100, 149, 237, 0.7)),
                        new Stop(1, Color.rgb(100, 149, 237, 0.3))
                );

                LinearGradient gradientY = new LinearGradient(
                        0, 0, 0, 1, true, null,
                        new Stop(0, Color.rgb(100, 149, 237, 0.7)),
                        new Stop(1, Color.rgb(100, 149, 237, 0.3))
                );

                gc.setStroke(originLineColor);
                gc.strokeLine(origin.getX(), 0, origin.getX(), height);
                gc.strokeLine(0, origin.getY(), width, origin.getY());

                // Indicateur d'origine
                gc.setFill(originLineColor);
                gc.fillOval(origin.getX() - 4, origin.getY() - 4, 8, 8);

                // Légende de l'origine
                if (scale > 0.5) {
                    gc.setFill(originLineColor);
                    gc.setFont(new Font("Arial", 12));
                    gc.fillText("(0,0)", origin.getX() + 10, origin.getY() - 10);
                }
            }
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