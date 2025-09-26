package com.novasql.query;

public abstract class SQLStatement {
    public enum Type {
        SELECT, INSERT, UPDATE, DELETE, CREATE_TABLE, DROP_TABLE
    }

    public abstract Type getType();
}