
package com.diaggen.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.UUID;

/**
 * Représente un diagramme de classes UML avec des références bidirectionnelles.
 */
public class ClassDiagram {
    private final String id;
    private final StringProperty name;
    private final ObservableList<DiagramClass> classes;
    private final ObservableList<DiagramRelation> relations;

    public ClassDiagram(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = new SimpleStringProperty(name);
        this.classes = FXCollections.observableArrayList();
        this.relations = FXCollections.observableArrayList();
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

    public ObservableList<DiagramClass> getClasses() {
        return classes;
    }

    public ObservableList<DiagramRelation> getRelations() {
        return relations;
    }

    public void addClass(DiagramClass diagramClass) {
        classes.add(diagramClass);
        diagramClass.setDiagramId(this.id); // Maintenir la référence bidirectionnelle
    }

    public void removeClass(DiagramClass diagramClass) {
        classes.remove(diagramClass);
        // Supprimer les relations associées
        relations.removeIf(relation ->
                relation.getSourceClass().equals(diagramClass) ||
                        relation.getTargetClass().equals(diagramClass));
        diagramClass.setDiagramId(null); // Annuler la référence
    }

    public void addRelation(DiagramRelation relation) {
        relations.add(relation);
        relation.setDiagramId(this.id); // Maintenir la référence bidirectionnelle
    }

    public void removeRelation(DiagramRelation relation) {
        relations.remove(relation);
        relation.setDiagramId(null); // Annuler la référence
    }
}