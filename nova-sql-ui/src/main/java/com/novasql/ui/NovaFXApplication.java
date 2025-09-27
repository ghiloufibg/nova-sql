package com.novasql.ui;

import com.novasql.DatabaseEngine;
import com.novasql.ui.controller.MainController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Main JavaFX application for Nova SQL Database Engine.
 *
 * <p>This application provides a modern desktop interface for interacting with
 * the Nova SQL database engine. Features include:</p>
 * <ul>
 *   <li>Interactive SQL query editor with syntax highlighting</li>
 *   <li>Database schema browser</li>
 *   <li>Query results viewer with export capabilities</li>
 *   <li>Performance monitoring dashboard</li>
 *   <li>Database administration tools</li>
 * </ul>
 *
 * @author Nova SQL Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class NovaFXApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(NovaFXApplication.class);

    private DatabaseEngine databaseEngine;
    private MainController mainController;

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Starting Nova SQL JavaFX Application");

            // Initialize database engine
            databaseEngine = new DatabaseEngine();
            databaseEngine.start("nova_ui", "./data");

            // Load FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Scene scene = new Scene(loader.load());

            // Get controller and inject dependencies
            mainController = loader.getController();
            mainController.setDatabaseEngine(databaseEngine);

            // Configure stage
            primaryStage.setTitle("Nova SQL Database Engine");
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);

            // Set application icon
            try {
                primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/nova-sql-icon.png")));
            } catch (Exception e) {
                logger.debug("Could not load application icon", e);
            }

            // Load CSS
            scene.getStylesheets().add(getClass().getResource("/css/nova-sql.css").toExternalForm());

            // Set shutdown handler
            primaryStage.setOnCloseRequest(event -> {
                logger.info("Application shutdown requested");
                try {
                    if (databaseEngine != null && databaseEngine.isRunning()) {
                        databaseEngine.stop();
                    }
                } catch (Exception e) {
                    logger.error("Error during shutdown", e);
                }
                Platform.exit();
            });

            primaryStage.show();
            logger.info("Nova SQL JavaFX Application started successfully");

        } catch (IOException e) {
            logger.error("Failed to load FXML", e);
            showErrorAlert("Application Error", "Failed to load user interface", e.getMessage());
            Platform.exit();
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            showErrorAlert("Startup Error", "Failed to start Nova SQL", e.getMessage());
            Platform.exit();
        }
    }

    @Override
    public void stop() {
        logger.info("JavaFX Application stopping");
        try {
            if (databaseEngine != null && databaseEngine.isRunning()) {
                databaseEngine.stop();
            }
        } catch (Exception e) {
            logger.error("Error stopping database engine", e);
        }
    }

    private void showErrorAlert(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    public static void main(String[] args) {
        // Configure system properties for JavaFX
        System.setProperty("javafx.preloader", "com.novasql.ui.NovaPreloader");

        logger.info("Launching Nova SQL JavaFX Application with args: {}", String.join(" ", args));
        launch(args);
    }
}