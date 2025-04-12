package com.diaggen.view.dialog.controller;

import com.diaggen.model.DiagramClass;
import com.diaggen.model.DiagramRelation;
import com.diaggen.model.RelationType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.util.Callback;
import javafx.util.StringConverter;

public class RelationEditorController {

    @FXML
    private ComboBox<DiagramClass> sourceClassComboBox;

    @FXML
    private ComboBox<DiagramClass> targetClassComboBox;

    @FXML
    private ComboBox<RelationType> relationTypeComboBox;

    @FXML
    private TextField sourceMultiplicityField;

    @FXML
    private TextField targetMultiplicityField;

    @FXML
    private TextField labelField;

    private DiagramRelation relation;
    private Dialog<DiagramRelation> dialog;
    private ObservableList<DiagramClass> classes;

    private boolean relationTypeChanged = false;

    private RelationType originalRelationType;

    @FXML
    public void initialize() {

        relationTypeComboBox.setItems(FXCollections.observableArrayList(RelationType.values()));
        relationTypeComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(RelationType relationType) {
                return relationType != null ? relationType.getDisplayName() + " (" + relationType.getSymbol() + ")" : "";
            }

            @Override
            public RelationType fromString(String string) {
                return null;
            }
        });

        relationTypeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (oldVal != null && newVal != null && !oldVal.equals(newVal)) {
                relationTypeChanged = true;
            }
        });

        StringConverter<DiagramClass> classConverter = new StringConverter<>() {
            @Override
            public String toString(DiagramClass diagramClass) {
                return diagramClass != null ? diagramClass.getName() : "";
            }

            @Override
            public DiagramClass fromString(String string) {
                return null;
            }
        };

        sourceClassComboBox.setConverter(classConverter);
        targetClassComboBox.setConverter(classConverter);
    }

        public void setDialog(Dialog<DiagramRelation> dialog, DiagramRelation relation, ObservableList<DiagramClass> classes) {
        this.dialog = dialog;
        this.relation = relation;
        this.classes = classes;
        this.relationTypeChanged = false;

        dialog.setTitle(relation == null ? "Ajouter une relation" : "Modifier une relation");
        dialog.setHeaderText(relation == null ? "Créer une nouvelle relation" : "Modifier la relation");

        sourceClassComboBox.setItems(classes);
        targetClassComboBox.setItems(classes);

        if (relation != null) {
            sourceClassComboBox.getSelectionModel().select(relation.getSourceClass());
            targetClassComboBox.getSelectionModel().select(relation.getTargetClass());
            relationTypeComboBox.getSelectionModel().select(relation.getRelationType());
            sourceMultiplicityField.setText(relation.getSourceMultiplicity());
            targetMultiplicityField.setText(relation.getTargetMultiplicity());
            labelField.setText(relation.getLabel());

            originalRelationType = relation.getRelationType();
        } else {

            relationTypeComboBox.getSelectionModel().select(RelationType.ASSOCIATION);
        }

        dialog.setResultConverter(createResultConverter());
    }

        private Callback<ButtonType, DiagramRelation> createResultConverter() {
        return buttonType -> {
            if (buttonType == ButtonType.OK) {
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


                        if (relationTypeChanged) {



                            return new DiagramRelation(
                                    sourceClass,
                                    targetClass,
                                    relationType,
                                    sourceMultiplicityField.getText(),
                                    targetMultiplicityField.getText(),
                                    labelField.getText());
                        }

                        return relation;
                    }
                }
            }
            return null;
        };
    }

        public boolean isRelationTypeChanged() {
        return relationTypeChanged;
    }

        public RelationType getOriginalRelationType() {
        return originalRelationType;
    }
}