package com.diaggen.layout;

import com.diaggen.model.ClassDiagram;
import com.diaggen.model.ClassType;
import com.diaggen.model.DiagramClass;
import com.diaggen.model.DiagramRelation;
import com.diaggen.model.RelationType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ForceDirectedLayout implements LayoutManager.LayoutAlgorithm {

    private static final Logger LOGGER = Logger.getLogger(ForceDirectedLayout.class.getName());

    private double width = 1000;
    private double height = 1000;
    private double margin = 50;

    private int iterations = 100;
    private double k = 100.0;  // Spring constant
    private double gravity = 0.1;
    private double damping = 0.9;
    private double maxVelocity = 10.0;

    // Constants for different relation types
    private static final double INHERITANCE_WEIGHT = 2.5;
    private static final double IMPLEMENTATION_WEIGHT = 2.0;
    private static final double COMPOSITION_WEIGHT = 1.8;
    private static final double AGGREGATION_WEIGHT = 1.5;
    private static final double ASSOCIATION_WEIGHT = 1.2;
    private static final double DEPENDENCY_WEIGHT = 1.0;

    private final Random random = new Random(System.currentTimeMillis());

    @Override
    public void layout(ClassDiagram diagram) {
        LOGGER.log(Level.INFO, "Starting force-directed layout algorithm");
        if (diagram == null || diagram.getClasses().isEmpty()) {
            return;
        }

        // Initial setup
        Map<String, Vector2D> positions = initializePositions(diagram);
        Map<String, Vector2D> velocities = new HashMap<>();
        Map<String, Double> classWeights = calculateClassWeights(diagram);

        // Initialize velocities to zero
        for (DiagramClass diagramClass : diagram.getClasses()) {
            velocities.put(diagramClass.getId(), new Vector2D(0, 0));
        }

        // Run simulation for a fixed number of iterations
        for (int i = 0; i < iterations; i++) {
            Map<String, Vector2D> forces = calculateForces(diagram, positions, classWeights);

            // Update positions and velocities
            for (DiagramClass diagramClass : diagram.getClasses()) {
                String id = diagramClass.getId();
                Vector2D force = forces.get(id);
                Vector2D velocity = velocities.get(id);

                // Apply force with damping
                velocity.x = (velocity.x + force.x) * damping;
                velocity.y = (velocity.y + force.y) * damping;

                // Limit velocity
                limitVelocity(velocity);

                // Update position
                Vector2D position = positions.get(id);
                position.x += velocity.x;
                position.y += velocity.y;

                // Keep position within boundaries with margin
                position.x = Math.max(margin, Math.min(width - margin, position.x));
                position.y = Math.max(margin, Math.min(height - margin, position.y));
            }
        }

        // Apply the final positions with adjustments for hierarchies
        applyPositionsWithHierarchy(diagram, positions);

        LOGGER.log(Level.INFO, "Force-directed layout completed");
    }

    @Override
    public void setDimensions(double width, double height) {
        this.width = width;
        this.height = height;
    }

    private Map<String, Vector2D> initializePositions(ClassDiagram diagram) {
        Map<String, Vector2D> positions = new HashMap<>();

        // Check if classes already have positions
        boolean hasPositions = false;
        for (DiagramClass diagramClass : diagram.getClasses()) {
            if (diagramClass.getX() != 0 || diagramClass.getY() != 0) {
                hasPositions = true;
                break;
            }
        }

        // If some classes already have positions, use them as initial positions
        // Otherwise, place classes randomly in a circle
        double centerX = width / 2;
        double centerY = height / 2;
        double radius = Math.min(width, height) / 3;
        int count = diagram.getClasses().size();
        int index = 0;

        for (DiagramClass diagramClass : diagram.getClasses()) {
            Vector2D position;
            if (hasPositions && (diagramClass.getX() != 0 || diagramClass.getY() != 0)) {
                position = new Vector2D(diagramClass.getX(), diagramClass.getY());
            } else {
                double angle = 2 * Math.PI * index / count;
                double x = centerX + radius * Math.cos(angle);
                double y = centerY + radius * Math.sin(angle);
                position = new Vector2D(x, y);
                index++;
            }
            positions.put(diagramClass.getId(), position);
        }

        return positions;
    }

    private Map<String, Double> calculateClassWeights(ClassDiagram diagram) {
        Map<String, Double> weights = new HashMap<>();

        // Calculate weight based on number of attributes and methods
        for (DiagramClass diagramClass : diagram.getClasses()) {
            double weight = 1.0;

            // Add weight based on attributes and methods
            weight += diagramClass.getAttributes().size() * 0.1;
            weight += diagramClass.getMethods().size() * 0.15;

            // Adjust weight based on class type
            if (diagramClass.getClassType() == ClassType.ABSTRACT_CLASS) {
                weight *= 1.2;
            } else if (diagramClass.getClassType() == ClassType.INTERFACE) {
                weight *= 1.3;
            }

            weights.put(diagramClass.getId(), weight);
        }

        return weights;
    }

    private Map<String, Vector2D> calculateForces(ClassDiagram diagram, Map<String, Vector2D> positions, Map<String, Double> weights) {
        Map<String, Vector2D> forces = new HashMap<>();

        // Initialize forces to zero
        for (DiagramClass diagramClass : diagram.getClasses()) {
            forces.put(diagramClass.getId(), new Vector2D(0, 0));
        }

        // Calculate repulsive forces (nodes repel each other)
        for (DiagramClass class1 : diagram.getClasses()) {
            String id1 = class1.getId();
            Vector2D pos1 = positions.get(id1);
            Vector2D force = forces.get(id1);

            // Apply gravity towards the center
            double centerX = width / 2;
            double centerY = height / 2;
            force.x -= gravity * (pos1.x - centerX);
            force.y -= gravity * (pos1.y - centerY);

            for (DiagramClass class2 : diagram.getClasses()) {
                if (class1 == class2) continue;

                String id2 = class2.getId();
                Vector2D pos2 = positions.get(id2);

                double dx = pos1.x - pos2.x;
                double dy = pos1.y - pos2.y;
                double distance = Math.sqrt(dx * dx + dy * dy);

                // Avoid division by zero
                if (distance < 1) distance = 1;

                // Scale force inversely with distance
                double repulsiveForce = k * k / distance;

                // Apply class weights
                double weight1 = weights.get(id1);
                double weight2 = weights.get(id2);
                repulsiveForce *= Math.sqrt(weight1 * weight2);

                force.x += repulsiveForce * dx / distance;
                force.y += repulsiveForce * dy / distance;
            }
        }

        // Calculate attractive forces (relations pull connected nodes together)
        for (DiagramRelation relation : diagram.getRelations()) {
            DiagramClass sourceClass = relation.getSourceClass();
            DiagramClass targetClass = relation.getTargetClass();

            String sourceId = sourceClass.getId();
            String targetId = targetClass.getId();

            Vector2D sourcePos = positions.get(sourceId);
            Vector2D targetPos = positions.get(targetId);

            Vector2D sourceForce = forces.get(sourceId);
            Vector2D targetForce = forces.get(targetId);

            double dx = sourcePos.x - targetPos.x;
            double dy = sourcePos.y - targetPos.y;
            double distance = Math.sqrt(dx * dx + dy * dy);

            // Avoid division by zero
            if (distance < 1) distance = 1;

            // Basic attractive force
            double attractiveForce = distance / k;

            // Adjust based on relation type
            double relationWeight = getRelationWeight(relation.getRelationType());
            attractiveForce *= relationWeight;

            // Apply force
            double fx = attractiveForce * dx / distance;
            double fy = attractiveForce * dy / distance;

            sourceForce.x -= fx;
            sourceForce.y -= fy;
            targetForce.x += fx;
            targetForce.y += fy;
        }

        return forces;
    }

    private double getRelationWeight(RelationType type) {
        switch (type) {
            case INHERITANCE:
                return INHERITANCE_WEIGHT;
            case IMPLEMENTATION:
                return IMPLEMENTATION_WEIGHT;
            case COMPOSITION:
                return COMPOSITION_WEIGHT;
            case AGGREGATION:
                return AGGREGATION_WEIGHT;
            case ASSOCIATION:
                return ASSOCIATION_WEIGHT;
            case DEPENDENCY:
                return DEPENDENCY_WEIGHT;
            default:
                return 1.0;
        }
    }

    private void limitVelocity(Vector2D velocity) {
        double speed = Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y);
        if (speed > maxVelocity) {
            velocity.x = (velocity.x / speed) * maxVelocity;
            velocity.y = (velocity.y / speed) * maxVelocity;
        }
    }

    private void applyPositionsWithHierarchy(ClassDiagram diagram, Map<String, Vector2D> positions) {
        // First, identify class hierarchies
        Map<String, Set<String>> hierarchies = identifyHierarchies(diagram);

        // Adjust positions to respect hierarchies (parent classes above child classes)
        adjustHierarchyPositions(diagram, positions, hierarchies);

        // Apply final positions to diagram classes
        for (DiagramClass diagramClass : diagram.getClasses()) {
            String id = diagramClass.getId();
            Vector2D position = positions.get(id);
            diagramClass.setX(position.x);
            diagramClass.setY(position.y);
        }
    }

    private Map<String, Set<String>> identifyHierarchies(ClassDiagram diagram) {
        Map<String, Set<String>> hierarchies = new HashMap<>();

        // Initialize empty sets for each class
        for (DiagramClass diagramClass : diagram.getClasses()) {
            hierarchies.put(diagramClass.getId(), new HashSet<>());
        }

        // Process inheritance and implementation relations
        for (DiagramRelation relation : diagram.getRelations()) {
            if (relation.getRelationType() == RelationType.INHERITANCE ||
                    relation.getRelationType() == RelationType.IMPLEMENTATION) {

                String parentId = relation.getTargetClass().getId();
                String childId = relation.getSourceClass().getId();

                hierarchies.get(parentId).add(childId);
            }
        }

        return hierarchies;
    }

    private void adjustHierarchyPositions(ClassDiagram diagram, Map<String, Vector2D> positions, Map<String, Set<String>> hierarchies) {
        // For each parent-child relationship, ensure the child is below the parent
        double verticalSpacing = 150;

        for (DiagramClass parentClass : diagram.getClasses()) {
            String parentId = parentClass.getId();
            Set<String> children = hierarchies.get(parentId);

            if (!children.isEmpty()) {
                Vector2D parentPos = positions.get(parentId);

                // Calculate average horizontal position for children
                double totalX = 0;
                for (String childId : children) {
                    totalX += positions.get(childId).x;
                }
                double avgX = totalX / children.size();

                // Adjust parent position slightly towards children's average X
                parentPos.x = parentPos.x * 0.8 + avgX * 0.2;

                // Ensure children are positioned below parent
                for (String childId : children) {
                    Vector2D childPos = positions.get(childId);

                    // Adjust Y position to ensure child is below parent
                    if (childPos.y < parentPos.y + verticalSpacing) {
                        childPos.y = parentPos.y + verticalSpacing;
                    }

                    // Slightly adjust X position to be closer to parent
                    childPos.x = childPos.x * 0.8 + parentPos.x * 0.2;
                }
            }
        }
    }

    private static class Vector2D {
        double x;
        double y;

        public Vector2D(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}