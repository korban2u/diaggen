package com.diaggen.controller.command;

import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramClass;
import com.diaggen.model.DiagramRelation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RemoveClassCommand implements Command {

    private final ClassDiagram diagram;
    private final DiagramClass diagramClass;
    private final List<DiagramRelation> affectedRelations = new ArrayList<>();

    public RemoveClassCommand(ClassDiagram diagram, DiagramClass diagramClass) {
        this.diagram = diagram;
        this.diagramClass = diagramClass;

        // Collecter les relations qui seront affectées par la suppression
        this.affectedRelations.addAll(diagram.getRelations().stream()
                .filter(r -> r.getSourceClass().equals(diagramClass) || r.getTargetClass().equals(diagramClass))
                .collect(Collectors.toList()));
    }

    @Override
    public void execute() {
        // Supprimer d'abord les relations associées
        for (DiagramRelation relation : affectedRelations) {
            diagram.removeRelation(relation);
        }

        // Puis supprimer la classe
        diagram.removeClass(diagramClass);
    }

    @Override
    public void undo() {
        // Ajouter d'abord la classe
        diagram.addClass(diagramClass);

        // Puis restaurer les relations
        for (DiagramRelation relation : affectedRelations) {
            diagram.addRelation(relation);
        }
    }

    @Override
    public String getDescription() {
        return "Suppression de la classe " + diagramClass.getName();
    }
}