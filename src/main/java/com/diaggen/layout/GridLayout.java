package com.diaggen.layout;

import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramClass;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GridLayout implements LayoutManager.LayoutAlgorithm {

    private static final Logger LOGGER = Logger.getLogger(GridLayout.class.getName());

    private double width = 1000;
    private double height = 1000;
    private double gridWidth = 250;
    private double gridHeight = 200;
    private double margin = 50;
    private int maxColumns = 4;

    @Override
    public void layout(ClassDiagram diagram) {
        LOGGER.log(Level.INFO, "Starting grid layout algorithm");
        if (diagram == null || diagram.getClasses().isEmpty()) {
            return;
        }

        int row = 0;
        int col = 0;

        // Calculate max columns based on available width
        maxColumns = Math.max(1, (int) ((width - 2 * margin) / gridWidth));

        for (DiagramClass diagramClass : diagram.getClasses()) {
            double newX = margin + col * gridWidth;
            double newY = margin + row * gridHeight;

            diagramClass.setX(newX);
            diagramClass.setY(newY);

            col++;
            if (col >= maxColumns) {
                col = 0;
                row++;
            }
        }

        LOGGER.log(Level.INFO, "Grid layout completed");
    }

    @Override
    public void setDimensions(double width, double height) {
        this.width = width;
        this.height = height;

        // Recalculate maxColumns based on new width
        this.maxColumns = Math.max(1, (int) ((width - 2 * margin) / gridWidth));
    }

    public void setGridSize(double gridWidth, double gridHeight) {
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;

        // Recalculate maxColumns based on new grid width
        this.maxColumns = Math.max(1, (int) ((width - 2 * margin) / gridWidth));
    }

    public void setMaxColumns(int maxColumns) {
        this.maxColumns = maxColumns;
    }
}