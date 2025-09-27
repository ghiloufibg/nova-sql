package com.novasql.ui.history;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages query history and favorites with persistent storage.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Persistent query history with execution metadata</li>
 *   <li>Favorites management with tags and categories</li>
 *   <li>Search and filtering capabilities</li>
 *   <li>Import/Export functionality</li>
 *   <li>Usage statistics and analytics</li>
 * </ul>
 */
public class QueryHistoryManager {
    private static final Logger logger = LoggerFactory.getLogger(QueryHistoryManager.class);

    private static final String HISTORY_FILE = "query_history.json";
    private static final String FAVORITES_FILE = "query_favorites.json";
    private static final int MAX_HISTORY_SIZE = 1000;

    private final ObservableList<QueryHistoryItem> historyItems = FXCollections.observableArrayList();
    private final ObservableList<QueryFavorite> favorites = FXCollections.observableArrayList();
    private final ObjectMapper objectMapper;
    private final File dataDirectory;

    public QueryHistoryManager(String dataDirectory) {
        this.dataDirectory = new File(dataDirectory);
        this.dataDirectory.mkdirs();

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        loadHistory();
        loadFavorites();
    }

    // History Management
    public void addToHistory(String query, long executionTimeMs, boolean success, String errorMessage, int rowsAffected) {
        QueryHistoryItem item = new QueryHistoryItem();
        item.setId(UUID.randomUUID().toString());
        item.setQuery(query.trim());
        item.setExecutionTime(LocalDateTime.now());
        item.setExecutionDurationMs(executionTimeMs);
        item.setSuccess(success);
        item.setErrorMessage(errorMessage);
        item.setRowsAffected(rowsAffected);
        item.setQueryHash(query.trim().hashCode());

        // Check for duplicate (same query executed recently)
        boolean isDuplicate = historyItems.stream()
            .filter(h -> h.getQueryHash() == item.getQueryHash())
            .filter(h -> h.getExecutionTime().isAfter(LocalDateTime.now().minusMinutes(5)))
            .findFirst()
            .isPresent();

        if (!isDuplicate) {
            historyItems.add(0, item); // Add to beginning

            // Maintain max size
            if (historyItems.size() > MAX_HISTORY_SIZE) {
                historyItems.remove(historyItems.size() - 1);
            }

            saveHistory();
        }
    }

    public ObservableList<QueryHistoryItem> getHistory() {
        return historyItems;
    }

    public List<QueryHistoryItem> searchHistory(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return new ArrayList<>(historyItems);
        }

