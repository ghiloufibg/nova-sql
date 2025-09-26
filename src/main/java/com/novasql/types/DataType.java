package com.novasql.types;

public enum DataType {
    INTEGER("INTEGER", Integer.class),
    VARCHAR("VARCHAR", String.class),
    BOOLEAN("BOOLEAN", Boolean.class),
    DATE("DATE", java.time.LocalDate.class),
    DECIMAL("DECIMAL", java.math.BigDecimal.class);

    private final String typeName;
    private final Class<?> javaType;

    DataType(String typeName, Class<?> javaType) {
        this.typeName = typeName;
        this.javaType = javaType;
    }

    public String getTypeName() {
        return typeName;
    }

    public Class<?> getJavaType() {
        return javaType;
    }

    public static DataType fromString(String typeStr) {
        String upperType = typeStr.toUpperCase();

        if (upperType.startsWith("VARCHAR")) {
            return VARCHAR;
        }

        for (DataType type : values()) {
            if (type.typeName.equals(upperType)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unsupported data type: " + typeStr);
    }

    public Object parseValue(String value) {
        if (value == null || "NULL".equalsIgnoreCase(value)) {
            return null;
        }

        try {
            switch (this) {
                case INTEGER:
                    return Integer.parseInt(value);
                case VARCHAR:
                    return value;
                case BOOLEAN:
                    return Boolean.parseBoolean(value);
                case DATE:
                    return java.time.LocalDate.parse(value);
                case DECIMAL:
                    return new java.math.BigDecimal(value);
                default:
                    throw new IllegalArgumentException("Cannot parse value for type: " + this);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid value '" + value + "' for type " + this + ": " + e.getMessage());
        }
    }

    public String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }

        switch (this) {
            case INTEGER:
            case BOOLEAN:
            case DECIMAL:
                return value.toString();
            case VARCHAR:
                return value.toString();
            case DATE:
                return value.toString();
            default:
                return value.toString();
        }
    }

    public boolean isValidValue(String value) {
        try {
            parseValue(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}