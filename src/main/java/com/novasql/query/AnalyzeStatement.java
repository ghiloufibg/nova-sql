package com.novasql.query;

public class AnalyzeStatement extends SQLStatement {
    private final String tableName; // null for all tables

    public AnalyzeStatement() {
        this.tableName = null;
    }

    public AnalyzeStatement(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public Type getType() {
        return Type.CREATE_TABLE; // Reuse CREATE_TABLE for utility commands
    }

    public String getTableName() {
        return tableName;
    }

    public boolean isFullAnalyze() {
        return tableName == null;
    }
}