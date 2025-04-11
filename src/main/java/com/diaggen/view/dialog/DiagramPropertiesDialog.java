package com.diaggen.view.dialog;

import com.diaggen.model.ClassDiagram;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class DiagramPropertiesDialog extends Dialog<String> {

    private final TextField nameField;

    public DiagramPropertiesDialog(ClassDiagram diagram) {
        setTitle("Propriétés du diagramme");
        setHeaderText("Modifier les propriétés du diagramme");

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        nameField = new TextField(diagram.getName());
        nameField.setMinWidth(250);

        grid.add(new Label("Nom:"), 0, 0);
        grid.add(nameField, 1, 0);

        getDialogPane().setContent(grid);

        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return nameField.getText();
            }
            return null;
        });
    }
}


