package com.novasql.query;

import java.util.Map;

public class UpdateStatement extends SQLStatement {
    private final String tableName;
    private final Map<String, String> updates;
    private final WhereCondition whereCondition;

    public UpdateStatement(String tableName, Map<String, String> updates, WhereCondition whereCondition) {
        this.tableName = tableName;
        this.updates = updates;
        this.whereCondition = whereCondition;
    }

    @Override
    public Type getType() {
        return Type.UPDATE;
    }

    public String getTableName() {
        return tableName;
    }

    public Map<String, String> getUpdates() {
        return updates;
    }

    public WhereCondition getWhereCondition() {
        return whereCondition;
    }
}