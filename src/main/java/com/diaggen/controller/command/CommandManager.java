package com.diaggen.controller.command;

import com.diaggen.model.session.ProjectSessionManager;

import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommandManager {
    private static final Logger LOGGER = Logger.getLogger(CommandManager.class.getName());

    private final Stack<Command> undoStack = new Stack<>();
    private final Stack<Command> redoStack = new Stack<>();
    private final Stack<Command> groupStack = new Stack<>();
    private boolean inGroup = false;
    private String currentGroupName = "";
    private final ProjectSessionManager sessionManager;

    public CommandManager() {
        this.sessionManager = ProjectSessionManager.getInstance();
    }

    public void executeCommand(Command command) {
        if (command == null) return;

        if (inGroup) {
            groupStack.push(command);
        } else {
            command.execute();
            undoStack.push(command);
            redoStack.clear();
        }
        sessionManager.markProjectAsModified();

        LOGGER.log(Level.FINE, "Command executed: {0}, project marked as modified",
                new Object[]{command.getClass().getSimpleName()});
    }

    public void undo() {
        if (canUndo()) {
            Command command = undoStack.pop();
            command.undo();
            redoStack.push(command);
            sessionManager.markProjectAsModified();

            LOGGER.log(Level.FINE, "Command undone: {0}, project marked as modified",
                    new Object[]{command.getClass().getSimpleName()});
        }
    }

    public void redo() {
        if (canRedo()) {
            Command command = redoStack.pop();
            command.execute();
            undoStack.push(command);
            sessionManager.markProjectAsModified();

            LOGGER.log(Level.FINE, "Command redone: {0}, project marked as modified",
                    new Object[]{command.getClass().getSimpleName()});
        }
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void startCommandGroup(String name) {
        if (inGroup) {
            LOGGER.log(Level.WARNING, "Trying to start a command group while already in a group");
            return;
        }

        inGroup = true;
        currentGroupName = name;
        groupStack.clear();
        LOGGER.log(Level.FINE, "Command group started: {0}", new Object[]{name});
    }

    public void endCommandGroup() {
        if (!inGroup) {
            LOGGER.log(Level.WARNING, "Trying to end a command group while not in a group");
            return;
        }

        inGroup = false;

        if (!groupStack.isEmpty()) {
            CommandGroup group = new CommandGroup(currentGroupName);
            for (Command cmd : groupStack) {
                group.addCommand(cmd);
            }

            undoStack.push(group);
            redoStack.clear();
            sessionManager.markProjectAsModified();
            int commandCount = groupStack.size();
            LOGGER.log(Level.FINE, "Command group ended with {0} commands, project marked as modified",
                    new Object[]{commandCount});
        } else {
            LOGGER.log(Level.FINE, "Command group ended with no commands");
        }
        currentGroupName = "";
    }

    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
        LOGGER.log(Level.INFO, "Command history cleared");
    }
}