        String lowerSearchTerm = searchTerm.toLowerCase();
        return historyItems.stream()
            .filter(item -> item.getQuery().toLowerCase().contains(lowerSearchTerm))
            .collect(Collectors.toList());
    }

    public List<QueryHistoryItem> getHistoryByDateRange(LocalDateTime start, LocalDateTime end) {
        return historyItems.stream()
            .filter(item -> item.getExecutionTime().isAfter(start) && item.getExecutionTime().isBefore(end))
            .collect(Collectors.toList());
    }

    public List<QueryHistoryItem> getFailedQueries() {
        return historyItems.stream()
            .filter(item -> !item.isSuccess())
            .collect(Collectors.toList());
    }

    public List<QueryHistoryItem> getSlowQueries(long thresholdMs) {
        return historyItems.stream()
            .filter(item -> item.getExecutionDurationMs() > thresholdMs)
            .sorted((a, b) -> Long.compare(b.getExecutionDurationMs(), a.getExecutionDurationMs()))
            .collect(Collectors.toList());
    }

    public void clearHistory() {
        historyItems.clear();
        saveHistory();
    }

    public void removeFromHistory(QueryHistoryItem item) {
        historyItems.remove(item);
        saveHistory();
    }

    // Favorites Management
    public void addToFavorites(String query, String name, String description, String category, Set<String> tags) {
        QueryFavorite favorite = new QueryFavorite();
        favorite.setId(UUID.randomUUID().toString());
        favorite.setName(name);
        favorite.setQuery(query.trim());
        favorite.setDescription(description);
        favorite.setCategory(category);
        favorite.setTags(new HashSet<>(tags));
        favorite.setCreatedAt(LocalDateTime.now());
        favorite.setLastUsed(LocalDateTime.now());
        favorite.setUsageCount(0);

        favorites.add(favorite);
        saveFavorites();
    }

    public void updateFavorite(QueryFavorite favorite) {
        int index = favorites.indexOf(favorite);
        if (index != -1) {
            favorites.set(index, favorite);
            saveFavorites();
        }
    }

    public void removeFavorite(QueryFavorite favorite) {
        favorites.remove(favorite);
        saveFavorites();
    }

    public void incrementFavoriteUsage(QueryFavorite favorite) {
        favorite.setUsageCount(favorite.getUsageCount() + 1);
        favorite.setLastUsed(LocalDateTime.now());
        saveFavorites();
    }

    public ObservableList<QueryFavorite> getFavorites() {
        return favorites;
    }

    public List<QueryFavorite> searchFavorites(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return new ArrayList<>(favorites);
        }

        String lowerSearchTerm = searchTerm.toLowerCase();
        return favorites.stream()
            .filter(fav ->
                fav.getName().toLowerCase().contains(lowerSearchTerm) ||
                fav.getQuery().toLowerCase().contains(lowerSearchTerm) ||
                fav.getDescription().toLowerCase().contains(lowerSearchTerm) ||
                fav.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(lowerSearchTerm))
            )
            .collect(Collectors.toList());
    }

    public List<QueryFavorite> getFavoritesByCategory(String category) {
        return favorites.stream()
            .filter(fav -> Objects.equals(fav.getCategory(), category))
            .collect(Collectors.toList());
    }

    public List<QueryFavorite> getFavoritesByTag(String tag) {
        return favorites.stream()
            .filter(fav -> fav.getTags().contains(tag))
            .collect(Collectors.toList());
    }

    public Set<String> getAllCategories() {
        return favorites.stream()
            .map(QueryFavorite::getCategory)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    public Set<String> getAllTags() {
        return favorites.stream()
            .flatMap(fav -> fav.getTags().stream())
            .collect(Collectors.toSet());
    }

    // Analytics
    public QueryAnalytics getAnalytics() {
        QueryAnalytics analytics = new QueryAnalytics();

        analytics.setTotalQueries(historyItems.size());
        analytics.setSuccessfulQueries((int) historyItems.stream().filter(QueryHistoryItem::isSuccess).count());
        analytics.setFailedQueries((int) historyItems.stream().filter(item -> !item.isSuccess()).count());

        if (!historyItems.isEmpty()) {
            analytics.setAverageExecutionTime(
                historyItems.stream()
                    .mapToLong(QueryHistoryItem::getExecutionDurationMs)
                    .average()
                    .orElse(0.0)
            );

            analytics.setFastestQuery(
                historyItems.stream()
                    .min(Comparator.comparing(QueryHistoryItem::getExecutionDurationMs))
                    .orElse(null)
            );

            analytics.setSlowestQuery(
                historyItems.stream()
                    .max(Comparator.comparing(QueryHistoryItem::getExecutionDurationMs))
                    .orElse(null)
            );
        }

        analytics.setTotalFavorites(favorites.size());
        analytics.setMostUsedFavorite(
            favorites.stream()
                .max(Comparator.comparing(QueryFavorite::getUsageCount))
                .orElse(null)
        );

        // Query type distribution
        Map<String, Integer> queryTypes = new HashMap<>();
        for (QueryHistoryItem item : historyItems) {
            String query = item.getQuery().trim().toUpperCase();
            String type = "OTHER";

            if (query.startsWith("SELECT")) type = "SELECT";
            else if (query.startsWith("INSERT")) type = "INSERT";
            else if (query.startsWith("UPDATE")) type = "UPDATE";
            else if (query.startsWith("DELETE")) type = "DELETE";
            else if (query.startsWith("CREATE")) type = "CREATE";
            else if (query.startsWith("ALTER")) type = "ALTER";
            else if (query.startsWith("DROP")) type = "DROP";

            queryTypes.merge(type, 1, Integer::sum);
        }
        analytics.setQueryTypeDistribution(queryTypes);

        return analytics;
    }

    // Import/Export
    public void exportHistory(File file) throws IOException {
        objectMapper.writeValue(file, historyItems);
    }

    public void importHistory(File file) throws IOException {
        List<QueryHistoryItem> importedItems = objectMapper.readValue(file,
            objectMapper.getTypeFactory().constructCollectionType(List.class, QueryHistoryItem.class));

        for (QueryHistoryItem item : importedItems) {
            if (item.getId() == null) {
                item.setId(UUID.randomUUID().toString());
            }
        }

        historyItems.addAll(importedItems);

        // Maintain max size
        while (historyItems.size() > MAX_HISTORY_SIZE) {
            historyItems.remove(historyItems.size() - 1);
        }

        saveHistory();
    }

    public void exportFavorites(File file) throws IOException {
        objectMapper.writeValue(file, favorites);
    }

    public void importFavorites(File file) throws IOException {
        List<QueryFavorite> importedFavorites = objectMapper.readValue(file,
            objectMapper.getTypeFactory().constructCollectionType(List.class, QueryFavorite.class));

        for (QueryFavorite fav : importedFavorites) {
            if (fav.getId() == null) {
                fav.setId(UUID.randomUUID().toString());
            }
        }

        favorites.addAll(importedFavorites);
        saveFavorites();
    }

    // Persistence
    private void loadHistory() {
        File historyFile = new File(dataDirectory, HISTORY_FILE);
        if (historyFile.exists()) {
            try {
                List<QueryHistoryItem> loadedItems = objectMapper.readValue(historyFile,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, QueryHistoryItem.class));
                historyItems.addAll(loadedItems);
                logger.info("Loaded {} query history items", loadedItems.size());
            } catch (IOException e) {
                logger.error("Error loading query history", e);
            }
        }
    }

    private void saveHistory() {
        File historyFile = new File(dataDirectory, HISTORY_FILE);
        try {
            objectMapper.writeValue(historyFile, historyItems);
        } catch (IOException e) {
            logger.error("Error saving query history", e);
        }
    }

    private void loadFavorites() {
        File favoritesFile = new File(dataDirectory, FAVORITES_FILE);
        if (favoritesFile.exists()) {
            try {
                List<QueryFavorite> loadedFavorites = objectMapper.readValue(favoritesFile,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, QueryFavorite.class));
                favorites.addAll(loadedFavorites);
                logger.info("Loaded {} query favorites", loadedFavorites.size());
            } catch (IOException e) {
                logger.error("Error loading query favorites", e);
            }
        }
    }

    private void saveFavorites() {
        File favoritesFile = new File(dataDirectory, FAVORITES_FILE);
        try {
            objectMapper.writeValue(favoritesFile, favorites);
        } catch (IOException e) {
            logger.error("Error saving query favorites", e);
        }
    }
}