package com.diaggen.view.diagram.canvas;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point2D;

public class ViewportTransform {
    private final DoubleProperty scale = new SimpleDoubleProperty(1.0);
    private final DoubleProperty translateX = new SimpleDoubleProperty(0.0);
    private final DoubleProperty translateY = new SimpleDoubleProperty(0.0);
    private double minScale = 0.1;
    private double maxScale = 10.0;  // Augmenté pour permettre un zoom plus important

    // Limites de translation (virtuellement illimitées)
    private double minTranslateX = -100000;
    private double maxTranslateX = 100000;
    private double minTranslateY = -100000;
    private double maxTranslateY = 100000;

    public ViewportTransform() {
    }

    public ViewportTransform(double scale, double translateX, double translateY) {
        setScale(scale);
        setTranslateX(translateX);
        setTranslateY(translateY);
    }

    public double getScale() {
        return scale.get();
    }

    public void setScale(double scale) {
        this.scale.set(Math.max(minScale, Math.min(maxScale, scale)));
    }

    public DoubleProperty scaleProperty() {
        return scale;
    }

    public double getTranslateX() {
        return translateX.get();
    }

    public void setTranslateX(double translateX) {
        // Vérification des limites, mais avec des limites très larges
        this.translateX.set(Math.max(minTranslateX, Math.min(maxTranslateX, translateX)));
    }

    public DoubleProperty translateXProperty() {
        return translateX;
    }

    public double getTranslateY() {
        return translateY.get();
    }

    public void setTranslateY(double translateY) {
        // Vérification des limites, mais avec des limites très larges
        this.translateY.set(Math.max(minTranslateY, Math.min(maxTranslateY, translateY)));
    }

    public DoubleProperty translateYProperty() {
        return translateY;
    }

    public void setMinScale(double minScale) {
        this.minScale = minScale;
        if (getScale() < minScale) {
            setScale(minScale);
        }
    }

    public void setMaxScale(double maxScale) {
        this.maxScale = maxScale;
        if (getScale() > maxScale) {
            setScale(maxScale);
        }
    }

    public void setTranslateLimits(double minX, double maxX, double minY, double maxY) {
        this.minTranslateX = minX;
        this.maxTranslateX = maxX;
        this.minTranslateY = minY;
        this.maxTranslateY = maxY;

        // S'assurer que les valeurs actuelles sont dans les limites
        setTranslateX(getTranslateX());
        setTranslateY(getTranslateY());
    }

    /**
     * Convertit un point de l'espace d'affichage (viewport) vers l'espace de contenu
     */
    public Point2D transformPoint(Point2D viewportPoint) {
        return new Point2D(
                (viewportPoint.getX() - getTranslateX()) / getScale(),
                (viewportPoint.getY() - getTranslateY()) / getScale()
        );
    }

    /**
     * Convertit un point de l'espace de contenu vers l'espace d'affichage (viewport)
     */
    public Point2D inverseTransformPoint(Point2D contentPoint) {
        return new Point2D(
                contentPoint.getX() * getScale() + getTranslateX(),
                contentPoint.getY() * getScale() + getTranslateY()
        );
    }
}