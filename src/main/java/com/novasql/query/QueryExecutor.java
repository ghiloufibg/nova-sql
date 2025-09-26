package com.novasql.query;

import com.novasql.schema.Database;
import com.novasql.schema.Record;
import com.novasql.schema.Table;
import com.novasql.transaction.Transaction;
import com.novasql.transaction.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryExecutor {
    private static final Logger logger = LoggerFactory.getLogger(QueryExecutor.class);

    private final Database database;
    private final TransactionManager transactionManager;

    public QueryExecutor(Database database, TransactionManager transactionManager) {
        this.database = database;
        this.transactionManager = transactionManager;
    }

    public QueryResult execute(SQLStatement statement) {
        Transaction transaction = transactionManager.beginTransaction();
        try {
            QueryResult result = executeWithTransaction(statement, transaction);
            transaction.commit();
            return result;
        } catch (Exception e) {
            transaction.abort();
            throw e;
        }
    }

    public QueryResult executeWithTransaction(SQLStatement statement, Transaction transaction) {
        switch (statement.getType()) {
            case SELECT:
                if (statement instanceof SelectStatement) {
                    return executeSelect((SelectStatement) statement, transaction);
                } else if (statement instanceof ShowStatement) {
                    return executeShow((ShowStatement) statement, transaction);
                } else if (statement instanceof ExplainStatement) {
                    return executeExplain((ExplainStatement) statement, transaction);
                } else {
                    throw new UnsupportedOperationException("Unsupported SELECT statement type: " + statement.getClass());
                }
            case INSERT:
                return executeInsert((InsertStatement) statement, transaction);
            case UPDATE:
                return executeUpdate((UpdateStatement) statement, transaction);
            case DELETE:
                return executeDelete((DeleteStatement) statement, transaction);
            case CREATE_TABLE:
                if (statement instanceof CreateTableStatement) {
                    return executeCreateTable((CreateTableStatement) statement, transaction);
                } else if (statement instanceof CreateIndexStatement) {
                    return executeCreateIndex((CreateIndexStatement) statement, transaction);
                } else if (statement instanceof VacuumStatement) {
                    return executeVacuum((VacuumStatement) statement, transaction);
                } else if (statement instanceof AnalyzeStatement) {
                    return executeAnalyze((AnalyzeStatement) statement, transaction);
                } else {
                    throw new UnsupportedOperationException("Unsupported CREATE statement type: " + statement.getClass());
                }
            default:
                throw new UnsupportedOperationException("Unsupported statement type: " + statement.getType());
        }
    }

    private QueryResult executeSelect(SelectStatement statement, Transaction transaction) {
        logger.debug("Executing SELECT on table: {}", statement.getTableName());

        // Acquire shared lock on table
        String lockResource = "table:" + statement.getTableName();
        if (!transactionManager.acquireSharedLock(transaction.getTransactionId(), lockResource)) {
            throw new RuntimeException("Failed to acquire lock on table: " + statement.getTableName());
        }

        try {
            Table table = database.getTable(statement.getTableName());
            List<Record> records;

            if (statement.getWhereCondition() != null) {
                WhereCondition where = statement.getWhereCondition();
                records = table.selectRecords(statement.getColumns(), where.getColumn(), where.getValue());
            } else {
                records = table.selectRecords(statement.getColumns());
            }

            // Apply ordering if specified
            if (!statement.getOrderByColumns().isEmpty()) {
                records = applyOrdering(records, statement.getOrderByColumns());
            }

            // Apply LIMIT and OFFSET if specified
            records = applyLimitAndOffset(records, statement.getLimit(), statement.getOffset());

            logger.debug("SELECT returned {} records", records.size());
            return new QueryResult(QueryResult.ResultType.SELECT, records);

        } finally {
            transactionManager.releaseLock(transaction.getTransactionId(), lockResource);
        }
    }

    private QueryResult executeInsert(InsertStatement statement, Transaction transaction) {
        logger.debug("Executing INSERT on table: {}", statement.getTableName());

        // Acquire exclusive lock on table
        String lockResource = "table:" + statement.getTableName();
        if (!transactionManager.acquireExclusiveLock(transaction.getTransactionId(), lockResource)) {
            throw new RuntimeException("Failed to acquire lock on table: " + statement.getTableName());
        }

        try {
            Table table = database.getTable(statement.getTableName());
            table.insertRecord(statement.getColumnValues());

            logger.debug("Inserted 1 record into table: {}", statement.getTableName());
            return new QueryResult(QueryResult.ResultType.INSERT, 1);

        } finally {
            transactionManager.releaseLock(transaction.getTransactionId(), lockResource);
        }
    }

    private QueryResult executeCreateTable(CreateTableStatement statement, Transaction transaction) {
        logger.debug("Executing CREATE TABLE: {}", statement.getTableName());

        // Acquire exclusive lock on database schema
        String lockResource = "schema:" + database.getName();
        if (!transactionManager.acquireExclusiveLock(transaction.getTransactionId(), lockResource)) {
            throw new RuntimeException("Failed to acquire lock on database schema");
        }

        try {
            database.createTable(statement.getTableName(), statement.getColumns());

            logger.debug("Created table: {}", statement.getTableName());
            return new QueryResult(QueryResult.ResultType.CREATE_TABLE, statement.getTableName());

        } finally {
            transactionManager.releaseLock(transaction.getTransactionId(), lockResource);
        }
    }

    private List<Record> applyOrdering(List<Record> records, List<SQLParser.OrderByColumn> orderByColumns) {
        if (orderByColumns.isEmpty()) {
            return records;
        }

        List<Record> sortedRecords = new ArrayList<>(records);

        sortedRecords.sort((r1, r2) -> {
            for (SQLParser.OrderByColumn orderCol : orderByColumns) {
                String v1 = r1.getValue(orderCol.getColumnName());
                String v2 = r2.getValue(orderCol.getColumnName());

                int compareResult = 0;
                if (v1 == null && v2 == null) {
                    compareResult = 0;
                } else if (v1 == null) {
                    compareResult = -1;
                } else if (v2 == null) {
                    compareResult = 1;
                } else {
                    compareResult = v1.compareTo(v2);
                }

                if (compareResult != 0) {
                    return orderCol.isAscending() ? compareResult : -compareResult;
                }
            }
            return 0;
        });

        return sortedRecords;
    }

    private List<Record> applyLimitAndOffset(List<Record> records, Integer limit, Integer offset) {
        if (records.isEmpty()) {
            return records;
        }

        int startIndex = offset != null ? offset : 0;
        if (startIndex >= records.size()) {
            return new ArrayList<>();
        }

        int endIndex = records.size();
        if (limit != null) {
            endIndex = Math.min(startIndex + limit, records.size());
        }

        return new ArrayList<>(records.subList(startIndex, endIndex));
    }

    private QueryResult executeUpdate(UpdateStatement statement, Transaction transaction) {
        logger.debug("Executing UPDATE on table: {}", statement.getTableName());

        // Acquire exclusive lock on table
        String lockResource = "table:" + statement.getTableName();
        if (!transactionManager.acquireExclusiveLock(transaction.getTransactionId(), lockResource)) {
            throw new RuntimeException("Failed to acquire lock on table: " + statement.getTableName());
        }

        try {
            Table table = database.getTable(statement.getTableName());
            int updatedRows;

            if (statement.getWhereCondition() != null) {
                WhereCondition where = statement.getWhereCondition();
                updatedRows = table.updateRecords(statement.getUpdates(), where.getColumn(), where.getValue());
            } else {
                updatedRows = table.updateRecords(statement.getUpdates(), null, null);
            }

            logger.debug("Updated {} records in table: {}", updatedRows, statement.getTableName());
            return new QueryResult(QueryResult.ResultType.UPDATE, updatedRows);

        } finally {
            transactionManager.releaseLock(transaction.getTransactionId(), lockResource);
        }
    }

    private QueryResult executeDelete(DeleteStatement statement, Transaction transaction) {
        logger.debug("Executing DELETE on table: {}", statement.getTableName());

        // Acquire exclusive lock on table
        String lockResource = "table:" + statement.getTableName();
        if (!transactionManager.acquireExclusiveLock(transaction.getTransactionId(), lockResource)) {
            throw new RuntimeException("Failed to acquire lock on table: " + statement.getTableName());
        }

        try {
            Table table = database.getTable(statement.getTableName());
            int deletedRows;

            if (statement.getWhereCondition() != null) {
                WhereCondition where = statement.getWhereCondition();
                deletedRows = table.deleteRecords(where.getColumn(), where.getValue());
            } else {
                deletedRows = table.deleteRecords(null, null);
            }

            logger.debug("Deleted {} records from table: {}", deletedRows, statement.getTableName());
            return new QueryResult(QueryResult.ResultType.DELETE, deletedRows);

        } finally {
            transactionManager.releaseLock(transaction.getTransactionId(), lockResource);
        }
    }

    private QueryResult executeCreateIndex(CreateIndexStatement statement, Transaction transaction) {
        logger.debug("Executing CREATE INDEX: {} on table {}", statement.getIndexName(), statement.getTableName());

        // Acquire exclusive lock on table
        String lockResource = "table:" + statement.getTableName();
        if (!transactionManager.acquireExclusiveLock(transaction.getTransactionId(), lockResource)) {
            throw new RuntimeException("Failed to acquire lock on table: " + statement.getTableName());
        }

        try {
            Table table = database.getTable(statement.getTableName());
            table.createIndex(statement.getColumnName());

            logger.debug("Created index {} on table {}", statement.getIndexName(), statement.getTableName());
            return new QueryResult(QueryResult.ResultType.CREATE_TABLE, "Index " + statement.getIndexName() + " created");

        } finally {
            transactionManager.releaseLock(transaction.getTransactionId(), lockResource);
        }
    }

    private QueryResult executeShow(ShowStatement statement, Transaction transaction) {
        logger.debug("Executing SHOW command: {}", statement.getShowType());

        List<Record> records = new ArrayList<>();

        switch (statement.getShowType()) {
            case TABLES:
                // List all table names
                for (String tableName : database.getTableNames()) {
                    Map<String, String> values = new HashMap<>();
                    values.put("table_name", tableName);
                    Record record = new Record(1, values);
                    records.add(record);
                }
                break;

            case INDEXES:
                // List indexes for a specific table or all tables
                if (statement.getTableName() != null) {
                    Table table = database.getTable(statement.getTableName());
                    if (table != null) {
                        for (String indexName : table.getIndexNames()) {
                            Map<String, String> values = new HashMap<>();
                            values.put("table_name", statement.getTableName());
                            values.put("index_name", indexName);
                            Record record = new Record(1, values);
                            records.add(record);
                        }
                    }
                } else {
                    // All indexes from all tables
                    for (String tableName : database.getTableNames()) {
                        Table table = database.getTable(tableName);
                        for (String indexName : table.getIndexNames()) {
                            Map<String, String> values = new HashMap<>();
                            values.put("table_name", tableName);
                            values.put("index_name", indexName);
                            Record record = new Record(1, values);
                            records.add(record);
                        }
                    }
                }
                break;

            case STATS:
                // Show database statistics
                Map<String, String> statsValues = new HashMap<>();
                statsValues.put("statistic", "total_tables");
                statsValues.put("value", String.valueOf(database.getTableNames().size()));
                Record statsRecord = new Record(1, statsValues);
                records.add(statsRecord);

                int totalRecords = 0;
                for (String tableName : database.getTableNames()) {
                    Table table = database.getTable(tableName);
                    totalRecords += table.getRecordCount();
                }

                Map<String, String> recordCountValues = new HashMap<>();
                recordCountValues.put("statistic", "total_records");
                recordCountValues.put("value", String.valueOf(totalRecords));
                Record recordCountRecord = new Record(1, recordCountValues);
                records.add(recordCountRecord);
                break;
        }

        return new QueryResult(QueryResult.ResultType.SELECT, records);
    }

    private QueryResult executeExplain(ExplainStatement statement, Transaction transaction) {
        logger.debug("Executing EXPLAIN for: {}", statement.getInnerStatement().getClass().getSimpleName());

        List<Record> records = new ArrayList<>();
        SQLStatement innerStatement = statement.getInnerStatement();

        Map<String, String> planValues = new HashMap<>();
        planValues.put("step", "1");
        planValues.put("operation", innerStatement.getClass().getSimpleName());

        if (innerStatement instanceof SelectStatement) {
            SelectStatement select = (SelectStatement) innerStatement;
            planValues.put("table", select.getTableName());

            if (select.getWhereCondition() != null) {
                planValues.put("filter", select.getWhereCondition().getColumn() + " " +
                                                select.getWhereCondition().getOperator() + " " +
                                                select.getWhereCondition().getValue());
            }

            if (!select.getOrderByColumns().isEmpty()) {
                StringBuilder orderBy = new StringBuilder();
                for (SQLParser.OrderByColumn col : select.getOrderByColumns()) {
                    if (orderBy.length() > 0) orderBy.append(", ");
                    orderBy.append(col.getColumnName()).append(col.isAscending() ? " ASC" : " DESC");
                }
                planValues.put("ordering", orderBy.toString());
            }

            if (select.hasLimit()) {
                planValues.put("limit", select.getLimit().toString());
            }

            // Estimate index usage (simplified)
            if (select.getWhereCondition() != null) {
                Table table = database.getTable(select.getTableName());
                if (table != null && table.hasIndex(select.getWhereCondition().getColumn())) {
                    planValues.put("index_used", "YES");
                } else {
                    planValues.put("index_used", "NO - FULL TABLE SCAN");
                }
            }
        }

        planValues.put("estimated_cost", "1.0");
        Record planRecord = new Record(1, planValues);
        records.add(planRecord);

        return new QueryResult(QueryResult.ResultType.SELECT, records);
    }

    private QueryResult executeVacuum(VacuumStatement statement, Transaction transaction) {
        logger.debug("Executing VACUUM operation");

        if (statement.isFullVacuum()) {
            // Vacuum entire database
            for (String tableName : database.getTableNames()) {
                Table table = database.getTable(tableName);
                table.vacuum();
            }
            return new QueryResult(QueryResult.ResultType.CREATE_TABLE, "Database vacuumed successfully");
        } else {
            // Vacuum specific table
            Table table = database.getTable(statement.getTableName());
            if (table != null) {
                table.vacuum();
                return new QueryResult(QueryResult.ResultType.CREATE_TABLE,
                    "Table " + statement.getTableName() + " vacuumed successfully");
            } else {
                throw new IllegalArgumentException("Table not found: " + statement.getTableName());
            }
        }
    }

    private QueryResult executeAnalyze(AnalyzeStatement statement, Transaction transaction) {
        logger.debug("Executing ANALYZE operation");

        if (statement.isFullAnalyze()) {
            // Analyze all tables
            int tablesAnalyzed = 0;
            for (String tableName : database.getTableNames()) {
                Table table = database.getTable(tableName);
                table.analyze();
                tablesAnalyzed++;
            }
            return new QueryResult(QueryResult.ResultType.CREATE_TABLE,
                "Analyzed " + tablesAnalyzed + " tables successfully");
        } else {
            // Analyze specific table
            Table table = database.getTable(statement.getTableName());
            if (table != null) {
                table.analyze();
                return new QueryResult(QueryResult.ResultType.CREATE_TABLE,
                    "Table " + statement.getTableName() + " analyzed successfully");
            } else {
                throw new IllegalArgumentException("Table not found: " + statement.getTableName());
            }
        }
    }
}