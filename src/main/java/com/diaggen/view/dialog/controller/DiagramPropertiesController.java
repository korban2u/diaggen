package com.diaggen.view.dialog.controller;

import com.diaggen.model.ClassDiagram;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.util.Callback;

public class DiagramPropertiesController {

    @FXML
    private TextField nameField;

    private ClassDiagram diagram;
    private Dialog<String> dialog;

    @FXML
    public void initialize() {

    }

    public void setDialog(Dialog<String> dialog, ClassDiagram diagram) {
        this.dialog = dialog;
        this.diagram = diagram;

        dialog.setTitle("Propriétés du diagramme");
        dialog.setHeaderText("Modifier les propriétés du diagramme");

        nameField.setText(diagram.getName());

        dialog.setResultConverter(createResultConverter());
    }

    private Callback<ButtonType, String> createResultConverter() {
        return buttonType -> {
            if (buttonType == ButtonType.OK) {
                return nameField.getText();
            }
            return null;
        };
    }
}