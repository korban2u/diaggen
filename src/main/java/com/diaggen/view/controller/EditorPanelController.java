package com.diaggen.view.controller;

import com.diaggen.controller.MainController;
import com.diaggen.model.*;
import com.diaggen.view.dialog.DialogFactory;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EditorPanelController {
    private static final Logger LOGGER = Logger.getLogger(EditorPanelController.class.getName());

    private VBox editorContent;
    private MainController mainController;
    private DialogFactory dialogFactory;

    // Champs pour l'édition de classe
    private TextField classNameField;
    private TextField packageNameField;
    private ComboBox<ClassType> classTypeComboBox;
    private ListView<Member> attributesListView;
    private ListView<Method> methodsListView;

    // Champs pour l'édition de relation
    private ComboBox<RelationType> relationTypeComboBox;
    private TextField sourceMultiplicityField;
    private TextField targetMultiplicityField;
    private TextField relationLabelField;
    private Label sourceClassLabel;
    private Label targetClassLabel;

    // Éléments actuellement édités
    private DiagramClass currentClass;
    private DiagramRelation currentRelation;

    public EditorPanelController(VBox editorContent) {
        this.editorContent = editorContent;
        this.dialogFactory = DialogFactory.getInstance();
        LOGGER.log(Level.INFO, "EditorPanelController initialized");
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        LOGGER.log(Level.INFO, "MainController set in EditorPanelController");
    }

    public void showClassEditor(DiagramClass diagramClass) {
        if (diagramClass == null) {
            LOGGER.log(Level.WARNING, "Attempt to show class editor with null class");
            return;
        }

        LOGGER.log(Level.INFO, "Showing class editor for class: {0}", diagramClass.getName());

        this.currentClass = diagramClass;
        this.currentRelation = null;

        editorContent.getChildren().clear();

        // Titre
        Label titleLabel = new Label("Édition de classe");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        editorContent.getChildren().add(titleLabel);

        // Section Général
        TitledPane generalPane = createClassGeneralSection();

        // Section Attributs
        TitledPane attributesPane = createAttributesSection();

        // Section Méthodes
        TitledPane methodsPane = createMethodsSection();

        editorContent.getChildren().addAll(generalPane, attributesPane, methodsPane);

        // Mettre à jour les champs avec les données de la classe
        updateClassFields();
    }

    public void showRelationEditor(DiagramRelation relation) {
        if (relation == null) {
            LOGGER.log(Level.WARNING, "Attempt to show relation editor with null relation");
            return;
        }

        LOGGER.log(Level.INFO, "Showing relation editor for relation between {0} and {1}",
                new Object[]{relation.getSourceClass().getName(), relation.getTargetClass().getName()});

        this.currentRelation = relation;
        this.currentClass = null;

        editorContent.getChildren().clear();

        // Titre
        Label titleLabel = new Label("Édition de relation");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        editorContent.getChildren().add(titleLabel);

        // Section Propriétés
        TitledPane propertiesPane = createRelationPropertiesSection();

        editorContent.getChildren().add(propertiesPane);

        // Mettre à jour les champs avec les données de la relation
        updateRelationFields();
    }

    private TitledPane createClassGeneralSection() {
        TitledPane pane = new TitledPane();
        pane.setText("Général");
        pane.setCollapsible(true);
        pane.setExpanded(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        // Nom
        Label nameLabel = new Label("Nom:");
        classNameField = new TextField();
        classNameField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (currentClass != null && !newValue.equals(oldValue)) {
                currentClass.setName(newValue);
                LOGGER.log(Level.FINE, "Class name updated to: {0}", newValue);
            }
        });
        grid.add(nameLabel, 0, 0);
        grid.add(classNameField, 1, 0);

        // Package
        Label packageLabel = new Label("Package:");
        packageNameField = new TextField();
        packageNameField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (currentClass != null && !newValue.equals(oldValue)) {
                currentClass.setPackageName(newValue);
                LOGGER.log(Level.FINE, "Package name updated to: {0}", newValue);
            }
        });
        grid.add(packageLabel, 0, 1);
        grid.add(packageNameField, 1, 1);

        // Type de classe
        Label typeLabel = new Label("Type:");
        classTypeComboBox = new ComboBox<>(FXCollections.observableArrayList(ClassType.values()));
        classTypeComboBox.setConverter(new StringConverter<ClassType>() {
            @Override
            public String toString(ClassType type) {
                return type != null ? type.getDisplayName() : "";
            }

            @Override
            public ClassType fromString(String string) {
                return null;
            }
        });
        classTypeComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (currentClass != null && newValue != null && !newValue.equals(oldValue)) {
                currentClass.setClassType(newValue);
                LOGGER.log(Level.FINE, "Class type updated to: {0}", newValue);
            }
        });
        grid.add(typeLabel, 0, 2);
        grid.add(classTypeComboBox, 1, 2);

        pane.setContent(grid);
        return pane;
    }

    private TitledPane createAttributesSection() {
        TitledPane pane = new TitledPane();
        pane.setText("Attributs");
        pane.setCollapsible(true);
        pane.setExpanded(true);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Liste des attributs
        attributesListView = new ListView<>();
        attributesListView.setPrefHeight(150);
        attributesListView.setCellFactory(param -> new ListCell<Member>() {
            @Override
            protected void updateItem(Member item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getVisibility().getSymbol() + " " +
                            item.getName() + " : " + item.getType());
                }
            }
        });

        // Boutons pour gérer les attributs
        HBox buttons = new HBox(10);
        Button addButton = new Button("Ajouter");
        Button editButton = new Button("Modifier");
        Button removeButton = new Button("Supprimer");

        addButton.setOnAction(e -> handleAddAttribute());
        editButton.setOnAction(e -> handleEditAttribute());
        removeButton.setOnAction(e -> handleRemoveAttribute());

        // Désactiver les boutons d'édition/suppression si aucun attribut n'est sélectionné
        editButton.disableProperty().bind(attributesListView.getSelectionModel().selectedItemProperty().isNull());
        removeButton.disableProperty().bind(attributesListView.getSelectionModel().selectedItemProperty().isNull());

        buttons.getChildren().addAll(addButton, editButton, removeButton);

        content.getChildren().addAll(attributesListView, buttons);
        pane.setContent(content);

        return pane;
    }

    private TitledPane createMethodsSection() {
        TitledPane pane = new TitledPane();
        pane.setText("Méthodes");
        pane.setCollapsible(true);
        pane.setExpanded(true);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Liste des méthodes
        methodsListView = new ListView<>();
        methodsListView.setPrefHeight(150);
        methodsListView.setCellFactory(param -> new ListCell<Method>() {
            @Override
            protected void updateItem(Method item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append(item.getVisibility().getSymbol()).append(" ");

                    if (item.isStatic()) {
                        sb.append("static ");
                    }

                    if (item.isAbstract()) {
                        sb.append("abstract ");
                    }

                    sb.append(item.getName()).append("(...)");
                    sb.append(" : ").append(item.getReturnType());

                    setText(sb.toString());
                }
            }
        });

        // Boutons pour gérer les méthodes
        HBox buttons = new HBox(10);
        Button addButton = new Button("Ajouter");
        Button editButton = new Button("Modifier");
        Button removeButton = new Button("Supprimer");

        addButton.setOnAction(e -> handleAddMethod());
        editButton.setOnAction(e -> handleEditMethod());
        removeButton.setOnAction(e -> handleRemoveMethod());

        // Désactiver les boutons d'édition/suppression si aucune méthode n'est sélectionnée
        editButton.disableProperty().bind(methodsListView.getSelectionModel().selectedItemProperty().isNull());
        removeButton.disableProperty().bind(methodsListView.getSelectionModel().selectedItemProperty().isNull());

        buttons.getChildren().addAll(addButton, editButton, removeButton);

        content.getChildren().addAll(methodsListView, buttons);
        pane.setContent(content);

        return pane;
    }

    private TitledPane createRelationPropertiesSection() {
        TitledPane pane = new TitledPane();
        pane.setText("Propriétés");
        pane.setCollapsible(true);
        pane.setExpanded(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        // Classes source et cible
        Label sourceLabel = new Label("Classe source:");
        sourceClassLabel = new Label();
        sourceClassLabel.setStyle("-fx-font-weight: bold;");
        grid.add(sourceLabel, 0, 0);
        grid.add(sourceClassLabel, 1, 0);

        Label targetLabel = new Label("Classe cible:");
        targetClassLabel = new Label();
        targetClassLabel.setStyle("-fx-font-weight: bold;");
        grid.add(targetLabel, 0, 1);
        grid.add(targetClassLabel, 1, 1);

        // Type de relation
        Label typeLabel = new Label("Type:");
        relationTypeComboBox = new ComboBox<>(FXCollections.observableArrayList(RelationType.values()));
        relationTypeComboBox.setConverter(new StringConverter<RelationType>() {
            @Override
            public String toString(RelationType type) {
                return type != null ? type.getDisplayName() + " (" + type.getSymbol() + ")" : "";
            }

            @Override
            public RelationType fromString(String string) {
                return null;
            }
        });
        relationTypeComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (currentRelation != null && newValue != null && oldValue != null && !oldValue.equals(newValue)) {
                // Changer le type de relation via le contrôleur
                if (mainController != null) {
                    mainController.changeRelationType(currentRelation, newValue);
                    LOGGER.log(Level.INFO, "Relation type changed from {0} to {1}",
                            new Object[]{oldValue, newValue});
                } else {
                    LOGGER.log(Level.WARNING, "Cannot change relation type: mainController is null");
                    // Restaurer l'ancienne valeur
                    relationTypeComboBox.setValue(oldValue);
                }
            }
        });
        grid.add(typeLabel, 0, 2);
        grid.add(relationTypeComboBox, 1, 2);

        // Multiplicité source
        Label sourceMultLabel = new Label("Multiplicité source:");
        sourceMultiplicityField = new TextField();
        sourceMultiplicityField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (currentRelation != null && !newValue.equals(oldValue)) {
                currentRelation.setSourceMultiplicity(newValue);
                LOGGER.log(Level.FINE, "Source multiplicity updated to: {0}", newValue);
            }
        });
        grid.add(sourceMultLabel, 0, 3);
        grid.add(sourceMultiplicityField, 1, 3);

        // Multiplicité cible
        Label targetMultLabel = new Label("Multiplicité cible:");
        targetMultiplicityField = new TextField();
        targetMultiplicityField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (currentRelation != null && !newValue.equals(oldValue)) {
                currentRelation.setTargetMultiplicity(newValue);
                LOGGER.log(Level.FINE, "Target multiplicity updated to: {0}", newValue);
            }
        });
        grid.add(targetMultLabel, 0, 4);
        grid.add(targetMultiplicityField, 1, 4);

        // Libellé
        Label labelTextLabel = new Label("Libellé:");
        relationLabelField = new TextField();
        relationLabelField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (currentRelation != null && !newValue.equals(oldValue)) {
                currentRelation.setLabel(newValue);
                LOGGER.log(Level.FINE, "Relation label updated to: {0}", newValue);
            }
        });
        grid.add(labelTextLabel, 0, 5);
        grid.add(relationLabelField, 1, 5);

        // Bouton d'inversion
        Button invertButton = new Button("Inverser la relation");
        invertButton.setOnAction(e -> handleInvertRelation());
        invertButton.setMaxWidth(Double.MAX_VALUE);
        grid.add(invertButton, 0, 6, 2, 1);

        pane.setContent(grid);
        return pane;
    }

    private void handleInvertRelation() {
        if (currentRelation == null || mainController == null) return;

        LOGGER.log(Level.INFO, "Inverting relation");

        // Obtenir les données actuelles
        DiagramClass source = currentRelation.getSourceClass();
        DiagramClass target = currentRelation.getTargetClass();
        String sourceMulti = currentRelation.getSourceMultiplicity();
        String targetMulti = currentRelation.getTargetMultiplicity();
        RelationType type = currentRelation.getRelationType();
        String label = currentRelation.getLabel();

        // Créer une nouvelle relation inversée
        DiagramRelation newRelation = new DiagramRelation(
                target, source, type, targetMulti, sourceMulti, label);

        // Ajouter la nouvelle relation via le mainController
        // Cette logique dépend de la méthode spécifique dans le contrôleur
        // À implémenter plus tard

        // Pour le moment, juste informer l'utilisateur
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Fonctionnalité à implémenter");
        alert.setHeaderText("Inversion de relation");
        alert.setContentText("Cette fonctionnalité serait implémentée dans une version ultérieure.");
        alert.showAndWait();
    }

    private void updateClassFields() {
        if (currentClass != null) {
            classNameField.setText(currentClass.getName());
            packageNameField.setText(currentClass.getPackageName());
            classTypeComboBox.setValue(currentClass.getClassType());
            attributesListView.setItems(currentClass.getAttributes());
            methodsListView.setItems(currentClass.getMethods());
        }
    }

    private void updateRelationFields() {
        if (currentRelation != null) {
            sourceClassLabel.setText(currentRelation.getSourceClass().getName());
            targetClassLabel.setText(currentRelation.getTargetClass().getName());
            relationTypeComboBox.setValue(currentRelation.getRelationType());
            sourceMultiplicityField.setText(currentRelation.getSourceMultiplicity());
            targetMultiplicityField.setText(currentRelation.getTargetMultiplicity());
            relationLabelField.setText(currentRelation.getLabel());
        }
    }

    private void handleAddAttribute() {
        if (currentClass != null && dialogFactory != null) {
            LOGGER.log(Level.INFO, "Adding attribute to class: {0}", currentClass.getName());
            Dialog<Member> dialog = dialogFactory.createAttributeEditorDialog(null);
            dialog.showAndWait().ifPresent(attribute -> {
                currentClass.addAttribute(attribute);
                attributesListView.refresh();
                LOGGER.log(Level.INFO, "Attribute added: {0}", attribute.getName());
            });
        }
    }

    private void handleEditAttribute() {
        if (currentClass != null && dialogFactory != null) {
            Member selectedAttribute = attributesListView.getSelectionModel().getSelectedItem();
            if (selectedAttribute != null) {
                LOGGER.log(Level.INFO, "Editing attribute: {0}", selectedAttribute.getName());
                Dialog<Member> dialog = dialogFactory.createAttributeEditorDialog(selectedAttribute);
                dialog.showAndWait();
                attributesListView.refresh();
            }
        }
    }

    private void handleRemoveAttribute() {
        if (currentClass != null) {
            Member selectedAttribute = attributesListView.getSelectionModel().getSelectedItem();
            if (selectedAttribute != null) {
                LOGGER.log(Level.INFO, "Removing attribute: {0}", selectedAttribute.getName());
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Supprimer l'attribut");
                alert.setHeaderText("Êtes-vous sûr de vouloir supprimer cet attribut ?");
                alert.setContentText(selectedAttribute.getName() + " : " + selectedAttribute.getType());

                alert.showAndWait().ifPresent(result -> {
                    if (result == ButtonType.OK) {
                        currentClass.removeAttribute(selectedAttribute);
                    }
                });
            }
        }
    }

    private void handleAddMethod() {
        if (currentClass != null && dialogFactory != null) {
            LOGGER.log(Level.INFO, "Adding method to class: {0}", currentClass.getName());
            Dialog<Method> dialog = dialogFactory.createMethodEditorDialog(null);
            dialog.showAndWait().ifPresent(method -> {
                currentClass.addMethod(method);
                methodsListView.refresh();
                LOGGER.log(Level.INFO, "Method added: {0}", method.getName());
            });
        }
    }

    private void handleEditMethod() {
        if (currentClass != null && dialogFactory != null) {
            Method selectedMethod = methodsListView.getSelectionModel().getSelectedItem();
            if (selectedMethod != null) {
                LOGGER.log(Level.INFO, "Editing method: {0}", selectedMethod.getName());
                Dialog<Method> dialog = dialogFactory.createMethodEditorDialog(selectedMethod);
                dialog.showAndWait();
                methodsListView.refresh();
            }
        }
    }

    private void handleRemoveMethod() {
        if (currentClass != null) {
            Method selectedMethod = methodsListView.getSelectionModel().getSelectedItem();
            if (selectedMethod != null) {
                LOGGER.log(Level.INFO, "Removing method: {0}", selectedMethod.getName());
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Supprimer la méthode");
                alert.setHeaderText("Êtes-vous sûr de vouloir supprimer cette méthode ?");
                alert.setContentText(selectedMethod.getName() + "()");

                alert.showAndWait().ifPresent(result -> {
                    if (result == ButtonType.OK) {
                        currentClass.removeMethod(selectedMethod);
                    }
                });
            }
        }
    }

    public void clearEditor() {
        editorContent.getChildren().clear();
        currentClass = null;
        currentRelation = null;
        LOGGER.log(Level.INFO, "Editor cleared");
    }

    public Node getContent() {
        return editorContent;
    }

    public boolean isEditing() {
        return currentClass != null || currentRelation != null;
    }

    public DiagramClass getCurrentClass() {
        return currentClass;
    }

    public DiagramRelation getCurrentRelation() {
        return currentRelation;
    }
}