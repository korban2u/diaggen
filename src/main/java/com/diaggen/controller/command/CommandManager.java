package com.diaggen.controller.command;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Stack;

public class CommandManager {

    private final Stack<Command> undoStack = new Stack<>();
    private final Stack<Command> redoStack = new Stack<>();

    private final BooleanProperty canUndoProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty canRedoProperty = new SimpleBooleanProperty(false);

    private final ObservableList<String> commandHistory = FXCollections.observableArrayList();

    // CommandGroup en cours de construction (si utilisé)
    private CommandGroup currentGroup = null;

    public void executeCommand(Command command) {
        // Si nous sommes en train de construire un groupe, ajouter à ce groupe
        if (currentGroup != null) {
            currentGroup.addCommand(command);
            return;
        }

        // Sinon, exécuter et empiler normalement
        command.execute();
        undoStack.push(command);
        redoStack.clear();

        updateProperties();
        updateCommandHistory();
    }

    public void startCommandGroup(String description) {
        // Commencer un nouveau groupe de commandes
        currentGroup = new CommandGroup(description);
    }

    public void endCommandGroup() {
        // Terminer le groupe courant et l'exécuter si non vide
        if (currentGroup != null && !currentGroup.isEmpty()) {
            Command group = currentGroup;
            currentGroup = null;
            executeCommand(group);
        } else {
            currentGroup = null;
        }
    }

    public void cancelCommandGroup() {
        // Annuler le groupe en cours sans l'exécuter
        currentGroup = null;
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            Command command = undoStack.pop();
            command.undo();
            redoStack.push(command);

            updateProperties();
            updateCommandHistory();
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            Command command = redoStack.pop();
            command.execute();
            undoStack.push(command);

            updateProperties();
            updateCommandHistory();
        }
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public ReadOnlyBooleanProperty canUndoProperty() {
        return canUndoProperty;
    }

    public ReadOnlyBooleanProperty canRedoProperty() {
        return canRedoProperty;
    }

    public ObservableList<String> getCommandHistory() {
        return commandHistory;
    }

    private void updateProperties() {
        canUndoProperty.set(canUndo());
        canRedoProperty.set(canRedo());
    }

    private void updateCommandHistory() {
        commandHistory.clear();

        // Ajouter les commandes annulables
        for (int i = undoStack.size() - 1; i >= 0; i--) {
            Command command = undoStack.get(i);
            commandHistory.add(command.getDescription() + " ✓");
        }

        // Ajouter les commandes rétablissables
        for (Command command : redoStack) {
            commandHistory.add(command.getDescription() + " ↶");
        }
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
        updateProperties();
        updateCommandHistory();
    }
}