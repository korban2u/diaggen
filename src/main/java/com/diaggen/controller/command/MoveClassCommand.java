package com.diaggen.controller.command;

import com.diaggen.model.DiagramClass;

/**
 * Commande pour déplacer une classe dans le diagramme
 */
public class MoveClassCommand implements Command {

    private final DiagramClass diagramClass;
    private final double oldX;
    private final double oldY;
    private final double newX;
    private final double newY;

    /**
     * Constructeur
     * @param diagramClass La classe à déplacer
     * @param oldX L'ancienne position X
     * @param oldY L'ancienne position Y
     * @param newX La nouvelle position X
     * @param newY La nouvelle position Y
     */
    public MoveClassCommand(DiagramClass diagramClass, double oldX, double oldY, double newX, double newY) {
        this.diagramClass = diagramClass;
        this.oldX = oldX;
        this.oldY = oldY;
        this.newX = newX;
        this.newY = newY;
    }

    @Override
    public void execute() {
        diagramClass.setX(newX);
        diagramClass.setY(newY);
    }

    @Override
    public void undo() {
        diagramClass.setX(oldX);
        diagramClass.setY(oldY);
    }

    @Override
    public String getDescription() {
        return "Déplacement de la classe " + diagramClass.getName();
    }
}