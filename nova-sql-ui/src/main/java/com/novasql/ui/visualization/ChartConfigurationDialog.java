package com.novasql.ui.visualization;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Dialog for configuring chart creation settings.
 */
public class ChartConfigurationDialog {
    private final Stage parentStage;
    private final List<String> columnNames;
    private Stage dialogStage;
    private ChartConfiguration result;

    private ComboBox<ChartManager.ChartType> chartTypeCombo;
    private TextField titleField;
    private TextField xAxisLabelField;
    private TextField yAxisLabelField;
    private ComboBox<String> xColumnCombo;
    private ListView<String> yColumnsListView;
    private CheckBox showLegendBox;
    private CheckBox animatedBox;
    private CheckBox showDataPointsBox;
    private ComboBox<String> themeCombo;
    private TextArea customCssArea;
    private Spinner<Integer> binCountSpinner;

    public ChartConfigurationDialog(Stage parentStage, List<String> columnNames) {
        this.parentStage = parentStage;
        this.columnNames = columnNames;
        this.result = null;
    }

    public Optional<ChartConfiguration> showAndWait() {
        createDialog();
        dialogStage.showAndWait();
        return Optional.ofNullable(result);
    }

    private void createDialog() {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(parentStage);
        dialogStage.initStyle(StageStyle.UTILITY);
        dialogStage.setTitle("Chart Configuration");
        dialogStage.setResizable(false);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        // Chart type selection
        HBox chartTypeBox = new HBox(10);
        chartTypeBox.getChildren().addAll(
            new Label("Chart Type:"),
            chartTypeCombo = new ComboBox<>()
        );
        chartTypeCombo.getItems().addAll(ChartManager.ChartType.values());
        chartTypeCombo.setValue(ChartManager.ChartType.BAR_CHART);
        chartTypeCombo.setOnAction(e -> updateFieldsForChartType());

        // Basic settings
        GridPane basicPane = new GridPane();
        basicPane.setHgap(10);
        basicPane.setVgap(10);

        basicPane.add(new Label("Title:"), 0, 0);
        basicPane.add(titleField = new TextField("Chart Title"), 1, 0);
        titleField.setPrefWidth(250);

        basicPane.add(new Label("X-Axis Label:"), 0, 1);
        basicPane.add(xAxisLabelField = new TextField("X Axis"), 1, 1);

        basicPane.add(new Label("Y-Axis Label:"), 0, 2);
        basicPane.add(yAxisLabelField = new TextField("Y Axis"), 1, 2);

        // Column selection
        GridPane columnPane = new GridPane();
        columnPane.setHgap(10);
        columnPane.setVgap(10);

        columnPane.add(new Label("X Column:"), 0, 0);
        columnPane.add(xColumnCombo = new ComboBox<>(), 1, 0);
        xColumnCombo.getItems().addAll(columnNames);
        if (!columnNames.isEmpty()) {
            xColumnCombo.setValue(columnNames.get(0));
        }

        columnPane.add(new Label("Y Columns:"), 0, 1);
        yColumnsListView = new ListView<>();
        yColumnsListView.getItems().addAll(columnNames);
        yColumnsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        yColumnsListView.setPrefHeight(100);
        if (columnNames.size() > 1) {
            yColumnsListView.getSelectionModel().select(1);
        }
        columnPane.add(yColumnsListView, 1, 1);

        // Chart options
        VBox optionsBox = new VBox(10);
        optionsBox.getChildren().addAll(
            showLegendBox = new CheckBox("Show Legend"),
            animatedBox = new CheckBox("Animated"),
            showDataPointsBox = new CheckBox("Show Data Points")
        );
        showLegendBox.setSelected(true);
        animatedBox.setSelected(true);
        showDataPointsBox.setSelected(true);

        // Special options (for histograms)
        HBox binCountBox = new HBox(10);
        binCountSpinner = new Spinner<>(1, 100, 10);
        binCountBox.getChildren().addAll(
            new Label("Bin Count:"),
            binCountSpinner
        );

        // Theme selection
        HBox themeBox = new HBox(10);
        themeCombo = new ComboBox<>();
        themeCombo.getItems().addAll("default", "dark", "modern", "colorful");
        themeCombo.setValue("default");
        themeBox.getChildren().addAll(
            new Label("Theme:"),
            themeCombo
        );

        // Custom CSS
        VBox cssBox = new VBox(5);
        cssBox.getChildren().addAll(
            new Label("Custom CSS:"),
            customCssArea = new TextArea()
        );
        customCssArea.setPrefRowCount(3);
        customCssArea.setPromptText("Enter custom CSS styling...");

        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setStyle("-fx-alignment: center-right;");

        Button okButton = new Button("Create Chart");
        Button cancelButton = new Button("Cancel");

        okButton.setOnAction(e -> {
            result = createConfiguration();
            dialogStage.close();
        });

        cancelButton.setOnAction(e -> {
            result = null;
            dialogStage.close();
        });

        buttonBox.getChildren().addAll(cancelButton, okButton);

        // Layout
        root.getChildren().addAll(
            chartTypeBox,
            new Separator(),
            new Label("Basic Settings:"),
            basicPane,
            new Separator(),
            new Label("Data Columns:"),
            columnPane,
            new Separator(),
            new Label("Chart Options:"),
            optionsBox,
            binCountBox,
            new Separator(),
            new Label("Styling:"),
            themeBox,
            cssBox,
            new Separator(),
            buttonBox
        );

        updateFieldsForChartType();

        Scene scene = new Scene(new ScrollPane(root), 400, 600);
        dialogStage.setScene(scene);
    }

