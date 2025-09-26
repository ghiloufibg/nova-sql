package com.novasql.query;

import java.util.List;

public class CreateTableStatement extends SQLStatement {
    private final String tableName;
    private final List<ColumnDefinition> columns;

    public CreateTableStatement(String tableName, List<ColumnDefinition> columns) {
        this.tableName = tableName;
        this.columns = columns;
    }

    @Override
    public Type getType() {
        return Type.CREATE_TABLE;
    }

    public String getTableName() {
        return tableName;
    }

    public List<ColumnDefinition> getColumns() {
        return columns;
    }
}