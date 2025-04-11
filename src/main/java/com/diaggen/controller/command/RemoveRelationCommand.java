package com.diaggen.controller.command;

import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramRelation;

public class RemoveRelationCommand implements Command {
    private final ClassDiagram diagram;
    private final DiagramRelation relation;

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
}


