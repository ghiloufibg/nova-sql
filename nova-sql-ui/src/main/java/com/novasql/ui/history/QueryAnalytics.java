package com.novasql.ui.history;

import java.util.Map;

/**
 * Analytics data for query history and usage patterns.
 */
public class QueryAnalytics {
    private int totalQueries;
    private int successfulQueries;
    private int failedQueries;
    private double averageExecutionTime;
    private QueryHistoryItem fastestQuery;
    private QueryHistoryItem slowestQuery;
    private int totalFavorites;
    private QueryFavorite mostUsedFavorite;
    private Map<String, Integer> queryTypeDistribution;

    // Constructors
    public QueryAnalytics() {}

    // Getters and Setters
    public int getTotalQueries() {
        return totalQueries;
    }

    public void setTotalQueries(int totalQueries) {
        this.totalQueries = totalQueries;
    }

    public int getSuccessfulQueries() {
        return successfulQueries;
    }

    public void setSuccessfulQueries(int successfulQueries) {
        this.successfulQueries = successfulQueries;
    }

    public int getFailedQueries() {
        return failedQueries;
    }

    public void setFailedQueries(int failedQueries) {
        this.failedQueries = failedQueries;
    }

    public double getAverageExecutionTime() {
        return averageExecutionTime;
    }

    public void setAverageExecutionTime(double averageExecutionTime) {
        this.averageExecutionTime = averageExecutionTime;
    }

    public QueryHistoryItem getFastestQuery() {
        return fastestQuery;
    }

    public void setFastestQuery(QueryHistoryItem fastestQuery) {
        this.fastestQuery = fastestQuery;
    }

    public QueryHistoryItem getSlowestQuery() {
        return slowestQuery;
    }

    public void setSlowestQuery(QueryHistoryItem slowestQuery) {
        this.slowestQuery = slowestQuery;
    }

    public int getTotalFavorites() {
        return totalFavorites;
    }

    public void setTotalFavorites(int totalFavorites) {
        this.totalFavorites = totalFavorites;
    }

    public QueryFavorite getMostUsedFavorite() {
        return mostUsedFavorite;
    }

    public void setMostUsedFavorite(QueryFavorite mostUsedFavorite) {
        this.mostUsedFavorite = mostUsedFavorite;
    }

    public Map<String, Integer> getQueryTypeDistribution() {
        return queryTypeDistribution;
    }

    public void setQueryTypeDistribution(Map<String, Integer> queryTypeDistribution) {
        this.queryTypeDistribution = queryTypeDistribution;
    }

    // Utility methods
    public double getSuccessRate() {
        if (totalQueries == 0) return 0.0;
        return (double) successfulQueries / totalQueries * 100.0;
    }

    public double getFailureRate() {
        if (totalQueries == 0) return 0.0;
        return (double) failedQueries / totalQueries * 100.0;
    }

    public String getFormattedAverageExecutionTime() {
        if (averageExecutionTime < 1000) {
            return String.format("%.1fms", averageExecutionTime);
        } else if (averageExecutionTime < 60000) {
            return String.format("%.2fs", averageExecutionTime / 1000.0);
        } else {
            long minutes = (long) (averageExecutionTime / 60000);
            double seconds = (averageExecutionTime % 60000) / 1000.0;
            return String.format("%dm %.1fs", minutes, seconds);
        }
    }
}