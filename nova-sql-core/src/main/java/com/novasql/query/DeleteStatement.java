package com.novasql.query;

public class DeleteStatement extends SQLStatement {
    private final String tableName;
    private final WhereCondition whereCondition;

    public DeleteStatement(String tableName, WhereCondition whereCondition) {
        this.tableName = tableName;
        this.whereCondition = whereCondition;
    }

    @Override
    public Type getType() {
        return Type.DELETE;
    }

    public String getTableName() {
        return tableName;
    }

    public WhereCondition getWhereCondition() {
        return whereCondition;
    }
}