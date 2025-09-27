package com.novasql.query;

public class ExplainStatement extends SQLStatement {
    private final SQLStatement innerStatement;

    public ExplainStatement(SQLStatement innerStatement) {
        this.innerStatement = innerStatement;
    }

    @Override
    public Type getType() {
        return Type.SELECT; // Treat EXPLAIN as a special SELECT
    }

    public SQLStatement getInnerStatement() {
        return innerStatement;
    }
}