package com.novasql.ui.results;

import com.novasql.schema.Record;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Enhanced results table with pagination, filtering, sorting, and export capabilities.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Pagination for large result sets</li>
 *   <li>Column-based filtering and sorting</li>
 *   <li>In-cell editing for data modification</li>
 *   <li>Export to multiple formats (CSV, Excel, JSON)</li>
 *   <li>Column freezing and resizing</li>
 *   <li>Search and find functionality</li>
 *   <li>Data visualization integration</li>
 * </ul>
 */
public class EnhancedResultsTable extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedResultsTable.class);

    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final int[] PAGE_SIZE_OPTIONS = {50, 100, 200, 500, 1000};

    // UI Components
    private final TableView<ObservableList<String>> tableView;
    private TextField searchField;
    private ComboBox<Integer> pageSizeCombo;
    private Label statusLabel;
    private Button prevPageBtn;
    private Button nextPageBtn;
    private Label pageInfoLabel;
    private Button exportBtn;
    private Button chartBtn;

    // Data Management
    private final ObservableList<ObservableList<String>> allData = FXCollections.observableArrayList();
    private final FilteredList<ObservableList<String>> filteredData;
    private final SortedList<ObservableList<String>> sortedData;

    // Pagination
    private int currentPage = 0;
    private int pageSize = DEFAULT_PAGE_SIZE;
    private int totalRows = 0;

    // Column Information
    private List<String> columnNames = new ArrayList<>();
    private Map<String, String> columnTypes = new HashMap<>();

    // Callbacks
    private Consumer<String> onStatusUpdate;
    private Consumer<List<ObservableList<String>>> onDataForChart;

    public EnhancedResultsTable() {
        this.tableView = new TableView<>();
        this.filteredData = new FilteredList<>(allData);
        this.sortedData = new SortedList<>(filteredData);

        setupComponents();
        setupLayout();
        setupEventHandlers();

        tableView.setItems(sortedData);
    }

    private void setupComponents() {
        // Search field
        searchField = new TextField();
        searchField.setPromptText("Search in results...");
        searchField.setPrefWidth(200);

        // Page size combo
        pageSizeCombo = new ComboBox<>();
        pageSizeCombo.getItems().addAll(Arrays.stream(PAGE_SIZE_OPTIONS).boxed().collect(Collectors.toList()));
        pageSizeCombo.setValue(DEFAULT_PAGE_SIZE);

        // Navigation buttons
        prevPageBtn = new Button("◀ Previous");
        nextPageBtn = new Button("Next ▶");
        pageInfoLabel = new Label("Page 1 of 1");

        // Action buttons
        exportBtn = new Button("Export");
        chartBtn = new Button("Chart");

        // Status label
        statusLabel = new Label("No data");

        // Table setup
        tableView.setEditable(true);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.setPlaceholder(new Label("No query results to display"));

        // Enable multiple selection
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void setupLayout() {
        // Top toolbar
        HBox topToolbar = new HBox(10);
        topToolbar.setPadding(new Insets(5));
        topToolbar.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 0 0 1 0;");

        Label searchLabel = new Label("Search:");
        Label pageSizeLabel = new Label("Page Size:");

        topToolbar.getChildren().addAll(
            searchLabel, searchField,
            new Separator(),
            pageSizeLabel, pageSizeCombo,
            new Separator(),
            exportBtn, chartBtn
        );

        // Table with scroll pane
        ScrollPane tableScrollPane = new ScrollPane(tableView);
        tableScrollPane.setFitToWidth(true);
        tableScrollPane.setFitToHeight(true);
        VBox.setVgrow(tableScrollPane, Priority.ALWAYS);

        // Bottom toolbar
        HBox bottomToolbar = new HBox(10);
        bottomToolbar.setPadding(new Insets(5));
        bottomToolbar.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 1 0 0 0;");

        HBox navigation = new HBox(5);
        navigation.getChildren().addAll(prevPageBtn, pageInfoLabel, nextPageBtn);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bottomToolbar.getChildren().addAll(navigation, spacer, statusLabel);

        // Main layout
        getChildren().addAll(topToolbar, tableScrollPane, bottomToolbar);
    }

    private void setupEventHandlers() {
        // Search functionality
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            applyFilter(newVal);
            updatePagination();
        });

        // Page size change
        pageSizeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                pageSize = newVal;
                currentPage = 0;
                updatePagination();
            }
        });

        // Navigation
        prevPageBtn.setOnAction(e -> {
            if (currentPage > 0) {
                currentPage--;
                updatePagination();
            }
        });

        nextPageBtn.setOnAction(e -> {
            int maxPage = getMaxPage();
            if (currentPage < maxPage) {
                currentPage++;
                updatePagination();
            }
        });

        // Export
        exportBtn.setOnAction(e -> showExportDialog());

        // Chart
        chartBtn.setOnAction(e -> {
            if (onDataForChart != null) {
                List<ObservableList<String>> selectedData = getVisibleData();
                onDataForChart.accept(selectedData);
            }
        });

        // Double-click to edit
        tableView.setRowFactory(tv -> {
            TableRow<ObservableList<String>> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    // Enable editing mode for the row
                    editRow(row.getIndex());
                }
            });
            return row;
        });
    }

    private void applyFilter(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            filteredData.setPredicate(null);
        } else {
            String lowerSearchText = searchText.toLowerCase();
            filteredData.setPredicate(row -> {
                return row.stream().anyMatch(cell ->
                    cell != null && cell.toLowerCase().contains(lowerSearchText));
            });
        }
    }

    private void updatePagination() {
        int totalFilteredRows = filteredData.size();
        int maxPage = getMaxPage();

        // Update button states
        prevPageBtn.setDisable(currentPage <= 0);
        nextPageBtn.setDisable(currentPage >= maxPage);

        // Update page info
        if (totalFilteredRows == 0) {
            pageInfoLabel.setText("No results");
        } else {
            int startRow = currentPage * pageSize + 1;
            int endRow = Math.min((currentPage + 1) * pageSize, totalFilteredRows);
            pageInfoLabel.setText(String.format("Page %d of %d (%d-%d of %d rows)",
                currentPage + 1, maxPage + 1, startRow, endRow, totalFilteredRows));
        }

        // Update visible data
        updateVisibleData();

        // Update status
        updateStatus();
    }

    private int getMaxPage() {
        int totalFilteredRows = filteredData.size();
        return totalFilteredRows == 0 ? 0 : (totalFilteredRows - 1) / pageSize;
    }

    private void updateVisibleData() {
        int startIndex = currentPage * pageSize;
        int endIndex = Math.min(startIndex + pageSize, filteredData.size());

        ObservableList<ObservableList<String>> pageData = FXCollections.observableArrayList();

        for (int i = startIndex; i < endIndex; i++) {
            pageData.add(filteredData.get(i));
        }

        // Update table items
        sortedData.setComparator(null); // Reset sorting
        tableView.setItems(pageData);
    }

    private List<ObservableList<String>> getVisibleData() {
        return new ArrayList<>(tableView.getItems());
    }

    private void updateStatus() {
        int totalRows = allData.size();
        int filteredRows = filteredData.size();
        int visibleRows = tableView.getItems().size();

        String status = String.format("Total: %d, Filtered: %d, Visible: %d",
            totalRows, filteredRows, visibleRows);

        statusLabel.setText(status);

        if (onStatusUpdate != null) {
            onStatusUpdate.accept(status);
        }
    }

    public void setData(List<Record> records) {
        Platform.runLater(() -> {
            clearData();

            if (records == null || records.isEmpty()) {
                updateStatus();
                return;
            }

            // Extract column names from first record
            Record firstRecord = records.get(0);
            columnNames = new ArrayList<>(firstRecord.getValues().keySet());

            // Create table columns
            createTableColumns();

            // Convert records to observable lists
            for (Record record : records) {
                ObservableList<String> row = FXCollections.observableArrayList();
                for (String columnName : columnNames) {
                    String value = record.getValue(columnName);
                    row.add(value != null ? value : "NULL");
                }
                allData.add(row);
            }

            totalRows = records.size();
            currentPage = 0;
            updatePagination();
        });
    }

    private void createTableColumns() {
        tableView.getColumns().clear();

        for (int i = 0; i < columnNames.size(); i++) {
            final int columnIndex = i;
            String columnName = columnNames.get(i);

            TableColumn<ObservableList<String>, String> column = new TableColumn<>(columnName);
            column.setPrefWidth(120);
            column.setMinWidth(80);

            // Cell value factory
            column.setCellValueFactory(param -> {
                ObservableList<String> row = param.getValue();
                if (columnIndex < row.size()) {
                    return new SimpleStringProperty(row.get(columnIndex));
                }
                return new SimpleStringProperty("");
            });

            // Enable editing
            column.setCellFactory(TextFieldTableCell.forTableColumn());
            column.setOnEditCommit(event -> {
                ObservableList<String> row = event.getRowValue();
                if (columnIndex < row.size()) {
                    row.set(columnIndex, event.getNewValue());
                    // TODO: Implement actual database update
                }
            });

            // Add context menu for column operations
            ContextMenu columnMenu = createColumnContextMenu(columnName, columnIndex);
            column.setContextMenu(columnMenu);

            tableView.getColumns().add(column);
        }
    }

    private ContextMenu createColumnContextMenu(String columnName, int columnIndex) {
        ContextMenu menu = new ContextMenu();

        MenuItem sortAscItem = new MenuItem("Sort Ascending");
        sortAscItem.setOnAction(e -> sortColumn(columnIndex, true));

        MenuItem sortDescItem = new MenuItem("Sort Descending");
        sortDescItem.setOnAction(e -> sortColumn(columnIndex, false));

        MenuItem filterItem = new MenuItem("Filter Column...");
        filterItem.setOnAction(e -> showColumnFilterDialog(columnName, columnIndex));

        MenuItem hideItem = new MenuItem("Hide Column");
        hideItem.setOnAction(e -> hideColumn(columnIndex));

        MenuItem exportItem = new MenuItem("Export Column");
        exportItem.setOnAction(e -> exportColumn(columnName, columnIndex));

        menu.getItems().addAll(
            sortAscItem, sortDescItem,
            new SeparatorMenuItem(),
            filterItem, hideItem,
            new SeparatorMenuItem(),
            exportItem
        );

        return menu;
    }

    private void sortColumn(int columnIndex, boolean ascending) {
        sortedData.setComparator((row1, row2) -> {
            String val1 = columnIndex < row1.size() ? row1.get(columnIndex) : "";
            String val2 = columnIndex < row2.size() ? row2.get(columnIndex) : "";

            // Try numeric comparison first
            try {
                Double num1 = Double.parseDouble(val1);
                Double num2 = Double.parseDouble(val2);
                return ascending ? num1.compareTo(num2) : num2.compareTo(num1);
            } catch (NumberFormatException e) {
                // Fall back to string comparison
                return ascending ? val1.compareTo(val2) : val2.compareTo(val1);
            }
        });
    }

    private void showColumnFilterDialog(String columnName, int columnIndex) {
        // TODO: Implement column filter dialog
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Filter Column");
        dialog.setHeaderText("Filter " + columnName);
        dialog.setContentText("Enter filter value:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(filterValue -> applyColumnFilter(columnIndex, filterValue));
    }

    private void applyColumnFilter(int columnIndex, String filterValue) {
        String lowerFilterValue = filterValue.toLowerCase();
        filteredData.setPredicate(row -> {
            if (columnIndex < row.size()) {
                String cellValue = row.get(columnIndex);
                return cellValue != null && cellValue.toLowerCase().contains(lowerFilterValue);
            }
            return false;
        });
        updatePagination();
    }

    private void hideColumn(int columnIndex) {
        if (columnIndex < tableView.getColumns().size()) {
            tableView.getColumns().get(columnIndex).setVisible(false);
        }
    }

    private void exportColumn(String columnName, int columnIndex) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Column: " + columnName);
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        Stage stage = (Stage) getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            exportColumnToFile(columnName, columnIndex, file);
        }
    }

    private void exportColumnToFile(String columnName, int columnIndex, File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(columnName + "\n");

            for (ObservableList<String> row : filteredData) {
                if (columnIndex < row.size()) {
                    String value = row.get(columnIndex);
                    writer.write((value != null ? value : "") + "\n");
                }
            }

            if (onStatusUpdate != null) {
                onStatusUpdate.accept("Column exported to: " + file.getName());
            }
        } catch (IOException e) {
            logger.error("Error exporting column", e);
            showError("Export Error", "Failed to export column: " + e.getMessage());
        }
    }

    private void showExportDialog() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("CSV", "CSV", "Excel", "JSON", "HTML");
        dialog.setTitle("Export Data");
        dialog.setHeaderText("Choose Export Format");
        dialog.setContentText("Format:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(this::exportData);
    }

    private void exportData(String format) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Results");

        switch (format.toLowerCase()) {
            case "csv":
                fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
                break;
            case "excel":
                fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
                break;
            case "json":
                fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("JSON Files", "*.json"));
                break;
            case "html":
                fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("HTML Files", "*.html"));
                break;
        }

        Stage stage = (Stage) getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            performExport(format, file);
        }
    }

    private void performExport(String format, File file) {
        try {
            switch (format.toLowerCase()) {
                case "csv":
                    exportToCSV(file);
                    break;
                case "json":
                    exportToJSON(file);
                    break;
                case "html":
                    exportToHTML(file);
                    break;
                default:
                    showError("Export Error", "Unsupported format: " + format);
                    return;
            }

            if (onStatusUpdate != null) {
                onStatusUpdate.accept("Data exported to: " + file.getName());
            }
        } catch (IOException e) {
            logger.error("Error exporting data", e);
            showError("Export Error", "Failed to export data: " + e.getMessage());
        }
    }

    private void exportToCSV(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            // Write headers
            writer.write(String.join(",", columnNames) + "\n");

            // Write data
            for (ObservableList<String> row : filteredData) {
                List<String> escapedRow = row.stream()
                    .map(cell -> cell != null ? "\"" + cell.replace("\"", "\"\"") + "\"" : "")
                    .collect(Collectors.toList());
                writer.write(String.join(",", escapedRow) + "\n");
            }
        }
    }

    private void exportToJSON(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("[\n");

            boolean first = true;
            for (ObservableList<String> row : filteredData) {
                if (!first) writer.write(",\n");
                first = false;

                writer.write("  {\n");
                for (int i = 0; i < columnNames.size() && i < row.size(); i++) {
                    if (i > 0) writer.write(",\n");
                    String value = row.get(i);
                    writer.write(String.format("    \"%s\": \"%s\"",
                        columnNames.get(i),
                        value != null ? value.replace("\"", "\\\"") : ""));
                }
                writer.write("\n  }");
            }

            writer.write("\n]");
        }
    }

    private void exportToHTML(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("<!DOCTYPE html>\n<html>\n<head>\n");
            writer.write("<title>Query Results</title>\n");
            writer.write("<style>table{border-collapse:collapse;width:100%;}th,td{border:1px solid #ddd;padding:8px;text-align:left;}th{background-color:#f2f2f2;}</style>\n");
            writer.write("</head>\n<body>\n");
            writer.write("<h1>Query Results</h1>\n");
            writer.write("<table>\n<thead>\n<tr>\n");

            // Headers
            for (String columnName : columnNames) {
                writer.write("<th>" + escapeHtml(columnName) + "</th>\n");
            }
            writer.write("</tr>\n</thead>\n<tbody>\n");

            // Data
            for (ObservableList<String> row : filteredData) {
                writer.write("<tr>\n");
                for (int i = 0; i < columnNames.size() && i < row.size(); i++) {
                    String value = row.get(i);
                    writer.write("<td>" + escapeHtml(value != null ? value : "") + "</td>\n");
                }
                writer.write("</tr>\n");
            }

            writer.write("</tbody>\n</table>\n</body>\n</html>");
        }
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }

    private void editRow(int rowIndex) {
        // TODO: Implement row editing dialog
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void clearData() {
        allData.clear();
        tableView.getColumns().clear();
        columnNames.clear();
        columnTypes.clear();
        currentPage = 0;
        totalRows = 0;
        updateStatus();
    }

    // Public API
    public void setOnStatusUpdate(Consumer<String> onStatusUpdate) {
        this.onStatusUpdate = onStatusUpdate;
    }

    public void setOnDataForChart(Consumer<List<ObservableList<String>>> onDataForChart) {
        this.onDataForChart = onDataForChart;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public List<String> getColumnNames() {
        return new ArrayList<>(columnNames);
    }

    public List<ObservableList<String>> getSelectedRows() {
        return new ArrayList<>(tableView.getSelectionModel().getSelectedItems());
    }
}