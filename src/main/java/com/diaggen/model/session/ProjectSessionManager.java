package com.diaggen.model.session;

import com.diaggen.config.AppConfig;
import com.diaggen.model.Project;
import com.diaggen.util.AlertHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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

        public void setCurrentProject(Project project, File projectFile) {
        this.currentProject = project;
        this.currentProjectFile = projectFile;
        this.projectModified = (projectFile == null && project != null);
        if (projectFile != null && projectFile.exists()) {
            addRecentProject(projectFile.getAbsolutePath());
            LOGGER.log(Level.INFO, "Project set with file: {0}", projectFile.getAbsolutePath());
        } else if (project != null) {
            LOGGER.log(Level.INFO, "Project set without file: {0}", project.getName());
        }
    }

        public void setCurrentProjectFile(File file) {
        this.currentProjectFile = file;
        if (file != null) {
            LOGGER.log(Level.INFO, "Current project file updated: {0}", file.getAbsolutePath());
        } else {
            LOGGER.log(Level.INFO, "Current project file cleared");
        }
    }

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

        public List<String> getRecentProjects() {
        List<String> recentProjects = new ArrayList<>();
        String recentFilesStr = config.getRecentFiles();

        if (recentFilesStr != null && !recentFilesStr.isEmpty()) {
            String[] files = recentFilesStr.split(";");
            for (String file : files) {
                if (!file.trim().isEmpty()) {
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

        public String getMostRecentProject() {
        List<String> recentProjects = getRecentProjects();
        return recentProjects.isEmpty() ? null : recentProjects.get(0);
    }

        public boolean isProjectModified() {
        return projectModified;
    }

        public void markProjectAsModified() {
        this.projectModified = true;
        LOGGER.log(Level.FINE, "Project marked as modified");
    }

        public void markProjectAsSaved() {
        this.projectModified = false;
        LOGGER.log(Level.FINE, "Project marked as saved");
    }

        public Project getCurrentProject() {
        return currentProject;
    }

        public File getCurrentProjectFile() {
        return currentProjectFile;
    }

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

        public interface ProjectSaveCallback {
        boolean saveProject();
    }
}