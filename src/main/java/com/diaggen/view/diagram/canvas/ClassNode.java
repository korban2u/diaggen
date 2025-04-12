package com.diaggen.view.diagram.canvas;

import com.diaggen.model.ClassType;
import com.diaggen.model.DiagramClass;
import com.diaggen.model.Member;
import com.diaggen.model.Method;
import com.diaggen.model.Parameter;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;

public class ClassNode extends Region {

    private final DiagramClass diagramClass;
    private final ClassNodeContent content;
    private static final double MIN_WIDTH = 150;
    private static final double PADDING = 10;

    private Runnable positionChangeListener;

    public ClassNode(DiagramClass diagramClass) {
        this.diagramClass = diagramClass;
        this.content = new ClassNodeContent(diagramClass);

        getStyleClass().add("class-node");
        setStyle("-fx-background-color: white; -fx-border-color: #1a1a1a; -fx-border-width: 1; -fx-border-radius: 5;");

        setPadding(new Insets(1));
        getChildren().add(content);

        layoutXProperty().bindBidirectional(diagramClass.xProperty());
        layoutYProperty().bindBidirectional(diagramClass.yProperty());

        content.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            setPrefSize(
                    newBounds.getWidth() + PADDING,
                    newBounds.getHeight() + PADDING
            );
        });

        bindModelToView();
    }

        private void bindModelToView() {

        diagramClass.nameProperty().addListener((obs, oldVal, newVal) -> refresh());

        diagramClass.packageNameProperty().addListener((obs, oldVal, newVal) -> refresh());

        diagramClass.classTypeProperty().addListener((obs, oldVal, newVal) -> refresh());

        diagramClass.getAttributes().addListener((ListChangeListener<Member>) change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved() || change.wasUpdated()) {
                    refresh();
                    break;
                }
            }
        });

        diagramClass.getMethods().addListener((ListChangeListener<Method>) change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved() || change.wasUpdated()) {
                    refresh();
                    break;
                }
            }
        });

        for (Member member : diagramClass.getAttributes()) {
            member.nameProperty().addListener((obs, oldVal, newVal) -> refresh());
            member.typeProperty().addListener((obs, oldVal, newVal) -> refresh());
        }

        for (Method method : diagramClass.getMethods()) {
            method.nameProperty().addListener((obs, oldVal, newVal) -> refresh());
            method.returnTypeProperty().addListener((obs, oldVal, newVal) -> refresh());
            method.getParameters().addListener((ListChangeListener<Parameter>) change -> refresh());
        }
    }

    public DiagramClass getDiagramClass() {
        return diagramClass;
    }

        public void setSelected(boolean selected) {

        if (selected) {

            if (!getStyleClass().contains("selected")) {
                getStyleClass().add("selected");
            }
            toFront(); // Amener l'élément sélectionné au premier plan
        } else {

            getStyleClass().remove("selected");
        }
    }

    public void setPositionChangeListener(Runnable listener) {
        this.positionChangeListener = listener;

        layoutXProperty().addListener((obs, oldVal, newVal) -> {
            if (positionChangeListener != null) {
                positionChangeListener.run();
            }
        });

        layoutYProperty().addListener((obs, oldVal, newVal) -> {
            if (positionChangeListener != null) {
                positionChangeListener.run();
            }
        });
    }

    public Point2D getConnectionPoint(Point2D target) {

        double cx = getLayoutX() + getWidth() / 2;
        double cy = getLayoutY() + getHeight() / 2;

        double width = getWidth();
        double height = getHeight();

        if (width <= 0) width = MIN_WIDTH;
        if (height <= 0) height = MIN_WIDTH;

        double dx = target.getX() - cx;
        double dy = target.getY() - cy;

        if (dx == 0 && dy == 0) {
            return new Point2D(cx, cy);
        }

        double angle = Math.atan2(dy, dx);

        double boundaryX, boundaryY;

        double margin = 2.0;

        double distToVertical = Math.abs(width / 2 / Math.cos(angle)) - margin;
        double distToHorizontal = Math.abs(height / 2 / Math.sin(angle)) - margin;

        if (Double.isNaN(distToHorizontal) || (Math.abs(dx) > 0 && Math.abs(distToVertical) < Math.abs(distToHorizontal))) {

            boundaryX = cx + Math.signum(dx) * (width / 2 - margin);
            boundaryY = cy + dy * Math.abs((boundaryX - cx) / dx);
        } else {

            boundaryY = cy + Math.signum(dy) * (height / 2 - margin);
            boundaryX = cx + dx * Math.abs((boundaryY - cy) / dy);
        }

        return new Point2D(boundaryX, boundaryY);
    }

    public void refresh() {

        updateModelListeners();

        content.update();

        content.applyCss();
        content.layout();

        double width = Math.max(MIN_WIDTH, content.getLayoutBounds().getWidth() + PADDING * 2);
        double height = content.getLayoutBounds().getHeight() + PADDING * 2;

        double oldWidth = getWidth();
        double oldHeight = getHeight();

        setPrefSize(width, height);
        resize(width, height);

        if (oldWidth != width || oldHeight != height) {
            setVisible(false);
            setVisible(true);
        }

        requestLayout();
        applyCss();
        layout();

        if (getParent() != null && getParent() instanceof Pane) {
            Pane parent = (Pane) getParent();
            double parentWidth = parent.getWidth();
            double parentHeight = parent.getHeight();

            if (parentWidth > 0 && parentHeight > 0) {
                double margin = 20;
                double newX = Math.max(margin, Math.min(parentWidth - width - margin, getLayoutX()));
                double newY = Math.max(margin, Math.min(parentHeight - height - margin, getLayoutY()));

                if (newX != getLayoutX() || newY != getLayoutY()) {
                    setLayoutX(newX);
                    setLayoutY(newY);

                    diagramClass.setX(newX);
                    diagramClass.setY(newY);
                }
            }
        }

        if (positionChangeListener != null) {
            Platform.runLater(positionChangeListener);
        }
    }


        private void updateModelListeners() {

        for (Member member : diagramClass.getAttributes()) {

            member.nameProperty().removeListener(observable -> refresh());
            member.typeProperty().removeListener(observable -> refresh());

            member.nameProperty().addListener((obs, oldVal, newVal) -> refresh());
            member.typeProperty().addListener((obs, oldVal, newVal) -> refresh());
        }

        for (Method method : diagramClass.getMethods()) {

            method.nameProperty().removeListener(observable -> refresh());
            method.returnTypeProperty().removeListener(observable -> refresh());

            method.nameProperty().addListener((obs, oldVal, newVal) -> refresh());
            method.returnTypeProperty().addListener((obs, oldVal, newVal) -> refresh());
            method.getParameters().addListener((ListChangeListener<Parameter>) change -> refresh());
        }
    }

    private static class ClassNodeContent extends VBox {

        private final DiagramClass diagramClass;
        private static final Font DEFAULT_FONT = Font.font("System", 12);
        private static final Font TITLE_FONT = Font.font("System", 14);

        public ClassNodeContent(DiagramClass diagramClass) {
            this.diagramClass = diagramClass;

            getStyleClass().add("class-node-content");
            setPadding(new Insets(PADDING));
            setSpacing(5);

            update();
        }

        public void update() {
            getChildren().clear();

            if (diagramClass.getClassType() != null) {
                switch (diagramClass.getClassType()) {
                    case INTERFACE:
                        Text typeText = new Text("«interface»");
                        typeText.setStyle("-fx-font-style: italic; -fx-font-size: 12;");
                        getChildren().add(typeText);
                        break;
                    case ABSTRACT_CLASS:
                        Text abstractText = new Text("«abstract»");
                        abstractText.setStyle("-fx-font-style: italic; -fx-font-size: 12;");
                        getChildren().add(abstractText);
                        break;
                    case ENUM:
                        Text enumText = new Text("«enumeration»");
                        enumText.setStyle("-fx-font-style: italic; -fx-font-size: 12;");
                        getChildren().add(enumText);
                        break;
                    default:
                        break;
                }
            }

            Text nameText = new Text(diagramClass.getName());
            nameText.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
            getChildren().add(nameText);

            double prefWidth = calculatePreferredWidth();

            Line separator1 = new Line(0, 0, prefWidth, 0);
            separator1.getStyleClass().add("separator");
            getChildren().add(separator1);

            for (Member attribute : diagramClass.getAttributes()) {
                Text attrText = new Text(
                        attribute.getVisibility().getSymbol() + " " +
                                attribute.getName() + " : " + attribute.getType());
                attrText.setStyle("-fx-font-size: 12;");
                getChildren().add(attrText);
            }

            Line separator2 = new Line(0, 0, prefWidth, 0);
            separator2.getStyleClass().add("separator");
            getChildren().add(separator2);

            for (Method method : diagramClass.getMethods()) {
                StringBuilder methodText = new StringBuilder();
                methodText.append(method.getVisibility().getSymbol()).append(" ");

                if (method.isStatic()) {
                    methodText.append("static ");
                }

                if (method.isAbstract()) {
                    methodText.append("abstract ");
                }

                methodText.append(method.getName()).append("(");

                boolean first = true;
                for (Parameter param : method.getParameters()) {
                    if (!first) {
                        methodText.append(", ");
                    }
                    methodText.append(param.getName()).append(" : ").append(param.getType());
                    first = false;
                }

                methodText.append(") : ").append(method.getReturnType());

                Text methText = new Text(methodText.toString());
                methText.setStyle("-fx-font-size: 12;");
                getChildren().add(methText);
            }

            applyCss();
            layout();
        }

        private double calculatePreferredWidth() {

            Text helper = new Text();
            helper.setBoundsType(TextBoundsType.VISUAL);

            double prefWidth = MIN_WIDTH;

            helper.setFont(TITLE_FONT);
            helper.setText(diagramClass.getName());
            prefWidth = Math.max(prefWidth, helper.getLayoutBounds().getWidth() + 20);

            helper.setFont(DEFAULT_FONT);
            if (diagramClass.getClassType() != null) {
                switch (diagramClass.getClassType()) {
                    case INTERFACE:
                        helper.setText("«interface»");
                        prefWidth = Math.max(prefWidth, helper.getLayoutBounds().getWidth() + 20);
                        break;
                    case ABSTRACT_CLASS:
                        helper.setText("«abstract»");
                        prefWidth = Math.max(prefWidth, helper.getLayoutBounds().getWidth() + 20);
                        break;
                    case ENUM:
                        helper.setText("«enumeration»");
                        prefWidth = Math.max(prefWidth, helper.getLayoutBounds().getWidth() + 20);
                        break;
                    default:
                        break;
                }
            }

            for (Member attribute : diagramClass.getAttributes()) {
                String fullText = attribute.getVisibility().getSymbol() + " " +
                        attribute.getName() + " : " + attribute.getType();
                helper.setText(fullText);
                prefWidth = Math.max(prefWidth, helper.getLayoutBounds().getWidth() + 20);
            }

            for (Method method : diagramClass.getMethods()) {
                StringBuilder methodText = new StringBuilder();
                methodText.append(method.getVisibility().getSymbol()).append(" ");

                if (method.isStatic()) {
                    methodText.append("static ");
                }

                if (method.isAbstract()) {
                    methodText.append("abstract ");
                }

                methodText.append(method.getName()).append("(");

                boolean first = true;
                for (Parameter param : method.getParameters()) {
                    if (!first) {
                        methodText.append(", ");
                    }
                    methodText.append(param.getName()).append(" : ").append(param.getType());
                    first = false;
                }

                methodText.append(") : ").append(method.getReturnType());

                helper.setText(methodText.toString());
                prefWidth = Math.max(prefWidth, helper.getLayoutBounds().getWidth() + 20);
            }

            return prefWidth;
        }
    }
}