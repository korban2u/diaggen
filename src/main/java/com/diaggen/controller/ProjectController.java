package com.diaggen.controller;

import com.diaggen.controller.command.CommandManager;
import com.diaggen.event.EventBus;
import com.diaggen.event.ProjectActivatedEvent;
import com.diaggen.event.ProjectChangedEvent;
import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramStore;
import com.diaggen.model.Project;
import com.diaggen.model.persist.DiagramSerializer;
import com.diaggen.model.persist.ProjectSerializer;
import com.diaggen.util.AlertHelper;
import com.diaggen.view.dialog.DiagramImportDialog;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProjectController extends BaseController {
    private static final Logger LOGGER = Logger.getLogger(ProjectController.class.getName());
    private Window ownerWindow;
    private final EventBus eventBus = EventBus.getInstance();

    public ProjectController(DiagramStore diagramStore, CommandManager commandManager) {
        super(diagramStore, commandManager);
    }

    public void setOwnerWindow(Window ownerWindow) {
        this.ownerWindow = ownerWindow;
    }

    public Project createNewProjectWithDialog() {
        TextInputDialog dialog = new TextInputDialog("Nouveau projet");
        dialog.setTitle("Nouveau projet");
        dialog.setHeaderText("Créer un nouveau projet");
        dialog.setContentText("Nom du projet:");

        if (ownerWindow != null) {
            dialog.initOwner(ownerWindow);
        }

        Optional<String> result = dialog.showAndWait();

        if (result.isPresent()) {
            String projectName = result.get().trim();

            if (projectName.isEmpty()) {
                projectName = "Nouveau projet";
            }

            return createNewProject(projectName);
        }

        return null;
    }

    public Project createNewProject(String name) {
        LOGGER.log(Level.INFO, "Creating new project: {0}", name);

        Project project = diagramStore.createNewProject(name);

        eventBus.publish(new ProjectChangedEvent(project.getId(),
                ProjectChangedEvent.ChangeType.PROJECT_CREATED, null));

        activateProject(project, true);

        return project;
    }

    public void activateProject(Project project, boolean forceActivation) {
        if (project == null) {
            LOGGER.log(Level.WARNING, "Attempted to activate null project");
            return;
        }

        if (!forceActivation && diagramStore.getActiveProject() == project) {
            LOGGER.log(Level.INFO, "Project is already active, skipping activation: {0}", project.getName());
            return;
        }

        LOGGER.log(Level.INFO, "Activating project: {0} (ID: {1})", new Object[]{project.getName(), project.getId()});

        diagramStore.setActiveProject(project);

        eventBus.publish(new ProjectActivatedEvent(project.getId()));

        eventBus.publish(new ProjectChangedEvent(project.getId(),
                ProjectChangedEvent.ChangeType.PROJECT_ACTIVATED, null));
    }

    public void activateProject(Project project) {
        activateProject(project, false);
    }

    public void renameProject(Project project, String newName) {
        if (project == null) return;

        if (newName == null) {
            TextInputDialog dialog = new TextInputDialog(project.getName());
            dialog.setTitle("Renommer le projet");
            dialog.setHeaderText("Renommer \"" + project.getName() + "\"");
            dialog.setContentText("Nouveau nom:");

            if (ownerWindow != null) {
                dialog.initOwner(ownerWindow);
            }

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && !result.get().trim().isEmpty()) {
                newName = result.get().trim();
            } else {
                return;
            }
        }

        LOGGER.log(Level.INFO, "Renaming project {0} to {1}", new Object[]{project.getName(), newName});
        project.setName(newName);
        eventBus.publish(new ProjectChangedEvent(project.getId(),
                ProjectChangedEvent.ChangeType.PROJECT_RENAMED, null));
    }

    public void deleteProject(Project project) {
        if (project == null) return;

        LOGGER.log(Level.INFO, "Deleting project: {0}", project.getName());

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer le projet");
        alert.setHeaderText("Supprimer \"" + project.getName() + "\"");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer ce projet et tous ses diagrammes ? Cette action est irréversible.");

        if (ownerWindow != null) {
            alert.initOwner(ownerWindow);
        }

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String projectId = project.getId();
            diagramStore.removeProject(project);
            eventBus.publish(new ProjectChangedEvent(projectId,
                    ProjectChangedEvent.ChangeType.PROJECT_DELETED, null));
        }
    }

    public void saveProject() {
        File currentFile = diagramStore.getCurrentProjectFile();
        if (currentFile != null) {
            saveToFile(currentFile);
        } else {
            saveProjectAs();
        }
    }

    public void saveProjectAs() {
        if (diagramStore.getActiveProject() == null) {
            AlertHelper.showWarning("Aucun projet actif", "Il n'y a pas de projet à enregistrer.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le projet");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers DiagGen Projet (*.dgp)", "*.dgp"));

        File file = fileChooser.showSaveDialog(ownerWindow);
        if (file != null) {
            if (!file.getName().endsWith(".dgp")) {
                file = new File(file.getAbsolutePath() + ".dgp");
            }
            saveToFile(file);
            diagramStore.setCurrentProjectFile(file);
        }
    }

    public void openProject() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Ouvrir un projet");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers DiagGen Projet (*.dgp)", "*.dgp"));

        File file = fileChooser.showOpenDialog(ownerWindow);
        if (file != null) {
            try {
                ProjectSerializer serializer = new ProjectSerializer();
                Project loadedProject = serializer.deserialize(file);

                diagramStore.getProjects().add(loadedProject);
                diagramStore.setCurrentProjectFile(file);

                activateProject(loadedProject, true);

                LOGGER.log(Level.INFO, "Successfully loaded project from {0}", file.getName());
            } catch (IOException | ClassNotFoundException e) {
                LOGGER.log(Level.SEVERE, "Error loading project", e);
                AlertHelper.showError("Erreur lors de l'ouverture",
                        "Une erreur est survenue lors de l'ouverture du projet : " + e.getMessage());
            }
        }
    }

    public void importDiagramsFromProject() {
        if (diagramStore.getActiveProject() == null) {
            AlertHelper.showWarning("Aucun projet actif", "Vous devez avoir un projet actif pour importer des diagrammes.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Importer des diagrammes depuis un autre projet");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers DiagGen Projet (*.dgp)", "*.dgp"));

        File file = fileChooser.showOpenDialog(ownerWindow);
        if (file != null) {
            try {
                ProjectSerializer serializer = new ProjectSerializer();
                Project sourceProject = serializer.deserialize(file);

                if (sourceProject.getDiagrams().isEmpty()) {
                    AlertHelper.showWarning("Aucun diagramme", "Le projet source ne contient aucun diagramme à importer.");
                    return;
                }

                DiagramImportDialog dialog = new DiagramImportDialog(ownerWindow, sourceProject.getDiagrams());
                Optional<List<ClassDiagram>> result = dialog.showAndWait();

                if (result.isPresent() && !result.get().isEmpty()) {
                    for (ClassDiagram diagram : result.get()) {

                        ClassDiagram copy = createDiagramCopy(diagram);
                        diagramStore.getActiveProject().addDiagram(copy);
                    }

                    AlertHelper.showInfo("Importation réussie",
                            result.get().size() + " diagramme(s) importé(s) avec succès.");

                    eventBus.publish(new ProjectChangedEvent(diagramStore.getActiveProject().getId(),
                            ProjectChangedEvent.ChangeType.DIAGRAMS_IMPORTED, null));
                }

            } catch (IOException | ClassNotFoundException e) {
                LOGGER.log(Level.SEVERE, "Error importing diagrams", e);
                AlertHelper.showError("Erreur lors de l'importation",
                        "Une erreur est survenue lors de l'importation des diagrammes : " + e.getMessage());
            }
        }
    }

    private ClassDiagram createDiagramCopy(ClassDiagram original) {

        try {
            File tempFile = File.createTempFile("diagram_copy", ".tmp");
            new DiagramSerializer().serialize(original, tempFile);
            ClassDiagram copy = new DiagramSerializer().deserialize(tempFile);
            tempFile.delete();
            copy.setName(original.getName() + " (copie)");
            return copy;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating diagram copy", e);
            return null;
        }
    }

    private void saveToFile(File file) {
        try {
            ProjectSerializer serializer = new ProjectSerializer();
            serializer.serialize(diagramStore.getActiveProject(), file);
            LOGGER.log(Level.INFO, "Successfully saved project to {0}", file.getName());
            AlertHelper.showInfo("Sauvegarde réussie", "Le projet a été enregistré avec succès.");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving project", e);
            AlertHelper.showError("Erreur lors de l'enregistrement",
                    "Une erreur est survenue lors de l'enregistrement du projet : " + e.getMessage());
        }
    }
}