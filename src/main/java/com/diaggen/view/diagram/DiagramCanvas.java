package com.diaggen.view.diagram;

import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramClass;
import com.diaggen.model.DiagramRelation;
import com.diaggen.model.RelationType;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DiagramCanvas extends Pane {

    private ClassDiagram diagram;
    private DiagramClass selectedClass;
    private DiagramRelation selectedRelation;
    private final Map<String, ClassNode> classNodes = new HashMap<>();
    private final Map<String, RelationLine> relationLines = new HashMap<>();
    private final List<Node> diagramElements = new ArrayList<>();

    private double dragStartX;
    private double dragStartY;
    private Point2D dragStartPoint;

    public DiagramCanvas() {
        getStyleClass().add("diagram-canvas");
        setStyle("-fx-background-color: white;");

        Canvas gridCanvas = new Canvas();
        gridCanvas.widthProperty().bind(widthProperty());
        gridCanvas.heightProperty().bind(heightProperty());
        getChildren().add(gridCanvas);

        widthProperty().addListener((obs, oldVal, newVal) -> drawGrid(gridCanvas));
        heightProperty().addListener((obs, oldVal, newVal) -> drawGrid(gridCanvas));

        drawGrid(gridCanvas);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem addClassItem = new MenuItem("Ajouter une classe");

        contextMenu.getItems().add(addClassItem);

        setOnContextMenuRequested(e -> {
            contextMenu.show(this, e.getScreenX(), e.getScreenY());
        });
    }

    private void drawGrid(Canvas canvas) {
        int width = (int) canvas.getWidth();
        int height = (int) canvas.getHeight();
        int gridSize = 20;

        var gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(0.5);

        for (int x = 0; x < width; x += gridSize) {
            gc.strokeLine(x, 0, x, height);
        }

        for (int y = 0; y < height; y += gridSize) {
            gc.strokeLine(0, y, width, y);
        }
    }

    public void loadDiagram(ClassDiagram diagram) {
        this.diagram = diagram;
        selectedClass = null;
        selectedRelation = null;

        clear();

        for (DiagramClass diagramClass : diagram.getClasses()) {
            createClassNode(diagramClass);
        }

        for (DiagramRelation relation : diagram.getRelations()) {
            createRelationLine(relation);
        }
    }

    public void refresh() {
        if (diagram != null) {
            clear();

            for (DiagramClass diagramClass : diagram.getClasses()) {
                createClassNode(diagramClass);
            }

            for (DiagramRelation relation : diagram.getRelations()) {
                createRelationLine(relation);
            }
        }
    }

    public void clear() {
        getChildren().removeAll(diagramElements);
        diagramElements.clear();
        classNodes.clear();
        relationLines.clear();
        selectedClass = null;
        selectedRelation = null;
    }

    private void createClassNode(DiagramClass diagramClass) {
        ClassNode classNode = new ClassNode(diagramClass);
        classNode.setLayoutX(diagramClass.getX());
        classNode.setLayoutY(diagramClass.getY());

        classNode.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                selectClass(diagramClass);
                dragStartX = e.getSceneX();
                dragStartY = e.getSceneY();
                dragStartPoint = new Point2D(classNode.getLayoutX(), classNode.getLayoutY());
                e.consume();
            }
        });

        classNode.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                double offsetX = e.getSceneX() - dragStartX;
                double offsetY = e.getSceneY() - dragStartY;

                classNode.setLayoutX(dragStartPoint.getX() + offsetX);
                classNode.setLayoutY(dragStartPoint.getY() + offsetY);

                diagramClass.setX(classNode.getLayoutX());
                diagramClass.setY(classNode.getLayoutY());

                updateRelationLines();
                e.consume();
            }
        });

        classNode.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                diagramClass.setX(classNode.getLayoutX());
                diagramClass.setY(classNode.getLayoutY());
            }
        });

        diagramElements.add(classNode);
        getChildren().add(classNode);
        classNodes.put(diagramClass.getId(), classNode);
    }

    private void createRelationLine(DiagramRelation relation) {
        ClassNode sourceNode = classNodes.get(relation.getSourceClass().getId());
        ClassNode targetNode = classNodes.get(relation.getTargetClass().getId());

        if (sourceNode != null && targetNode != null) {
            RelationLine relationLine = new RelationLine(relation, sourceNode, targetNode);

            relationLine.setOnMousePressed(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    selectRelation(relation);
                    e.consume();
                }
            });

            diagramElements.add(relationLine);
            getChildren().add(0, relationLine);
            relationLines.put(relation.getId(), relationLine);
        }
    }

    private void updateRelationLines() {
        for (RelationLine line : relationLines.values()) {
            line.update();
        }
    }

    private void selectClass(DiagramClass diagramClass) {
        if (selectedClass != null) {
            ClassNode previousNode = classNodes.get(selectedClass.getId());
            if (previousNode != null) {
                previousNode.setSelected(false);
            }
        }

        if (selectedRelation != null) {
            RelationLine previousLine = relationLines.get(selectedRelation.getId());
            if (previousLine != null) {
                previousLine.setSelected(false);
            }
            selectedRelation = null;
        }

        selectedClass = diagramClass;
        ClassNode node = classNodes.get(diagramClass.getId());
        if (node != null) {
            node.setSelected(true);
            node.toFront();
        }
    }

    private void selectRelation(DiagramRelation relation) {
        if (selectedClass != null) {
            ClassNode previousNode = classNodes.get(selectedClass.getId());
            if (previousNode != null) {
                previousNode.setSelected(false);
            }
            selectedClass = null;
        }

        if (selectedRelation != null) {
            RelationLine previousLine = relationLines.get(selectedRelation.getId());
            if (previousLine != null) {
                previousLine.setSelected(false);
            }
        }

        selectedRelation = relation;
        RelationLine line = relationLines.get(relation.getId());
        if (line != null) {
            line.setSelected(true);
            line.toFront();
        }
    }

    public DiagramClass getSelectedClass() {
        return selectedClass;
    }

    public DiagramRelation getSelectedRelation() {
        return selectedRelation;
    }

    private class ClassNode extends Region {
        private final DiagramClass diagramClass;
        private final ClassNodeContent content;

        public ClassNode(DiagramClass diagramClass) {
            this.diagramClass = diagramClass;
            this.content = new ClassNodeContent(diagramClass);

            getStyleClass().add("class-node");
            setStyle("-fx-background-color: white; -fx-border-color: #1a1a1a; -fx-border-width: 1; -fx-border-radius: 5;");

            setPadding(new Insets(1));
            getChildren().add(content);

            setPrefSize(200, 200);
            setMinSize(100, 80);

            // Bind size to content
            prefWidthProperty().bind(Bindings.max(200, content.prefWidthProperty().add(2)));
            prefHeightProperty().bind(Bindings.max(100, content.prefHeightProperty().add(2)));
        }

        public void setSelected(boolean selected) {
            if (selected) {
                setStyle("-fx-background-color: white; -fx-border-color: #0077cc; -fx-border-width: 2; -fx-border-radius: 5;");
            } else {
                setStyle("-fx-background-color: white; -fx-border-color: #1a1a1a; -fx-border-width: 1; -fx-border-radius: 5;");
            }
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

            double scaleX = width / (2 * absDx);
            double scaleY = height / (2 * absDy);

            double scale = Math.min(scaleX, scaleY);

            if (scale < 1) {
                dx *= scale;
                dy *= scale;
                return new Point2D(cx + dx, cy + dy);
            } else {
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
        }
    }

    private class RelationLine extends Pane {
        private final DiagramRelation relation;
        private final ClassNode sourceNode;
        private final ClassNode targetNode;
        private final Line line;
        private final Polygon arrowHead;

        public RelationLine(DiagramRelation relation, ClassNode sourceNode, ClassNode targetNode) {
            this.relation = relation;
            this.sourceNode = sourceNode;
            this.targetNode = targetNode;

            line = new Line();
            line.setStrokeWidth(1.5);
            line.getStrokeDashArray().clear();

            arrowHead = new Polygon();
            arrowHead.getPoints().addAll(0.0, 0.0, -10.0, -5.0, -10.0, 5.0);

            getChildren().addAll(line, arrowHead);

            setStyle("-fx-padding: 10;");
            update();

            if (relation.getRelationType() == RelationType.IMPLEMENTATION ||
                relation.getRelationType() == RelationType.DEPENDENCY) {
                line.getStrokeDashArray().addAll(10.0, 5.0);
            }
        }

        public void update() {
            Point2D sourceCenter = new Point2D(
                    sourceNode.getLayoutX() + sourceNode.getWidth() / 2,
                    sourceNode.getLayoutY() + sourceNode.getHeight() / 2);

            Point2D targetCenter = new Point2D(
                    targetNode.getLayoutX() + targetNode.getWidth() / 2,
                    targetNode.getLayoutY() + targetNode.getHeight() / 2);

            Point2D sourcePoint = sourceNode.getConnectionPoint(targetCenter);
            Point2D targetPoint = targetNode.getConnectionPoint(sourceCenter);

            line.setStartX(sourcePoint.getX());
            line.setStartY(sourcePoint.getY());
            line.setEndX(targetPoint.getX());
            line.setEndY(targetPoint.getY());

            double angle = Math.atan2(
                    targetPoint.getY() - sourcePoint.getY(),
                    targetPoint.getX() - sourcePoint.getX());

            arrowHead.setTranslateX(targetPoint.getX());
            arrowHead.setTranslateY(targetPoint.getY());
            arrowHead.setRotate(Math.toDegrees(angle));

            updateArrowStyle();
        }

        private void updateArrowStyle() {
            RelationType type = relation.getRelationType();

            switch (type) {
                case INHERITANCE:
                case IMPLEMENTATION:
                    arrowHead.setFill(Color.WHITE);
                    arrowHead.setStroke(Color.BLACK);
                    arrowHead.setStrokeWidth(1.5);
                    break;
                case ASSOCIATION:
                    arrowHead.setFill(Color.BLACK);
                    arrowHead.setStroke(Color.BLACK);
                    break;
                case DEPENDENCY:
                    arrowHead.setFill(Color.BLACK);
                    arrowHead.setStroke(Color.BLACK);
                    break;
                case AGGREGATION:
                    arrowHead.setFill(Color.WHITE);
                    arrowHead.setStroke(Color.BLACK);
                    arrowHead.setStrokeWidth(1.5);
                    break;
                case COMPOSITION:
                    arrowHead.setFill(Color.BLACK);
                    arrowHead.setStroke(Color.BLACK);
                    arrowHead.setStrokeWidth(1.5);
                    break;
            }
        }

        public void setSelected(boolean selected) {
            if (selected) {
                line.setStroke(Color.BLUE);
                arrowHead.setStroke(Color.BLUE);
            } else {
                line.setStroke(Color.BLACK);
                arrowHead.setStroke(Color.BLACK);
                updateArrowStyle();
            }
        }
    }

    private class ClassNodeContent extends Pane {
        private final DiagramClass diagramClass;

        public ClassNodeContent(DiagramClass diagramClass) {
            this.diagramClass = diagramClass;

            getStyleClass().add("class-node-content");
            setPadding(new Insets(10));

            update();
        }

        private void update() {
            getChildren().clear();

            double padding = 10;
            double y = padding;

            // Class type and name header
            javafx.scene.text.Text typeText = null;
            if (diagramClass.getClassType() != null) {
                switch (diagramClass.getClassType()) {
                    case INTERFACE:
                        typeText = new javafx.scene.text.Text("«interface»");
                        break;
                    case ABSTRACT_CLASS:
                        typeText = new javafx.scene.text.Text("«abstract»");
                        break;
                    case ENUM:
                        typeText = new javafx.scene.text.Text("«enumeration»");
                        break;
                    default:
                        break;
                }
            }

            if (typeText != null) {
                typeText.setStyle("-fx-font-style: italic; -fx-font-size: 12;");
                typeText.setLayoutX(padding);
                typeText.setLayoutY(y + 12);
                getChildren().add(typeText);
                y += 20;
            }

            javafx.scene.text.Text nameText = new javafx.scene.text.Text(diagramClass.getName());
            nameText.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
            nameText.setLayoutX(padding);
            nameText.setLayoutY(y + 14);
            getChildren().add(nameText);
            y += 25;

            // Separator
            javafx.scene.shape.Line separator1 = new javafx.scene.shape.Line();
            separator1.setStartX(0);
            separator1.setEndX(180);
            separator1.setStartY(0);
            separator1.setEndY(0);
            separator1.setLayoutX(padding);
            separator1.setLayoutY(y);
            getChildren().add(separator1);
            y += 10;

            // Attributes
            for (com.diaggen.model.Member attribute : diagramClass.getAttributes()) {
                javafx.scene.text.Text attrText = new javafx.scene.text.Text(
                        attribute.getVisibility().getSymbol() + " " +
                        attribute.getName() + " : " + attribute.getType());
                attrText.setStyle("-fx-font-size: 12;");
                attrText.setLayoutX(padding);
                attrText.setLayoutY(y + 12);
                getChildren().add(attrText);
                y += 20;
            }

            // Separator
            javafx.scene.shape.Line separator2 = new javafx.scene.shape.Line();
            separator2.setStartX(0);
            separator2.setEndX(180);
            separator2.setStartY(0);
            separator2.setEndY(0);
            separator2.setLayoutX(padding);
            separator2.setLayoutY(y);
            getChildren().add(separator2);
            y += 10;

            // Methods
            for (com.diaggen.model.Method method : diagramClass.getMethods()) {
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
                for (com.diaggen.model.Parameter param : method.getParameters()) {
                    if (!first) {
                        methodText.append(", ");
                    }
                    methodText.append(param.getName()).append(" : ").append(param.getType());
                    first = false;
                }

                methodText.append(") : ").append(method.getReturnType());

                javafx.scene.text.Text methText = new javafx.scene.text.Text(methodText.toString());
                methText.setStyle("-fx-font-size: 12;");
                methText.setLayoutX(padding);
                methText.setLayoutY(y + 12);
                getChildren().add(methText);
                y += 20;
            }

            setPrefSize(200, y + padding);
        }
    }
}


