package com.diaggen.model.session;

import com.diaggen.config.AppConfig;
import com.diaggen.model.Project;
import com.diaggen.util.AlertHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gestionnaire de session pour les projets.
 * Responsable du suivi des projets récents et de l'état de modification.
 */
public class ProjectSessionManager {
    private static final Logger LOGGER = Logger.getLogger(ProjectSessionManager.class.getName());
    private static final int MAX_RECENT_PROJECTS = 10;

    private static ProjectSessionManager instance;
    private final AppConfig config;
    private Project currentProject;
    private File currentProjectFile;
    private boolean projectModified = false;

    private ProjectSessionManager() {
        this.config = AppConfig.getInstance();
    }

    public static synchronized ProjectSessionManager getInstance() {
        if (instance == null) {
            instance = new ProjectSessionManager();
        }
        return instance;
    }

    /**
     * Définit le projet courant et le marque comme non modifié.
     * Si le projet est nouveau (file est null), il est explicitement marqué comme modifié
     * pour inciter à une sauvegarde.
     */
    public void setCurrentProject(Project project, File projectFile) {
        this.currentProject = project;
        this.currentProjectFile = projectFile;

        // Si on n'a pas de fichier associé, c'est un nouveau projet qui doit être sauvegardé
        this.projectModified = (projectFile == null && project != null);

        // N'ajoute le projet aux récents que si un fichier est effectivement associé
        if (projectFile != null && projectFile.exists()) {
            addRecentProject(projectFile.getAbsolutePath());
            LOGGER.log(Level.INFO, "Project set with file: {0}", projectFile.getAbsolutePath());
        } else if (project != null) {
            LOGGER.log(Level.INFO, "Project set without file: {0}", project.getName());
        }
    }

    /**
     * Définit uniquement le fichier du projet courant, sans changer le projet lui-même.
     */
    public void setCurrentProjectFile(File file) {
        this.currentProjectFile = file;
        if (file != null) {
            LOGGER.log(Level.INFO, "Current project file updated: {0}", file.getAbsolutePath());
        } else {
            LOGGER.log(Level.INFO, "Current project file cleared");
        }
    }

    /**
     * Ajoute un projet à la liste des projets récents.
     */
    public void addRecentProject(String projectPath) {
        if (projectPath != null && !projectPath.isEmpty()) {
            File file = new File(projectPath);
            if (file.exists() && file.isFile()) {
                config.addRecentFile(projectPath, MAX_RECENT_PROJECTS);
                LOGGER.log(Level.INFO, "Added to recent projects: {0}", projectPath);
            } else {
                LOGGER.log(Level.WARNING, "Tentative d'ajouter un chemin invalide aux projets récents: {0}", projectPath);
            }
        }
    }

    /**
     * Récupère la liste des projets récents.
     */
    public List<String> getRecentProjects() {
        List<String> recentProjects = new ArrayList<>();
        String recentFilesStr = config.getRecentFiles();

        if (recentFilesStr != null && !recentFilesStr.isEmpty()) {
            String[] files = recentFilesStr.split(";");
            for (String file : files) {
                if (!file.trim().isEmpty()) {
                    // Vérifier si le fichier existe toujours
                    File f = new File(file.trim());
                    if (f.exists() && f.isFile()) {
                        recentProjects.add(file.trim());
                    } else {
                        LOGGER.log(Level.INFO, "Ignoring non-existent file in recent list: {0}", file.trim());
                    }
                }
            }
        }

        return recentProjects;
    }

    /**
     * Récupère le projet le plus récent.
     */
    public String getMostRecentProject() {
        List<String> recentProjects = getRecentProjects();
        return recentProjects.isEmpty() ? null : recentProjects.get(0);
    }

    /**
     * Vérifie si le projet courant a été modifié.
     */
    public boolean isProjectModified() {
        return projectModified;
    }

    /**
     * Marque le projet comme modifié.
     */
    public void markProjectAsModified() {
        this.projectModified = true;
        LOGGER.log(Level.FINE, "Project marked as modified");
    }

    /**
     * Marque le projet comme sauvegardé (non modifié).
     */
    public void markProjectAsSaved() {
        this.projectModified = false;
        LOGGER.log(Level.FINE, "Project marked as saved");
    }

    /**
     * Récupère le projet courant.
     */
    public Project getCurrentProject() {
        return currentProject;
    }

    /**
     * Récupère le fichier du projet courant.
     */
    public File getCurrentProjectFile() {
        return currentProjectFile;
    }

    /**
     * Vérifie si le projet doit être sauvegardé et le sauvegarde si nécessaire.
     * @return true si l'opération peut continuer, false si elle doit être annulée
     */
    public boolean checkSaveBeforeAction(ProjectSaveCallback saveCallback) {
        if (currentProject != null && isProjectModified()) {
            String projectName = currentProject.getName();
            boolean result = AlertHelper.showConfirmation(
                    "Projet non sauvegardé",
                    "Le projet \"" + projectName + "\" a été modifié",
                    "Voulez-vous enregistrer les modifications avant de continuer ?"
            );

            if (result) {
                if (saveCallback != null) {
                    return saveCallback.saveProject();
                }
                return false;
            }
        }

        return true;
    }

    /**
     * Interface pour la callback de sauvegarde
     */
    public interface ProjectSaveCallback {
        boolean saveProject();
    }
}