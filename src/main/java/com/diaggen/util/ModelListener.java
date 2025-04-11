package com.diaggen.util;

import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramClass;
import com.diaggen.model.DiagramRelation;
import javafx.collections.ListChangeListener;

/**
 * Classe utilitaire pour écouter les changements dans le modèle
 */
public class ModelListener {

    /**
     * Interface pour écouter les changements de classes
     */
    public interface ClassChangeListener {
        /**
         * Appelé lorsqu'une classe est ajoutée
         * @param diagram Le diagramme contenant la classe
         * @param diagramClass La classe ajoutée
         */
        void onClassAdded(ClassDiagram diagram, DiagramClass diagramClass);

        /**
         * Appelé lorsqu'une classe est supprimée
         * @param diagram Le diagramme contenant la classe
         * @param diagramClass La classe supprimée
         */
        void onClassRemoved(ClassDiagram diagram, DiagramClass diagramClass);
    }

    /**
     * Interface pour écouter les changements de relations
     */
    public interface RelationChangeListener {
        /**
         * Appelé lorsqu'une relation est ajoutée
         * @param diagram Le diagramme contenant la relation
         * @param relation La relation ajoutée
         */
        void onRelationAdded(ClassDiagram diagram, DiagramRelation relation);

        /**
         * Appelé lorsqu'une relation est supprimée
         * @param diagram Le diagramme contenant la relation
         * @param relation La relation supprimée
         */
        void onRelationRemoved(ClassDiagram diagram, DiagramRelation relation);
    }

    /**
     * Attache un écouteur de changements de classes à un diagramme
     * @param diagram Le diagramme à observer
     * @param listener L'écouteur à attacher
     */
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

    /**
     * Attache un écouteur de changements de relations à un diagramme
     * @param diagram Le diagramme à observer
     * @param listener L'écouteur à attacher
     */
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
}