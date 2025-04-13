package com.diaggen.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.util.Optional;

public class DiagramStore {
    private final ObservableList<Project> projects = FXCollections.observableArrayList();
    private final ObjectProperty<Project> activeProject = new SimpleObjectProperty<>();
    private final ObjectProperty<ClassDiagram> activeDiagram = new SimpleObjectProperty<>();
    private File currentProjectFile;

    public DiagramStore() {
    }

    public ObservableList<Project> getProjects() {
        return projects;
    }

    public Project getActiveProject() {
        return activeProject.get();
    }

    public void setActiveProject(Project project) {
        this.activeProject.set(project);

        if (project == null || (getActiveDiagram() != null && !project.getDiagrams().contains(getActiveDiagram()))) {
            setActiveDiagram(null);
        }
    }

    public ObjectProperty<Project> activeProjectProperty() {
        return activeProject;
    }

    public Project createNewProject(String name) {
        Project project = new Project(name);
        projects.add(project);
        return project;
    }

    public void removeProject(Project project) {
        if (project == getActiveProject()) {
            setActiveProject(null);
        }
        projects.remove(project);
    }

    public ClassDiagram getActiveDiagram() {
        return activeDiagram.get();
    }

    public void setActiveDiagram(ClassDiagram diagram) {
        this.activeDiagram.set(diagram);
    }

    public ObjectProperty<ClassDiagram> activeDiagramProperty() {
        return activeDiagram;
    }

    public ClassDiagram createNewDiagram(String name) {
        if (getActiveProject() == null) {
            return null;
        }

        ClassDiagram diagram = new ClassDiagram(name);
        getActiveProject().addDiagram(diagram);
        return diagram;
    }

    public void removeDiagram(ClassDiagram diagram) {
        if (diagram == getActiveDiagram()) {
            setActiveDiagram(null);
        }

        if (getActiveProject() != null) {
            getActiveProject().removeDiagram(diagram);
        }
    }

    public File getCurrentProjectFile() {
        return currentProjectFile;
    }

    public void setCurrentProjectFile(File file) {
        this.currentProjectFile = file;
    }

    public Optional<DiagramClass> findClassById(String id) {
        if (getActiveDiagram() == null) {
            return Optional.empty();
        }

        return getActiveDiagram().getClasses().stream()
                .filter(diagramClass -> diagramClass.getId().equals(id))
                .findFirst();
    }

    public Optional<DiagramRelation> findRelationById(String id) {
        if (getActiveDiagram() == null) {
            return Optional.empty();
        }

        return getActiveDiagram().getRelations().stream()
                .filter(relation -> relation.getId().equals(id))
                .findFirst();
    }

    public Project findProjectById(String id) {
        if (id == null) return null;

        return projects.stream()
                .filter(project -> project.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public ClassDiagram findDiagramById(String id) {
        if (id == null) return null;

        for (Project project : projects) {
            ClassDiagram diagram = project.getDiagramById(id);
            if (diagram != null) {
                return diagram;
            }
        }

        return null;
    }
}