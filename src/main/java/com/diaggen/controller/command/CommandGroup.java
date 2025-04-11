package com.diaggen.controller.command;

import java.util.ArrayList;
import java.util.List;

public class CommandGroup implements Command {

    private final String description;
    private final List<Command> commands = new ArrayList<>();

    public CommandGroup(String description) {
        this.description = description;
    }

    public void addCommand(Command command) {
        commands.add(command);
    }

    @Override
    public void execute() {
        // Exécuter les commandes dans l'ordre
        for (Command command : commands) {
            command.execute();
        }
    }

    @Override
    public void undo() {
        // Défaire les commandes dans l'ordre inverse
        for (int i = commands.size() - 1; i >= 0; i--) {
            commands.get(i).undo();
        }
    }

    @Override
    public String getDescription() {
        return description;
    }

    public boolean isEmpty() {
        return commands.isEmpty();
    }
}