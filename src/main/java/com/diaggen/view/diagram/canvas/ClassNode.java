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

        // Appliquer les styles CSS
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

        // Ajouter des écouteurs pour les changements du modèle
        bindModelToView();
    }

        private void bindModelToView() {
        // Observer les changements de nom
        diagramClass.nameProperty().addListener((obs, oldVal, newVal) -> refresh());

        // Observer les changements de package
        diagramClass.packageNameProperty().addListener((obs, oldVal, newVal) -> refresh());

        // Observer les changements de type de classe (maintenant une propriété observable)
        diagramClass.classTypeProperty().addListener((obs, oldVal, newVal) -> refresh());

        // Observer les changements dans la liste des attributs
        diagramClass.getAttributes().addListener((ListChangeListener<Member>) change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved() || change.wasUpdated()) {
                    refresh();
                    break;
                }
            }
        });

        // Observer les changements dans la liste des méthodes
        diagramClass.getMethods().addListener((ListChangeListener<Method>) change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved() || change.wasUpdated()) {
                    refresh();
                    break;
                }
            }
        });

        // Observer les changements des attributs individuels
        for (Member member : diagramClass.getAttributes()) {
            member.nameProperty().addListener((obs, oldVal, newVal) -> refresh());
            member.typeProperty().addListener((obs, oldVal, newVal) -> refresh());
        }

        // Observer les changements des méthodes individuelles
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
        // Utiliser des classes CSS pour la sélection au lieu de styles en ligne
        if (selected) {
            // S'assurer que la classe selected est ajoutée uniquement si elle n'existe pas déjà
            if (!getStyleClass().contains("selected")) {
                getStyleClass().add("selected");
            }
            toFront(); // Amener l'élément sélectionné au premier plan
        } else {
            // Supprimer la classe selected
            getStyleClass().remove("selected");
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
        // Mettre à jour les écouteurs pour les nouveaux éléments
        updateModelListeners();

        // Rafraîchir le contenu
        content.update();

        // Forcer une nouvelle disposition
        content.applyCss();
        content.layout();

        // Calculer et mettre à jour la taille
        double width = Math.max(MIN_WIDTH, content.getLayoutBounds().getWidth() + PADDING * 2);
        double height = content.getLayoutBounds().getHeight() + PADDING * 2;
        setPrefSize(width, height);

        // Utiliser requestLayout pour s'assurer que le composant se redessine correctement
        requestLayout();

        // Notifier d'éventuels changements de position
        if (positionChangeListener != null) {
            Platform.runLater(positionChangeListener::run);
        }
    }

        private void updateModelListeners() {
        // Observer les changements des attributs individuels
        for (Member member : diagramClass.getAttributes()) {
            // Vérifier si les écouteurs sont déjà ajoutés pour éviter les doublons
            member.nameProperty().removeListener(observable -> refresh());
            member.typeProperty().removeListener(observable -> refresh());

            member.nameProperty().addListener((obs, oldVal, newVal) -> refresh());
            member.typeProperty().addListener((obs, oldVal, newVal) -> refresh());
        }

        // Observer les changements des méthodes individuelles
        for (Method method : diagramClass.getMethods()) {
            // Vérifier si les écouteurs sont déjà ajoutés pour éviter les doublons
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
            separator1.getStyleClass().add("separator");
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
            separator2.getStyleClass().add("separator");
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

            // Forcer une mise à jour de la disposition
            applyCss();
            layout();
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