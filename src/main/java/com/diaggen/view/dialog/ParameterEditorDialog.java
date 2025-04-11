package com.diaggen.view.dialog;

import com.diaggen.model.Parameter;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class ParameterEditorDialog extends Dialog<Parameter> {

    private final TextField nameField;
    private final TextField typeField;

    private final Parameter parameter;

    public ParameterEditorDialog(Parameter parameter) {
        this.parameter = parameter;

        setTitle(parameter == null ? "Ajouter un paramètre" : "Modifier un paramètre");
        setHeaderText(parameter == null ? "Créer un nouveau paramètre" : "Modifier le paramètre");

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        nameField = new TextField(parameter != null ? parameter.getName() : "");
        typeField = new TextField(parameter != null ? parameter.getType() : "");

        grid.add(new Label("Nom:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Type:"), 0, 1);
        grid.add(typeField, 1, 1);

        getDialogPane().setContent(grid);

        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
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
        });
    }
}


