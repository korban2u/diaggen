package com.diaggen.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;


public class AppConfig {

    private static final String CONFIG_DIRECTORY = System.getProperty("user.home") + File.separator + ".diaggen";
    private static final String CONFIG_FILE = CONFIG_DIRECTORY + File.separator + "config.properties";

    private static final String KEY_LAST_DIRECTORY = "last.directory";
    private static final String KEY_WINDOW_WIDTH = "window.width";
    private static final String KEY_WINDOW_HEIGHT = "window.height";
    private static final String KEY_WINDOW_MAXIMIZED = "window.maximized";
    private static final String KEY_RECENT_FILES = "recent.files";

    private static AppConfig instance;
    private final Properties properties;

    private AppConfig() {
        properties = new Properties();
        load();
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    private void load() {
        try {

            Path configDir = Paths.get(CONFIG_DIRECTORY);
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }


            File configFile = new File(CONFIG_FILE);
            if (!configFile.exists()) {
                setDefaultProperties();
                save();
                return;
            }


            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la configuration: " + e.getMessage());
            setDefaultProperties();
        }
    }

    private void setDefaultProperties() {
        properties.setProperty(KEY_LAST_DIRECTORY, System.getProperty("user.home"));
        properties.setProperty(KEY_WINDOW_WIDTH, "1280");
        properties.setProperty(KEY_WINDOW_HEIGHT, "800");
        properties.setProperty(KEY_WINDOW_MAXIMIZED, "false");
        properties.setProperty(KEY_RECENT_FILES, "");
    }

    public void save() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "DiagGen Configuration");
        } catch (IOException e) {
            System.err.println("Erreur lors de l'enregistrement de la configuration: " + e.getMessage());
        }
    }

    public String getLastDirectory() {
        return properties.getProperty(KEY_LAST_DIRECTORY, System.getProperty("user.home"));
    }

    public void setLastDirectory(String directory) {
        properties.setProperty(KEY_LAST_DIRECTORY, directory);
        save();
    }

    public int getWindowWidth() {
        return Integer.parseInt(properties.getProperty(KEY_WINDOW_WIDTH, "1280"));
    }

    public void setWindowWidth(int width) {
        properties.setProperty(KEY_WINDOW_WIDTH, String.valueOf(width));
        save();
    }

    public int getWindowHeight() {
        return Integer.parseInt(properties.getProperty(KEY_WINDOW_HEIGHT, "800"));
    }

    public void setWindowHeight(int height) {
        properties.setProperty(KEY_WINDOW_HEIGHT, String.valueOf(height));
        save();
    }

    public boolean isWindowMaximized() {
        return Boolean.parseBoolean(properties.getProperty(KEY_WINDOW_MAXIMIZED, "false"));
    }

    public void setWindowMaximized(boolean maximized) {
        properties.setProperty(KEY_WINDOW_MAXIMIZED, String.valueOf(maximized));
        save();
    }

    public String getRecentFiles() {
        return properties.getProperty(KEY_RECENT_FILES, "");
    }

    public void addRecentFile(String filePath, int maxFiles) {
        String recentFiles = getRecentFiles();


        if (recentFiles.contains(filePath)) {
            recentFiles = recentFiles.replace(filePath + ";", "");
            recentFiles = recentFiles.replace(filePath, "");
        }

        // Ajouter le fichier au début de la liste
        recentFiles = filePath + (recentFiles.isEmpty() ? "" : ";" + recentFiles);

        // Limiter le nombre de fichiers récents
        String[] files = recentFiles.split(";");
        if (files.length > maxFiles) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < maxFiles; i++) {
                sb.append(files[i]).append(";");
            }
            recentFiles = sb.toString();
            if (recentFiles.endsWith(";")) {
                recentFiles = recentFiles.substring(0, recentFiles.length() - 1);
            }
        }

        properties.setProperty(KEY_RECENT_FILES, recentFiles);
        save();
    }
}