package com.diaggen.view.diagram.canvas;

import com.diaggen.model.DiagramRelation;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

public class RelationLine extends Pane {

    private final DiagramRelation relation;
    private final ClassNode sourceNode;
    private final ClassNode targetNode;
    private final ArrowRenderer arrowRenderer;
    private final Label sourceMultiplicityLabel;
    private final Label targetMultiplicityLabel;
    private final Label relationLabel;

    private static final double BASE_CLICK_TOLERANCE = 5.0;
    private double currentZoomScale = 1.0;

    private static final double MULTIPLICITY_OFFSET = 20.0;

    public RelationLine(DiagramRelation relation, ClassNode sourceNode, ClassNode targetNode) {
        this.relation = relation;
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;

        getStyleClass().add("relation-line");

        arrowRenderer = new ArrowRenderer();

        sourceMultiplicityLabel = new Label();
        sourceMultiplicityLabel.setStyle("-fx-font-size: 11; -fx-background-color: rgba(255,255,255,0.7); -fx-padding: 1 3 1 3;");
        sourceMultiplicityLabel.getStyleClass().add("multiplicity-label");

        targetMultiplicityLabel = new Label();
        targetMultiplicityLabel.setStyle("-fx-font-size: 11; -fx-background-color: rgba(255,255,255,0.7); -fx-padding: 1 3 1 3;");
        targetMultiplicityLabel.getStyleClass().add("multiplicity-label");

        relationLabel = new Label();
        relationLabel.setStyle("-fx-font-size: 11;");
        relationLabel.getStyleClass().add("relation-name-label");

        getChildren().addAll(arrowRenderer.getArrowGroup(), sourceMultiplicityLabel,
                targetMultiplicityLabel, relationLabel);

        updateTooltip();

        setPickOnBounds(false);

        bindModelToView();

        Platform.runLater(this::update);
    }
    public void setZoomScale(double scale) {
        this.currentZoomScale = scale;
        arrowRenderer.setZoomScale(scale);
    }

    private void bindModelToView() {
        relation.sourceMultiplicityProperty().addListener((obs, oldVal, newVal) -> {
            sourceMultiplicityLabel.setText(newVal);
            update();
        });

        relation.targetMultiplicityProperty().addListener((obs, oldVal, newVal) -> {
            targetMultiplicityLabel.setText(newVal);
            update();
        });

        relation.labelProperty().addListener((obs, oldVal, newVal) -> {
            relationLabel.setText(newVal);
            update();
        });
    }

    private void updateTooltip() {
        String tooltipText = relation.getRelationType().getDisplayName() + "\n" +
                "De: " + relation.getSourceClass().getName() +
                (relation.getSourceMultiplicity().isEmpty() ? "" : " [" + relation.getSourceMultiplicity() + "]") + "\n" +
                "Vers: " + relation.getTargetClass().getName() +
                (relation.getTargetMultiplicity().isEmpty() ? "" : " [" + relation.getTargetMultiplicity() + "]");

        if (!relation.getLabel().isEmpty()) {
            tooltipText += "\nLabel: " + relation.getLabel();
        }

        Tooltip tooltip = new Tooltip(tooltipText);
        Tooltip.install(this, tooltip);
    }

    public DiagramRelation getRelation() {
        return relation;
    }

    public void update() {
        if (sourceNode.getWidth() <= 0 || sourceNode.getHeight() <= 0 ||
                targetNode.getWidth() <= 0 || targetNode.getHeight() <= 0) {
            Platform.runLater(this::update);
            return;
        }

        Point2D sourceCenter = new Point2D(
                sourceNode.getLayoutX() + sourceNode.getWidth() / 2,
                sourceNode.getLayoutY() + sourceNode.getHeight() / 2);

        Point2D targetCenter = new Point2D(
                targetNode.getLayoutX() + targetNode.getWidth() / 2,
                targetNode.getLayoutY() + targetNode.getHeight() / 2);

        Point2D sourcePoint = sourceNode.getConnectionPoint(targetCenter);
        Point2D targetPoint = targetNode.getConnectionPoint(sourceCenter);

        arrowRenderer.updateArrow(sourcePoint, targetPoint, relation.getRelationType());

        updateLabels(sourcePoint, targetPoint);

        updateTooltip();
    }

    private void updateLabels(Point2D sourcePoint, Point2D targetPoint) {
        sourceMultiplicityLabel.setText(relation.getSourceMultiplicity());
        targetMultiplicityLabel.setText(relation.getTargetMultiplicity());
        relationLabel.setText(relation.getLabel());

        sourceMultiplicityLabel.setVisible(!relation.getSourceMultiplicity().isEmpty());
        targetMultiplicityLabel.setVisible(!relation.getTargetMultiplicity().isEmpty());
        relationLabel.setVisible(!relation.getLabel().isEmpty());

        double dx = targetPoint.getX() - sourcePoint.getX();
        double dy = targetPoint.getY() - sourcePoint.getY();
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length == 0) return;

