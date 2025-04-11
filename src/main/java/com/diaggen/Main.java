package com.diaggen;

import com.diaggen.controller.MainController;
import com.diaggen.model.DiagramStore;
import com.diaggen.view.ViewFactory;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        DiagramStore diagramStore = new DiagramStore();
        ViewFactory viewFactory = ViewFactory.getInstance();
        MainController mainController = new MainController(diagramStore, viewFactory);

        Scene scene = new Scene(mainController.getMainView(), 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

        primaryStage.setTitle("DiagGen - Générateur de diagrammes de classe");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/icon.png")));
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
