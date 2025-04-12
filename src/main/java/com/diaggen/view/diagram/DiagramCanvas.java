package com.diaggen.view.diagram;

import com.diaggen.event.DiagramChangedEvent;
import com.diaggen.event.ElementSelectedEvent;
import com.diaggen.event.EventBus;
import com.diaggen.event.EventListener;
import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramClass;
import com.diaggen.model.DiagramRelation;
import com.diaggen.view.diagram.canvas.ClassNode;
import com.diaggen.view.diagram.canvas.GridRenderer;
import com.diaggen.view.diagram.canvas.NodeManager;
import com.diaggen.view.diagram.canvas.RelationLine;
import com.diaggen.view.diagram.canvas.RelationManager;
import javafx.application.Platform;
import javafx.geometry.Dimension2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class DiagramCanvas extends Pane {

    private ClassDiagram diagram;
    private final Canvas gridCanvas;
    private final GridRenderer gridRenderer;
    private final NodeManager nodeManager;
    private final RelationManager relationManager;
    private final EventBus eventBus;

    private Runnable onAddClassRequest;
    private Runnable onDeleteRequest;
    private Consumer<DiagramClass> classSelectionListener;
    private Consumer<DiagramRelation> relationSelectionListener;

    public DiagramCanvas() {
        getStyleClass().add("diagram-canvas");
        setStyle("-fx-background-color: white;");

        gridCanvas = new Canvas();
        gridCanvas.widthProperty().bind(widthProperty());
        gridCanvas.heightProperty().bind(heightProperty());
        getChildren().add(gridCanvas);

        gridRenderer = new GridRenderer(gridCanvas, 20);
        nodeManager = new NodeManager(this);
        relationManager = new RelationManager(this, nodeManager);
        eventBus = EventBus.getInstance();

        nodeManager.setRelationManager(relationManager);

        setupContextMenu();
        setupKeyHandlers();
        setupSelectionListeners();
        setupEventBusListeners();

        widthProperty().addListener((obs, oldVal, newVal) -> gridRenderer.drawGrid());
        heightProperty().addListener((obs, oldVal, newVal) -> gridRenderer.drawGrid());
        gridRenderer.drawGrid();

        // Configurer les interactions par défaut
        setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getTarget() == this) {
                deselectAll();
            }
        });
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem addClassItem = new MenuItem("Ajouter une classe");
        addClassItem.setOnAction(e -> {
            if (onAddClassRequest != null) {
                onAddClassRequest.run();
            }
        });

        contextMenu.getItems().add(addClassItem);

        setOnContextMenuRequested(e -> {
            if (e.getTarget() == this) {
                contextMenu.show(this, e.getScreenX(), e.getScreenY());
            }
        });
    }

    private void setupKeyHandlers() {
        setOnKeyPressed(this::handleKeyPress);
        setFocusTraversable(true);
    }

    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
            if ((getSelectedClass() != null || getSelectedRelation() != null) && onDeleteRequest != null) {
                onDeleteRequest.run();
                event.consume();
            }
        } else if (event.getCode() == KeyCode.ESCAPE) {
            deselectAll();
            event.consume();
        }
    }

    private void setupSelectionListeners() {
        nodeManager.setNodeSelectionListener(node -> {
            if (node != null) {
                relationManager.selectRelation(null);
                DiagramClass selectedClass = node.getDiagramClass();

                // Publier un événement de sélection
                if (diagram != null) {
                    eventBus.publish(new ElementSelectedEvent(diagram.getId(), selectedClass.getId(), true));
                }

                if (classSelectionListener != null) {
                    classSelectionListener.accept(selectedClass);
                }
            } else {
                if (classSelectionListener != null) {
                    classSelectionListener.accept(null);
                }
            }
        });

        relationManager.setRelationSelectionListener(line -> {
            if (line != null) {
                nodeManager.selectNode(null);
                DiagramRelation selectedRelation = line.getRelation();

                // Publier un événement de sélection
                if (diagram != null) {
                    eventBus.publish(new ElementSelectedEvent(diagram.getId(), selectedRelation.getId(), false));
                }

                if (relationSelectionListener != null) {
                    relationSelectionListener.accept(selectedRelation);
                }
            } else {
                if (relationSelectionListener != null) {
                    relationSelectionListener.accept(null);
                }
            }
        });
    }

    private void setupEventBusListeners() {
        // Écouter les événements de changement de diagramme
        eventBus.subscribe(DiagramChangedEvent.class, (EventListener<DiagramChangedEvent>) event -> {
            if (diagram != null && diagram.getId().equals(event.getDiagramId())) {
                Platform.runLater(this::refresh);
            }
        });
    }

    public void loadDiagram(ClassDiagram diagram) {
        this.diagram = diagram;

        clear();

        // Première étape : créer tous les nœuds de classe
        for (DiagramClass diagramClass : diagram.getClasses()) {
            nodeManager.createClassNode(diagramClass);
        }

        // Deuxième étape : créer toutes les relations
        for (DiagramRelation relation : diagram.getRelations()) {
            relationManager.createRelationLine(relation);
        }

        // Force un layout complet après le chargement
        Platform.runLater(() -> {
            // Force un rafraîchissement complet de tous les nœuds
            for (ClassNode node : nodeManager.getNodes().values()) {
                node.refresh();
            }

            // Mettre à jour toutes les relations
            relationManager.updateAllRelations();

            // Demander une mise en page complète
            requestLayout();

            // Force une seconde passe de mise à jour pour s'assurer que tout est bien positionné
            Platform.runLater(this::refresh);
        });
    }

    // Méthode refresh améliorée à insérer dans DiagramCanvas.java

    public void refresh() {
        if (diagram != null) {
            DiagramClass selectedClass = getSelectedClass();
            DiagramRelation selectedRelation = getSelectedRelation();

            Map<String, DiagramRelation> existingRelations = new HashMap<>();
            Map<String, DiagramClass> existingClasses = new HashMap<>();

            for (RelationLine line : relationManager.getRelationLines().values()) {
                existingRelations.put(line.getRelation().getId(), line.getRelation());
            }

            for (ClassNode node : nodeManager.getNodes().values()) {
                existingClasses.put(node.getDiagramClass().getId(), node.getDiagramClass());
            }

            // Ajouter les nouveaux éléments et mettre à jour les existants
            for (DiagramClass diagramClass : diagram.getClasses()) {
                if (!existingClasses.containsKey(diagramClass.getId())) {
                    // Créer un nouveau nœud pour les classes qui n'existent pas encore
                    nodeManager.createClassNode(diagramClass);
                } else {
                    // Mettre à jour la classe existante
                    ClassNode node = nodeManager.getNodeById(diagramClass.getId());
                    if (node != null) {
                        // Vérifier que les positions sont dans les limites du canevas
                        ensureNodeWithinBounds(node);
                        node.refresh();
                    }
                }
            }

            for (DiagramRelation relation : diagram.getRelations()) {
                if (!existingRelations.containsKey(relation.getId())) {
                    // Créer une nouvelle ligne pour les relations qui n'existent pas encore
                    relationManager.createRelationLine(relation);
                }
            }

            // Supprimer les éléments qui ne sont plus dans le diagramme
            List<String> classesToRemove = new ArrayList<>();
            for (String classId : existingClasses.keySet()) {
                boolean found = false;
                for (DiagramClass diagramClass : diagram.getClasses()) {
                    if (diagramClass.getId().equals(classId)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    classesToRemove.add(classId);
                }
            }

            for (String classId : classesToRemove) {
                nodeManager.removeClassNode(existingClasses.get(classId));
            }

            List<String> relationsToRemove = new ArrayList<>();
            for (String relationId : existingRelations.keySet()) {
                boolean found = false;
                for (DiagramRelation relation : diagram.getRelations()) {
                    if (relation.getId().equals(relationId)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    relationsToRemove.add(relationId);
                }
            }

            for (String relationId : relationsToRemove) {
                relationManager.removeRelationLine(existingRelations.get(relationId));
            }

            // Mettre à jour toutes les relations
            relationManager.updateAllRelationsLater();

            // Restaurer la sélection si possible
            if (selectedClass != null) {
                selectClass(selectedClass);
            } else if (selectedRelation != null) {
                selectRelation(selectedRelation);
            }
        }
    }

    private void ensureNodeWithinBounds(ClassNode node) {
        double nodeWidth = node.getWidth();
        double nodeHeight = node.getHeight();

        // Si le nœud n'a pas encore de dimensions, on attend le prochain cycle de mise en page
        if (nodeWidth <= 0 || nodeHeight <= 0) {
            return;
        }

        double canvasWidth = getWidth();
        double canvasHeight = getHeight();

        // Marge de sécurité
        double margin = 20;

        // Position actuelle
        double currentX = node.getLayoutX();
        double currentY = node.getLayoutY();

        // Calculer les nouvelles positions contraintes
        double newX = Math.max(margin, Math.min(canvasWidth - nodeWidth - margin, currentX));
        double newY = Math.max(margin, Math.min(canvasHeight - nodeHeight - margin, currentY));

        // Appliquer les nouvelles positions si elles diffèrent
        if (currentX != newX || currentY != newY) {
            node.setLayoutX(newX);
            node.setLayoutY(newY);

            // Mettre à jour le modèle sous-jacent
            DiagramClass diagramClass = node.getDiagramClass();
            diagramClass.setX(newX);
            diagramClass.setY(newY);
        }
    }

    public Dimension2D calculateRequiredSize() {
        if (diagram == null || diagram.getClasses().isEmpty()) {
            return new Dimension2D(600, 400); // Taille par défaut
        }

        double maxX = 0;
        double maxY = 0;
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;

        // Parcourir toutes les classes pour déterminer les extrémités
        for (DiagramClass diagramClass : diagram.getClasses()) {
            ClassNode node = nodeManager.getNodeById(diagramClass.getId());
            if (node != null) {
                double nodeRight = node.getLayoutX() + node.getWidth();
                double nodeBottom = node.getLayoutY() + node.getHeight();

                maxX = Math.max(maxX, nodeRight);
                maxY = Math.max(maxY, nodeBottom);
                minX = Math.min(minX, node.getLayoutX());
                minY = Math.min(minY, node.getLayoutY());
            }
        }

        // Ajouter une marge
        double margin = 100;
        double width = Math.max(600, maxX - minX + 2 * margin);
        double height = Math.max(400, maxY - minY + 2 * margin);

        return new Dimension2D(width, height);
    }

    public void clear() {
        nodeManager.clear();
        relationManager.clear();
    }

    public void deselectAll() {
        nodeManager.selectNode(null);
        relationManager.selectRelation(null);
        requestFocus(); // S'assurer que le canvas a le focus pour les événements clavier
    }

    public DiagramClass getSelectedClass() {
        return nodeManager.getSelectedClass();
    }

    public DiagramRelation getSelectedRelation() {
        return relationManager.getSelectedRelation();
    }

    public void setOnAddClassRequest(Runnable handler) {
        this.onAddClassRequest = handler;
    }

    public void setOnDeleteRequest(Runnable handler) {
        this.onDeleteRequest = handler;
    }

    public void selectClass(DiagramClass diagramClass) {
        if (diagramClass == null) {
            nodeManager.selectNode(null);
            return;
        }

        ClassNode node = nodeManager.getNodeById(diagramClass.getId());
        if (node != null) {
            nodeManager.selectNode(node);
        }
    }

    public void selectRelation(DiagramRelation relation) {
        if (relation == null) {
            relationManager.selectRelation(null);
            return;
        }

        RelationLine line = relationManager.getLineById(relation.getId());
        if (line != null) {
            relationManager.selectRelation(line);
        }
    }

    public boolean hasSelection() {
        return getSelectedClass() != null || getSelectedRelation() != null;
    }

    public void setClassSelectionListener(Consumer<DiagramClass> listener) {
        this.classSelectionListener = listener;
    }

    public void setRelationSelectionListener(Consumer<DiagramRelation> listener) {
        this.relationSelectionListener = listener;
    }

    public NodeManager getNodeManager() {
        return nodeManager;
    }

    public RelationManager getRelationManager() {
        return relationManager;
    }

    public ClassDiagram getDiagram() {
        return diagram;
    }
}