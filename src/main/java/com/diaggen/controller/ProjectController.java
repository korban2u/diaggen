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
import com.diaggen.model.session.ProjectSessionManager;
import com.diaggen.util.AlertHelper;
import com.diaggen.view.dialog.DiagramImportDialog;
import com.diaggen.view.dialog.RecentProjectsDialog;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProjectController extends BaseController {
    private static final Logger LOGGER = Logger.getLogger(ProjectController.class.getName());
    private Window ownerWindow;
    private final EventBus eventBus = EventBus.getInstance();
    private final ProjectSessionManager sessionManager;

    // Cache des fichiers de projet pour éviter la recherche répétée
    private final Map<String, File> projectFileCache = new HashMap<>();

    public ProjectController(DiagramStore diagramStore, CommandManager commandManager) {
        super(diagramStore, commandManager);
        this.sessionManager = ProjectSessionManager.getInstance();
    }

    public void setOwnerWindow(Window ownerWindow) {
        this.ownerWindow = ownerWindow;
    }

    public Project createNewProjectWithDialog() {
        // Vérifie si un projet est en cours d'édition et propose de le sauvegarder
        if (!checkSaveCurrentProject()) {
            return null;
        }

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

            Project newProject = createNewProject(projectName);

            // Demander immédiatement où sauvegarder le nouveau projet
            saveProjectAs();

            return newProject;
        }

        return null;
    }

    public Project createNewProject(String name) {
        LOGGER.log(Level.INFO, "Creating new project: {0}", name);

        Project project = diagramStore.createNewProject(name);

        // Important: Réinitialiser le fichier de projet courant pour éviter l'écrasement accidentel
        diagramStore.setCurrentProjectFile(null);
        // Effacer également dans le session manager
        sessionManager.setCurrentProjectFile(null);
        // Et dans notre cache
        projectFileCache.remove(project.getId());

        eventBus.publish(new ProjectChangedEvent(project.getId(),
                ProjectChangedEvent.ChangeType.PROJECT_CREATED, null));

        activateProject(project, true);

        // Marquer le projet comme modifié car il est nouveau
        sessionManager.markProjectAsModified();

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

        // Vérifie si le projet actuel doit être sauvegardé avant d'activer un autre projet
        Project currentProject = diagramStore.getActiveProject();
        if (currentProject != null && sessionManager.isProjectModified()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Projet non sauvegardé");
            alert.setHeaderText("Le projet \"" + currentProject.getName() + "\" a été modifié");
            alert.setContentText("Voulez-vous enregistrer les modifications avant de continuer ?");

            if (ownerWindow != null) {
                alert.initOwner(ownerWindow);
            }

            ButtonType saveButton = new ButtonType("Enregistrer");
            ButtonType dontSaveButton = new ButtonType("Ne pas enregistrer");
            ButtonType cancelButton = ButtonType.CANCEL;

            alert.getButtonTypes().setAll(saveButton, dontSaveButton, cancelButton);

            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent()) {
                if (result.get() == saveButton) {
                    saveProject();
                } else if (result.get() == cancelButton) {
                    return;
                }
                // Pour "Ne pas enregistrer", on continue normalement
            } else {
                return; // Boîte de dialogue fermée, annuler l'activation
            }
        }

        LOGGER.log(Level.INFO, "Activating project: {0} (ID: {1})", new Object[]{project.getName(), project.getId()});

        diagramStore.setActiveProject(project);

        // Obtenir le fichier correspondant au projet
        File projectFile = getProjectFile(project);

        // Mettre à jour toutes les références au fichier de projet
        if (projectFile != null) {
            LOGGER.log(Level.INFO, "Project file found for {0}: {1}",
                    new Object[]{project.getName(), projectFile.getAbsolutePath()});
            sessionManager.setCurrentProjectFile(projectFile);
            diagramStore.setCurrentProjectFile(projectFile);
            projectFileCache.put(project.getId(), projectFile);
        } else {
            LOGGER.log(Level.INFO, "No project file found for: {0}", project.getName());
            sessionManager.setCurrentProjectFile(null);
            diagramStore.setCurrentProjectFile(null);
            projectFileCache.remove(project.getId());
        }

        sessionManager.setCurrentProject(project, projectFile);

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

        // Marquer le projet comme modifié
        sessionManager.markProjectAsModified();

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

            if (project.equals(diagramStore.getActiveProject())) {
                diagramStore.setActiveDiagram(null);
                diagramStore.setActiveProject(null);
                diagramStore.setCurrentProjectFile(null);

                // Réinitialiser le gestionnaire de session
                sessionManager.setCurrentProject(null, null);
                sessionManager.setCurrentProjectFile(null);

                // Supprimer du cache
                projectFileCache.remove(projectId);
            }

            diagramStore.removeProject(project);
            eventBus.publish(new ProjectChangedEvent(projectId,
                    ProjectChangedEvent.ChangeType.PROJECT_DELETED, null));
        }
    }

    public void saveProject() {
        Project activeProject = diagramStore.getActiveProject();
        if (activeProject == null) {
            AlertHelper.showWarning("Aucun projet actif", "Il n'y a pas de projet à enregistrer.");
            return;
        }

        // Vérifier si nous avons déjà un fichier pour ce projet
        File projectFile = getProjectFile(activeProject);

        if (projectFile != null && projectFile.exists()) {
            // Si oui, sauvegarder directement dans ce fichier
            LOGGER.log(Level.INFO, "Saving directly to existing file: {0}", projectFile.getAbsolutePath());
            saveToFile(projectFile);
        } else {
            // Sinon, demander où sauvegarder
            LOGGER.log(Level.INFO, "No file found for project, asking for save location");
            saveProjectAs();
        }
    }

    public boolean saveProjectAs() {
        if (diagramStore.getActiveProject() == null) {
            AlertHelper.showWarning("Aucun projet actif", "Il n'y a pas de projet à enregistrer.");
            return false;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le projet");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers DiagGen Projet (*.dgp)", "*.dgp"));

        // Suggestion de nom de fichier basée sur le nom du projet
        if (diagramStore.getActiveProject() != null) {
            String suggestedName = diagramStore.getActiveProject().getName().replaceAll("[^a-zA-Z0-9]", "_") + ".dgp";
            fileChooser.setInitialFileName(suggestedName);
        }

        File file = fileChooser.showSaveDialog(ownerWindow);
        if (file != null) {
            if (!file.getName().endsWith(".dgp")) {
                file = new File(file.getAbsolutePath() + ".dgp");
            }
            saveToFile(file);

            // Mettre à jour toutes les références au fichier
            diagramStore.setCurrentProjectFile(file);
            sessionManager.setCurrentProjectFile(file);
            sessionManager.setCurrentProject(diagramStore.getActiveProject(), file);

            // Mettre à jour le cache
            projectFileCache.put(diagramStore.getActiveProject().getId(), file);

            return true;
        }
        return false;
    }

    public boolean openProject() {
        // Vérifie si un projet est en cours d'édition et propose de le sauvegarder
        if (!checkSaveCurrentProject()) {
            return false;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Ouvrir un projet");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers DiagGen Projet (*.dgp)", "*.dgp"));

        File file = fileChooser.showOpenDialog(ownerWindow);
        if (file != null) {
            return openProjectFile(file);
        }
        return false;
    }

    public boolean openProjectFile(File file) {
        if (file == null || !file.exists()) {
            return false;
        }

        try {
            ProjectSerializer serializer = new ProjectSerializer();
            Project loadedProject = serializer.deserialize(file);

            diagramStore.getProjects().add(loadedProject);

            // Mettre à jour toutes les références au fichier
            diagramStore.setCurrentProjectFile(file);
            sessionManager.setCurrentProjectFile(file);
            projectFileCache.put(loadedProject.getId(), file);

            activateProject(loadedProject, true);

            // Mise à jour des projets récents
            sessionManager.addRecentProject(file.getAbsolutePath());

            // Projet non modifié car vient d'être chargé
            sessionManager.markProjectAsSaved();

            LOGGER.log(Level.INFO, "Successfully loaded project from {0}", file.getName());
            return true;
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Error loading project", e);
            AlertHelper.showError("Erreur lors de l'ouverture",
                    "Une erreur est survenue lors de l'ouverture du projet : " + e.getMessage());
            return false;
        }
    }

    public void showRecentProjects() {
        // Vérifie si un projet est en cours d'édition et propose de le sauvegarder
        if (!checkSaveCurrentProject()) {
            return;
        }

        List<String> recentProjects = sessionManager.getRecentProjects();

        if (recentProjects.isEmpty()) {
            AlertHelper.showInfo("Aucun projet récent",
                    "Aucun projet récent n'a été trouvé. Créez un nouveau projet ou ouvrez-en un existant.");
            return;
        }

        RecentProjectsDialog dialog = new RecentProjectsDialog(ownerWindow, recentProjects);
        Optional<String> result = dialog.showAndWait();

        if (result.isPresent()) {
            File projectFile = new File(result.get());
            openProjectFile(projectFile);
        }
    }

    // Méthode clé pour obtenir le fichier associé à un projet
    // Vérifiez dans cet ordre:
    // 1. Cache
    // 2. DiagramStore
    // 3. SessionManager
    // 4. Projets récents
    private File getProjectFile(Project project) {
        if (project == null) return null;

        // 1. Vérifier dans le cache
        File cachedFile = projectFileCache.get(project.getId());
        if (cachedFile != null && cachedFile.exists()) {
            LOGGER.log(Level.FINE, "Project file found in cache: {0}", cachedFile.getAbsolutePath());
            return cachedFile;
        }

        // 2. Vérifier dans DiagramStore si c'est le projet actif
        if (project.equals(diagramStore.getActiveProject())) {
            File storeFile = diagramStore.getCurrentProjectFile();
            if (storeFile != null && storeFile.exists()) {
                LOGGER.log(Level.FINE, "Project file found in DiagramStore: {0}", storeFile.getAbsolutePath());
                projectFileCache.put(project.getId(), storeFile);
                return storeFile;
            }
        }

        // 3. Vérifier dans le SessionManager si c'est le projet actif
        if (project.equals(sessionManager.getCurrentProject())) {
            File sessionFile = sessionManager.getCurrentProjectFile();
            if (sessionFile != null && sessionFile.exists()) {
                LOGGER.log(Level.FINE, "Project file found in SessionManager: {0}", sessionFile.getAbsolutePath());
                projectFileCache.put(project.getId(), sessionFile);
                return sessionFile;
            }
        }

        // 4. Chercher dans les projets récents
        List<String> recentProjects = sessionManager.getRecentProjects();
        for (String path : recentProjects) {
            try {
                File file = new File(path);
                if (file.exists()) {
                    ProjectSerializer serializer = new ProjectSerializer();
                    Project loadedProject = serializer.deserialize(file);
                    if (loadedProject.getId().equals(project.getId())) {
                        LOGGER.log(Level.FINE, "Project file found in recent projects: {0}", file.getAbsolutePath());
                        projectFileCache.put(project.getId(), file);
                        return file;
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error checking recent project: {0}", e.getMessage());
                // Continuer avec le prochain projet récent
            }
        }

        // Aucun fichier trouvé
        LOGGER.log(Level.FINE, "No file found for project: {0}", project.getName());
        return null;
    }

    public boolean checkSaveCurrentProject() {
        Project activeProject = diagramStore.getActiveProject();

        if (activeProject != null && sessionManager.isProjectModified()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Projet non sauvegardé");
            alert.setHeaderText("Le projet \"" + activeProject.getName() + "\" a été modifié");
            alert.setContentText("Voulez-vous enregistrer les modifications avant de continuer ?");

            if (ownerWindow != null) {
                alert.initOwner(ownerWindow);
            }

            ButtonType saveButton = new ButtonType("Enregistrer");
            ButtonType dontSaveButton = new ButtonType("Ne pas enregistrer");
            ButtonType cancelButton = ButtonType.CANCEL;

            alert.getButtonTypes().setAll(saveButton, dontSaveButton, cancelButton);

            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent()) {
                if (result.get() == saveButton) {
                    saveProject();
                    return true;
                } else if (result.get() == dontSaveButton) {
                    return true;
                } else {
                    return false;
                }
            }

            return false;
        }

        return true;
    }

    public boolean isProjectModified() {
        return sessionManager.isProjectModified();
    }

    public void markProjectAsModified() {
        sessionManager.markProjectAsModified();
    }

    public String getMostRecentProject() {
        return sessionManager.getMostRecentProject();
    }

    public List<String> getRecentProjects() {
        return sessionManager.getRecentProjects();
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
        Project activeProject = diagramStore.getActiveProject();
        try {
            ProjectSerializer serializer = new ProjectSerializer();
            serializer.serialize(activeProject, file);

            // Enregistrer dans les projets récents
            sessionManager.addRecentProject(file.getAbsolutePath());

            // Marquer le projet comme sauvegardé
            sessionManager.markProjectAsSaved();

            // Mettre à jour toutes les références au fichier
            diagramStore.setCurrentProjectFile(file);
            sessionManager.setCurrentProjectFile(file);
            projectFileCache.put(activeProject.getId(), file);

            LOGGER.log(Level.INFO, "Successfully saved project to {0}", file.getName());
            AlertHelper.showInfo("Sauvegarde réussie", "Le projet a été enregistré avec succès dans " + file.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving project", e);
            AlertHelper.showError("Erreur lors de l'enregistrement",
                    "Une erreur est survenue lors de l'enregistrement du projet : " + e.getMessage());
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

                    // Marquer le projet comme modifié
                    sessionManager.markProjectAsModified();
                }

            } catch (IOException | ClassNotFoundException e) {
                LOGGER.log(Level.SEVERE, "Error importing diagrams", e);
                AlertHelper.showError("Erreur lors de l'importation",
                        "Une erreur est survenue lors de l'importation des diagrammes : " + e.getMessage());
            }
        }
    }
}