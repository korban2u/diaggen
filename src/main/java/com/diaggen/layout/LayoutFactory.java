package com.diaggen.layout;

public class LayoutFactory {

    public static LayoutManager.LayoutAlgorithm createLayout(LayoutType type) {
        switch (type) {
            case FORCE_DIRECTED:
                return new ForceDirectedLayout();
            case HIERARCHICAL:
                return new HierarchicalLayout();
            case GRID:
                return new GridLayout();
            default:
                return new ForceDirectedLayout();
        }
    }

    public enum LayoutType {
        FORCE_DIRECTED,
        HIERARCHICAL,
        GRID
    }
}