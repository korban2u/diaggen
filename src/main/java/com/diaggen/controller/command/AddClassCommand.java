package com.diaggen.controller.command;

import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramClass;

public class AddClassCommand implements Command {

    private final ClassDiagram diagram;
    private final DiagramClass diagramClass;

        public AddClassCommand(ClassDiagram diagram, DiagramClass diagramClass) {
        this.diagram = diagram;
        this.diagramClass = diagramClass;
    }

    @Override
    public void execute() {
        diagram.addClass(diagramClass);
    }

    @Override
    public void undo() {
        diagram.removeClass(diagramClass);
    }

    @Override
    public String getDescription() {
        return "Ajout de la classe " + diagramClass.getName();
    }
}