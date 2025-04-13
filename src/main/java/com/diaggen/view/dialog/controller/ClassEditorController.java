package com.diaggen.view.dialog.controller;

import com.diaggen.model.ClassType;
import com.diaggen.model.DiagramClass;
import com.diaggen.model.Member;
import com.diaggen.model.Method;
import com.diaggen.view.dialog.DialogFactory;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.util.Callback;
import javafx.util.StringConverter;

public class ClassEditorController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField packageField;

    @FXML
    private ComboBox<ClassType> typeComboBox;

    @FXML
    private ListView<Member> attributesListView;

    @FXML
    private ListView<Method> methodsListView;

    @FXML
    private Button addAttributeButton;

    @FXML
    private Button editAttributeButton;

    @FXML
    private Button removeAttributeButton;

    @FXML
    private Button addMethodButton;

    @FXML
    private Button editMethodButton;

    @FXML
    private Button removeMethodButton;

    private DiagramClass diagramClass;
    private Dialog<DiagramClass> dialog;

    @FXML
    public void initialize() {

        typeComboBox.setItems(FXCollections.observableArrayList(ClassType.values()));
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

        attributesListView.setCellFactory(param -> new AttributeListCell());

        methodsListView.setCellFactory(param -> new MethodListCell());
    }

    public void setDialog(Dialog<DiagramClass> dialog, DiagramClass diagramClass) {
        this.dialog = dialog;
        this.diagramClass = diagramClass;

        dialog.setTitle(diagramClass == null ? "Ajouter une classe" : "Modifier une classe");
        dialog.setHeaderText(diagramClass == null ? "Créer une nouvelle classe" : "Modifier la classe " + diagramClass.getName());

        if (diagramClass != null) {
            nameField.setText(diagramClass.getName());
            packageField.setText(diagramClass.getPackageName());
            typeComboBox.getSelectionModel().select(diagramClass.getClassType());
            attributesListView.setItems(diagramClass.getAttributes());
            methodsListView.setItems(diagramClass.getMethods());
        } else {

            typeComboBox.getSelectionModel().select(ClassType.CLASS);
            attributesListView.setItems(FXCollections.observableArrayList());
            methodsListView.setItems(FXCollections.observableArrayList());
        }

        dialog.setResultConverter(createResultConverter());
    }

    @FXML
    public void handleAddAttribute() {
        DialogFactory dialogFactory = DialogFactory.getInstance();
        Dialog<Member> dialog = dialogFactory.createAttributeEditorDialog(null);

        dialog.showAndWait().ifPresent(attribute -> {
            attributesListView.getItems().add(attribute);
        });
    }

    @FXML
    public void handleEditAttribute() {
        Member selectedAttribute = attributesListView.getSelectionModel().getSelectedItem();
        if (selectedAttribute != null) {
            DialogFactory dialogFactory = DialogFactory.getInstance();
            Dialog<Member> dialog = dialogFactory.createAttributeEditorDialog(selectedAttribute);

            dialog.showAndWait();
            attributesListView.refresh();
        }
    }

    @FXML
    public void handleRemoveAttribute() {
        Member selectedAttribute = attributesListView.getSelectionModel().getSelectedItem();
        if (selectedAttribute != null) {
            attributesListView.getItems().remove(selectedAttribute);
        }
    }

    @FXML
    public void handleAddMethod() {
        DialogFactory dialogFactory = DialogFactory.getInstance();
        Dialog<Method> dialog = dialogFactory.createMethodEditorDialog(null);

        dialog.showAndWait().ifPresent(method -> {
            methodsListView.getItems().add(method);
        });
    }

    @FXML
    public void handleEditMethod() {
        Method selectedMethod = methodsListView.getSelectionModel().getSelectedItem();
        if (selectedMethod != null) {
            DialogFactory dialogFactory = DialogFactory.getInstance();
            Dialog<Method> dialog = dialogFactory.createMethodEditorDialog(selectedMethod);

            dialog.showAndWait();
            methodsListView.refresh();
        }
    }

    @FXML
    public void handleRemoveMethod() {
        Method selectedMethod = methodsListView.getSelectionModel().getSelectedItem();
        if (selectedMethod != null) {
            methodsListView.getItems().remove(selectedMethod);
        }
    }

    private Callback<ButtonType, DiagramClass> createResultConverter() {
        return buttonType -> {
            if (buttonType == ButtonType.OK) {
                ClassType selectedType = typeComboBox.getSelectionModel().getSelectedItem();

                if (diagramClass == null) {

                    DiagramClass resultClass = new DiagramClass(
                            nameField.getText(),
                            packageField.getText(),
                            selectedType);

                    for (Member attribute : attributesListView.getItems()) {
                        resultClass.addAttribute(attribute);
                    }

                    for (Method method : methodsListView.getItems()) {
                        resultClass.addMethod(method);
                    }

                    return resultClass;
                } else {

                    diagramClass.setName(nameField.getText());
                    diagramClass.setPackageName(packageField.getText());
                    diagramClass.setClassType(selectedType);
                    return diagramClass;
                }
            }
            return null;
        };
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
                    for (com.diaggen.model.Parameter param : method.getParameters()) {
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