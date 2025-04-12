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

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Initialisation des composants de base
        DiagramStore diagramStore = new DiagramStore();
        CommandManager commandManager = new CommandManager();
        DialogFactory dialogFactory = DialogFactory.getInstance();
        EventBus eventBus = EventBus.getInstance();

        // Charger la vue principale
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
        Parent root = loader.load();
        MainViewController viewController = loader.getController();

        // Obtenir le canvas de diagramme
        DiagramCanvas diagramCanvas = viewController.getDiagramCanvas();

        // Initialiser le service d'exportation
        ExportService exportService = new ExportService(diagramCanvas);

        // Créer les contrôleurs spécialisés
        ClassController classController = new ClassController(diagramStore, commandManager, dialogFactory);
        RelationController relationController = new RelationController(diagramStore, commandManager, dialogFactory, diagramCanvas);
        DiagramController diagramController = new DiagramController(diagramStore, commandManager);
        ExportController exportController = new ExportController(diagramStore, commandManager, exportService, classController);

        // Configurer les fenêtres parent pour les dialogues
        diagramController.setOwnerWindow(primaryStage);
        exportController.setOwnerWindow(primaryStage);

        // Injecter le CommandManager dans NodeManager pour gérer les déplacements
        NodeManager nodeManager = diagramCanvas.getNodeManager();
        nodeManager.setCommandManager(commandManager);

        // Créer un contrôleur principal pour coordonner les sous-contrôleurs
        MainController mainController = new MainController(
                diagramStore,
                commandManager,
                classController,
                relationController,
                diagramController,
                exportController,
                diagramCanvas
        );

        // Connecter les contrôleurs à la vue
        viewController.setMainController(mainController);

        // Configurer les écouteurs d'événements
        setupEventListeners(eventBus, diagramCanvas, viewController);

        // Charger le diagramme initial
        if (diagramStore.getActiveDiagram() != null) {
            diagramCanvas.loadDiagram(diagramStore.getActiveDiagram());
            viewController.updateDiagramList(diagramStore.getDiagrams());
            viewController.updateSelectedDiagram(diagramStore.getActiveDiagram());
        }

        // Configurer et afficher la scène
        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/main.css")).toExternalForm());

        primaryStage.setTitle("DiagGen - Générateur de diagrammes de classe");
        primaryStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/icon.png"))));
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

        // Gérer la fermeture propre de l'application
        primaryStage.setOnCloseRequest(e -> {
            // Si besoin, ajouter ici un dialogue pour confirmer la fermeture si des modifications n'ont pas été sauvegardées
        });

        primaryStage.show();
    }

    private void setupEventListeners(EventBus eventBus, DiagramCanvas diagramCanvas, MainViewController viewController) {
        // Écouter les changements de diagramme pour rafraîchir le canvas
        eventBus.subscribe(DiagramChangedEvent.class, event -> {
            diagramCanvas.refresh();
            viewController.setStatus("Diagramme mis à jour");
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}