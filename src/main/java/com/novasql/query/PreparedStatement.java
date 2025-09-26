package com.novasql.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreparedStatement {
    private final String originalSql;
    private final List<String> parameterPlaceholders;
    private final Map<Integer, String> parameters;

    public PreparedStatement(String sql) {
        this.originalSql = sql;
        this.parameterPlaceholders = new ArrayList<>();
        this.parameters = new HashMap<>();
        parseParameters();
    }

    private void parseParameters() {
        // Find all ? placeholders in the SQL
        String remaining = originalSql;
        int index = 0;
        while ((index = remaining.indexOf('?', index)) != -1) {
            parameterPlaceholders.add("param" + parameterPlaceholders.size());
            index++;
        }
    }

    public void setString(int parameterIndex, String value) {
        if (parameterIndex < 1 || parameterIndex > parameterPlaceholders.size()) {
            throw new IndexOutOfBoundsException("Parameter index out of range: " + parameterIndex);
        }
        parameters.put(parameterIndex - 1, "'" + value.replace("'", "''") + "'"); // Escape single quotes
    }

    public void setInt(int parameterIndex, int value) {
        if (parameterIndex < 1 || parameterIndex > parameterPlaceholders.size()) {
            throw new IndexOutOfBoundsException("Parameter index out of range: " + parameterIndex);
        }
        parameters.put(parameterIndex - 1, String.valueOf(value));
    }

    public void setLong(int parameterIndex, long value) {
        if (parameterIndex < 1 || parameterIndex > parameterPlaceholders.size()) {
            throw new IndexOutOfBoundsException("Parameter index out of range: " + parameterIndex);
        }
        parameters.put(parameterIndex - 1, String.valueOf(value));
    }

    public void setBoolean(int parameterIndex, boolean value) {
        if (parameterIndex < 1 || parameterIndex > parameterPlaceholders.size()) {
            throw new IndexOutOfBoundsException("Parameter index out of range: " + parameterIndex);
        }
        parameters.put(parameterIndex - 1, String.valueOf(value));
    }

    public void setNull(int parameterIndex) {
        if (parameterIndex < 1 || parameterIndex > parameterPlaceholders.size()) {
            throw new IndexOutOfBoundsException("Parameter index out of range: " + parameterIndex);
        }
        parameters.put(parameterIndex - 1, "NULL");
    }

    public String getExecutableSQL() {
        if (parameters.size() < parameterPlaceholders.size()) {
            throw new IllegalStateException("Not all parameters have been set");
        }

        String result = originalSql;

        // Replace ? with actual parameter values
        for (int i = 0; i < parameterPlaceholders.size(); i++) {
            String paramValue = parameters.get(i);
            if (paramValue == null) {
                throw new IllegalStateException("Parameter " + (i + 1) + " has not been set");
            }

            // Replace the first occurrence of ? with the parameter value
            int questionMarkIndex = result.indexOf('?');
            if (questionMarkIndex != -1) {
                result = result.substring(0, questionMarkIndex) + paramValue + result.substring(questionMarkIndex + 1);
            }
        }

        return result;
    }

    public void clearParameters() {
        parameters.clear();
    }

    public int getParameterCount() {
        return parameterPlaceholders.size();
    }

    public String getOriginalSQL() {
        return originalSql;
    }
}