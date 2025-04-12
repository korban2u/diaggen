package com.diaggen.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.util.Optional;

/**
 * Stocke et gère la collection de diagrammes de classe dans l'application.
 */
public class DiagramStore {
    private final ObservableList<ClassDiagram> diagrams;
    private ClassDiagram activeDiagram;
    private File currentFile;

    public DiagramStore() {
        diagrams = FXCollections.observableArrayList();
        createNewDiagram("Nouveau diagramme");
    }

    public ObservableList<ClassDiagram> getDiagrams() {
        return diagrams;
    }

    public ClassDiagram getActiveDiagram() {
        return activeDiagram;
    }

    public void setActiveDiagram(ClassDiagram diagram) {
        this.activeDiagram = diagram;
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public void setCurrentFile(File currentFile) {
        this.currentFile = currentFile;
    }

    public ClassDiagram createNewDiagram(String name) {
        ClassDiagram diagram = new ClassDiagram(name);
        diagrams.add(diagram);
        activeDiagram = diagram;
        return diagram;
    }

    public void removeDiagram(ClassDiagram diagram) {
        diagrams.remove(diagram);
        if (activeDiagram == diagram) {
            activeDiagram = diagrams.isEmpty() ? null : diagrams.get(0);
        }
    }

    public Optional<DiagramClass> findClassById(String id) {
        if (activeDiagram == null) return Optional.empty();

        return activeDiagram.getClasses().stream()
                .filter(c -> c.getId().equals(id))
                .findFirst();
    }

    public Optional<DiagramRelation> findRelationById(String id) {
        if (activeDiagram == null) return Optional.empty();

        return activeDiagram.getRelations().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst();
    }

    public boolean isDiagramEmpty(ClassDiagram diagram) {
        return diagram == null ||
                (diagram.getClasses().isEmpty() && diagram.getRelations().isEmpty());
    }
}