package com.novasql.query;

public class VacuumStatement extends SQLStatement {
    private final String tableName; // null for full database vacuum

    public VacuumStatement() {
        this.tableName = null;
    }

    public VacuumStatement(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public Type getType() {
        return Type.CREATE_TABLE; // Reuse CREATE_TABLE for utility commands
    }

    public String getTableName() {
        return tableName;
    }

    public boolean isFullVacuum() {
        return tableName == null;
    }
}