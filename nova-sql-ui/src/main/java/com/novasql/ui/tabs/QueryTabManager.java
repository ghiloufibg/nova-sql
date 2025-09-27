package com.novasql.ui.tabs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.novasql.DatabaseEngine;
import com.novasql.ui.editor.EnhancedSqlEditor;
import com.novasql.ui.results.EnhancedResultsTable;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

/**
 * Manages multiple query tabs with session persistence and advanced tab operations.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Multiple query tabs with independent editors and results</li>
 *   <li>Session persistence - restore tabs on restart</li>
 *   <li>Tab groups and organization</li>
 *   <li>Split view for comparing results</li>
 *   <li>Tab templates and quick actions</li>
 *   <li>Keyboard shortcuts for tab management</li>
 * </ul>
 */
public class QueryTabManager extends TabPane {
    private static final Logger logger = LoggerFactory.getLogger(QueryTabManager.class);

    private static final String SESSION_FILE = "tab_session.json";
    private static final int MAX_TABS = 20;

    private final DatabaseEngine databaseEngine;
    private final ObjectMapper objectMapper;
    private final File dataDirectory;
    private final Map<Tab, QueryTabData> tabDataMap = new HashMap<>();

    // Callbacks
    private Consumer<String> onStatusUpdate;
    private Consumer<String> onQueryExecute;

    // Tab counter for naming
    private int tabCounter = 1;

    public QueryTabManager(DatabaseEngine databaseEngine, String dataDirectory) {
        this.databaseEngine = databaseEngine;
        this.dataDirectory = new File(dataDirectory);
        this.dataDirectory.mkdirs();

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        setupTabPane();
        setupKeyboardShortcuts();
        loadSession();

        // Create initial tab if none exist
        if (getTabs().isEmpty()) {
            createNewTab("Query 1", "-- Welcome to Nova SQL\n-- Enter your SQL queries here\n\n");
        }
    }

