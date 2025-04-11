package com.diaggen.controller;

import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramClass;
import com.diaggen.model.DiagramStore;
import com.diaggen.model.DiagramRelation;
import com.diaggen.model.export.DiagramExporter;
import com.diaggen.model.export.JavaCodeExporter;
import com.diaggen.model.export.PNGExporter;
import com.diaggen.model.export.PlantUMLExporter;
import com.diaggen.model.export.SVGExporter;
import com.diaggen.model.java.JavaCodeParser;
import com.diaggen.model.persist.DiagramSerializer;
import com.diaggen.view.MainView;
import com.diaggen.view.ViewFactory;
import com.diaggen.view.dialog.ClassEditorDialog;
import com.diaggen.view.dialog.DiagramPropertiesDialog;
import com.diaggen.view.dialog.RelationEditorDialog;
import com.diaggen.view.diagram.DiagramCanvas;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class MainController {
    private final DiagramStore diagramStore;
    private final ViewFactory viewFactory;
    private MainView mainView;
    private DiagramCanvas diagramCanvas;

    public MainController(DiagramStore diagramStore, ViewFactory viewFactory) {
        this.diagramStore = diagramStore;
        this.viewFactory = viewFactory;
        initialize();
    }

    private void initialize() {
        mainView = viewFactory.createMainView();
        diagramCanvas = mainView.getDiagramCanvas();

        bindEventHandlers();
        updateDiagramList();

        if (diagramStore.getActiveDiagram() != null) {
            loadDiagram(diagramStore.getActiveDiagram());
        }
    }

    private void bindEventHandlers() {
        mainView.setOnNewDiagram(this::handleNewDiagram);
        mainView.setOnEditDiagram(this::handleEditDiagram);
        mainView.setOnDeleteDiagram(this::handleDeleteDiagram);
        mainView.setOnSelectDiagram(this::handleSelectDiagram);

        mainView.setOnAddClass(this::handleAddClass);
        mainView.setOnEditClass(this::handleEditClass);
        mainView.setOnDeleteClass(this::handleDeleteClass);

        mainView.setOnAddRelation(this::handleAddRelation);
        mainView.setOnEditRelation(this::handleEditRelation);
        mainView.setOnDeleteRelation(this::handleDeleteRelation);

        mainView.setOnSave(this::handleSave);
        mainView.setOnSaveAs(this::handleSaveAs);
        mainView.setOnOpen(this::handleOpen);

        mainView.setOnImportJavaCode(this::handleImportJavaCode);
        mainView.setOnExportImage(this::handleExportImage);
        mainView.setOnExportSVG(this::handleExportSVG);
        mainView.setOnExportPlantUML(this::handleExportPlantUML);
        mainView.setOnExportJavaCode(this::handleExportJavaCode);
    }

    private void updateDiagramList() {
        mainView.updateDiagramList(diagramStore.getDiagrams());
    }

    private void loadDiagram(ClassDiagram diagram) {
        diagramStore.setActiveDiagram(diagram);
        diagramCanvas.loadDiagram(diagram);
        mainView.updateSelectedDiagram(diagram);
    }

    public MainView getMainView() {
        return mainView;
    }

    private void handleNewDiagram() {
        ClassDiagram diagram = diagramStore.createNewDiagram("Nouveau diagramme");
        updateDiagramList();
        loadDiagram(diagram);
    }

    private void handleEditDiagram() {
        ClassDiagram currentDiagram = diagramStore.getActiveDiagram();
        if (currentDiagram == null) return;

        DiagramPropertiesDialog dialog = viewFactory.createDiagramPropertiesDialog(currentDiagram);
        dialog.showAndWait().ifPresent(result -> {
            currentDiagram.setName(result);
            updateDiagramList();
            mainView.updateSelectedDiagram(currentDiagram);
        });
    }

    private void handleDeleteDiagram() {
        ClassDiagram currentDiagram = diagramStore.getActiveDiagram();
        if (currentDiagram == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer le diagramme");
        alert.setHeaderText("Supprimer \"" + currentDiagram.getName() + "\"");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer ce diagramme ? Cette action est irréversible.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            diagramStore.removeDiagram(currentDiagram);
            updateDiagramList();

            if (diagramStore.getActiveDiagram() != null) {
                loadDiagram(diagramStore.getActiveDiagram());
            } else {
                diagramCanvas.clear();
            }
        }
    }

    private void handleSelectDiagram(ClassDiagram diagram) {
        loadDiagram(diagram);
    }

    private void handleAddClass() {
        ClassDiagram currentDiagram = diagramStore.getActiveDiagram();
        if (currentDiagram == null) return;

        ClassEditorDialog dialog = viewFactory.createClassEditorDialog(null);
        dialog.showAndWait().ifPresent(diagramClass -> {
            currentDiagram.addClass(diagramClass);
            diagramCanvas.refresh();
        });
    }

    private void handleEditClass() {
        ClassDiagram currentDiagram = diagramStore.getActiveDiagram();
        if (currentDiagram == null) return;

        DiagramClass selectedClass = diagramCanvas.getSelectedClass();
        if (selectedClass == null) {
            showWarning("Aucune classe sélectionnée",
                    "Veuillez sélectionner une classe à modifier.");
            return;
        }

        ClassEditorDialog dialog = viewFactory.createClassEditorDialog(selectedClass);
        dialog.showAndWait().ifPresent(updatedClass -> diagramCanvas.refresh());
    }

    private void handleDeleteClass() {
        ClassDiagram currentDiagram = diagramStore.getActiveDiagram();
        if (currentDiagram == null) return;

        DiagramClass selectedClass = diagramCanvas.getSelectedClass();
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
            currentDiagram.removeClass(selectedClass);
            diagramCanvas.refresh();
        }
    }

    private void handleAddRelation() {
        ClassDiagram currentDiagram = diagramStore.getActiveDiagram();
        if (currentDiagram == null) return;

        ObservableList<DiagramClass> classes = currentDiagram.getClasses();
        if (classes.size() < 2) {
            showWarning("Impossible d'ajouter une relation",
                    "Vous devez avoir au moins deux classes pour créer une relation.");
            return;
        }

        RelationEditorDialog dialog = viewFactory.createRelationEditorDialog(null, classes);
        dialog.showAndWait().ifPresent(relation -> {
            currentDiagram.addRelation(relation);
            diagramCanvas.refresh();
        });
    }

    private void handleEditRelation() {
        ClassDiagram currentDiagram = diagramStore.getActiveDiagram();
        if (currentDiagram == null) return;

        DiagramRelation selectedRelation = diagramCanvas.getSelectedRelation();
        if (selectedRelation == null) {
            showWarning("Aucune relation sélectionnée",
                    "Veuillez sélectionner une relation à modifier.");
            return;
        }

        RelationEditorDialog dialog = viewFactory.createRelationEditorDialog(
                selectedRelation, currentDiagram.getClasses());
        dialog.showAndWait().ifPresent(updatedRelation -> diagramCanvas.refresh());
    }

    private void handleDeleteRelation() {
        ClassDiagram currentDiagram = diagramStore.getActiveDiagram();
        if (currentDiagram == null) return;

        DiagramRelation selectedRelation = diagramCanvas.getSelectedRelation();
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
            currentDiagram.removeRelation(selectedRelation);
            diagramCanvas.refresh();
        }
    }

    private void handleSave() {
        File currentFile = diagramStore.getCurrentFile();
        if (currentFile != null) {
            saveToFile(currentFile);
        } else {
            handleSaveAs();
        }
    }

    private void handleSaveAs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le diagramme");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers DiagGen (*.dgn)", "*.dgn"));

        File file = fileChooser.showSaveDialog(mainView.getScene().getWindow());
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

    private void handleOpen() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Ouvrir un diagramme");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers DiagGen (*.dgn)", "*.dgn"));

        File file = fileChooser.showOpenDialog(mainView.getScene().getWindow());
        if (file != null) {
            try {
                DiagramSerializer serializer = new DiagramSerializer();
                ClassDiagram diagram = serializer.deserialize(file);

                diagramStore.getDiagrams().add(diagram);
                diagramStore.setCurrentFile(file);
                updateDiagramList();
                loadDiagram(diagram);

                showInfo("Chargement réussi", "Le diagramme a été chargé avec succès.");
            } catch (Exception e) {
                showError("Erreur lors du chargement",
                        "Une erreur est survenue lors du chargement du diagramme : " + e.getMessage());
            }
        }
    }

    private void handleImportJavaCode() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Importer du code Java");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers Java (*.java)", "*.java"));

        File file = fileChooser.showOpenDialog(mainView.getScene().getWindow());
        if (file != null) {
            try {
                JavaCodeParser parser = new JavaCodeParser();
                ClassDiagram diagram = parser.parseFile(file);

                diagramStore.getDiagrams().add(diagram);
                updateDiagramList();
                loadDiagram(diagram);

                showInfo("Importation réussie", "Le code Java a été importé avec succès.");
            } catch (Exception e) {
                showError("Erreur lors de l'importation",
                        "Une erreur est survenue lors de l'importation du code Java : " + e.getMessage());
            }
        }
    }

    private void handleImportJavaProject() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Importer un projet Java");

        File directory = directoryChooser.showDialog(mainView.getScene().getWindow());
        if (directory != null) {
            try {
                JavaCodeParser parser = new JavaCodeParser();
                ClassDiagram diagram = parser.parseProject(directory.toPath());

                diagramStore.getDiagrams().add(diagram);
                updateDiagramList();
                loadDiagram(diagram);

                showInfo("Importation réussie", "Le projet Java a été importé avec succès.");
            } catch (Exception e) {
                showError("Erreur lors de l'importation",
                        "Une erreur est survenue lors de l'importation du projet Java : " + e.getMessage());
            }
        }
    }

    private void handleExportImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter en image PNG");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers PNG (*.png)", "*.png"));

        File file = fileChooser.showSaveDialog(mainView.getScene().getWindow());
        if (file != null) {
            if (!file.getName().endsWith(".png")) {
                file = new File(file.getAbsolutePath() + ".png");
            }
            try {
                DiagramExporter exporter = new PNGExporter(diagramCanvas);
                exporter.export(diagramStore.getActiveDiagram(), file);
                showInfo("Exportation réussie", "Le diagramme a été exporté en PNG avec succès.");
            } catch (IOException e) {
                showError("Erreur lors de l'exportation",
                        "Une erreur est survenue lors de l'exportation en PNG : " + e.getMessage());
            }
        }
    }

    private void handleExportSVG() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter en SVG");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers SVG (*.svg)", "*.svg"));

        File file = fileChooser.showSaveDialog(mainView.getScene().getWindow());
        if (file != null) {
            if (!file.getName().endsWith(".svg")) {
                file = new File(file.getAbsolutePath() + ".svg");
            }
            try {
                DiagramExporter exporter = new SVGExporter();
                exporter.export(diagramStore.getActiveDiagram(), file);
                showInfo("Exportation réussie", "Le diagramme a été exporté en SVG avec succès.");
            } catch (IOException e) {
                showError("Erreur lors de l'exportation",
                        "Une erreur est survenue lors de l'exportation en SVG : " + e.getMessage());
            }
        }
    }

    private void handleExportPlantUML() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter en PlantUML");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers PlantUML (*.puml)", "*.puml"));

        File file = fileChooser.showSaveDialog(mainView.getScene().getWindow());
        if (file != null) {
            if (!file.getName().endsWith(".puml")) {
                file = new File(file.getAbsolutePath() + ".puml");
            }
            try {
                DiagramExporter exporter = new PlantUMLExporter();
                exporter.export(diagramStore.getActiveDiagram(), file);
                showInfo("Exportation réussie", "Le diagramme a été exporté en PlantUML avec succès.");
            } catch (IOException e) {
                showError("Erreur lors de l'exportation",
                        "Une erreur est survenue lors de l'exportation en PlantUML : " + e.getMessage());
            }
        }
    }

    private void handleExportJavaCode() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Exporter en code Java");

        File directory = directoryChooser.showDialog(mainView.getScene().getWindow());
        if (directory != null) {
            try {
                DiagramExporter exporter = new JavaCodeExporter();
                exporter.export(diagramStore.getActiveDiagram(), directory);
                showInfo("Exportation réussie", "Le diagramme a été exporté en code Java avec succès.");
            } catch (IOException e) {
                showError("Erreur lors de l'exportation",
                        "Une erreur est survenue lors de l'exportation en code Java : " + e.getMessage());
            }
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}


