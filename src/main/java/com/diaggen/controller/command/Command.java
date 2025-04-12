package com.diaggen.controller.command;

public interface Command {

        void execute();

        void undo();

        String getDescription();
}