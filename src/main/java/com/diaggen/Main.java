package com.diaggen;

import javafx.application.Application;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Point d'entrée principal de l'application DiagGen.
 * Cette classe lance directement l'application JavaFX
 * sans utiliser la méthode statique de réflexion.
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    /**
     * Méthode principale qui lance l'application DiagGen
     *
     * @param args Arguments de la ligne de commande
     */
    public static void main(String[] args) {
        LOGGER.log(Level.INFO, "Démarrage de DiagGen");

        // Lancement direct de l'application sans utiliser la réflexion
        Application.launch(DiagGenApp.class, args);

        LOGGER.log(Level.INFO, "Fin de l'exécution de DiagGen");
    }
}