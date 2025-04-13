package com.diaggen.controller;

import com.diaggen.controller.command.CommandManager;
import com.diaggen.event.DiagramActivatedEvent;
import com.diaggen.event.DiagramChangedEvent;
import com.diaggen.event.EventBus;
import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramStore;
import com.diaggen.model.Project;
import com.diaggen.model.persist.DiagramSerializer;
import com.diaggen.model.session.ProjectSessionManager;
import com.diaggen.util.AlertHelper;
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
    private final EventBus eventBus = EventBus.getInstance();
    private final ProjectSessionManager sessionManager;

    public DiagramController(DiagramStore diagramStore, CommandManager commandManager) {
        super(diagramStore, commandManager);
        this.sessionManager = ProjectSessionManager.getInstance();
    }

    public void setOwnerWindow(Window ownerWindow) {
        this.ownerWindow = ownerWindow;
    }

    public ClassDiagram createNewDiagramWithDialog() {
        if (diagramStore.getActiveProject() == null) {
            AlertHelper.showWarning("Aucun projet actif", "Vous devez avoir un projet actif pour créer un diagramme.");
            return null;
        }

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
        if (diagramStore.getActiveProject() == null) {
            AlertHelper.showWarning("Aucun projet actif", "Vous devez avoir un projet actif pour créer un diagramme.");
            return null;
        }

        ClassDiagram diagram = diagramStore.createNewDiagram(name);
        activateDiagram(diagram);

        // Marquer le projet comme modifié
        sessionManager.markProjectAsModified();

        return diagram;
    }

    public void activateDiagram(ClassDiagram diagram) {
        activateDiagram(diagram, false);
    }

    public void activateDiagram(ClassDiagram diagram, boolean forceActivation) {
        if (diagram == null) {
            diagramStore.setActiveDiagram(null);
            eventBus.publish(new DiagramActivatedEvent(null));
            return;
        }

        if (!forceActivation && diagramStore.getActiveDiagram() == diagram) {
            return;
        }

        diagramStore.setActiveDiagram(diagram);
        eventBus.publish(new DiagramActivatedEvent(diagram.getId()));
    }

    public void renameDiagram(ClassDiagram diagram, String newName) {
        if (diagram == null) return;

        if (newName == null) {
            TextInputDialog dialog = new TextInputDialog(diagram.getName());
            dialog.setTitle("Renommer le diagramme");
            dialog.setHeaderText("Renommer \"" + diagram.getName() + "\"");
            dialog.setContentText("Nouveau nom :");

            if (ownerWindow != null) {
                dialog.initOwner(ownerWindow);
            }

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && !result.get().trim().isEmpty()) {
                newName = result.get().trim();
            } else {
                return;
            }
        }

        String oldName = diagram.getName();
        diagram.setName(newName);

        // Marquer le projet comme modifié
        sessionManager.markProjectAsModified();

        eventBus.publish(new DiagramChangedEvent(diagram.getId(),
                DiagramChangedEvent.ChangeType.DIAGRAM_RENAMED, oldName));
    }

    public void deleteDiagram(ClassDiagram diagram) {
        if (diagram == null) return;

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

            if (diagram.equals(diagramStore.getActiveDiagram())) {
                activateDiagram(null);
            }

            diagramStore.removeDiagram(diagram);

            // Marquer le projet comme modifié
            sessionManager.markProjectAsModified();

            // Utiliser le nouveau type DIAGRAM_DELETED au lieu de DIAGRAM_DELETED
            eventBus.publish(new DiagramChangedEvent(diagramId,
                    DiagramChangedEvent.ChangeType.DIAGRAM_DELETED, null));
        }
    }

    public void duplicateDiagram(ClassDiagram diagram) {
        if (diagram == null) return;

        ClassDiagram copy = diagram.createCopy();
        diagramStore.getActiveProject().addDiagram(copy);

        // Marquer le projet comme modifié
        sessionManager.markProjectAsModified();

        eventBus.publish(new DiagramChangedEvent(copy.getId(),
                DiagramChangedEvent.ChangeType.DIAGRAM_CREATED, null));

        activateDiagram(copy);
    }

    public void saveDiagram() {
        ClassDiagram diagram = diagramStore.getActiveDiagram();
        if (diagram == null) {
            AlertHelper.showWarning("Aucun diagramme actif", "Il n'y a pas de diagramme à enregistrer.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le diagramme");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers DiagGen (*.dg)", "*.dg"));

        File file = fileChooser.showSaveDialog(ownerWindow);
        if (file != null) {
            if (!file.getName().endsWith(".dg")) {
                file = new File(file.getAbsolutePath() + ".dg");
            }
            saveToFile(diagram, file);
        }
    }

    public void saveAsDiagram() {
        saveDiagram();
    }

    public void openDiagram() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Ouvrir un diagramme");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers DiagGen (*.dg)", "*.dg"));

        File file = fileChooser.showOpenDialog(ownerWindow);
        if (file != null) {
            try {
                DiagramSerializer serializer = new DiagramSerializer();
                ClassDiagram loadedDiagram = serializer.deserialize(file);

                Project activeProject = diagramStore.getActiveProject();
                if (activeProject == null) {
                    AlertHelper.showWarning("Aucun projet actif",
                            "Vous devez avoir un projet actif pour ouvrir un diagramme.");
                    return;
                }

                activeProject.addDiagram(loadedDiagram);

                // Marquer le projet comme modifié
                sessionManager.markProjectAsModified();

                activateDiagram(loadedDiagram);

                LOGGER.log(Level.INFO, "Successfully loaded diagram from {0}", file.getName());
                AlertHelper.showInfo("Chargement réussi", "Le diagramme a été chargé avec succès.");
            } catch (IOException | ClassNotFoundException e) {
                LOGGER.log(Level.SEVERE, "Error loading diagram", e);
                AlertHelper.showError("Erreur lors de l'ouverture",
                        "Une erreur est survenue lors de l'ouverture du diagramme : " + e.getMessage());
            }
        }
    }

    private void saveToFile(ClassDiagram diagram, File file) {
        try {
            DiagramSerializer serializer = new DiagramSerializer();
            serializer.serialize(diagram, file);
            LOGGER.log(Level.INFO, "Successfully saved diagram to {0}", file.getName());
            AlertHelper.showInfo("Sauvegarde réussie", "Le diagramme a été enregistré avec succès.");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving diagram", e);
            AlertHelper.showError("Erreur lors de l'enregistrement",
                    "Une erreur est survenue lors de l'enregistrement du diagramme : " + e.getMessage());
        }
    }
}