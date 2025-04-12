package com.diaggen.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.UUID;


public class DiagramRelation {
    private final String id;
    private final DiagramClass sourceClass;
    private final DiagramClass targetClass;
    private final RelationType relationType;
    private final StringProperty sourceMultiplicity;
    private final StringProperty targetMultiplicity;
    private final StringProperty label;
    private String diagramId; // Référence à l'ID du diagramme parent

    public DiagramRelation(DiagramClass sourceClass, DiagramClass targetClass, RelationType relationType,
                           String sourceMultiplicity, String targetMultiplicity, String label) {
        this.id = UUID.randomUUID().toString();
        this.sourceClass = sourceClass;
        this.targetClass = targetClass;
        this.relationType = relationType;
        this.sourceMultiplicity = new SimpleStringProperty(sourceMultiplicity);
        this.targetMultiplicity = new SimpleStringProperty(targetMultiplicity);
        this.label = new SimpleStringProperty(label);
        this.diagramId = null;
    }

    public String getId() {
        return id;
    }

    public DiagramClass getSourceClass() {
        return sourceClass;
    }

    public DiagramClass getTargetClass() {
        return targetClass;
    }

    public RelationType getRelationType() {
        return relationType;
    }

    public String getSourceMultiplicity() {
        return sourceMultiplicity.get();
    }

    public StringProperty sourceMultiplicityProperty() {
        return sourceMultiplicity;
    }

    public void setSourceMultiplicity(String sourceMultiplicity) {
        this.sourceMultiplicity.set(sourceMultiplicity);
    }

    public String getTargetMultiplicity() {
        return targetMultiplicity.get();
    }

    public StringProperty targetMultiplicityProperty() {
        return targetMultiplicity;
    }

    public void setTargetMultiplicity(String targetMultiplicity) {
        this.targetMultiplicity.set(targetMultiplicity);
    }

    public String getLabel() {
        return label.get();
    }

    public StringProperty labelProperty() {
        return label;
    }

    public void setLabel(String label) {
        this.label.set(label);
    }

    public String getDiagramId() {
        return diagramId;
    }

    public void setDiagramId(String diagramId) {
        this.diagramId = diagramId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiagramRelation that = (DiagramRelation) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}