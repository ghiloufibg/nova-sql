package com.novasql.schema;

import com.novasql.query.ColumnDefinition;
import com.novasql.index.BTree;

import java.util.*;
import java.util.Arrays;

public class Table {
    private final String name;
    private final List<ColumnDefinition> columns;
    private final Map<String, Integer> columnIndexMap;
    private final Map<String, BTree> indexes;
    private final List<Record> records;
    private int nextRecordId;

    public Table(String name, List<ColumnDefinition> columns) {
        this.name = name;
        this.columns = new ArrayList<>(columns);
        this.columnIndexMap = new HashMap<>();
        this.indexes = new HashMap<>();
        this.records = new ArrayList<>();
        this.nextRecordId = 1;

        // Build column index map
        for (int i = 0; i < columns.size(); i++) {
            columnIndexMap.put(columns.get(i).getName(), i);
        }

        // Create primary key index
        for (ColumnDefinition column : columns) {
            if (column.isPrimaryKey()) {
                indexes.put(column.getName(), new BTree());
            }
        }
    }

    public void insertRecord(Map<String, String> values) {
        validateRecord(values);

        Record record = new Record(nextRecordId++, values);
        records.add(record);

        // Update indexes
        for (Map.Entry<String, BTree> indexEntry : indexes.entrySet()) {
            String columnName = indexEntry.getKey();
            String value = values.get(columnName);
            if (value != null) {
                indexEntry.getValue().insert(value, record.getId());
            }
        }
    }

    public List<Record> selectRecords(List<String> requestedColumns) {
        if (requestedColumns.size() == 1 && "*".equals(requestedColumns.get(0))) {
            return new ArrayList<>(records);
        }

        // Filter columns
        List<Record> filteredRecords = new ArrayList<>();
        for (Record record : records) {
            Map<String, String> filteredValues = new HashMap<>();
            for (String column : requestedColumns) {
                if (record.getValues().containsKey(column)) {
                    filteredValues.put(column, record.getValues().get(column));
                }
            }
            filteredRecords.add(new Record(record.getId(), filteredValues));
        }

        return filteredRecords;
    }

    public List<Record> selectRecords(List<String> requestedColumns, String whereColumn, String whereValue) {
        List<Record> matchingRecords = new ArrayList<>();

        // Use index if available
        BTree index = indexes.get(whereColumn);
        if (index != null) {
            Integer recordId = index.search(whereValue);
            if (recordId != null) {
                Record record = findRecordById(recordId);
                if (record != null) {
                    matchingRecords.add(record);
                }
            }
        } else {
            // Full table scan
            for (Record record : records) {
                String value = record.getValues().get(whereColumn);
                if (whereValue.equals(value)) {
                    matchingRecords.add(record);
                }
            }
        }

        // Filter columns
        if (requestedColumns.size() == 1 && "*".equals(requestedColumns.get(0))) {
            return matchingRecords;
        }

        List<Record> filteredRecords = new ArrayList<>();
        for (Record record : matchingRecords) {
            Map<String, String> filteredValues = new HashMap<>();
            for (String column : requestedColumns) {
                if (record.getValues().containsKey(column)) {
                    filteredValues.put(column, record.getValues().get(column));
                }
            }
            filteredRecords.add(new Record(record.getId(), filteredValues));
        }

        return filteredRecords;
    }

    private Record findRecordById(int recordId) {
        for (Record record : records) {
            if (record.getId() == recordId) {
                return record;
            }
        }
        return null;
    }

    private void validateRecord(Map<String, String> values) {
        // Check if all required columns are present
        for (ColumnDefinition column : columns) {
            if (column.isPrimaryKey() && !values.containsKey(column.getName())) {
                throw new IllegalArgumentException("Primary key column '" + column.getName() + "' is required");
            }
        }

        // Check for duplicate primary keys
        for (ColumnDefinition column : columns) {
            if (column.isPrimaryKey()) {
                String value = values.get(column.getName());
                if (value != null && indexes.get(column.getName()).search(value) != null) {
                    throw new IllegalArgumentException("Duplicate primary key value: " + value);
                }
            }
        }
    }

