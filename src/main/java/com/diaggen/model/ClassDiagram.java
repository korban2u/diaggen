package com.diaggen.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.*;


public class ClassDiagram {
    private final String id;
    private final StringProperty name;
    private final ObservableList<DiagramClass> classes;
    private final ObservableList<DiagramRelation> relations;

    public ClassDiagram(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = new SimpleStringProperty(name);
        this.classes = FXCollections.observableArrayList();
        this.relations = FXCollections.observableArrayList();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public ObservableList<DiagramClass> getClasses() {
        return classes;
    }

    public ObservableList<DiagramRelation> getRelations() {
        return relations;
    }

    public void addClass(DiagramClass diagramClass) {
        classes.add(diagramClass);
        diagramClass.setDiagramId(this.id);
    }

    public void removeClass(DiagramClass diagramClass) {
        classes.remove(diagramClass);

        relations.removeIf(relation ->
                relation.getSourceClass().equals(diagramClass) ||
                        relation.getTargetClass().equals(diagramClass));
        diagramClass.setDiagramId(null);
    }

    public void addRelation(DiagramRelation relation) {
        relations.add(relation);
        relation.setDiagramId(this.id);
    }

    public void removeRelation(DiagramRelation relation) {
        relations.remove(relation);
        relation.setDiagramId(null);
    }


    public ClassDiagram createCopy() {
        ClassDiagram copy = new ClassDiagram(getName() + " (copie)");

        Map<String, DiagramClass> originalToNewClassMap = new HashMap<>();
        for (DiagramClass originalClass : getClasses()) {
            DiagramClass classCopy = new DiagramClass(
                    originalClass.getName(),
                    originalClass.getPackageName(),
                    originalClass.getClassType()
            );
            classCopy.setX(originalClass.getX());
            classCopy.setY(originalClass.getY());

            for (Member attribute : originalClass.getAttributes()) {
                Member attributeCopy = new Member(
                        attribute.getName(),
                        attribute.getType(),
                        attribute.getVisibility()
                );
                classCopy.addAttribute(attributeCopy);
            }

            for (Method method : originalClass.getMethods()) {
                List<Parameter> parametersCopy = new ArrayList<>();
                for (Parameter parameter : method.getParameters()) {
                    parametersCopy.add(new Parameter(parameter.getName(), parameter.getType()));
                }

                Method methodCopy = new Method(
                        method.getName(),
                        method.getReturnType(),
                        parametersCopy,
                        method.getVisibility(),
                        method.isAbstract(),
                        method.isStatic()
                );
                classCopy.addMethod(methodCopy);
            }

            copy.addClass(classCopy);
            originalToNewClassMap.put(originalClass.getId(), classCopy);
        }

        for (DiagramRelation relation : getRelations()) {
            DiagramClass sourceClass = originalToNewClassMap.get(relation.getSourceClass().getId());
            DiagramClass targetClass = originalToNewClassMap.get(relation.getTargetClass().getId());

            if (sourceClass != null && targetClass != null) {
                DiagramRelation relationCopy = new DiagramRelation(
                        sourceClass,
                        targetClass,
                        relation.getRelationType(),
                        relation.getSourceMultiplicity(),
                        relation.getTargetMultiplicity(),
                        relation.getLabel()
                );
                copy.addRelation(relationCopy);
            }
        }

        return copy;
    }
}