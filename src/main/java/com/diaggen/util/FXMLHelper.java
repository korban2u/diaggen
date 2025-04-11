package com.diaggen.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.net.URL;

/**
 * Classe utilitaire pour faciliter le chargement des fichiers FXML
 */
public class FXMLHelper {

    /**
     * Charge un fichier FXML et retourne son nœud racine
     * @param fxmlPath Le chemin relatif vers le fichier FXML
     * @return Le nœud racine du fichier FXML
     * @throws IOException En cas d'erreur de chargement
     */
    public static Parent loadFXML(String fxmlPath) throws IOException {
        URL location = FXMLHelper.class.getResource(fxmlPath);
        if (location == null) {
            throw new IOException("Fichier FXML non trouvé: " + fxmlPath);
        }

        FXMLLoader loader = new FXMLLoader(location);
        return loader.load();
    }

    /**
     * Charge un fichier FXML et retourne son contrôleur
     * @param fxmlPath Le chemin relatif vers le fichier FXML
     * @param <T> Le type du contrôleur
     * @return Le contrôleur du fichier FXML
     * @throws IOException En cas d'erreur de chargement
     */
    public static <T> T loadFXMLWithController(String fxmlPath) throws IOException {
        URL location = FXMLHelper.class.getResource(fxmlPath);
        if (location == null) {
            throw new IOException("Fichier FXML non trouvé: " + fxmlPath);
        }

        FXMLLoader loader = new FXMLLoader(location);
        loader.load();
        return loader.getController();
    }

    /**
     * Charge un fichier FXML et retourne à la fois son nœud racine et son contrôleur
     * @param fxmlPath Le chemin relatif vers le fichier FXML
     * @param <T> Le type du contrôleur
     * @return Un tableau de deux éléments: le nœud racine et le contrôleur
     * @throws IOException En cas d'erreur de chargement
     */
    public static <T> Object[] loadFXMLWithRoot(String fxmlPath) throws IOException {
        URL location = FXMLHelper.class.getResource(fxmlPath);
        if (location == null) {
            throw new IOException("Fichier FXML non trouvé: " + fxmlPath);
        }

        FXMLLoader loader = new FXMLLoader(location);
        Parent root = loader.load();
        T controller = loader.getController();

        return new Object[] { root, controller };
    }
}