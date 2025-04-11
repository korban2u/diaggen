package com.diaggen.controller.command;

/**
 * Interface représentant une commande dans le pattern Command
 */
public interface Command {

    /**
     * Exécute la commande
     */
    void execute();

    /**
     * Annule la commande
     */
    void undo();

    /**
     * Obtient une description de la commande
     * @return La description de la commande
     */
    String getDescription();
}