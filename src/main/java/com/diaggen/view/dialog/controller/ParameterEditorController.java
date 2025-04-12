package com.diaggen.view.dialog.controller;

import com.diaggen.model.Parameter;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.util.Callback;

public class ParameterEditorController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField typeField;

    private Parameter parameter;
    private Dialog<Parameter> dialog;

    @FXML
    public void initialize() {

    }

        public void setDialog(Dialog<Parameter> dialog, Parameter parameter) {
        this.dialog = dialog;
        this.parameter = parameter;

        dialog.setTitle(parameter == null ? "Ajouter un paramètre" : "Modifier un paramètre");
        dialog.setHeaderText(parameter == null ? "Créer un nouveau paramètre" : "Modifier le paramètre");

        if (parameter != null) {
            nameField.setText(parameter.getName());
            typeField.setText(parameter.getType());
        }

        dialog.setResultConverter(createResultConverter());
    }

        private Callback<ButtonType, Parameter> createResultConverter() {
        return buttonType -> {
            if (buttonType == ButtonType.OK) {
                if (parameter == null) {

                    return new Parameter(
                            nameField.getText(),
                            typeField.getText());
                } else {

                    parameter.setName(nameField.getText());
                    parameter.setType(typeField.getText());
                    return parameter;
                }
            }
            return null;
        };
    }
}