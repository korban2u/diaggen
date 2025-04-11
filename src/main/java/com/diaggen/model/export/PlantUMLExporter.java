package com.diaggen.model.export;

import com.diaggen.model.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class PlantUMLExporter implements DiagramExporter {

    @Override
    public void export(ClassDiagram diagram, File file) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("@startuml");
            writer.println("skinparam classAttributeIconSize 0");
            writer.println("skinparam packageStyle rectangle");
            writer.println("hide circle");

            for (DiagramClass diagramClass : diagram.getClasses()) {
                String classDeclaration;

                switch (diagramClass.getClassType()) {
                    case INTERFACE:
                        classDeclaration = "interface";
                        break;
                    case ABSTRACT_CLASS:
                        classDeclaration = "abstract class";
                        break;
                    case ENUM:
                        classDeclaration = "enum";
                        break;
                    default:
                        classDeclaration = "class";
                }

                writer.println(classDeclaration + " " + getFullClassName(diagramClass) + " {");

                for (Member attribute : diagramClass.getAttributes()) {
                    writer.println("  " + attribute.getVisibility().getSymbol() + " " +
                            attribute.getName() + " : " + attribute.getType());
                }

                for (Method method : diagramClass.getMethods()) {
                    StringBuilder methodStr = new StringBuilder();
                    methodStr.append("  ").append(method.getVisibility().getSymbol()).append(" ");

                    if (method.isAbstract()) {
                        methodStr.append("{abstract} ");
                    }

                    if (method.isStatic()) {
                        methodStr.append("{static} ");
                    }

                    methodStr.append(method.getName()).append("(");

                    boolean first = true;
                    for (Parameter param : method.getParameters()) {
                        if (!first) {
                            methodStr.append(", ");
                        }
                        methodStr.append(param.getName()).append(" : ").append(param.getType());
                        first = false;
                    }

                    methodStr.append(") : ").append(method.getReturnType());

                    writer.println(methodStr.toString());
                }

                writer.println("}");

                if (!diagramClass.getPackageName().isEmpty()) {
                    writer.println("package " + diagramClass.getPackageName() + " {");
                    writer.println("  " + getFullClassName(diagramClass));
                    writer.println("}");
                }
            }

            for (DiagramRelation relation : diagram.getRelations()) {
                String sourceClassName = getFullClassName(relation.getSourceClass());
                String targetClassName = getFullClassName(relation.getTargetClass());
                String sourceMultiplicity = relation.getSourceMultiplicity().isEmpty() ?
                        "" : " \"" + relation.getSourceMultiplicity() + "\"";
                String targetMultiplicity = relation.getTargetMultiplicity().isEmpty() ?
                        "" : " \"" + relation.getTargetMultiplicity() + "\"";
                String label = relation.getLabel().isEmpty() ?
                        "" : " : " + relation.getLabel();

                switch (relation.getRelationType()) {
                    case INHERITANCE:
                        writer.println(targetClassName + " <|-- " + sourceClassName);
                        break;
                    case IMPLEMENTATION:
                        writer.println(targetClassName + " <|.. " + sourceClassName);
                        break;
                    case ASSOCIATION:
                        writer.println(sourceClassName + sourceMultiplicity + " --> " +
                                targetMultiplicity + " " + targetClassName + label);
                        break;
                    case AGGREGATION:
                        writer.println(sourceClassName + sourceMultiplicity + " o-- " +
                                targetMultiplicity + " " + targetClassName + label);
                        break;
                    case COMPOSITION:
                        writer.println(sourceClassName + sourceMultiplicity + " *-- " +
                                targetMultiplicity + " " + targetClassName + label);
                        break;
                    case DEPENDENCY:
                        writer.println(sourceClassName + " ..> " + targetClassName + label);
                        break;
                }
            }

            writer.println("@enduml");
        }
    }

    private String getFullClassName(DiagramClass diagramClass) {
        return diagramClass.getName().replace(".", "_");
    }
}


