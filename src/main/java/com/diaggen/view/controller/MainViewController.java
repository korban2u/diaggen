package com.diaggen.view.controller;

import com.diaggen.controller.ExportController;
import com.diaggen.controller.LayoutController;
import com.diaggen.controller.MainController;
import com.diaggen.controller.ProjectController;
import com.diaggen.event.*;
import com.diaggen.layout.LayoutFactory;
import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramClass;
import com.diaggen.model.DiagramRelation;
import com.diaggen.model.Project;
import com.diaggen.view.diagram.DiagramCanvas;
import com.diaggen.view.diagram.DiagramPlaceholderView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainViewController {
    private static final Logger LOGGER = Logger.getLogger(MainViewController.class.getName());
    private final EventBus eventBus = EventBus.getInstance();
    private final boolean isProcessingSelection = false;
    @FXML
    private StackPane diagramCanvasContainer;
    @FXML
    private Label statusLabel;
    @FXML
    private Label projectInfoLabel;
    @FXML
    private VBox editorPanel;
    @FXML
    private VBox editorContent;
    @FXML
    private Button deleteClassButton;
    @FXML
    private Button deleteRelationButton;
    @FXML
    private StackPane editorPaneContainer;
    @FXML
    private ProjectExplorerController projectExplorerController;
    private MainController mainController;
    private DiagramCanvas diagramCanvas;
    private DiagramPlaceholderView placeholderView;
    private EditorPanelController editorController;
    private DiagramClass selectedClass;
    private DiagramRelation selectedRelation;
    private boolean isProcessingEvent = false;

    private LayoutController layoutController;
    private ProjectController projectController;
    private ExportController exportController;

    @FXML
    public void initialize() {
        LOGGER.log(Level.INFO, "Initializing MainViewController");

        diagramCanvas = new DiagramCanvas();
        placeholderView = new DiagramPlaceholderView();

        // Initialiser les composants dans le conteneur
        diagramCanvasContainer.getChildren().addAll(diagramCanvas, placeholderView);
        placeholderView.setVisible(false); // Caché par défaut

        // Configuration du bouton de création de diagramme
        placeholderView.getCreateDiagramButton().setOnAction(e -> handleNewDiagram());

        editorController = new EditorPanelController(editorContent);

        editorPanel.visibleProperty().addListener((obs, wasVisible, isVisible) -> {
            editorPaneContainer.setMouseTransparent(!isVisible);
            if (diagramCanvas != null) {
                double editorWidth = isVisible ? editorPanel.getWidth() : 0;
                if (editorWidth <= 0 && isVisible) {
                    editorWidth = 300;
                }

                diagramCanvas.setEditorPanelState(isVisible, editorWidth);
            }
        });

        Platform.runLater(() -> {
            if (editorPanel.getScene() != null) {
                editorPanel.widthProperty().addListener((obs, oldWidth, newWidth) -> {
                    if (editorPanel.isVisible() && diagramCanvas != null && newWidth.doubleValue() > 0) {
                        diagramCanvas.setEditorPanelState(true, newWidth.doubleValue());
                    }
                });
            }
        });

        Button closeButton = new Button("×");
        closeButton.getStyleClass().add("close-button");
        closeButton.setOnAction(e -> {
            editorPanel.setVisible(false);
            if (diagramCanvas != null) {
                diagramCanvas.deselectAll();
                diagramCanvas.setEditorPanelState(false, 0);
            }
        });

        Label editorTitleLabel = (Label) editorPanel.getChildren().stream()
                .filter(node -> node instanceof Label && ((Label) node).getText().equals("Éditeur"))
                .findFirst()
                .orElse(null);

        if (editorTitleLabel != null) {
            HBox titleBox = new HBox();
            titleBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            titleBox.setSpacing(10);
            Label newTitleLabel = new Label("Éditeur");
            newTitleLabel.getStyleClass().add("editor-title");

            titleBox.getChildren().addAll(newTitleLabel, new javafx.scene.layout.Region(), closeButton);
            javafx.scene.layout.HBox.setHgrow(titleBox.getChildren().get(1), javafx.scene.layout.Priority.ALWAYS);
            int labelIndex = editorPanel.getChildren().indexOf(editorTitleLabel);
            editorPanel.getChildren().remove(editorTitleLabel);
            editorPanel.getChildren().add(labelIndex, titleBox);
            titleBox.setPadding(new javafx.geometry.Insets(10, 15, 10, 15));
        }

        deleteClassButton.setDisable(true);
        deleteRelationButton.setDisable(true);

        setupSelectionHandling();
        setupEventBusListeners();
        setupKeyboardShortcuts();
        setupProperLayering();

        // Appliquer l'état initial
        updateDiagramDisplay();

        LOGGER.log(Level.INFO, "MainViewController initialization complete");
    }

    private void updateDiagramDisplay() {
        if (mainController == null) {
            // Si mainController n'est pas encore initialisé, on affiche simplement le placeholder
            diagramCanvas.setVisible(false);
            placeholderView.updateForProject(null);
            placeholderView.setVisible(true);
            setStatus("Initialisation...");
            return;
        }

        ClassDiagram diagram = mainController.getDiagramStore().getActiveDiagram();
        Project activeProject = mainController.getDiagramStore().getActiveProject();

        if (diagram != null) {
            // Un diagramme est sélectionné - afficher le canvas
            diagramCanvas.loadDiagram(diagram);
            diagramCanvas.setVisible(true);
            placeholderView.setVisible(false);
            setStatus("Diagramme actif: " + diagram.getName());
        } else {
            // Aucun diagramme sélectionné - afficher le placeholder
            // D'abord s'assurer que le canvas est vide
            diagramCanvas.clear();
            diagramCanvas.setVisible(false);

            // Mettre à jour le placeholder selon que nous avons un projet actif ou non
            placeholderView.updateForProject(activeProject);
            placeholderView.setVisible(true);

            if (activeProject != null) {
                setStatus("Projet actif: " + activeProject.getName() + " - Aucun diagramme sélectionné");
            } else {
                setStatus("Aucun projet actif");
            }
        }
    }

    public void setLayoutController(LayoutController layoutController) {
        this.layoutController = layoutController;
    }

    public void setProjectController(ProjectController projectController) {
        this.projectController = projectController;
    }

    public void setExportController(ExportController exportController) {
        this.exportController = exportController;
    }

    private void setupProperLayering() {
        diagramCanvasContainer.setViewOrder(1.0);
        if (diagramCanvasContainer.getParent() != null) {
            diagramCanvasContainer.getParent().setViewOrder(0.5);
        }
        if (editorPanel != null) {
            editorPanel.setViewOrder(0.0);
        }
    }

    private void setupEventBusListeners() {
        eventBus.subscribe(ProjectActivatedEvent.class, event -> {
            if (isProcessingEvent) return;

            LOGGER.log(Level.INFO, "ProjectActivatedEvent received for project ID: {0}", event.getDiagramId());
            isProcessingEvent = true;
            try {
                Platform.runLater(() -> {
                    updateDiagramDisplay();
                    updateProjectInfo();
                });
            } finally {
                isProcessingEvent = false;
            }
        });

        eventBus.subscribe(DiagramActivatedEvent.class, event -> {
            if (isProcessingEvent) return;

            LOGGER.log(Level.INFO, "DiagramActivatedEvent received for diagram ID: {0}", event.getDiagramId());
            isProcessingEvent = true;
            try {
                Platform.runLater(() -> {
                    updateDiagramDisplay();
                    updateProjectInfo();
                });
            } finally {
                isProcessingEvent = false;
            }
        });

        eventBus.subscribe(DiagramChangedEvent.class, event -> {
            LOGGER.log(Level.FINE, "DiagramChangedEvent received for diagram ID: {0}", event.getDiagramId());
            Platform.runLater(() -> {
                if (diagramCanvas.getDiagram() != null &&
                        diagramCanvas.getDiagram().getId().equals(event.getDiagramId())) {
                    diagramCanvas.refresh();
                }
                updateProjectInfo();
            });
        });

        eventBus.subscribe(ProjectChangedEvent.class, event -> {
            LOGGER.log(Level.FINE, "ProjectChangedEvent received for project ID: {0}", event.getDiagramId());
            Platform.runLater(() -> {
                updateProjectInfo();
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
                Platform.runLater(() -> {
                    double editorWidth = editorPanel.getWidth();
                    if (editorWidth <= 0) {
                        editorWidth = 300;
                    }
                    diagramCanvas.setEditorPanelState(true, editorWidth);
                });

                setStatus("Classe sélectionnée: " + diagramClass.getName());
            } else {
                selectedClass = null;
                deleteClassButton.setDisable(true);
                if (selectedRelation == null) {
                    editorPanel.setVisible(false);
                    if (diagramCanvas != null) {
                        diagramCanvas.setEditorPanelState(false, 0);
                    }
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
                Platform.runLater(() -> {
                    double editorWidth = editorPanel.getWidth();
                    if (editorWidth <= 0) {
                        editorWidth = 300;
                    }
                    diagramCanvas.setEditorPanelState(true, editorWidth);
                });

                setStatus("Relation sélectionnée: " + relation.getRelationType().getDisplayName() +
                        " entre " + relation.getSourceClass().getName() +
                        " et " + relation.getTargetClass().getName());
            } else {
                selectedRelation = null;
                deleteRelationButton.setDisable(true);
                if (selectedClass == null) {
                    editorPanel.setVisible(false);
                    if (diagramCanvas != null) {
                        diagramCanvas.setEditorPanelState(false, 0);
                    }
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
        Platform.runLater(() -> {
            Scene scene = diagramCanvasContainer.getScene();
            if (scene != null) {
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

                KeyCombination undoKey = new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN);
                scene.getAccelerators().put(undoKey, this::handleUndo);

                KeyCombination redoKey = new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN);
                scene.getAccelerators().put(redoKey, this::handleRedo);

                KeyCombination zoomInKey = new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.CONTROL_DOWN);
                scene.getAccelerators().put(zoomInKey, () -> diagramCanvas.getNavigationManager().zoomIn());

                KeyCombination zoomOutKey = new KeyCodeCombination(KeyCode.MINUS, KeyCombination.CONTROL_DOWN);
                scene.getAccelerators().put(zoomOutKey, () -> diagramCanvas.getNavigationManager().zoomOut());

                KeyCombination resetZoomKey = new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.CONTROL_DOWN);
                scene.getAccelerators().put(resetZoomKey, () -> diagramCanvas.getNavigationManager().resetView());

                KeyCombination fitToViewKey = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN);
                scene.getAccelerators().put(fitToViewKey, () -> diagramCanvas.zoomToFit());

                KeyCombination addClassKey = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN);
                scene.getAccelerators().put(addClassKey, () -> {
                    if (mainController != null) {
                        mainController.handleAddClass();
                    }
                });

                KeyCombination addRelationKey = new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN);
                scene.getAccelerators().put(addRelationKey, () -> {
                    if (mainController != null) {
                        mainController.handleAddRelation();
                    }
                });

                KeyCombination newProjectKey = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
                scene.getAccelerators().put(newProjectKey, this::handleNewProject);

                KeyCombination newDiagramKey = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
                scene.getAccelerators().put(newDiagramKey, this::handleNewDiagram);

                KeyCombination saveProjectKey = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
                scene.getAccelerators().put(saveProjectKey, this::handleSaveProject);

                KeyCombination openProjectKey = new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
                scene.getAccelerators().put(openProjectKey, this::handleOpenProject);
            }
        });
    }

    public void setMainController(MainController mainController) {
        LOGGER.log(Level.INFO, "Setting MainController");
        this.mainController = mainController;
        // Mise à jour de l'affichage après initialisation du contrôleur
        Platform.runLater(this::updateDiagramDisplay);
    }

    public DiagramCanvas getDiagramCanvas() {
        return diagramCanvas;
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    private void updateEditorPanelState(boolean isVisible) {
        if (diagramCanvas == null) return;

        double editorWidth = 0;
        if (isVisible) {
            editorWidth = editorPanel.getWidth();
            if (editorWidth <= 0) {
                editorWidth = editorPanel.getPrefWidth();
            }
            if (editorWidth <= 0) {
                editorWidth = 300;
            }
        }
        diagramCanvas.setEditorPanelState(isVisible, editorWidth);
    }

    public void deselectAllAndCloseEditor() {
        if (diagramCanvas != null) {
            diagramCanvas.deselectAll();
        }

        if (editorPanel != null) {
            editorPanel.setVisible(false);
            updateEditorPanelState(false);
        }
    }

    public DiagramClass getSelectedClass() {
        return selectedClass;
    }

    public DiagramRelation getSelectedRelation() {
        return selectedRelation;
    }


    @FXML
    private void handleNewProject() {
        if (projectController != null) {
            LOGGER.log(Level.INFO, "Creating new project");
            Project newProject = projectController.createNewProjectWithDialog();
            if (newProject != null) {
                setStatus("Nouveau projet créé : " + newProject.getName());
                updateProjectInfo();
            }
        }
    }

    @FXML
    private void handleOpenProject() {
        if (projectController != null) {
            LOGGER.log(Level.INFO, "Opening project");
            projectController.openProject();
        }
    }

    @FXML
    private void handleSaveProject() {
        if (projectController != null) {
            LOGGER.log(Level.INFO, "Saving project");
            projectController.saveProject();
            Project activeProject = mainController.getDiagramStore().getActiveProject();
            if (activeProject != null) {
                File projectFile = mainController.getDiagramStore().getCurrentProjectFile();
                if (projectFile != null) {
                    setStatus("Projet sauvegardé : " + projectFile.getAbsolutePath());
                }
            }
            updateProjectInfo();
        }
    }


    @FXML
    private void handleSaveProjectAs() {
        if (projectController != null) {
            LOGGER.log(Level.INFO, "Saving project as...");
            projectController.saveProjectAs();
            Project activeProject = mainController.getDiagramStore().getActiveProject();
            if (activeProject != null) {
                File projectFile = mainController.getDiagramStore().getCurrentProjectFile();
                if (projectFile != null) {
                    setStatus("Projet sauvegardé sous : " + projectFile.getAbsolutePath());
                }
            }
            updateProjectInfo();
        }
    }

    private void updateProjectInfo() {
        Platform.runLater(() -> {
            Project activeProject = mainController != null ? mainController.getDiagramStore().getActiveProject() : null;
            ClassDiagram activeDiagram = mainController != null ? mainController.getDiagramStore().getActiveDiagram() : null;

            if (activeProject != null) {
                String modifiedIndicator = projectController != null && projectController.isProjectModified() ? " *" : "";
                String projectInfo = "Projet: " + activeProject.getName() + modifiedIndicator;

                if (activeDiagram != null) {
                    projectInfo += " | Diagramme: " + activeDiagram.getName();
                }

                File projectFile = mainController.getDiagramStore().getCurrentProjectFile();
                if (projectFile != null) {
                    projectInfo += " | Fichier: " + projectFile.getName();
                } else if (projectController != null && projectController.isProjectModified()) {
                    projectInfo += " | Non sauvegardé";
                }

                projectInfoLabel.setText(projectInfo);
            } else {
                projectInfoLabel.setText("Aucun projet actif");
            }
        });
    }


    @FXML
    private void handleImportDiagrams() {
        if (projectController != null) {
            LOGGER.log(Level.INFO, "Importing diagrams from another project");
            projectController.importDiagramsFromProject();
        }
    }

    @FXML
    private void handleExportDiagram() {
        if (exportController != null) {
            Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
            dialog.setTitle("Exporter le diagramme");
            dialog.setHeaderText("Choisir le format d'exportation");
            dialog.setContentText("Quel format souhaitez-vous utiliser ?");

            ButtonType pngButton = new ButtonType("PNG");
            ButtonType svgButton = new ButtonType("SVG");
            ButtonType pumlButton = new ButtonType("PlantUML");
            ButtonType javaButton = new ButtonType("Code Java");
            ButtonType cancelButton = ButtonType.CANCEL;

            dialog.getButtonTypes().setAll(pngButton, svgButton, pumlButton, javaButton, cancelButton);

            dialog.showAndWait().ifPresent(result -> {
                if (result == pngButton) {
                    exportController.exportToPNG();
                } else if (result == svgButton) {
                    exportController.exportToSVG();
                } else if (result == pumlButton) {
                    exportController.exportToPlantUML();
                } else if (result == javaButton) {
                    exportController.exportToJavaCode();
                }
            });
        }
    }


    @FXML
    private void handleNewDiagram() {
        if (mainController != null) {
            LOGGER.log(Level.INFO, "Creating new diagram");
            mainController.handleNewDiagram();
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
                        "et gérer des projets contenant des diagrammes de classes.\n\n" +
                        "Développé avec JavaFX " + System.getProperty("javafx.version") + "\n" +
                        "Java " + System.getProperty("java.version") + "\n\n" +
                        "© 2025 Ryan Korban"
        );
        alert.showAndWait();
    }

    @FXML
    private void handleExit() {
        if (projectController != null && mainController != null) {
            boolean canExit = projectController.checkSaveCurrentProject();
            if (canExit) {
                closeApplication();
            }
        } else {
            closeApplication();
        }
    }

    private void closeApplication() {
        javafx.stage.Stage stage = (javafx.stage.Stage) diagramCanvasContainer.getScene().getWindow();
        stage.close();
    }

    public void setupWindowCloseHandler() {
        Platform.runLater(() -> {
            if (diagramCanvasContainer.getScene() != null && diagramCanvasContainer.getScene().getWindow() instanceof javafx.stage.Stage stage) {

                stage.setOnCloseRequest(event -> {
                    event.consume();
                    handleExit();
                });
            }
        });
    }

    public void handleOpenRecentProjects() {
        if (projectController != null) {
            projectController.showRecentProjects();
        }
    }

    @FXML
    private void handleArrangeClasses() {
        if (layoutController != null) {
            LOGGER.log(Level.INFO, "Arranging classes automatically");
            layoutController.arrangeClasses();
        }
    }

    @FXML
    private void handleForceDirectedLayout() {
        if (layoutController != null && mainController != null) {
            LOGGER.log(Level.INFO, "Applying force-directed layout");
            ClassDiagram activeDiagram = mainController.getDiagramStore().getActiveDiagram();
            if (activeDiagram != null) {
                layoutController.applyLayout(
                        activeDiagram,
                        LayoutFactory.LayoutType.FORCE_DIRECTED
                );
            }
        }
    }

    @FXML
    private void handleHierarchicalLayout() {
        if (layoutController != null && mainController != null) {
            LOGGER.log(Level.INFO, "Applying hierarchical layout");
            ClassDiagram activeDiagram = mainController.getDiagramStore().getActiveDiagram();
            if (activeDiagram != null) {
                layoutController.applyLayout(
                        activeDiagram,
                        LayoutFactory.LayoutType.HIERARCHICAL
                );
            }
        }
    }

    @FXML
    private void handleGridLayout() {
        if (layoutController != null && mainController != null) {
            LOGGER.log(Level.INFO, "Applying grid layout");
            ClassDiagram activeDiagram = mainController.getDiagramStore().getActiveDiagram();
            if (activeDiagram != null) {
                layoutController.applyLayout(
                        activeDiagram,
                        LayoutFactory.LayoutType.GRID
                );
            }
        }
    }

    public void configureAllControllers() {
        LOGGER.log(Level.INFO, "Configuring all controllers");
        if (projectExplorerController != null && mainController != null && projectController != null) {
            projectExplorerController.setDiagramStore(mainController.getDiagramStore());
            projectExplorerController.setProjectController(projectController);
            projectExplorerController.setDiagramController(mainController.getDiagramController());
            Platform.runLater(() -> {
                projectExplorerController.refreshUI();
            });
        } else {
            LOGGER.log(Level.WARNING, "Cannot configure ProjectExplorerController: one or more controllers are missing");
        }

        if (editorController != null && mainController != null) {
            editorController.setMainController(mainController);
        }
    }
}