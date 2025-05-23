package com.diaggen.view.diagram.canvas;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class NavigationControls extends HBox {
    private final ViewportTransform transform;
    private final NavigationManager navigationManager;
    private final GridRenderer gridRenderer;

    private final ToggleButton gridToggle;
    private final ToggleButton coordinatesToggle;
    private final ToggleButton originMarkerToggle;
    private final Slider zoomSlider;
    private final Label zoomLabel;

    public NavigationControls(ViewportTransform transform, NavigationManager navigationManager, GridRenderer gridRenderer) {
        this.transform = transform;
        this.navigationManager = navigationManager;
        this.gridRenderer = gridRenderer;
        setSpacing(5);
        setPadding(new Insets(5));
        setAlignment(Pos.CENTER_LEFT);
        setBackground(new Background(new BackgroundFill(
                Color.rgb(245, 245, 245, 0.85), new CornerRadii(4), Insets.EMPTY)));
        setBorder(new Border(new BorderStroke(
                Color.rgb(200, 200, 200), BorderStrokeStyle.SOLID, new CornerRadii(4), new BorderWidths(1))));
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(5.0);
        dropShadow.setOffsetX(1.0);
        dropShadow.setOffsetY(1.0);
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.3));
        setEffect(dropShadow);
        Button zoomInButton = createStyledButton("+", "Zoom avant (Ctrl +)");
        zoomInButton.setOnAction(e -> navigationManager.zoomIn());

        Button zoomOutButton = createStyledButton("-", "Zoom arrière (Ctrl -)");
        zoomOutButton.setOnAction(e -> navigationManager.zoomOut());
        Button zoomFitButton = createStyledButton("Ajuster", "Adapter à la vue (Ctrl+F)");
        zoomFitButton.setOnAction(e -> navigationManager.zoomToFit(null, 50));

        Button zoomResetButton = createStyledButton("100%", "Réinitialiser le zoom (Ctrl+0)");
        zoomResetButton.setOnAction(e -> navigationManager.resetView());
        zoomLabel = new Label();
        zoomLabel.textProperty().bind(Bindings.createStringBinding(
                () -> String.format("%.0f%%", transform.getScale() * 100),
                transform.scaleProperty()));
        zoomLabel.setStyle("-fx-padding: 3 8; -fx-background-color: white; -fx-background-radius: 3;" +
                "-fx-border-radius: 3; -fx-border-color: #ddd; -fx-min-width: 45; -fx-alignment: center;");
        zoomSlider = new Slider(0.1, 5.0, 1.0);
        zoomSlider.setPrefWidth(100);
        zoomSlider.getStyleClass().add("zoom-slider");
        zoomSlider.valueProperty().bindBidirectional(transform.scaleProperty());
        gridToggle = new ToggleButton("Grille");
        gridToggle.setSelected(true);
        gridToggle.setTooltip(new Tooltip("Afficher/masquer la grille"));
        gridToggle.setOnAction(e -> gridRenderer.setShowGrid(gridToggle.isSelected()));

        coordinatesToggle = new ToggleButton("Coord.");
        coordinatesToggle.setSelected(true);
        coordinatesToggle.setTooltip(new Tooltip("Afficher/masquer les coordonnées"));
        coordinatesToggle.setOnAction(e -> gridRenderer.setShowCoordinates(coordinatesToggle.isSelected()));

        originMarkerToggle = new ToggleButton("Origine");
        originMarkerToggle.setSelected(true);
        originMarkerToggle.setTooltip(new Tooltip("Afficher/masquer l'origine"));
        originMarkerToggle.setOnAction(e -> gridRenderer.setShowOriginMarker(originMarkerToggle.isSelected()));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        getChildren().addAll(
                zoomOutButton,
                zoomLabel,
                zoomInButton,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                zoomFitButton,
                zoomResetButton,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                gridToggle,
                coordinatesToggle,
                originMarkerToggle
        );
        setupToggles();
    }

    private Button createStyledButton(String text, String tooltipText) {
        Button button = new Button(text);
        button.setTooltip(new Tooltip(tooltipText));
        button.setFocusTraversable(false);
        button.setStyle("-fx-background-color: white; -fx-background-radius: 3; -fx-border-radius: 3; " +
                "-fx-border-color: #ccc; -fx-font-weight: bold; -fx-padding: 2 8; -fx-min-width: 30; -fx-cursor: hand;");
        button.setOnMouseEntered(e ->
                button.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 3; -fx-border-radius: 3; " +
                        "-fx-border-color: #999; -fx-font-weight: bold; -fx-padding: 2 8; -fx-min-width: 30; -fx-cursor: hand;")
        );

        button.setOnMouseExited(e ->
                button.setStyle("-fx-background-color: white; -fx-background-radius: 3; -fx-border-radius: 3; " +
                        "-fx-border-color: #ccc; -fx-font-weight: bold; -fx-padding: 2 8; -fx-min-width: 30; -fx-cursor: hand;")
        );

        return button;
    }

    private void setupToggles() {
        gridToggle.setStyle("-fx-background-color: white; -fx-background-radius: 3; -fx-border-radius: 3; " +
                "-fx-border-color: #ccc; -fx-padding: 2 8; -fx-cursor: hand;");

        gridToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                gridToggle.setStyle("-fx-background-color: #4a89dc; -fx-text-fill: white; -fx-background-radius: 3; " +
                        "-fx-border-radius: 3; -fx-border-color: #2a69bc; -fx-padding: 2 8; -fx-cursor: hand;");
            } else {
                gridToggle.setStyle("-fx-background-color: white; -fx-background-radius: 3; -fx-border-radius: 3; " +
                        "-fx-border-color: #ccc; -fx-padding: 2 8; -fx-cursor: hand;");
            }
        });
        coordinatesToggle.setStyle("-fx-background-color: white; -fx-background-radius: 3; -fx-border-radius: 3; " +
                "-fx-border-color: #ccc; -fx-padding: 2 8; -fx-cursor: hand;");

        coordinatesToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                coordinatesToggle.setStyle("-fx-background-color: #4a89dc; -fx-text-fill: white; -fx-background-radius: 3; " +
                        "-fx-border-radius: 3; -fx-border-color: #2a69bc; -fx-padding: 2 8; -fx-cursor: hand;");
            } else {
                coordinatesToggle.setStyle("-fx-background-color: white; -fx-background-radius: 3; -fx-border-radius: 3; " +
                        "-fx-border-color: #ccc; -fx-padding: 2 8; -fx-cursor: hand;");
            }
        });

        originMarkerToggle.setStyle("-fx-background-color: white; -fx-background-radius: 3; -fx-border-radius: 3; " +
                "-fx-border-color: #ccc; -fx-padding: 2 8; -fx-cursor: hand;");

        originMarkerToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                originMarkerToggle.setStyle("-fx-background-color: #4a89dc; -fx-text-fill: white; -fx-background-radius: 3; " +
                        "-fx-border-radius: 3; -fx-border-color: #2a69bc; -fx-padding: 2 8; -fx-cursor: hand;");
            } else {
                originMarkerToggle.setStyle("-fx-background-color: white; -fx-background-radius: 3; -fx-border-radius: 3; " +
                        "-fx-border-color: #ccc; -fx-padding: 2 8; -fx-cursor: hand;");
            }
        });
        gridToggle.setSelected(true);
        coordinatesToggle.setSelected(true);
        originMarkerToggle.setSelected(true);
    }
}