package com.diaggen;

import com.diaggen.config.AppConfig;
import com.diaggen.controller.*;
import com.diaggen.controller.command.CommandManager;
import com.diaggen.event.DiagramChangedEvent;
import com.diaggen.event.EventBus;
import com.diaggen.model.DiagramStore;
import com.diaggen.service.ExportService;
import com.diaggen.service.LayoutService;
import com.diaggen.view.controller.MainViewController;
import com.diaggen.view.diagram.DiagramCanvas;
import com.diaggen.view.diagram.canvas.NodeManager;
import com.diaggen.view.dialog.DialogFactory;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Classe principale de l'application DiagGen
 * Contient la logique d'initialisation et de démarrage de l'application
 */
public class DiagGenApp extends Application {

    private static final Logger LOGGER = Logger.getLogger(DiagGenApp.class.getName());
    private DiagramStore diagramStore;
    private CommandManager commandManager;
    private ProjectController projectController;

    /**
     * Point d'entrée principal pour l'application JavaFX
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        LOGGER.log(Level.INFO, "Démarrage de l'application DiagGen");

        // Initialisation de l'application
        initializeApplication(primaryStage);

        // Affichage de la fenêtre principale
        primaryStage.show();

        // Chargement des données initiales
        loadInitialData();
    }

    /**
     * Initialise tous les composants de l'application
     */
    private void initializeApplication(Stage primaryStage) throws Exception {
        ensureStylesDirectory();

        // Initialisation des composants principaux
        diagramStore = new DiagramStore();
        commandManager = new CommandManager();
        DialogFactory dialogFactory = DialogFactory.getInstance();
        EventBus eventBus = EventBus.getInstance();

        // Chargement de l'interface utilisateur
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
        Parent root = loader.load();
        MainViewController viewController = loader.getController();

        // Configuration du canvas et des services
        DiagramCanvas diagramCanvas = viewController.getDiagramCanvas();
        ExportService exportService = new ExportService(diagramCanvas);
        LayoutService layoutService = new LayoutService(diagramCanvas);

        // Initialisation des contrôleurs
        ClassController classController = new ClassController(diagramStore, commandManager, dialogFactory);
        classController.setLayoutService(layoutService);

        DiagramController diagramController = new DiagramController(diagramStore, commandManager);
        diagramController.setOwnerWindow(primaryStage);

        RelationController relationController = new RelationController(diagramStore, commandManager, dialogFactory, diagramCanvas);

        projectController = new ProjectController(diagramStore, commandManager);
        projectController.setOwnerWindow(primaryStage);

        ExportController exportController = new ExportController(diagramStore, commandManager, exportService,
                classController, diagramController);
        exportController.setLayoutService(layoutService);
        exportController.setOwnerWindow(primaryStage);

        LayoutController layoutController = new LayoutController(diagramStore, commandManager, layoutService);

        LOGGER.log(Level.INFO, "Contrôleurs initialisés");

        // Configuration du gestionnaire de nœuds
        NodeManager nodeManager = diagramCanvas.getNodeManager();
        nodeManager.setCommandManager(commandManager);

        // Configuration du contrôleur principal
        MainController mainController = new MainController(
                diagramStore,
                commandManager,
                classController,
                relationController,
                diagramController,
                exportController,
                diagramCanvas
        );

        // Configuration des contrôleurs de vue
        viewController.setProjectController(projectController);
        viewController.setExportController(exportController);
        viewController.setMainController(mainController);
        viewController.setLayoutController(layoutController);
        viewController.configureAllControllers();

        // Configuration des écouteurs d'événements
        setupEventListeners(eventBus, diagramCanvas, viewController);

        // Configuration de la scène
        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/main.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/navigation-styles.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/project-explorer.css")).toExternalForm());

        // Configuration de la fenêtre principale
        primaryStage.setTitle("DiagGen - Générateur de diagrammes de classe");
        loadApplicationIcon(primaryStage);

        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

        // Configuration de la gestion de fermeture de la fenêtre
        viewController.setupWindowCloseHandler();
    }

    /**
     * Charge les données initiales de l'application
     */
    private void loadInitialData() {
        if (diagramStore.getProjects().isEmpty()) {
            loadMostRecentProject();
            if (diagramStore.getProjects().isEmpty()) {
                projectController.createNewProject("Projet par défaut");
            }
        }
    }

    /**
     * Configure les écouteurs d'événements
     */
    private void setupEventListeners(EventBus eventBus, DiagramCanvas diagramCanvas, MainViewController viewController) {
        eventBus.subscribe(DiagramChangedEvent.class, event -> {
            diagramCanvas.refresh();
            viewController.setStatus("Diagramme mis à jour");
        });
    }

    /**
     * Vérifie et crée le répertoire de styles si nécessaire
     */
    private void ensureStylesDirectory() {
        try {
            Path stylesDir = Paths.get(getClass().getResource("/styles").toURI());
            Path navigationStylesPath = stylesDir.resolve("navigation-styles.css");

            if (!Files.exists(navigationStylesPath)) {
                if (!Files.exists(stylesDir)) {
                    Files.createDirectories(stylesDir);
                }
                Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/styles/navigation-styles.css")), navigationStylesPath);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Échec de la vérification du répertoire de styles: " + e.getMessage());
        }
    }

    /**
     * Charge l'icône de l'application
     */
    private void loadApplicationIcon(Stage primaryStage) {
        try {
            InputStream iconStream = getClass().getResourceAsStream("/images/diagram-icon.png");
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            } else {
                InputStream defaultIconStream = getClass().getResourceAsStream("/images/icon.png");
                if (defaultIconStream != null) {
                    primaryStage.getIcons().add(new Image(defaultIconStream));
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Impossible de charger l'icône de l'application", e);
        }
    }

    /**
     * Charge le projet le plus récent
     */
    private void loadMostRecentProject() {
        if (!AppConfig.getInstance().isAutoLoadLastProject()) {
            return;
        }

        String recentProjectPath = projectController.getMostRecentProject();
        if (recentProjectPath != null && !recentProjectPath.isEmpty()) {
            File projectFile = new File(recentProjectPath);
            if (projectFile.exists()) {
                LOGGER.log(Level.INFO, "Chargement du projet le plus récent: {0}", projectFile.getAbsolutePath());
                projectController.openProjectFile(projectFile);
            }
        }
    }

    /**
     * Méthode appelée lors de l'arrêt de l'application
     */
    @Override
    public void stop() {
        LOGGER.log(Level.INFO, "Arrêt de l'application DiagGen");
        // Enregistrer les configurations, fermer les ressources, etc.
    }
}