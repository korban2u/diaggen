package com.diaggen.layout;

import com.diaggen.model.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ForceDirectedLayout implements LayoutManager.LayoutAlgorithm {

    private static final Logger LOGGER = Logger.getLogger(ForceDirectedLayout.class.getName());
    private static final double INHERITANCE_WEIGHT = 2.5;
    private static final double IMPLEMENTATION_WEIGHT = 2.0;
    private static final double COMPOSITION_WEIGHT = 1.8;
    private static final double AGGREGATION_WEIGHT = 1.5;
    private static final double ASSOCIATION_WEIGHT = 1.2;
    private static final double DEPENDENCY_WEIGHT = 1.0;
    private final double margin = 50;
    private final int iterations = 100;
    private final double k = 100.0;
    private final double gravity = 0.1;
    private final double damping = 0.9;
    private final double maxVelocity = 10.0;
    private final Random random = new Random(System.currentTimeMillis());
    private double width = 1000;
    private double height = 1000;

    @Override
    public void layout(ClassDiagram diagram) {
        LOGGER.log(Level.INFO, "Starting force-directed layout algorithm");
        if (diagram == null || diagram.getClasses().isEmpty()) {
            return;
        }
        Map<String, Vector2D> positions = initializePositions(diagram);
        Map<String, Vector2D> velocities = new HashMap<>();
        Map<String, Double> classWeights = calculateClassWeights(diagram);
        for (DiagramClass diagramClass : diagram.getClasses()) {
            velocities.put(diagramClass.getId(), new Vector2D(0, 0));
        }
        for (int i = 0; i < iterations; i++) {
            Map<String, Vector2D> forces = calculateForces(diagram, positions, classWeights);
            for (DiagramClass diagramClass : diagram.getClasses()) {
                String id = diagramClass.getId();
                Vector2D force = forces.get(id);
                Vector2D velocity = velocities.get(id);
                velocity.x = (velocity.x + force.x) * damping;
                velocity.y = (velocity.y + force.y) * damping;
                limitVelocity(velocity);
                Vector2D position = positions.get(id);
                position.x += velocity.x;
                position.y += velocity.y;
                position.x = Math.max(margin, Math.min(width - margin, position.x));
                position.y = Math.max(margin, Math.min(height - margin, position.y));
            }
        }
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
        boolean hasPositions = false;
        for (DiagramClass diagramClass : diagram.getClasses()) {
            if (diagramClass.getX() != 0 || diagramClass.getY() != 0) {
                hasPositions = true;
                break;
            }
        }
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
        for (DiagramClass diagramClass : diagram.getClasses()) {
            double weight = 1.0;
            weight += diagramClass.getAttributes().size() * 0.1;
            weight += diagramClass.getMethods().size() * 0.15;
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
        for (DiagramClass diagramClass : diagram.getClasses()) {
            forces.put(diagramClass.getId(), new Vector2D(0, 0));
        }
        for (DiagramClass class1 : diagram.getClasses()) {
            String id1 = class1.getId();
            Vector2D pos1 = positions.get(id1);
            Vector2D force = forces.get(id1);
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
                if (distance < 1) distance = 1;
                double repulsiveForce = k * k / distance;
                double weight1 = weights.get(id1);
                double weight2 = weights.get(id2);
                repulsiveForce *= Math.sqrt(weight1 * weight2);

                force.x += repulsiveForce * dx / distance;
                force.y += repulsiveForce * dy / distance;
            }
        }
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
            if (distance < 1) distance = 1;
            double attractiveForce = distance / k;
            double relationWeight = getRelationWeight(relation.getRelationType());
            attractiveForce *= relationWeight;
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
        Map<String, Set<String>> hierarchies = identifyHierarchies(diagram);
        adjustHierarchyPositions(diagram, positions, hierarchies);
        for (DiagramClass diagramClass : diagram.getClasses()) {
            String id = diagramClass.getId();
            Vector2D position = positions.get(id);
            diagramClass.setX(position.x);
            diagramClass.setY(position.y);
        }
    }

    private Map<String, Set<String>> identifyHierarchies(ClassDiagram diagram) {
        Map<String, Set<String>> hierarchies = new HashMap<>();
        for (DiagramClass diagramClass : diagram.getClasses()) {
            hierarchies.put(diagramClass.getId(), new HashSet<>());
        }
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
        double verticalSpacing = 150;

        for (DiagramClass parentClass : diagram.getClasses()) {
            String parentId = parentClass.getId();
            Set<String> children = hierarchies.get(parentId);

            if (!children.isEmpty()) {
                Vector2D parentPos = positions.get(parentId);
                double totalX = 0;
                for (String childId : children) {
                    totalX += positions.get(childId).x;
                }
                double avgX = totalX / children.size();
                parentPos.x = parentPos.x * 0.8 + avgX * 0.2;
                for (String childId : children) {
                    Vector2D childPos = positions.get(childId);
                    if (childPos.y < parentPos.y + verticalSpacing) {
                        childPos.y = parentPos.y + verticalSpacing;
                    }
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