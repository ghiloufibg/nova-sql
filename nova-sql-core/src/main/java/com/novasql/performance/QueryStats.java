package com.novasql.performance;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public class QueryStats {
    private static final AtomicLong queryCounter = new AtomicLong(0);

    private final long queryId;
    private final String sql;
    private final Instant startTime;
    private final long executionTime;
    private final int rowsProcessed;
    private final boolean indexUsed;
    private final String executionPlan;

    public QueryStats(String sql, Instant startTime, long executionTime,
                     int rowsProcessed, boolean indexUsed, String executionPlan) {
        this.queryId = queryCounter.incrementAndGet();
        this.sql = sql;
        this.startTime = startTime;
        this.executionTime = executionTime;
        this.rowsProcessed = rowsProcessed;
        this.indexUsed = indexUsed;
        this.executionPlan = executionPlan;
    }

    public long getQueryId() {
        return queryId;
    }

    public String getSql() {
        return sql;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public int getRowsProcessed() {
        return rowsProcessed;
    }

    public boolean isIndexUsed() {
        return indexUsed;
    }

    public String getExecutionPlan() {
        return executionPlan;
    }

    @Override
    public String toString() {
        return "QueryStats{" +
                "queryId=" + queryId +
                ", sql='" + sql + '\'' +
                ", executionTime=" + executionTime + "ms" +
                ", rowsProcessed=" + rowsProcessed +
                ", indexUsed=" + indexUsed +
                '}';
    }
}