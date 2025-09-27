package com.novasql;

import com.novasql.audit.AuditLogger;
import com.novasql.cache.QueryCache;
import com.novasql.config.DatabaseConfig;
import com.novasql.io.BackupHandler;
import com.novasql.io.CSVHandler;
import com.novasql.performance.QueryStats;
import com.novasql.query.QueryExecutor;
import com.novasql.query.QueryResult;
import com.novasql.query.PreparedStatement;
import com.novasql.query.SQLParser;
import com.novasql.query.SQLStatement;
import com.novasql.schema.Database;
import com.novasql.storage.BufferPool;
import com.novasql.storage.DiskManager;
import com.novasql.transaction.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The main database engine for Nova SQL Database.
 *
 * <p>This class provides the primary interface for interacting with the Nova SQL database system.
 * It manages the lifecycle of the database engine, coordinates between different subsystems
 * (storage, transactions, query processing), and provides high-level API methods for
 * executing SQL statements and managing database operations.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>ACID transaction support</li>
 *   <li>SQL query execution with comprehensive statement support</li>
 *   <li>Buffer pool management for memory efficiency</li>
 *   <li>Persistent storage with page-based disk management</li>
 *   <li>Performance monitoring and query statistics</li>
 *   <li>Import/export utilities (CSV, SQL backup)</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * DatabaseEngine engine = new DatabaseEngine();
 * engine.start("mydb", "./data");
 *
 * QueryResult result = engine.executeSQL("CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(50))");
 * engine.executeSQL("INSERT INTO users VALUES (1, 'John Doe')");
 *
 * engine.stop();
 * }</pre>
 *
 * @author Nova SQL Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class DatabaseEngine {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseEngine.class);

    private boolean running = false;
    private DatabaseConfig config;
    private DiskManager diskManager;
    private BufferPool bufferPool;
    private TransactionManager transactionManager;
    private Database database;
    private SQLParser sqlParser;
    private QueryExecutor queryExecutor;
    private CSVHandler csvHandler;
    private BackupHandler backupHandler;
    private List<QueryStats> queryHistory;
    private QueryCache queryCache;
    private AuditLogger auditLogger;

    /**
     * Starts the database engine with default settings.
     *
     * <p>Initializes the database engine using default database name "default"
     * and default data directory "./data".</p>
     *
     * @throws RuntimeException if the engine fails to start
     */
    public void start() {
        start("default", "./data");
    }

    /**
     * Starts the database engine with specified settings.
     *
     * <p>Initializes all subsystems including:</p>
     * <ul>
     *   <li>Configuration management</li>
     *   <li>Storage layer (disk manager and buffer pool)</li>
     *   <li>Transaction management</li>
     *   <li>Query processing</li>
     *   <li>Utility handlers (CSV, backup)</li>
     * </ul>
     *
     * @param databaseName the name of the database
     * @param dataDirectory the directory where database files will be stored
     * @throws RuntimeException if the engine fails to start or if already running
     */
    public void start(String databaseName, String dataDirectory) {
        if (running) {
            logger.warn("Database engine is already running");
            return;
        }

        logger.info("Initializing Nova SQL Database Engine");

        try {
            // Initialize configuration
            config = new DatabaseConfig();
            queryHistory = new ArrayList<>();
            queryCache = new QueryCache(1000, 300); // Cache up to 1000 queries for 5 minutes
            auditLogger = new AuditLogger(dataDirectory + "/audit.log");
            auditLogger.start();

            // Initialize core components
            initializeStorage(databaseName, dataDirectory != null ? dataDirectory : config.getDataDirectory());
            initializeTransactionManager();
            initializeDatabase(databaseName);
            initializeQueryProcessor();
            initializeUtilities();

            running = true;
            logger.info("Nova SQL Database Engine initialized successfully");
            logger.debug("Configuration: {}", config);

        } catch (Exception e) {
            logger.error("Failed to start database engine", e);
            throw new RuntimeException("Database engine startup failed", e);
        }
    }

    /**
     * Stops the database engine and cleans up resources.
     *
     * <p>This method performs a graceful shutdown by:</p>
     * <ul>
     *   <li>Flushing all dirty pages to disk</li>
     *   <li>Closing database files</li>
     *   <li>Releasing system resources</li>
     * </ul>
     *
     * <p>It's safe to call this method multiple times.</p>
     */
    public void stop() {
        if (!running) {
            logger.warn("Database engine is not running");
            return;
        }

        logger.info("Shutting down Nova SQL Database Engine");

        try {
            if (bufferPool != null) {
                bufferPool.flushAll();
            }

            if (diskManager != null) {
                diskManager.close();
            }

            if (auditLogger != null) {
                auditLogger.stop();
            }

            running = false;
            logger.info("Nova SQL Database Engine shutdown complete");

        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        }
    }

    /**
     * Executes a SQL statement and returns the result.
     *
     * <p>Supports the following SQL statements:</p>
     * <ul>
     *   <li>CREATE TABLE - Creates new tables</li>
     *   <li>INSERT INTO - Inserts data into tables</li>
     *   <li>SELECT - Queries data from tables</li>
     *   <li>UPDATE - Modifies existing data</li>
     *   <li>DELETE - Removes data from tables</li>
     *   <li>CREATE INDEX - Creates secondary indexes</li>
     * </ul>
     *
     * <p>Each SQL execution is automatically wrapped in a transaction and
     * performance statistics are recorded in the query history.</p>
     *
     * @param sql the SQL statement to execute
     * @return QueryResult containing the execution results
     * @throws IllegalStateException if the database engine is not running
     * @throws RuntimeException if SQL parsing or execution fails
     */
    public QueryResult executeSQL(String sql) {
        if (!running) {
            throw new IllegalStateException("Database engine is not running");
        }

        long startTimeMillis = System.currentTimeMillis();
        Instant startTime = Instant.now();

        try {
            // Check cache for SELECT statements
            QueryResult cachedResult = queryCache.get(sql);
            if (cachedResult != null) {
                logger.debug("Returning cached result for query: {}", sql);
                return cachedResult;
            }

            SQLStatement statement = sqlParser.parse(sql);

            boolean success = false;
            String errorMessage = null;
            QueryResult result = null;

            try {
                result = queryExecutor.execute(statement);
                success = true;

                // Cache SELECT results
                queryCache.put(sql, result);

                // Invalidate cache for data modification statements
                if (statement.getType() != SQLStatement.Type.SELECT) {
                    String tableName = extractTableName(statement);
                    if (tableName != null) {
                        queryCache.invalidateTable(tableName);
                    }
                }

            } catch (Exception e) {
                errorMessage = e.getMessage();
                throw e;
            } finally {
                // Audit log DML operations
                if (statement.getType() != SQLStatement.Type.SELECT) {
                    String operation = statement.getType().name();
                    String tableName = extractTableName(statement);
                    auditLogger.logDML(operation, tableName, sql, "system", success, errorMessage);
                }
            }

            // Record query statistics
            long executionTime = System.currentTimeMillis() - startTimeMillis;
            boolean indexUsed = false; // TODO: Track index usage
            int rowsProcessed = result.getAffectedRows();

            QueryStats stats = new QueryStats(sql, startTime, executionTime,
                    rowsProcessed, indexUsed, "Basic execution plan");
            queryHistory.add(stats);

            // Keep only last 1000 queries
            if (queryHistory.size() > 1000) {
                queryHistory.remove(0);
            }

            return result;

        } catch (Exception e) {
            logger.error("Failed to execute SQL: {}", sql, e);
            throw new RuntimeException("SQL execution failed: " + e.getMessage(), e);
        }
    }

    private String extractTableName(SQLStatement statement) {
        try {
            // Extract table name from different statement types for cache invalidation
            switch (statement.getType()) {
                case INSERT:
                    return ((com.novasql.query.InsertStatement) statement).getTableName();
                case UPDATE:
                    return ((com.novasql.query.UpdateStatement) statement).getTableName();
                case DELETE:
                    return ((com.novasql.query.DeleteStatement) statement).getTableName();
                case CREATE_TABLE:
                    if (statement instanceof com.novasql.query.CreateTableStatement) {
                        return ((com.novasql.query.CreateTableStatement) statement).getTableName();
                    }
                    break;
            }
        } catch (Exception e) {
            logger.debug("Could not extract table name from statement", e);
        }
        return null;
    }

    /**
     * Checks if the database engine is currently running.
     *
     * @return true if the engine is running, false otherwise
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Gets the database instance managed by this engine.
     *
     * @return the Database instance
     */
    public Database getDatabase() {
        return database;
    }

    /**
     * Gets the transaction manager for this engine.
     *
     * @return the TransactionManager instance
     */
    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    /**
     * Gets the configuration settings for this database engine.
     *
     * @return the DatabaseConfig instance
     */
    public DatabaseConfig getConfig() {
        return config;
    }

    /**
     * Gets a copy of the query execution history.
     *
     * <p>The history contains performance statistics for the last 1000 queries
     * including execution time, rows processed, and SQL statements.</p>
     *
     * @return a list of QueryStats representing the query history
     */
    public List<QueryStats> getQueryHistory() {
        return new ArrayList<>(queryHistory);
    }

    /**
     * Gets the CSV handler for import/export operations.
     *
     * @return the CSVHandler instance
     */
    public CSVHandler getCSVHandler() {
        return csvHandler;
    }

    /**
     * Gets the backup handler for database backup/restore operations.
     *
     * @return the BackupHandler instance
     */
    public BackupHandler getBackupHandler() {
        return backupHandler;
    }

    /**
     * Gets the query cache for performance monitoring.
     *
     * @return the QueryCache instance
     */
    public QueryCache getQueryCache() {
        return queryCache;
    }

    /**
     * Prepares a SQL statement for repeated execution with different parameters.
     *
     * @param sql the SQL statement with ? placeholders for parameters
     * @return a PreparedStatement object
     * @throws IllegalStateException if the database engine is not running
     */
    public PreparedStatement prepareStatement(String sql) {
        if (!running) {
            throw new IllegalStateException("Database engine is not running");
        }

        return new PreparedStatement(sql);
    }

    /**
     * Executes a prepared statement.
     *
     * @param preparedStatement the prepared statement to execute
     * @return QueryResult containing the execution results
     * @throws IllegalStateException if the database engine is not running
     */
    public QueryResult executePreparedStatement(PreparedStatement preparedStatement) {
        if (!running) {
            throw new IllegalStateException("Database engine is not running");
        }

        String executableSQL = preparedStatement.getExecutableSQL();
        logger.debug("Executing prepared statement: {}", executableSQL);

        return executeSQL(executableSQL);
    }

    /**
     * Imports data from a CSV file into the specified table.
     *
     * @param filePath the path to the CSV file to import
     * @param tableName the name of the target table
     * @throws IOException if file reading fails
     */
    public void importCSV(String filePath, String tableName) throws IOException {
        csvHandler.importCSV(filePath, tableName);
    }

    /**
     * Exports table data to a CSV file.
     *
     * @param tableName the name of the table to export
     * @param filePath the path where the CSV file will be created
     * @throws IOException if file writing fails
     */
    public void exportCSV(String tableName, String filePath) throws IOException {
        csvHandler.exportCSV(tableName, filePath);
    }

    /**
     * Exports the entire database to a SQL file.
     *
     * <p>The export includes:</p>
     * <ul>
     *   <li>All table definitions (CREATE TABLE statements)</li>
     *   <li>All data (INSERT statements)</li>
     *   <li>All secondary indexes (CREATE INDEX statements)</li>
     * </ul>
     *
     * @param filePath the path where the SQL backup file will be created
     * @throws IOException if file writing fails
     */
    public void exportDatabase(String filePath) throws IOException {
        backupHandler.exportDatabase(filePath);
    }

    /**
     * Imports database structure and data from a SQL file.
     *
     * @param filePath the path to the SQL backup file to import
     * @throws IOException if file reading fails
     */
    public void importDatabase(String filePath) throws IOException {
        backupHandler.importDatabase(filePath);
    }

    private void initializeStorage(String databaseName, String dataDirectory) throws IOException {
        logger.debug("Initializing storage subsystem");
        diskManager = new DiskManager(dataDirectory, databaseName);
        bufferPool = new BufferPool(diskManager, config.getBufferPoolSize());
    }

    private void initializeTransactionManager() {
        logger.debug("Initializing transaction manager");
        transactionManager = new TransactionManager();
    }

    private void initializeDatabase(String databaseName) {
        logger.debug("Initializing database: {}", databaseName);
        database = new Database(databaseName);
    }

    private void initializeQueryProcessor() {
        logger.debug("Initializing query processor");
        sqlParser = new SQLParser();
        queryExecutor = new QueryExecutor(database, transactionManager);
    }

    private void initializeUtilities() {
        logger.debug("Initializing utility handlers");
        csvHandler = new CSVHandler(this);
        backupHandler = new BackupHandler(this);
    }
}