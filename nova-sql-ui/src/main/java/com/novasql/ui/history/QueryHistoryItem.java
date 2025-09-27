package com.novasql.ui.history;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a single query execution in the history.
 */
public class QueryHistoryItem {
    private String id;
    private String query;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime executionTime;

    private long executionDurationMs;
    private boolean success;
    private String errorMessage;
    private int rowsAffected;
    private int queryHash;

    // Constructors
    public QueryHistoryItem() {}

    public QueryHistoryItem(String query, LocalDateTime executionTime, long executionDurationMs,
                           boolean success, String errorMessage, int rowsAffected) {
        this.query = query;
        this.executionTime = executionTime;
        this.executionDurationMs = executionDurationMs;
        this.success = success;
        this.errorMessage = errorMessage;
        this.rowsAffected = rowsAffected;
        this.queryHash = query.hashCode();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
        this.queryHash = query != null ? query.hashCode() : 0;
    }

    public LocalDateTime getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(LocalDateTime executionTime) {
        this.executionTime = executionTime;
    }

    public long getExecutionDurationMs() {
        return executionDurationMs;
    }

    public void setExecutionDurationMs(long executionDurationMs) {
        this.executionDurationMs = executionDurationMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getRowsAffected() {
        return rowsAffected;
    }

    public void setRowsAffected(int rowsAffected) {
        this.rowsAffected = rowsAffected;
    }

    public int getQueryHash() {
        return queryHash;
    }

    public void setQueryHash(int queryHash) {
        this.queryHash = queryHash;
    }

    // Utility methods
    public String getFormattedExecutionTime() {
        if (executionTime == null) return "";
        return executionTime.toString();
    }

    public String getFormattedDuration() {
        if (executionDurationMs < 1000) {
            return executionDurationMs + "ms";
        } else if (executionDurationMs < 60000) {
            return String.format("%.1fs", executionDurationMs / 1000.0);
        } else {
            long minutes = executionDurationMs / 60000;
            long seconds = (executionDurationMs % 60000) / 1000;
            return String.format("%dm %ds", minutes, seconds);
        }
    }

    public String getStatus() {
        return success ? "SUCCESS" : "FAILED";
    }

    public String getShortQuery() {
        if (query == null) return "";
        if (query.length() <= 50) return query;
        return query.substring(0, 47) + "...";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryHistoryItem that = (QueryHistoryItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("QueryHistoryItem{id='%s', query='%s', success=%s, duration=%dms}",
            id, getShortQuery(), success, executionDurationMs);
    }
}