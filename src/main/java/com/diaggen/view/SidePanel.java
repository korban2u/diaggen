package com.diaggen.view;

import com.diaggen.model.DiagramClass;
import com.diaggen.model.DiagramRelation;
import com.diaggen.view.editor.ClassEditorPanel;
import com.diaggen.view.editor.RelationEditorPanel;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class SidePanel extends BorderPane {

    private final VBox contentBox;
    private final ScrollPane scrollPane;
    private final TranslateTransition showTransition;
    private final TranslateTransition hideTransition;
    private boolean isVisible = false;

    // Éditeurs
    private final ClassEditorPanel classEditorPanel;
    private final RelationEditorPanel relationEditorPanel;

    public SidePanel() {
        setPrefWidth(300);
        setMaxWidth(300);
        setMinWidth(300);

        // Style du panneau
        getStyleClass().add("side-panel");
        setStyle("-fx-background-color: #f8f8f8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, -5, 0);");

        // Barre de titre avec bouton fermer
        HBox titleBar = createTitleBar();
        setTop(titleBar);

        // Créer les éditeurs spécialisés
        classEditorPanel = new ClassEditorPanel();
        relationEditorPanel = new RelationEditorPanel();

        // Zone de contenu
        contentBox = new VBox(10);
        contentBox.setPadding(new Insets(15));

        // ScrollPane pour permettre le défilement si nécessaire
        scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent;");
        setCenter(scrollPane);

        // Configuration des animations
        showTransition = new TranslateTransition(Duration.millis(250), this);
        showTransition.setFromX(300);
        showTransition.setToX(0);

        hideTransition = new TranslateTransition(Duration.millis(250), this);
        hideTransition.setFromX(0);
        hideTransition.setToX(300);
        hideTransition.setOnFinished(e -> setVisible(false));

        // Initialement caché
        setTranslateX(300);
        setVisible(false);
    }

    private HBox createTitleBar() {
        HBox titleBar = new HBox();
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(10, 15, 10, 15));
        titleBar.setStyle("-fx-background-color: #e8e8e8; -fx-border-color: #d0d0d0; -fx-border-width: 0 0 1 0;");

        Label titleLabel = new Label("Éditeur de propriétés");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Button closeButton = new Button("×");
        closeButton.setStyle("-fx-font-size: 16px; -fx-background-color: transparent; -fx-padding: 0 5 0 5;");
        closeButton.setOnAction(e -> hide());

        titleBar.getChildren().addAll(titleLabel, closeButton);
        return titleBar;
    }

    public void showForClass(DiagramClass diagramClass) {
        contentBox.getChildren().clear();

        // Configurer l'éditeur de classe
        classEditorPanel.setDiagramClass(diagramClass);
        contentBox.getChildren().add(classEditorPanel);

        show();
    }

    public void showForRelation(DiagramRelation relation) {
        contentBox.getChildren().clear();

        // Configurer l'éditeur de relation
        relationEditorPanel.setRelation(relation);
        contentBox.getChildren().add(relationEditorPanel);

        show();
    }

    public void show() {
        if (!isVisible) {
            setVisible(true);
            showTransition.play();
            isVisible = true;
        }
    }

    public void hide() {
        if (isVisible) {
            hideTransition.play();
            isVisible = false;
        }
    }

    public void toggle() {
        if (isVisible) {
            hide();
        } else {
            show();
        }
    }

    public boolean isShown() {
        return isVisible;
    }

    public void addContent(Node content) {
        contentBox.getChildren().add(content);
    }

    public void clearContent() {
        contentBox.getChildren().clear();
    }
}