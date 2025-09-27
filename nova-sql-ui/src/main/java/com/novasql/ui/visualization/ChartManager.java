package com.novasql.ui.visualization;

import javafx.collections.ObservableList;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive chart and visualization manager.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Multiple chart types (Bar, Line, Pie, Scatter, Area)</li>
 *   <li>Interactive data exploration</li>
 *   <li>Real-time chart updates</li>
 *   <li>Chart export capabilities</li>
 *   <li>Dashboard creation</li>
 *   <li>Chart templates and themes</li>
 * </ul>
 */
public class ChartManager {
    private static final Logger logger = LoggerFactory.getLogger(ChartManager.class);

    public enum ChartType {
        BAR_CHART("Bar Chart", "Shows data in rectangular bars"),
        LINE_CHART("Line Chart", "Shows data points connected by lines"),
        PIE_CHART("Pie Chart", "Shows data as slices of a circular pie"),
        SCATTER_CHART("Scatter Chart", "Shows data as points on X-Y axes"),
        AREA_CHART("Area Chart", "Shows data as filled areas"),
        HISTOGRAM("Histogram", "Shows frequency distribution"),
        BOX_PLOT("Box Plot", "Shows statistical distribution"),
        HEAT_MAP("Heat Map", "Shows data intensity with colors");

        public final String displayName;
        public final String description;

        ChartType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final Stage parentStage;
    private final Map<String, Chart> activeCharts = new HashMap<>();

    public ChartManager(Stage parentStage) {
        this.parentStage = parentStage;
    }

    /**
     * Creates a chart from the given data and configuration.
     */
    public Chart createChart(ChartConfiguration config, List<ObservableList<String>> data, List<String> columnNames) {
        try {
            Chart chart = switch (config.getChartType()) {
                case BAR_CHART -> createBarChart(config, data, columnNames);
                case LINE_CHART -> createLineChart(config, data, columnNames);
                case PIE_CHART -> createPieChart(config, data, columnNames);
                case SCATTER_CHART -> createScatterChart(config, data, columnNames);
                case AREA_CHART -> createAreaChart(config, data, columnNames);
                case HISTOGRAM -> createHistogram(config, data, columnNames);
                case BOX_PLOT -> createBoxPlot(config, data, columnNames);
                case HEAT_MAP -> createHeatMap(config, data, columnNames);
            };

            // Apply styling and configuration
            styleChart(chart, config);

            // Store for management
            activeCharts.put(config.getTitle(), chart);

            return chart;
        } catch (Exception e) {
            logger.error("Error creating chart", e);
            throw new RuntimeException("Failed to create chart: " + e.getMessage());
        }
    }

    private Chart createBarChart(ChartConfiguration config, List<ObservableList<String>> data, List<String> columnNames) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel(config.getXAxisLabel());
        yAxis.setLabel(config.getYAxisLabel());

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle(config.getTitle());

        // Process data for bar chart
        Map<String, List<Double>> seriesData = processDataForChart(data, columnNames, config);

        for (Map.Entry<String, List<Double>> entry : seriesData.entrySet()) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(entry.getKey());

            List<Double> values = entry.getValue();
            for (int i = 0; i < values.size(); i++) {
                String category = data.get(i).get(config.getXColumnIndex());
                series.getData().add(new XYChart.Data<>(category, values.get(i)));
            }

            chart.getData().add(series);
        }

