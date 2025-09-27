package com.novasql.query;

import java.util.List;
import java.util.ArrayList;

public class SelectStatement extends SQLStatement {
    private final String tableName;
    private final List<String> columns;
    private final WhereCondition whereCondition;
    private final List<String> orderBy;
    private final List<SQLParser.OrderByColumn> orderByColumns;
    private final Integer limit;
    private final Integer offset;

    public SelectStatement(String tableName, List<String> columns, WhereCondition whereCondition, List<String> orderBy) {
        this(tableName, columns, whereCondition, orderBy, null, null);
    }

    public SelectStatement(String tableName, List<String> columns, WhereCondition whereCondition, List<String> orderBy, Integer limit, Integer offset) {
        this.tableName = tableName;
        this.columns = columns;
        this.whereCondition = whereCondition;
        this.orderBy = orderBy;
        this.orderByColumns = orderBy != null ? SQLParser.parseOrderByColumns(String.join(", ", orderBy)) : new ArrayList<>();
        this.limit = limit;
        this.offset = offset;
    }

    @Override
    public Type getType() {
        return Type.SELECT;
    }

    public String getTableName() {
        return tableName;
    }

    public List<String> getColumns() {
        return columns;
    }

    public WhereCondition getWhereCondition() {
        return whereCondition;
    }

    public List<String> getOrderBy() {
        return orderBy;
    }

    public List<SQLParser.OrderByColumn> getOrderByColumns() {
        return orderByColumns;
    }

    public boolean isSelectAll() {
        return columns.size() == 1 && "*".equals(columns.get(0));
    }

    public Integer getLimit() {
        return limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public boolean hasLimit() {
        return limit != null;
    }

    public boolean hasOffset() {
        return offset != null;
    }
}