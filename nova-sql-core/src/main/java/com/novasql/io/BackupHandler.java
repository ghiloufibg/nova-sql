package com.novasql.io;

import com.novasql.DatabaseEngine;
import com.novasql.query.QueryResult;
import com.novasql.schema.Database;
import com.novasql.schema.Record;
import com.novasql.schema.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class BackupHandler {
    private static final Logger logger = LoggerFactory.getLogger(BackupHandler.class);

    private final DatabaseEngine engine;

    public BackupHandler(DatabaseEngine engine) {
        this.engine = engine;
    }

    public void exportDatabase(String filePath) throws IOException {
        logger.info("Exporting database to {}", filePath);

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath))) {
            Database db = engine.getDatabase();

            // Write header
            writer.write("-- Nova SQL Database Export");
            writer.newLine();
            writer.write("-- Database: " + db.getName());
            writer.newLine();
            writer.write("-- Export Date: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            writer.newLine();
            writer.newLine();

            // Export each table
            for (String tableName : db.getTableNames()) {
                exportTable(writer, tableName);
                writer.newLine();
            }

            logger.info("Database export completed");
        }
    }

    private void exportTable(BufferedWriter writer, String tableName) throws IOException {
        Table table = engine.getDatabase().getTable(tableName);

        // Write CREATE TABLE statement
        writer.write("-- Table: " + tableName);
        writer.newLine();
        writer.write("CREATE TABLE " + tableName + " (");
        writer.newLine();

        var columns = table.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            var column = columns.get(i);
            writer.write("    " + column.toString());
            if (i < columns.size() - 1) {
                writer.write(",");
            }
            writer.newLine();
        }

        writer.write(");");
        writer.newLine();
        writer.newLine();

        // Write data
        QueryResult result = engine.executeSQL("SELECT * FROM " + tableName);
        if (result.hasRecords()) {
            writer.write("-- Data for table: " + tableName);
            writer.newLine();

            for (Record record : result.getRecords()) {
                writer.write("INSERT INTO " + tableName + " (");

                String[] columnNames = record.getValues().keySet().toArray(new String[0]);
                for (int i = 0; i < columnNames.length; i++) {
                    writer.write(columnNames[i]);
                    if (i < columnNames.length - 1) {
                        writer.write(", ");
                    }
                }

                writer.write(") VALUES (");

                for (int i = 0; i < columnNames.length; i++) {
                    String value = record.getValue(columnNames[i]);
                    if (value == null) {
                        writer.write("NULL");
                    } else {
                        writer.write("'" + value.replace("'", "''") + "'");
                    }
                    if (i < columnNames.length - 1) {
                        writer.write(", ");
                    }
                }

                writer.write(");");
                writer.newLine();
            }
        }

        // Write indexes
        if (!table.getIndexedColumns().isEmpty()) {
            writer.newLine();
            writer.write("-- Indexes for table: " + tableName);
            writer.newLine();

            for (String indexedColumn : table.getIndexedColumns()) {
                // Skip primary key indexes as they're created automatically
                boolean isPrimaryKey = table.getColumns().stream()
                        .anyMatch(col -> col.getName().equals(indexedColumn) && col.isPrimaryKey());

                if (!isPrimaryKey) {
                    writer.write("CREATE INDEX idx_" + tableName + "_" + indexedColumn +
                            " ON " + tableName + "(" + indexedColumn + ");");
                    writer.newLine();
                }
            }
        }
    }

    public void importDatabase(String filePath) throws IOException {
        logger.info("Importing database from {}", filePath);

        String content = Files.readString(Paths.get(filePath));
        String[] statements = content.split(";");

        int executedStatements = 0;
        for (String statement : statements) {
            String trimmed = statement.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }

            try {
                engine.executeSQL(trimmed + ";");
                executedStatements++;
            } catch (Exception e) {
                logger.warn("Failed to execute statement: {} - {}", trimmed, e.getMessage());
            }
        }

        logger.info("Database import completed, executed {} statements", executedStatements);
    }
}