        double ux = dx / length;
        double uy = dy / length;

        double perpX = -uy;
        double perpY = ux;

        if (!relation.getSourceMultiplicity().isEmpty()) {
            double offsetX = perpX * MULTIPLICITY_OFFSET;
            double offsetY = perpY * MULTIPLICITY_OFFSET;

            double inwardFactor = 0.1;
            double inwardX = ux * (length * inwardFactor);
            double inwardY = uy * (length * inwardFactor);

            sourceMultiplicityLabel.setTranslateX(sourcePoint.getX() + offsetX + inwardX);
            sourceMultiplicityLabel.setTranslateY(sourcePoint.getY() + offsetY + inwardY);

            sourceMultiplicityLabel.applyCss();
            sourceMultiplicityLabel.layout();
            sourceMultiplicityLabel.setTranslateX(
                    sourceMultiplicityLabel.getTranslateX() - sourceMultiplicityLabel.getWidth() / 2);
            sourceMultiplicityLabel.setTranslateY(
                    sourceMultiplicityLabel.getTranslateY() - sourceMultiplicityLabel.getHeight() / 2);
        }

        if (!relation.getTargetMultiplicity().isEmpty()) {
            double offsetX = perpX * MULTIPLICITY_OFFSET;
            double offsetY = perpY * MULTIPLICITY_OFFSET;

            double inwardFactor = 0.1;
            double inwardX = -ux * (length * inwardFactor);
            double inwardY = -uy * (length * inwardFactor);

            targetMultiplicityLabel.setTranslateX(targetPoint.getX() + offsetX + inwardX);
            targetMultiplicityLabel.setTranslateY(targetPoint.getY() + offsetY + inwardY);

            targetMultiplicityLabel.applyCss();
            targetMultiplicityLabel.layout();
            targetMultiplicityLabel.setTranslateX(
                    targetMultiplicityLabel.getTranslateX() - targetMultiplicityLabel.getWidth() / 2);
            targetMultiplicityLabel.setTranslateY(
                    targetMultiplicityLabel.getTranslateY() - targetMultiplicityLabel.getHeight() / 2);
        }

        if (!relation.getLabel().isEmpty()) {
            double midX = (sourcePoint.getX() + targetPoint.getX()) / 2;
            double midY = (sourcePoint.getY() + targetPoint.getY()) / 2;

            double labelOffset = 20.0;
            midX += perpX * labelOffset;
            midY += perpY * labelOffset;

            relationLabel.applyCss();
            relationLabel.layout();

            relationLabel.setTranslateX(midX - relationLabel.getWidth() / 2);
            relationLabel.setTranslateY(midY - relationLabel.getHeight() / 2);
        }
    }

    public void setSelected(boolean selected) {
        if (selected) {
            if (!getStyleClass().contains("selected")) {
                getStyleClass().add("selected");
            }
            arrowRenderer.setSelected(true);
            toFront();
        } else {
            getStyleClass().remove("selected");
            arrowRenderer.setSelected(false);
        }
    }

    public boolean isNearLine(double x, double y) {
        double adjustedTolerance = BASE_CLICK_TOLERANCE / Math.sqrt(currentZoomScale);

        Point2D sourceCenter = new Point2D(
                sourceNode.getLayoutX() + sourceNode.getWidth() / 2,
                sourceNode.getLayoutY() + sourceNode.getHeight() / 2);

        Point2D targetCenter = new Point2D(
                targetNode.getLayoutX() + targetNode.getWidth() / 2,
                targetNode.getLayoutY() + targetNode.getHeight() / 2);

        Point2D sourcePoint = sourceNode.getConnectionPoint(targetCenter);
        Point2D targetPoint = targetNode.getConnectionPoint(sourceCenter);

        return distanceToLine(x, y, sourcePoint.getX(), sourcePoint.getY(),
                targetPoint.getX(), targetPoint.getY()) <= adjustedTolerance;
    }

    private double distanceToLine(double x, double y, double x1, double y1, double x2, double y2) {
        double lineLengthSquared = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);

        if (lineLengthSquared == 0) {
            return Math.sqrt((x - x1) * (x - x1) + (y - y1) * (y - y1));
        }

        double t = ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1)) / lineLengthSquared;

        if (t < 0) {
            return Math.sqrt((x - x1) * (x - x1) + (y - y1) * (y - y1));
        }
        if (t > 1) {
            return Math.sqrt((x - x2) * (x - x2) + (y - y2) * (y - y2));
        }

        double projX = x1 + t * (x2 - x1);
        double projY = y1 + t * (y2 - y1);

        return Math.sqrt((x - projX) * (x - projX) + (y - projY) * (y - projY));
    }
}