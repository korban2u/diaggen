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
import java.nio.file.Path;
import java.util.Optional;

/**
 * Contrôleur principal de l'application
 */
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

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public void initialize() {
        viewController.updateDiagramList(diagramStore.getDiagrams());

        if (diagramStore.getActiveDiagram() != null) {
            loadDiagram(diagramStore.getActiveDiagram());
        }

        // Configurer le canvas pour qu'il notifie le contrôleur lors d'une demande d'ajout de classe
        diagramCanvas.setOnAddClassRequest(this::handleAddClass);
        diagramCanvas.setOnDeleteRequest(this::handleDeleteSelected);
    }

    /**
     * Gère la suppression de l'élément sélectionné (classe ou relation)
     */
    private void handleDeleteSelected() {
        DiagramClass selectedClass = viewController.getSelectedClass();
        DiagramRelation selectedRelation = viewController.getSelectedRelation();

        if (selectedClass != null) {
            handleDeleteClass();
        } else if (selectedRelation != null) {
            handleDeleteRelation();
        }
    }

    /**
     * Charge un diagramme dans l'application
     * @param diagram Le diagramme à charger
     */
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

        // Utiliser la classe sélectionnée du viewController
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

        // Utiliser la classe sélectionnée du viewController
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

        // Utiliser la relation sélectionnée du viewController
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
                // Si oui, créer et exécuter une commande de changement de type avec rafraîchissement
                ChangeRelationTypeCommand command = new ChangeRelationTypeCommand(
                        currentDiagram, selectedRelation, updatedRelation.getRelationType(), diagramCanvas);
                commandManager.executeCommand(command);

                // La mise à jour de l'affichage et la sélection sont gérées par la commande
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

        // Utiliser la relation sélectionnée du viewController
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

    /**
     * Change le type d'une relation (méthode utilitaire)
     * @param relation La relation à modifier
     * @param newType Le nouveau type de relation
     */
    public void changeRelationType(DiagramRelation relation, RelationType newType) {
        if (relation == null || relation.getRelationType() == newType) return;

        ClassDiagram currentDiagram = diagramStore.getActiveDiagram();
        if (currentDiagram == null) return;

        // Créer et exécuter la commande avec rafraîchissement automatique
        ChangeRelationTypeCommand command = new ChangeRelationTypeCommand(
                currentDiagram, relation, newType, diagramCanvas);
        commandManager.executeCommand(command);

        // Mettre à jour le statut
        viewController.setStatus("Type de relation modifié en " + newType.getDisplayName());
    }

    /**
     * Gère l'enregistrement du diagramme actif
     */
    public void handleSave() {
        File currentFile = diagramStore.getCurrentFile();
        if (currentFile != null) {
            saveToFile(currentFile);
        } else {
            handleSaveAs();
        }
    }

    /**
     * Gère l'enregistrement du diagramme actif avec choix de fichier
     */
    public void handleSaveAs() {
        if (diagramStore.getActiveDiagram() == null) {
            showWarning("Aucun diagramme actif", "Il n'y a pas de diagramme à enregistrer.");
            return;
        }

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

    /**
     * Enregistre le diagramme dans un fichier
     * @param file Le fichier de destination
     */
    private void saveToFile(File file) {
        try {
            DiagramSerializer serializer = new DiagramSerializer();
            serializer.serialize(diagramStore.getActiveDiagram(), file);
            viewController.setStatus("Diagramme enregistré dans " + file.getName());
        } catch (IOException e) {
            showError("Erreur lors de l'enregistrement",
                    "Une erreur est survenue lors de l'enregistrement du diagramme : " + e.getMessage());
        }
    }

    /**
     * Gère l'ouverture d'un fichier diagramme
     */
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
                ClassDiagram loadedDiagram = serializer.deserialize(file);

                // Ajouter le diagramme chargé à la liste et le définir comme actif
                diagramStore.getDiagrams().add(loadedDiagram);
                diagramStore.setActiveDiagram(loadedDiagram);
                diagramStore.setCurrentFile(file);

                // Mettre à jour l'interface
                viewController.updateDiagramList(diagramStore.getDiagrams());
                loadDiagram(loadedDiagram);
                viewController.setStatus("Diagramme chargé depuis " + file.getName());
            } catch (IOException | ClassNotFoundException e) {
                showError("Erreur lors de l'ouverture",
                        "Une erreur est survenue lors de l'ouverture du diagramme : " + e.getMessage());
            }
        }
    }

    /**
     * Gère l'importation de code Java
     */
    public void handleImportJavaCode() {
        Window window = viewController.getWindow();

        // Choisir un fichier Java ou un répertoire
        Alert choiceAlert = new Alert(Alert.AlertType.CONFIRMATION);
        choiceAlert.setTitle("Importer du code Java");
        choiceAlert.setHeaderText("Choisir le type d'importation");
        choiceAlert.setContentText("Voulez-vous importer un fichier Java unique ou un projet/dossier complet?");

        ButtonType fileButton = new ButtonType("Fichier unique");
        ButtonType dirButton = new ButtonType("Projet/Dossier");
        ButtonType cancelButton = ButtonType.CANCEL;

        choiceAlert.getButtonTypes().setAll(fileButton, dirButton, cancelButton);

        Optional<ButtonType> choice = choiceAlert.showAndWait();
        if (!choice.isPresent()) {
            return;
        }

        JavaCodeParser parser = new JavaCodeParser();
        ClassDiagram parsedDiagram = null;

        if (choice.get() == fileButton) {
            // Importer un fichier unique
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Importer un fichier Java");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Fichiers Java (*.java)", "*.java"));

            File file = fileChooser.showOpenDialog(window);
            if (file != null) {
                try {
                    parsedDiagram = parser.parseFile(file);
                } catch (Exception e) {
                    showError("Erreur d'importation",
                            "Erreur lors de l'analyse du fichier Java: " + e.getMessage());
                    return;
                }
            } else {
                return; // Opération annulée
            }
        } else if (choice.get() == dirButton) {
            // Importer un projet/dossier
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle("Importer un projet Java");

            File dir = dirChooser.showDialog(window);
            if (dir != null) {
                try {
                    parsedDiagram = parser.parseProject(dir.toPath());
                } catch (Exception e) {
                    showError("Erreur d'importation",
                            "Erreur lors de l'analyse du projet Java: " + e.getMessage());
                    return;
                }
            } else {
                return; // Opération annulée
            }
        } else {
            return; // Opération annulée
        }

        if (parsedDiagram != null) {
            // Ajouter le diagramme importé à la liste et le définir comme actif
            diagramStore.getDiagrams().add(parsedDiagram);
            diagramStore.setActiveDiagram(parsedDiagram);
            diagramStore.setCurrentFile(null); // Pas encore enregistré

            // Mettre à jour l'interface
            viewController.updateDiagramList(diagramStore.getDiagrams());
            loadDiagram(parsedDiagram);
            viewController.setStatus("Code Java importé avec succès");

            // Disposition automatique des classes si nécessaire
            arrangeClassesAutomatically(parsedDiagram);
        }
    }

    /**
     * Arrange les classes dans le diagramme en grille
     * @param diagram Le diagramme à arranger
     */
    private void arrangeClassesAutomatically(ClassDiagram diagram) {
        if (diagram == null || diagram.getClasses().isEmpty()) return;

        final int GRID_WIDTH = 250;
        final int GRID_HEIGHT = 200;
        final int MAX_COLUMNS = 4;

        int row = 0;
        int col = 0;

        for (DiagramClass diagramClass : diagram.getClasses()) {
            diagramClass.setX(50 + col * GRID_WIDTH);
            diagramClass.setY(50 + row * GRID_HEIGHT);

            col++;
            if (col >= MAX_COLUMNS) {
                col = 0;
                row++;
            }
        }

        // Rafraîchir l'affichage
        diagramCanvas.refresh();
    }

    /**
     * Gère l'exportation du diagramme au format PNG
     */
    public void handleExportImage() {
        ClassDiagram currentDiagram = diagramStore.getActiveDiagram();
        if (currentDiagram == null) {
            showWarning("Aucun diagramme actif", "Il n'y a pas de diagramme à exporter.");
            return;
        }

        Window window = viewController.getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter en PNG");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images PNG (*.png)", "*.png"));

        File file = fileChooser.showSaveDialog(window);
        if (file != null) {
            if (!file.getName().endsWith(".png")) {
                file = new File(file.getAbsolutePath() + ".png");
            }

            try {
                exportService.exportDiagram(currentDiagram, "png", file);
                viewController.setStatus("Diagramme exporté en PNG: " + file.getName());
            } catch (IOException e) {
                showError("Erreur d'exportation",
                        "Erreur lors de l'exportation en PNG: " + e.getMessage());
            }
        }
    }

    /**
     * Gère l'exportation du diagramme au format SVG
     */
    public void handleExportSVG() {
        ClassDiagram currentDiagram = diagramStore.getActiveDiagram();
        if (currentDiagram == null) {
            showWarning("Aucun diagramme actif", "Il n'y a pas de diagramme à exporter.");
            return;
        }

        Window window = viewController.getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter en SVG");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images SVG (*.svg)", "*.svg"));

        File file = fileChooser.showSaveDialog(window);
        if (file != null) {
            if (!file.getName().endsWith(".svg")) {
                file = new File(file.getAbsolutePath() + ".svg");
            }

            try {
                exportService.exportDiagram(currentDiagram, "svg", file);
                viewController.setStatus("Diagramme exporté en SVG: " + file.getName());
            } catch (IOException e) {
                showError("Erreur d'exportation",
                        "Erreur lors de l'exportation en SVG: " + e.getMessage());
            }
        }
    }

    /**
     * Gère l'exportation du diagramme au format PlantUML
     */
    public void handleExportPlantUML() {
        ClassDiagram currentDiagram = diagramStore.getActiveDiagram();
        if (currentDiagram == null) {
            showWarning("Aucun diagramme actif", "Il n'y a pas de diagramme à exporter.");
            return;
        }

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
                exportService.exportDiagram(currentDiagram, "puml", file);
                viewController.setStatus("Diagramme exporté en PlantUML: " + file.getName());
            } catch (IOException e) {
                showError("Erreur d'exportation",
                        "Erreur lors de l'exportation en PlantUML: " + e.getMessage());
            }
        }
    }

    /**
     * Gère l'exportation du diagramme en code Java
     */
    public void handleExportJavaCode() {
        ClassDiagram currentDiagram = diagramStore.getActiveDiagram();
        if (currentDiagram == null) {
            showWarning("Aucun diagramme actif", "Il n'y a pas de diagramme à exporter.");
            return;
        }

        Window window = viewController.getWindow();

        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Exporter en code Java - Sélectionner le répertoire de destination");

        File dir = dirChooser.showDialog(window);
        if (dir != null) {
            if (!dir.isDirectory()) {
                showError("Erreur de sélection", "Veuillez sélectionner un répertoire valide.");
                return;
            }

            try {
                exportService.exportDiagram(currentDiagram, "java", dir);
                viewController.setStatus("Code Java généré dans: " + dir.getAbsolutePath());

                // Demander à l'utilisateur s'il souhaite ouvrir le répertoire
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Exportation réussie");
                alert.setHeaderText("Le code Java a été généré avec succès");
                alert.setContentText("Voulez-vous ouvrir le répertoire contenant les fichiers?");

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    try {
                        // Ouvrir le répertoire avec l'explorateur de fichiers par défaut
                        java.awt.Desktop.getDesktop().open(dir);
                    } catch (IOException e) {
                        // Ignorer silencieusement si l'ouverture échoue
                    }
                }
            } catch (IOException e) {
                showError("Erreur d'exportation",
                        "Erreur lors de la génération du code Java: " + e.getMessage());
            }
        }
    }

    /**
     * Affiche un message d'information
     * @param title Le titre de la boîte de dialogue
     * @param message Le message à afficher
     */
    private void showInfo(String title, String message) {
        AlertHelper.showInfo(title, message);
    }

    /**
     * Affiche un message d'avertissement
     * @param title Le titre de la boîte de dialogue
     * @param message Le message à afficher
     */
    private void showWarning(String title, String message) {
        AlertHelper.showWarning(title, message);
    }

    /**
     * Affiche un message d'erreur
     * @param title Le titre de la boîte de dialogue
     * @param message Le message à afficher
     */
    private void showError(String title, String message) {
        AlertHelper.showError(title, message);
    }

    /**
     * Gère l'annulation de la dernière action
     */
    public void handleUndo() {
        if (commandManager.canUndo()) {
            commandManager.undo();
            diagramCanvas.refresh();
            viewController.setStatus("Action annulée");
        }
    }

    /**
     * Gère le rétablissement de la dernière action annulée
     */
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