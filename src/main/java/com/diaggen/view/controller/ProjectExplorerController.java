package com.diaggen.view.controller;

import com.diaggen.controller.DiagramController;
import com.diaggen.controller.ProjectController;
import com.diaggen.event.*;
import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramStore;
import com.diaggen.model.Project;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProjectExplorerController {
    private static final Logger LOGGER = Logger.getLogger(ProjectExplorerController.class.getName());

    @FXML
    private ListView<Project> projectListView;

    @FXML
    private ListView<ClassDiagram> diagramListView;

    @FXML
    private Button addDiagramButton;

    @FXML
    private Button importDiagramsButton;

    @FXML
    private Button newDiagramButton;

    private DiagramStore diagramStore;
    private ProjectController projectController;
    private DiagramController diagramController;
    private final EventBus eventBus = EventBus.getInstance();

    private ContextMenu projectContextMenu;
    private ContextMenu diagramContextMenu;

    private boolean isProcessingSelection = false;
    private boolean isInitialized = false;

    @FXML
    public void initialize() {
        LOGGER.log(Level.INFO, "Initializing ProjectExplorerController");

        setupProjectListView();
        setupDiagramListView();
        setupContextMenus();

        projectListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !isProcessingSelection) {
                isProcessingSelection = true;
                try {
                    handleSelectProject(newVal);
                } finally {
                    isProcessingSelection = false;
                }
            }

            boolean projectSelected = (newVal != null);
            addDiagramButton.setDisable(!projectSelected);
            importDiagramsButton.setDisable(!projectSelected);
            newDiagramButton.setDisable(!projectSelected);
        });

        diagramListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !isProcessingSelection) {
                isProcessingSelection = true;
                try {
                    handleSelectDiagram(newVal);
                } finally {
                    isProcessingSelection = false;
                }
            }
        });

        setupEventBusListeners();
        isInitialized = true;
    }

    private void setupProjectListView() {
        projectListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        projectListView.setCellFactory(param -> new ListCell<Project>() {
            @Override
            protected void updateItem(Project item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getName());

                    ImageView icon = new ImageView();
                    icon.setFitHeight(16);
                    icon.setFitWidth(16);
                    setGraphic(icon);

                    if (diagramStore != null && item.equals(diagramStore.getActiveProject())) {
                        setStyle("-fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        projectListView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                Project selectedProject = projectListView.getSelectionModel().getSelectedItem();
                if (selectedProject != null) {
                    handleSelectProject(selectedProject);
                }
            }
        });
    }

    private void setupDiagramListView() {
        diagramListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        diagramListView.setCellFactory(param -> new ListCell<ClassDiagram>() {
            @Override
            protected void updateItem(ClassDiagram item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getName());

                    ImageView icon = new ImageView();
                    icon.setFitHeight(16);
                    icon.setFitWidth(16);
                    setGraphic(icon);

                    if (diagramStore != null && item.equals(diagramStore.getActiveDiagram())) {
                        setStyle("-fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        diagramListView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                ClassDiagram selectedDiagram = diagramListView.getSelectionModel().getSelectedItem();
                if (selectedDiagram != null) {
                    handleSelectDiagram(selectedDiagram);
                }
            }
        });
    }

    private void setupContextMenus() {

        projectContextMenu = new ContextMenu();
        MenuItem newProjectItem = new MenuItem("Nouveau projet");
        newProjectItem.setOnAction(e -> handleAddProject());

        MenuItem renameProjectItem = new MenuItem("Renommer");
        renameProjectItem.setOnAction(e -> {
            Project selectedProject = projectListView.getSelectionModel().getSelectedItem();
            if (selectedProject != null) {
                handleRenameProject(selectedProject);
            }
        });

        MenuItem deleteProjectItem = new MenuItem("Supprimer");
        deleteProjectItem.setOnAction(e -> {
            Project selectedProject = projectListView.getSelectionModel().getSelectedItem();
            if (selectedProject != null) {
                handleDeleteProject(selectedProject);
            }
        });

        projectContextMenu.getItems().addAll(newProjectItem, renameProjectItem, deleteProjectItem);
        projectListView.setContextMenu(projectContextMenu);

        diagramContextMenu = new ContextMenu();
        MenuItem newDiagramItem = new MenuItem("Nouveau diagramme");
        newDiagramItem.setOnAction(e -> handleAddDiagram());

        MenuItem renameDiagramItem = new MenuItem("Renommer");
        renameDiagramItem.setOnAction(e -> {
            ClassDiagram selectedDiagram = diagramListView.getSelectionModel().getSelectedItem();
            if (selectedDiagram != null) {
                handleRenameDiagram(selectedDiagram);
            }
        });

        MenuItem deleteDiagramItem = new MenuItem("Supprimer");
        deleteDiagramItem.setOnAction(e -> {
            ClassDiagram selectedDiagram = diagramListView.getSelectionModel().getSelectedItem();
            if (selectedDiagram != null) {
                handleDeleteDiagram(selectedDiagram);
            }
        });

        MenuItem duplicateDiagramItem = new MenuItem("Dupliquer");
        duplicateDiagramItem.setOnAction(e -> {
            ClassDiagram selectedDiagram = diagramListView.getSelectionModel().getSelectedItem();
            if (selectedDiagram != null) {
                handleDuplicateDiagram(selectedDiagram);
            }
        });

        diagramContextMenu.getItems().addAll(newDiagramItem, renameDiagramItem, duplicateDiagramItem, deleteDiagramItem);
        diagramListView.setContextMenu(diagramContextMenu);
    }

    public void setDiagramStore(DiagramStore diagramStore) {
        this.diagramStore = diagramStore;
        projectListView.setItems(diagramStore.getProjects());
        updateSelection();
    }

    public void setProjectController(ProjectController projectController) {
        this.projectController = projectController;
    }

    public void setDiagramController(DiagramController diagramController) {
        this.diagramController = diagramController;
    }

    private void setupEventBusListeners() {
        eventBus.subscribe(ProjectActivatedEvent.class, (EventListener<ProjectActivatedEvent>) event -> {
            Platform.runLater(() -> {
                updateSelection();
            });
        });

        eventBus.subscribe(DiagramActivatedEvent.class, (EventListener<DiagramActivatedEvent>) event -> {
            Platform.runLater(() -> {
                updateSelection();
            });
        });

        eventBus.subscribe(ProjectChangedEvent.class, (EventListener<ProjectChangedEvent>) event -> {
            Platform.runLater(() -> {
                projectListView.refresh();

                if (event.getChangeType() == ProjectChangedEvent.ChangeType.DIAGRAMS_IMPORTED &&
                        diagramStore.getActiveProject() != null) {
                    updateDiagramList(diagramStore.getActiveProject());
                }
            });
        });

        eventBus.subscribe(DiagramChangedEvent.class, (EventListener<DiagramChangedEvent>) event -> {
            Platform.runLater(() -> {
                diagramListView.refresh();
            });
        });
    }

    private void updateSelection() {
        if (!isInitialized) return;

        isProcessingSelection = true;
        try {

            Project activeProject = diagramStore.getActiveProject();
            if (activeProject != null) {
                projectListView.getSelectionModel().select(activeProject);
                updateDiagramList(activeProject);

                ClassDiagram activeDiagram = diagramStore.getActiveDiagram();
                if (activeDiagram != null && activeProject.getDiagrams().contains(activeDiagram)) {
                    diagramListView.getSelectionModel().select(activeDiagram);
                }
            } else {
                projectListView.getSelectionModel().clearSelection();
                diagramListView.setItems(FXCollections.observableArrayList());
            }

            projectListView.refresh();
            diagramListView.refresh();
        } finally {
            isProcessingSelection = false;
        }
    }

    private void updateDiagramList(Project project) {
        if (project != null) {
            diagramListView.setItems(project.getDiagrams());
        } else {
            diagramListView.setItems(FXCollections.observableArrayList());
        }
    }

    private void handleSelectProject(Project project) {
        if (projectController != null && project != null) {
            projectController.activateProject(project);
            updateDiagramList(project);
        }
    }

    private void handleSelectDiagram(ClassDiagram diagram) {
        if (diagramController != null && diagram != null) {
            diagramController.activateDiagram(diagram);
        }
    }

    @FXML
    private void handleAddProject() {
        if (projectController != null) {
            projectController.createNewProjectWithDialog();
        }
    }

    private void handleRenameProject(Project project) {
        if (projectController != null) {
            projectController.renameProject(project, null);
        }
    }

    private void handleDeleteProject(Project project) {
        if (projectController != null) {
            projectController.deleteProject(project);
        }
    }

    @FXML
    private void handleAddDiagram() {
        if (diagramController != null) {
            diagramController.createNewDiagramWithDialog();
        }
    }

    @FXML
    private void handleImportDiagrams() {
        if (projectController != null) {
            projectController.importDiagramsFromProject();
        }
    }

    private void handleRenameDiagram(ClassDiagram diagram) {
        if (diagramController != null) {
            diagramController.renameDiagram(diagram, null);
        }
    }

    private void handleDeleteDiagram(ClassDiagram diagram) {
        if (diagramController != null) {
            diagramController.deleteDiagram(diagram);
        }
    }

    private void handleDuplicateDiagram(ClassDiagram diagram) {
        if (diagramController != null && diagramStore.getActiveProject() != null) {

            try {
                ClassDiagram copy = diagram.createCopy();
                copy.setName(diagram.getName() + " (copie)");
                diagramStore.getActiveProject().addDiagram(copy);

                diagramListView.refresh();
                diagramListView.getSelectionModel().select(copy);

                diagramController.activateDiagram(copy);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error duplicating diagram", e);
            }
        }
    }


}