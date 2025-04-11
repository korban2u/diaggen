package com.diaggen.model;

public enum RelationType {
    ASSOCIATION("Association", "-->"),
    AGGREGATION("Agrégation", "o-->"),
    COMPOSITION("Composition", "*-->"),
    INHERITANCE("Héritage", "--|>"),
    IMPLEMENTATION("Implémentation", "..|>"),
    DEPENDENCY("Dépendance", "..>");

    private final String displayName;
    private final String symbol;

    RelationType(String displayName, String symbol) {
        this.displayName = displayName;
        this.symbol = symbol;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSymbol() {
        return symbol;
    }
}


