package com.diaggen.layout;

import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramClass;
import com.diaggen.model.DiagramRelation;
import com.diaggen.model.RelationType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HierarchicalLayout implements LayoutManager.LayoutAlgorithm {

    private static final Logger LOGGER = Logger.getLogger(HierarchicalLayout.class.getName());

    private double width = 1000;
    private double height = 1000;
    private double horizontalSpacing = 200;
    private double verticalSpacing = 150;
    private double margin = 50;

    @Override
    public void layout(ClassDiagram diagram) {
        LOGGER.log(Level.INFO, "Starting hierarchical layout algorithm");
        if (diagram == null || diagram.getClasses().isEmpty()) {
            return;
        }

        // Create a graph representation
        Map<String, Node> nodes = createNodes(diagram);

        // Identify roots (classes with no parent)
        List<Node> roots = findRoots(nodes);

        // If there are no clear roots, use classes with the most children
        if (roots.isEmpty()) {
            roots = findClassesWithMostChildren(nodes);
        }

        // Assign levels to nodes (distance from root)
        assignLevels(roots);

        // Position nodes based on levels
        positionNodes(nodes, roots);

        // Apply the final positions to diagram classes
        for (DiagramClass diagramClass : diagram.getClasses()) {
            Node node = nodes.get(diagramClass.getId());
            if (node != null) {
                diagramClass.setX(node.x);
                diagramClass.setY(node.y);
            }
        }

        LOGGER.log(Level.INFO, "Hierarchical layout completed");
    }

    @Override
    public void setDimensions(double width, double height) {
        this.width = width;
        this.height = height;
    }

    private Map<String, Node> createNodes(ClassDiagram diagram) {
        Map<String, Node> nodes = new HashMap<>();

        // Create nodes for all classes
        for (DiagramClass diagramClass : diagram.getClasses()) {
            nodes.put(diagramClass.getId(), new Node(diagramClass));
        }

        // Add parent-child relationships
        for (DiagramRelation relation : diagram.getRelations()) {
            Node source = nodes.get(relation.getSourceClass().getId());
            Node target = nodes.get(relation.getTargetClass().getId());

            if (source != null && target != null) {
                switch (relation.getRelationType()) {
                    case INHERITANCE:
                        source.parents.add(target);
                        target.children.add(source);
                        break;
                    case IMPLEMENTATION:
                        source.implementedInterfaces.add(target);
                        target.implementingClasses.add(source);
                        break;
                    case COMPOSITION:
                        source.compositions.add(target);
                        target.compositedBy.add(source);
                        break;
                    case AGGREGATION:
                        source.aggregations.add(target);
                        target.aggregatedBy.add(source);
                        break;
                    case ASSOCIATION:
                        source.associations.add(target);
                        target.associatedBy.add(source);
                        break;
                    case DEPENDENCY:
                        source.dependencies.add(target);
                        target.dependedOnBy.add(source);
                        break;
                }
            }
        }

        return nodes;
    }

    private List<Node> findRoots(Map<String, Node> nodes) {
        List<Node> roots = new ArrayList<>();

        for (Node node : nodes.values()) {
            // A root is a class that has no parents (inheritance) and is not
            // implemented by any class
            if (node.parents.isEmpty() && node.implementingClasses.isEmpty() &&
                    !node.aggregatedBy.isEmpty() && !node.compositedBy.isEmpty()) {
                roots.add(node);
            }
        }

        return roots;
    }

    private List<Node> findClassesWithMostChildren(Map<String, Node> nodes) {
        List<Node> candidates = new ArrayList<>(nodes.values());

        // Sort by number of direct children (inheritance and implementation)
        candidates.sort((a, b) -> Integer.compare(
                b.children.size() + b.implementingClasses.size(),
                a.children.size() + a.implementingClasses.size()
        ));

        // Return the top 20% nodes with most children, or at least one
        int numRoots = Math.max(1, (int)(nodes.size() * 0.2));
        return candidates.subList(0, Math.min(numRoots, candidates.size()));
    }

    private void assignLevels(List<Node> roots) {
        // Reset levels
        for (Node root : roots) {
            root.level = 0;
            assignLevelsRecursive(root, new HashSet<>());
        }
    }

    private void assignLevelsRecursive(Node node, Set<Node> visited) {
        if (visited.contains(node)) {
            return;
        }

        visited.add(node);

        // Process children (inheritance)
        for (Node child : node.children) {
            child.level = Math.max(child.level, node.level + 1);
            assignLevelsRecursive(child, visited);
        }

        // Process implementing classes
        for (Node impl : node.implementingClasses) {
            impl.level = Math.max(impl.level, node.level + 1);
            assignLevelsRecursive(impl, visited);
        }

        // Process composed classes (different level)
        for (Node comp : node.compositions) {
            comp.level = Math.max(comp.level, node.level);
            assignLevelsRecursive(comp, visited);
        }

        // Process aggregated classes (similar level)
        for (Node agg : node.aggregations) {
            agg.level = Math.max(agg.level, node.level);
            assignLevelsRecursive(agg, visited);
        }

        // Process associated classes (similar level)
        for (Node assoc : node.associations) {
            assoc.level = Math.max(assoc.level, node.level);
            assignLevelsRecursive(assoc, visited);
        }
    }

    private void positionNodes(Map<String, Node> nodes, List<Node> roots) {
        // Group nodes by level
        Map<Integer, List<Node>> levelGroups = new HashMap<>();

        for (Node node : nodes.values()) {
            levelGroups.computeIfAbsent(node.level, k -> new ArrayList<>()).add(node);
        }

        // Get maximum level
        int maxLevel = levelGroups.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);

        // Position nodes level by level
        double yPosition = margin;

        for (int level = 0; level <= maxLevel; level++) {
            List<Node> levelNodes = levelGroups.get(level);

            if (levelNodes != null && !levelNodes.isEmpty()) {
                double xPosition = margin;
                double maxHeight = 0;

                // Sort nodes to keep related nodes close to each other
                sortNodesByRelations(levelNodes);

                for (Node node : levelNodes) {
                    node.x = xPosition;
                    node.y = yPosition;

                    // Estimate node size (can be refined)
                    double width = 200;
                    double height = 120 + estimateNodeHeight(node);

                    xPosition += width + horizontalSpacing;
                    maxHeight = Math.max(maxHeight, height);
                }

                yPosition += maxHeight + verticalSpacing;
            }
        }

        // Center classes horizontally
        centerNodesHorizontally(levelGroups);

        // Adjust for parent-child alignment
        alignParentsAndChildren(nodes);
    }

    private double estimateNodeHeight(Node node) {
        // Estimate based on number of attributes and methods
        DiagramClass diagramClass = node.diagramClass;
        return diagramClass.getAttributes().size() * 20 + diagramClass.getMethods().size() * 20;
    }

    private void sortNodesByRelations(List<Node> nodes) {
        // Attempt to place related nodes next to each other
        nodes.sort((a, b) -> {
            // First sort by number of relations to show important nodes first
            int relationsA = countRelations(a);
            int relationsB = countRelations(b);
            return Integer.compare(relationsB, relationsA);
        });
    }

    private int countRelations(Node node) {
        return node.children.size() + node.parents.size() +
                node.implementingClasses.size() + node.implementedInterfaces.size() +
                node.compositions.size() + node.compositedBy.size() +
                node.aggregations.size() + node.aggregatedBy.size() +
                node.associations.size() + node.associatedBy.size() +
                node.dependencies.size() + node.dependedOnBy.size();
    }

    private void centerNodesHorizontally(Map<Integer, List<Node>> levelGroups) {
        for (List<Node> levelNodes : levelGroups.values()) {
            if (levelNodes.isEmpty()) continue;

            // Find rightmost position
            double maxX = levelNodes.stream()
                    .mapToDouble(node -> node.x)
                    .max()
                    .orElse(0);

            // Center all nodes in this level
            double offset = (width - maxX) / 2;

            if (offset > 0) {
                for (Node node : levelNodes) {
                    node.x += offset;
                }
            }
        }
    }

    private void alignParentsAndChildren(Map<String, Node> nodes) {
        // Iterate through nodes multiple times to propagate adjustments
        for (int i = 0; i < 3; i++) {
            for (Node node : nodes.values()) {
                if (!node.parents.isEmpty()) {
                    // Calculate average X position of parents
                    double avgParentX = node.parents.stream()
                            .mapToDouble(parent -> parent.x)
                            .average()
                            .orElse(node.x);

                    // Move slightly toward average parent X
                    node.x = node.x * 0.7 + avgParentX * 0.3;
                }

                if (!node.children.isEmpty()) {
                    // Calculate average X position of children
                    double avgChildX = node.children.stream()
                            .mapToDouble(child -> child.x)
                            .average()
                            .orElse(node.x);

                    // Move slightly toward average child X
                    node.x = node.x * 0.7 + avgChildX * 0.3;
                }
            }
        }
    }

    private static class Node {
        final DiagramClass diagramClass;
        double x;
        double y;
        int level = 0;

        final Set<Node> parents = new HashSet<>();
        final Set<Node> children = new HashSet<>();

        final Set<Node> implementedInterfaces = new HashSet<>();
        final Set<Node> implementingClasses = new HashSet<>();

        final Set<Node> compositions = new HashSet<>();
        final Set<Node> compositedBy = new HashSet<>();

        final Set<Node> aggregations = new HashSet<>();
        final Set<Node> aggregatedBy = new HashSet<>();

        final Set<Node> associations = new HashSet<>();
        final Set<Node> associatedBy = new HashSet<>();

        final Set<Node> dependencies = new HashSet<>();
        final Set<Node> dependedOnBy = new HashSet<>();

        public Node(DiagramClass diagramClass) {
            this.diagramClass = diagramClass;
            this.x = diagramClass.getX();
            this.y = diagramClass.getY();
        }
    }
}