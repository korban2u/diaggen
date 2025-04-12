package com.diaggen.controller;

import com.diaggen.controller.command.CommandManager;
import com.diaggen.event.DiagramActivatedEvent;
import com.diaggen.event.DiagramChangedEvent;
import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramStore;
import com.diaggen.model.java.JavaCodeParser;
import com.diaggen.service.ExportService;
import com.diaggen.util.AlertHelper;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExportController extends BaseController {

    private static final Logger LOGGER = Logger.getLogger(ExportController.class.getName());
    private final ExportService exportService;
    private final ClassController classController;
    private final DiagramController diagramController;  // Référence ajoutée au DiagramController
    private Window ownerWindow;

    public ExportController(DiagramStore diagramStore, CommandManager commandManager,
                            ExportService exportService, ClassController classController) {
        super(diagramStore, commandManager);
        this.exportService = exportService;
        this.classController = classController;
        this.diagramController = null; // Sera initialisé plus tard
    }

    public ExportController(DiagramStore diagramStore, CommandManager commandManager,
                            ExportService exportService, ClassController classController,
                            DiagramController diagramController) {
        super(diagramStore, commandManager);
        this.exportService = exportService;
        this.classController = classController;
        this.diagramController = diagramController;
    }

    public void setDiagramController(DiagramController diagramController) {
        LOGGER.log(Level.INFO, "Setting DiagramController in ExportController");
        if (this.diagramController == null) {
            LOGGER.log(Level.INFO, "DiagramController initialized");
        }
    }

    public void setOwnerWindow(Window ownerWindow) {
        this.ownerWindow = ownerWindow;
    }

    public void exportToPNG() {
        ClassDiagram currentDiagram = getActiveDiagram();
        if (currentDiagram == null) {
            AlertHelper.showWarning("Aucun diagramme actif", "Il n'y a pas de diagramme à exporter.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter en PNG");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images PNG (*.png)", "*.png"));

        File file = fileChooser.showSaveDialog(ownerWindow);
        if (file != null) {
            if (!file.getName().endsWith(".png")) {
                file = new File(file.getAbsolutePath() + ".png");
            }

            try {
                exportService.exportDiagram(currentDiagram, "png", file);
            } catch (IOException e) {
                AlertHelper.showError("Erreur d'exportation",
                        "Erreur lors de l'exportation en PNG: " + e.getMessage());
            }
        }
    }

    public void exportToSVG() {
        ClassDiagram currentDiagram = getActiveDiagram();
        if (currentDiagram == null) {
            AlertHelper.showWarning("Aucun diagramme actif", "Il n'y a pas de diagramme à exporter.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter en SVG");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images SVG (*.svg)", "*.svg"));

        File file = fileChooser.showSaveDialog(ownerWindow);
        if (file != null) {
            if (!file.getName().endsWith(".svg")) {
                file = new File(file.getAbsolutePath() + ".svg");
            }

            try {
                exportService.exportDiagram(currentDiagram, "svg", file);
            } catch (IOException e) {
                AlertHelper.showError("Erreur d'exportation",
                        "Erreur lors de l'exportation en SVG: " + e.getMessage());
            }
        }
    }

    public void exportToPlantUML() {
        ClassDiagram currentDiagram = getActiveDiagram();
        if (currentDiagram == null) {
            AlertHelper.showWarning("Aucun diagramme actif", "Il n'y a pas de diagramme à exporter.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter en PlantUML");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers PlantUML (*.puml)", "*.puml"));

        File file = fileChooser.showSaveDialog(ownerWindow);
        if (file != null) {
            if (!file.getName().endsWith(".puml")) {
                file = new File(file.getAbsolutePath() + ".puml");
            }

            try {
                exportService.exportDiagram(currentDiagram, "puml", file);
            } catch (IOException e) {
                AlertHelper.showError("Erreur d'exportation",
                        "Erreur lors de l'exportation en PlantUML: " + e.getMessage());
            }
        }
    }

    public void exportToJavaCode() {
        ClassDiagram currentDiagram = getActiveDiagram();
        if (currentDiagram == null) {
            AlertHelper.showWarning("Aucun diagramme actif", "Il n'y a pas de diagramme à exporter.");
            return;
        }

        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Exporter en code Java - Sélectionner le répertoire de destination");

        File dir = dirChooser.showDialog(ownerWindow);
        if (dir != null) {
            if (!dir.isDirectory()) {
                AlertHelper.showError("Erreur de sélection", "Veuillez sélectionner un répertoire valide.");
                return;
            }

            try {
                exportService.exportDiagram(currentDiagram, "java", dir);

                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Exportation réussie");
                alert.setHeaderText("Le code Java a été généré avec succès");
                alert.setContentText("Voulez-vous ouvrir le répertoire contenant les fichiers?");

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    try {
                        java.awt.Desktop.getDesktop().open(dir);
                    } catch (IOException e) {

                    }
                }
            } catch (IOException e) {
                AlertHelper.showError("Erreur d'exportation",
                        "Erreur lors de la génération du code Java: " + e.getMessage());
            }
        }
    }

    public void importJavaCode() {
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
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Importer un fichier Java");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Fichiers Java (*.java)", "*.java"));

            File file = fileChooser.showOpenDialog(ownerWindow);
            if (file != null) {
                try {
                    parsedDiagram = parser.parseFile(file);

                    parsedDiagram.setName("Diagramme - " + file.getName());
                } catch (Exception e) {
                    AlertHelper.showError("Erreur d'importation",
                            "Erreur lors de l'analyse du fichier Java: " + e.getMessage());
                    return;
                }
            } else {
                return;
            }
        } else if (choice.get() == dirButton) {
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle("Importer un projet Java");

            File dir = dirChooser.showDialog(ownerWindow);
            if (dir != null) {
                try {
                    parsedDiagram = parser.parseProject(dir.toPath());

                    parsedDiagram.setName("Projet - " + dir.getName());
                } catch (Exception e) {
                    AlertHelper.showError("Erreur d'importation",
                            "Erreur lors de l'analyse du projet Java: " + e.getMessage());
                    return;
                }
            } else {
                return;
            }
        } else {
            return;
        }

        if (parsedDiagram != null) {

            diagramStore.getDiagrams().add(parsedDiagram);
            diagramStore.setCurrentFile(null);

            if (diagramController != null) {
                LOGGER.log(Level.INFO, "Activating imported diagram using DiagramController");
                diagramController.activateDiagram(parsedDiagram, true);
            } else {

                LOGGER.log(Level.INFO, "Activating imported diagram manually (DiagramController not available)");
                diagramStore.setActiveDiagram(parsedDiagram);
                eventBus.publish(new DiagramChangedEvent(parsedDiagram.getId(),
                        DiagramChangedEvent.ChangeType.DIAGRAM_RENAMED, null));
                eventBus.publish(new DiagramActivatedEvent(parsedDiagram.getId()));
            }

            classController.arrangeClassesAutomatically();
        }
    }
}