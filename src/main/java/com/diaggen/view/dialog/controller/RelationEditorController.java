package com.diaggen.view.dialog.controller;

import com.diaggen.model.DiagramClass;
import com.diaggen.model.DiagramRelation;
import com.diaggen.model.RelationType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
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

    @FXML
    public void initialize() {
        // Configuration du ComboBox des types de relation
        relationTypeComboBox.setItems(FXCollections.observableArrayList(RelationType.values()));
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

        // Configuration des ComboBox de classes
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

    /**
     * Configure le dialogue et définit la relation à éditer
     * @param dialog Le dialogue à configurer
     * @param relation La relation à éditer (null pour création)
     * @param classes La liste des classes disponibles
     */
    public void setDialog(Dialog<DiagramRelation> dialog, DiagramRelation relation, ObservableList<DiagramClass> classes) {
        this.dialog = dialog;
        this.relation = relation;
        this.classes = classes;

        // Définir le titre du dialogue
        dialog.setTitle(relation == null ? "Ajouter une relation" : "Modifier une relation");
        dialog.setHeaderText(relation == null ? "Créer une nouvelle relation" : "Modifier la relation");

        // Configurer les ComboBox avec la liste des classes
        sourceClassComboBox.setItems(classes);
        targetClassComboBox.setItems(classes);

        // Initialiser les champs avec les valeurs de la relation existante
        if (relation != null) {
            sourceClassComboBox.getSelectionModel().select(relation.getSourceClass());
            targetClassComboBox.getSelectionModel().select(relation.getTargetClass());
            relationTypeComboBox.getSelectionModel().select(relation.getRelationType());
            sourceMultiplicityField.setText(relation.getSourceMultiplicity());
            targetMultiplicityField.setText(relation.getTargetMultiplicity());
            labelField.setText(relation.getLabel());
        } else {
            // Valeurs par défaut pour une nouvelle relation
            relationTypeComboBox.getSelectionModel().select(RelationType.ASSOCIATION);
        }

        // Configurer le convertisseur de résultat
        dialog.setResultConverter(createResultConverter());
    }

    /**
     * Crée un convertisseur de résultat pour le dialogue
     */
    private Callback<ButtonType, DiagramRelation> createResultConverter() {
        return buttonType -> {
            if (buttonType == ButtonType.OK) {
                DiagramClass sourceClass = sourceClassComboBox.getSelectionModel().getSelectedItem();
                DiagramClass targetClass = targetClassComboBox.getSelectionModel().getSelectedItem();
                RelationType relationType = relationTypeComboBox.getSelectionModel().getSelectedItem();

                if (sourceClass != null && targetClass != null && relationType != null) {
                    if (relation == null) {
                        // Créer une nouvelle relation
                        return new DiagramRelation(
                                sourceClass,
                                targetClass,
                                relationType,
                                sourceMultiplicityField.getText(),
                                targetMultiplicityField.getText(),
                                labelField.getText());
                    } else {
                        // Mettre à jour la relation existante
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
}