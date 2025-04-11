package com.diaggen.view.diagram.canvas;

import com.diaggen.model.RelationType;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

public class ArrowHeadStrategy {

    public static void configureArrowHead(Polygon arrowHead, RelationType relationType) {
        arrowHead.getPoints().clear();

        switch (relationType) {
            case INHERITANCE:
            case IMPLEMENTATION:
                // Triangle ouvert pour héritage et implémentation
                arrowHead.getPoints().addAll(0.0, 0.0, -15.0, -7.0, -15.0, 7.0);
                arrowHead.setFill(Color.WHITE);
                arrowHead.setStroke(Color.BLACK);
                arrowHead.setStrokeWidth(1.5);
                break;

            case ASSOCIATION:
            case DEPENDENCY:
                // Flèche simple pour association et dépendance
                arrowHead.getPoints().addAll(0.0, 0.0, -10.0, -5.0, -10.0, 5.0);
                arrowHead.setFill(Color.BLACK);
                arrowHead.setStroke(Color.BLACK);
                break;

            case AGGREGATION:
                // Diamant vide pour agrégation
                arrowHead.getPoints().addAll(0.0, 0.0, -10.0, -6.0, -20.0, 0.0, -10.0, 6.0);
                arrowHead.setFill(Color.WHITE);
                arrowHead.setStroke(Color.BLACK);
                arrowHead.setStrokeWidth(1.5);
                break;

            case COMPOSITION:
                // Diamant plein pour composition
                arrowHead.getPoints().addAll(0.0, 0.0, -10.0, -6.0, -20.0, 0.0, -10.0, 6.0);
                arrowHead.setFill(Color.BLACK);
                arrowHead.setStroke(Color.BLACK);
                arrowHead.setStrokeWidth(1.5);
                break;
        }
    }

    public static double getArrowHeadLength(RelationType relationType) {
        switch (relationType) {
            case AGGREGATION:
            case COMPOSITION:
                return 20.0; // Diamant plus grand
            case INHERITANCE:
            case IMPLEMENTATION:
                return 15.0; // Triangle ouvert
            default:
                return 10.0; // Flèche simple
        }
    }

    public static Point2D adjustLineEndPoint(
            Point2D startPoint,
            Point2D endPoint,
            RelationType relationType) {

        // Calculer le vecteur directeur
        double dx = endPoint.getX() - startPoint.getX();
        double dy = endPoint.getY() - startPoint.getY();

        // Normaliser le vecteur
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length == 0) return endPoint;

        double unitDx = dx / length;
        double unitDy = dy / length;

        // Déterminer le décalage en fonction du type de relation
        double offset = getArrowHeadLength(relationType);

        // Calculer le point ajusté
        double adjustedX = endPoint.getX() - (offset * unitDx);
        double adjustedY = endPoint.getY() - (offset * unitDy);

        return new Point2D(adjustedX, adjustedY);
    }
}