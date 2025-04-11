package com.diaggen.view.dialog;

import com.diaggen.model.*;
import com.diaggen.view.dialog.controller.*;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

import java.io.IOException;

/**
 * Factory pour la création de dialogues basés sur FXML
 */
public class DialogFactory {

    private static DialogFactory instance;

    private DialogFactory() {}

    public static DialogFactory getInstance() {
        if (instance == null) {
            instance = new DialogFactory();
        }
        return instance;
    }

    /**
     * Crée un dialogue d'édition de paramètres
     * @param parameter Le paramètre à éditer (null pour création)
     * @return Le dialogue configuré
     */
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

    /**
     * Crée un dialogue d'édition d'attributs
     * @param attribute L'attribut à éditer (null pour création)
     * @return Le dialogue configuré
     */
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

    /**
     * Crée un dialogue d'édition de méthodes
     * @param method La méthode à éditer (null pour création)
     * @return Le dialogue configuré
     */
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

    /**
     * Crée un dialogue d'édition de classes
     * @param diagramClass La classe à éditer (null pour création)
     * @return Le dialogue configuré
     */
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

    /**
     * Crée un dialogue d'édition de relations
     * @param relation La relation à éditer (null pour création)
     * @param classes La liste des classes disponibles
     * @return Le dialogue configuré
     */
    public Dialog<DiagramRelation> createRelationEditorDialog(DiagramRelation relation,
                                                              ObservableList<DiagramClass> classes) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialog/RelationEditorDialog.fxml"));
            DialogPane dialogPane = loader.load();

            Dialog<DiagramRelation> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);

            RelationEditorController controller = loader.getController();
            controller.setDialog(dialog, relation, classes);

            return dialog;
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors du chargement du dialogue d'édition de relations", e);
        }
    }

    /**
     * Crée un dialogue de propriétés de diagramme
     * @param diagram Le diagramme à éditer
     * @return Le dialogue configuré
     */
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