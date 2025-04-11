package com.diaggen.view;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class StatusBar extends HBox {

    private final Label statusLabel;

    public StatusBar() {
        setPadding(new Insets(5));
        setSpacing(10);
        setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #cccccc; -fx-border-width: 1 0 0 0;");

        statusLabel = new Label("Prêt");
        getChildren().add(statusLabel);
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    public String getStatus() {
        return statusLabel.getText();
    }
}