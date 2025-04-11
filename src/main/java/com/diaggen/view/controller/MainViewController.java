package com.diaggen.view.controller;

import com.diaggen.controller.MainController;
import com.diaggen.model.ClassDiagram;
import com.diaggen.model.ClassType;
import com.diaggen.model.DiagramClass;
import com.diaggen.model.DiagramRelation;
import com.diaggen.view.dialog.DialogFactory;
import com.diaggen.view.diagram.DiagramCanvas;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

public class MainViewController {

    @FXML
    private ListView<ClassDiagram> diagramListView;

    @FXML
    private StackPane diagramCanvasContainer;

    @FXML
    private Label statusLabel;

    @FXML
    private VBox editorPanel;

    @FXML
    private VBox editorContent;

    @FXML
    private Button deleteClassButton;

    @FXML
    private Button deleteRelationButton;

    private MainController mainController;
    private DiagramCanvas diagramCanvas;
    private EditorPanelController editorController;
    private DialogFactory dialogFactory;

    private DiagramClass selectedClass;
    private DiagramRelation selectedRelation;

    @FXML
    public void initialize() {
        // Configurer la liste des diagrammes
        diagramListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(ClassDiagram item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });

        diagramListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        diagramListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                handleSelectDiagram(newValue);
            }
        });

        // Initialiser le canvas
        diagramCanvas = new DiagramCanvas();
        diagramCanvasContainer.getChildren().add(diagramCanvas);

        // Initialiser le factory de dialogues
        dialogFactory = DialogFactory.getInstance();

        // Initialiser le contrôleur du panneau d'édition
        editorController = new EditorPanelController(editorContent, dialogFactory);

        // Configurer les boutons de suppression (désactivés par défaut)
        deleteClassButton.setDisable(true);
        deleteRelationButton.setDisable(true);

        // Configurer le mécanisme de sélection
        setupSelectionHandling();
    }

    private void setupSelectionHandling() {
        // Configurer le gestionnaire d'événements pour la sélection de classe
        diagramCanvas.setClassSelectionListener(diagramClass -> {
            // Désélectionner la relation si une classe est sélectionnée
            if (diagramClass != null) {
                selectedClass = diagramClass;
                selectedRelation = null;
                diagramCanvas.selectRelation(null);

                // Mettre à jour l'état des boutons
                deleteClassButton.setDisable(false);
                deleteRelationButton.setDisable(true);

                // Afficher le panneau d'édition de classe
                editorPanel.setVisible(true);
                editorController.showClassEditor(diagramClass);

                // Mettre à jour le statut
                setStatus("Classe sélectionnée: " + diagramClass.getName());
            } else {
                selectedClass = null;
                deleteClassButton.setDisable(true);

                // Vérifier si une relation est sélectionnée
                if (selectedRelation == null) {
                    editorPanel.setVisible(false);
                }

                setStatus("Prêt");
            }
        });

        // Configurer le gestionnaire d'événements pour la sélection de relation
        diagramCanvas.setRelationSelectionListener(relation -> {
            // Désélectionner la classe si une relation est sélectionnée
            if (relation != null) {
                selectedRelation = relation;
                selectedClass = null;
                diagramCanvas.selectClass(null);

                // Mettre à jour l'état des boutons
                deleteRelationButton.setDisable(false);
                deleteClassButton.setDisable(true);

                // Afficher le panneau d'édition de relation
                editorPanel.setVisible(true);
                editorController.showRelationEditor(relation);

                // Mettre à jour le statut
                setStatus("Relation sélectionnée: " + relation.getRelationType().getDisplayName() +
                        " entre " + relation.getSourceClass().getName() +
                        " et " + relation.getTargetClass().getName());
            } else {
                selectedRelation = null;
                deleteRelationButton.setDisable(true);

                // Vérifier si une classe est sélectionnée
                if (selectedClass == null) {
                    editorPanel.setVisible(false);
                }

                setStatus("Prêt");
            }
        });
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void updateDiagramList(ObservableList<ClassDiagram> diagrams) {
        diagramListView.setItems(diagrams);
    }

    public void updateSelectedDiagram(ClassDiagram diagram) {
        diagramListView.getSelectionModel().select(diagram);
    }

    public DiagramCanvas getDiagramCanvas() {
        return diagramCanvas;
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    private void handleSelectDiagram(ClassDiagram diagram) {
        if (mainController != null) {
            mainController.handleSelectDiagram(diagram);
        }
    }

    @FXML
    private void handleNewDiagram() {
        if (mainController != null) {
            mainController.handleNewDiagram();
        }
    }

    @FXML
    private void handleOpenDiagram() {
        if (mainController != null) {
            mainController.handleOpen();
        }
    }

    @FXML
    private void handleSaveDiagram() {
        if (mainController != null) {
            mainController.handleSave();
        }
    }

    @FXML
    private void handleSaveAsDiagram() {
        if (mainController != null) {
            mainController.handleSaveAs();
        }
    }

    @FXML
    private void handleEditDiagram() {
        if (mainController != null) {
            mainController.handleEditDiagram();
        }
    }

    @FXML
    private void handleDeleteDiagram() {
        if (mainController != null) {
            mainController.handleDeleteDiagram();
        }
    }

    @FXML
    private void handleAddClass() {
        if (mainController != null) {
            // on crée une nouvelle implémentation dans le contrôleur principal

            // Créer une nouvelle classe avec un nom par défaut
            int classCount = 1;
            // Si possible, obtenez le nombre actuel de classes pour le nom par défaut
            ClassDiagram currentDiagram = mainController.getDiagramStore().getActiveDiagram();
            if (currentDiagram != null) {
                classCount = currentDiagram.getClasses().size() + 1;
            }

            String defaultName = "Classe" + classCount;

            // Créer la classe avec un type par défaut et l'ajouter directement
            DiagramClass newClass = new DiagramClass(defaultName, "", ClassType.CLASS);

            // Positionner la classe à un emplacement visible
            newClass.setX(100 + (classCount % 5) * 220);
            newClass.setY(100 + (classCount / 5) * 150);

            // Utiliser le mainController pour ajouter la classe
            mainController.addNewClass(newClass);

            // Sélectionner la nouvelle classe
            diagramCanvas.selectClass(newClass);
        }
    }
    @FXML
    private void handleDeleteClass() {
        if (mainController != null && selectedClass != null) {
            mainController.handleDeleteClass();
        }
    }

    @FXML
    private void handleAddRelation() {
        if (mainController != null) {
            mainController.handleAddRelation();
        }
    }

    @FXML
    private void handleDeleteRelation() {
        if (mainController != null && selectedRelation != null) {
            mainController.handleDeleteRelation();
        }
    }

    @FXML
    private void handleImportJavaCode() {
        if (mainController != null) {
            mainController.handleImportJavaCode();
        }
    }

    @FXML
    private void handleExportImage() {
        if (mainController != null) {
            mainController.handleExportImage();
        }
    }

    @FXML
    private void handleExportSVG() {
        if (mainController != null) {
            mainController.handleExportSVG();
        }
    }

    @FXML
    private void handleExportPlantUML() {
        if (mainController != null) {
            mainController.handleExportPlantUML();
        }
    }

    @FXML
    private void handleExportJavaCode() {
        if (mainController != null) {
            mainController.handleExportJavaCode();
        }
    }

    @FXML
    private void handleUndo() {
        if (mainController != null) {
            mainController.handleUndo();
        }
    }

    @FXML
    private void handleRedo() {
        if (mainController != null) {
            mainController.handleRedo();
        }
    }

    public Window getWindow() {
        return diagramCanvasContainer.getScene().getWindow();
    }

    @FXML
    private void handleAbout() {
        // Afficher la boîte de dialogue À propos
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("À propos de DiagGen");
        alert.setHeaderText("DiagGen - Générateur de diagrammes de classe");
        alert.setContentText(
                "Version: 1.0.0\n\n" +
                        "DiagGen est un outil de modélisation UML pour créer\n" +
                        "et manipuler des diagrammes de classes.\n\n" +
                        "Développé avec JavaFX " + System.getProperty("javafx.version") + "\n" +
                        "Java " + System.getProperty("java.version") + "\n\n" +
                        "© 2025 DiagGen Team"
        );
        alert.showAndWait();
    }

    @FXML
    private void handleExit() {
        Stage stage = (Stage) diagramCanvasContainer.getScene().getWindow();
        stage.close();
    }
}