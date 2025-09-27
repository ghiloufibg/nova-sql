package com.novasql.ui.util;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL syntax highlighter for RichTextFX CodeArea.
 *
 * <p>Provides syntax highlighting for SQL keywords, strings, comments, and numbers
 * using CSS classes that can be styled in the application stylesheet.</p>
 */
public class SqlSyntaxHighlighter {

    // SQL Keywords
    private static final String[] KEYWORDS = {
        "SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE", "CREATE", "DROP", "ALTER",
        "TABLE", "INDEX", "VIEW", "DATABASE", "SCHEMA", "PRIMARY", "KEY", "FOREIGN",
        "REFERENCES", "CONSTRAINT", "UNIQUE", "NOT", "NULL", "DEFAULT", "AUTO_INCREMENT",
        "INT", "INTEGER", "VARCHAR", "CHAR", "TEXT", "DATE", "DATETIME", "TIMESTAMP",
        "BOOLEAN", "DECIMAL", "FLOAT", "DOUBLE", "BIGINT", "SMALLINT", "TINYINT",
        "AND", "OR", "IN", "LIKE", "BETWEEN", "IS", "AS", "ON", "INNER", "LEFT", "RIGHT",
        "OUTER", "JOIN", "UNION", "GROUP", "BY", "ORDER", "HAVING", "LIMIT", "OFFSET",
        "DISTINCT", "COUNT", "SUM", "AVG", "MIN", "MAX", "EXISTS", "CASE", "WHEN", "THEN",
        "ELSE", "END", "IF", "IFNULL", "COALESCE", "CAST", "CONVERT", "SUBSTRING",
        "CONCAT", "LENGTH", "UPPER", "LOWER", "TRIM", "REPLACE", "NOW", "CURDATE",
        "CURTIME", "YEAR", "MONTH", "DAY", "HOUR", "MINUTE", "SECOND",
        "BEGIN", "COMMIT", "ROLLBACK", "TRANSACTION", "START", "SAVEPOINT",
        "GRANT", "REVOKE", "PRIVILEGES", "USER", "ROLE", "ADMIN", "BACKUP", "RESTORE"
    };

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String STRING_PATTERN = "'([^'\\\\]|\\\\.)*'";
    private static final String COMMENT_PATTERN = "--[^\r\n]*" + "|" + "/\\*(.|\\R)*?\\*/";
    private static final String NUMBER_PATTERN = "\\b\\d+(\\.\\d+)?\\b";

    private static final Pattern PATTERN = Pattern.compile(
        "(?<KEYWORD>" + KEYWORD_PATTERN + ")" +
        "|(?<STRING>" + STRING_PATTERN + ")" +
        "|(?<COMMENT>" + COMMENT_PATTERN + ")" +
        "|(?<NUMBER>" + NUMBER_PATTERN + ")",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Applies SQL syntax highlighting to the given CodeArea.
     *
     * @param codeArea The CodeArea to apply syntax highlighting to
     */
    public static void applySyntaxHighlighting(CodeArea codeArea) {
        codeArea.richChanges()
            .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
            .subscribe(change -> {
                codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText()));
            });
    }

    /**
     * Computes syntax highlighting for the given SQL text.
     *
     * @param text The SQL text to highlight
     * @return StyleSpans containing the highlighting information
     */
    public static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        while (matcher.find()) {
            String styleClass = null;

            if (matcher.group("KEYWORD") != null) {
                styleClass = "sql-keyword";
            } else if (matcher.group("STRING") != null) {
                styleClass = "sql-string";
            } else if (matcher.group("COMMENT") != null) {
                styleClass = "sql-comment";
            } else if (matcher.group("NUMBER") != null) {
                styleClass = "sql-number";
            }

            assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }

        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    /**
     * Gets a list of SQL keywords for auto-completion.
     *
     * @return A list of SQL keywords
     */
    public static List<String> getSqlKeywords() {
        return Arrays.asList(KEYWORDS);
    }

    /**
     * Checks if a given word is a SQL keyword.
     *
     * @param word The word to check
     * @return true if the word is a SQL keyword, false otherwise
     */
    public static boolean isSqlKeyword(String word) {
        return Arrays.stream(KEYWORDS)
            .anyMatch(keyword -> keyword.equalsIgnoreCase(word));
    }
}