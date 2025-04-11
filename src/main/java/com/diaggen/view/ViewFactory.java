package com.diaggen.view;

import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramClass;
import com.diaggen.model.DiagramRelation;
import com.diaggen.view.dialog.ClassEditorDialog;
import com.diaggen.view.dialog.DiagramPropertiesDialog;
import com.diaggen.view.dialog.RelationEditorDialog;
import javafx.collections.ObservableList;

public class ViewFactory {
    private static ViewFactory instance;

    private ViewFactory() {}

    public static ViewFactory getInstance() {
        if (instance == null) {
            instance = new ViewFactory();
        }
        return instance;
    }

    public MainView createMainView() {
        return new MainView();
    }

    public DiagramPropertiesDialog createDiagramPropertiesDialog(ClassDiagram diagram) {
        return new DiagramPropertiesDialog(diagram);
    }

    public ClassEditorDialog createClassEditorDialog(DiagramClass diagramClass) {
        return new ClassEditorDialog(diagramClass);
    }

    public RelationEditorDialog createRelationEditorDialog(DiagramRelation relation,
                                                       ObservableList<DiagramClass> classes) {
        return new RelationEditorDialog(relation, classes);
    }
}


