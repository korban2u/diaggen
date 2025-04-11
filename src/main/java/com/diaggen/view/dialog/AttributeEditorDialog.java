package com.diaggen.view.dialog;

import com.diaggen.model.Member;
import com.diaggen.model.Visibility;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

public class AttributeEditorDialog extends Dialog<Member> {

    private final TextField nameField;
    private final TextField typeField;
    private final ComboBox<Visibility> visibilityComboBox;

    private final Member attribute;

    public AttributeEditorDialog(Member attribute) {
        this.attribute = attribute;

        setTitle(attribute == null ? "Ajouter un attribut" : "Modifier un attribut");
        setHeaderText(attribute == null ? "Créer un nouvel attribut" : "Modifier l'attribut");

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        nameField = new TextField(attribute != null ? attribute.getName() : "");
        typeField = new TextField(attribute != null ? attribute.getType() : "");

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

        if (attribute != null) {
            visibilityComboBox.getSelectionModel().select(attribute.getVisibility());
        } else {
            visibilityComboBox.getSelectionModel().select(Visibility.PRIVATE);
        }

        grid.add(new Label("Nom:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Type:"), 0, 1);
        grid.add(typeField, 1, 1);
        grid.add(new Label("Visibilité:"), 0, 2);
        grid.add(visibilityComboBox, 1, 2);

        getDialogPane().setContent(grid);

        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                Visibility selectedVisibility = visibilityComboBox.getSelectionModel().getSelectedItem();

                if (attribute == null) {
                    return new Member(
                            nameField.getText(),
                            typeField.getText(),
                            selectedVisibility);
                } else {
                    attribute.setName(nameField.getText());
                    attribute.setType(typeField.getText());
                    return attribute;
                }
            }
            return null;
        });
    }
}


