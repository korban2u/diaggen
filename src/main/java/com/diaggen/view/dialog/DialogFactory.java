package com.diaggen.view.dialog;

import com.diaggen.model.*;
import com.diaggen.view.dialog.controller.*;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

import java.io.IOException;

public class DialogFactory {

    private static DialogFactory instance;

    private DialogFactory() {}

    public static DialogFactory getInstance() {
        if (instance == null) {
            instance = new DialogFactory();
        }
        return instance;
    }

    public Dialog<Parameter> createParameterEditorDialog(Parameter parameter) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialog/ParameterEditorDialog.fxml"));
            DialogPane dialogPane = loader.load();

            Dialog<Parameter> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);

            ParameterEditorController controller = loader.getController();
            controller.setDialog(dialog, parameter);

            return dialog;
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors du chargement du dialogue d'édition de paramètres", e);
        }
    }

    public Dialog<Member> createAttributeEditorDialog(Member attribute) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialog/AttributeEditorDialog.fxml"));
            DialogPane dialogPane = loader.load();

            Dialog<Member> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);

            AttributeEditorController controller = loader.getController();
            controller.setDialog(dialog, attribute);

            return dialog;
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors du chargement du dialogue d'édition d'attributs", e);
        }
    }

    public Dialog<Method> createMethodEditorDialog(Method method) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialog/MethodEditorDialog.fxml"));
            DialogPane dialogPane = loader.load();

            Dialog<Method> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);

            MethodEditorController controller = loader.getController();
            controller.setDialog(dialog, method);

            return dialog;
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors du chargement du dialogue d'édition de méthodes", e);
        }
    }

    public Dialog<DiagramClass> createClassEditorDialog(DiagramClass diagramClass) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialog/ClassEditorDialog.fxml"));
            DialogPane dialogPane = loader.load();

            Dialog<DiagramClass> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);

            ClassEditorController controller = loader.getController();
            controller.setDialog(dialog, diagramClass);

            return dialog;
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors du chargement du dialogue d'édition de classes", e);
        }
    }

    public Dialog<DiagramRelation> createRelationEditorDialog(DiagramRelation relation,
                                                              ObservableList<DiagramClass> classes) {
        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialog/RelationEditorDialog.fxml"));
            DialogPane dialogPane = loader.load();

            Dialog<DiagramRelation> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);

            EnhancedRelationEditorController controller = loader.getController();
            controller.setDialog(dialog, relation, classes);

            return dialog;
        } catch (IOException e) {

            try {

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialog/RelationEditorDialog.fxml"));
                DialogPane dialogPane = loader.load();

                Dialog<DiagramRelation> dialog = new Dialog<>();
                dialog.setDialogPane(dialogPane);

                RelationEditorController controller = loader.getController();
                controller.setDialog(dialog, relation, classes);

                return dialog;
            } catch (IOException ex) {
                throw new RuntimeException("Erreur lors du chargement du dialogue d'édition de relations", ex);
            }
        }
    }

    public Dialog<String> createDiagramPropertiesDialog(ClassDiagram diagram) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialog/DiagramPropertiesDialog.fxml"));
            DialogPane dialogPane = loader.load();

            Dialog<String> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);

            DiagramPropertiesController controller = loader.getController();
            controller.setDialog(dialog, diagram);

            return dialog;
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors du chargement du dialogue de propriétés du diagramme", e);
        }
    }
}