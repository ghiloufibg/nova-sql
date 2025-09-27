package com.novasql.schema;

import com.novasql.query.ColumnDefinition;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Database {
    private final String name;
    private final Map<String, Table> tables;

    public Database(String name) {
        this.name = name;
        this.tables = new HashMap<>();
    }

    public void createTable(String tableName, java.util.List<ColumnDefinition> columns) {
        if (tables.containsKey(tableName)) {
            throw new IllegalArgumentException("Table '" + tableName + "' already exists");
        }

        Table table = new Table(tableName, columns);
        tables.put(tableName, table);
    }

    public void dropTable(String tableName) {
        if (!tables.containsKey(tableName)) {
            throw new IllegalArgumentException("Table '" + tableName + "' does not exist");
        }

        tables.remove(tableName);
    }

    public Table getTable(String tableName) {
        Table table = tables.get(tableName);
        if (table == null) {
            throw new IllegalArgumentException("Table '" + tableName + "' does not exist");
        }
        return table;
    }

    public boolean hasTable(String tableName) {
        return tables.containsKey(tableName);
    }

    public Set<String> getTableNames() {
        return tables.keySet();
    }

    public String getName() {
        return name;
    }

    public int getTableCount() {
        return tables.size();
    }
}