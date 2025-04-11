package com.diaggen.view.dialog;

import com.diaggen.model.Method;
import com.diaggen.model.Parameter;
import com.diaggen.model.Visibility;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;

public class MethodEditorDialog extends Dialog<Method> {

    private final TextField nameField;
    private final TextField returnTypeField;
    private final ComboBox<Visibility> visibilityComboBox;
    private final CheckBox abstractCheckBox;
    private final CheckBox staticCheckBox;
    private final ListView<Parameter> parametersListView;

    private final Method method;

    public MethodEditorDialog(Method method) {
        this.method = method;

        setTitle(method == null ? "Ajouter une méthode" : "Modifier une méthode");
        setHeaderText(method == null ? "Créer une nouvelle méthode" : "Modifier la méthode");

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20, 10, 10, 10));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        nameField = new TextField(method != null ? method.getName() : "");
        returnTypeField = new TextField(method != null ? method.getReturnType() : "void");

        visibilityComboBox = new ComboBox<>(FXCollections.observableArrayList(Visibility.values()));
        visibilityComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Visibility visibility) {
                return visibility != null ? visibility.getSymbol() + " " + visibility.name() : "";
            }

            @Override
            public Visibility fromString(String string) {
                return null;
            }
        });

        if (method != null) {
            visibilityComboBox.getSelectionModel().select(method.getVisibility());
        } else {
            visibilityComboBox.getSelectionModel().select(Visibility.PUBLIC);
        }

        abstractCheckBox = new CheckBox("Abstract");
        abstractCheckBox.setSelected(method != null && method.isAbstract());

        staticCheckBox = new CheckBox("Static");
        staticCheckBox.setSelected(method != null && method.isStatic());

        grid.add(new Label("Nom:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Type de retour:"), 0, 1);
        grid.add(returnTypeField, 1, 1);
        grid.add(new Label("Visibilité:"), 0, 2);
        grid.add(visibilityComboBox, 1, 2);

        HBox checkBoxes = new HBox(10);
        checkBoxes.getChildren().addAll(abstractCheckBox, staticCheckBox);

        grid.add(checkBoxes, 1, 3);

        Label parametersLabel = new Label("Paramètres:");

        parametersListView = new ListView<>();
        parametersListView.setPrefHeight(150);
        parametersListView.setCellFactory(param -> new ParameterListCell());

        if (method != null) {
            parametersListView.setItems(FXCollections.observableArrayList(method.getParameters()));
        }

        HBox parametersButtonsBox = new HBox(10);
        javafx.scene.control.Button addParameterButton = new javafx.scene.control.Button("Ajouter");
        javafx.scene.control.Button editParameterButton = new javafx.scene.control.Button("Modifier");
        javafx.scene.control.Button removeParameterButton = new javafx.scene.control.Button("Supprimer");

        addParameterButton.setOnAction(e -> handleAddParameter());
        editParameterButton.setOnAction(e -> handleEditParameter());
        removeParameterButton.setOnAction(e -> handleRemoveParameter());

        parametersButtonsBox.getChildren().addAll(addParameterButton, editParameterButton, removeParameterButton);

        content.getChildren().addAll(grid, parametersLabel, parametersListView, parametersButtonsBox);

        getDialogPane().setContent(content);

        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                Visibility selectedVisibility = visibilityComboBox.getSelectionModel().getSelectedItem();

                List<Parameter> parameters = new ArrayList<>(parametersListView.getItems());

                if (method == null) {
                    return new Method(
                            nameField.getText(),
                            returnTypeField.getText(),
                            parameters,
                            selectedVisibility,
                            abstractCheckBox.isSelected(),
                            staticCheckBox.isSelected());
                } else {
                    method.setName(nameField.getText());
                    method.setReturnType(returnTypeField.getText());
                    return method;
                }
            }
            return null;
        });
    }

    private void handleAddParameter() {
        ParameterEditorDialog dialog = new ParameterEditorDialog(null);
        dialog.showAndWait().ifPresent(parameter -> {
            if (parametersListView.getItems() == null) {
                parametersListView.setItems(FXCollections.observableArrayList());
            }
            parametersListView.getItems().add(parameter);
        });
    }

    private void handleEditParameter() {
        Parameter selectedParameter = parametersListView.getSelectionModel().getSelectedItem();
        if (selectedParameter != null) {
            ParameterEditorDialog dialog = new ParameterEditorDialog(selectedParameter);
            dialog.showAndWait();
        }
    }

    private void handleRemoveParameter() {
        Parameter selectedParameter = parametersListView.getSelectionModel().getSelectedItem();
        if (selectedParameter != null) {
            parametersListView.getItems().remove(selectedParameter);
        }
    }

    private static class ParameterListCell extends TextFieldListCell<Parameter> {
        public ParameterListCell() {
            setConverter(new StringConverter<>() {
                @Override
                public String toString(Parameter parameter) {
                    return parameter != null ?
                            parameter.getName() + " : " + parameter.getType() : "";
                }

                @Override
                public Parameter fromString(String string) {
                    return null;
                }
            });
        }
    }
}


