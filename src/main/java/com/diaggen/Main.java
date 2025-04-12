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

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
        Parent root = loader.load();


        MainViewController viewController = loader.getController();


        DiagramStore diagramStore = new DiagramStore();
        DialogFactory dialogFactory = DialogFactory.getInstance();
        ExportService exportService = new ExportService(viewController.getDiagramCanvas());


        MainController mainController = new MainController(diagramStore, dialogFactory, exportService);
        mainController.setMainViewController(viewController);
        viewController.setMainController(mainController);


        mainController.initialize();


        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/main.css")).toExternalForm());

        primaryStage.setTitle("DiagGen - Générateur de diagrammes de classe");
        primaryStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/icon.png"))));
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}