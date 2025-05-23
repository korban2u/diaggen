package com.diaggen.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.UUID;


public class DiagramClass {
    private final String id;
    private final StringProperty name;
    private final StringProperty packageName;
    private final ObservableList<Member> attributes;
    private final ObservableList<Method> methods;
    private final DoubleProperty x;
    private final DoubleProperty y;
    private final ObjectProperty<ClassType> classType;
    private String diagramId;

    public DiagramClass(String name, String packageName, ClassType classType) {
        this.id = UUID.randomUUID().toString();
        this.name = new SimpleStringProperty(name);
        this.packageName = new SimpleStringProperty(packageName);
        this.attributes = FXCollections.observableArrayList();
        this.methods = FXCollections.observableArrayList();
        this.x = new SimpleDoubleProperty(0);
        this.y = new SimpleDoubleProperty(0);
        this.classType = new SimpleObjectProperty<>(classType);
        this.diagramId = null;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getPackageName() {
        return packageName.get();
    }

    public void setPackageName(String packageName) {
        this.packageName.set(packageName);
    }

    public StringProperty packageNameProperty() {
        return packageName;
    }

    public ObservableList<Member> getAttributes() {
        return attributes;
    }

    public ObservableList<Method> getMethods() {
        return methods;
    }

    public double getX() {
        return x.get();
    }

    public void setX(double x) {
        this.x.set(x);
    }

    public DoubleProperty xProperty() {
        return x;
    }

    public double getY() {
        return y.get();
    }

    public void setY(double y) {
        this.y.set(y);
    }

    public DoubleProperty yProperty() {
        return y;
    }

    public ClassType getClassType() {
        return classType.get();
    }

    public void setClassType(ClassType classType) {
        this.classType.set(classType);
    }

    public ObjectProperty<ClassType> classTypeProperty() {
        return classType;
    }

    public String getDiagramId() {
        return diagramId;
    }

    public void setDiagramId(String diagramId) {
        this.diagramId = diagramId;
    }

    public void addAttribute(Member attribute) {
        attributes.add(attribute);
    }

    public void removeAttribute(Member attribute) {
        attributes.remove(attribute);
    }

    public void addMethod(Method method) {
        methods.add(method);
    }

    public void removeMethod(Method method) {
        methods.remove(method);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiagramClass that = (DiagramClass) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}