package com.diaggen.model;

public enum Visibility {
    PUBLIC("+"),
    PRIVATE("-"),
    PROTECTED("#"),
    PACKAGE("~");

    private final String symbol;

    Visibility(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}


