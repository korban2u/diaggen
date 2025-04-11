package com.diaggen.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.UUID;

public class Method {
    private final String id;
    private final StringProperty name;
    private final StringProperty returnType;
    private final ObservableList<Parameter> parameters;
    private final Visibility visibility;
    private final boolean isAbstract;
    private final boolean isStatic;

    public Method(String name, String returnType, List<Parameter> parameters,
                 Visibility visibility, boolean isAbstract, boolean isStatic) {
        this.id = UUID.randomUUID().toString();
        this.name = new SimpleStringProperty(name);
        this.returnType = new SimpleStringProperty(returnType);
        this.parameters = FXCollections.observableArrayList(parameters);
        this.visibility = visibility;
        this.isAbstract = isAbstract;
        this.isStatic = isStatic;
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

    public String getReturnType() {
        return returnType.get();
    }

    public StringProperty returnTypeProperty() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType.set(returnType);
    }

    public ObservableList<Parameter> getParameters() {
        return parameters;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Method method = (Method) o;
        return id.equals(method.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}


