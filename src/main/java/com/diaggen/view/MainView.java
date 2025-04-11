package com.diaggen.view;

import com.diaggen.model.ClassDiagram;
import com.diaggen.view.diagram.DiagramCanvas;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class MainView extends BorderPane {

    private final MenuBar menuBar;
    private final ListView<ClassDiagram> diagramListView;
    private final ToolBar toolBar;
    private final DiagramCanvas diagramCanvas;
    private final StatusBar statusBar;

    private Runnable onNewDiagram;
    private Runnable onEditDiagram;
    private Runnable onDeleteDiagram;
    private Consumer<ClassDiagram> onSelectDiagram;

    private Runnable onAddClass;
    private Runnable onEditClass;
    private Runnable onDeleteClass;

    private Runnable onAddRelation;
    private Runnable onEditRelation;
    private Runnable onDeleteRelation;

    private Runnable onSave;
    private Runnable onSaveAs;
    private Runnable onOpen;

    private Runnable onImportJavaCode;
    private Runnable onExportImage;
    private Runnable onExportSVG;
    private Runnable onExportPlantUML;
    private Runnable onExportJavaCode;

    public MainView() {
        menuBar = createMenuBar();
        diagramListView = createDiagramListView();
        toolBar = createToolBar();
        diagramCanvas = new DiagramCanvas();
        statusBar = new StatusBar();

        SplitPane splitPane = new SplitPane();

        VBox leftPanel = new VBox();
        leftPanel.getChildren().addAll(new Label("Diagrammes"), diagramListView);
        VBox.setVgrow(diagramListView, Priority.ALWAYS);

        splitPane.getItems().addAll(leftPanel, diagramCanvas);
        splitPane.setDividerPositions(0.2);

        setTop(new VBox(menuBar, toolBar));
        setCenter(splitPane);
        setBottom(statusBar);

        setPrefSize(1280, 800);
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        Menu fileMenu = new Menu("Fichier");
        MenuItem newItem = new MenuItem("Nouveau");
        MenuItem openItem = new MenuItem("Ouvrir...");
        MenuItem saveItem = new MenuItem("Enregistrer");
        MenuItem saveAsItem = new MenuItem("Enregistrer sous...");
        MenuItem exitItem = new MenuItem("Quitter");

        newItem.setOnAction(e -> {
            if (onNewDiagram != null) onNewDiagram.run();
        });
        openItem.setOnAction(e -> {
            if (onOpen != null) onOpen.run();
        });
        saveItem.setOnAction(e -> {
            if (onSave != null) onSave.run();
        });
        saveAsItem.setOnAction(e -> {
            if (onSaveAs != null) onSaveAs.run();
        });
        exitItem.setOnAction(e -> System.exit(0));

        fileMenu.getItems().addAll(newItem, openItem, new SeparatorMenuItem(),
                saveItem, saveAsItem, new SeparatorMenuItem(), exitItem);

        Menu editMenu = new Menu("Édition");
        MenuItem undoItem = new MenuItem("Annuler");
        MenuItem redoItem = new MenuItem("Rétablir");

        editMenu.getItems().addAll(undoItem, redoItem);

        Menu diagramMenu = new Menu("Diagramme");
        MenuItem addClassItem = new MenuItem("Ajouter une classe");
        MenuItem editClassItem = new MenuItem("Modifier la classe sélectionnée");
        MenuItem deleteClassItem = new MenuItem("Supprimer la classe sélectionnée");
        MenuItem addRelationItem = new MenuItem("Ajouter une relation");
        MenuItem editRelationItem = new MenuItem("Modifier la relation sélectionnée");
        MenuItem deleteRelationItem = new MenuItem("Supprimer la relation sélectionnée");

        addClassItem.setOnAction(e -> {
            if (onAddClass != null) onAddClass.run();
        });
        editClassItem.setOnAction(e -> {
            if (onEditClass != null) onEditClass.run();
        });
        deleteClassItem.setOnAction(e -> {
            if (onDeleteClass != null) onDeleteClass.run();
        });
        addRelationItem.setOnAction(e -> {
            if (onAddRelation != null) onAddRelation.run();
        });
        editRelationItem.setOnAction(e -> {
            if (onEditRelation != null) onEditRelation.run();
        });
        deleteRelationItem.setOnAction(e -> {
            if (onDeleteRelation != null) onDeleteRelation.run();
        });

        diagramMenu.getItems().addAll(
                addClassItem, editClassItem, deleteClassItem, new SeparatorMenuItem(),
                addRelationItem, editRelationItem, deleteRelationItem);

        Menu importExportMenu = new Menu("Import/Export");
        MenuItem importJavaItem = new MenuItem("Importer du code Java...");
        MenuItem exportImageItem = new MenuItem("Exporter en PNG...");
        MenuItem exportSVGItem = new MenuItem("Exporter en SVG...");
        MenuItem exportPlantUMLItem = new MenuItem("Exporter en PlantUML...");
        MenuItem exportJavaItem = new MenuItem("Générer du code Java...");

        importJavaItem.setOnAction(e -> {
            if (onImportJavaCode != null) onImportJavaCode.run();
        });
        exportImageItem.setOnAction(e -> {
            if (onExportImage != null) onExportImage.run();
        });
        exportSVGItem.setOnAction(e -> {
            if (onExportSVG != null) onExportSVG.run();
        });
        exportPlantUMLItem.setOnAction(e -> {
            if (onExportPlantUML != null) onExportPlantUML.run();
        });
        exportJavaItem.setOnAction(e -> {
            if (onExportJavaCode != null) onExportJavaCode.run();
        });

        importExportMenu.getItems().addAll(
                importJavaItem, new SeparatorMenuItem(),
                exportImageItem, exportSVGItem, exportPlantUMLItem, exportJavaItem);

        Menu helpMenu = new Menu("Aide");
        MenuItem aboutItem = new MenuItem("À propos");

        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, editMenu, diagramMenu, importExportMenu, helpMenu);

        return menuBar;
    }

    private ListView<ClassDiagram> createDiagramListView() {
        ListView<ClassDiagram> listView = new ListView<>();
        listView.setCellFactory(param -> new ListCell<>() {
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

        listView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && onSelectDiagram != null) {
                onSelectDiagram.accept(newValue);
            }
        });

        ContextMenu contextMenu = new ContextMenu();
        MenuItem newItem = new MenuItem("Nouveau diagramme");
        MenuItem editItem = new MenuItem("Renommer");
        MenuItem deleteItem = new MenuItem("Supprimer");

        newItem.setOnAction(e -> {
            if (onNewDiagram != null) onNewDiagram.run();
        });
        editItem.setOnAction(e -> {
            if (onEditDiagram != null) onEditDiagram.run();
        });
        deleteItem.setOnAction(e -> {
            if (onDeleteDiagram != null) onDeleteDiagram.run();
        });

        contextMenu.getItems().addAll(newItem, editItem, deleteItem);
        listView.setContextMenu(contextMenu);

        return listView;
    }

    private ToolBar createToolBar() {
        ToolBar toolBar = new ToolBar();

        Button newDiagramButton = new Button("Nouveau");
        Button saveDiagramButton = new Button("Enregistrer");
        Button openDiagramButton = new Button("Ouvrir");

        Separator separator1 = new Separator(Orientation.VERTICAL);

        Button addClassButton = new Button("+ Classe");
        Button editClassButton = new Button("Éditer Classe");
        Button deleteClassButton = new Button("- Classe");

        Separator separator2 = new Separator(Orientation.VERTICAL);

        Button addRelationButton = new Button("+ Relation");
        Button editRelationButton = new Button("Éditer Relation");
        Button deleteRelationButton = new Button("- Relation");

        Separator separator3 = new Separator(Orientation.VERTICAL);

        Button exportImageButton = new Button("Export PNG");

        newDiagramButton.setOnAction(e -> {
            if (onNewDiagram != null) onNewDiagram.run();
        });
        saveDiagramButton.setOnAction(e -> {
            if (onSave != null) onSave.run();
        });
        openDiagramButton.setOnAction(e -> {
            if (onOpen != null) onOpen.run();
        });

        addClassButton.setOnAction(e -> {
            if (onAddClass != null) onAddClass.run();
        });
        editClassButton.setOnAction(e -> {
            if (onEditClass != null) onEditClass.run();
        });
        deleteClassButton.setOnAction(e -> {
            if (onDeleteClass != null) onDeleteClass.run();
        });

        addRelationButton.setOnAction(e -> {
            if (onAddRelation != null) onAddRelation.run();
        });
        editRelationButton.setOnAction(e -> {
            if (onEditRelation != null) onEditRelation.run();
        });
        deleteRelationButton.setOnAction(e -> {
            if (onDeleteRelation != null) onDeleteRelation.run();
        });

        exportImageButton.setOnAction(e -> {
            if (onExportImage != null) onExportImage.run();
        });

        toolBar.getItems().addAll(
                newDiagramButton, saveDiagramButton, openDiagramButton, separator1,
                addClassButton, editClassButton, deleteClassButton, separator2,
                addRelationButton, editRelationButton, deleteRelationButton, separator3,
                exportImageButton);

        return toolBar;
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

    public void setOnNewDiagram(Runnable handler) {
        this.onNewDiagram = handler;
    }

    public void setOnEditDiagram(Runnable handler) {
        this.onEditDiagram = handler;
    }

    public void setOnDeleteDiagram(Runnable handler) {
        this.onDeleteDiagram = handler;
    }

    public void setOnSelectDiagram(Consumer<ClassDiagram> handler) {
        this.onSelectDiagram = handler;
    }

    public void setOnAddClass(Runnable handler) {
        this.onAddClass = handler;
    }

    public void setOnEditClass(Runnable handler) {
        this.onEditClass = handler;
    }

    public void setOnDeleteClass(Runnable handler) {
        this.onDeleteClass = handler;
    }

    public void setOnAddRelation(Runnable handler) {
        this.onAddRelation = handler;
    }

    public void setOnEditRelation(Runnable handler) {
        this.onEditRelation = handler;
    }

    public void setOnDeleteRelation(Runnable handler) {
        this.onDeleteRelation = handler;
    }

    public void setOnSave(Runnable handler) {
        this.onSave = handler;
    }

    public void setOnSaveAs(Runnable handler) {
        this.onSaveAs = handler;
    }

    public void setOnOpen(Runnable handler) {
        this.onOpen = handler;
    }

    public void setOnImportJavaCode(Runnable handler) {
        this.onImportJavaCode = handler;
    }

    public void setOnExportImage(Runnable handler) {
        this.onExportImage = handler;
    }

    public void setOnExportSVG(Runnable handler) {
        this.onExportSVG = handler;
    }

    public void setOnExportPlantUML(Runnable handler) {
        this.onExportPlantUML = handler;
    }

    public void setOnExportJavaCode(Runnable handler) {
        this.onExportJavaCode = handler;
    }

    private static class StatusBar extends HBox {
        private final Label statusLabel;

        public StatusBar() {
            setPadding(new Insets(5));
            setSpacing(10);

            statusLabel = new Label("Prêt");
            getChildren().add(statusLabel);
        }

        public void setStatus(String status) {
            statusLabel.setText(status);
        }
    }
}


