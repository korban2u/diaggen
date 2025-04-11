package com.diaggen.controller.command;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Stack;

/**
 * Gestionnaire de commandes pour l'implémentation de undo/redo
 */
public class CommandManager {

    private final Stack<Command> undoStack = new Stack<>();
    private final Stack<Command> redoStack = new Stack<>();

    private final BooleanProperty canUndoProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty canRedoProperty = new SimpleBooleanProperty(false);

    private final ObservableList<String> commandHistory = FXCollections.observableArrayList();

    public void executeCommand(Command command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear();

        updateProperties();
        updateCommandHistory();
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