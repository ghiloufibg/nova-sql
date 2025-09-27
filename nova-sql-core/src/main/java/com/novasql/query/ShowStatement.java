package com.novasql.query;

public class ShowStatement extends SQLStatement {
    public enum ShowType {
        TABLES,
        INDEXES,
        STATS,
        DATABASES
    }

    private final ShowType showType;
    private final String tableName; // For SHOW INDEXES FROM table

    public ShowStatement(ShowType showType) {
        this(showType, null);
    }

    public ShowStatement(ShowType showType, String tableName) {
        this.showType = showType;
        this.tableName = tableName;
    }

    @Override
    public Type getType() {
        return Type.SELECT; // Treat SHOW as a special SELECT
    }

    public ShowType getShowType() {
        return showType;
    }

    public String getTableName() {
        return tableName;
    }
}