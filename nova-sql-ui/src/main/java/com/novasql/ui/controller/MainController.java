package com.novasql.ui.controller;

import com.novasql.DatabaseEngine;
import com.novasql.query.QueryResult;
import com.novasql.schema.Record;
import com.novasql.ui.util.SqlSyntaxHighlighter;
import com.novasql.ui.util.SchemaTreeItem;
import com.novasql.ui.visualization.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.chart.Chart;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main controller for the Nova SQL JavaFX application.
 *
 * <p>This controller manages the primary user interface including:</p>
 * <ul>
 *   <li>SQL query editor</li>
 *   <li>Database schema tree</li>
 *   <li>Query results table</li>
 *   <li>Status bar and messaging</li>
 * </ul>
 */
public class MainController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    // FXML Components
    @FXML private VBox root;
    @FXML private MenuBar menuBar;
    @FXML private ToolBar toolBar;
    @FXML private SplitPane mainSplitPane;
    @FXML private SplitPane leftSplitPane;
    @FXML private TreeView<String> schemaTree;
    @FXML private TabPane queryTabPane;
    @FXML private VBox queryEditorContainer;
    @FXML private TableView<ObservableList<String>> resultsTable;
    @FXML private TextArea messagesArea;
    @FXML private Label statusLabel;
    @FXML private Label rowCountLabel;
    @FXML private Label executionTimeLabel;

    // UI Components (programmatically created)
    private CodeArea sqlEditor;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // Dependencies
    private DatabaseEngine databaseEngine;
    private DatabaseAdminController adminController;
    private ChartManager chartManager;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.info("Initializing MainController");

        setupSqlEditor();
        setupSchemaTree();
        setupResultsTable();
        setupMenus();
        setupToolbar();
        setupStatusBar();

        // Add welcome message
        appendMessage("Welcome to Nova SQL Database Engine", MessageType.INFO);
        logger.info("MainController initialized successfully");
    }

    private void setupSqlEditor() {
        // Create SQL code editor
        sqlEditor = new CodeArea();
        sqlEditor.setParagraphGraphicFactory(LineNumberFactory.get(sqlEditor));
        sqlEditor.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 14px;");

        // Apply SQL syntax highlighting
        SqlSyntaxHighlighter.applySyntaxHighlighting(sqlEditor);

        // Add keyboard shortcuts
        sqlEditor.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown()) {
                switch (event.getCode()) {
                    case ENTER:
                        handleExecuteQuery();
                        event.consume();
                        break;
                    case S:
                        // TODO: Save query
                        event.consume();
                        break;
                    case O:
                        // TODO: Open query
                        event.consume();
                        break;
                }
            } else if (event.getCode() == KeyCode.F5) {
                handleExecuteQuery();
                event.consume();
            }
        });

        // Add placeholder text
        sqlEditor.replaceText(0, 0, "-- Welcome to Nova SQL\n-- Enter your SQL queries here\n\nSELECT * FROM users LIMIT 10;");

        // Add to container
        queryEditorContainer.getChildren().add(sqlEditor);
        VBox.setVgrow(sqlEditor, javafx.scene.layout.Priority.ALWAYS);
    }

    private void setupSchemaTree() {
        SchemaTreeItem root = new SchemaTreeItem("Database", SchemaTreeItem.ItemType.DATABASE);
        root.setExpanded(true);

        SchemaTreeItem tablesNode = new SchemaTreeItem("Tables", SchemaTreeItem.ItemType.TABLES_FOLDER);
        SchemaTreeItem viewsNode = new SchemaTreeItem("Views", SchemaTreeItem.ItemType.VIEWS_FOLDER);
        SchemaTreeItem indexesNode = new SchemaTreeItem("Indexes", SchemaTreeItem.ItemType.INDEXES_FOLDER);

        root.getChildren().addAll(tablesNode, viewsNode, indexesNode);
        schemaTree.setRoot(root);

        // Add double-click handler
        schemaTree.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<String> selectedItem = schemaTree.getSelectionModel().getSelectedItem();
                if (selectedItem instanceof SchemaTreeItem) {
                    handleSchemaItemDoubleClick((SchemaTreeItem) selectedItem);
                }
            }
        });

        // Add context menu
        setupSchemaContextMenu();
    }

    private void setupResultsTable() {
        resultsTable.setPlaceholder(new Label("No query results to display"));
        resultsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupMenus() {
        // File menu handlers will be added here
        logger.debug("Menu setup completed");
    }

    private void setupToolbar() {
        // Toolbar button handlers will be added here
        logger.debug("Toolbar setup completed");
    }

    private void setupStatusBar() {
        updateStatus("Ready");
        updateRowCount(0);
        updateExecutionTime(0);
    }

    public void setDatabaseEngine(DatabaseEngine engine) {
        this.databaseEngine = engine;

        // Initialize admin controller
        if (adminController == null) {
            // We'll need to get the stage reference from the scene
            javafx.stage.Stage stage = (javafx.stage.Stage) root.getScene().getWindow();
            adminController = new DatabaseAdminController(engine, stage);
        }

        // Initialize chart manager
        if (chartManager == null && root.getScene() != null) {
            Stage stage = (Stage) root.getScene().getWindow();
            chartManager = new ChartManager(stage);
        }

        refreshSchemaTree();
    }

    @FXML
    private void handleExecuteQuery() {
        String sql = sqlEditor.getSelectedText();
        if (sql.isEmpty()) {
            sql = sqlEditor.getText();
        }

        if (sql.trim().isEmpty()) {
            appendMessage("No SQL query to execute", MessageType.WARNING);
            return;
        }

        executeQuery(sql.trim());
    }

    @FXML
    private void handleClearEditor() {
        sqlEditor.clear();
        appendMessage("Query editor cleared", MessageType.INFO);
    }

    @FXML
    private void handleNewQuery() {
        Tab newTab = new Tab("Query " + (queryTabPane.getTabs().size() + 1));
        // For now, just show a placeholder
        newTab.setContent(new Label("New query tab - implementation pending"));
        queryTabPane.getTabs().add(newTab);
        queryTabPane.getSelectionModel().select(newTab);
    }

    @FXML
    private void handleImportCSV() {
        if (adminController != null) {
            adminController.handleImportCSV();
            // Refresh schema after import
            refreshSchemaTree();
        }
    }

    @FXML
    private void handleExportCSV() {
        if (adminController != null) {
            adminController.handleExportCSV();
        }
    }

    @FXML
    private void handleBackupDatabase() {
        if (adminController != null) {
            adminController.handleBackupDatabase();
        }
    }

    @FXML
    private void handleRestoreDatabase() {
        if (adminController != null) {
            adminController.handleRestoreDatabase();
            // Refresh schema after restore
            refreshSchemaTree();
        }
    }

    private void executeQuery(String sql) {
        if (databaseEngine == null) {
            appendMessage("Database engine not initialized", MessageType.ERROR);
            return;
        }

        updateStatus("Executing query...");
        appendMessage("Executing: " + sql.substring(0, Math.min(sql.length(), 100)) +
                     (sql.length() > 100 ? "..." : ""), MessageType.INFO);

        Task<QueryResult> queryTask = new Task<QueryResult>() {
            @Override
            protected QueryResult call() throws Exception {
                long startTime = System.currentTimeMillis();
                try {
                    QueryResult result = databaseEngine.executeSQL(sql);
                    long executionTime = System.currentTimeMillis() - startTime;

                    Platform.runLater(() -> {
                        updateExecutionTime(executionTime);
                        displayQueryResult(result);
                        refreshSchemaTree(); // Refresh in case schema changed
                    });

                    return result;
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        appendMessage("Query failed: " + e.getMessage(), MessageType.ERROR);
                        updateStatus("Query failed");
                    });
                    throw e;
                }
            }
        };

        queryTask.setOnSucceeded(e -> {
            updateStatus("Query completed successfully");
            appendMessage("Query executed successfully", MessageType.SUCCESS);
        });

        queryTask.setOnFailed(e -> {
            Throwable exception = queryTask.getException();
            logger.error("Query execution failed", exception);
            updateStatus("Ready");
        });

        executorService.submit(queryTask);
    }

    private void displayQueryResult(QueryResult result) {
        resultsTable.getColumns().clear();
        resultsTable.getItems().clear();

        if (result.getType() == QueryResult.ResultType.SELECT && result.hasRecords()) {
            List<Record> records = result.getRecords();
            if (!records.isEmpty()) {
                // Create columns based on first record
                Record firstRecord = records.get(0);
                Set<String> columnNames = firstRecord.getValues().keySet();

                int columnIndex = 0;
                for (String columnName : columnNames) {
                    final int colIndex = columnIndex;
                    TableColumn<ObservableList<String>, String> column =
                        new TableColumn<>(columnName);
                    column.setCellValueFactory(param ->
                        new SimpleStringProperty(param.getValue().get(colIndex)));
                    resultsTable.getColumns().add(column);
                    columnIndex++;
                }

                // Add data rows
                ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
                for (Record record : records) {
                    ObservableList<String> row = FXCollections.observableArrayList();
                    for (String columnName : columnNames) {
                        String value = record.getValue(columnName);
                        row.add(value != null ? value : "NULL");
                    }
                    data.add(row);
                }

                resultsTable.setItems(data);
                updateRowCount(records.size());
            }
        } else {
            // For non-SELECT statements, show affected rows
            updateRowCount(result.getAffectedRows());
            appendMessage(getResultMessage(result), MessageType.SUCCESS);
        }
    }

    private String getResultMessage(QueryResult result) {
        switch (result.getType()) {
            case INSERT:
                return result.getAffectedRows() + " row(s) inserted";
            case UPDATE:
                return result.getAffectedRows() + " row(s) updated";
            case DELETE:
                return result.getAffectedRows() + " row(s) deleted";
            case CREATE_TABLE:
                return "Table created: " + result.getMessage();
            default:
                return "Query executed successfully";
        }
    }

    private void setupSchemaContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem selectAllItem = new MenuItem("SELECT * FROM table");
        selectAllItem.setOnAction(e -> {
            TreeItem<String> selectedItem = schemaTree.getSelectionModel().getSelectedItem();
            if (selectedItem instanceof SchemaTreeItem) {
                SchemaTreeItem schemaItem = (SchemaTreeItem) selectedItem;
                if (schemaItem.isTable()) {
                    generateSelectQuery(schemaItem.getFullName());
                }
            }
        });

        MenuItem describeItem = new MenuItem("Describe Table");
        describeItem.setOnAction(e -> {
            TreeItem<String> selectedItem = schemaTree.getSelectionModel().getSelectedItem();
            if (selectedItem instanceof SchemaTreeItem) {
                SchemaTreeItem schemaItem = (SchemaTreeItem) selectedItem;
                if (schemaItem.isTable()) {
                    showTableDetails(schemaItem.getFullName());
                }
            }
        });

        MenuItem refreshItem = new MenuItem("Refresh");
        refreshItem.setOnAction(e -> refreshSchemaTree());

        contextMenu.getItems().addAll(selectAllItem, describeItem, new SeparatorMenuItem(), refreshItem);
        schemaTree.setContextMenu(contextMenu);
    }

    private void refreshSchemaTree() {
        if (databaseEngine == null || !databaseEngine.isRunning()) {
            return;
        }

        Task<Void> refreshTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(() -> {
                    SchemaTreeItem root = (SchemaTreeItem) schemaTree.getRoot();
                    SchemaTreeItem tablesNode = (SchemaTreeItem) root.getChildren().get(0);

                    // Clear existing tables
                    tablesNode.getChildren().clear();

                    // Add current tables with details
                    for (String tableName : databaseEngine.getDatabase().getTableNames()) {
                        try {
                            var table = databaseEngine.getDatabase().getTable(tableName);
                            SchemaTreeItem tableItem = new SchemaTreeItem(
                                tableName,
                                SchemaTreeItem.ItemType.TABLE,
                                tableName,
                                "Table with " + table.getColumns().size() + " columns"
                            );
                            tableItem.setRecordCount(table.getRecordCount());

                            // Add columns as children
                            for (var column : table.getColumns()) {
                                String columnDesc = column.getType().toString();
                                if (column.isPrimaryKey()) {
                                    columnDesc += " (PRIMARY KEY)";
                                }

                                SchemaTreeItem columnItem = new SchemaTreeItem(
                                    column.getName(),
                                    SchemaTreeItem.ItemType.COLUMN,
                                    column.getName(),
                                    columnDesc
                                );
                                tableItem.getChildren().add(columnItem);
                            }

                            tablesNode.getChildren().add(tableItem);
                        } catch (Exception e) {
                            logger.debug("Error getting table details for: " + tableName, e);
                            SchemaTreeItem tableItem = new SchemaTreeItem(tableName, SchemaTreeItem.ItemType.TABLE);
                            tablesNode.getChildren().add(tableItem);
                        }
                    }

                    tablesNode.setExpanded(true);
                });
                return null;
            }
        };

        executorService.submit(refreshTask);
    }

    private void handleSchemaItemDoubleClick(SchemaTreeItem item) {
        if (item.isTable()) {
            generateSelectQuery(item.getFullName());
        } else if (item.isColumn()) {
            // Get parent table name and generate column-specific query
            TreeItem<String> parent = item.getParent();
            if (parent instanceof SchemaTreeItem && ((SchemaTreeItem) parent).isTable()) {
                String tableName = ((SchemaTreeItem) parent).getFullName();
                String columnName = item.getFullName();
                String selectQuery = "SELECT " + columnName + " FROM " + tableName + " LIMIT 100;";
                sqlEditor.replaceText(0, sqlEditor.getLength(), selectQuery);
                appendMessage("Generated query for column: " + columnName, MessageType.INFO);
            }
        }
    }

    private void generateSelectQuery(String tableName) {
        String selectQuery = "SELECT * FROM " + tableName + " LIMIT 100;";
        sqlEditor.replaceText(0, sqlEditor.getLength(), selectQuery);
        appendMessage("Generated query for table: " + tableName, MessageType.INFO);
    }

    private void showTableDetails(String tableName) {
        try {
            var table = databaseEngine.getDatabase().getTable(tableName);
            StringBuilder details = new StringBuilder();
            details.append("Table: ").append(tableName).append("\n");
            details.append("Records: ").append(table.getRecordCount()).append("\n");
            details.append("Columns:\n");

            for (var column : table.getColumns()) {
                details.append("  - ").append(column.getName())
                       .append(" (").append(column.getType()).append(")");
                if (column.isPrimaryKey()) {
                    details.append(" PRIMARY KEY");
                }
                details.append("\n");
            }

            details.append("Indexes:\n");
            for (String indexedColumn : table.getIndexedColumns()) {
                details.append("  - ").append(indexedColumn).append("\n");
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Table Details");
            alert.setHeaderText("Details for table: " + tableName);
            alert.setContentText(details.toString());
            alert.showAndWait();

        } catch (Exception e) {
            appendMessage("Error getting table details: " + e.getMessage(), MessageType.ERROR);
        }
    }

    private void appendMessage(String message, MessageType type) {
        Platform.runLater(() -> {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String prefix = switch (type) {
                case INFO -> "[INFO]";
                case SUCCESS -> "[SUCCESS]";
                case WARNING -> "[WARNING]";
                case ERROR -> "[ERROR]";
            };

            String fullMessage = String.format("%s %s %s%n", timestamp, prefix, message);
            messagesArea.appendText(fullMessage);

            // Auto-scroll to bottom
            messagesArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    private void updateStatus(String status) {
        Platform.runLater(() -> statusLabel.setText(status));
    }

    private void updateRowCount(int count) {
        Platform.runLater(() -> rowCountLabel.setText("Rows: " + count));
    }

    private void updateExecutionTime(long timeMs) {
        Platform.runLater(() -> executionTimeLabel.setText("Time: " + timeMs + "ms"));
    }

    @FXML
    private void handleCreateChart() {
        if (resultsTable.getItems().isEmpty()) {
            appendMessage("No data available for chart creation. Execute a query first.", MessageType.WARNING);
            return;
        }

        createChartFromResults();
    }

    private void createChartFromResults() {
        if (chartManager == null) {
            // Initialize chart manager if not already done
            Stage stage = (Stage) root.getScene().getWindow();
            chartManager = new ChartManager(stage);
        }

        try {
            // Extract column names from table
            List<String> columnNames = new ArrayList<>();
            for (TableColumn<?, ?> column : resultsTable.getColumns()) {
                columnNames.add(column.getText());
            }

            if (columnNames.isEmpty()) {
                appendMessage("No columns available for chart creation", MessageType.WARNING);
                return;
            }

            // Show chart configuration dialog
            ChartConfiguration config = chartManager.showChartConfigurationDialog(columnNames);
            if (config == null) {
                return; // User cancelled
            }

            // Convert table data to format expected by ChartManager
            List<ObservableList<String>> chartData = new ArrayList<>(resultsTable.getItems());

            // Create the chart
            Chart chart = chartManager.createChart(config, chartData, columnNames);

            // Show chart in new window
            showChartWindow(chart, config);

            appendMessage("Chart created successfully: " + config.getTitle(), MessageType.SUCCESS);

        } catch (Exception e) {
            logger.error("Error creating chart", e);
            appendMessage("Error creating chart: " + e.getMessage(), MessageType.ERROR);
        }
    }

    private void showChartWindow(Chart chart, ChartConfiguration config) {
        Stage chartStage = new Stage();
        chartStage.setTitle("Chart: " + config.getTitle());
        chartStage.initOwner((Stage) root.getScene().getWindow());

        // Create chart window layout
        VBox chartRoot = new VBox(10);
        chartRoot.setPadding(new javafx.geometry.Insets(10));

        // Chart title
        Label titleLabel = new Label(config.getTitle());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Chart
        chart.setPrefSize(800, 600);

        // Buttons
        javafx.scene.layout.HBox buttonBox = new javafx.scene.layout.HBox(10);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);

        Button exportButton = new Button("Export Chart");
        exportButton.setOnAction(e -> chartManager.exportChart(chart, config.getTitle()));

        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> chartStage.close());

        buttonBox.getChildren().addAll(exportButton, closeButton);

        chartRoot.getChildren().addAll(titleLabel, chart, buttonBox);

        javafx.scene.Scene chartScene = new javafx.scene.Scene(new javafx.scene.control.ScrollPane(chartRoot), 900, 700);
        chartStage.setScene(chartScene);
        chartStage.show();
    }

    @FXML
    private void handleCreateDashboard() {
        if (chartManager == null || chartManager.getActiveCharts().isEmpty()) {
            appendMessage("No charts available for dashboard creation", MessageType.WARNING);
            return;
        }

        try {
            // Create dashboard with all active charts
            List<Chart> charts = new ArrayList<>(chartManager.getActiveCharts().values());
            VBox dashboard = chartManager.createDashboard(charts, "Nova SQL Dashboard");

            // Show dashboard in new window
            Stage dashboardStage = new Stage();
            dashboardStage.setTitle("Nova SQL Dashboard");
            dashboardStage.initOwner((Stage) root.getScene().getWindow());

            javafx.scene.Scene dashboardScene = new javafx.scene.Scene(
                new javafx.scene.control.ScrollPane(dashboard), 1000, 800);
            dashboardStage.setScene(dashboardScene);
            dashboardStage.show();

            appendMessage("Dashboard created with " + charts.size() + " charts", MessageType.SUCCESS);

        } catch (Exception e) {
            logger.error("Error creating dashboard", e);
            appendMessage("Error creating dashboard: " + e.getMessage(), MessageType.ERROR);
        }
    }

    public void shutdown() {
        logger.info("Shutting down MainController");
        executorService.shutdown();
    }

    private enum MessageType {
        INFO, SUCCESS, WARNING, ERROR
    }
}