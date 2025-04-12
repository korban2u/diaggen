package com.diaggen.view.diagram;

import com.diaggen.model.ClassDiagram;
import com.diaggen.model.DiagramClass;
import com.diaggen.model.DiagramRelation;
import com.diaggen.view.diagram.canvas.ClassNode;
import com.diaggen.view.diagram.canvas.GridRenderer;
import com.diaggen.view.diagram.canvas.NodeManager;
import com.diaggen.view.diagram.canvas.RelationLine;
import com.diaggen.view.diagram.canvas.RelationManager;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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

        nodeManager.setRelationManager(relationManager);

        // Menu contextuel pour le canvas
        setupContextMenu();

        // Gérer le clic sur le fond pour désélectionner
        setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getTarget() == this) {
                deselectAll();
            }
        });

        // Configurer la gestion des touches clavier
        setupKeyHandlers();

        widthProperty().addListener((obs, oldVal, newVal) -> gridRenderer.drawGrid());
        heightProperty().addListener((obs, oldVal, newVal) -> gridRenderer.drawGrid());
        gridRenderer.drawGrid();

        // Configurer les écouteurs de sélection
        setupSelectionListeners();
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
            // Ne montrer le menu contextuel que si le clic est sur le fond
            if (e.getTarget() == this) {
                contextMenu.show(this, e.getScreenX(), e.getScreenY());
            }
        });
    }

    private void setupKeyHandlers() {
        // Gérer les touches clavier
        setOnKeyPressed(this::handleKeyPress);

        // S'assurer que le composant peut recevoir le focus et les événements clavier
        setFocusTraversable(true);
    }

    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
            // Si un élément est sélectionné et que la touche Delete est pressée
            if ((getSelectedClass() != null || getSelectedRelation() != null) && onDeleteRequest != null) {
                onDeleteRequest.run();
                event.consume();
            }
        } else if (event.getCode() == KeyCode.ESCAPE) {
            // Echap désélectionne tout
            deselectAll();
            event.consume();
        }
    }

    private void setupSelectionListeners() {
        // Configurer les écouteurs de sélection
        nodeManager.setNodeSelectionListener(node -> {
            if (node != null) {
                relationManager.selectRelation(null);
                if (classSelectionListener != null) {
                    classSelectionListener.accept(node.getDiagramClass());
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
                if (relationSelectionListener != null) {
                    relationSelectionListener.accept(line.getRelation());
                }
            } else {
                if (relationSelectionListener != null) {
                    relationSelectionListener.accept(null);
                }
            }
        });
    }

    public void loadDiagram(ClassDiagram diagram) {
        this.diagram = diagram;

        clear();

        for (DiagramClass diagramClass : diagram.getClasses()) {
            nodeManager.createClassNode(diagramClass);
        }

        for (DiagramRelation relation : diagram.getRelations()) {
            relationManager.createRelationLine(relation);
        }

        relationManager.updateAllRelationsLater();
    }

    /**
     * Rafraîchit l'affichage du diagramme
     */
    public void refresh() {
        if (diagram != null) {
            // Sauvegarder l'élément sélectionné
            DiagramClass selectedClass = getSelectedClass();
            DiagramRelation selectedRelation = getSelectedRelation();

            // Stocker les relations et les classes existantes pour vérifier la présence
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
                    ClassNode node = nodeManager.createClassNode(diagramClass);
                } else {
                    // Mettre à jour la classe existante
                    ClassNode node = nodeManager.getNodeById(diagramClass.getId());
                    if (node != null) {
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

    /**
     * Vérifie s'il y a une sélection active (classe ou relation)
     * @return true si une classe ou une relation est sélectionnée
     */
    public boolean hasSelection() {
        return getSelectedClass() != null || getSelectedRelation() != null;
    }

    // Accesseurs pour les écouteurs de sélection
    public void setClassSelectionListener(Consumer<DiagramClass> listener) {
        this.classSelectionListener = listener;
    }

    public void setRelationSelectionListener(Consumer<DiagramRelation> listener) {
        this.relationSelectionListener = listener;
    }
}