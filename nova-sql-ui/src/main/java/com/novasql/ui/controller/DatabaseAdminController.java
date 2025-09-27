package com.novasql.ui.controller;

import com.novasql.DatabaseEngine;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller for database administration operations.
 *
 * <p>Handles database backup, restore, CSV import/export operations
 * with user feedback and progress monitoring.</p>
 */
public class DatabaseAdminController {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseAdminController.class);

    private final DatabaseEngine databaseEngine;
    private final Stage parentStage;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public DatabaseAdminController(DatabaseEngine databaseEngine, Stage parentStage) {
        this.databaseEngine = databaseEngine;
        this.parentStage = parentStage;
    }

    /**
     * Handles CSV import operation.
     */
    public void handleImportCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select CSV File to Import");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        File selectedFile = fileChooser.showOpenDialog(parentStage);
        if (selectedFile != null) {
            // Ask for table name
            String tableName = showTextInputDialog("Table Name",
                "Enter the target table name:", "imported_data");

            if (tableName != null && !tableName.trim().isEmpty()) {
                performCsvImport(selectedFile.getAbsolutePath(), tableName.trim());
            }
        }
    }

    /**
     * Handles CSV export operation.
     */
    public void handleExportCSV() {
        // Ask for table name
        String tableName = showTextInputDialog("Export Table",
            "Enter the table name to export:", "");

        if (tableName != null && !tableName.trim().isEmpty()) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save CSV Export");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
            );
            fileChooser.setInitialFileName(tableName + ".csv");

            File selectedFile = fileChooser.showSaveDialog(parentStage);
            if (selectedFile != null) {
                performCsvExport(tableName.trim(), selectedFile.getAbsolutePath());
            }
        }
    }

    /**
     * Handles database backup operation.
     */
    public void handleBackupDatabase() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Database Backup");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("SQL Files", "*.sql")
        );
        fileChooser.setInitialFileName("nova_backup.sql");

        File selectedFile = fileChooser.showSaveDialog(parentStage);
        if (selectedFile != null) {
            performDatabaseBackup(selectedFile.getAbsolutePath());
        }
    }

    /**
     * Handles database restore operation.
     */
    public void handleRestoreDatabase() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Restore Database");
        confirmAlert.setHeaderText("Database Restore Warning");
        confirmAlert.setContentText("This operation will replace the current database content. Continue?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Database Backup File");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("SQL Files", "*.sql")
            );

            File selectedFile = fileChooser.showOpenDialog(parentStage);
            if (selectedFile != null) {
                performDatabaseRestore(selectedFile.getAbsolutePath());
            }
        }
    }

    private void performCsvImport(String filePath, String tableName) {
        Task<Void> importTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    databaseEngine.importCSV(filePath, tableName);
                    Platform.runLater(() -> {
                        showSuccessAlert("CSV Import",
                            "CSV file imported successfully into table: " + tableName);
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        showErrorAlert("CSV Import Failed",
                            "Failed to import CSV file: " + e.getMessage());
                    });
                    throw e;
                }
                return null;
            }
        };

        executorService.submit(importTask);
    }

    private void performCsvExport(String tableName, String filePath) {
        Task<Void> exportTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    databaseEngine.exportCSV(tableName, filePath);
                    Platform.runLater(() -> {
                        showSuccessAlert("CSV Export",
                            "Table '" + tableName + "' exported successfully to: " + filePath);
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        showErrorAlert("CSV Export Failed",
                            "Failed to export table: " + e.getMessage());
                    });
                    throw e;
                }
                return null;
            }
        };

        executorService.submit(exportTask);
    }

    private void performDatabaseBackup(String filePath) {
        Task<Void> backupTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    databaseEngine.exportDatabase(filePath);
                    Platform.runLater(() -> {
                        showSuccessAlert("Database Backup",
                            "Database backup created successfully: " + filePath);
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        showErrorAlert("Backup Failed",
                            "Failed to create database backup: " + e.getMessage());
                    });
                    throw e;
                }
                return null;
            }
        };

        executorService.submit(backupTask);
    }

    private void performDatabaseRestore(String filePath) {
        Task<Void> restoreTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    databaseEngine.importDatabase(filePath);
                    Platform.runLater(() -> {
                        showSuccessAlert("Database Restore",
                            "Database restored successfully from: " + filePath);
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        showErrorAlert("Restore Failed",
                            "Failed to restore database: " + e.getMessage());
                    });
                    throw e;
                }
                return null;
            }
        };

        executorService.submit(restoreTask);
    }

    private String showTextInputDialog(String title, String message, String defaultValue) {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(defaultValue);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(message);

        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("Operation Successful");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Operation Failed");
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void shutdown() {
        logger.info("Shutting down DatabaseAdminController");
        executorService.shutdown();
    }
}