package com.diaggen.view.diagram.canvas;

import com.diaggen.model.DiagramRelation;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

public class RelationLine extends Pane {

    private final DiagramRelation relation;
    private final ClassNode sourceNode;
    private final ClassNode targetNode;
    private final ArrowRenderer arrowRenderer;
    private final Label sourceMultiplicityLabel;
    private final Label targetMultiplicityLabel;
    private final Label relationLabel;

    public RelationLine(DiagramRelation relation, ClassNode sourceNode, ClassNode targetNode) {
        this.relation = relation;
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;

        // Créer le renderer de flèche
        arrowRenderer = new ArrowRenderer();

        // Créer les étiquettes
        sourceMultiplicityLabel = new Label();
        sourceMultiplicityLabel.setStyle("-fx-font-size: 11;");

        targetMultiplicityLabel = new Label();
        targetMultiplicityLabel.setStyle("-fx-font-size: 11;");

        relationLabel = new Label();
        relationLabel.setStyle("-fx-font-size: 11;");

        getChildren().addAll(arrowRenderer.getArrowGroup(), sourceMultiplicityLabel,
                targetMultiplicityLabel, relationLabel);

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

        // Mettre à jour la flèche
        arrowRenderer.updateArrow(sourcePoint, targetPoint, relation.getRelationType());

        // Mettre à jour les étiquettes
        updateLabels(sourcePoint, targetPoint);
    }

    private void updateLabels(Point2D sourcePoint, Point2D targetPoint) {
        // Calculer le vecteur directeur
        double dx = targetPoint.getX() - sourcePoint.getX();
        double dy = targetPoint.getY() - sourcePoint.getY();
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length == 0) return;

        // Vecteurs unitaires
        double ux = dx / length;
        double uy = dy / length;

        // Vecteur perpendiculaire
        double perpX = -uy;
        double perpY = ux;

        // Décalage pour les étiquettes de multiplicité
        double offset = 15.0;

        // Position pour la multiplicité source
        sourceMultiplicityLabel.setText(relation.getSourceMultiplicity());
        if (!relation.getSourceMultiplicity().isEmpty()) {
            sourceMultiplicityLabel.setTranslateX(sourcePoint.getX() + perpX * offset);
            sourceMultiplicityLabel.setTranslateY(sourcePoint.getY() + perpY * offset);
        }

        // Position pour la multiplicité cible
        targetMultiplicityLabel.setText(relation.getTargetMultiplicity());
        if (!relation.getTargetMultiplicity().isEmpty()) {
            targetMultiplicityLabel.setTranslateX(targetPoint.getX() + perpX * offset);
            targetMultiplicityLabel.setTranslateY(targetPoint.getY() + perpY * offset);
        }

        // Position pour l'étiquette de relation
        relationLabel.setText(relation.getLabel());
        if (!relation.getLabel().isEmpty()) {
            // Point milieu de la ligne
            double midX = (sourcePoint.getX() + targetPoint.getX()) / 2;
            double midY = (sourcePoint.getY() + targetPoint.getY()) / 2;

            // Décalage perpendiculaire
            double labelOffset = 15.0;
            midX += perpX * labelOffset;
            midY += perpY * labelOffset;

            relationLabel.applyCss();
            relationLabel.layout();

            relationLabel.setTranslateX(midX - relationLabel.getWidth() / 2);
            relationLabel.setTranslateY(midY - relationLabel.getHeight() / 2);
        }
    }

    public void setSelected(boolean selected) {
        arrowRenderer.setSelected(selected);
    }
}