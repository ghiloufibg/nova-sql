package com.novasql.query;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL statement parser for the Nova SQL database engine.
 *
 * <p>This class provides comprehensive parsing capabilities for SQL statements using
 * regular expressions. It supports parsing of common SQL operations including:</p>
 * <ul>
 *   <li>SELECT statements with WHERE and ORDER BY clauses</li>
 *   <li>INSERT statements with column specifications</li>
 *   <li>UPDATE statements with SET and WHERE clauses</li>
 *   <li>DELETE statements with WHERE conditions</li>
 *   <li>CREATE TABLE statements with column definitions</li>
 *   <li>CREATE INDEX statements</li>
 *   <li>JOIN operations (INNER, LEFT, RIGHT, FULL)</li>
 * </ul>
 *
 * <p>The parser is designed to be case-insensitive and handles basic SQL syntax
 * validation. It converts raw SQL strings into structured SQLStatement objects
 * that can be executed by the query engine.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * SQLParser parser = new SQLParser();
 * SQLStatement statement = parser.parse("SELECT * FROM users WHERE id = 1");
 * }</pre>
 *
 * @author Nova SQL Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class SQLParser {

    private static final Pattern SELECT_PATTERN = Pattern.compile(
            "SELECT\\s+(.*?)\\s+FROM\\s+(\\w+)(?:\\s+WHERE\\s+(.*?))?(?:\\s+ORDER\\s+BY\\s+(.*?))?(?:\\s+LIMIT\\s+(\\d+)(?:\\s+OFFSET\\s+(\\d+))?)?",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern JOIN_PATTERN = Pattern.compile(
            "SELECT\\s+(.*?)\\s+FROM\\s+(\\w+)\\s+(\\w+)?\\s+(?:(INNER|LEFT|RIGHT|FULL)\\s+)?JOIN\\s+(\\w+)\\s+(\\w+)?\\s+ON\\s+(\\w+)\\.(\\w+)\\s*=\\s*(\\w+)\\.(\\w+)(?:\\s+WHERE\\s+(.*?))?",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern INSERT_PATTERN = Pattern.compile(
            "INSERT\\s+INTO\\s+(\\w+)\\s*\\(([^)]+)\\)\\s*VALUES\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile(
            "CREATE\\s+TABLE\\s+(\\w+)\\s*\\((.+)\\)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern UPDATE_PATTERN = Pattern.compile(
            "UPDATE\\s+(\\w+)\\s+SET\\s+(.+?)(?:\\s+WHERE\\s+(.+?))?",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern DELETE_PATTERN = Pattern.compile(
            "DELETE\\s+FROM\\s+(\\w+)(?:\\s+WHERE\\s+(.+?))?",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    /**
     * Parses a SQL statement string into a corresponding SQLStatement object.
     *
     * <p>This method analyzes the input SQL string and determines the statement type,
     * then delegates to the appropriate parsing method. It supports case-insensitive
     * parsing and handles various SQL statement formats.</p>
     *
     * <p>Supported statement types:</p>
     * <ul>
     *   <li>SELECT (including JOINs)</li>
     *   <li>INSERT INTO</li>
     *   <li>UPDATE</li>
     *   <li>DELETE FROM</li>
     *   <li>CREATE TABLE</li>
     *   <li>CREATE INDEX</li>
     * </ul>
     *
     * @param sql the SQL statement string to parse
     * @return a SQLStatement object representing the parsed statement
     * @throws IllegalArgumentException if the SQL statement is null, empty, or invalid
     * @throws UnsupportedOperationException if the SQL statement type is not supported
     */
    public SQLStatement parse(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL statement cannot be null or empty");
        }

        String trimmedSql = sql.trim();

        if (trimmedSql.toUpperCase().startsWith("SELECT")) {
            // Check if it's a JOIN query
            if (trimmedSql.toUpperCase().contains(" JOIN ")) {
                return parseJoin(trimmedSql);
            } else {
                return parseSelect(trimmedSql);
            }
        } else if (trimmedSql.toUpperCase().startsWith("INSERT")) {
            return parseInsert(trimmedSql);
        } else if (trimmedSql.toUpperCase().startsWith("UPDATE")) {
            return parseUpdate(trimmedSql);
        } else if (trimmedSql.toUpperCase().startsWith("DELETE")) {
            return parseDelete(trimmedSql);
        } else if (trimmedSql.toUpperCase().startsWith("CREATE TABLE")) {
            return parseCreateTable(trimmedSql);
        } else if (trimmedSql.toUpperCase().startsWith("CREATE INDEX")) {
            return parseCreateIndex(trimmedSql);
        } else if (trimmedSql.toUpperCase().startsWith("SHOW")) {
            return parseShow(trimmedSql);
        } else if (trimmedSql.toUpperCase().startsWith("EXPLAIN")) {
            return parseExplain(trimmedSql);
        } else if (trimmedSql.toUpperCase().startsWith("VACUUM")) {
            return parseVacuum(trimmedSql);
        } else if (trimmedSql.toUpperCase().startsWith("ANALYZE")) {
            return parseAnalyze(trimmedSql);
        } else {
            throw new UnsupportedOperationException("Unsupported SQL statement: " + trimmedSql);
        }
    }

    private SelectStatement parseSelect(String sql) {
        Matcher matcher = SELECT_PATTERN.matcher(sql);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid SELECT statement: " + sql);
        }

        String columnsStr = matcher.group(1).trim();
        String tableName = matcher.group(2).trim();
        String whereClause = matcher.group(3);
        String orderByClause = matcher.group(4);
        String limitStr = matcher.group(5);
        String offsetStr = matcher.group(6);

        List<String> columns = parseColumns(columnsStr);
        WhereCondition whereCondition = whereClause != null ? parseWhereClause(whereClause) : null;
        List<String> orderBy = orderByClause != null ? parseOrderBy(orderByClause) : new ArrayList<>();
        Integer limit = limitStr != null ? Integer.parseInt(limitStr) : null;
        Integer offset = offsetStr != null ? Integer.parseInt(offsetStr) : null;

        return new SelectStatement(tableName, columns, whereCondition, orderBy, limit, offset);
    }

    private InsertStatement parseInsert(String sql) {
        Matcher matcher = INSERT_PATTERN.matcher(sql);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid INSERT statement: " + sql);
        }

        String tableName = matcher.group(1).trim();
        String columnsStr = matcher.group(2).trim();
        String valuesStr = matcher.group(3).trim();

        List<String> columns = parseColumns(columnsStr);
        List<String> values = parseValues(valuesStr);

        if (columns.size() != values.size()) {
            throw new IllegalArgumentException("Column count doesn't match value count");
        }

        Map<String, String> columnValues = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            columnValues.put(columns.get(i), values.get(i));
        }

        return new InsertStatement(tableName, columnValues);
    }

    private CreateTableStatement parseCreateTable(String sql) {
        Matcher matcher = CREATE_TABLE_PATTERN.matcher(sql);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid CREATE TABLE statement: " + sql);
        }

        String tableName = matcher.group(1).trim();
        String columnsStr = matcher.group(2).trim();

        List<ColumnDefinition> columns = parseColumnDefinitions(columnsStr);

        return new CreateTableStatement(tableName, columns);
    }

    private List<String> parseColumns(String columnsStr) {
        if ("*".equals(columnsStr.trim())) {
            return Arrays.asList("*");
        }

        return Arrays.stream(columnsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private List<String> parseValues(String valuesStr) {
        List<String> values = new ArrayList<>();
        String[] parts = valuesStr.split(",");

        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.startsWith("'") && trimmed.endsWith("'")) {
                values.add(trimmed.substring(1, trimmed.length() - 1));
            } else {
                values.add(trimmed);
            }
        }

        return values;
    }

    private WhereCondition parseWhereClause(String whereClause) {
        whereClause = whereClause.trim();

        // Handle IS NULL and IS NOT NULL
        if (whereClause.toUpperCase().matches(".*\\s+IS\\s+NULL\\s*$")) {
            String column = whereClause.replaceAll("(?i)\\s+IS\\s+NULL\\s*$", "").trim();
            return new WhereCondition(column, "IS NULL", (String) null);
        }
        if (whereClause.toUpperCase().matches(".*\\s+IS\\s+NOT\\s+NULL\\s*$")) {
            String column = whereClause.replaceAll("(?i)\\s+IS\\s+NOT\\s+NULL\\s*$", "").trim();
            return new WhereCondition(column, "IS NOT NULL", (String) null);
        }

        // Handle LIKE and NOT LIKE
        if (whereClause.toUpperCase().contains(" LIKE ")) {
            String[] parts = whereClause.split("(?i)\\s+LIKE\\s+", 2);
            if (parts.length == 2) {
                String column = parts[0].trim();
                String value = parseValue(parts[1].trim());
                return new WhereCondition(column, "LIKE", value);
            }
        }
        if (whereClause.toUpperCase().contains(" NOT LIKE ")) {
            String[] parts = whereClause.split("(?i)\\s+NOT\\s+LIKE\\s+", 2);
            if (parts.length == 2) {
                String column = parts[0].trim();
                String value = parseValue(parts[1].trim());
                return new WhereCondition(column, "NOT LIKE", value);
            }
        }

        // Handle BETWEEN and NOT BETWEEN
        if (whereClause.toUpperCase().contains(" BETWEEN ") && whereClause.toUpperCase().contains(" AND ")) {
            String[] betweenParts = whereClause.split("(?i)\\s+BETWEEN\\s+", 2);
            if (betweenParts.length == 2) {
                String column = betweenParts[0].trim();
                String[] rangeParts = betweenParts[1].split("(?i)\\s+AND\\s+", 2);
                if (rangeParts.length == 2) {
                    String start = parseValue(rangeParts[0].trim());
                    String end = parseValue(rangeParts[1].trim());
                    return new WhereCondition(column, "BETWEEN", start, end);
                }
            }
        }
        if (whereClause.toUpperCase().contains(" NOT BETWEEN ") && whereClause.toUpperCase().contains(" AND ")) {
            String[] betweenParts = whereClause.split("(?i)\\s+NOT\\s+BETWEEN\\s+", 2);
            if (betweenParts.length == 2) {
                String column = betweenParts[0].trim();
                String[] rangeParts = betweenParts[1].split("(?i)\\s+AND\\s+", 2);
                if (rangeParts.length == 2) {
                    String start = parseValue(rangeParts[0].trim());
                    String end = parseValue(rangeParts[1].trim());
                    return new WhereCondition(column, "NOT BETWEEN", start, end);
                }
            }
        }

        // Handle IN and NOT IN
        if (whereClause.toUpperCase().contains(" IN ") && whereClause.contains("(") && whereClause.contains(")")) {
            String[] parts = whereClause.split("(?i)\\s+IN\\s+", 2);
            if (parts.length == 2) {
                String column = parts[0].trim();
                String valueList = parts[1].trim();
                if (valueList.startsWith("(") && valueList.endsWith(")")) {
                    String valuesStr = valueList.substring(1, valueList.length() - 1);
                    List<String> values = parseValuesList(valuesStr);
                    return new WhereCondition(column, "IN", values);
                }
            }
        }
        if (whereClause.toUpperCase().contains(" NOT IN ") && whereClause.contains("(") && whereClause.contains(")")) {
            String[] parts = whereClause.split("(?i)\\s+NOT\\s+IN\\s+", 2);
            if (parts.length == 2) {
                String column = parts[0].trim();
                String valueList = parts[1].trim();
                if (valueList.startsWith("(") && valueList.endsWith(")")) {
                    String valuesStr = valueList.substring(1, valueList.length() - 1);
                    List<String> values = parseValuesList(valuesStr);
                    return new WhereCondition(column, "NOT IN", values);
                }
            }
        }

        // Handle basic comparison operators
        for (String op : new String[]{">=", "<=", "!=", "<>", ">", "<", "="}) {
            if (whereClause.contains(op)) {
                String[] parts = whereClause.split(Pattern.quote(op), 2);
                if (parts.length == 2) {
                    String column = parts[0].trim();
                    String value = parseValue(parts[1].trim());
                    return new WhereCondition(column, op, value);
                }
            }
        }

        throw new IllegalArgumentException("Unsupported WHERE clause: " + whereClause);
    }

    private String parseValue(String value) {
        value = value.trim();
        if (value.startsWith("'") && value.endsWith("'")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private List<String> parseValuesList(String valuesStr) {
        List<String> values = new ArrayList<>();
        String[] parts = valuesStr.split(",");

        for (String part : parts) {
            values.add(parseValue(part.trim()));
        }

        return values;
    }

    private List<String> parseOrderBy(String orderByClause) {
        return Arrays.stream(orderByClause.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public static class OrderByColumn {
        private final String columnName;
        private final boolean ascending;

        public OrderByColumn(String columnName, boolean ascending) {
            this.columnName = columnName;
            this.ascending = ascending;
        }

        public String getColumnName() {
            return columnName;
        }

        public boolean isAscending() {
            return ascending;
        }

        @Override
        public String toString() {
            return columnName + (ascending ? " ASC" : " DESC");
        }
    }

    public static List<OrderByColumn> parseOrderByColumns(String orderByClause) {
        if (orderByClause == null || orderByClause.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<OrderByColumn> orderColumns = new ArrayList<>();
        String[] parts = orderByClause.split(",");

        for (String part : parts) {
            String trimmed = part.trim();
            String[] columnParts = trimmed.split("\\s+");
            String columnName = columnParts[0];
            boolean ascending = true;

            if (columnParts.length > 1) {
                String direction = columnParts[1].toUpperCase();
                ascending = !"DESC".equals(direction);
            }

            orderColumns.add(new OrderByColumn(columnName, ascending));
        }

        return orderColumns;
    }

    private List<ColumnDefinition> parseColumnDefinitions(String columnsStr) {
        List<ColumnDefinition> columns = new ArrayList<>();
        String[] parts = columnsStr.split(",");

        for (String part : parts) {
            String trimmed = part.trim();
            String[] columnParts = trimmed.split("\\s+");
            if (columnParts.length >= 2) {
                String name = columnParts[0];
                String type = columnParts[1];
                String upperTrimmed = trimmed.toUpperCase();

                boolean primaryKey = upperTrimmed.contains("PRIMARY KEY");
                boolean autoIncrement = upperTrimmed.contains("AUTO_INCREMENT");
                boolean unique = upperTrimmed.contains("UNIQUE") && !primaryKey;
                boolean notNull = upperTrimmed.contains("NOT NULL") && !primaryKey;

                // Parse DEFAULT value
                String defaultValue = null;
                if (upperTrimmed.contains("DEFAULT")) {
                    String[] defaultParts = trimmed.split("(?i)\\s+DEFAULT\\s+", 2);
                    if (defaultParts.length == 2) {
                        String defaultPart = defaultParts[1].split("\\s")[0];
                        if (defaultPart.startsWith("'") && defaultPart.endsWith("'")) {
                            defaultValue = defaultPart.substring(1, defaultPart.length() - 1);
                        } else {
                            defaultValue = defaultPart;
                        }
                    }
                }

                columns.add(new ColumnDefinition(name, type, primaryKey, notNull, defaultValue, autoIncrement, unique));
            }
        }

        return columns;
    }

    private UpdateStatement parseUpdate(String sql) {
        Matcher matcher = UPDATE_PATTERN.matcher(sql);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid UPDATE statement: " + sql);
        }

        String tableName = matcher.group(1).trim();
        String setClause = matcher.group(2).trim();
        String whereClause = matcher.group(3);

        Map<String, String> updates = parseSetClause(setClause);
        WhereCondition whereCondition = whereClause != null ? parseWhereClause(whereClause) : null;

        return new UpdateStatement(tableName, updates, whereCondition);
    }

    private DeleteStatement parseDelete(String sql) {
        Matcher matcher = DELETE_PATTERN.matcher(sql);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid DELETE statement: " + sql);
        }

        String tableName = matcher.group(1).trim();
        String whereClause = matcher.group(2);

        WhereCondition whereCondition = whereClause != null ? parseWhereClause(whereClause) : null;

        return new DeleteStatement(tableName, whereCondition);
    }

    private Map<String, String> parseSetClause(String setClause) {
        Map<String, String> updates = new HashMap<>();
        String[] assignments = setClause.split(",");

        for (String assignment : assignments) {
            String[] parts = assignment.split("=", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid SET clause: " + assignment);
            }

            String column = parts[0].trim();
            String value = parts[1].trim();
            if (value.startsWith("'") && value.endsWith("'")) {
                value = value.substring(1, value.length() - 1);
            }

            updates.put(column, value);
        }

        return updates;
    }

    private JoinStatement parseJoin(String sql) {
        Matcher matcher = JOIN_PATTERN.matcher(sql);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid JOIN statement: " + sql);
        }

        String columnsStr = matcher.group(1).trim();
        String leftTable = matcher.group(2).trim();
        String leftAlias = matcher.group(3); // Optional alias
        String joinTypeStr = matcher.group(4); // Optional join type
        String rightTable = matcher.group(5).trim();
        String rightAlias = matcher.group(6); // Optional alias
        String leftJoinTable = matcher.group(7).trim();
        String leftJoinColumn = matcher.group(8).trim();
        String rightJoinTable = matcher.group(9).trim();
        String rightJoinColumn = matcher.group(10).trim();
        String whereClause = matcher.group(11);

        List<String> columns = parseColumns(columnsStr);
        JoinStatement.JoinType joinType = parseJoinType(joinTypeStr);
        WhereCondition whereCondition = whereClause != null ? parseWhereClause(whereClause) : null;

        return new JoinStatement(columns, leftTable, rightTable,
                leftJoinColumn, rightJoinColumn, joinType, whereCondition);
    }

    private JoinStatement.JoinType parseJoinType(String joinTypeStr) {
        if (joinTypeStr == null || joinTypeStr.trim().isEmpty()) {
            return JoinStatement.JoinType.INNER;
        }

        switch (joinTypeStr.toUpperCase().trim()) {
            case "INNER":
                return JoinStatement.JoinType.INNER;
            case "LEFT":
                return JoinStatement.JoinType.LEFT;
            case "RIGHT":
                return JoinStatement.JoinType.RIGHT;
            case "FULL":
                return JoinStatement.JoinType.FULL;
            default:
                return JoinStatement.JoinType.INNER;
        }
    }

    private CreateIndexStatement parseCreateIndex(String sql) {
        // CREATE INDEX idx_name ON table_name(column_name)
        Pattern pattern = Pattern.compile(
            "CREATE\\s+INDEX\\s+(\\w+)\\s+ON\\s+(\\w+)\\s*\\(\\s*(\\w+)\\s*\\)",
            Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(sql);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid CREATE INDEX statement: " + sql);
        }

        String indexName = matcher.group(1).trim();
        String tableName = matcher.group(2).trim();
        String columnName = matcher.group(3).trim();

        return new CreateIndexStatement(indexName, tableName, columnName);
    }

    private ShowStatement parseShow(String sql) {
        String upperSql = sql.toUpperCase().trim();

        if (upperSql.equals("SHOW TABLES")) {
            return new ShowStatement(ShowStatement.ShowType.TABLES);
        } else if (upperSql.equals("SHOW STATS")) {
            return new ShowStatement(ShowStatement.ShowType.STATS);
        } else if (upperSql.startsWith("SHOW INDEXES")) {
            if (upperSql.contains(" FROM ")) {
                String[] parts = upperSql.split(" FROM ");
                if (parts.length == 2) {
                    String tableName = parts[1].trim();
                    return new ShowStatement(ShowStatement.ShowType.INDEXES, tableName);
                }
            }
            return new ShowStatement(ShowStatement.ShowType.INDEXES);
        } else {
            throw new IllegalArgumentException("Unsupported SHOW statement: " + sql);
        }
    }

    private ExplainStatement parseExplain(String sql) {
        String innerSql = sql.substring(7).trim(); // Remove "EXPLAIN"
        SQLStatement innerStatement = parse(innerSql);
        return new ExplainStatement(innerStatement);
    }

    private VacuumStatement parseVacuum(String sql) {
        String trimmed = sql.trim();
        String[] parts = trimmed.split("\\s+");

        if (parts.length == 1) {
            // VACUUM (full database)
            return new VacuumStatement();
        } else if (parts.length == 2) {
            // VACUUM tablename
            return new VacuumStatement(parts[1]);
        } else {
            throw new IllegalArgumentException("Invalid VACUUM statement: " + sql);
        }
    }

    private AnalyzeStatement parseAnalyze(String sql) {
        String trimmed = sql.trim();
        String[] parts = trimmed.split("\\s+");

        if (parts.length == 1) {
            // ANALYZE (all tables)
            return new AnalyzeStatement();
        } else if (parts.length == 2) {
            // ANALYZE tablename
            return new AnalyzeStatement(parts[1]);
        } else {
            throw new IllegalArgumentException("Invalid ANALYZE statement: " + sql);
        }
    }
}