package com.diaggen.view.diagram.canvas;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point2D;

DiagramCanvaspublic class ViewportTransform {
    private final DoubleProperty scale = new SimpleDoubleProperty(1.0);
    private final DoubleProperty translateX = new SimpleDoubleProperty(0.0);
    private final DoubleProperty translateY = new SimpleDoubleProperty(0.0);
DiagramCanvas    private double minScale = 0.1;
    private double maxScale = 5.0;

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
        this.translateX.set(translateX);
    }

    public DoubleProperty translateXProperty() {
        return translateX;
    }

    public double getTranslateY() {
        return translateY.get();
    }

    public void setTranslateY(double translateY) {
        this.translateY.set(translateY);
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

    DiagramCanvas    public Point2D transformPoint(Point2D viewportPoint) {
        return new Point2D(
                (viewportPoint.getX() - getTranslateX()) / getScale(),
                (viewportPoint.getY() - getTranslateY()) / getScale()
        );
    }

    DiagramCanvas    public Point2D inverseTransformPoint(Point2D contentPoint) {
        return new Point2D(
                contentPoint.getX() * getScale() + getTranslateX(),
                contentPoint.getY() * getScale() + getTranslateY()
        );
    }
}