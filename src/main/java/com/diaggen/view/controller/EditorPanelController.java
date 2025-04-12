package com.diaggen.view.controller;

import com.diaggen.controller.command.CommandManager;
import com.diaggen.model.*;
import com.diaggen.view.dialog.DialogFactory;
import com.diaggen.view.editor.RelationEditorPanel;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class EditorPanelController {

    private VBox editorContent;
    private DialogFactory dialogFactory;
    private CommandManager commandManager;

    // Éléments pour l'édition de classe
    private TextField classNameField;
    private TextField packageNameField;
    private ComboBox<ClassType> classTypeComboBox;
    private ListView<Member> attributesListView;
    private ListView<Method> methodsListView;

    // Éléments pour l'édition de relation
    private ComboBox<RelationType> relationTypeComboBox;
    private TextField sourceMultiplicityField;
    private TextField targetMultiplicityField;
    private TextField relationLabelField;
    private Label sourceClassLabel;
    private Label targetClassLabel;

    // Objets actuellement édités
    private DiagramClass currentClass;
    private DiagramRelation currentRelation;

    public EditorPanelController(VBox editorContent, DialogFactory dialogFactory) {
        this.editorContent = editorContent;
        this.dialogFactory = dialogFactory;
    }

    public void setCommandManager(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    public void showClassEditor(DiagramClass diagramClass) {
        this.currentClass = diagramClass;
        this.currentRelation = null;

        editorContent.getChildren().clear();

        // Titre avec le type d'éditeur
        Label titleLabel = new Label("Édition de classe");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        editorContent.getChildren().add(titleLabel);

        // Panneau pour les propriétés générales
        TitledPane generalPane = createClassGeneralSection();

        // Panneau pour les attributs
        TitledPane attributesPane = createAttributesSection();

        // Panneau pour les méthodes
        TitledPane methodsPane = createMethodsSection();

        editorContent.getChildren().addAll(generalPane, attributesPane, methodsPane);

        // Remplir les champs avec les valeurs de la classe
        updateClassFields();
    }

    public void showRelationEditor(DiagramRelation relation) {
        this.currentRelation = relation;
        this.currentClass = null;

        editorContent.getChildren().clear();

        // Titre avec le type d'éditeur
        Label titleLabel = new Label("Édition de relation");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        editorContent.getChildren().add(titleLabel);

        // Panneau pour les propriétés de la relation
        TitledPane propertiesPane = createRelationPropertiesSection();

        editorContent.getChildren().add(propertiesPane);

        // Remplir les champs avec les valeurs de la relation
        updateRelationFields();
    }

    // Nouvelle méthode pour utiliser un RelationEditorPanel personnalisé
    public void showCustomRelationEditor(RelationEditorPanel relationEditorPanel) {
        this.currentRelation = relationEditorPanel.getRelation();
        this.currentClass = null;

        editorContent.getChildren().clear();

        // Titre avec le type d'éditeur
        Label titleLabel = new Label("Édition de relation");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        editorContent.getChildren().add(titleLabel);

        // Ajouter le panneau d'édition de relation personnalisé
        editorContent.getChildren().add(relationEditorPanel);
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

        // Nom de la classe
        Label nameLabel = new Label("Nom:");
        classNameField = new TextField();
        classNameField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (currentClass != null) {
                currentClass.setName(newValue);
            }
        });
        grid.add(nameLabel, 0, 0);
        grid.add(classNameField, 1, 0);

        // Package
        Label packageLabel = new Label("Package:");
        packageNameField = new TextField();
        packageNameField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (currentClass != null) {
                currentClass.setPackageName(newValue);
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
            if (currentClass != null && newValue != null) {
                currentClass.setClassType(newValue);
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
                // Si le CommandManager est disponible, on peut changer le type de relation
                if (commandManager != null) {
                    // La gestion sera externalisée et implémentée dans une version ultérieure
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Changement de type de relation");
                    alert.setHeaderText("Cette fonctionnalité nécessite une implémentation spéciale");
                    alert.setContentText("Pour changer le type de relation, veuillez utiliser l'éditeur personnalisé qui sera ajouté dans une version ultérieure.");
                    alert.showAndWait();

                    // Restaurer l'ancienne valeur car nous ne pouvons pas gérer le changement ici
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
            if (currentRelation != null) {
                currentRelation.setSourceMultiplicity(newValue);
            }
        });
        grid.add(sourceMultLabel, 0, 3);
        grid.add(sourceMultiplicityField, 1, 3);

        // Multiplicité cible
        Label targetMultLabel = new Label("Multiplicité cible:");
        targetMultiplicityField = new TextField();
        targetMultiplicityField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (currentRelation != null) {
                currentRelation.setTargetMultiplicity(newValue);
            }
        });
        grid.add(targetMultLabel, 0, 4);
        grid.add(targetMultiplicityField, 1, 4);

        // Libellé
        Label labelTextLabel = new Label("Libellé:");
        relationLabelField = new TextField();
        relationLabelField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (currentRelation != null) {
                currentRelation.setLabel(newValue);
            }
        });
        grid.add(labelTextLabel, 0, 5);
        grid.add(relationLabelField, 1, 5);

        pane.setContent(grid);
        return pane;
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
        if (currentClass != null) {
            Dialog<Member> dialog = dialogFactory.createAttributeEditorDialog(null);
            dialog.showAndWait().ifPresent(attribute -> {
                currentClass.addAttribute(attribute);
                attributesListView.refresh();
            });
        }
    }

    private void handleEditAttribute() {
        if (currentClass != null) {
            Member selectedAttribute = attributesListView.getSelectionModel().getSelectedItem();
            if (selectedAttribute != null) {
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
        if (currentClass != null) {
            Dialog<Method> dialog = dialogFactory.createMethodEditorDialog(null);
            dialog.showAndWait().ifPresent(method -> {
                currentClass.addMethod(method);
                methodsListView.refresh();
            });
        }
    }

    private void handleEditMethod() {
        if (currentClass != null) {
            Method selectedMethod = methodsListView.getSelectionModel().getSelectedItem();
            if (selectedMethod != null) {
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
    }

    public Node getContent() {
        return editorContent;
    }

    public boolean isEditing() {
        return currentClass != null || currentRelation != null;
    }
}