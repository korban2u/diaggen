package com.diaggen.view.dialog;

import com.diaggen.model.ClassType;
import com.diaggen.model.DiagramClass;
import com.diaggen.model.Member;
import com.diaggen.model.Method;
import com.diaggen.model.Parameter;
import com.diaggen.model.Visibility;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClassEditorDialog extends Dialog<DiagramClass> {

    private final TextField nameField;
    private final TextField packageField;
    private final ComboBox<ClassType> typeComboBox;

    private final ListView<Member> attributesListView;
    private final ListView<Method> methodsListView;

    // Déclaré final pour signifier qu'il ne sera pas réassigné
    private final DiagramClass originalDiagramClass;

    public ClassEditorDialog(DiagramClass diagramClass) {
        this.originalDiagramClass = diagramClass;

        setTitle(diagramClass == null ? "Ajouter une classe" : "Modifier une classe");
        setHeaderText(diagramClass == null ? "Créer une nouvelle classe" : "Modifier la classe " + diagramClass.getName());

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TabPane tabPane = new TabPane();

        // General tab
        Tab generalTab = new Tab("Général");
        generalTab.setClosable(false);

        GridPane generalGrid = new GridPane();
        generalGrid.setHgap(10);
        generalGrid.setVgap(10);
        generalGrid.setPadding(new Insets(20, 20, 10, 10));

        nameField = new TextField(diagramClass != null ? diagramClass.getName() : "");
        packageField = new TextField(diagramClass != null ? diagramClass.getPackageName() : "");

        typeComboBox = new ComboBox<>(FXCollections.observableArrayList(ClassType.values()));
        typeComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ClassType classType) {
                return classType != null ? classType.getDisplayName() : "";
            }

            @Override
            public ClassType fromString(String string) {
                return null;
            }
        });

        if (diagramClass != null) {
            typeComboBox.getSelectionModel().select(diagramClass.getClassType());
        } else {
            typeComboBox.getSelectionModel().select(ClassType.CLASS);
        }

        generalGrid.add(new Label("Nom:"), 0, 0);
        generalGrid.add(nameField, 1, 0);
        generalGrid.add(new Label("Package:"), 0, 1);
        generalGrid.add(packageField, 1, 1);
        generalGrid.add(new Label("Type:"), 0, 2);
        generalGrid.add(typeComboBox, 1, 2);

        generalTab.setContent(generalGrid);

        // Attributes tab
        Tab attributesTab = new Tab("Attributs");
        attributesTab.setClosable(false);

        VBox attributesBox = new VBox(10);
        attributesBox.setPadding(new Insets(10));

        attributesListView = new ListView<>();
        attributesListView.setCellFactory(param -> new AttributeListCell());

        if (diagramClass != null) {
            attributesListView.setItems(diagramClass.getAttributes());
        } else {
            attributesListView.setItems(FXCollections.observableArrayList());
        }

        HBox attributesButtonsBox = new HBox(10);
        javafx.scene.control.Button addAttributeButton = new javafx.scene.control.Button("Ajouter");
        javafx.scene.control.Button editAttributeButton = new javafx.scene.control.Button("Modifier");
        javafx.scene.control.Button removeAttributeButton = new javafx.scene.control.Button("Supprimer");

        addAttributeButton.setOnAction(e -> handleAddAttribute());
        editAttributeButton.setOnAction(e -> handleEditAttribute());
        removeAttributeButton.setOnAction(e -> handleRemoveAttribute());

        attributesButtonsBox.getChildren().addAll(addAttributeButton, editAttributeButton, removeAttributeButton);
        attributesBox.getChildren().addAll(attributesListView, attributesButtonsBox);

        attributesTab.setContent(attributesBox);

        // Methods tab
        Tab methodsTab = new Tab("Méthodes");
        methodsTab.setClosable(false);

        VBox methodsBox = new VBox(10);
        methodsBox.setPadding(new Insets(10));

        methodsListView = new ListView<>();
        methodsListView.setCellFactory(param -> new MethodListCell());

        if (diagramClass != null) {
            methodsListView.setItems(diagramClass.getMethods());
        } else {
            methodsListView.setItems(FXCollections.observableArrayList());
        }

        HBox methodsButtonsBox = new HBox(10);
        javafx.scene.control.Button addMethodButton = new javafx.scene.control.Button("Ajouter");
        javafx.scene.control.Button editMethodButton = new javafx.scene.control.Button("Modifier");
        javafx.scene.control.Button removeMethodButton = new javafx.scene.control.Button("Supprimer");

        addMethodButton.setOnAction(e -> handleAddMethod());
        editMethodButton.setOnAction(e -> handleEditMethod());
        removeMethodButton.setOnAction(e -> handleRemoveMethod());

        methodsButtonsBox.getChildren().addAll(addMethodButton, editMethodButton, removeMethodButton);
        methodsBox.getChildren().addAll(methodsListView, methodsButtonsBox);

        methodsTab.setContent(methodsBox);

        tabPane.getTabs().addAll(generalTab, attributesTab, methodsTab);
        getDialogPane().setContent(tabPane);

        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                ClassType selectedType = typeComboBox.getSelectionModel().getSelectedItem();
                DiagramClass resultClass;

                if (originalDiagramClass == null) {
                    // Créer une nouvelle instance si on est en mode création
                    resultClass = new DiagramClass(
                            nameField.getText(),
                            packageField.getText(),
                            selectedType);

                    // Ajouter les attributs et méthodes à la nouvelle classe
                    for (Member attribute : attributesListView.getItems()) {
                        resultClass.addAttribute(attribute);
                    }

                    for (Method method : methodsListView.getItems()) {
                        resultClass.addMethod(method);
                    }
                } else {
                    // Mettre à jour l'instance existante en mode modification
                    resultClass = originalDiagramClass;
                    resultClass.setName(nameField.getText());
                    resultClass.setPackageName(packageField.getText());
                    resultClass.setClassType(selectedType);
                    // Note: attributs et méthodes sont directement mis à jour via les ObservableLists
                }

                return resultClass;
            }
            return null;
        });
    }

    private void handleAddAttribute() {
        AttributeEditorDialog dialog = new AttributeEditorDialog(null);
        dialog.showAndWait().ifPresent(attributesListView.getItems()::add);
    }

    private void handleEditAttribute() {
        Member selectedAttribute = attributesListView.getSelectionModel().getSelectedItem();
        if (selectedAttribute != null) {
            AttributeEditorDialog dialog = new AttributeEditorDialog(selectedAttribute);
            dialog.showAndWait();
            // Rafraîchir la vue si nécessaire
            attributesListView.refresh();
        }
    }

    private void handleRemoveAttribute() {
        Member selectedAttribute = attributesListView.getSelectionModel().getSelectedItem();
        if (selectedAttribute != null) {
            attributesListView.getItems().remove(selectedAttribute);
        }
    }

    private void handleAddMethod() {
        MethodEditorDialog dialog = new MethodEditorDialog(null);
        dialog.showAndWait().ifPresent(methodsListView.getItems()::add);
    }

    private void handleEditMethod() {
        Method selectedMethod = methodsListView.getSelectionModel().getSelectedItem();
        if (selectedMethod != null) {
            MethodEditorDialog dialog = new MethodEditorDialog(selectedMethod);
            dialog.showAndWait();
            // Rafraîchir la vue si nécessaire
            methodsListView.refresh();
        }
    }

    private void handleRemoveMethod() {
        Method selectedMethod = methodsListView.getSelectionModel().getSelectedItem();
        if (selectedMethod != null) {
            methodsListView.getItems().remove(selectedMethod);
        }
    }

    private static class AttributeListCell extends TextFieldListCell<Member> {
        public AttributeListCell() {
            setConverter(new StringConverter<>() {
                @Override
                public String toString(Member member) {
                    return member != null ?
                            member.getVisibility().getSymbol() + " " +
                                    member.getName() + " : " + member.getType() : "";
                }

                @Override
                public Member fromString(String string) {
                    return null;
                }
            });
        }
    }

    private static class MethodListCell extends TextFieldListCell<Method> {
        public MethodListCell() {
            setConverter(new StringConverter<>() {
                @Override
                public String toString(Method method) {
                    if (method == null) return "";

                    StringBuilder sb = new StringBuilder();
                    sb.append(method.getVisibility().getSymbol()).append(" ");

                    if (method.isStatic()) {
                        sb.append("static ");
                    }

                    if (method.isAbstract()) {
                        sb.append("abstract ");
                    }

                    sb.append(method.getName()).append("(");

                    boolean first = true;
                    for (Parameter param : method.getParameters()) {
                        if (!first) {
                            sb.append(", ");
                        }
                        sb.append(param.getName()).append(" : ").append(param.getType());
                        first = false;
                    }

                    sb.append(") : ").append(method.getReturnType());

                    return sb.toString();
                }

                @Override
                public Method fromString(String string) {
                    return null;
                }
            });
        }
    }
}