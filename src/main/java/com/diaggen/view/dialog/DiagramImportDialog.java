package com.diaggen.view.dialog;

import com.diaggen.model.ClassDiagram;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.List;
import java.util.stream.Collectors;

public class DiagramImportDialog extends Dialog<List<ClassDiagram>> {

    private final ListView<ClassDiagram> diagramListView;

    public DiagramImportDialog(Window owner, List<ClassDiagram> availableDiagrams) {
        setTitle("Importer des diagrammes");
        setHeaderText("Sélectionnez les diagrammes à importer");

        if (owner != null) {
            initOwner(owner);
        }

        ButtonType importButtonType = new ButtonType("Importer", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(importButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        diagramListView = new ListView<>();
        diagramListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        diagramListView.setCellFactory(lv -> new ListCell<ClassDiagram>() {
            @Override
            protected void updateItem(ClassDiagram item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });

        diagramListView.setItems(FXCollections.observableArrayList(availableDiagrams));
        diagramListView.setPrefHeight(300);

        diagramListView.getSelectionModel().selectAll();

        VBox vbox = new VBox(10);
        vbox.getChildren().addAll(
                new Label("Diagrammes disponibles :"),
                diagramListView,
                new Label("Astuce : Utilisez Ctrl ou Shift pour sélectionner plusieurs diagrammes.")
        );

        grid.add(vbox, 0, 0);

        getDialogPane().setContent(grid);

        ButtonType selectAllType = new ButtonType("Tout sélectionner", ButtonBar.ButtonData.LEFT);
        ButtonType deselectAllType = new ButtonType("Tout désélectionner", ButtonBar.ButtonData.LEFT);

        getDialogPane().getButtonTypes().add(0, selectAllType);
        getDialogPane().getButtonTypes().add(1, deselectAllType);

        Button selectAllButton = (Button) getDialogPane().lookupButton(selectAllType);
        selectAllButton.setOnAction(e -> diagramListView.getSelectionModel().selectAll());

        Button deselectAllButton = (Button) getDialogPane().lookupButton(deselectAllType);
        deselectAllButton.setOnAction(e -> diagramListView.getSelectionModel().clearSelection());

        setResultConverter(dialogButton -> {
            if (dialogButton == importButtonType) {
                return diagramListView.getSelectionModel().getSelectedItems()
                        .stream()
                        .collect(Collectors.toList());
            }
            return null;
        });
    }
}