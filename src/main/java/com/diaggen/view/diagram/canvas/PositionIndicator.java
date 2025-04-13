package com.diaggen.view.diagram.canvas;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public class PositionIndicator extends HBox {
    private final ViewportTransform transform;
    private final Label positionLabel;
    private final Label scaleLabel;
    private boolean isVisible = true;

    public PositionIndicator(ViewportTransform transform) {
        this.transform = transform;
        setSpacing(10);
        setPadding(new Insets(5, 10, 5, 10));
        setAlignment(Pos.CENTER_LEFT);
        setBackground(new Background(new BackgroundFill(
                Color.rgb(245, 245, 245, 0.85), new CornerRadii(4), Insets.EMPTY)));
        setBorder(new Border(new BorderStroke(
                Color.rgb(200, 200, 200), BorderStrokeStyle.SOLID, new CornerRadii(4), new BorderWidths(1))));
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(3.0);
        dropShadow.setOffsetX(1.0);
        dropShadow.setOffsetY(1.0);
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.3));
        setEffect(dropShadow);
        Label coordsTitle = new Label("Position:");
        coordsTitle.setFont(Font.font("System", FontWeight.BOLD, 12));

        positionLabel = new Label("(0, 0)");
        positionLabel.setFont(Font.font("Monospace", 12));

        Label zoomTitle = new Label("Zoom:");
        zoomTitle.setFont(Font.font("System", FontWeight.BOLD, 12));

        scaleLabel = new Label("100%");
        scaleLabel.setFont(Font.font("Monospace", 12));
        transform.scaleProperty().addListener((obs, oldVal, newVal) -> {
            updateScaleLabel();
        });
        getChildren().addAll(coordsTitle, positionLabel, zoomTitle, scaleLabel);
        updateScaleLabel();
        setupAnimation();
    }

    private void setupAnimation() {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), this);
        fadeIn.setFromValue(0.7);
        fadeIn.setToValue(1.0);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), this);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.7);

        setOpacity(0.7);
        setOnMouseEntered(e -> fadeIn.play());
        setOnMouseExited(e -> fadeOut.play());
    }

    public void updatePosition(MouseEvent event, Pane canvas) {
        if (!isVisible) return;

        Point2D viewportPoint = new Point2D(event.getX(), event.getY());
        Point2D contentPoint = viewportToContent(viewportPoint);

        int x = (int) contentPoint.getX();
        int y = (int) contentPoint.getY();

        positionLabel.setText(String.format("(%d, %d)", x, y));
    }

    public void updateScaleLabel() {
        if (!isVisible) return;

        int zoomPercentage = (int) (transform.getScale() * 100);
        scaleLabel.setText(zoomPercentage + "%");
    }

    private Point2D viewportToContent(Point2D viewportPoint) {
        return new Point2D(
                (viewportPoint.getX() - transform.getTranslateX()) / transform.getScale(),
                (viewportPoint.getY() - transform.getTranslateY()) / transform.getScale()
        );
    }

    public void settVisible(boolean visible) {
        this.isVisible = visible;
        super.setVisible(visible);
    }
}