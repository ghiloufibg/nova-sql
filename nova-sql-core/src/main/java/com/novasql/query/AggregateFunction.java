package com.novasql.query;

import com.novasql.schema.Record;

import java.math.BigDecimal;
import java.util.List;

public enum AggregateFunction {
    COUNT, SUM, AVG, MIN, MAX;

    public Object apply(List<Record> records, String columnName) {
        if (records.isEmpty()) {
            return this == COUNT ? 0 : null;
        }

        switch (this) {
            case COUNT:
                if ("*".equals(columnName)) {
                    return records.size();
                } else {
                    return records.stream()
                            .mapToLong(r -> r.getValue(columnName) != null ? 1 : 0)
                            .sum();
                }

            case SUM:
                return records.stream()
                        .map(r -> r.getValue(columnName))
                        .filter(v -> v != null && !v.isEmpty())
                        .mapToDouble(v -> {
                            try {
                                return Double.parseDouble(v);
                            } catch (NumberFormatException e) {
                                return 0.0;
                            }
                        })
                        .sum();

            case AVG:
                double sum = records.stream()
                        .map(r -> r.getValue(columnName))
                        .filter(v -> v != null && !v.isEmpty())
                        .mapToDouble(v -> {
                            try {
                                return Double.parseDouble(v);
                            } catch (NumberFormatException e) {
                                return 0.0;
                            }
                        })
                        .sum();
                long count = records.stream()
                        .mapToLong(r -> {
                            String v = r.getValue(columnName);
                            return v != null && !v.isEmpty() ? 1 : 0;
                        })
                        .sum();
                return count > 0 ? sum / count : 0.0;

            case MIN:
                return records.stream()
                        .map(r -> r.getValue(columnName))
                        .filter(v -> v != null && !v.isEmpty())
                        .min(String::compareTo)
                        .orElse(null);

            case MAX:
                return records.stream()
                        .map(r -> r.getValue(columnName))
                        .filter(v -> v != null && !v.isEmpty())
                        .max(String::compareTo)
                        .orElse(null);

            default:
                throw new UnsupportedOperationException("Unsupported aggregate function: " + this);
        }
    }

    public static AggregateFunction fromString(String function) {
        try {
            return valueOf(function.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown aggregate function: " + function);
        }
    }
}