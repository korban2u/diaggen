package com.diaggen.view.diagram.canvas;

import com.diaggen.model.ClassType;
import com.diaggen.model.DiagramClass;
import com.diaggen.model.Member;
import com.diaggen.model.Method;
import com.diaggen.model.Parameter;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;


public class ClassNode extends Region {

    private final DiagramClass diagramClass;
    private final ClassNodeContent content;

    // Interface fonctionnelle pour notifier des changements de position
    private Runnable positionChangeListener;

    public ClassNode(DiagramClass diagramClass) {
        this.diagramClass = diagramClass;
        this.content = new ClassNodeContent(diagramClass);

        getStyleClass().add("class-node");
        setStyle("-fx-background-color: white; -fx-border-color: #1a1a1a; -fx-border-width: 1; -fx-border-radius: 5;");

        setPadding(new Insets(1));
        getChildren().add(content);

        setPrefSize(200, 200);
        setMinSize(100, 80);

        // Lier la taille au contenu
        prefWidthProperty().bind(Bindings.max(200, content.prefWidthProperty().add(2)));
        prefHeightProperty().bind(Bindings.max(100, content.prefHeightProperty().add(2)));

        // Lier la position aux propriétés du modèle
        layoutXProperty().bindBidirectional(diagramClass.xProperty());
        layoutYProperty().bindBidirectional(diagramClass.yProperty());
    }

    public DiagramClass getDiagramClass() {
        return diagramClass;
    }

    public void setSelected(boolean selected) {
        if (selected) {
            setStyle("-fx-background-color: white; -fx-border-color: #0077cc; -fx-border-width: 2; -fx-border-radius: 5;");
        } else {
            setStyle("-fx-background-color: white; -fx-border-color: #1a1a1a; -fx-border-width: 1; -fx-border-radius: 5;");
        }
    }

    public void setPositionChangeListener(Runnable listener) {
        this.positionChangeListener = listener;

        // Ajouter des écouteurs sur les propriétés de position
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

        double dx = target.getX() - cx;
        double dy = target.getY() - cy;

        double absDx = Math.abs(dx);
        double absDy = Math.abs(dy);


        if (absDx * height > absDy * width) {

            double x = cx + (dx > 0 ? width / 2 : -width / 2);
            double y = cy + dy * width / (2 * absDx);
            return new Point2D(x, y);
        } else {

            double x = cx + dx * height / (2 * absDy);
            double y = cy + (dy > 0 ? height / 2 : -height / 2);
            return new Point2D(x, y);
        }
    }

    public void refresh() {
        content.update();
    }

    private static class ClassNodeContent extends VBox {

        private final DiagramClass diagramClass;

        public ClassNodeContent(DiagramClass diagramClass) {
            this.diagramClass = diagramClass;

            getStyleClass().add("class-node-content");
            setPadding(new Insets(10));
            setSpacing(2);

            update();
        }

        public void update() {
            getChildren().clear();

            // En-tête (type et nom de la classe)
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

            // Séparateur entre le nom et les attributs
            Line separator1 = new Line(0, 0, 180, 0);
            getChildren().add(separator1);

            // Attributs
            for (Member attribute : diagramClass.getAttributes()) {
                Text attrText = new Text(
                        attribute.getVisibility().getSymbol() + " " +
                                attribute.getName() + " : " + attribute.getType());
                attrText.setStyle("-fx-font-size: 12;");
                getChildren().add(attrText);
            }

            // Séparateur entre les attributs et les méthodes
            Line separator2 = new Line(0, 0, 180, 0);
            getChildren().add(separator2);

            // Méthodes
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
        }
    }
}