package com.novasql.ui.tabs;

import com.novasql.ui.editor.EnhancedSqlEditor;
import com.novasql.ui.results.EnhancedResultsTable;

import java.time.LocalDateTime;

/**
 * Holds data and state for a query tab.
 */
public class QueryTabData {
    private String name;
    private String content;
    private boolean pinned = false;
    private boolean unsavedChanges = false;
    private LocalDateTime created;
    private LocalDateTime lastAccessed;

    // UI Components (not serialized)
    private transient EnhancedSqlEditor editor;
    private transient EnhancedResultsTable resultsTable;

    // Constructors
    public QueryTabData() {
        this.created = LocalDateTime.now();
        this.lastAccessed = LocalDateTime.now();
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public boolean hasUnsavedChanges() {
        return unsavedChanges;
    }

    public void setUnsavedChanges(boolean unsavedChanges) {
        this.unsavedChanges = unsavedChanges;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getLastAccessed() {
        return lastAccessed;
    }

    public void setLastAccessed(LocalDateTime lastAccessed) {
        this.lastAccessed = lastAccessed;
    }

    public EnhancedSqlEditor getEditor() {
        return editor;
    }

    public void setEditor(EnhancedSqlEditor editor) {
        this.editor = editor;
    }

    public EnhancedResultsTable getResultsTable() {
        return resultsTable;
    }

    public void setResultsTable(EnhancedResultsTable resultsTable) {
        this.resultsTable = resultsTable;
    }
}