package com.diaggen.view.diagram.canvas;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;


public class NavigationControls extends HBox {
    private final ViewportTransform transform;
    private final NavigationManager navigationManager;
    private final GridRenderer gridRenderer;

    public NavigationControls(ViewportTransform transform, NavigationManager navigationManager, GridRenderer gridRenderer) {
        this.transform = transform;
        this.navigationManager = navigationManager;
        this.gridRenderer = gridRenderer;

        initialize();
    }

    private void initialize() {
        setSpacing(5);
        setPadding(new Insets(5));
        setAlignment(Pos.CENTER_LEFT);
        setBackground(new Background(new BackgroundFill(
                Color.rgb(245, 245, 245, 0.85), new CornerRadii(4), Insets.EMPTY)));
        setBorder(new Border(new BorderStroke(
                Color.rgb(200, 200, 200), BorderStrokeStyle.SOLID, new CornerRadii(4), new BorderWidths(1))));
        Button zoomInButton = createButton("+", "Zoom In");
        zoomInButton.setOnAction(e -> navigationManager.zoomIn());

        Button zoomOutButton = createButton("-", "Zoom Out");
        zoomOutButton.setOnAction(e -> navigationManager.zoomOut());

        Button zoomFitButton = createButton("Fit", "Zoom to Fit");
        zoomFitButton.setOnAction(e -> navigationManager.zoomToFit(null, 50));

        Button zoomResetButton = createButton("1:1", "Reset Zoom");
        zoomResetButton.setOnAction(e -> navigationManager.resetView());
        Label zoomLabel = new Label();
        zoomLabel.textProperty().bind(Bindings.createStringBinding(
                () -> String.format("%.0f%%", transform.getScale() * 100),
                transform.scaleProperty()));
        ToggleButton gridToggle = new ToggleButton("Grid");
        gridToggle.setSelected(true);
        gridToggle.setTooltip(new Tooltip("Show/Hide Grid"));
        gridToggle.setOnAction(e -> gridRenderer.setShowGrid(gridToggle.isSelected()));
        Slider zoomSlider = new Slider(0.1, 5.0, 1.0);
        zoomSlider.setPrefWidth(100);
        zoomSlider.valueProperty().bindBidirectional(transform.scaleProperty());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        getChildren().addAll(
                zoomOutButton,
                zoomLabel,
                zoomInButton,
                new Separator(),
                zoomFitButton,
                zoomResetButton,
                new Separator(),
                gridToggle
        );
        getStyleClass().add("navigation-controls");
    }

    private Button createButton(String text, String tooltipText) {
        Button button = new Button(text);
        button.setTooltip(new Tooltip(tooltipText));
        button.setFocusTraversable(false);
        return button;
    }
}