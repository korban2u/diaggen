package com.diaggen.view.dialog.controller;

import com.diaggen.controller.command.CommandManager;
import com.diaggen.model.DiagramClass;
import com.diaggen.model.DiagramRelation;
import com.diaggen.model.RelationType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Callback;
import javafx.util.StringConverter;

public class EnhancedRelationEditorController {

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

    @FXML
    private TextArea exampleArea;

    @FXML
    private Button invertRelationButton;

    private DiagramRelation relation;
    private Dialog<DiagramRelation> dialog;
    private ObservableList<DiagramClass> classes;
    private CommandManager commandManager;

    private boolean relationTypeChanged = false;

    private RelationType originalRelationType;

    private boolean inversionRequested = false;

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
            updateExampleArea();
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

    private void updateExampleArea() {
        RelationType type = relationTypeComboBox.getValue();
        if (type != null) {
            exampleArea.setText("Class1 " + type.getSymbol() + " Class2\n\n" +
                    getRelationDescription(type));
        }
    }

    private String getRelationDescription(RelationType type) {
        switch (type) {
            case ASSOCIATION:
                return "Relation simple entre deux classes";
            case AGGREGATION:
                return "Une classe contient une référence à une autre classe (relation \"a un\")";
            case COMPOSITION:
                return "Une classe contient et est responsable d'une autre classe (relation \"fait partie de\")";
            case INHERITANCE:
                return "Une classe hérite d'une autre classe (relation \"est un\")";
            case IMPLEMENTATION:
                return "Une classe implémente une interface";
            case DEPENDENCY:
                return "Une classe utilise temporairement une autre classe";
            default:
                return "";
        }
    }

    public void setDialog(Dialog<DiagramRelation> dialog, DiagramRelation relation, ObservableList<DiagramClass> classes) {
        this.dialog = dialog;
        this.relation = relation;
        this.classes = classes;
        this.relationTypeChanged = false;
        this.inversionRequested = false;

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

            updateExampleArea();
        } else {

            relationTypeComboBox.getSelectionModel().select(RelationType.ASSOCIATION);
            updateExampleArea();
        }

        dialog.setResultConverter(createResultConverter());
    }

    public void setCommandManager(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    @FXML
    public void handleInvertRelation() {

        inversionRequested = true;

        DiagramClass source = sourceClassComboBox.getValue();
        DiagramClass target = targetClassComboBox.getValue();

        if (source != null && target != null) {
            sourceClassComboBox.setValue(target);
            targetClassComboBox.setValue(source);

            String sourceMulti = sourceMultiplicityField.getText();
            String targetMulti = targetMultiplicityField.getText();

            sourceMultiplicityField.setText(targetMulti);
            targetMultiplicityField.setText(sourceMulti);
        }
    }

    private Callback<ButtonType, DiagramRelation> createResultConverter() {
        return buttonType -> {
            if (buttonType == ButtonType.OK) {
                DiagramClass sourceClass = sourceClassComboBox.getSelectionModel().getSelectedItem();
                DiagramClass targetClass = targetClassComboBox.getSelectionModel().getSelectedItem();
                RelationType relationType = relationTypeComboBox.getSelectionModel().getSelectedItem();

                if (sourceClass != null && targetClass != null && relationType != null) {
                    if (relation == null || inversionRequested || relationTypeChanged) {




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
        };
    }

    public boolean isRelationTypeChanged() {
        return relationTypeChanged;
    }

    public boolean isInversionRequested() {
        return inversionRequested;
    }

    public RelationType getOriginalRelationType() {
        return originalRelationType;
    }
}