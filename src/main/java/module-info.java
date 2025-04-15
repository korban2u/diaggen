module com.diaggen {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires javafx.graphics;
    requires java.desktop;
    requires com.github.javaparser.core;
    requires java.logging;
    requires org.controlsfx.controls;

    opens com.diaggen to javafx.fxml;
    opens com.diaggen.view.controller to javafx.fxml;
    opens com.diaggen.view.dialog.controller to javafx.fxml;

    exports com.diaggen;
    exports com.diaggen.model;
    exports com.diaggen.controller;
    exports com.diaggen.view.controller;
    exports com.diaggen.view.dialog.controller;
}