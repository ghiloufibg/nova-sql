package com.novasql.query;

import java.util.List;
import java.util.Arrays;
import java.util.regex.Pattern;

public class WhereCondition {
    private final String column;
    private final String operator;
    private final String value;
    private final List<String> values; // For IN operator
    private final String rangeStart; // For BETWEEN operator
    private final String rangeEnd; // For BETWEEN operator

    public WhereCondition(String column, String operator, String value) {
        this.column = column;
        this.operator = operator;
        this.value = value;
        this.values = null;
        this.rangeStart = null;
        this.rangeEnd = null;
    }

    public WhereCondition(String column, String operator, List<String> values) {
        this.column = column;
        this.operator = operator;
        this.value = null;
        this.values = values;
        this.rangeStart = null;
        this.rangeEnd = null;
    }

    public WhereCondition(String column, String operator, String rangeStart, String rangeEnd) {
        this.column = column;
        this.operator = operator;
        this.value = null;
        this.values = null;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
    }

    public String getColumn() {
        return column;
    }

    public String getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }

    public List<String> getValues() {
        return values;
    }

    public String getRangeStart() {
        return rangeStart;
    }

    public String getRangeEnd() {
        return rangeEnd;
    }

    public boolean evaluate(String actualValue) {
        switch (operator.toUpperCase()) {
            case "=":
                return actualValue != null && actualValue.equals(value);
            case "!=":
            case "<>":
                return actualValue == null || !actualValue.equals(value);
            case ">":
                return actualValue != null && actualValue.compareTo(value) > 0;
            case ">=":
                return actualValue != null && actualValue.compareTo(value) >= 0;
            case "<":
                return actualValue != null && actualValue.compareTo(value) < 0;
            case "<=":
                return actualValue != null && actualValue.compareTo(value) <= 0;
            case "IS NULL":
                return actualValue == null || "NULL".equalsIgnoreCase(actualValue);
            case "IS NOT NULL":
                return actualValue != null && !"NULL".equalsIgnoreCase(actualValue);
            case "LIKE":
                return actualValue != null && matchesLikePattern(actualValue, value);
            case "NOT LIKE":
                return actualValue == null || !matchesLikePattern(actualValue, value);
            case "IN":
                return actualValue != null && values != null && values.contains(actualValue);
            case "NOT IN":
                return actualValue == null || values == null || !values.contains(actualValue);
            case "BETWEEN":
                return actualValue != null && rangeStart != null && rangeEnd != null &&
                        actualValue.compareTo(rangeStart) >= 0 && actualValue.compareTo(rangeEnd) <= 0;
            case "NOT BETWEEN":
                return actualValue == null || rangeStart == null || rangeEnd == null ||
                        actualValue.compareTo(rangeStart) < 0 || actualValue.compareTo(rangeEnd) > 0;
            default:
                throw new UnsupportedOperationException("Unsupported operator: " + operator);
        }
    }

    private boolean matchesLikePattern(String text, String pattern) {
        // Convert SQL LIKE pattern to regex pattern
        // % matches any sequence of characters
        // _ matches any single character
        String regexPattern = pattern
                .replace("\\", "\\\\")
                .replace(".", "\\.")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("^", "\\^")
                .replace("$", "\\$")
                .replace("+", "\\+")
                .replace("?", "\\?")
                .replace("*", "\\*")
                .replace("|", "\\|")
                .replace("%", ".*")
                .replace("_", ".");

        return Pattern.matches(regexPattern, text);
    }
}