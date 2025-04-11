package com.diaggen.view.diagram.canvas;

import com.diaggen.model.RelationType;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

public class ArrowRenderer {

    private final Group arrowGroup = new Group();
    private final Line line = new Line();
    private final Polygon arrowHead = new Polygon();

    public ArrowRenderer() {
        arrowGroup.getChildren().addAll(line, arrowHead);
        line.setStrokeWidth(1.8);  // Ligne légèrement plus épaisse
    }

    public Group getArrowGroup() {
        return arrowGroup;
    }

    public Line getLine() {
        return line;
    }

    public Polygon getArrowHead() {
        return arrowHead;
    }

    public void updateArrow(Point2D start, Point2D end, RelationType relationType) {
        // Définir le style de ligne selon le type de relation
        line.getStrokeDashArray().clear();
        if (relationType == RelationType.IMPLEMENTATION ||
                relationType == RelationType.DEPENDENCY) {
            line.getStrokeDashArray().addAll(10.0, 5.0);
        }

        // Calculer le vecteur directeur
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length == 0) {
            // Éviter la division par zéro
            return;
        }

        // Vecteur unitaire
        double ux = dx / length;
        double uy = dy / length;

        // Dessiner la ligne
        line.setStartX(start.getX());
        line.setStartY(start.getY());

        // Configurer la tête de flèche selon le type de relation
        arrowHead.getPoints().clear();
        switch (relationType) {
            case INHERITANCE:
            case IMPLEMENTATION:
                createInheritanceArrow(end, ux, uy);
                break;
            case ASSOCIATION:
            case DEPENDENCY:
                createAssociationArrow(end, ux, uy);
                break;
            case AGGREGATION:
                createAggregationArrow(end, ux, uy);
                break;
            case COMPOSITION:
                createCompositionArrow(end, ux, uy);
                break;
        }

        // Déterminer où la ligne doit s'arrêter
        double arrowLength = getArrowLength(relationType);
        Point2D lineEnd = new Point2D(
                end.getX() - ux * arrowLength,
                end.getY() - uy * arrowLength
        );

        line.setEndX(lineEnd.getX());
        line.setEndY(lineEnd.getY());
    }

    private void createInheritanceArrow(Point2D end, double ux, double uy) {
        // Vecteur perpendiculaire
        double perpX = -uy;
        double perpY = ux;

        // Taille de la flèche augmentée
        double arrowSize = 14.0;  // Était 10.0

        // Calculer les points du triangle ouvert
        // Le point de pointe est exactement à la position 'end'
        double baseX = end.getX() - ux * arrowSize;
        double baseY = end.getY() - uy * arrowSize;

        double leftX = baseX + perpX * arrowSize;
        double leftY = baseY + perpY * arrowSize;

        double rightX = baseX - perpX * arrowSize;
        double rightY = baseY - perpY * arrowSize;

        // Créer le triangle
        arrowHead.getPoints().addAll(
                end.getX(), end.getY(),  // Pointe
                leftX, leftY,            // Coin gauche
                baseX, baseY,            // Milieu de la base
                rightX, rightY,          // Coin droit
                end.getX(), end.getY()   // Retour à la pointe pour fermer
        );

        arrowHead.setFill(Color.WHITE);
        arrowHead.setStroke(Color.BLACK);
        arrowHead.setStrokeWidth(1.8);
    }

    private void createAssociationArrow(Point2D end, double ux, double uy) {
        // Vecteur perpendiculaire
        double perpX = -uy;
        double perpY = ux;

        // Taille de la flèche augmentée
        double arrowSize = 12.0;   // Était 8.0
        double arrowWidth = 6.0;   // Plus large pour une meilleure visibilité

        // Calculer les points de la flèche simple
        double leftX = end.getX() - ux * arrowSize + perpX * arrowWidth;
        double leftY = end.getY() - uy * arrowSize + perpY * arrowWidth;

        double rightX = end.getX() - ux * arrowSize - perpX * arrowWidth;
        double rightY = end.getY() - uy * arrowSize - perpY * arrowWidth;

        // Créer la flèche
        arrowHead.getPoints().addAll(
                end.getX(), end.getY(),  // Pointe
                leftX, leftY,            // Coin gauche
                rightX, rightY           // Coin droit
        );

        arrowHead.setFill(Color.BLACK);
        arrowHead.setStroke(Color.BLACK);
    }

    private void createAggregationArrow(Point2D end, double ux, double uy) {
        createDiamondArrow(end, ux, uy, Color.WHITE);
    }

    private void createCompositionArrow(Point2D end, double ux, double uy) {
        createDiamondArrow(end, ux, uy, Color.BLACK);
    }

    private void createDiamondArrow(Point2D end, double ux, double uy, Color fillColor) {
        // Vecteur perpendiculaire
        double perpX = -uy;
        double perpY = ux;

        // Taille du diamant augmentée
        double diamondLength = 16.0;  // Était 12.0
        double diamondWidth = 10.0;   // Était 8.0

        // Calculer les points du diamant
        // Le point de pointe est exactement à la position 'end'
        double midX = end.getX() - ux * diamondLength;
        double midY = end.getY() - uy * diamondLength;

        double backX = midX - ux * diamondLength;
        double backY = midY - uy * diamondLength;

        double leftX = midX + perpX * diamondWidth;
        double leftY = midY + perpY * diamondWidth;

        double rightX = midX - perpX * diamondWidth;
        double rightY = midY - perpY * diamondWidth;

        // Créer le diamant
        arrowHead.getPoints().addAll(
                end.getX(), end.getY(),  // Pointe
                leftX, leftY,            // Coin gauche
                backX, backY,            // Arrière
                rightX, rightY,          // Coin droit
                end.getX(), end.getY()   // Retour à la pointe pour fermer
        );

        arrowHead.setFill(fillColor);
        arrowHead.setStroke(Color.BLACK);
        arrowHead.setStrokeWidth(1.8);
    }

    private double getArrowLength(RelationType relationType) {
        switch (relationType) {
            case AGGREGATION:
            case COMPOSITION:
                return 32.0;  // Était 24.0 (longueur totale du diamant)
            case INHERITANCE:
            case IMPLEMENTATION:
                return 14.0;  // Était 10.0 (longueur du triangle)
            default:
                return 0.0;   // Pour les flèches simples, la ligne va jusqu'à la base de la flèche
        }
    }

    public void setSelected(boolean selected) {
        Color lineColor = selected ? Color.BLUE : Color.BLACK;
        line.setStroke(lineColor);
        arrowHead.setStroke(lineColor);

        if (arrowHead.getFill() != Color.WHITE) {
            arrowHead.setFill(selected ? Color.BLUE : Color.BLACK);
        }
    }
}