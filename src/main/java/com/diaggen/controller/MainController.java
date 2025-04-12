package com.diaggen.controller;

import com.diaggen.controller.command.*;
import com.diaggen.model.*;
import com.diaggen.model.java.JavaCodeParser;
import com.diaggen.model.persist.DiagramSerializer;
import com.diaggen.service.ExportService;
import com.diaggen.util.AlertHelper;
import com.diaggen.view.controller.MainViewController;
import com.diaggen.view.diagram.DiagramCanvas;
import com.diaggen.view.dialog.DialogFactory;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class MainController {

    private final DiagramStore diagramStore;
    private final DialogFactory dialogFactory;
    private final ExportService exportService;
    private final CommandManager commandManager;

    private MainViewController viewController;
    private DiagramCanvas diagramCanvas;

    public MainController(DiagramStore diagramStore, DialogFactory dialogFactory, ExportService exportService) {
        this.diagramStore = diagramStore;
        this.dialogFactory = dialogFactory;
        this.exportService = exportService;
        this.commandManager = new CommandManager();
    }

    public void setMainViewController(MainViewController viewController) {
        this.viewController = viewController;
        this.diagramCanvas = viewController.getDiagramCanvas();
    }

    public void initialize() {
        viewController.updateDiagramList(diagramStore.getDiagrams());

        if (diagramStore.getActiveDiagram() != null) {
            loadDiagram(diagramStore.getActiveDiagram());
        }

        // Configurer le canvas pour qu'il notifie le contrôleur lors d'une demande d'ajout de classe
        diagramCanvas.setOnAddClassRequest(this::handleAddClass);
    }

    private void loadDiagram(ClassDiagram diagram) {
        diagramStore.setActiveDiagram(diagram);
        diagramCanvas.loadDiagram(diagram);
        viewController.updateSelectedDiagram(diagram);
    }

    public void handleSelectDiagram(ClassDiagram diagram) {
        loadDiagram(diagram);
    }

    public void handleNewDiagram() {
        ClassDiagram diagram = diagramStore.createNewDiagram("Nouveau diagramme");
        viewController.updateDiagramList(diagramStore.getDiagrams());
        loadDiagram(diagram);
    }

    public void handleEditDiagram() {
        ClassDiagram currentDiagram = diagramStore.getActiveDiagram();
        if (currentDiagram == null) return;

        var dialog = dialogFactory.createDiagramPropertiesDialog(currentDiagram);
        dialog.showAndWait().ifPresent(result -> {
            currentDiagram.setName(result);
            viewController.updateDiagramList(diagramStore.getDiagrams());
            viewController.updateSelectedDiagram(currentDiagram);
        });
    }

    public void handleDeleteDiagram() {
        ClassDiagram currentDiagram = diagramStore.getActiveDiagram();
        if (currentDiagram == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer le diagramme");
        alert.setHeaderText("Supprimer \"" + currentDiagram.getName() + "\"");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer ce diagramme ? Cette action est irréversible.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            diagramStore.removeDiagram(currentDiagram);
            viewController.updateDiagramList(diagramStore.getDiagrams());

            if (diagramStore.getActiveDiagram() != null) {
                loadDiagram(diagramStore.getActiveDiagram());
            } else {
                diagramCanvas.clear();
            }
        }
    }


    /**
     * Gestion de l'ajout d'une classe
     */
    public void handleAddClass() {
        ClassDiagram currentDiagram = diagramStore.getActiveDiagram();
        if (currentDiagram == null) return;

        var dialog = dialogFactory.createClassEditorDialog(null);
        dialog.showAndWait().ifPresent(diagramClass -> {

            AddClassCommand command = new AddClassCommand(currentDiagram, diagramClass);
            commandManager.executeCommand(command);

            diagramCanvas.refresh();

            // Sélectionner automatiquement la nouvelle classe
            diagramCanvas.selectClass(diagramClass);

            viewController.setStatus("Classe " + diagramClass.getName() + " ajoutée");
        });
    }

    public void handleEditClass() {
        ClassDiagram currentDiagram = diagramStore.getActiveDiagram();
        if (currentDiagram == null) return;

        // Utiliser la classe sélectionnée du viewController au lieu du canvas
        DiagramClass selectedClass = viewController.getSelectedClass();
        if (selectedClass == null) {
            showWarning("Aucune classe sélectionnée",
                    "Veuillez sélectionner une classe à modifier.");
            return;
        }

        var dialog = dialogFactory.createClassEditorDialog(selectedClass);
        dialog.showAndWait().ifPresent(updatedClass -> {
            // Rafraîchir le canvas pour refléter les changements
            diagramCanvas.refresh();
            // Maintenir la sélection
            diagramCanvas.selectClass(selectedClass);
        });
    }

    public void handleDeleteClass() {
        ClassDiagram currentDiagram = diagramStore.getActiveDiagram();
        if (currentDiagram == null) return;

        // Utiliser la classe sélectionnée du viewController au lieu du canvas
        DiagramClass selectedClass = viewController.getSelectedClass();
        if (selectedClass == null) {
            showWarning("Aucune classe sélectionnée",
                    "Veuillez sélectionner une classe à supprimer.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer la classe");
        alert.setHeaderText("Supprimer \"" + selectedClass.getName() + "\"");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cette classe ? Cette action supprimera également toutes les relations associées.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Créer et exécuter la commande
            RemoveClassCommand command = new RemoveClassCommand(currentDiagram, selectedClass);
            commandManager.executeCommand(command);

            // Rafraîchir le canvas
            diagramCanvas.refresh();

            // Désélectionner tout
            diagramCanvas.deselectAll();

            // Mettre à jour le statut
            viewController.setStatus("Classe " + selectedClass.getName() + " supprimée");
        }
    }

    public void handleAddRelation() {
        ClassDiagram currentDiagram = diagramStore.getActiveDiagram();
        if (currentDiagram == null) return;

        var classes = currentDiagram.getClasses();
        if (classes.size() < 2) {
            showWarning("Impossible d'ajouter une relation",
                    "Vous devez avoir au moins deux classes pour créer une relation.");
            return;
        }

        var dialog = dialogFactory.createRelationEditorDialog(null, classes);
        dialog.showAndWait().ifPresent(relation -> {
            // Créer et exécuter la commande pour ajouter une relation
            AddRelationCommand command = new AddRelationCommand(currentDiagram, relation);
            commandManager.executeCommand(command);

            // Rafraîchir le canvas
            diagramCanvas.refresh();

            // Sélectionner automatiquement la nouvelle relation
            diagramCanvas.selectRelation(relation);

            // Mettre à jour le statut
            viewController.setStatus("Relation ajoutée entre " + relation.getSourceClass().getName() +
                    " et " + relation.getTargetClass().getName());
        });
    }


    public void handleEditRelation() {
        ClassDiagram currentDiagram = diagramStore.getActiveDiagram();
        if (currentDiagram == null) return;

        // Utiliser la relation sélectionnée du viewController au lieu du canvas
        DiagramRelation selectedRelation = viewController.getSelectedRelation();
        if (selectedRelation == null) {
            showWarning("Aucune relation sélectionnée",
                    "Veuillez sélectionner une relation à modifier.");
            return;
        }

        // Obtenir le type de relation original avant modification
        RelationType originalType = selectedRelation.getRelationType();

        var dialog = dialogFactory.createRelationEditorDialog(
                selectedRelation, currentDiagram.getClasses());

        dialog.showAndWait().ifPresent(updatedRelation -> {
            // Vérifier si le type de relation a changé
            if (!updatedRelation.getRelationType().equals(originalType)) {
                // Si oui, créer et exécuter une commande de changement de type
                ChangeRelationTypeCommand command = new ChangeRelationTypeCommand(
                        currentDiagram, selectedRelation, updatedRelation.getRelationType());
                commandManager.executeCommand(command);

                // Rafraîchir le canvas
                diagramCanvas.refresh();

                // Sélectionner la nouvelle relation (puisque l'ancienne n'existe plus)
                // La relation est maintenant la dernière relation ajoutée
                DiagramRelation newRelation = currentDiagram.getRelations().get(
                        currentDiagram.getRelations().size() - 1);
                diagramCanvas.selectRelation(newRelation);
            } else {
                // Sinon, juste rafraîchir le canvas
                diagramCanvas.refresh();

                // Maintenir la sélection
                diagramCanvas.selectRelation(selectedRelation);
            }

            // Mettre à jour le statut
            viewController.setStatus("Relation modifiée");
        });
    }

    public void handleDeleteRelation() {
        ClassDiagram currentDiagram = diagramStore.getActiveDiagram();
        if (currentDiagram == null) return;

        // Utiliser la relation sélectionnée du viewController au lieu du canvas
        DiagramRelation selectedRelation = viewController.getSelectedRelation();
        if (selectedRelation == null) {
            showWarning("Aucune relation sélectionnée",
                    "Veuillez sélectionner une relation à supprimer.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer la relation");
        alert.setHeaderText("Supprimer la relation");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cette relation ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Créer et exécuter la commande pour supprimer une relation
            RemoveRelationCommand command = new RemoveRelationCommand(currentDiagram, selectedRelation);
            commandManager.executeCommand(command);

            // Rafraîchir le canvas
            diagramCanvas.refresh();

            // Désélectionner tout
            diagramCanvas.deselectAll();

            // Mettre à jour le statut
            viewController.setStatus("Relation supprimée");
        }
    }


    public void handleSave() {
        File currentFile = diagramStore.getCurrentFile();
        if (currentFile != null) {
            saveToFile(currentFile);
        } else {
            handleSaveAs();
        }
    }

    public void handleSaveAs() {
        Window window = viewController.getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le diagramme");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers DiagGen (*.dgn)", "*.dgn"));

        File file = fileChooser.showSaveDialog(window);
        if (file != null) {
            if (!file.getName().endsWith(".dgn")) {
                file = new File(file.getAbsolutePath() + ".dgn");
            }
            saveToFile(file);
            diagramStore.setCurrentFile(file);
        }
    }

    private void saveToFile(File file) {
        try {
            DiagramSerializer serializer = new DiagramSerializer();
            serializer.serialize(diagramStore.getActiveDiagram(), file);
            showInfo("Enregistrement réussi", "Le diagramme a été enregistré avec succès.");
        } catch (IOException e) {
            showError("Erreur lors de l'enregistrement",
                    "Une erreur est survenue lors de l'enregistrement du diagramme : " + e.getMessage());
        }
    }

    public void handleOpen() {
        Window window = viewController.getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Ouvrir un diagramme");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers DiagGen (*.dgn)", "*.dgn"));

        File file = fileChooser.showOpenDialog(window);
        if (file != null) {
            try {
                DiagramSerializer serializer = new DiagramSerializer();
                ClassDiagram diagram = serializer.deserialize(file);

                diagramStore.getDiagrams().add(diagram);
                diagramStore.setCurrentFile(file);
                viewController.updateDiagramList(diagramStore.getDiagrams());
                loadDiagram(diagram);

                showInfo("Chargement réussi", "Le diagramme a été chargé avec succès.");
            } catch (Exception e) {
                showError("Erreur lors du chargement",
                        "Une erreur est survenue lors du chargement du diagramme : " + e.getMessage());
            }
        }
    }

    public void handleImportJavaCode() {
        Window window = viewController.getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Importer du code Java");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers Java (*.java)", "*.java"));

        File file = fileChooser.showOpenDialog(window);
        if (file != null) {
            try {
                JavaCodeParser parser = new JavaCodeParser();
                ClassDiagram diagram = parser.parseFile(file);

                diagramStore.getDiagrams().add(diagram);
                viewController.updateDiagramList(diagramStore.getDiagrams());
                loadDiagram(diagram);

                showInfo("Importation réussie", "Le code Java a été importé avec succès.");
            } catch (Exception e) {
                showError("Erreur lors de l'importation",
                        "Une erreur est survenue lors de l'importation du code Java : " + e.getMessage());
            }
        }
    }

    public void handleExportImage() {
        Window window = viewController.getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter en image PNG");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers PNG (*.png)", "*.png"));

        File file = fileChooser.showSaveDialog(window);
        if (file != null) {
            if (!file.getName().endsWith(".png")) {
                file = new File(file.getAbsolutePath() + ".png");
            }
            try {
                exportService.exportDiagram(diagramStore.getActiveDiagram(), "png", file);
                showInfo("Exportation réussie", "Le diagramme a été exporté en PNG avec succès.");
            } catch (IOException e) {
                showError("Erreur lors de l'exportation",
                        "Une erreur est survenue lors de l'exportation en PNG : " + e.getMessage());
            }
        }
    }

    public void handleExportSVG() {
        Window window = viewController.getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter en SVG");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers SVG (*.svg)", "*.svg"));

        File file = fileChooser.showSaveDialog(window);
        if (file != null) {
            if (!file.getName().endsWith(".svg")) {
                file = new File(file.getAbsolutePath() + ".svg");
            }
            try {
                exportService.exportDiagram(diagramStore.getActiveDiagram(), "svg", file);
                showInfo("Exportation réussie", "Le diagramme a été exporté en SVG avec succès.");
            } catch (IOException e) {
                showError("Erreur lors de l'exportation",
                        "Une erreur est survenue lors de l'exportation en SVG : " + e.getMessage());
            }
        }
    }

    public void handleExportPlantUML() {
        Window window = viewController.getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter en PlantUML");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers PlantUML (*.puml)", "*.puml"));

        File file = fileChooser.showSaveDialog(window);
        if (file != null) {
            if (!file.getName().endsWith(".puml")) {
                file = new File(file.getAbsolutePath() + ".puml");
            }
            try {
                exportService.exportDiagram(diagramStore.getActiveDiagram(), "puml", file);
                showInfo("Exportation réussie", "Le diagramme a été exporté en PlantUML avec succès.");
            } catch (IOException e) {
                showError("Erreur lors de l'exportation",
                        "Une erreur est survenue lors de l'exportation en PlantUML : " + e.getMessage());
            }
        }
    }

    public void handleExportJavaCode() {
        Window window = viewController.getWindow();

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Exporter en code Java");

        File directory = directoryChooser.showDialog(window);
        if (directory != null) {
            try {
                exportService.exportDiagram(diagramStore.getActiveDiagram(), "java", directory);
                showInfo("Exportation réussie", "Le diagramme a été exporté en code Java avec succès.");
            } catch (IOException e) {
                showError("Erreur lors de l'exportation",
                        "Une erreur est survenue lors de l'exportation en code Java : " + e.getMessage());
            }
        }
    }

    private void showInfo(String title, String message) {
        AlertHelper.showInfo(title, message);
    }


    private void showWarning(String title, String message) {
        AlertHelper.showWarning(title, message);
    }

    private void showError(String title, String message) {
        AlertHelper.showError(title, message);
    }

    public void handleUndo() {
        if (commandManager.canUndo()) {
            commandManager.undo();
            diagramCanvas.refresh();
            viewController.setStatus("Action annulée");
        }
    }

    public void handleRedo() {
        if (commandManager.canRedo()) {
            commandManager.redo();
            diagramCanvas.refresh();
            viewController.setStatus("Action rétablie");
        }
    }

    /**
     * Donne accès au DiagramStore pour des opérations spéciales
     * @return le DiagramStore
     */
    public DiagramStore getDiagramStore() {
        return diagramStore;
    }

    /**
     * Ajoute une nouvelle classe directement au diagramme actif
     * @param newClass la classe à ajouter
     */
    public void addNewClass(DiagramClass newClass) {
        ClassDiagram currentDiagram = diagramStore.getActiveDiagram();
        if (currentDiagram != null) {
            // Créer et exécuter la commande pour ajouter une classe
            AddClassCommand command = new AddClassCommand(currentDiagram, newClass);
            commandManager.executeCommand(command);

            // Rafraîchir le canvas
            diagramCanvas.refresh();

            // Sélectionner la nouvelle classe
            diagramCanvas.selectClass(newClass);

            // Mettre à jour le statut
            viewController.setStatus("Classe " + newClass.getName() + " ajoutée");
        }
    }
}