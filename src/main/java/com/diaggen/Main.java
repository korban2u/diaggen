package com.diaggen;

import com.diaggen.controller.MainController;
import com.diaggen.model.DiagramStore;
import com.diaggen.service.ExportService;
import com.diaggen.view.controller.MainViewController;
import com.diaggen.view.dialog.DialogFactory;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * Classe principale de l'application
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Charger la vue principale depuis FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
        Parent root = loader.load();

        // Obtenir le contrôleur de vue
        MainViewController viewController = loader.getController();

        // Créer le modèle et les services
        DiagramStore diagramStore = new DiagramStore();
        DialogFactory dialogFactory = DialogFactory.getInstance();
        ExportService exportService = new ExportService(viewController.getDiagramCanvas());

        // Créer et configurer le contrôleur principal
        MainController mainController = new MainController(diagramStore, dialogFactory, exportService);
        mainController.setMainViewController(viewController);
        viewController.setMainController(mainController);

        // Configuration initiale
        mainController.initialize();

        // Configurer la scène et le stage
        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/main.css")).toExternalForm());

        primaryStage.setTitle("DiagGen - Générateur de diagrammes de classe");
        primaryStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/icon.png"))));
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    /**
     * Point d'entrée de l'application
     * @param args Arguments de la ligne de commande
     */
    public static void main(String[] args) {
        launch(args);
    }
}