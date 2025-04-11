package com.diaggen.model;

public enum ClassType {
    CLASS("Classe"),
    INTERFACE("Interface"),
    ABSTRACT_CLASS("Classe abstraite"),
    ENUM("Énumération");

    private final String displayName;

    ClassType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}


