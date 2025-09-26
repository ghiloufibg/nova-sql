package com.novasql.cli;

import com.novasql.DatabaseEngine;
import com.novasql.query.QueryResult;
import com.novasql.schema.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * Interactive command-line shell for the Nova SQL database engine.
 *
 * <p>The NovaShell provides a user-friendly command-line interface for interacting
 * with the Nova SQL database. It supports both SQL statement execution and
 * administrative commands for database management.</p>
 *
 * <p>Features include:</p>
 * <ul>
 *   <li>Interactive SQL statement execution</li>
 *   <li>Administrative commands (status, tables, describe)</li>
 *   <li>Query result formatting and display</li>
 *   <li>Help system and command documentation</li>
 *   <li>Graceful error handling and user feedback</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * java com.novasql.cli.NovaShell [database_name] [data_directory]
 * }</pre>
 *
 * @author Nova SQL Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class NovaShell {
    private static final Logger logger = LoggerFactory.getLogger(NovaShell.class);

    private final DatabaseEngine engine;
    private boolean running = true;

    public NovaShell(DatabaseEngine engine) {
        this.engine = engine;
    }

    public void startShell() {
        System.out.println("Nova SQL Database Engine");
        System.out.println("Version 1.0.0");
        System.out.println("Type 'help' for commands or 'exit' to quit");
        System.out.println();

        Scanner scanner = new Scanner(System.in);

        while (running) {
            System.out.print("nova-sql> ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            try {
                processCommand(input);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                logger.debug("Command execution error", e);
            }
        }

        scanner.close();
    }

    private void processCommand(String input) {
        String lowerInput = input.toLowerCase();

        if (lowerInput.equals("exit") || lowerInput.equals("quit")) {
            System.out.println("Goodbye!");
            running = false;
            return;
        }

        if (lowerInput.equals("help")) {
            showHelp();
            return;
        }

        if (lowerInput.equals("status")) {
            showStatus();
            return;
        }

        if (lowerInput.equals("tables")) {
            showTables();
            return;
        }

        if (lowerInput.startsWith("desc ")) {
            String tableName = input.substring(5).trim();
            describeTable(tableName);
            return;
        }

        // Execute SQL
        executeSql(input);
    }

    private void showHelp() {
        System.out.println("Available commands:");
        System.out.println("  help          - Show this help message");
        System.out.println("  status        - Show database status");
        System.out.println("  tables        - List all tables");
        System.out.println("  desc <table>  - Describe table structure");
        System.out.println("  exit/quit     - Exit the shell");
        System.out.println();
        System.out.println("SQL commands:");
        System.out.println("  CREATE TABLE table_name (column_definitions)");
        System.out.println("  INSERT INTO table_name (columns) VALUES (values)");
        System.out.println("  SELECT columns FROM table_name [WHERE condition]");
        System.out.println("  UPDATE table_name SET column=value [WHERE condition]");
        System.out.println("  DELETE FROM table_name [WHERE condition]");
        System.out.println();
    }

    private void showStatus() {
        System.out.println("Database Status:");
        System.out.println("  Engine: " + (engine.isRunning() ? "Running" : "Stopped"));
        System.out.println("  Database: " + engine.getDatabase().getName());
        System.out.println("  Tables: " + engine.getDatabase().getTableCount());
        System.out.println("  Active Transactions: " + engine.getTransactionManager().getActiveTransactionCount());
        System.out.println();
    }

    private void showTables() {
        System.out.println("Tables:");
        for (String tableName : engine.getDatabase().getTableNames()) {
            var table = engine.getDatabase().getTable(tableName);
            System.out.println("  " + tableName + " (" + table.getRecordCount() + " rows)");
        }
        System.out.println();
    }

    private void describeTable(String tableName) {
        try {
            var table = engine.getDatabase().getTable(tableName);
            System.out.println("Table: " + tableName);
            System.out.println("Columns:");
            for (var column : table.getColumns()) {
                String keyInfo = column.isPrimaryKey() ? " (PRIMARY KEY)" : "";
                System.out.println("  " + column.getName() + " " + column.getType() + keyInfo);
            }
            System.out.println("Indexes:");
            for (String indexedColumn : table.getIndexedColumns()) {
                System.out.println("  " + indexedColumn);
            }
            System.out.println("Records: " + table.getRecordCount());
            System.out.println();
        } catch (IllegalArgumentException e) {
            System.err.println("Table '" + tableName + "' does not exist");
        }
    }

    private void executeSql(String sql) {
        long startTime = System.currentTimeMillis();

        try {
            QueryResult result = engine.executeSQL(sql);
            long executionTime = System.currentTimeMillis() - startTime;

            displayResult(result, executionTime);

        } catch (Exception e) {
            System.err.println("SQL Error: " + e.getMessage());
        }
    }

    private void displayResult(QueryResult result, long executionTime) {
        switch (result.getType()) {
            case SELECT:
                if (result.hasRecords()) {
                    displaySelectResult(result);
                } else {
                    System.out.println("No records found");
                }
                break;

            case INSERT:
                System.out.println(result.getAffectedRows() + " row(s) inserted");
                break;

            case UPDATE:
                System.out.println(result.getAffectedRows() + " row(s) updated");
                break;

            case DELETE:
                System.out.println(result.getAffectedRows() + " row(s) deleted");
                break;

            case CREATE_TABLE:
                System.out.println("Table created: " + result.getMessage());
                break;

            default:
                System.out.println("Query executed successfully");
                break;
        }

        System.out.println("(" + executionTime + "ms)");
        System.out.println();
    }

    private void displaySelectResult(QueryResult result) {
        var records = result.getRecords();
        if (records.isEmpty()) {
            return;
        }

        // Get column names from first record
        var firstRecord = records.get(0);
        var columnNames = firstRecord.getValues().keySet().toArray(new String[0]);

        // Print header
        for (String column : columnNames) {
            System.out.printf("%-20s", column);
        }
        System.out.println();

        // Print separator
        for (int i = 0; i < columnNames.length; i++) {
            System.out.print("--------------------");
        }
        System.out.println();

        // Print data
        for (Record record : records) {
            for (String column : columnNames) {
                String value = record.getValue(column);
                System.out.printf("%-20s", value != null ? value : "NULL");
            }
            System.out.println();
        }

        System.out.println(records.size() + " row(s) returned");
    }

    public static void main(String[] args) {
        DatabaseEngine engine = new DatabaseEngine();

        String dbName = args.length > 0 ? args[0] : "nova_shell";
        String dataDir = args.length > 1 ? args[1] : "./data";

        engine.start(dbName, dataDir);

        NovaShell shell = new NovaShell(engine);

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down database...");
            engine.stop();
        }));

        shell.startShell();
        engine.stop();
    }
}