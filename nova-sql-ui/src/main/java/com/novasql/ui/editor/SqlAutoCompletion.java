package com.novasql.ui.editor;

import com.novasql.DatabaseEngine;
import com.novasql.schema.Table;
import com.novasql.query.ColumnDefinition;
import javafx.application.Platform;
import javafx.scene.control.ListView;
import javafx.scene.control.PopupControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.fxmisc.richtext.CodeArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Advanced SQL auto-completion system with IntelliSense capabilities.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>SQL keyword completion</li>
 *   <li>Table and column name suggestions</li>
 *   <li>Function completion with parameter hints</li>
 *   <li>Context-aware suggestions</li>
 *   <li>Smart filtering based on current input</li>
 * </ul>
 */
public class SqlAutoCompletion {
    private static final Logger logger = LoggerFactory.getLogger(SqlAutoCompletion.class);

    // SQL Keywords organized by category
    private static final Set<String> DDL_KEYWORDS = Set.of(
        "CREATE", "ALTER", "DROP", "TRUNCATE", "COMMENT"
    );

    private static final Set<String> DML_KEYWORDS = Set.of(
        "SELECT", "INSERT", "UPDATE", "DELETE", "MERGE"
    );

    private static final Set<String> CLAUSE_KEYWORDS = Set.of(
        "FROM", "WHERE", "GROUP", "BY", "HAVING", "ORDER", "LIMIT", "OFFSET",
        "JOIN", "INNER", "LEFT", "RIGHT", "OUTER", "FULL", "CROSS", "ON", "USING"
    );

    private static final Set<String> FUNCTION_KEYWORDS = Set.of(
        "COUNT", "SUM", "AVG", "MIN", "MAX", "DISTINCT", "CASE", "WHEN", "THEN", "ELSE", "END",
        "SUBSTRING", "CONCAT", "LENGTH", "UPPER", "LOWER", "TRIM", "REPLACE",
        "NOW", "CURDATE", "CURTIME", "YEAR", "MONTH", "DAY", "HOUR", "MINUTE", "SECOND"
    );

    private static final Set<String> DATA_TYPE_KEYWORDS = Set.of(
        "INT", "INTEGER", "VARCHAR", "CHAR", "TEXT", "DATE", "DATETIME", "TIMESTAMP",
        "BOOLEAN", "DECIMAL", "FLOAT", "DOUBLE", "BIGINT", "SMALLINT", "TINYINT"
    );

    private static final Set<String> CONSTRAINT_KEYWORDS = Set.of(
        "PRIMARY", "FOREIGN", "KEY", "REFERENCES", "UNIQUE", "NOT", "NULL", "DEFAULT",
        "CHECK", "CONSTRAINT", "AUTO_INCREMENT"
    );

    private static final Set<String> LOGICAL_KEYWORDS = Set.of(
        "AND", "OR", "NOT", "IN", "EXISTS", "BETWEEN", "LIKE", "IS", "AS"
    );

    private static final Set<String> ALL_KEYWORDS;
    static {
        Set<String> allKeywords = new HashSet<>();
        allKeywords.addAll(DDL_KEYWORDS);
        allKeywords.addAll(DML_KEYWORDS);
        allKeywords.addAll(CLAUSE_KEYWORDS);
        allKeywords.addAll(FUNCTION_KEYWORDS);
        allKeywords.addAll(DATA_TYPE_KEYWORDS);
        allKeywords.addAll(CONSTRAINT_KEYWORDS);
        allKeywords.addAll(LOGICAL_KEYWORDS);
        ALL_KEYWORDS = Collections.unmodifiableSet(allKeywords);
    }

    private final CodeArea codeArea;
    private final DatabaseEngine databaseEngine;
    private final CompletionPopup completionPopup;
    private final List<CompletionItem> cachedCompletions = new ArrayList<>();
    private boolean showingPopup = false;

    public SqlAutoCompletion(CodeArea codeArea, DatabaseEngine databaseEngine) {
        this.codeArea = codeArea;
        this.databaseEngine = databaseEngine;
        this.completionPopup = new CompletionPopup();

        setupAutoCompletion();
        refreshSchemaCompletions();
    }

