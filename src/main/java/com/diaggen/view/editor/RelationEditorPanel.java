package com.diaggen.view.editor;

import com.diaggen.controller.command.ChangeRelationTypeCommand;
import com.diaggen.controller.command.CommandManager;
import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramRelation;
import com.diaggen.model.RelationType;
import com.diaggen.view.diagram.DiagramCanvas;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Panneau d'édition amélioré pour les relations, avec mise à jour instantanée
 * lorsque le type de relation est modifié.
 */
public class RelationEditorPanel extends VBox {

    private DiagramRelation relation;
    private CommandManager commandManager;
    private ClassDiagram diagram;
    private DiagramCanvas diagramCanvas;

    // Champs d'édition
    private Label sourceClassLabel;
    private Label targetClassLabel;
    private ComboBox<RelationType> relationTypeComboBox;
    private TextField sourceMultiplicityField;
    private TextField targetMultiplicityField;
    private TextField labelField;

    /**
     * Constructeur complet
     * @param commandManager Le gestionnaire de commandes
     * @param diagram Le diagramme
     * @param diagramCanvas Le canvas (pour les mises à jour d'affichage)
     */
    public RelationEditorPanel(CommandManager commandManager, ClassDiagram diagram, DiagramCanvas diagramCanvas) {
        this.commandManager = commandManager;
        this.diagram = diagram;
        this.diagramCanvas = diagramCanvas;

        setSpacing(15);
        setPadding(new Insets(0, 5, 0, 5));

        createGeneralSection();
    }

    /**
     * Constructeur simplifié pour compatibilité
     */
    public RelationEditorPanel() {
        this(null, null, null);
    }

    private void createGeneralSection() {
        TitledPane generalSection = new TitledPane();
        generalSection.setText("Propriétés de la relation");
        generalSection.setCollapsible(true);
        generalSection.setExpanded(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        // Classes source et cible
        Label sourceTitleLabel = new Label("Classe source:");
        sourceClassLabel = new Label();
        sourceClassLabel.setStyle("-fx-font-weight: bold;");
        grid.add(sourceTitleLabel, 0, 0);
        grid.add(sourceClassLabel, 1, 0);

        Label targetTitleLabel = new Label("Classe cible:");
        targetClassLabel = new Label();
        targetClassLabel.setStyle("-fx-font-weight: bold;");
        grid.add(targetTitleLabel, 0, 1);
        grid.add(targetClassLabel, 1, 1);

        // Type de relation
        Label typeLabel = new Label("Type de relation:");
        relationTypeComboBox = new ComboBox<>(FXCollections.observableArrayList(RelationType.values()));
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
        relationTypeComboBox.setMaxWidth(Double.MAX_VALUE);
        relationTypeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (relation != null && newVal != null && oldVal != null && !oldVal.equals(newVal)) {
                // Utiliser la commande améliorée pour changer le type de relation
                if (commandManager != null && diagram != null && diagramCanvas != null) {
                    // Créer et exécuter la commande pour changer le type de relation
                    ChangeRelationTypeCommand command = new ChangeRelationTypeCommand(
                            diagram, relation, newVal, diagramCanvas);
                    commandManager.executeCommand(command);

                    // Mettre à jour la référence à la relation (puisque c'est un nouvel objet)
                    // La dernière relation ajoutée au diagramme est celle qui remplace l'originale
                    if (!diagram.getRelations().isEmpty()) {
                        relation = diagram.getRelations().get(diagram.getRelations().size() - 1);
                    }
                }
            }
        });
        grid.add(typeLabel, 0, 2);
        grid.add(relationTypeComboBox, 1, 2);

        // Multiplicité source
        Label sourceMultiplicityLabel = new Label("Multiplicité source:");
        sourceMultiplicityField = new TextField();
        sourceMultiplicityField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (relation != null) {
                relation.setSourceMultiplicity(newVal);
                // Actualiser l'affichage après modification
                if (diagramCanvas != null) {
                    diagramCanvas.refresh();
                }
            }
        });
        grid.add(sourceMultiplicityLabel, 0, 3);
        grid.add(sourceMultiplicityField, 1, 3);

        // Multiplicité cible
        Label targetMultiplicityLabel = new Label("Multiplicité cible:");
        targetMultiplicityField = new TextField();
        targetMultiplicityField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (relation != null) {
                relation.setTargetMultiplicity(newVal);
                // Actualiser l'affichage après modification
                if (diagramCanvas != null) {
                    diagramCanvas.refresh();
                }
            }
        });
        grid.add(targetMultiplicityLabel, 0, 4);
        grid.add(targetMultiplicityField, 1, 4);

        // Libellé
        Label labelTextLabel = new Label("Libellé:");
        labelField = new TextField();
        labelField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (relation != null) {
                relation.setLabel(newVal);
                // Actualiser l'affichage après modification
                if (diagramCanvas != null) {
                    diagramCanvas.refresh();
                }
            }
        });
        grid.add(labelTextLabel, 0, 5);
        grid.add(labelField, 1, 5);

        // Exemple visuel du type de relation
        Label exampleLabel = new Label("Exemple:");
        TextArea exampleArea = new TextArea();
        exampleArea.setEditable(false);
        exampleArea.setPrefRowCount(3);
        exampleArea.setWrapText(true);
        exampleArea.setStyle("-fx-font-family: monospace;");

        relationTypeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                exampleArea.setText("Class1 " + newVal.getSymbol() + " Class2\n\n" +
                        getRelationDescription(newVal));
            }
        });

        grid.add(exampleLabel, 0, 6);
        grid.add(exampleArea, 1, 6);

        // Boutons d'action
        Button inverseButton = new Button("Inverser la relation");
        inverseButton.setMaxWidth(Double.MAX_VALUE);
        inverseButton.setOnAction(e -> handleInverseRelation());
        grid.add(inverseButton, 0, 7, 2, 1);

        generalSection.setContent(grid);
        getChildren().add(generalSection);
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

    public void setRelation(DiagramRelation relation) {
        this.relation = relation;

        if (relation != null) {
            // Mise à jour des champs
            sourceClassLabel.setText(relation.getSourceClass().getName());
            targetClassLabel.setText(relation.getTargetClass().getName());
            relationTypeComboBox.setValue(relation.getRelationType());
            sourceMultiplicityField.setText(relation.getSourceMultiplicity());
            targetMultiplicityField.setText(relation.getTargetMultiplicity());
            labelField.setText(relation.getLabel());
        }
    }

    /**
     * Obtient la relation en cours d'édition
     * @return La relation en cours d'édition
     */
    public DiagramRelation getRelation() {
        return this.relation;
    }

    private void handleInverseRelation() {
        // Cette fonctionnalité pourrait être implémentée dans une version future
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Fonctionnalité à implémenter");
        alert.setHeaderText("Inversion de relation");
        alert.setContentText("Cette fonctionnalité serait implémentée dans le contrôleur pour gérer l'inversion complète de la relation.");
        alert.showAndWait();
    }

    /**
     * Méthode pour configurer tous les paramètres nécessaires après la construction
     * @param commandManager Le gestionnaire de commandes
     * @param diagram Le diagramme
     * @param diagramCanvas Le canvas pour les mises à jour d'affichage
     */
    public void configure(CommandManager commandManager, ClassDiagram diagram, DiagramCanvas diagramCanvas) {
        this.commandManager = commandManager;
        this.diagram = diagram;
        this.diagramCanvas = diagramCanvas;
    }
}