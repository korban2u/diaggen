package com.diaggen;

import com.diaggen.controller.command.CommandManager;
import com.diaggen.controller.*;
import com.diaggen.event.DiagramChangedEvent;
import com.diaggen.event.EventBus;
import com.diaggen.event.EventListener;
import com.diaggen.model.DiagramStore;
import com.diaggen.service.ExportService;
import com.diaggen.view.diagram.DiagramCanvas;
import com.diaggen.view.controller.MainViewController;
import com.diaggen.view.diagram.canvas.NodeManager;
import com.diaggen.view.dialog.DialogFactory;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends Application {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    @Override
    public void start(Stage primaryStage) throws Exception {

        DiagramStore diagramStore = new DiagramStore();
        CommandManager commandManager = new CommandManager();
        DialogFactory dialogFactory = DialogFactory.getInstance();
        EventBus eventBus = EventBus.getInstance();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
        Parent root = loader.load();
        MainViewController viewController = loader.getController();

        DiagramCanvas diagramCanvas = viewController.getDiagramCanvas();

        ExportService exportService = new ExportService(diagramCanvas);

        ClassController classController = new ClassController(diagramStore, commandManager, dialogFactory);
        DiagramController diagramController = new DiagramController(diagramStore, commandManager);

        RelationController relationController = new RelationController(diagramStore, commandManager, dialogFactory, diagramCanvas);
        ExportController exportController = new ExportController(diagramStore, commandManager, exportService, classController, diagramController);

        diagramController.setOwnerWindow(primaryStage);
        exportController.setOwnerWindow(primaryStage);

        LOGGER.log(Level.INFO, "Controllers initialized");

        NodeManager nodeManager = diagramCanvas.getNodeManager();
        nodeManager.setCommandManager(commandManager);

        MainController mainController = new MainController(
                diagramStore,
                commandManager,
                classController,
                relationController,
                diagramController,
                exportController,
                diagramCanvas
        );

        viewController.setMainController(mainController);

        setupEventListeners(eventBus, diagramCanvas, viewController);

        if (diagramStore.getActiveDiagram() != null) {
            diagramCanvas.loadDiagram(diagramStore.getActiveDiagram());
            viewController.updateDiagramList(diagramStore.getDiagrams());
            viewController.updateSelectedDiagram(diagramStore.getActiveDiagram());
        }

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/main.css")).toExternalForm());

        primaryStage.setTitle("DiagGen - Générateur de diagrammes de classe");
        primaryStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/icon.png"))));
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

        primaryStage.setOnCloseRequest(e -> {

        });

        primaryStage.show();
    }

    private void setupEventListeners(EventBus eventBus, DiagramCanvas diagramCanvas, MainViewController viewController) {

        eventBus.subscribe(DiagramChangedEvent.class, event -> {
            diagramCanvas.refresh();
            viewController.setStatus("Diagramme mis à jour");
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}