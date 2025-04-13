package com.diaggen.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DiagramStore {
    private static final Logger LOGGER = Logger.getLogger(DiagramStore.class.getName());

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
        LOGGER.log(Level.INFO, "New project created: {0}", name);
        return project;
    }

    public void removeProject(Project project) {
        if (project == getActiveProject()) {
            setActiveProject(null);
            setCurrentProjectFile(null);
        }
        projects.remove(project);
        LOGGER.log(Level.INFO, "Project removed: {0}", project.getName());
    }

    public ClassDiagram getActiveDiagram() {
        return activeDiagram.get();
    }

    public void setActiveDiagram(ClassDiagram diagram) {
        this.activeDiagram.set(diagram);
        if (diagram != null) {
            LOGGER.log(Level.INFO, "Active diagram set: {0}", diagram.getName());
        } else {
            LOGGER.log(Level.INFO, "Active diagram cleared");
        }
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
        LOGGER.log(Level.INFO, "New diagram created: {0}", name);
        return diagram;
    }

    public void removeDiagram(ClassDiagram diagram) {
        if (diagram == getActiveDiagram()) {
            setActiveDiagram(null);
        }

        if (getActiveProject() != null) {
            getActiveProject().removeDiagram(diagram);
            LOGGER.log(Level.INFO, "Diagram removed: {0}", diagram.getName());
        }
    }

    public File getCurrentProjectFile() {
        return currentProjectFile;
    }

    public void setCurrentProjectFile(File file) {
        if (file != null) {
            LOGGER.log(Level.INFO, "Current project file set to: {0}", file.getAbsolutePath());
        } else {
            LOGGER.log(Level.INFO, "Current project file cleared");
        }
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