        return chart;
    }

    private Chart createLineChart(ChartConfiguration config, List<ObservableList<String>> data, List<String> columnNames) {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel(config.getXAxisLabel());
        yAxis.setLabel(config.getYAxisLabel());

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle(config.getTitle());
        chart.setCreateSymbols(config.isShowDataPoints());

        // Process data for line chart
        Map<String, List<Double>> seriesData = processDataForChart(data, columnNames, config);

        for (Map.Entry<String, List<Double>> entry : seriesData.entrySet()) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(entry.getKey());

            List<Double> xValues = getNumericColumn(data, config.getXColumnIndex());
            List<Double> yValues = entry.getValue();

            for (int i = 0; i < Math.min(xValues.size(), yValues.size()); i++) {
                series.getData().add(new XYChart.Data<>(xValues.get(i), yValues.get(i)));
            }

            chart.getData().add(series);
        }

        return chart;
    }

    private Chart createPieChart(ChartConfiguration config, List<ObservableList<String>> data, List<String> columnNames) {
        PieChart chart = new PieChart();
        chart.setTitle(config.getTitle());

        // Aggregate data for pie chart
        Map<String, Double> aggregatedData = new HashMap<>();

        for (ObservableList<String> row : data) {
            String category = row.get(config.getXColumnIndex());
            double value = parseNumericValue(row.get(config.getYColumnIndices().get(0)));
            aggregatedData.merge(category, value, Double::sum);
        }

        for (Map.Entry<String, Double> entry : aggregatedData.entrySet()) {
            chart.getData().add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }

        return chart;
    }

    private Chart createScatterChart(ChartConfiguration config, List<ObservableList<String>> data, List<String> columnNames) {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel(config.getXAxisLabel());
        yAxis.setLabel(config.getYAxisLabel());

        ScatterChart<Number, Number> chart = new ScatterChart<>(xAxis, yAxis);
        chart.setTitle(config.getTitle());

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Data Points");

        List<Double> xValues = getNumericColumn(data, config.getXColumnIndex());
        List<Double> yValues = getNumericColumn(data, config.getYColumnIndices().get(0));

        for (int i = 0; i < Math.min(xValues.size(), yValues.size()); i++) {
            series.getData().add(new XYChart.Data<>(xValues.get(i), yValues.get(i)));
        }

        chart.getData().add(series);
        return chart;
    }

    private Chart createAreaChart(ChartConfiguration config, List<ObservableList<String>> data, List<String> columnNames) {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel(config.getXAxisLabel());
        yAxis.setLabel(config.getYAxisLabel());

        AreaChart<Number, Number> chart = new AreaChart<>(xAxis, yAxis);
        chart.setTitle(config.getTitle());

        Map<String, List<Double>> seriesData = processDataForChart(data, columnNames, config);

        for (Map.Entry<String, List<Double>> entry : seriesData.entrySet()) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(entry.getKey());

            List<Double> xValues = getNumericColumn(data, config.getXColumnIndex());
            List<Double> yValues = entry.getValue();

            for (int i = 0; i < Math.min(xValues.size(), yValues.size()); i++) {
                series.getData().add(new XYChart.Data<>(xValues.get(i), yValues.get(i)));
            }

            chart.getData().add(series);
        }

        return chart;
    }

    private Chart createHistogram(ChartConfiguration config, List<ObservableList<String>> data, List<String> columnNames) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel("Bins");
        yAxis.setLabel("Frequency");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle(config.getTitle());

        // Create histogram bins
        List<Double> values = getNumericColumn(data, config.getXColumnIndex());
        Map<String, Integer> histogram = createHistogramBins(values, config.getBinCount());

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Frequency");

        for (Map.Entry<String, Integer> entry : histogram.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        chart.getData().add(series);
        return chart;
    }

    private Chart createBoxPlot(ChartConfiguration config, List<ObservableList<String>> data, List<String> columnNames) {
        // Custom box plot implementation
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel(config.getXAxisLabel());
        yAxis.setLabel(config.getYAxisLabel());

        // For now, create a simple representation using bar chart
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle(config.getTitle() + " (Box Plot)");

        // Calculate statistics for box plot
        List<Double> values = getNumericColumn(data, config.getYColumnIndices().get(0));
        BoxPlotStatistics stats = calculateBoxPlotStatistics(values);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Statistics");

        series.getData().add(new XYChart.Data<>("Min", stats.min));
        series.getData().add(new XYChart.Data<>("Q1", stats.q1));
        series.getData().add(new XYChart.Data<>("Median", stats.median));
        series.getData().add(new XYChart.Data<>("Q3", stats.q3));
        series.getData().add(new XYChart.Data<>("Max", stats.max));

        chart.getData().add(series);
        return chart;
    }

    private Chart createHeatMap(ChartConfiguration config, List<ObservableList<String>> data, List<String> columnNames) {
        // Heat map would require custom implementation
        // For now, return a scatter chart as placeholder
        return createScatterChart(config, data, columnNames);
    }

    private void styleChart(Chart chart, ChartConfiguration config) {
        chart.setAnimated(config.isAnimated());
        chart.setLegendVisible(config.isShowLegend());

        // Apply theme
        String theme = config.getTheme();
        if (theme != null) {
            chart.getStyleClass().add("chart-" + theme.toLowerCase());
        }

        // Apply custom styling
        if (config.getCustomCss() != null) {
            chart.setStyle(config.getCustomCss());
        }
    }

    private Map<String, List<Double>> processDataForChart(List<ObservableList<String>> data,
                                                         List<String> columnNames,
                                                         ChartConfiguration config) {
        Map<String, List<Double>> seriesData = new HashMap<>();

        for (int yIndex : config.getYColumnIndices()) {
            String seriesName = columnNames.get(yIndex);
            List<Double> values = getNumericColumn(data, yIndex);
            seriesData.put(seriesName, values);
        }

        return seriesData;
    }

    private List<Double> getNumericColumn(List<ObservableList<String>> data, int columnIndex) {
        return data.stream()
            .map(row -> parseNumericValue(row.get(columnIndex)))
            .collect(Collectors.toList());
    }

    private double parseNumericValue(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private Map<String, Integer> createHistogramBins(List<Double> values, int binCount) {
        if (values.isEmpty()) return new HashMap<>();

        double min = values.stream().mapToDouble(d -> d).min().orElse(0);
        double max = values.stream().mapToDouble(d -> d).max().orElse(0);
        double binWidth = (max - min) / binCount;

        Map<String, Integer> histogram = new LinkedHashMap<>();

        for (int i = 0; i < binCount; i++) {
            double binStart = min + i * binWidth;
            double binEnd = min + (i + 1) * binWidth;
            String binLabel = String.format("%.1f-%.1f", binStart, binEnd);

            final boolean isLastBin = (i == binCount - 1);
            final double finalBinStart = binStart;
            final double finalBinEnd = binEnd;

            int count = (int) values.stream()
                .mapToDouble(d -> d)
                .filter(v -> v >= finalBinStart && (isLastBin ? v <= finalBinEnd : v < finalBinEnd))
                .count();

            histogram.put(binLabel, count);
        }

        return histogram;
    }

    private BoxPlotStatistics calculateBoxPlotStatistics(List<Double> values) {
        if (values.isEmpty()) return new BoxPlotStatistics();

        List<Double> sorted = values.stream().sorted().collect(Collectors.toList());
        int size = sorted.size();

        BoxPlotStatistics stats = new BoxPlotStatistics();
        stats.min = sorted.get(0);
        stats.max = sorted.get(size - 1);

        if (size % 2 == 0) {
            stats.median = (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2;
        } else {
            stats.median = sorted.get(size / 2);
        }

        int q1Index = size / 4;
        int q3Index = 3 * size / 4;
        stats.q1 = sorted.get(q1Index);
        stats.q3 = sorted.get(q3Index);

        return stats;
    }

    /**
     * Shows the chart configuration dialog.
     */
    public ChartConfiguration showChartConfigurationDialog(List<String> columnNames) {
        ChartConfigurationDialog dialog = new ChartConfigurationDialog(parentStage, columnNames);
        return dialog.showAndWait().orElse(null);
    }

    /**
     * Exports a chart to an image file.
     */
    public void exportChart(Chart chart, String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Chart: " + title);
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("PNG Images", "*.png"),
            new FileChooser.ExtensionFilter("JPEG Images", "*.jpg"),
            new FileChooser.ExtensionFilter("SVG Images", "*.svg")
        );

        File file = fileChooser.showSaveDialog(parentStage);
        if (file != null) {
            try {
                ChartExporter.exportChart(chart, file);
                logger.info("Chart exported to: {}", file.getAbsolutePath());
            } catch (Exception e) {
                logger.error("Error exporting chart", e);
            }
        }
    }

    /**
     * Creates a dashboard with multiple charts.
     */
    public VBox createDashboard(List<Chart> charts, String title) {
        VBox dashboard = new VBox(10);
        dashboard.setStyle("-fx-padding: 10px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        dashboard.getChildren().add(titleLabel);

        // Add charts in a grid layout
        for (Chart chart : charts) {
            chart.setPrefHeight(300);
            dashboard.getChildren().add(chart);
        }

        return dashboard;
    }

    public Map<String, Chart> getActiveCharts() {
        return new HashMap<>(activeCharts);
    }

    public void removeChart(String title) {
        activeCharts.remove(title);
    }

    public void clearAllCharts() {
        activeCharts.clear();
    }

    // Helper classes
    private static class BoxPlotStatistics {
        double min, q1, median, q3, max;
    }
}