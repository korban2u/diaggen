package com.diaggen.view.dialog.controller;

import com.diaggen.model.Member;
import com.diaggen.model.Visibility;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.util.Callback;
import javafx.util.StringConverter;

public class AttributeEditorController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField typeField;

    @FXML
    private ComboBox<Visibility> visibilityComboBox;

    private Member attribute;
    private Dialog<Member> dialog;

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
    }

        public void setDialog(Dialog<Member> dialog, Member attribute) {
        this.dialog = dialog;
        this.attribute = attribute;

        dialog.setTitle(attribute == null ? "Ajouter un attribut" : "Modifier un attribut");
        dialog.setHeaderText(attribute == null ? "Créer un nouvel attribut" : "Modifier l'attribut");

        if (attribute != null) {
            nameField.setText(attribute.getName());
            typeField.setText(attribute.getType());
            visibilityComboBox.getSelectionModel().select(attribute.getVisibility());
        } else {

            visibilityComboBox.getSelectionModel().select(Visibility.PRIVATE);
        }

        dialog.setResultConverter(createResultConverter());
    }

        private Callback<ButtonType, Member> createResultConverter() {
        return buttonType -> {
            if (buttonType == ButtonType.OK) {
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
        };
    }
}