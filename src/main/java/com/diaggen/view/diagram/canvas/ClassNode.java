package com.diaggen.view.diagram.canvas;

import com.diaggen.model.ClassType;
import com.diaggen.model.DiagramClass;
import com.diaggen.model.Member;
import com.diaggen.model.Method;
import com.diaggen.model.Parameter;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
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

    // Interface fonctionnelle pour notifier des changements de position
    private Runnable positionChangeListener;

    public ClassNode(DiagramClass diagramClass) {
        this.diagramClass = diagramClass;
        this.content = new ClassNodeContent(diagramClass);

        getStyleClass().add("class-node");
        setStyle("-fx-background-color: white; -fx-border-color: #1a1a1a; -fx-border-width: 1; -fx-border-radius: 5;");

        setPadding(new Insets(1));
        getChildren().add(content);

        // Lier la position aux propriétés du modèle
        layoutXProperty().bindBidirectional(diagramClass.xProperty());
        layoutYProperty().bindBidirectional(diagramClass.yProperty());

        // Observer les changements de taille du contenu
        content.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            setPrefSize(
                    newBounds.getWidth() + PADDING,
                    newBounds.getHeight() + PADDING
            );
        });
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
        // Coordonnées du centre du nœud
        double cx = getLayoutX() + getWidth() / 2;
        double cy = getLayoutY() + getHeight() / 2;

        // Dimensions du nœud
        double width = getWidth();
        double height = getHeight();

        // Vecteur du centre vers la cible
        double dx = target.getX() - cx;
        double dy = target.getY() - cy;

        // Si le vecteur est nul, retourner le centre
        if (dx == 0 && dy == 0) {
            return new Point2D(cx, cy);
        }

        // Angle du vecteur
        double angle = Math.atan2(dy, dx);

        // Calculer l'intersection avec le bord
        double boundaryX, boundaryY;

        // Ajouter une petite marge pour éviter que la ligne touche exactement le bord
        double margin = 1.0;

        // Calculer les distances aux bords selon l'angle
        double distToVertical = Math.abs(width / 2 / Math.cos(angle)) - margin;
        double distToHorizontal = Math.abs(height / 2 / Math.sin(angle)) - margin;

        // Déterminer quelle bordure est touchée en premier
        if (Double.isNaN(distToHorizontal) || (Math.abs(dx) > 0 && Math.abs(distToVertical) < Math.abs(distToHorizontal))) {
            // Intersection avec un bord vertical
            boundaryX = cx + Math.signum(dx) * (width / 2 - margin);
            boundaryY = cy + dy * Math.abs((boundaryX - cx) / dx);
        } else {
            // Intersection avec un bord horizontal
            boundaryY = cy + Math.signum(dy) * (height / 2 - margin);
            boundaryX = cx + dx * Math.abs((boundaryY - cy) / dy);
        }

        return new Point2D(boundaryX, boundaryY);
    }

    public void refresh() {
        content.update();
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

            // Calculer la largeur préférée basée sur le contenu
            double prefWidth = calculatePreferredWidth();

            // Séparateur entre le nom et les attributs
            Line separator1 = new Line(0, 0, prefWidth, 0);
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
            Line separator2 = new Line(0, 0, prefWidth, 0);
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

        private double calculatePreferredWidth() {
            // Helper pour calculer la largeur nécessaire d'un texte
            Text helper = new Text();
            helper.setBoundsType(TextBoundsType.VISUAL);

            // Commencer avec une largeur minimale
            double prefWidth = MIN_WIDTH;

            // Vérifier la largeur du nom de classe
            helper.setFont(TITLE_FONT);
            helper.setText(diagramClass.getName());
            prefWidth = Math.max(prefWidth, helper.getLayoutBounds().getWidth() + 20);

            // Vérifier la largeur du type de classe
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

            // Vérifier les attributs
            for (Member attribute : diagramClass.getAttributes()) {
                String fullText = attribute.getVisibility().getSymbol() + " " +
                        attribute.getName() + " : " + attribute.getType();
                helper.setText(fullText);
                prefWidth = Math.max(prefWidth, helper.getLayoutBounds().getWidth() + 20);
            }

            // Vérifier les méthodes
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