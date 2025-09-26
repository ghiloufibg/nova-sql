package com.novasql.query;

import com.novasql.types.DataType;

public class ColumnDefinition {
    private final String name;
    private final String type;
    private final DataType dataType;
    private final boolean primaryKey;
    private final boolean notNull;
    private final String defaultValue;
    private final boolean autoIncrement;
    private final boolean unique;

    public ColumnDefinition(String name, String type, boolean primaryKey) {
        this(name, type, primaryKey, false, null, false, false);
    }

    public ColumnDefinition(String name, String type, boolean primaryKey, boolean notNull, String defaultValue) {
        this(name, type, primaryKey, notNull, defaultValue, false, false);
    }

    public ColumnDefinition(String name, String type, boolean primaryKey, boolean notNull, String defaultValue, boolean autoIncrement, boolean unique) {
        this.name = name;
        this.type = type;
        this.dataType = DataType.fromString(type);
        this.primaryKey = primaryKey;
        this.notNull = notNull || primaryKey; // Primary keys are always NOT NULL
        this.defaultValue = defaultValue;
        this.autoIncrement = autoIncrement;
        this.unique = unique || primaryKey; // Primary keys are always UNIQUE
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public DataType getDataType() {
        return dataType;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public boolean isNotNull() {
        return notNull;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public boolean isAutoIncrement() {
        return autoIncrement;
    }

    public boolean isUnique() {
        return unique;
    }

    public Object parseValue(String value) {
        return dataType.parseValue(value);
    }

    public String formatValue(Object value) {
        return dataType.formatValue(value);
    }

    public boolean isValidValue(String value) {
        if (value == null || "NULL".equalsIgnoreCase(value)) {
            return !notNull;
        }
        return dataType.isValidValue(value);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" ").append(type);
        if (autoIncrement) sb.append(" AUTO_INCREMENT");
        if (primaryKey) sb.append(" PRIMARY KEY");
        if (unique && !primaryKey) sb.append(" UNIQUE");
        if (notNull && !primaryKey) sb.append(" NOT NULL");
        if (defaultValue != null) sb.append(" DEFAULT ").append(defaultValue);
        return sb.toString();
    }
}