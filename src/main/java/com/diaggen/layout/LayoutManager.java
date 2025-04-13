package com.diaggen.layout;

import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramClass;
import com.diaggen.model.DiagramRelation;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LayoutManager {
    private static final Logger LOGGER = Logger.getLogger(LayoutManager.class.getName());

    private final ClassDiagram diagram;
    private LayoutAlgorithm currentAlgorithm;

    public LayoutManager(ClassDiagram diagram) {
        this.diagram = diagram;
        this.currentAlgorithm = new ForceDirectedLayout();
    }

    public void setAlgorithm(LayoutAlgorithm algorithm) {
        this.currentAlgorithm = algorithm;
    }

    public void applyLayout() {
        if (diagram == null || diagram.getClasses().isEmpty()) {
            LOGGER.log(Level.INFO, "No diagram or empty diagram to layout");
            return;
        }

        LOGGER.log(Level.INFO, "Applying layout to diagram: {0} with {1} classes and {2} relations",
                new Object[]{diagram.getName(), diagram.getClasses().size(), diagram.getRelations().size()});

        currentAlgorithm.layout(diagram);
    }

    public void applyLayout(double width, double height) {
        if (diagram == null || diagram.getClasses().isEmpty()) {
            return;
        }

        currentAlgorithm.setDimensions(width, height);
        currentAlgorithm.layout(diagram);
    }

    public interface LayoutAlgorithm {
        void layout(ClassDiagram diagram);
        void setDimensions(double width, double height);
    }
}