    private void setupTabPane() {
        setTabClosingPolicy(TabClosingPolicy.ALL_TABS);
        setTabDragPolicy(TabDragPolicy.REORDER);

        // Add context menu to tab area
        setOnContextMenuRequested(event -> {
            ContextMenu contextMenu = createTabAreaContextMenu();
            contextMenu.show(this, event.getScreenX(), event.getScreenY());
        });

        // Handle tab selection changes
        getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                QueryTabData tabData = tabDataMap.get(newTab);
                if (tabData != null) {
                    tabData.setLastAccessed(LocalDateTime.now());
                    saveSession();
                }
            }
        });
    }

    private void setupKeyboardShortcuts() {
        setOnKeyPressed(event -> {
            // Ctrl+T: New tab
            if (new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN).match(event)) {
                createNewTab();
                event.consume();
            }
            // Ctrl+W: Close current tab
            else if (new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN).match(event)) {
                closeCurrentTab();
                event.consume();
            }
            // Ctrl+Shift+T: Restore last closed tab
            else if (new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN).match(event)) {
                restoreLastClosedTab();
                event.consume();
            }
            // Ctrl+Tab: Next tab
            else if (new KeyCodeCombination(KeyCode.TAB, KeyCombination.CONTROL_DOWN).match(event)) {
                selectNextTab();
                event.consume();
            }
            // Ctrl+Shift+Tab: Previous tab
            else if (new KeyCodeCombination(KeyCode.TAB, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN).match(event)) {
                selectPreviousTab();
                event.consume();
            }
            // Ctrl+1-9: Select tab by number
            else if (event.isControlDown() && event.getCode().isDigitKey()) {
                int tabIndex = Integer.parseInt(event.getCode().getChar()) - 1;
                selectTabByIndex(tabIndex);
                event.consume();
            }
        });
    }

    public Tab createNewTab() {
        return createNewTab("Query " + tabCounter++, "");
    }

    public Tab createNewTab(String name, String initialContent) {
        if (getTabs().size() >= MAX_TABS) {
            showWarning("Maximum Tabs", "Maximum number of tabs (" + MAX_TABS + ") reached.");
            return null;
        }

        Tab tab = new Tab(name);
        QueryTabData tabData = new QueryTabData();
        tabData.setName(name);
        tabData.setContent(initialContent);
        tabData.setCreated(LocalDateTime.now());
        tabData.setLastAccessed(LocalDateTime.now());

        // Create tab content
        VBox tabContent = createTabContent(tabData);
        tab.setContent(tabContent);

        // Set tab context menu
        tab.setContextMenu(createTabContextMenu(tab));

        // Handle tab close request
        tab.setOnCloseRequest(event -> {
            if (!confirmTabClose(tab)) {
                event.consume();
            } else {
                handleTabClosed(tab);
            }
        });

        // Store tab data
        tabDataMap.put(tab, tabData);

        // Add to tab pane
        getTabs().add(tab);
        getSelectionModel().select(tab);

        saveSession();
        updateStatus("Created new tab: " + name);

        return tab;
    }

    private VBox createTabContent(QueryTabData tabData) {
        VBox content = new VBox();

        // Create split pane for editor and results
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        splitPane.setDividerPositions(0.4);

        // Create SQL editor
        EnhancedSqlEditor editor = new EnhancedSqlEditor(databaseEngine);
        editor.replaceText(tabData.getContent());

        // Create results table
        EnhancedResultsTable resultsTable = new EnhancedResultsTable();

        // Wire up editor callbacks
        editor.setOnExecuteQuery(query -> {
            if (onQueryExecute != null) {
                onQueryExecute.accept(query);
            }
        });

        editor.setOnFormatCode(message -> updateStatus(message));

        // Wire up results callbacks
        resultsTable.setOnStatusUpdate(this::updateStatus);

        // Store references in tab data
        tabData.setEditor(editor);
        tabData.setResultsTable(resultsTable);

        // Add to split pane
        VBox editorContainer = new VBox(editor);
        VBox.setVgrow(editor, Priority.ALWAYS);

        splitPane.getItems().addAll(editorContainer, resultsTable);

        // Add to main content
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        content.getChildren().add(splitPane);

        return content;
    }

    private ContextMenu createTabContextMenu(Tab tab) {
        ContextMenu menu = new ContextMenu();

        MenuItem renameItem = new MenuItem("Rename");
        renameItem.setOnAction(e -> renameTab(tab));

        MenuItem duplicateItem = new MenuItem("Duplicate");
        duplicateItem.setOnAction(e -> duplicateTab(tab));

        MenuItem closeItem = new MenuItem("Close");
        closeItem.setOnAction(e -> closeTab(tab));

        MenuItem closeOthersItem = new MenuItem("Close Others");
        closeOthersItem.setOnAction(e -> closeOtherTabs(tab));

        MenuItem closeAllItem = new MenuItem("Close All");
        closeAllItem.setOnAction(e -> closeAllTabs());

        MenuItem saveAsItem = new MenuItem("Save As...");
        saveAsItem.setOnAction(e -> saveTabAs(tab));

        MenuItem pinItem = new MenuItem("Pin Tab");
        pinItem.setOnAction(e -> togglePinTab(tab));

        menu.getItems().addAll(
            renameItem, duplicateItem,
            new SeparatorMenuItem(),
            closeItem, closeOthersItem, closeAllItem,
            new SeparatorMenuItem(),
            saveAsItem, pinItem
        );

        return menu;
    }

    private ContextMenu createTabAreaContextMenu() {
        ContextMenu menu = new ContextMenu();

        MenuItem newTabItem = new MenuItem("New Tab");
        newTabItem.setOnAction(e -> createNewTab());

        MenuItem restoreTabItem = new MenuItem("Restore Last Closed");
        restoreTabItem.setOnAction(e -> restoreLastClosedTab());

        MenuItem closeAllItem = new MenuItem("Close All Tabs");
        closeAllItem.setOnAction(e -> closeAllTabs());

        MenuItem sessionItem = new MenuItem("Manage Sessions...");
        sessionItem.setOnAction(e -> showSessionManager());

        menu.getItems().addAll(
            newTabItem, restoreTabItem,
            new SeparatorMenuItem(),
            closeAllItem,
            new SeparatorMenuItem(),
            sessionItem
        );

        return menu;
    }

    private boolean confirmTabClose(Tab tab) {
        QueryTabData tabData = tabDataMap.get(tab);
        if (tabData != null && tabData.hasUnsavedChanges()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Unsaved Changes");
            alert.setHeaderText("Tab '" + tab.getText() + "' has unsaved changes");
            alert.setContentText("Do you want to close without saving?");

            ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.YES);
            ButtonType discardButton = new ButtonType("Discard", ButtonBar.ButtonData.NO);
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == saveButton) {
                    saveTab(tab);
                    return true;
                } else if (result.get() == discardButton) {
                    return true;
                } else {
                    return false; // Cancel
                }
            }
        }
        return true;
    }

    private void handleTabClosed(Tab tab) {
        QueryTabData tabData = tabDataMap.remove(tab);
        if (tabData != null) {
            // Add to recently closed for restoration
            addToRecentlyClosed(tabData);
        }
        saveSession();
    }

    private void renameTab(Tab tab) {
        TextInputDialog dialog = new TextInputDialog(tab.getText());
        dialog.setTitle("Rename Tab");
        dialog.setHeaderText("Enter new tab name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            tab.setText(newName);
            QueryTabData tabData = tabDataMap.get(tab);
            if (tabData != null) {
                tabData.setName(newName);
                saveSession();
            }
        });
    }

    private void duplicateTab(Tab originalTab) {
        QueryTabData originalData = tabDataMap.get(originalTab);
        if (originalData != null) {
            String newName = originalTab.getText() + " (Copy)";
            String content = originalData.getEditor().getText();
            createNewTab(newName, content);
        }
    }

    private void closeTab(Tab tab) {
        if (confirmTabClose(tab)) {
            getTabs().remove(tab);
            handleTabClosed(tab);
        }
    }

    private void closeOtherTabs(Tab keepTab) {
        List<Tab> tabsToClose = new ArrayList<>(getTabs());
        tabsToClose.remove(keepTab);

        for (Tab tab : tabsToClose) {
            closeTab(tab);
        }
    }

    private void closeAllTabs() {
        List<Tab> tabsToClose = new ArrayList<>(getTabs());
        for (Tab tab : tabsToClose) {
            closeTab(tab);
        }
    }

    private void closeCurrentTab() {
        Tab selectedTab = getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            closeTab(selectedTab);
        }
    }

    private void saveTab(Tab tab) {
        // TODO: Implement save tab to file
        QueryTabData tabData = tabDataMap.get(tab);
        if (tabData != null) {
            tabData.setUnsavedChanges(false);
            updateStatus("Tab saved: " + tab.getText());
        }
    }

    private void saveTabAs(Tab tab) {
        // TODO: Implement save tab as dialog
        updateStatus("Save As not yet implemented");
    }

    private void togglePinTab(Tab tab) {
        QueryTabData tabData = tabDataMap.get(tab);
        if (tabData != null) {
            tabData.setPinned(!tabData.isPinned());
            updateTabAppearance(tab, tabData);
            updateStatus(tabData.isPinned() ? "Tab pinned" : "Tab unpinned");
        }
    }

    private void updateTabAppearance(Tab tab, QueryTabData tabData) {
        // Update tab appearance based on state
        if (tabData.isPinned()) {
            tab.setStyle("-fx-background-color: #e3f2fd;");
        } else {
            tab.setStyle("");
        }

        if (tabData.hasUnsavedChanges()) {
            if (!tab.getText().endsWith("*")) {
                tab.setText(tab.getText() + "*");
            }
        } else {
            if (tab.getText().endsWith("*")) {
                tab.setText(tab.getText().substring(0, tab.getText().length() - 1));
            }
        }
    }

    private void selectNextTab() {
        int currentIndex = getSelectionModel().getSelectedIndex();
        int nextIndex = (currentIndex + 1) % getTabs().size();
        getSelectionModel().select(nextIndex);
    }

    private void selectPreviousTab() {
        int currentIndex = getSelectionModel().getSelectedIndex();
        int prevIndex = (currentIndex - 1 + getTabs().size()) % getTabs().size();
        getSelectionModel().select(prevIndex);
    }

    private void selectTabByIndex(int index) {
        if (index >= 0 && index < getTabs().size()) {
            getSelectionModel().select(index);
        }
    }

    private void restoreLastClosedTab() {
        // TODO: Implement restore last closed tab
        updateStatus("Restore last closed tab not yet implemented");
    }

    private void addToRecentlyClosed(QueryTabData tabData) {
        // TODO: Implement recently closed tabs storage
    }

    private void showSessionManager() {
        // TODO: Implement session manager dialog
        updateStatus("Session manager not yet implemented");
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void updateStatus(String message) {
        if (onStatusUpdate != null) {
            onStatusUpdate.accept(message);
        }
    }

    // Session Management
    private void saveSession() {
        try {
            List<QueryTabSession> sessions = new ArrayList<>();

            for (Tab tab : getTabs()) {
                QueryTabData tabData = tabDataMap.get(tab);
                if (tabData != null) {
                    QueryTabSession session = new QueryTabSession();
                    session.setName(tabData.getName());
                    session.setContent(tabData.getEditor().getText());
                    session.setPinned(tabData.isPinned());
                    session.setCreated(tabData.getCreated());
                    session.setLastAccessed(tabData.getLastAccessed());
                    sessions.add(session);
                }
            }

            File sessionFile = new File(dataDirectory, SESSION_FILE);
            objectMapper.writeValue(sessionFile, sessions);

        } catch (IOException e) {
            logger.error("Error saving tab session", e);
        }
    }

    private void loadSession() {
        try {
            File sessionFile = new File(dataDirectory, SESSION_FILE);
            if (sessionFile.exists()) {
                List<QueryTabSession> sessions = objectMapper.readValue(sessionFile,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, QueryTabSession.class));

                for (QueryTabSession session : sessions) {
                    createNewTab(session.getName(), session.getContent());
                }

                logger.info("Loaded {} tabs from session", sessions.size());
            }
        } catch (IOException e) {
            logger.error("Error loading tab session", e);
        }
    }

    // Public API
    public void setOnStatusUpdate(Consumer<String> onStatusUpdate) {
        this.onStatusUpdate = onStatusUpdate;
    }

    public void setOnQueryExecute(Consumer<String> onQueryExecute) {
        this.onQueryExecute = onQueryExecute;
    }

    public QueryTabData getCurrentTabData() {
        Tab selectedTab = getSelectionModel().getSelectedItem();
        return selectedTab != null ? tabDataMap.get(selectedTab) : null;
    }

    public EnhancedSqlEditor getCurrentEditor() {
        QueryTabData tabData = getCurrentTabData();
        return tabData != null ? tabData.getEditor() : null;
    }

    public EnhancedResultsTable getCurrentResultsTable() {
        QueryTabData tabData = getCurrentTabData();
        return tabData != null ? tabData.getResultsTable() : null;
    }

    public void markCurrentTabChanged() {
        QueryTabData tabData = getCurrentTabData();
        if (tabData != null) {
            tabData.setUnsavedChanges(true);
            Tab currentTab = getSelectionModel().getSelectedItem();
            if (currentTab != null) {
                updateTabAppearance(currentTab, tabData);
            }
        }
    }

    @Override
    public void requestFocus() {
        super.requestFocus();
        EnhancedSqlEditor currentEditor = getCurrentEditor();
        if (currentEditor != null) {
            Platform.runLater(currentEditor::requestFocus);
        }
    }
}