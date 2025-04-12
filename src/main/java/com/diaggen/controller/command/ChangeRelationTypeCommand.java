package com.diaggen.controller.command;

import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramRelation;
import com.diaggen.model.RelationType;

/**
 * Commande pour changer le type d'une relation
 * Puisque le type de relation est final dans DiagramRelation,
 * cette commande crée une nouvelle relation et remplace l'ancienne
 */
public class ChangeRelationTypeCommand implements Command {

    private final ClassDiagram diagram;
    private final DiagramRelation oldRelation;
    private DiagramRelation newRelation;
    private final RelationType newType;

    /**
     * Constructeur
     * @param diagram Le diagramme contenant la relation
     * @param relation La relation à modifier
     * @param newType Le nouveau type de relation
     */
    public ChangeRelationTypeCommand(ClassDiagram diagram, DiagramRelation relation, RelationType newType) {
        this.diagram = diagram;
        this.oldRelation = relation;
        this.newType = newType;
    }

    @Override
    public void execute() {
        // Créer une nouvelle relation avec le nouveau type mais conserver les autres propriétés
        newRelation = new DiagramRelation(
                oldRelation.getSourceClass(),
                oldRelation.getTargetClass(),
                newType,
                oldRelation.getSourceMultiplicity(),
                oldRelation.getTargetMultiplicity(),
                oldRelation.getLabel()
        );

        // Retirer l'ancienne relation et ajouter la nouvelle
        diagram.removeRelation(oldRelation);
        diagram.addRelation(newRelation);
    }

    @Override
    public void undo() {
        // Retirer la nouvelle relation et restaurer l'ancienne
        if (newRelation != null) {
            diagram.removeRelation(newRelation);
        }
        diagram.addRelation(oldRelation);
    }

    @Override
    public String getDescription() {
        return "Changement du type de relation de " +
                oldRelation.getRelationType().getDisplayName() +
                " à " + newType.getDisplayName();
    }
}