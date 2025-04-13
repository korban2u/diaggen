package com.diaggen.service;

import com.diaggen.controller.command.CommandManager;
import com.diaggen.controller.command.MoveClassCommand;
import com.diaggen.layout.LayoutFactory;
import com.diaggen.layout.LayoutManager;
import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramClass;
import javafx.geometry.Dimension2D;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.layout.Pane;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LayoutService {

    private static final Logger LOGGER = Logger.getLogger(LayoutService.class.getName());

    private final Map<String, LayoutManager> layoutManagers = new HashMap<>();
    private final Pane diagramContainer;

    public LayoutService(Pane diagramContainer) {
        this.diagramContainer = diagramContainer;
    }

    public void arrangeClasses(ClassDiagram diagram, CommandManager commandManager) {
        if (diagram == null || diagram.getClasses().isEmpty()) {
            return;
        }

        LOGGER.log(Level.INFO, "Starting interactive class arrangement");
        List<String> choices = Arrays.asList(
                "Force-Directed (Optimal pour liens complexes)",
                "Hiérarchique (Optimal pour héritage)",
                "Grille simple (Arrangement basique)"
        );

        ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0), choices);
        dialog.setTitle("Arrangement automatique");
        dialog.setHeaderText("Choisir l'algorithme de placement");
        dialog.setContentText("Type d'arrangement:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(choice -> {
            LayoutFactory.LayoutType layoutType;

            if (choice.startsWith("Force")) {
                layoutType = LayoutFactory.LayoutType.FORCE_DIRECTED;
            } else if (choice.startsWith("Hiérarchique")) {
                layoutType = LayoutFactory.LayoutType.HIERARCHICAL;
            } else {
                layoutType = LayoutFactory.LayoutType.GRID;
            }

            applyLayoutWithCommands(diagram, layoutType, commandManager);
        });
    }

    public void applyLayoutWithCommands(ClassDiagram diagram, LayoutFactory.LayoutType layoutType,
                                        CommandManager commandManager) {
        if (diagram == null || diagram.getClasses().isEmpty()) {
            return;
        }

        LOGGER.log(Level.INFO, "Applying {0} layout to diagram {1} with command tracking",
                new Object[]{layoutType, diagram.getName()});
        commandManager.startCommandGroup("Arrangement automatique (" + layoutType.name() + ")");
        Map<DiagramClass, Point2D> originalPositions = new HashMap<>();
        for (DiagramClass diagramClass : diagram.getClasses()) {
            originalPositions.put(diagramClass, new Point2D(diagramClass.getX(), diagramClass.getY()));
        }
        applyLayout(diagram, layoutType);
        for (DiagramClass diagramClass : diagram.getClasses()) {
            Point2D original = originalPositions.get(diagramClass);
            if (original != null) {
                double oldX = original.getX();
                double oldY = original.getY();
                double newX = diagramClass.getX();
                double newY = diagramClass.getY();
                if (Math.abs(oldX - newX) > 1 || Math.abs(oldY - newY) > 1) {
                    MoveClassCommand command = new MoveClassCommand(diagramClass, oldX, oldY, newX, newY);
                    commandManager.executeCommand(command);
                }
            }
        }
        commandManager.endCommandGroup();

        LOGGER.log(Level.INFO, "Layout with commands applied successfully");
    }

    public void applyLayout(ClassDiagram diagram, LayoutFactory.LayoutType layoutType) {
        if (diagram == null || diagram.getClasses().isEmpty()) {
            LOGGER.log(Level.WARNING, "Cannot apply layout to null or empty diagram");
            return;
        }

        LOGGER.log(Level.INFO, "Applying {0} layout to diagram {1}",
                new Object[]{layoutType, diagram.getName()});
        LayoutManager layoutManager = getLayoutManager(diagram);
        layoutManager.setAlgorithm(LayoutFactory.createLayout(layoutType));
        double width = diagramContainer != null ? diagramContainer.getWidth() : 1000;
        double height = diagramContainer != null ? diagramContainer.getHeight() : 800;
        layoutManager.applyLayout(width, height);

        LOGGER.log(Level.INFO, "Layout applied successfully");
    }

    public void applyLayoutWithDimensions(ClassDiagram diagram, LayoutFactory.LayoutType layoutType,
                                          double width, double height) {
        if (diagram == null || diagram.getClasses().isEmpty()) {
            return;
        }

        LayoutManager layoutManager = getLayoutManager(diagram);
        layoutManager.setAlgorithm(LayoutFactory.createLayout(layoutType));
        layoutManager.applyLayout(width, height);
    }

    public Dimension2D calculateRequiredSpace(ClassDiagram diagram) {
        if (diagram == null || diagram.getClasses().isEmpty()) {
            return new Dimension2D(1000, 800);
        }

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        for (DiagramClass diagramClass : diagram.getClasses()) {
            double x = diagramClass.getX();
            double y = diagramClass.getY();
            double width = 200;
            double height = 120 + diagramClass.getAttributes().size() * 20 +
                    diagramClass.getMethods().size() * 20;

            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x + width);
            maxY = Math.max(maxY, y + height);
        }
        double margin = 100;
        double width = (maxX - minX) + 2 * margin;
        double height = (maxY - minY) + 2 * margin;

        return new Dimension2D(width, height);
    }

    private LayoutManager getLayoutManager(ClassDiagram diagram) {
        return layoutManagers.computeIfAbsent(
                diagram.getId(),
                id -> new LayoutManager(diagram)
        );
    }

    public void clearCachedLayouts() {
        layoutManagers.clear();
    }

    public static class Point2D {
        private final double x;
        private final double y;

        public Point2D(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }
    }
}