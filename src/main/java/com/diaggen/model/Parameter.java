package com.diaggen.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.UUID;

public class Parameter {
    private final String id;
    private final StringProperty name;
    private final StringProperty type;

    public Parameter(String name, String type) {
        this.id = UUID.randomUUID().toString();
        this.name = new SimpleStringProperty(name);
        this.type = new SimpleStringProperty(type);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public String getType() {
        return type.get();
    }

    public StringProperty typeProperty() {
        return type;
    }

    public void setType(String type) {
        this.type.set(type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Parameter parameter = (Parameter) o;
        return id.equals(parameter.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}


