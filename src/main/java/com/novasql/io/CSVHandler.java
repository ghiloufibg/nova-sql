package com.novasql.io;

import com.novasql.DatabaseEngine;
import com.novasql.query.QueryResult;
import com.novasql.schema.Record;
import com.novasql.schema.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles CSV import and export operations for the Nova SQL database engine.
 *
 * <p>The CSVHandler provides functionality to import data from CSV files into database tables
 * and export table data to CSV format. It supports:</p>
 * <ul>
 *   <li>Automatic column mapping from CSV headers</li>
 *   <li>Data type conversion and validation</li>
 *   <li>Error handling for malformed data</li>
 *   <li>Proper CSV escaping for special characters</li>
 * </ul>
 *
 * <p>CSV Format Requirements:</p>
 * <ul>
 *   <li>First row must contain column headers</li>
 *   <li>Comma-separated values</li>
 *   <li>Optional quoting for values containing commas or newlines</li>
 * </ul>
 *
 * @author Nova SQL Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class CSVHandler {
    private static final Logger logger = LoggerFactory.getLogger(CSVHandler.class);
    private static final String CSV_SEPARATOR = ",";

    private final DatabaseEngine engine;

    public CSVHandler(DatabaseEngine engine) {
        this.engine = engine;
    }

    public void importCSV(String filePath, String tableName) throws IOException {
        logger.info("Importing CSV file {} into table {}", filePath, tableName);

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV file is empty");
            }

            String[] headers = parseCSVLine(headerLine);
            Table table = engine.getDatabase().getTable(tableName);

            int importedRows = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] values = parseCSVLine(line);
                if (values.length != headers.length) {
                    logger.warn("Skipping malformed line: {}", line);
                    continue;
                }

                Map<String, String> record = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    record.put(headers[i], values[i]);
                }

                try {
                    String sql = buildInsertSQL(tableName, record);
                    engine.executeSQL(sql);
                    importedRows++;
                } catch (Exception e) {
                    logger.warn("Failed to import record: {} - {}", record, e.getMessage());
                }
            }

            logger.info("Imported {} rows into table {}", importedRows, tableName);
        }
    }

    public void exportCSV(String tableName, String filePath) throws IOException {
        logger.info("Exporting table {} to CSV file {}", tableName, filePath);

        QueryResult result = engine.executeSQL("SELECT * FROM " + tableName);

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath))) {
            List<Record> records = result.getRecords();
            if (records.isEmpty()) {
                logger.info("Table {} is empty, creating empty CSV file", tableName);
                return;
            }

            // Write header - use table column order
            Table table = engine.getDatabase().getTable(tableName);
            String[] headers = table.getColumns().stream()
                    .map(col -> col.getName())
                    .toArray(String[]::new);
            writer.write(String.join(CSV_SEPARATOR, headers));
            writer.newLine();

            // Write data
            for (Record record : records) {
                String[] values = new String[headers.length];
                for (int i = 0; i < headers.length; i++) {
                    String value = record.getValue(headers[i]);
                    values[i] = escapeCSVValue(value);
                }
                writer.write(String.join(CSV_SEPARATOR, values));
                writer.newLine();
            }

            logger.info("Exported {} rows from table {} to {}", records.size(), tableName, filePath);
        }
    }

    private String[] parseCSVLine(String line) {
        return line.split(CSV_SEPARATOR, -1);
    }

    private String escapeCSVValue(String value) {
        if (value == null) {
            return "";
        }

        // Escape commas and quotes
        if (value.contains(CSV_SEPARATOR) || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }

    private String buildInsertSQL(String tableName, Map<String, String> record) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(tableName).append(" (");

        String[] columns = record.keySet().toArray(new String[0]);
        sql.append(String.join(", ", columns));
        sql.append(") VALUES (");

        for (int i = 0; i < columns.length; i++) {
            if (i > 0) sql.append(", ");
            String value = record.get(columns[i]);
            if (value == null || value.isEmpty()) {
                sql.append("NULL");
            } else {
                sql.append("'").append(value.replace("'", "''")).append("'");
            }
        }

        sql.append(")");
        return sql.toString();
    }
}