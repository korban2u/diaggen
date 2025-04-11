package com.diaggen.controller.command;

import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramRelation;

/**
 * Commande pour supprimer une relation du diagramme
 */
public class RemoveRelationCommand implements Command {

    private final ClassDiagram diagram;
    private final DiagramRelation relation;

    /**
     * Constructeur
     * @param diagram Le diagramme
     * @param relation La relation à supprimer
     */
    public RemoveRelationCommand(ClassDiagram diagram, DiagramRelation relation) {
        this.diagram = diagram;
        this.relation = relation;
    }

    @Override
    public void execute() {
        diagram.removeRelation(relation);
    }

    @Override
    public void undo() {
        diagram.addRelation(relation);
    }

    @Override
    public String getDescription() {
        return "Suppression de la relation " + relation.getRelationType().getDisplayName() +
                " entre " + relation.getSourceClass().getName() +
                " et " + relation.getTargetClass().getName();
    }
}