    private void setupAutoCompletion() {
        codeArea.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        codeArea.addEventFilter(KeyEvent.KEY_TYPED, this::handleKeyTyped);

        // Hide popup when focus is lost
        codeArea.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                hideCompletionPopup();
            }
        });
    }

    private void handleKeyPressed(KeyEvent event) {
        if (showingPopup) {
            switch (event.getCode()) {
                case ESCAPE:
                    hideCompletionPopup();
                    event.consume();
                    break;
                case ENTER:
                case TAB:
                    if (completionPopup.hasSelection()) {
                        applySelectedCompletion();
                        event.consume();
                    }
                    break;
                case UP:
                    completionPopup.selectPrevious();
                    event.consume();
                    break;
                case DOWN:
                    completionPopup.selectNext();
                    event.consume();
                    break;
            }
        } else {
            // Trigger auto-completion
            if (event.isControlDown() && event.getCode() == KeyCode.SPACE) {
                showCompletions();
                event.consume();
            }
        }
    }

    private void handleKeyTyped(KeyEvent event) {
        String character = event.getCharacter();

        // Auto-trigger on certain characters
        if (".".equals(character) || " ".equals(character)) {
            Platform.runLater(() -> showCompletions());
        } else if (showingPopup && !character.matches("[a-zA-Z0-9_]")) {
            // Hide popup on non-alphanumeric characters
            hideCompletionPopup();
        } else if (showingPopup) {
            // Filter existing completions
            Platform.runLater(() -> filterCompletions());
        }
    }

    private void showCompletions() {
        int caretPosition = codeArea.getCaretPosition();
        String text = codeArea.getText();

        // Get current word being typed
        String currentWord = getCurrentWord(text, caretPosition);
        SqlContext context = analyzeContext(text, caretPosition);

        List<CompletionItem> completions = generateCompletions(currentWord, context);

        if (!completions.isEmpty()) {
            completionPopup.show(completions, currentWord);
            showingPopup = true;
        }
    }

    private void filterCompletions() {
        if (!showingPopup) return;

        int caretPosition = codeArea.getCaretPosition();
        String text = codeArea.getText();
        String currentWord = getCurrentWord(text, caretPosition);

        completionPopup.filter(currentWord);
    }

    private String getCurrentWord(String text, int caretPosition) {
        int start = caretPosition;
        int end = caretPosition;

        // Find word boundaries
        while (start > 0 && Character.isJavaIdentifierPart(text.charAt(start - 1))) {
            start--;
        }
        while (end < text.length() && Character.isJavaIdentifierPart(text.charAt(end))) {
            end++;
        }

        return text.substring(start, caretPosition);
    }

    private SqlContext analyzeContext(String text, int caretPosition) {
        String textBeforeCaret = text.substring(0, caretPosition).toUpperCase();

        SqlContext context = new SqlContext();

        // Determine if we're in a SELECT statement
        if (textBeforeCaret.contains("SELECT")) {
            context.inSelectStatement = true;

            // Check if we're after FROM
            int lastFromIndex = textBeforeCaret.lastIndexOf("FROM");
            int lastSelectIndex = textBeforeCaret.lastIndexOf("SELECT");

            if (lastFromIndex > lastSelectIndex) {
                context.afterFrom = true;

                // Extract table name after FROM
                String afterFrom = textBeforeCaret.substring(lastFromIndex + 4).trim();
                String[] words = afterFrom.split("\\s+");
                if (words.length > 0 && !words[0].isEmpty()) {
                    context.currentTable = words[0];
                }
            }

            // Check if we're after WHERE
            int lastWhereIndex = textBeforeCaret.lastIndexOf("WHERE");
            if (lastWhereIndex > lastFromIndex) {
                context.afterWhere = true;
            }
        }

        // Check for JOIN context
        if (textBeforeCaret.matches(".*\\b(JOIN|INNER JOIN|LEFT JOIN|RIGHT JOIN)\\s*$")) {
            context.afterJoin = true;
        }

        // Check for column context (after SELECT or WHERE)
        if (textBeforeCaret.endsWith(".")) {
            context.expectingColumn = true;
            // Extract table alias or name before the dot
            Pattern pattern = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)\\.$");
            Matcher matcher = pattern.matcher(textBeforeCaret);
            if (matcher.find()) {
                context.tableForColumns = matcher.group(1);
            }
        }

        return context;
    }

    private List<CompletionItem> generateCompletions(String currentWord, SqlContext context) {
        List<CompletionItem> completions = new ArrayList<>();
        String upperCurrentWord = currentWord.toUpperCase();

        // Add SQL keywords based on context
        addKeywordCompletions(completions, upperCurrentWord, context);

        // Add table names
        if (context.afterFrom || context.afterJoin || (!context.expectingColumn && !context.afterWhere)) {
            addTableCompletions(completions, currentWord);
        }

        // Add column names
        if (context.expectingColumn && context.tableForColumns != null) {
            addColumnCompletions(completions, currentWord, context.tableForColumns);
        } else if (context.afterWhere || (!context.afterFrom && context.inSelectStatement)) {
            addAllColumnCompletions(completions, currentWord);
        }

        // Add function completions
        addFunctionCompletions(completions, upperCurrentWord);

        // Sort by relevance and name
        completions.sort((a, b) -> {
            if (a.type != b.type) {
                return Integer.compare(a.type.priority, b.type.priority);
            }
            return a.text.compareToIgnoreCase(b.text);
        });

        return completions;
    }

    private void addKeywordCompletions(List<CompletionItem> completions, String currentWord, SqlContext context) {
        Set<String> relevantKeywords = new HashSet<>();

        if (context.inSelectStatement) {
            if (context.afterFrom) {
                relevantKeywords.addAll(Set.of("WHERE", "GROUP", "ORDER", "HAVING", "LIMIT", "JOIN", "INNER", "LEFT", "RIGHT"));
            } else {
                relevantKeywords.addAll(Set.of("FROM", "DISTINCT"));
            }
        } else {
            relevantKeywords.addAll(DML_KEYWORDS);
            relevantKeywords.addAll(DDL_KEYWORDS);
        }

        relevantKeywords.addAll(LOGICAL_KEYWORDS);
        relevantKeywords.addAll(FUNCTION_KEYWORDS);

        for (String keyword : relevantKeywords) {
            if (keyword.startsWith(currentWord)) {
                completions.add(new CompletionItem(keyword, CompletionType.KEYWORD, "SQL Keyword"));
            }
        }
    }

    private void addTableCompletions(List<CompletionItem> completions, String currentWord) {
        if (databaseEngine != null && databaseEngine.isRunning()) {
            try {
                for (String tableName : databaseEngine.getDatabase().getTableNames()) {
                    if (tableName.toLowerCase().startsWith(currentWord.toLowerCase())) {
                        Table table = databaseEngine.getDatabase().getTable(tableName);
                        String description = String.format("Table (%d columns, %d rows)",
                            table.getColumns().size(), table.getRecordCount());
                        completions.add(new CompletionItem(tableName, CompletionType.TABLE, description));
                    }
                }
            } catch (Exception e) {
                logger.debug("Error getting table completions", e);
            }
        }
    }

    private void addColumnCompletions(List<CompletionItem> completions, String currentWord, String tableName) {
        if (databaseEngine != null && databaseEngine.isRunning()) {
            try {
                Table table = databaseEngine.getDatabase().getTable(tableName);
                for (ColumnDefinition column : table.getColumns()) {
                    if (column.getName().toLowerCase().startsWith(currentWord.toLowerCase())) {
                        String description = String.format("%s%s",
                            column.getType(),
                            column.isPrimaryKey() ? " (PRIMARY KEY)" : "");
                        completions.add(new CompletionItem(column.getName(), CompletionType.COLUMN, description));
                    }
                }
            } catch (Exception e) {
                logger.debug("Error getting column completions for table: " + tableName, e);
            }
        }
    }

    private void addAllColumnCompletions(List<CompletionItem> completions, String currentWord) {
        if (databaseEngine != null && databaseEngine.isRunning()) {
            try {
                for (String tableName : databaseEngine.getDatabase().getTableNames()) {
                    Table table = databaseEngine.getDatabase().getTable(tableName);
                    for (ColumnDefinition column : table.getColumns()) {
                        if (column.getName().toLowerCase().startsWith(currentWord.toLowerCase())) {
                            String description = String.format("%s.%s (%s)",
                                tableName, column.getName(), column.getType());
                            completions.add(new CompletionItem(column.getName(), CompletionType.COLUMN, description));
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("Error getting all column completions", e);
            }
        }
    }

    private void addFunctionCompletions(List<CompletionItem> completions, String currentWord) {
        Map<String, String> functions = Map.ofEntries(
            Map.entry("COUNT", "COUNT(*) - Count rows"),
            Map.entry("SUM", "SUM(column) - Sum values"),
            Map.entry("AVG", "AVG(column) - Average value"),
            Map.entry("MIN", "MIN(column) - Minimum value"),
            Map.entry("MAX", "MAX(column) - Maximum value"),
            Map.entry("CONCAT", "CONCAT(str1, str2) - Concatenate strings"),
            Map.entry("SUBSTRING", "SUBSTRING(str, start, length) - Extract substring"),
            Map.entry("LENGTH", "LENGTH(str) - String length"),
            Map.entry("UPPER", "UPPER(str) - Convert to uppercase"),
            Map.entry("LOWER", "LOWER(str) - Convert to lowercase"),
            Map.entry("NOW", "NOW() - Current timestamp")
        );

        for (Map.Entry<String, String> entry : functions.entrySet()) {
            if (entry.getKey().startsWith(currentWord)) {
                completions.add(new CompletionItem(entry.getKey(), CompletionType.FUNCTION, entry.getValue()));
            }
        }
    }

    private void applySelectedCompletion() {
        CompletionItem selected = completionPopup.getSelectedItem();
        if (selected != null) {
            int caretPosition = codeArea.getCaretPosition();
            String text = codeArea.getText();
            String currentWord = getCurrentWord(text, caretPosition);

            // Replace current word with completion
            int wordStart = caretPosition - currentWord.length();
            codeArea.replaceText(wordStart, caretPosition, selected.text);

            // Position caret appropriately
            if (selected.type == CompletionType.FUNCTION) {
                codeArea.moveTo(wordStart + selected.text.length());
                codeArea.insertText(codeArea.getCaretPosition(), "()");
                codeArea.moveTo(codeArea.getCaretPosition() - 1);
            }
        }

        hideCompletionPopup();
    }

    private void hideCompletionPopup() {
        completionPopup.hide();
        showingPopup = false;
    }

    public void refreshSchemaCompletions() {
        // This would be called when schema changes
        cachedCompletions.clear();
    }

    // Helper classes
    private static class SqlContext {
        boolean inSelectStatement = false;
        boolean afterFrom = false;
        boolean afterWhere = false;
        boolean afterJoin = false;
        boolean expectingColumn = false;
        String currentTable = null;
        String tableForColumns = null;
    }

    public static class CompletionItem {
        public final String text;
        public final CompletionType type;
        public final String description;

        public CompletionItem(String text, CompletionType type, String description) {
            this.text = text;
            this.type = type;
            this.description = description;
        }

        @Override
        public String toString() {
            return text + (description != null ? " - " + description : "");
        }
    }

    public enum CompletionType {
        KEYWORD(1, "Keyword"),
        TABLE(2, "Table"),
        COLUMN(3, "Column"),
        FUNCTION(4, "Function");

        public final int priority;
        public final String displayName;

        CompletionType(int priority, String displayName) {
            this.priority = priority;
            this.displayName = displayName;
        }
    }

    // Completion popup implementation
    private class CompletionPopup extends PopupControl {
        private final ListView<CompletionItem> listView;
        private List<CompletionItem> allItems = new ArrayList<>();
        private String currentFilter = "";

        public CompletionPopup() {
            listView = new ListView<>();
            listView.setPrefSize(400, 200);
            listView.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    applySelectedCompletion();
                }
            });

            getScene().setRoot(listView);
            setAutoHide(true);
        }

        public void show(List<CompletionItem> items, String filter) {
            this.allItems = new ArrayList<>(items);
            this.currentFilter = filter;
            updateListView();

            if (!listView.getItems().isEmpty()) {
                listView.getSelectionModel().selectFirst();

                // Position popup near caret
                var bounds = codeArea.getCaretBounds();
                if (bounds.isPresent()) {
                    var caretBounds = bounds.get();
                    show(codeArea, caretBounds.getMinX(), caretBounds.getMaxY());
                }
            }
        }

        public void filter(String newFilter) {
            this.currentFilter = newFilter;
            updateListView();
        }

        private void updateListView() {
            List<CompletionItem> filtered = allItems.stream()
                .filter(item -> item.text.toLowerCase().startsWith(currentFilter.toLowerCase()))
                .collect(Collectors.toList());

            listView.getItems().setAll(filtered);

            if (filtered.isEmpty()) {
                hide();
                showingPopup = false;
            } else {
                listView.getSelectionModel().selectFirst();
            }
        }

        public boolean hasSelection() {
            return listView.getSelectionModel().getSelectedItem() != null;
        }

        public CompletionItem getSelectedItem() {
            return listView.getSelectionModel().getSelectedItem();
        }

        public void selectNext() {
            listView.getSelectionModel().selectNext();
        }

        public void selectPrevious() {
            listView.getSelectionModel().selectPrevious();
        }
    }
}