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

    // Indicateur pour savoir si le type de relation a été modifié
    private boolean relationTypeChanged = false;
    // Stocker le type de relation original
    private RelationType originalRelationType;
    // Indique si l'inversion a été demandée
    private boolean inversionRequested = false;

    @FXML
    public void initialize() {
        // Configuration du ComboBox des types de relation
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

        // Ajouter un écouteur de changement de type de relation pour l'exemple
        relationTypeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (oldVal != null && newVal != null && !oldVal.equals(newVal)) {
                relationTypeChanged = true;
            }
            updateExampleArea();
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

            // Stocker le type original pour détecter les changements
            originalRelationType = relation.getRelationType();

            // Mettre à jour l'exemple
            updateExampleArea();
        } else {
            // Valeurs par défaut pour une nouvelle relation
            relationTypeComboBox.getSelectionModel().select(RelationType.ASSOCIATION);
            updateExampleArea();
        }

        // Configurer le convertisseur de résultat
        dialog.setResultConverter(createResultConverter());
    }

    public void setCommandManager(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    @FXML
    public void handleInvertRelation() {
        // Si nous avons une relation existante
        inversionRequested = true;

        // Inverser les sélections des classes
        DiagramClass source = sourceClassComboBox.getValue();
        DiagramClass target = targetClassComboBox.getValue();

        if (source != null && target != null) {
            sourceClassComboBox.setValue(target);
            targetClassComboBox.setValue(source);

            // Inverser les multiplicités
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
                        // Créer une nouvelle relation si:
                        // - C'est une nouvelle relation
                        // - L'inversion a été demandée
                        // - Le type a été modifié
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