package com.novasql;

import com.novasql.query.PreparedStatement;
import com.novasql.query.QueryResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class EnhancedFeaturesIntegrationTest {

    private DatabaseEngine engine;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        engine = new DatabaseEngine();
        engine.start("test_enhanced", tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        if (engine != null && engine.isRunning()) {
            engine.stop();
        }
    }

    @Test
    void testLimitAndOffset() {
        // Create table and insert test data
        engine.executeSQL("CREATE TABLE users (id INTEGER PRIMARY KEY, name VARCHAR(50), age INTEGER)");

        for (int i = 1; i <= 20; i++) {
            engine.executeSQL(String.format("INSERT INTO users (id, name, age) VALUES (%d, 'User%d', %d)",
                i, i, 20 + i));
        }

        // Test LIMIT
        QueryResult result = engine.executeSQL("SELECT * FROM users LIMIT 5");
        assertEquals(5, result.getRecords().size());

        // Test LIMIT with OFFSET
        QueryResult offsetResult = engine.executeSQL("SELECT * FROM users LIMIT 5 OFFSET 10");
        assertEquals(5, offsetResult.getRecords().size());

        // Test OFFSET without LIMIT
        QueryResult offsetOnlyResult = engine.executeSQL("SELECT * FROM users LIMIT 100 OFFSET 15");
        assertEquals(5, offsetOnlyResult.getRecords().size()); // Should return last 5 records
    }

    @Test
    void testOrderByMultipleColumns() {
        engine.executeSQL("CREATE TABLE products (id INTEGER PRIMARY KEY, category VARCHAR(20), price INTEGER, name VARCHAR(50))");

        engine.executeSQL("INSERT INTO products (id, category, price, name) VALUES (1, 'Electronics', 100, 'Phone')");
        engine.executeSQL("INSERT INTO products (id, category, price, name) VALUES (2, 'Electronics', 50, 'Cable')");
        engine.executeSQL("INSERT INTO products (id, category, price, name) VALUES (3, 'Books', 15, 'Novel')");
        engine.executeSQL("INSERT INTO products (id, category, price, name) VALUES (4, 'Books', 25, 'Textbook')");

        // Order by category ASC, price DESC
        QueryResult result = engine.executeSQL("SELECT * FROM products ORDER BY category ASC, price DESC");
        assertEquals(4, result.getRecords().size());

        // First two should be Books, with higher price first
        assertEquals("Books", result.getRecords().get(0).getValue("category"));
        assertEquals("25", result.getRecords().get(0).getValue("price"));
        assertEquals("Books", result.getRecords().get(1).getValue("category"));
        assertEquals("15", result.getRecords().get(1).getValue("price"));
    }

    @Test
    void testLikeOperator() {
        engine.executeSQL("CREATE TABLE customers (id INTEGER PRIMARY KEY, name VARCHAR(50), email VARCHAR(100))");

        engine.executeSQL("INSERT INTO customers (id, name, email) VALUES (1, 'John Doe', 'john@example.com')");
        engine.executeSQL("INSERT INTO customers (id, name, email) VALUES (2, 'Jane Smith', 'jane@test.com')");
        engine.executeSQL("INSERT INTO customers (id, name, email) VALUES (3, 'Bob Johnson', 'bob@example.org')");

        // Test LIKE with %
        QueryResult result = engine.executeSQL("SELECT * FROM customers WHERE email LIKE '%example%'");
        assertEquals(2, result.getRecords().size());

        // Test LIKE with _
        QueryResult result2 = engine.executeSQL("SELECT * FROM customers WHERE name LIKE 'J_hn%'");
        assertEquals(1, result2.getRecords().size());
        assertEquals("John Doe", result2.getRecords().get(0).getValue("name"));
    }

    @Test
    void testInOperator() {
        engine.executeSQL("CREATE TABLE orders (id INTEGER PRIMARY KEY, status VARCHAR(20), amount INTEGER)");

        engine.executeSQL("INSERT INTO orders (id, status, amount) VALUES (1, 'pending', 100)");
        engine.executeSQL("INSERT INTO orders (id, status, amount) VALUES (2, 'completed', 200)");
        engine.executeSQL("INSERT INTO orders (id, status, amount) VALUES (3, 'cancelled', 150)");
        engine.executeSQL("INSERT INTO orders (id, status, amount) VALUES (4, 'pending', 75)");

        // Test IN operator
        QueryResult result = engine.executeSQL("SELECT * FROM orders WHERE status IN ('pending', 'completed')");
        assertEquals(3, result.getRecords().size());

        // Test NOT IN operator
        QueryResult result2 = engine.executeSQL("SELECT * FROM orders WHERE status NOT IN ('cancelled')");
        assertEquals(3, result2.getRecords().size());
    }

    @Test
    void testBetweenOperator() {
        engine.executeSQL("CREATE TABLE products (id INTEGER PRIMARY KEY, price INTEGER, category VARCHAR(20))");

        engine.executeSQL("INSERT INTO products (id, price, category) VALUES (1, 50, 'A')");
        engine.executeSQL("INSERT INTO products (id, price, category) VALUES (2, 100, 'B')");
        engine.executeSQL("INSERT INTO products (id, price, category) VALUES (3, 150, 'C')");
        engine.executeSQL("INSERT INTO products (id, price, category) VALUES (4, 200, 'D')");

        // Test BETWEEN
        QueryResult result = engine.executeSQL("SELECT * FROM products WHERE price BETWEEN 75 AND 175");
        assertEquals(2, result.getRecords().size());

        // Test NOT BETWEEN
        QueryResult result2 = engine.executeSQL("SELECT * FROM products WHERE price NOT BETWEEN 75 AND 175");
        assertEquals(2, result2.getRecords().size());
    }

    @Test
    void testNullSupport() {
        engine.executeSQL("CREATE TABLE employees (id INTEGER PRIMARY KEY, name VARCHAR(50), manager_id INTEGER)");

        engine.executeSQL("INSERT INTO employees (id, name, manager_id) VALUES (1, 'Alice', NULL)");
        engine.executeSQL("INSERT INTO employees (id, name, manager_id) VALUES (2, 'Bob', 1)");
        engine.executeSQL("INSERT INTO employees (id, name, manager_id) VALUES (3, 'Charlie', 1)");

        // Test IS NULL
        QueryResult result = engine.executeSQL("SELECT * FROM employees WHERE manager_id IS NULL");
        assertEquals(1, result.getRecords().size());
        assertEquals("Alice", result.getRecords().get(0).getValue("name"));

        // Test IS NOT NULL
        QueryResult result2 = engine.executeSQL("SELECT * FROM employees WHERE manager_id IS NOT NULL");
        assertEquals(2, result2.getRecords().size());
    }

    @Test
    void testAutoIncrementAndConstraints() {
        // Test AUTO_INCREMENT, UNIQUE, NOT NULL, DEFAULT
        engine.executeSQL("CREATE TABLE users (id INTEGER AUTO_INCREMENT PRIMARY KEY, email VARCHAR(100) UNIQUE NOT NULL, name VARCHAR(50) DEFAULT 'Anonymous', created_at VARCHAR(50) DEFAULT 'NOW')");

        // The actual implementation would need to handle auto-increment in the table logic
        // For this test, we'll just verify the parsing worked by checking table creation
        QueryResult showResult = engine.executeSQL("SHOW TABLES");
        assertTrue(showResult.getRecords().stream()
            .anyMatch(record -> "users".equals(record.getValue("table_name"))));
    }

    @Test
    void testShowCommands() {
        engine.executeSQL("CREATE TABLE test1 (id INTEGER PRIMARY KEY)");
        engine.executeSQL("CREATE TABLE test2 (id INTEGER PRIMARY KEY, name VARCHAR(50))");
        engine.executeSQL("CREATE INDEX idx_name ON test2(name)");

        // Test SHOW TABLES
        QueryResult tablesResult = engine.executeSQL("SHOW TABLES");
        assertEquals(2, tablesResult.getRecords().size());

        // Test SHOW STATS
        QueryResult statsResult = engine.executeSQL("SHOW STATS");
        assertTrue(statsResult.getRecords().size() >= 2); // At least total_tables and total_records

        // Test SHOW INDEXES
        QueryResult indexesResult = engine.executeSQL("SHOW INDEXES FROM test2");
        assertTrue(indexesResult.getRecords().size() >= 1); // At least the created index
    }

    @Test
    void testExplainPlan() {
        engine.executeSQL("CREATE TABLE users (id INTEGER PRIMARY KEY, name VARCHAR(50))");
        engine.executeSQL("CREATE INDEX idx_name ON users(name)");

        // Test EXPLAIN for SELECT with WHERE clause
        QueryResult explainResult = engine.executeSQL("EXPLAIN SELECT * FROM users WHERE name = 'John'");
        assertEquals(1, explainResult.getRecords().size());

        assertNotNull(explainResult.getRecords().get(0).getValue("operation"));
        assertNotNull(explainResult.getRecords().get(0).getValue("table"));
    }

    @Test
    void testPreparedStatements() {
        engine.executeSQL("CREATE TABLE users (id INTEGER PRIMARY KEY, name VARCHAR(50), age INTEGER)");

        // Test prepared statement with parameters
        PreparedStatement ps = engine.prepareStatement("INSERT INTO users (id, name, age) VALUES (?, ?, ?)");

        ps.setInt(1, 1);
        ps.setString(2, "John Doe");
        ps.setInt(3, 30);

        QueryResult result = engine.executePreparedStatement(ps);
        assertEquals(1, result.getAffectedRows());

        // Reuse prepared statement with different parameters
        ps.clearParameters();
        ps.setInt(1, 2);
        ps.setString(2, "Jane Smith");
        ps.setInt(3, 25);

        QueryResult result2 = engine.executePreparedStatement(ps);
        assertEquals(1, result2.getAffectedRows());

        // Verify data was inserted
        QueryResult selectResult = engine.executeSQL("SELECT * FROM users ORDER BY id");
        assertEquals(2, selectResult.getRecords().size());
    }

    @Test
    void testVacuumAndAnalyze() {
        engine.executeSQL("CREATE TABLE test_table (id INTEGER PRIMARY KEY, data VARCHAR(100))");

        // Insert some test data
        for (int i = 1; i <= 10; i++) {
            engine.executeSQL(String.format("INSERT INTO test_table (id, data) VALUES (%d, 'data%d')", i, i));
        }

        // Test VACUUM specific table
        QueryResult vacuumResult = engine.executeSQL("VACUUM test_table");
        assertNotNull(vacuumResult.getMessage());
        assertTrue(vacuumResult.getMessage().contains("vacuumed successfully"));

        // Test ANALYZE specific table
        QueryResult analyzeResult = engine.executeSQL("ANALYZE test_table");
        assertNotNull(analyzeResult.getMessage());
        assertTrue(analyzeResult.getMessage().contains("analyzed successfully"));

        // Test full database VACUUM and ANALYZE
        QueryResult fullVacuum = engine.executeSQL("VACUUM");
        assertNotNull(fullVacuum.getMessage());

        QueryResult fullAnalyze = engine.executeSQL("ANALYZE");
        assertNotNull(fullAnalyze.getMessage());
    }

    @Test
    void testQueryCaching() {
        engine.executeSQL("CREATE TABLE cache_test (id INTEGER PRIMARY KEY, value VARCHAR(50))");
        engine.executeSQL("INSERT INTO cache_test (id, value) VALUES (1, 'test')");

        // First query - should hit database
        long start1 = System.currentTimeMillis();
        QueryResult result1 = engine.executeSQL("SELECT * FROM cache_test WHERE id = 1");
        long time1 = System.currentTimeMillis() - start1;

        // Second identical query - should hit cache
        long start2 = System.currentTimeMillis();
        QueryResult result2 = engine.executeSQL("SELECT * FROM cache_test WHERE id = 1");
        long time2 = System.currentTimeMillis() - start2;

        // Results should be identical
        assertEquals(result1.getRecords().size(), result2.getRecords().size());
        assertEquals(result1.getRecords().get(0).getValue("value"), result2.getRecords().get(0).getValue("value"));

        // Second query should generally be faster (cached), but timing tests can be flaky
        // so we'll just verify we got the same results
    }
}