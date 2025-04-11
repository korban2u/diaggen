package com.diaggen.controller.command;

import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramRelation;

public class AddRelationCommand implements Command {
    private final ClassDiagram diagram;
    private final DiagramRelation relation;

    public AddRelationCommand(ClassDiagram diagram, DiagramRelation relation) {
        this.diagram = diagram;
        this.relation = relation;
    }

    @Override
    public void execute() {
        diagram.addRelation(relation);
    }

    @Override
    public void undo() {
        diagram.removeRelation(relation);
    }
}


