package com.diaggen.view.diagram.canvas;

import com.diaggen.model.DiagramRelation;
import com.diaggen.model.RelationType;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;


public class RelationLine extends Pane {

    private final DiagramRelation relation;
    private final ClassNode sourceNode;
    private final ClassNode targetNode;
    private final Line line;
    private final Polygon arrowHead;
    private final Label sourceMultiplicityLabel;
    private final Label targetMultiplicityLabel;
    private final Label relationLabel;

    public RelationLine(DiagramRelation relation, ClassNode sourceNode, ClassNode targetNode) {
        this.relation = relation;
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;

        // Créer la ligne
        line = new Line();
        line.setStrokeWidth(1.5);
        line.getStrokeDashArray().clear();

        // Créer la tête de flèche
        arrowHead = new Polygon();
        arrowHead.getPoints().addAll(0.0, 0.0, -10.0, -5.0, -10.0, 5.0);

        // Créer les étiquettes
        sourceMultiplicityLabel = new Label();
        sourceMultiplicityLabel.setStyle("-fx-font-size: 11;");

        targetMultiplicityLabel = new Label();
        targetMultiplicityLabel.setStyle("-fx-font-size: 11;");

        relationLabel = new Label();
        relationLabel.setStyle("-fx-font-size: 11;");

        getChildren().addAll(line, arrowHead, sourceMultiplicityLabel, targetMultiplicityLabel, relationLabel);

        // Définir le style selon le type de relation
        if (relation.getRelationType() == RelationType.IMPLEMENTATION ||
                relation.getRelationType() == RelationType.DEPENDENCY) {
            line.getStrokeDashArray().addAll(10.0, 5.0);
        }

        updateArrowStyle();

        // Planifier une mise à jour après le rendu complet de la scène
        Platform.runLater(this::update);
    }


    public DiagramRelation getRelation() {
        return relation;
    }

    public void update() {
        // S'assurer que les nœuds sont complètement initialisés
        if (sourceNode.getWidth() <= 0 || sourceNode.getHeight() <= 0 ||
                targetNode.getWidth() <= 0 || targetNode.getHeight() <= 0) {
            // Reporter la mise à jour jusqu'à ce que les nœuds soient rendus
            Platform.runLater(this::update);
            return;
        }

        // Calculer les points de connexion
        Point2D sourceCenter = new Point2D(
                sourceNode.getLayoutX() + sourceNode.getWidth() / 2,
                sourceNode.getLayoutY() + sourceNode.getHeight() / 2);

        Point2D targetCenter = new Point2D(
                targetNode.getLayoutX() + targetNode.getWidth() / 2,
                targetNode.getLayoutY() + targetNode.getHeight() / 2);

        Point2D sourcePoint = sourceNode.getConnectionPoint(targetCenter);
        Point2D targetPoint = targetNode.getConnectionPoint(sourceCenter);

        // Mettre à jour la ligne
        line.setStartX(sourcePoint.getX());
        line.setStartY(sourcePoint.getY());
        line.setEndX(targetPoint.getX());
        line.setEndY(targetPoint.getY());

        // Calculer l'angle pour la tête de flèche
        double angle = Math.atan2(
                targetPoint.getY() - sourcePoint.getY(),
                targetPoint.getX() - sourcePoint.getX());

        // Positionner et orienter la tête de flèche
        arrowHead.setTranslateX(targetPoint.getX());
        arrowHead.setTranslateY(targetPoint.getY());
        arrowHead.setRotate(Math.toDegrees(angle));

        // Mettre à jour les étiquettes de multiplicité
        sourceMultiplicityLabel.setText(relation.getSourceMultiplicity());
        sourceMultiplicityLabel.setTranslateX(sourcePoint.getX() + 15);
        sourceMultiplicityLabel.setTranslateY(sourcePoint.getY() - 15);

        targetMultiplicityLabel.setText(relation.getTargetMultiplicity());
        targetMultiplicityLabel.setTranslateX(targetPoint.getX() - 15);
        targetMultiplicityLabel.setTranslateY(targetPoint.getY() - 15);

        // Mettre à jour l'étiquette de la relation
        relationLabel.setText(relation.getLabel());

        // Positionner l'étiquette de la relation au milieu de la ligne
        double midX = (sourcePoint.getX() + targetPoint.getX()) / 2;
        double midY = (sourcePoint.getY() + targetPoint.getY()) / 2;

        relationLabel.applyCss();
        relationLabel.layout();

        relationLabel.setTranslateX(midX - relationLabel.getWidth() / 2);
        relationLabel.setTranslateY(midY - 20);
    }

    private void updateArrowStyle() {
        RelationType type = relation.getRelationType();

        switch (type) {
            case INHERITANCE:
            case IMPLEMENTATION:
                arrowHead.setFill(Color.WHITE);
                arrowHead.setStroke(Color.BLACK);
                arrowHead.setStrokeWidth(1.5);
                break;
            case ASSOCIATION:
                arrowHead.setFill(Color.BLACK);
                arrowHead.setStroke(Color.BLACK);
                break;
            case DEPENDENCY:
                arrowHead.setFill(Color.BLACK);
                arrowHead.setStroke(Color.BLACK);
                break;
            case AGGREGATION:
                arrowHead.setFill(Color.WHITE);
                arrowHead.setStroke(Color.BLACK);
                arrowHead.setStrokeWidth(1.5);
                break;
            case COMPOSITION:
                arrowHead.setFill(Color.BLACK);
                arrowHead.setStroke(Color.BLACK);
                arrowHead.setStrokeWidth(1.5);
                break;
        }
    }

    public void setSelected(boolean selected) {
        if (selected) {
            line.setStroke(Color.BLUE);
            arrowHead.setStroke(Color.BLUE);
        } else {
            line.setStroke(Color.BLACK);
            arrowHead.setStroke(Color.BLACK);
            updateArrowStyle();
        }
    }
}