package com.novasql.query;

import java.util.List;

public class JoinStatement extends SQLStatement {
    private final List<String> columns;
    private final String leftTable;
    private final String rightTable;
    private final String leftColumn;
    private final String rightColumn;
    private final JoinType joinType;
    private final WhereCondition whereCondition;

    public enum JoinType {
        INNER, LEFT, RIGHT, FULL
    }

    public JoinStatement(List<String> columns, String leftTable, String rightTable,
                        String leftColumn, String rightColumn, JoinType joinType,
                        WhereCondition whereCondition) {
        this.columns = columns;
        this.leftTable = leftTable;
        this.rightTable = rightTable;
        this.leftColumn = leftColumn;
        this.rightColumn = rightColumn;
        this.joinType = joinType;
        this.whereCondition = whereCondition;
    }

    @Override
    public Type getType() {
        return Type.SELECT;
    }

    public List<String> getColumns() {
        return columns;
    }

    public String getLeftTable() {
        return leftTable;
    }

    public String getRightTable() {
        return rightTable;
    }

    public String getLeftColumn() {
        return leftColumn;
    }

    public String getRightColumn() {
        return rightColumn;
    }

    public JoinType getJoinType() {
        return joinType;
    }

    public WhereCondition getWhereCondition() {
        return whereCondition;
    }

    public boolean isSelectAll() {
        return columns.size() == 1 && "*".equals(columns.get(0));
    }
}