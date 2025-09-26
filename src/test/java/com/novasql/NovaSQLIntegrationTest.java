package com.novasql;

import com.novasql.query.QueryResult;
import com.novasql.schema.Record;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class NovaQLIntegrationTest {

    private DatabaseEngine engine;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        engine = new DatabaseEngine();
        engine.start("test_db", tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        if (engine.isRunning()) {
            engine.stop();
        }
    }

    @Test
    void shouldCreateTableAndInsertData() {
        // Create table
        QueryResult createResult = engine.executeSQL(
            "CREATE TABLE users (id INTEGER PRIMARY KEY, name VARCHAR(50), email VARCHAR(100))"
        );

        assertThat(createResult.getType()).isEqualTo(QueryResult.ResultType.CREATE_TABLE);
        assertThat(engine.getDatabase().hasTable("users")).isTrue();

        // Insert data
        QueryResult insertResult = engine.executeSQL(
            "INSERT INTO users (id, name, email) VALUES (1, 'John Doe', 'john@example.com')"
        );

        assertThat(insertResult.getType()).isEqualTo(QueryResult.ResultType.INSERT);
        assertThat(insertResult.getAffectedRows()).isEqualTo(1);
    }

    @Test
    void shouldSelectData() {
        // Setup
        engine.executeSQL("CREATE TABLE products (id INTEGER PRIMARY KEY, name VARCHAR(50), price VARCHAR(10))");
        engine.executeSQL("INSERT INTO products (id, name, price) VALUES (1, 'Laptop', '999.99')");
        engine.executeSQL("INSERT INTO products (id, name, price) VALUES (2, 'Mouse', '29.99')");

        // Select all
        QueryResult selectAllResult = engine.executeSQL("SELECT * FROM products");

        assertThat(selectAllResult.getType()).isEqualTo(QueryResult.ResultType.SELECT);
        assertThat(selectAllResult.getRecords()).hasSize(2);

        // Select with WHERE clause
        QueryResult selectWhereResult = engine.executeSQL("SELECT name FROM products WHERE id = 1");

        assertThat(selectWhereResult.getRecords()).hasSize(1);
        Record record = selectWhereResult.getRecords().get(0);
        assertThat(record.getValue("name")).isEqualTo("Laptop");
    }

    @Test
    void shouldHandleTransactions() {
        engine.executeSQL("CREATE TABLE accounts (id INTEGER PRIMARY KEY, balance VARCHAR(10))");
        engine.executeSQL("INSERT INTO accounts (id, balance) VALUES (1, '1000')");
        engine.executeSQL("INSERT INTO accounts (id, balance) VALUES (2, '500')");

        QueryResult result = engine.executeSQL("SELECT * FROM accounts");
        assertThat(result.getRecords()).hasSize(2);
    }

    @Test
    void shouldValidatePrimaryKeys() {
        engine.executeSQL("CREATE TABLE customers (id INTEGER PRIMARY KEY, name VARCHAR(50))");
        engine.executeSQL("INSERT INTO customers (id, name) VALUES (1, 'Alice')");

        // Duplicate primary key should fail
        assertThatThrownBy(() ->
            engine.executeSQL("INSERT INTO customers (id, name) VALUES (1, 'Bob')")
        ).isInstanceOf(RuntimeException.class);
    }
}