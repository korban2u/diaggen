package com.diaggen.util;

import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramClass;
import com.diaggen.model.DiagramRelation;
import javafx.collections.ListChangeListener;

public class ModelListener {

    public static void attachClassListener(ClassDiagram diagram, ClassChangeListener listener) {
        diagram.getClasses().addListener((ListChangeListener<DiagramClass>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (DiagramClass diagramClass : change.getAddedSubList()) {
                        listener.onClassAdded(diagram, diagramClass);
                    }
                }
                if (change.wasRemoved()) {
                    for (DiagramClass diagramClass : change.getRemoved()) {
                        listener.onClassRemoved(diagram, diagramClass);
                    }
                }
            }
        });
    }

    public static void attachRelationListener(ClassDiagram diagram, RelationChangeListener listener) {
        diagram.getRelations().addListener((ListChangeListener<DiagramRelation>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (DiagramRelation relation : change.getAddedSubList()) {
                        listener.onRelationAdded(diagram, relation);
                    }
                }
                if (change.wasRemoved()) {
                    for (DiagramRelation relation : change.getRemoved()) {
                        listener.onRelationRemoved(diagram, relation);
                    }
                }
            }
        });
    }

    public interface ClassChangeListener {
        void onClassAdded(ClassDiagram diagram, DiagramClass diagramClass);

        void onClassRemoved(ClassDiagram diagram, DiagramClass diagramClass);
    }

    public interface RelationChangeListener {
        void onRelationAdded(ClassDiagram diagram, DiagramRelation relation);

        void onRelationRemoved(ClassDiagram diagram, DiagramRelation relation);
    }
}