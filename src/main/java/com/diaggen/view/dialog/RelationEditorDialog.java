package com.diaggen.view.dialog;

import com.diaggen.model.DiagramClass;
import com.diaggen.model.DiagramRelation;
import com.diaggen.model.RelationType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

public class RelationEditorDialog extends Dialog<DiagramRelation> {

    private final ComboBox<DiagramClass> sourceClassComboBox;
    private final ComboBox<DiagramClass> targetClassComboBox;
    private final ComboBox<RelationType> relationTypeComboBox;
    private final TextField sourceMultiplicityField;
    private final TextField targetMultiplicityField;
    private final TextField labelField;

    private final DiagramRelation relation;

    public RelationEditorDialog(DiagramRelation relation, ObservableList<DiagramClass> classes) {
        this.relation = relation;

        setTitle(relation == null ? "Ajouter une relation" : "Modifier une relation");
        setHeaderText(relation == null ? "Créer une nouvelle relation" : "Modifier la relation");

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        sourceClassComboBox = new ComboBox<>(classes);
        targetClassComboBox = new ComboBox<>(classes);

        sourceClassComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(DiagramClass diagramClass) {
                return diagramClass != null ? diagramClass.getName() : "";
            }

            @Override
            public DiagramClass fromString(String string) {
                return null;
            }
        });

        targetClassComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(DiagramClass diagramClass) {
                return diagramClass != null ? diagramClass.getName() : "";
            }

            @Override
            public DiagramClass fromString(String string) {
                return null;
            }
        });

        relationTypeComboBox = new ComboBox<>(FXCollections.observableArrayList(RelationType.values()));
        relationTypeComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(RelationType relationType) {
                return relationType != null ? relationType.getDisplayName() : "";
            }

            @Override
            public RelationType fromString(String string) {
                return null;
            }
        });

        sourceMultiplicityField = new TextField();
        targetMultiplicityField = new TextField();
        labelField = new TextField();

        if (relation != null) {
            sourceClassComboBox.getSelectionModel().select(relation.getSourceClass());
            targetClassComboBox.getSelectionModel().select(relation.getTargetClass());
            relationTypeComboBox.getSelectionModel().select(relation.getRelationType());
            sourceMultiplicityField.setText(relation.getSourceMultiplicity());
            targetMultiplicityField.setText(relation.getTargetMultiplicity());
            labelField.setText(relation.getLabel());
        } else {
            relationTypeComboBox.getSelectionModel().select(RelationType.ASSOCIATION);
        }

        grid.add(new Label("Classe source:"), 0, 0);
        grid.add(sourceClassComboBox, 1, 0);
        grid.add(new Label("Classe cible:"), 0, 1);
        grid.add(targetClassComboBox, 1, 1);
        grid.add(new Label("Type de relation:"), 0, 2);
        grid.add(relationTypeComboBox, 1, 2);
        grid.add(new Label("Multiplicité source:"), 0, 3);
        grid.add(sourceMultiplicityField, 1, 3);
        grid.add(new Label("Multiplicité cible:"), 0, 4);
        grid.add(targetMultiplicityField, 1, 4);
        grid.add(new Label("Libellé:"), 0, 5);
        grid.add(labelField, 1, 5);

        getDialogPane().setContent(grid);

        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                DiagramClass sourceClass = sourceClassComboBox.getSelectionModel().getSelectedItem();
                DiagramClass targetClass = targetClassComboBox.getSelectionModel().getSelectedItem();
                RelationType relationType = relationTypeComboBox.getSelectionModel().getSelectedItem();

                if (sourceClass != null && targetClass != null && relationType != null) {
                    if (relation == null) {
                        return new DiagramRelation(
                                sourceClass,
                                targetClass,
                                relationType,
                                sourceMultiplicityField.getText(),
                                targetMultiplicityField.getText(),
                                labelField.getText());
                    } else {
                        relation.setSourceMultiplicity(sourceMultiplicityField.getText());
                        relation.setTargetMultiplicity(targetMultiplicityField.getText());
                        relation.setLabel(labelField.getText());
                        return relation;
                    }
                }
            }
            return null;
        });
    }
}

// Styles