    private void updateFieldsForChartType() {
        ChartManager.ChartType selectedType = chartTypeCombo.getValue();
        if (selectedType == null) return;

        // Hide/show bin count for histogram
        binCountSpinner.getParent().setVisible(selectedType == ChartManager.ChartType.HISTOGRAM);

        // Update labels based on chart type
        switch (selectedType) {
            case PIE_CHART:
                xAxisLabelField.setText("Category");
                yAxisLabelField.setText("Value");
                showDataPointsBox.setDisable(true);
                break;
            case SCATTER_CHART:
                xAxisLabelField.setText("X Values");
                yAxisLabelField.setText("Y Values");
                showDataPointsBox.setDisable(false);
                break;
            case HISTOGRAM:
                xAxisLabelField.setText("Values");
                yAxisLabelField.setText("Frequency");
                showDataPointsBox.setDisable(true);
                break;
            case BOX_PLOT:
                xAxisLabelField.setText("Categories");
                yAxisLabelField.setText("Values");
                showDataPointsBox.setDisable(true);
                break;
            default:
                xAxisLabelField.setText("X Axis");
                yAxisLabelField.setText("Y Axis");
                showDataPointsBox.setDisable(false);
                break;
        }
    }

    private ChartConfiguration createConfiguration() {
        ChartConfiguration config = new ChartConfiguration();

        config.setChartType(chartTypeCombo.getValue());
        config.setTitle(titleField.getText());
        config.setXAxisLabel(xAxisLabelField.getText());
        config.setYAxisLabel(yAxisLabelField.getText());

        // Set X column
        String xColumn = xColumnCombo.getValue();
        if (xColumn != null) {
            config.setXColumnIndex(columnNames.indexOf(xColumn));
        }

        // Set Y columns
        List<Integer> yIndices = new ArrayList<>();
        for (String yColumn : yColumnsListView.getSelectionModel().getSelectedItems()) {
            yIndices.add(columnNames.indexOf(yColumn));
        }
        config.setYColumnIndices(yIndices);

        config.setShowLegend(showLegendBox.isSelected());
        config.setAnimated(animatedBox.isSelected());
        config.setShowDataPoints(showDataPointsBox.isSelected());
        config.setTheme(themeCombo.getValue());
        config.setBinCount(binCountSpinner.getValue());

        String customCss = customCssArea.getText().trim();
        if (!customCss.isEmpty()) {
            config.setCustomCss(customCss);
        }

        return config;
    }
}