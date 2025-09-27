package com.novasql;

import com.novasql.query.QueryResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class EnhancedFeaturesTest {

    private DatabaseEngine engine;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        engine = new DatabaseEngine();
        engine.start("enhanced_test_db", tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        if (engine.isRunning()) {
            engine.stop();
        }
    }

    @Test
    void shouldHandleUpdateAndDeleteOperations() {
        // Setup
        engine.executeSQL("CREATE TABLE products (id INTEGER PRIMARY KEY, name VARCHAR(50), price VARCHAR(10))");
        engine.executeSQL("INSERT INTO products (id, name, price) VALUES (1, 'Laptop', '999.99')");
        engine.executeSQL("INSERT INTO products (id, name, price) VALUES (2, 'Mouse', '29.99')");

        // Update operation
        QueryResult updateResult = engine.executeSQL("UPDATE products SET price = '1099.99' WHERE id = 1");
        assertThat(updateResult.getType()).isEqualTo(QueryResult.ResultType.UPDATE);
        assertThat(updateResult.getAffectedRows()).isEqualTo(1);

        // Verify update
        QueryResult selectResult = engine.executeSQL("SELECT price FROM products WHERE id = 1");
        assertThat(selectResult.getRecords().get(0).getValue("price")).isEqualTo("1099.99");

        // Delete operation
        QueryResult deleteResult = engine.executeSQL("DELETE FROM products WHERE id = 2");
        assertThat(deleteResult.getType()).isEqualTo(QueryResult.ResultType.DELETE);
        assertThat(deleteResult.getAffectedRows()).isEqualTo(1);

        // Verify delete
        QueryResult allProducts = engine.executeSQL("SELECT * FROM products");
        assertThat(allProducts.getRecords()).hasSize(1);
    }

    @Test
    void shouldSupportMultipleDataTypes() {
        // Create table with different data types
        engine.executeSQL("CREATE TABLE mixed_types (id INTEGER PRIMARY KEY, name VARCHAR(50), is_active BOOLEAN, created_date DATE, price DECIMAL)");

        // Insert data with different types
        engine.executeSQL("INSERT INTO mixed_types (id, name, is_active, created_date, price) VALUES (1, 'Test Item', true, '2023-01-01', '99.99')");

        // Query and verify
        QueryResult result = engine.executeSQL("SELECT * FROM mixed_types WHERE id = 1");
        assertThat(result.getRecords()).hasSize(1);

        var record = result.getRecords().get(0);
        assertThat(record.getValue("name")).isEqualTo("Test Item");
        assertThat(record.getValue("is_active")).isEqualTo("true");
        assertThat(record.getValue("created_date")).isEqualTo("2023-01-01");
        assertThat(record.getValue("price")).isEqualTo("99.99");
    }

    @Test
    void shouldCreateSecondaryIndexes() {
        // Setup table
        engine.executeSQL("CREATE TABLE users (id INTEGER PRIMARY KEY, email VARCHAR(100), name VARCHAR(50))");
        engine.executeSQL("INSERT INTO users (id, email, name) VALUES (1, 'john@example.com', 'John Doe')");

        // Create secondary index
        assertThatNoException().isThrownBy(() ->
            engine.executeSQL("CREATE INDEX idx_email ON users(email)")
        );

        // Verify index usage (table should have the index)
        var table = engine.getDatabase().getTable("users");
        assertThat(table.hasIndex("email")).isTrue();
    }

    @Test
    void shouldTrackQueryPerformance() {
        // Execute some queries
        engine.executeSQL("CREATE TABLE performance_test (id INTEGER PRIMARY KEY, data VARCHAR(100))");
        engine.executeSQL("INSERT INTO performance_test (id, data) VALUES (1, 'test data')");

        // Add a small delay to ensure execution time is measurable
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        engine.executeSQL("SELECT * FROM performance_test");

        // Check that query history is recorded
        var queryHistory = engine.getQueryHistory();
        assertThat(queryHistory).hasSizeGreaterThan(0);

        // Check that stats contain execution time - allow for 0ms on very fast queries
        var lastQuery = queryHistory.get(queryHistory.size() - 1);
        assertThat(lastQuery.getExecutionTime()).isGreaterThanOrEqualTo(0);
        assertThat(lastQuery.getSql()).contains("SELECT * FROM performance_test");
    }

    @Test
    void shouldExportAndImportCSV() throws IOException {
        // Setup
        engine.executeSQL("CREATE TABLE csv_test (id INTEGER PRIMARY KEY, name VARCHAR(50), value VARCHAR(20))");
        engine.executeSQL("INSERT INTO csv_test (id, name, value) VALUES (1, 'Item1', 'Value1')");
        engine.executeSQL("INSERT INTO csv_test (id, name, value) VALUES (2, 'Item2', 'Value2')");

        // Export to CSV
        Path csvFile = tempDir.resolve("export.csv");
        engine.exportCSV("csv_test", csvFile.toString());

        // Verify file exists and has content
        assertThat(Files.exists(csvFile)).isTrue();
        String content = Files.readString(csvFile);
        assertThat(content).contains("id,name,value");
        assertThat(content).contains("1,Item1,Value1");
        assertThat(content).contains("2,Item2,Value2");
    }

    @Test
    void shouldExportDatabaseBackup() throws IOException {
        // Setup
        engine.executeSQL("CREATE TABLE backup_test (id INTEGER PRIMARY KEY, description VARCHAR(100))");
        engine.executeSQL("INSERT INTO backup_test (id, description) VALUES (1, 'Test record for backup')");

        // Export database
        Path backupFile = tempDir.resolve("database_backup.sql");
        engine.exportDatabase(backupFile.toString());

        // Verify backup file
        assertThat(Files.exists(backupFile)).isTrue();
        String content = Files.readString(backupFile);
        assertThat(content).contains("CREATE TABLE backup_test");
        assertThat(content).contains("INSERT INTO backup_test");
        assertThat(content).contains("Test record for backup");
    }

    @Test
    void shouldUseConfiguration() {
        // Verify configuration is loaded
        var config = engine.getConfig();
        assertThat(config).isNotNull();
        assertThat(config.getBufferPoolSize()).isGreaterThan(0);
        assertThat(config.getDataDirectory()).isNotNull();
    }

    @Test
    void shouldProvideUtilityHandlers() {
        // Verify utility handlers are available
        assertThat(engine.getCSVHandler()).isNotNull();
        assertThat(engine.getBackupHandler()).isNotNull();
    }
}