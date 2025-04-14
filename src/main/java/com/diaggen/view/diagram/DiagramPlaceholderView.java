package com.diaggen.view.diagram;

import com.diaggen.model.Project;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DiagramPlaceholderView extends VBox {
    private static final Logger LOGGER = Logger.getLogger(DiagramPlaceholderView.class.getName());

    private final Label titleLabel;
    private final Label messageLabel;
    private final Button createDiagramButton;

    public DiagramPlaceholderView() {
        setAlignment(Pos.CENTER);
        setSpacing(30);
        setStyle("-fx-background-color: #f5f5f5;");

        // Créer et configurer l'ImageView
        ImageView iconView = new ImageView();
        iconView.setFitHeight(120);
        iconView.setFitWidth(120);
        iconView.setPreserveRatio(true);

        // Charger l'image
        try {
            Image image = new Image(getClass().getResourceAsStream("/images/diagram-icon.png"));
            iconView.setImage(image);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Impossible de charger l'image d'icône", e);
        }

        titleLabel = new Label("Aucun diagramme sélectionné");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 26));

        messageLabel = new Label("Sélectionnez un diagramme dans l'explorateur ou créez-en un nouveau");
        messageLabel.setFont(Font.font("System", 16));
        messageLabel.setTextAlignment(TextAlignment.CENTER);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(500);

        createDiagramButton = new Button("Créer un nouveau diagramme");
        createDiagramButton.getStyleClass().add("action-button");
        createDiagramButton.setStyle("-fx-font-size: 14px; -fx-padding: 10 20;");

        VBox spacer1 = new VBox();
        VBox spacer2 = new VBox();
        VBox.setVgrow(spacer1, Priority.ALWAYS);
        VBox.setVgrow(spacer2, Priority.ALWAYS);

        getChildren().addAll(spacer1, iconView, titleLabel, messageLabel, createDiagramButton, spacer2);
    }

    public void updateForProject(Project activeProject) {
        if (activeProject != null) {
            titleLabel.setText("Aucun diagramme sélectionné");
            messageLabel.setText("Projet: " + activeProject.getName() + "\nSélectionnez un diagramme dans l'explorateur ou créez-en un nouveau");
            createDiagramButton.setVisible(true);
        } else {
            titleLabel.setText("Aucun projet actif");
            messageLabel.setText("Créez un nouveau projet ou ouvrez-en un existant pour commencer");
            createDiagramButton.setVisible(false);
        }
    }

    public Button getCreateDiagramButton() {
        return createDiagramButton;
    }
}