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
        // Initialisation des composants après chargement du FXML
    }

    /**
     * Configure le dialogue et définit le diagramme à éditer
     * @param dialog Le dialogue à configurer
     * @param diagram Le diagramme à éditer
     */
    public void setDialog(Dialog<String> dialog, ClassDiagram diagram) {
        this.dialog = dialog;
        this.diagram = diagram;

        // Définir le titre du dialogue
        dialog.setTitle("Propriétés du diagramme");
        dialog.setHeaderText("Modifier les propriétés du diagramme");

        // Initialiser les champs avec les valeurs du diagramme
        nameField.setText(diagram.getName());

        // Configurer le convertisseur de résultat
        dialog.setResultConverter(createResultConverter());
    }

    /**
     * Crée un convertisseur de résultat pour le dialogue
     */
    private Callback<ButtonType, String> createResultConverter() {
        return buttonType -> {
            if (buttonType == ButtonType.OK) {
                return nameField.getText();
            }
            return null;
        };
    }
}