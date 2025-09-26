package com.novasql.query;

public class CreateIndexStatement extends SQLStatement {
    private final String indexName;
    private final String tableName;
    private final String columnName;

    public CreateIndexStatement(String indexName, String tableName, String columnName) {
        this.indexName = indexName;
        this.tableName = tableName;
        this.columnName = columnName;
    }

    @Override
    public Type getType() {
        return Type.CREATE_TABLE; // Reusing for DDL operations
    }

    public String getIndexName() {
        return indexName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getColumnName() {
        return columnName;
    }
}