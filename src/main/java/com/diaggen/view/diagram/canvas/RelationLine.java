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

    // Zone sensible pour améliorer la détection des clics
    private static final double CLICK_TOLERANCE = 5.0;

    // Décalage pour les multiplicités pour améliorer la visibilité
    private static final double MULTIPLICITY_OFFSET = 20.0;

    public RelationLine(DiagramRelation relation, ClassNode sourceNode, ClassNode targetNode) {
        this.relation = relation;
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;

        // Appliquer les styles CSS
        getStyleClass().add("relation-line");

        // Créer le renderer de flèche
        arrowRenderer = new ArrowRenderer();

        // Créer les étiquettes
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

        // Ajouter une infobulle pour la relation
        updateTooltip();

        // Améliorer la zone de détection des clics
        setPickOnBounds(false); // Ne pas détecter les clics sur la zone rectangulaire entière

        // Lier les propriétés du modèle à la vue
        bindModelToView();

        // Planifier une mise à jour après le rendu complet de la scène
        Platform.runLater(this::update);
    }

    /**
     * Lie les propriétés du modèle à la vue pour les mises à jour automatiques
     */
    private void bindModelToView() {
        // Observer les changements de multiplicité source
        relation.sourceMultiplicityProperty().addListener((obs, oldVal, newVal) -> {
            sourceMultiplicityLabel.setText(newVal);
            update();
        });

        // Observer les changements de multiplicité cible
        relation.targetMultiplicityProperty().addListener((obs, oldVal, newVal) -> {
            targetMultiplicityLabel.setText(newVal);
            update();
        });

        // Observer les changements de label
        relation.labelProperty().addListener((obs, oldVal, newVal) -> {
            relationLabel.setText(newVal);
            update();
        });
    }

    /**
     * Met à jour l'infobulle avec les informations de la relation
     */
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

        // Mettre à jour l'infobulle
        updateTooltip();
    }

    private void updateLabels(Point2D sourcePoint, Point2D targetPoint) {
        // Mettre à jour le contenu des labels depuis le modèle
        sourceMultiplicityLabel.setText(relation.getSourceMultiplicity());
        targetMultiplicityLabel.setText(relation.getTargetMultiplicity());
        relationLabel.setText(relation.getLabel());

        // Ne pas afficher les labels vides
        sourceMultiplicityLabel.setVisible(!relation.getSourceMultiplicity().isEmpty());
        targetMultiplicityLabel.setVisible(!relation.getTargetMultiplicity().isEmpty());
        relationLabel.setVisible(!relation.getLabel().isEmpty());

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

        // Position pour la multiplicité source - améliorée pour meilleure visibilité
        if (!relation.getSourceMultiplicity().isEmpty()) {
            // Positionner un peu éloigné du point de connexion,
            // dans la direction perpendiculaire à la ligne
            double offsetX = perpX * MULTIPLICITY_OFFSET;
            double offsetY = perpY * MULTIPLICITY_OFFSET;

            // Ajustement supplémentaire pour éviter les chevauchements
            // Déplacer un peu vers l'intérieur de la ligne
            double inwardFactor = 0.1; // 10% de la longueur vers l'intérieur
            double inwardX = ux * (length * inwardFactor);
            double inwardY = uy * (length * inwardFactor);

            sourceMultiplicityLabel.setTranslateX(sourcePoint.getX() + offsetX + inwardX);
            sourceMultiplicityLabel.setTranslateY(sourcePoint.getY() + offsetY + inwardY);

            // Centrer le texte par rapport au point
            sourceMultiplicityLabel.applyCss();
            sourceMultiplicityLabel.layout();
            sourceMultiplicityLabel.setTranslateX(
                    sourceMultiplicityLabel.getTranslateX() - sourceMultiplicityLabel.getWidth() / 2);
            sourceMultiplicityLabel.setTranslateY(
                    sourceMultiplicityLabel.getTranslateY() - sourceMultiplicityLabel.getHeight() / 2);
        }

        // Position pour la multiplicité cible - améliorée pour meilleure visibilité
        if (!relation.getTargetMultiplicity().isEmpty()) {
            // Positionner un peu éloigné du point de connexion,
            // dans la direction perpendiculaire à la ligne
            double offsetX = perpX * MULTIPLICITY_OFFSET;
            double offsetY = perpY * MULTIPLICITY_OFFSET;

            // Ajustement supplémentaire pour éviter les chevauchements
            // Déplacer un peu vers l'intérieur de la ligne
            double inwardFactor = 0.1; // 10% de la longueur vers l'intérieur
            double inwardX = -ux * (length * inwardFactor);
            double inwardY = -uy * (length * inwardFactor);

            targetMultiplicityLabel.setTranslateX(targetPoint.getX() + offsetX + inwardX);
            targetMultiplicityLabel.setTranslateY(targetPoint.getY() + offsetY + inwardY);

            // Centrer le texte par rapport au point
            targetMultiplicityLabel.applyCss();
            targetMultiplicityLabel.layout();
            targetMultiplicityLabel.setTranslateX(
                    targetMultiplicityLabel.getTranslateX() - targetMultiplicityLabel.getWidth() / 2);
            targetMultiplicityLabel.setTranslateY(
                    targetMultiplicityLabel.getTranslateY() - targetMultiplicityLabel.getHeight() / 2);
        }

        // Position pour l'étiquette de relation
        if (!relation.getLabel().isEmpty()) {
            // Point milieu de la ligne
            double midX = (sourcePoint.getX() + targetPoint.getX()) / 2;
            double midY = (sourcePoint.getY() + targetPoint.getY()) / 2;

            // Décalage perpendiculaire
            double labelOffset = 20.0; // Augmenté pour meilleure visibilité
            midX += perpX * labelOffset;
            midY += perpY * labelOffset;

            relationLabel.applyCss();
            relationLabel.layout();

            // Positionner le centre du label au point calculé
            relationLabel.setTranslateX(midX - relationLabel.getWidth() / 2);
            relationLabel.setTranslateY(midY - relationLabel.getHeight() / 2);
        }
    }

    public void setSelected(boolean selected) {
        // Utiliser des classes CSS pour la sélection
        if (selected) {
            if (!getStyleClass().contains("selected")) {
                getStyleClass().add("selected");
            }
            arrowRenderer.setSelected(true);
            toFront(); // Amener la relation sélectionnée au premier plan
        } else {
            getStyleClass().remove("selected");
            arrowRenderer.setSelected(false);
        }
    }

    /**
     * Vérifie si le point (x, y) est proche de la ligne de relation
     * @param x Coordonnée x du point
     * @param y Coordonnée y du point
     * @return true si le point est proche de la ligne
     */
    public boolean isNearLine(double x, double y) {
        // Points de connexion des classes
        Point2D sourceCenter = new Point2D(
                sourceNode.getLayoutX() + sourceNode.getWidth() / 2,
                sourceNode.getLayoutY() + sourceNode.getHeight() / 2);

        Point2D targetCenter = new Point2D(
                targetNode.getLayoutX() + targetNode.getWidth() / 2,
                targetNode.getLayoutY() + targetNode.getHeight() / 2);

        Point2D sourcePoint = sourceNode.getConnectionPoint(targetCenter);
        Point2D targetPoint = targetNode.getConnectionPoint(sourceCenter);

        // Calculer la distance du point (x, y) à la ligne définie par sourcePoint et targetPoint
        return distanceToLine(x, y, sourcePoint.getX(), sourcePoint.getY(),
                targetPoint.getX(), targetPoint.getY()) <= CLICK_TOLERANCE;
    }

    /**
     * Calcule la distance d'un point à une ligne
     * @param x X du point
     * @param y Y du point
     * @param x1 X du début de la ligne
     * @param y1 Y du début de la ligne
     * @param x2 X de la fin de la ligne
     * @param y2 Y de la fin de la ligne
     * @return La distance du point à la ligne
     */
    private double distanceToLine(double x, double y, double x1, double y1, double x2, double y2) {
        // Longueur de la ligne au carré
        double lineLengthSquared = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);

        if (lineLengthSquared == 0) {
            // Points identiques, retourner la distance au point
            return Math.sqrt((x - x1) * (x - x1) + (y - y1) * (y - y1));
        }

        // Calculer la projection du point sur la ligne
        double t = ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1)) / lineLengthSquared;

        if (t < 0) {
            // Le point le plus proche est le début de la ligne
            return Math.sqrt((x - x1) * (x - x1) + (y - y1) * (y - y1));
        }
        if (t > 1) {
            // Le point le plus proche est la fin de la ligne
            return Math.sqrt((x - x2) * (x - x2) + (y - y2) * (y - y2));
        }

        // Le point le plus proche est sur la ligne
        double projX = x1 + t * (x2 - x1);
        double projY = y1 + t * (y2 - y1);

        return Math.sqrt((x - projX) * (x - projX) + (y - projY) * (y - projY));
    }
}