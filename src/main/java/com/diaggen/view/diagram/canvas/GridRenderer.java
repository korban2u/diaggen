package com.diaggen.view.diagram.canvas;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Classe responsable du rendu de la grille de fond du diagramme
 */
public class GridRenderer {

    private final Canvas canvas;
    private final int gridSize;

    /**
     * Constructeur
     * @param canvas Le canvas sur lequel dessiner la grille
     * @param gridSize La taille de la grille en pixels
     */
    public GridRenderer(Canvas canvas, int gridSize) {
        this.canvas = canvas;
        this.gridSize = gridSize;

        // Lier les changements de taille du canvas au redimensionnement de la grille
        canvas.widthProperty().addListener((obs, oldVal, newVal) -> drawGrid());
        canvas.heightProperty().addListener((obs, oldVal, newVal) -> drawGrid());
    }

    /**
     * Dessine la grille
     */
    public void drawGrid() {
        int width = (int) canvas.getWidth();
        int height = (int) canvas.getHeight();

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(0.5);

        // Lignes verticales
        for (int x = 0; x < width; x += gridSize) {
            gc.strokeLine(x, 0, x, height);
        }

        // Lignes horizontales
        for (int y = 0; y < height; y += gridSize) {
            gc.strokeLine(0, y, width, y);
        }
    }
}