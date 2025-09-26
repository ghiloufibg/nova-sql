package com.novasql.query;

import java.util.Map;

public class InsertStatement extends SQLStatement {
    private final String tableName;
    private final Map<String, String> columnValues;

    public InsertStatement(String tableName, Map<String, String> columnValues) {
        this.tableName = tableName;
        this.columnValues = columnValues;
    }

    @Override
    public Type getType() {
        return Type.INSERT;
    }

    public String getTableName() {
        return tableName;
    }

    public Map<String, String> getColumnValues() {
        return columnValues;
    }
}