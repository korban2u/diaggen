package com.diaggen.controller;

import com.diaggen.controller.command.CommandManager;
import com.diaggen.event.DiagramActivatedEvent;
import com.diaggen.event.DiagramChangedEvent;
import com.diaggen.event.EventBus;
import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramStore;
import com.diaggen.model.persist.DiagramSerializer;
import com.diaggen.util.AlertHelper;
import com.diaggen.view.dialog.DialogFactory;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DiagramController extends BaseController {

    private static final Logger LOGGER = Logger.getLogger(DiagramController.class.getName());
    private Window ownerWindow;
    private DialogFactory dialogFactory;

    private boolean isActivating = false;

    public DiagramController(DiagramStore diagramStore, CommandManager commandManager) {
        super(diagramStore, commandManager);
        this.dialogFactory = DialogFactory.getInstance();

        diagramStore.getDiagrams().addListener((ListChangeListener<ClassDiagram>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (ClassDiagram diagram : change.getAddedSubList()) {
                        eventBus.publish(new DiagramChangedEvent(diagram.getId(),
                                DiagramChangedEvent.ChangeType.DIAGRAM_RENAMED, null));
                    }
                }
                if (change.wasRemoved()) {
                    for (ClassDiagram diagram : change.getRemoved()) {
                        eventBus.publish(new DiagramChangedEvent(diagram.getId(),
                                DiagramChangedEvent.ChangeType.DIAGRAM_CLEARED, null));
                    }
                }
            }
        });
    }

    public void setOwnerWindow(Window ownerWindow) {
        this.ownerWindow = ownerWindow;
    }

    public ClassDiagram createNewDiagramWithDialog() {

        TextInputDialog dialog = new TextInputDialog("Nouveau diagramme");
        dialog.setTitle("Nouveau diagramme");
        dialog.setHeaderText("Créer un nouveau diagramme");
        dialog.setContentText("Nom du diagramme:");

        if (ownerWindow != null) {
            dialog.initOwner(ownerWindow);
        }

        Optional<String> result = dialog.showAndWait();

        if (result.isPresent()) {
            String diagramName = result.get().trim();

            if (diagramName.isEmpty()) {
                diagramName = "Nouveau diagramme";
            }

            return createNewDiagram(diagramName);
        }

        return null;
    }

    public ClassDiagram createNewDiagram(String name) {
        LOGGER.log(Level.INFO, "Creating new diagram: {0}", name);

        ClassDiagram diagram = diagramStore.createNewDiagram(name);

        eventBus.publish(new DiagramChangedEvent(diagram.getId(),
                DiagramChangedEvent.ChangeType.DIAGRAM_RENAMED, null));

        activateDiagram(diagram, true);

        return diagram;
    }


    public void activateDiagram(ClassDiagram diagram, boolean forceActivation) {
        if (diagram == null) {
            LOGGER.log(Level.WARNING, "Attempted to activate null diagram");
            return;
        }

        if (!forceActivation && diagramStore.getActiveDiagram() == diagram) {
            LOGGER.log(Level.INFO, "Diagram is already active, skipping activation: {0}", diagram.getName());
            return;
        }

        if (isActivating) {
            LOGGER.log(Level.INFO, "Already activating a diagram, skipping: {0}", diagram.getName());
            return;
        }

        isActivating = true;
        try {
            LOGGER.log(Level.INFO, "Activating diagram: {0} (ID: {1})", new Object[]{diagram.getName(), diagram.getId()});

            diagramStore.setActiveDiagram(diagram);

            eventBus.publish(new DiagramActivatedEvent(diagram.getId()));

            eventBus.publish(new DiagramChangedEvent(diagram.getId(),
                    DiagramChangedEvent.ChangeType.DIAGRAM_RENAMED, null));
        } finally {

            isActivating = false;
        }
    }


    public void activateDiagram(ClassDiagram diagram) {
        activateDiagram(diagram, false);
    }

    public void renameDiagram(ClassDiagram diagram, String newName) {
        if (diagram == null) return;

        if (newName == null) {
            TextInputDialog dialog = new TextInputDialog(diagram.getName());
            dialog.setTitle("Renommer le diagramme");
            dialog.setHeaderText("Renommer \"" + diagram.getName() + "\"");
            dialog.setContentText("Nouveau nom:");

            if (ownerWindow != null) {
                dialog.initOwner(ownerWindow);
            }

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && !result.get().trim().isEmpty()) {
                newName = result.get().trim();
            } else {
                return; // Annulation ou nom vide
            }
        }

        LOGGER.log(Level.INFO, "Renaming diagram {0} to {1}", new Object[]{diagram.getName(), newName});
        diagram.setName(newName);
        eventBus.publish(new DiagramChangedEvent(diagram.getId(),
                DiagramChangedEvent.ChangeType.DIAGRAM_RENAMED, null));
    }

    public void deleteDiagram(ClassDiagram diagram) {
        if (diagram == null) return;

        LOGGER.log(Level.INFO, "Deleting diagram: {0}", diagram.getName());

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer le diagramme");
        alert.setHeaderText("Supprimer \"" + diagram.getName() + "\"");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer ce diagramme ? Cette action est irréversible.");

        if (ownerWindow != null) {
            alert.initOwner(ownerWindow);
        }

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String diagramId = diagram.getId();
            diagramStore.removeDiagram(diagram);
            eventBus.publish(new DiagramChangedEvent(diagramId,
                    DiagramChangedEvent.ChangeType.DIAGRAM_CLEARED, null));
        }
    }

    public void saveDiagram() {
        File currentFile = diagramStore.getCurrentFile();
        if (currentFile != null) {
            saveToFile(currentFile);
        } else {
            saveAsDiagram();
        }
    }

    public void saveAsDiagram() {
        if (diagramStore.getActiveDiagram() == null) {
            AlertHelper.showWarning("Aucun diagramme actif", "Il n'y a pas de diagramme à enregistrer.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le diagramme");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers DiagGen (*.dgn)", "*.dgn"));

        File file = fileChooser.showSaveDialog(ownerWindow);
        if (file != null) {
            if (!file.getName().endsWith(".dgn")) {
                file = new File(file.getAbsolutePath() + ".dgn");
            }
            saveToFile(file);
            diagramStore.setCurrentFile(file);
        }
    }

    public void openDiagram() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Ouvrir un diagramme");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers DiagGen (*.dgn)", "*.dgn"));

        File file = fileChooser.showOpenDialog(ownerWindow);
        if (file != null) {
            try {
                DiagramSerializer serializer = new DiagramSerializer();
                ClassDiagram loadedDiagram = serializer.deserialize(file);

                diagramStore.getDiagrams().add(loadedDiagram);
                diagramStore.setCurrentFile(file);

                activateDiagram(loadedDiagram, true);

                LOGGER.log(Level.INFO, "Successfully loaded diagram from {0}", file.getName());
            } catch (IOException | ClassNotFoundException e) {
                LOGGER.log(Level.SEVERE, "Error loading diagram", e);
                AlertHelper.showError("Erreur lors de l'ouverture",
                        "Une erreur est survenue lors de l'ouverture du diagramme : " + e.getMessage());
            }
        }
    }

    private void saveToFile(File file) {
        try {
            DiagramSerializer serializer = new DiagramSerializer();
            serializer.serialize(diagramStore.getActiveDiagram(), file);
            LOGGER.log(Level.INFO, "Successfully saved diagram to {0}", file.getName());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving diagram", e);
            AlertHelper.showError("Erreur lors de l'enregistrement",
                    "Une erreur est survenue lors de l'enregistrement du diagramme : " + e.getMessage());
        }
    }
}