package com.diaggen.view.dialog;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RecentProjectsDialog extends Dialog<String> {

    private final ListView<String> projectsListView;

    public RecentProjectsDialog(Window owner, List<String> recentProjects) {
        setTitle("Projets récents");
        setHeaderText("Sélectionnez un projet récent à ouvrir");

        if (owner != null) {
            initOwner(owner);
        }

        ButtonType openButtonType = new ButtonType("Ouvrir", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(openButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        projectsListView = new ListView<>();
        projectsListView.setCellFactory(param -> new RecentProjectCell());
        projectsListView.getItems().addAll(recentProjects);
        projectsListView.setPrefHeight(300);
        projectsListView.setPrefWidth(500);

        VBox.setVgrow(projectsListView, Priority.ALWAYS);

        grid.add(projectsListView, 0, 0);

        getDialogPane().setContent(grid);

        setResultConverter(dialogButton -> {
            if (dialogButton == openButtonType) {
                return projectsListView.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        projectsListView.getSelectionModel().selectFirst();

        projectsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selectedProject = projectsListView.getSelectionModel().getSelectedItem();
                if (selectedProject != null) {
                    setResult(selectedProject);
                    close();
                }
            }
        });
    }

    private static class RecentProjectCell extends ListCell<String> {
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        @Override
        protected void updateItem(String projectPath, boolean empty) {
            super.updateItem(projectPath, empty);

            if (empty || projectPath == null) {
                setText(null);
                setGraphic(null);
            } else {
                File file = new File(projectPath);
                String fileName = file.getName();
                String projectName = fileName;
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    projectName = fileName.substring(0, dotIndex);
                }
                LocalDateTime lastModified = LocalDateTime.ofInstant(
                        file.lastModified() > 0
                                ? java.time.Instant.ofEpochMilli(file.lastModified())
                                : java.time.Instant.now(),
                        java.time.ZoneId.systemDefault());

                String displayText = projectName + "\n" +
                        "Dernière modification: " + DATE_FORMATTER.format(lastModified) + "\n" +
                        file.getAbsolutePath();

                setText(displayText);
                setGraphic(null);
            }
        }
    }
}