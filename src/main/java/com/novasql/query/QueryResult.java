package com.novasql.query;

import com.novasql.schema.Record;

import java.util.List;

public class QueryResult {
    public enum ResultType {
        SELECT, INSERT, UPDATE, DELETE, CREATE_TABLE, DROP_TABLE
    }

    private final ResultType type;
    private final List<Record> records;
    private final int affectedRows;
    private final String message;

    // Constructor for SELECT queries
    public QueryResult(ResultType type, List<Record> records) {
        this.type = type;
        this.records = records;
        this.affectedRows = records.size();
        this.message = null;
    }

    // Constructor for INSERT/UPDATE/DELETE queries
    public QueryResult(ResultType type, int affectedRows) {
        this.type = type;
        this.records = null;
        this.affectedRows = affectedRows;
        this.message = null;
    }

    // Constructor for DDL queries
    public QueryResult(ResultType type, String message) {
        this.type = type;
        this.records = null;
        this.affectedRows = 0;
        this.message = message;
    }

    public ResultType getType() {
        return type;
    }

    public ResultType getResultType() {
        return type;
    }

    public List<Record> getRecords() {
        return records;
    }

    public int getAffectedRows() {
        return affectedRows;
    }

    public String getMessage() {
        return message;
    }

    public boolean hasRecords() {
        return records != null && !records.isEmpty();
    }

    @Override
    public String toString() {
        switch (type) {
            case SELECT:
                return String.format("SELECT result: %d records", affectedRows);
            case INSERT:
                return String.format("INSERT result: %d rows affected", affectedRows);
            case CREATE_TABLE:
                return String.format("CREATE TABLE result: %s", message);
            default:
                return String.format("%s result: %d rows affected", type, affectedRows);
        }
    }
}