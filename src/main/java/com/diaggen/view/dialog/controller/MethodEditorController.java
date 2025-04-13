package com.diaggen.view.dialog.controller;

import com.diaggen.model.Method;
import com.diaggen.model.Parameter;
import com.diaggen.model.Visibility;
import com.diaggen.view.dialog.DialogFactory;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.util.ArrayList;

public class MethodEditorController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField returnTypeField;

    @FXML
    private ComboBox<Visibility> visibilityComboBox;

    @FXML
    private CheckBox abstractCheckBox;

    @FXML
    private CheckBox staticCheckBox;

    @FXML
    private ListView<Parameter> parametersListView;

    @FXML
    private Button addParameterButton;

    @FXML
    private Button editParameterButton;

    @FXML
    private Button removeParameterButton;

    private Method method;
    private Dialog<Method> dialog;

    @FXML
    public void initialize() {

        visibilityComboBox.setItems(FXCollections.observableArrayList(Visibility.values()));
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

        parametersListView.setCellFactory(param -> new ParameterListCell());
    }

    public void setDialog(Dialog<Method> dialog, Method method) {
        this.dialog = dialog;
        this.method = method;

        dialog.setTitle(method == null ? "Ajouter une méthode" : "Modifier une méthode");
        dialog.setHeaderText(method == null ? "Créer une nouvelle méthode" : "Modifier la méthode");

        if (method != null) {
            nameField.setText(method.getName());
            returnTypeField.setText(method.getReturnType());
            visibilityComboBox.getSelectionModel().select(method.getVisibility());
            abstractCheckBox.setSelected(method.isAbstract());
            staticCheckBox.setSelected(method.isStatic());
            parametersListView.setItems(method.getParameters());
        } else {

            returnTypeField.setText("void");
            visibilityComboBox.getSelectionModel().select(Visibility.PUBLIC);
            parametersListView.setItems(FXCollections.observableArrayList());
        }

        dialog.setResultConverter(createResultConverter());
    }

    @FXML
    public void handleAddParameter() {
        DialogFactory dialogFactory = DialogFactory.getInstance();
        Dialog<Parameter> dialog = dialogFactory.createParameterEditorDialog(null);

        dialog.showAndWait().ifPresent(parameter -> {
            parametersListView.getItems().add(parameter);
        });
    }

    @FXML
    public void handleEditParameter() {
        Parameter selectedParameter = parametersListView.getSelectionModel().getSelectedItem();
        if (selectedParameter != null) {
            DialogFactory dialogFactory = DialogFactory.getInstance();
            Dialog<Parameter> dialog = dialogFactory.createParameterEditorDialog(selectedParameter);

            dialog.showAndWait();
            parametersListView.refresh();
        }
    }

    @FXML
    public void handleRemoveParameter() {
        Parameter selectedParameter = parametersListView.getSelectionModel().getSelectedItem();
        if (selectedParameter != null) {
            parametersListView.getItems().remove(selectedParameter);
        }
    }

    private Callback<ButtonType, Method> createResultConverter() {
        return dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                Visibility selectedVisibility = visibilityComboBox.getSelectionModel().getSelectedItem();

                if (method == null) {

                    return new Method(
                            nameField.getText(),
                            returnTypeField.getText(),
                            new ArrayList<>(parametersListView.getItems()),
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
        };
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