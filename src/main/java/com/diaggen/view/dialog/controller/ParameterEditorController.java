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
        // Initialisation des composants après chargement du FXML
    }

    /**
     * Configure le dialogue et définit le paramètre à éditer
     * @param dialog Le dialogue à configurer
     * @param parameter Le paramètre à éditer (null pour création)
     */
    public void setDialog(Dialog<Parameter> dialog, Parameter parameter) {
        this.dialog = dialog;
        this.parameter = parameter;

        // Définir le titre du dialogue
        dialog.setTitle(parameter == null ? "Ajouter un paramètre" : "Modifier un paramètre");
        dialog.setHeaderText(parameter == null ? "Créer un nouveau paramètre" : "Modifier le paramètre");

        // Initialiser les champs avec les valeurs du paramètre existant
        if (parameter != null) {
            nameField.setText(parameter.getName());
            typeField.setText(parameter.getType());
        }

        // Configurer le convertisseur de résultat
        dialog.setResultConverter(createResultConverter());
    }

    /**
     * Crée un convertisseur de résultat pour le dialogue
     */
    private Callback<ButtonType, Parameter> createResultConverter() {
        return buttonType -> {
            if (buttonType == ButtonType.OK) {
                if (parameter == null) {
                    // Créer un nouveau paramètre
                    return new Parameter(
                            nameField.getText(),
                            typeField.getText());
                } else {
                    // Mettre à jour le paramètre existant
                    parameter.setName(nameField.getText());
                    parameter.setType(typeField.getText());
                    return parameter;
                }
            }
            return null;
        };
    }
}