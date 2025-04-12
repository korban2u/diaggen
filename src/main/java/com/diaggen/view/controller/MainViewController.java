package com.diaggen.view.controller;

import com.diaggen.controller.MainController;
import com.diaggen.event.DiagramActivatedEvent;
import com.diaggen.event.DiagramChangedEvent;
import com.diaggen.event.EventBus;
import com.diaggen.event.EventListener;
import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramClass;
import com.diaggen.model.DiagramRelation;
import com.diaggen.view.diagram.DiagramCanvas;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainViewController {
    private static final Logger LOGGER = Logger.getLogger(MainViewController.class.getName());

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
    private final EventBus eventBus = EventBus.getInstance();

    private DiagramClass selectedClass;
    private DiagramRelation selectedRelation;
    private boolean isProcessingSelection = false;
    private boolean isProcessingEvent = false;

    @FXML
    public void initialize() {
        LOGGER.log(Level.INFO, "Initializing MainViewController");

        // Setup diagram list view
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
            if (newValue != null && !isProcessingSelection) {
                isProcessingSelection = true;
                try {
                    handleSelectDiagram(newValue);
                } finally {
                    isProcessingSelection = false;
                }
            }
        });

        // Create and setup the enhanced diagram canvas
        diagramCanvas = new DiagramCanvas();
        diagramCanvasContainer.getChildren().add(diagramCanvas);

        // Setup editor panel
        editorController = new EditorPanelController(editorContent);
        StackPane editorPaneContainer = (StackPane) editorPanel.getParent().getParent();
        editorPanel.visibleProperty().addListener((obs, wasVisible, isVisible) -> {
            editorPaneContainer.setMouseTransparent(!isVisible);
        });

        // Add close button to editor panel
        Button closeButton = new Button("×");
        closeButton.getStyleClass().add("close-button");
        closeButton.setOnAction(e -> {
            editorPanel.setVisible(false);
            if (diagramCanvas != null) {
                diagramCanvas.deselectAll();
            }
        });

        // Replace the title with a title bar containing the close button
        Label editorTitleLabel = (Label) editorPanel.getChildren().stream()
                .filter(node -> node instanceof Label && ((Label) node).getText().equals("Éditeur"))
                .findFirst()
                .orElse(null);

        if (editorTitleLabel != null) {
            HBox titleBox = new HBox();
            titleBox.setAlignment(Pos.CENTER_LEFT);
            titleBox.setSpacing(10);
            Label newTitleLabel = new Label("Éditeur");
            newTitleLabel.getStyleClass().add("editor-title");

            titleBox.getChildren().addAll(newTitleLabel, new Region(), closeButton);
            HBox.setHgrow(titleBox.getChildren().get(1), Priority.ALWAYS);
            int labelIndex = editorPanel.getChildren().indexOf(editorTitleLabel);
            editorPanel.getChildren().remove(editorTitleLabel);
            editorPanel.getChildren().add(labelIndex, titleBox);
            titleBox.setPadding(new Insets(10, 15, 10, 15));
        }

        // Set initial button states
        deleteClassButton.setDisable(true);
        deleteRelationButton.setDisable(true);

        setupSelectionHandling();
        setupEventBusListeners();
        setupKeyboardShortcuts();

        LOGGER.log(Level.INFO, "MainViewController initialization complete");
    }

    private void setupEventBusListeners() {
        eventBus.subscribe(DiagramActivatedEvent.class, (EventListener<DiagramActivatedEvent>) event -> {
            if (isProcessingEvent) return;

            LOGGER.log(Level.INFO, "DiagramActivatedEvent received for diagram ID: {0}", event.getDiagramId());
            isProcessingEvent = true;
            try {
                Platform.runLater(() -> {
                    ObservableList<ClassDiagram> diagrams = diagramListView.getItems();
                    ClassDiagram targetDiagram = null;

                    for (ClassDiagram diagram : diagrams) {
                        if (diagram.getId().equals(event.getDiagramId())) {
                            targetDiagram = diagram;
                            break;
                        }
                    }

                    if (targetDiagram != null) {
                        if (editorController != null) {
                            editorController.clearEditor();
                            editorPanel.setVisible(false);
                        }
                        selectedClass = null;
                        selectedRelation = null;
                        deleteClassButton.setDisable(true);
                        deleteRelationButton.setDisable(true);
                        ClassDiagram selectedDiagram = diagramListView.getSelectionModel().getSelectedItem();
                        if (selectedDiagram != targetDiagram) {
                            isProcessingSelection = true;
                            try {
                                diagramListView.getSelectionModel().select(targetDiagram);
                            } finally {
                                isProcessingSelection = false;
                            }
                        }
                        diagramCanvas.loadDiagram(targetDiagram);

                        setStatus("Diagramme actif: " + targetDiagram.getName());
                        LOGGER.log(Level.INFO, "Diagram activated in UI: {0}", targetDiagram.getName());
                    } else {
                        LOGGER.log(Level.WARNING, "Could not find diagram with ID: {0}", event.getDiagramId());
                    }
                });
            } finally {
                isProcessingEvent = false;
            }
        });

        eventBus.subscribe(DiagramChangedEvent.class, (EventListener<DiagramChangedEvent>) event -> {
            LOGGER.log(Level.FINE, "DiagramChangedEvent received for diagram ID: {0}", event.getDiagramId());
            Platform.runLater(() -> {
                diagramListView.refresh();
                if (diagramCanvas.getDiagram() != null &&
                        diagramCanvas.getDiagram().getId().equals(event.getDiagramId())) {
                    diagramCanvas.refresh();
                }
            });
        });
    }

    private void setupSelectionHandling() {
        diagramCanvas.setClassSelectionListener(diagramClass -> {
            if (diagramClass != null) {
                selectedClass = diagramClass;
                selectedRelation = null;

                deleteClassButton.setDisable(false);
                deleteRelationButton.setDisable(true);
                editorPanel.setVisible(true);
                editorController.showClassEditor(diagramClass);

                setStatus("Classe sélectionnée: " + diagramClass.getName());
            } else {
                selectedClass = null;
                deleteClassButton.setDisable(true);
                if (selectedRelation == null) {
                    editorPanel.setVisible(false);
                }

                setStatus("Prêt");
            }
        });

        diagramCanvas.setRelationSelectionListener(relation -> {
            if (relation != null) {
                selectedRelation = relation;
                selectedClass = null;

                deleteRelationButton.setDisable(false);
                deleteClassButton.setDisable(true);
                editorPanel.setVisible(true);
                editorController.showRelationEditor(relation);

                setStatus("Relation sélectionnée: " + relation.getRelationType().getDisplayName() +
                        " entre " + relation.getSourceClass().getName() +
                        " et " + relation.getTargetClass().getName());
            } else {
                selectedRelation = null;
                deleteRelationButton.setDisable(true);
                if (selectedClass == null) {
                    editorPanel.setVisible(false);
                }

                setStatus("Prêt");
            }
        });

        diagramCanvas.setOnDeleteRequest(() -> {
            if (selectedClass != null) {
                handleDeleteClass();
            } else if (selectedRelation != null) {
                handleDeleteRelation();
            }
        });

        diagramCanvas.setOnAddClassRequest(() -> {
            if (mainController != null) {
                mainController.handleAddClass();
            }
        });
    }

    private void setupKeyboardShortcuts() {
        Scene scene = diagramCanvasContainer.getScene();
        if (scene != null) {
            // Selection and deletion shortcuts
            KeyCombination deleteKey = new KeyCodeCombination(KeyCode.DELETE);
            scene.getAccelerators().put(deleteKey, () -> {
                if (diagramCanvas.hasSelection()) {
                    if (selectedClass != null) {
                        handleDeleteClass();
                    } else if (selectedRelation != null) {
                        handleDeleteRelation();
                    }
                }
            });

            // Undo/redo shortcuts
            KeyCombination undoKey = new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN);
            scene.getAccelerators().put(undoKey, this::handleUndo);

            KeyCombination redoKey = new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN);
            scene.getAccelerators().put(redoKey, this::handleRedo);

            // Zoom shortcuts
            KeyCombination zoomInKey = new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.CONTROL_DOWN);
            scene.getAccelerators().put(zoomInKey, () -> diagramCanvas.getNavigationManager().zoomIn());

            KeyCombination zoomOutKey = new KeyCodeCombination(KeyCode.MINUS, KeyCombination.CONTROL_DOWN);
            scene.getAccelerators().put(zoomOutKey, () -> diagramCanvas.getNavigationManager().zoomOut());

            KeyCombination resetZoomKey = new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.CONTROL_DOWN);
            scene.getAccelerators().put(resetZoomKey, () -> diagramCanvas.getNavigationManager().resetView());

            KeyCombination fitToViewKey = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN);
            scene.getAccelerators().put(fitToViewKey, () -> diagramCanvas.zoomToFit());

            // Add class shortcut
            KeyCombination addClassKey = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
            scene.getAccelerators().put(addClassKey, () -> {
                if (mainController != null) {
                    mainController.handleAddClass();
                }
            });

            // Add relation shortcut
            KeyCombination addRelationKey = new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN);
            scene.getAccelerators().put(addRelationKey, () -> {
                if (mainController != null) {
                    mainController.handleAddRelation();
                }
            });
        }
    }

    public void setMainController(MainController mainController) {
        LOGGER.log(Level.INFO, "Setting MainController");
        this.mainController = mainController;
        if (editorController != null) {
            editorController.setMainController(mainController);
        }
    }

    public void updateDiagramList(ObservableList<ClassDiagram> diagrams) {
        LOGGER.log(Level.INFO, "Updating diagram list with {0} diagrams", diagrams.size());
        diagramListView.setItems(diagrams);
    }

    public void updateSelectedDiagram(ClassDiagram diagram) {
        if (diagram != null) {
            LOGGER.log(Level.INFO, "Updating selected diagram to: {0}", diagram.getName());
            isProcessingSelection = true;
            try {
                diagramListView.getSelectionModel().select(diagram);
            } finally {
                isProcessingSelection = false;
            }
        }
    }

    public DiagramCanvas getDiagramCanvas() {
        return diagramCanvas;
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    private void handleSelectDiagram(ClassDiagram diagram) {
        if (mainController != null && diagram != null) {
            LOGGER.log(Level.INFO, "handleSelectDiagram called for diagram: {0}", diagram.getName());
            mainController.handleSelectDiagram(diagram);
        }
    }

    public DiagramClass getSelectedClass() {
        return selectedClass;
    }

    public DiagramRelation getSelectedRelation() {
        return selectedRelation;
    }

    @FXML
    private void handleNewDiagram() {
        if (mainController != null) {
            LOGGER.log(Level.INFO, "Creating new diagram");
            mainController.handleNewDiagram();
        }
    }

    @FXML
    private void handleOpenDiagram() {
        if (mainController != null) {
            LOGGER.log(Level.INFO, "Opening diagram");
            mainController.handleOpen();
        }
    }

    @FXML
    private void handleSaveDiagram() {
        if (mainController != null) {
            LOGGER.log(Level.INFO, "Saving diagram");
            mainController.handleSave();
        }
    }

    @FXML
    private void handleSaveAsDiagram() {
        if (mainController != null) {
            LOGGER.log(Level.INFO, "Saving diagram as...");
            mainController.handleSaveAs();
        }
    }

    @FXML
    private void handleEditDiagram() {
        if (mainController != null) {
            LOGGER.log(Level.INFO, "Editing diagram properties");
            mainController.handleEditDiagram();
        }
    }

    @FXML
    private void handleDeleteDiagram() {
        if (mainController != null) {
            LOGGER.log(Level.INFO, "Deleting diagram");
            mainController.handleDeleteDiagram();
        }
    }

    @FXML
    private void handleAddClass() {
        if (mainController != null) {
            LOGGER.log(Level.INFO, "Adding class");
            mainController.handleAddClass();
        }
    }

    @FXML
    private void handleDeleteClass() {
        if (mainController != null && selectedClass != null) {
            LOGGER.log(Level.INFO, "Deleting class: {0}", selectedClass.getName());
            mainController.handleDeleteClass();
        }
    }

    @FXML
    private void handleAddRelation() {
        if (mainController != null) {
            LOGGER.log(Level.INFO, "Adding relation");
            mainController.handleAddRelation();
        }
    }

    @FXML
    private void handleDeleteRelation() {
        if (mainController != null && selectedRelation != null) {
            LOGGER.log(Level.INFO, "Deleting relation");
            mainController.handleDeleteRelation();
        }
    }

    @FXML
    private void handleImportJavaCode() {
        if (mainController != null) {
            LOGGER.log(Level.INFO, "Importing Java code");
            mainController.handleImportJavaCode();
        }
    }

    @FXML
    private void handleExportImage() {
        if (mainController != null) {
            LOGGER.log(Level.INFO, "Exporting image");
            mainController.handleExportImage();
        }
    }

    @FXML
    private void handleExportSVG() {
        if (mainController != null) {
            LOGGER.log(Level.INFO, "Exporting SVG");
            mainController.handleExportSVG();
        }
    }

    @FXML
    private void handleExportPlantUML() {
        if (mainController != null) {
            LOGGER.log(Level.INFO, "Exporting PlantUML");
            mainController.handleExportPlantUML();
        }
    }

    @FXML
    private void handleExportJavaCode() {
        if (mainController != null) {
            LOGGER.log(Level.INFO, "Exporting Java code");
            mainController.handleExportJavaCode();
        }
    }

    @FXML
    private void handleUndo() {
        if (mainController != null) {
            LOGGER.log(Level.INFO, "Undoing action");
            mainController.handleUndo();
        }
    }

    @FXML
    private void handleRedo() {
        if (mainController != null) {
            LOGGER.log(Level.INFO, "Redoing action");
            mainController.handleRedo();
        }
    }

    @FXML
    private void handleZoomToFit() {
        diagramCanvas.zoomToFit();
    }

    public Window getWindow() {
        return diagramCanvasContainer.getScene().getWindow();
    }

    @FXML
    private void handleAbout() {
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