package com.diaggen.model.export;

import com.diaggen.model.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class PlantUMLExporter implements DiagramExporter {

    @Override
    public void export(ClassDiagram diagram, File file) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("@startuml");
            writer.println("skinparam classAttributeIconSize 0");
            writer.println("skinparam packageStyle rectangle");
            writer.println("hide circle");

            Map<String, StringBuilder> packageContents = new HashMap<>();

            Map<String, String> nestedClasses = new HashMap<>();

            for (DiagramClass diagramClass : diagram.getClasses()) {
                String className = diagramClass.getName();
                if (className.contains(".")) {
                    String parentClassName = className.substring(0, className.lastIndexOf("."));
                    String simpleClassName = className.substring(className.lastIndexOf(".") + 1);
                    nestedClasses.put(className, parentClassName);
                }
            }

            for (DiagramClass diagramClass : diagram.getClasses()) {
                String className = diagramClass.getName();
                String packageName = diagramClass.getPackageName();

                if (nestedClasses.containsKey(className)) {
                    continue;
                }

                StringBuilder packageContent = packageContents.computeIfAbsent(packageName, k -> new StringBuilder());

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

                packageContent.append(classDeclaration).append(" ").append(formatClassName(className)).append(" {\n");

                for (Member attribute : diagramClass.getAttributes()) {
                    packageContent.append("  ").append(attribute.getVisibility().getSymbol()).append(" ")
                            .append(attribute.getName()).append(" : ").append(attribute.getType()).append("\n");
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

                    packageContent.append(methodStr).append("\n");
                }

                packageContent.append("}\n\n");
            }

            for (Map.Entry<String, StringBuilder> entry : packageContents.entrySet()) {
                String packageName = entry.getKey();
                StringBuilder content = entry.getValue();

                if (!packageName.isEmpty()) {
                    writer.println("package \"" + packageName + "\" {");
                    writer.println(content.toString());
                    writer.println("}");
                } else {
                    writer.println(content.toString());
                }
            }

            for (Map.Entry<String, String> entry : nestedClasses.entrySet()) {
                String nestedClassName = entry.getKey();
                String parentClassName = entry.getValue();

                DiagramClass nestedClass = findClassByName(diagram, nestedClassName);
                if (nestedClass != null) {

                    writer.println(formatClassName(parentClassName) + " +-- " + formatClassName(nestedClassName));
                }
            }

            for (DiagramRelation relation : diagram.getRelations()) {
                String sourceClassName = formatClassName(relation.getSourceClass().getName());
                String targetClassName = formatClassName(relation.getTargetClass().getName());

                if (nestedClasses.containsKey(relation.getTargetClass().getName()) &&
                        relation.getSourceClass().getName().equals(nestedClasses.get(relation.getTargetClass().getName()))) {

                    continue;
                }

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

    private String formatClassName(String className) {


        return className.replace(".", "_");
    }

    private DiagramClass findClassByName(ClassDiagram diagram, String className) {
        for (DiagramClass diagramClass : diagram.getClasses()) {
            if (diagramClass.getName().equals(className)) {
                return diagramClass;
            }
        }
        return null;
    }
}