    public void createIndex(String columnName) {
        if (!columnIndexMap.containsKey(columnName)) {
            throw new IllegalArgumentException("Column '" + columnName + "' does not exist");
        }

        if (indexes.containsKey(columnName)) {
            throw new IllegalArgumentException("Index already exists for column: " + columnName);
        }

        BTree index = new BTree();
        for (Record record : records) {
            String value = record.getValues().get(columnName);
            if (value != null) {
                index.insert(value, record.getId());
            }
        }

        indexes.put(columnName, index);
    }

    public String getName() {
        return name;
    }

    public List<ColumnDefinition> getColumns() {
        return new ArrayList<>(columns);
    }

    public int getRecordCount() {
        return records.size();
    }

    public boolean hasIndex(String columnName) {
        return indexes.containsKey(columnName);
    }

    public Set<String> getIndexedColumns() {
        return new HashSet<>(indexes.keySet());
    }

    public Set<String> getIndexNames() {
        return new HashSet<>(indexes.keySet());
    }

    public void vacuum() {
        // Vacuum operation: reorganize storage and reclaim space
        // For now, this is a placeholder implementation
        System.out.println("VACUUM completed for table: " + name);
    }

    public void analyze() {
        // Analyze operation: update statistics for query optimization
        // For now, this is a placeholder implementation
        System.out.println("ANALYZE completed for table: " + name + " (" + records.size() + " records)");
    }

    public int updateRecords(Map<String, String> updates, String whereColumn, String whereValue) {
        int updatedCount = 0;
        List<Record> recordsToUpdate;

        if (whereColumn != null) {
            recordsToUpdate = selectRecords(Arrays.asList("*"), whereColumn, whereValue);
        } else {
            recordsToUpdate = new ArrayList<>(records);
        }

        for (Record record : recordsToUpdate) {
            // Find the actual record in the list
            Record actualRecord = findRecordById(record.getId());
            if (actualRecord != null) {
                updateRecord(actualRecord, updates);
                updatedCount++;
            }
        }

        return updatedCount;
    }

    public int deleteRecords(String whereColumn, String whereValue) {
        int deletedCount = 0;
        List<Record> recordsToDelete;

        if (whereColumn != null) {
            recordsToDelete = selectRecords(Arrays.asList("*"), whereColumn, whereValue);
        } else {
            recordsToDelete = new ArrayList<>(records);
        }

        for (Record record : recordsToDelete) {
            if (records.remove(record)) {
                // Remove from indexes
                for (Map.Entry<String, BTree> indexEntry : indexes.entrySet()) {
                    String columnName = indexEntry.getKey();
                    String value = record.getValue(columnName);
                    if (value != null) {
                        indexEntry.getValue().delete(value);
                    }
                }
                deletedCount++;
            }
        }

        return deletedCount;
    }

    private void updateRecord(Record record, Map<String, String> updates) {
        Map<String, String> newValues = new HashMap<>(record.getValues());

        // Remove old values from indexes
        for (Map.Entry<String, BTree> indexEntry : indexes.entrySet()) {
            String columnName = indexEntry.getKey();
            String oldValue = record.getValue(columnName);
            if (oldValue != null) {
                indexEntry.getValue().delete(oldValue);
            }
        }

        // Apply updates
        for (Map.Entry<String, String> update : updates.entrySet()) {
            newValues.put(update.getKey(), update.getValue());
        }

        // Validate updated record
        validateUpdatedRecord(newValues, record.getId());

        // Update the record
        record.getValues().clear();
        record.getValues().putAll(newValues);

        // Update indexes
        for (Map.Entry<String, BTree> indexEntry : indexes.entrySet()) {
            String columnName = indexEntry.getKey();
            String newValue = newValues.get(columnName);
            if (newValue != null) {
                indexEntry.getValue().insert(newValue, record.getId());
            }
        }
    }

    private void validateUpdatedRecord(Map<String, String> values, int excludeRecordId) {
        // Check for duplicate primary keys
        for (ColumnDefinition column : columns) {
            if (column.isPrimaryKey()) {
                String value = values.get(column.getName());
                if (value != null) {
                    Integer foundRecordId = indexes.get(column.getName()).search(value);
                    if (foundRecordId != null && foundRecordId != excludeRecordId) {
                        throw new IllegalArgumentException("Duplicate primary key value: " + value);
                    }
                }
            }
        }
    }
}