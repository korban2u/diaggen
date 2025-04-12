package com.diaggen.event;

/**
 * Événement signalant un déplacement de classe.
 */
public class ClassMovedEvent extends DiagramEvent {
    private final String classId;
    private final double oldX;
    private final double oldY;
    private final double newX;
    private final double newY;

    public ClassMovedEvent(String diagramId, String classId, double oldX, double oldY, double newX, double newY) {
        super(diagramId);
        this.classId = classId;
        this.oldX = oldX;
        this.oldY = oldY;
        this.newX = newX;
        this.newY = newY;
    }

    public String getClassId() {
        return classId;
    }

    public double getOldX() {
        return oldX;
    }

    public double getOldY() {
        return oldY;
    }

    public double getNewX() {
        return newX;
    }

    public double getNewY() {
        return newY;
    }
}
