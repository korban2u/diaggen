package com.diaggen.view.editor;

import com.diaggen.model.ClassType;
import com.diaggen.model.DiagramClass;
import com.diaggen.model.Member;
import com.diaggen.model.Method;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class ClassEditorPanel extends VBox {

    private DiagramClass diagramClass;

    // Champs d'édition
    private TextField nameField;
    private TextField packageField;
    private ComboBox<ClassType> typeComboBox;
    private ListView<Member> attributesListView;
    private ListView<Method> methodsListView;

    public ClassEditorPanel() {
        setSpacing(15);
        setPadding(new Insets(0, 5, 0, 5));

        createGeneralSection();
        createAttributesSection();
        createMethodsSection();
    }

    private void createGeneralSection() {
        TitledPane generalSection = new TitledPane();
        generalSection.setText("Général");
        generalSection.setCollapsible(true);
        generalSection.setExpanded(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        // Nom
        Label nameLabel = new Label("Nom:");
        nameField = new TextField();
        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (diagramClass != null) {
                diagramClass.setName(newVal);
            }
        });
        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);

        // Package
        Label packageLabel = new Label("Package:");
        packageField = new TextField();
        packageField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (diagramClass != null) {
                diagramClass.setPackageName(newVal);
            }
        });
        grid.add(packageLabel, 0, 1);
        grid.add(packageField, 1, 1);

        // Type
        Label typeLabel = new Label("Type:");
        typeComboBox = new ComboBox<>(FXCollections.observableArrayList(ClassType.values()));
        typeComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ClassType classType) {
                return classType != null ? classType.getDisplayName() : "";
            }

            @Override
            public ClassType fromString(String string) {
                return null;
            }
        });
        typeComboBox.setMaxWidth(Double.MAX_VALUE);
        typeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (diagramClass != null && newVal != null) {
                diagramClass.setClassType(newVal);
            }
        });
        grid.add(typeLabel, 0, 2);
        grid.add(typeComboBox, 1, 2);

        generalSection.setContent(grid);
        getChildren().add(generalSection);
    }

    private void createAttributesSection() {
        TitledPane attributesSection = new TitledPane();
        attributesSection.setText("Attributs");
        attributesSection.setCollapsible(true);
        attributesSection.setExpanded(true);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        attributesListView = new ListView<>();
        attributesListView.setCellFactory(param -> new AttributeListCell());
        attributesListView.setPrefHeight(150);

        HBox buttonBar = new HBox(10);
        Button addButton = new Button("Ajouter");
        addButton.setOnAction(e -> handleAddAttribute());

        Button editButton = new Button("Modifier");
        editButton.setOnAction(e -> handleEditAttribute());
        editButton.disableProperty().bind(attributesListView.getSelectionModel().selectedItemProperty().isNull());

        Button removeButton = new Button("Supprimer");
        removeButton.setOnAction(e -> handleRemoveAttribute());
        removeButton.disableProperty().bind(attributesListView.getSelectionModel().selectedItemProperty().isNull());

        buttonBar.getChildren().addAll(addButton, editButton, removeButton);

        content.getChildren().addAll(attributesListView, buttonBar);
        attributesSection.setContent(content);

        getChildren().add(attributesSection);
    }

    private void createMethodsSection() {
        TitledPane methodsSection = new TitledPane();
        methodsSection.setText("Méthodes");
        methodsSection.setCollapsible(true);
        methodsSection.setExpanded(true);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        methodsListView = new ListView<>();
        methodsListView.setCellFactory(param -> new MethodListCell());
        methodsListView.setPrefHeight(150);

        HBox buttonBar = new HBox(10);
        Button addButton = new Button("Ajouter");
        addButton.setOnAction(e -> handleAddMethod());

        Button editButton = new Button("Modifier");
        editButton.setOnAction(e -> handleEditMethod());
        editButton.disableProperty().bind(methodsListView.getSelectionModel().selectedItemProperty().isNull());

        Button removeButton = new Button("Supprimer");
        removeButton.setOnAction(e -> handleRemoveMethod());
        removeButton.disableProperty().bind(methodsListView.getSelectionModel().selectedItemProperty().isNull());

        buttonBar.getChildren().addAll(addButton, editButton, removeButton);

        content.getChildren().addAll(methodsListView, buttonBar);
        methodsSection.setContent(content);

        getChildren().add(methodsSection);
    }

    public void setDiagramClass(DiagramClass diagramClass) {
        this.diagramClass = diagramClass;

        if (diagramClass != null) {
            // Mise à jour des champs
            nameField.setText(diagramClass.getName());
            packageField.setText(diagramClass.getPackageName());
            typeComboBox.setValue(diagramClass.getClassType());

            // Mise à jour des listes
            attributesListView.setItems(diagramClass.getAttributes());
            methodsListView.setItems(diagramClass.getMethods());
        }
    }

    private void handleAddAttribute() {
        // Cette méthode sera implémentée pour appeler le dialogue d'édition d'attribut
        // Elle utilisera le DialogFactory existant
        // Pour l'instant, gardons-la vide
    }

    private void handleEditAttribute() {
        // Cette méthode sera implémentée pour appeler le dialogue d'édition d'attribut
    }

    private void handleRemoveAttribute() {
        if (diagramClass != null) {
            Member selected = attributesListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Supprimer l'attribut");
                alert.setHeaderText("Êtes-vous sûr de vouloir supprimer cet attribut ?");
                alert.setContentText(selected.getName() + " : " + selected.getType());

                alert.showAndWait().ifPresent(result -> {
                    if (result == ButtonType.OK) {
                        diagramClass.removeAttribute(selected);
                    }
                });
            }
        }
    }

    private void handleAddMethod() {
        // Cette méthode sera implémentée pour appeler le dialogue d'édition de méthode
    }

    private void handleEditMethod() {
        // Cette méthode sera implémentée pour appeler le dialogue d'édition de méthode
    }

    private void handleRemoveMethod() {
        if (diagramClass != null) {
            Method selected = methodsListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Supprimer la méthode");
                alert.setHeaderText("Êtes-vous sûr de vouloir supprimer cette méthode ?");
                alert.setContentText(selected.getName() + "()");

                alert.showAndWait().ifPresent(result -> {
                    if (result == ButtonType.OK) {
                        diagramClass.removeMethod(selected);
                    }
                });
            }
        }
    }

    private static class AttributeListCell extends ListCell<Member> {
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
    }

    private static class MethodListCell extends ListCell<Method> {
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
    }
}