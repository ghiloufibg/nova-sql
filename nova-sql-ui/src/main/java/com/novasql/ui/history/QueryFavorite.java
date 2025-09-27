package com.novasql.ui.history;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a saved favorite query.
 */
public class QueryFavorite {
    private String id;
    private String name;
    private String query;
    private String description;
    private String category;
    private Set<String> tags = new HashSet<>();

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUsed;

    private int usageCount;

    // Constructors
    public QueryFavorite() {}

    public QueryFavorite(String name, String query, String description, String category) {
        this.name = name;
        this.query = query;
        this.description = description;
        this.category = category;
        this.createdAt = LocalDateTime.now();
        this.lastUsed = LocalDateTime.now();
        this.usageCount = 0;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags != null ? tags : new HashSet<>();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(LocalDateTime lastUsed) {
        this.lastUsed = lastUsed;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }

    // Utility methods
    public void addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty()) {
            this.tags.add(tag.trim());
        }
    }

    public void removeTag(String tag) {
        this.tags.remove(tag);
    }

    public String getFormattedTags() {
        return String.join(", ", tags);
    }

    public String getShortQuery() {
        if (query == null) return "";
        if (query.length() <= 100) return query;
        return query.substring(0, 97) + "...";
    }

    public String getFormattedCreatedAt() {
        if (createdAt == null) return "";
        return createdAt.toString();
    }

    public String getFormattedLastUsed() {
        if (lastUsed == null) return "";
        return lastUsed.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryFavorite that = (QueryFavorite) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("QueryFavorite{id='%s', name='%s', category='%s', usageCount=%d}",
            id, name, category, usageCount);
    }
}