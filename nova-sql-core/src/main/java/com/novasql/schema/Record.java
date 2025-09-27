package com.novasql.schema;

import java.util.HashMap;
import java.util.Map;

public class Record {
    private final int id;
    private final Map<String, String> values;

    public Record(int id, Map<String, String> values) {
        this.id = id;
        this.values = new HashMap<>(values);
    }

    public int getId() {
        return id;
    }

    public Map<String, String> getValues() {
        return values;
    }

    public Map<String, String> getValuesCopy() {
        return new HashMap<>(values);
    }

    public String getValue(String columnName) {
        return values.get(columnName);
    }

    @Override
    public String toString() {
        return "Record{" +
                "id=" + id +
                ", values=" + values +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Record record = (Record) obj;
        return id == record.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}