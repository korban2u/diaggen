package com.diaggen.controller.command;

import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramRelation;
import com.diaggen.model.RelationType;
import com.diaggen.view.diagram.DiagramCanvas;


public class ChangeRelationTypeCommand implements Command {

    private final ClassDiagram diagram;
    private final DiagramRelation oldRelation;
    private DiagramRelation newRelation;
    private final RelationType newType;
    private final DiagramCanvas diagramCanvas; // Référence au canvas pour la mise à jour

        public ChangeRelationTypeCommand(ClassDiagram diagram, DiagramRelation relation, RelationType newType, DiagramCanvas diagramCanvas) {
        this.diagram = diagram;
        this.oldRelation = relation;
        this.newType = newType;
        this.diagramCanvas = diagramCanvas;
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

        // Actualiser l'affichage du canvas et sélectionner la nouvelle relation
        if (diagramCanvas != null) {
            diagramCanvas.refresh();
            diagramCanvas.selectRelation(newRelation);
        }
    }

    @Override
    public void undo() {
        // Retirer la nouvelle relation et restaurer l'ancienne
        if (newRelation != null) {
            diagram.removeRelation(newRelation);
        }
        diagram.addRelation(oldRelation);

        // Actualiser l'affichage du canvas et sélectionner l'ancienne relation
        if (diagramCanvas != null) {
            diagramCanvas.refresh();
            diagramCanvas.selectRelation(oldRelation);
        }
    }

    @Override
    public String getDescription() {
        return "Changement du type de relation de " +
                oldRelation.getRelationType().getDisplayName() +
                " à " + newType.getDisplayName();
    }
}