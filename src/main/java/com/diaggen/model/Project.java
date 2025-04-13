package com.diaggen.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;
import java.util.UUID;

public class Project {
    private final String id;
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> lastModified = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> created = new SimpleObjectProperty<>();
    private final ObservableList<ClassDiagram> diagrams = FXCollections.observableArrayList();

    public Project(String name) {
        this.id = UUID.randomUUID().toString();
        this.name.set(name);
        this.description.set("");
        LocalDateTime now = LocalDateTime.now();
        this.created.set(now);
        this.lastModified.set(now);
    }

    public Project(String name, String description) {
        this(name);
        this.description.set(description);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
        updateLastModified();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getDescription() {
        return description.get();
    }

    public void setDescription(String description) {
        this.description.set(description);
        updateLastModified();
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public LocalDateTime getLastModified() {
        return lastModified.get();
    }

    public ObjectProperty<LocalDateTime> lastModifiedProperty() {
        return lastModified;
    }

    public LocalDateTime getCreated() {
        return created.get();
    }

    public ObjectProperty<LocalDateTime> createdProperty() {
        return created;
    }

    public ObservableList<ClassDiagram> getDiagrams() {
        return diagrams;
    }

    public void addDiagram(ClassDiagram diagram) {
        diagrams.add(diagram);
        updateLastModified();
    }

    public void removeDiagram(ClassDiagram diagram) {
        diagrams.remove(diagram);
        updateLastModified();
    }

    public ClassDiagram getDiagramById(String id) {
        return diagrams.stream()
                .filter(diagram -> diagram.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    private void updateLastModified() {
        this.lastModified.set(LocalDateTime.now());
    }

    @Override
    public String toString() {
        return name.get();
    }
}