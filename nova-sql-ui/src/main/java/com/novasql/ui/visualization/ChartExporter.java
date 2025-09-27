package com.novasql.ui.visualization;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.Chart;
import javafx.scene.image.WritableImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for exporting charts to various file formats.
 */
public class ChartExporter {
    private static final Logger logger = LoggerFactory.getLogger(ChartExporter.class);

    /**
     * Exports a chart to the specified file.
     * Supports PNG, JPEG, and basic SVG formats.
     */
    public static void exportChart(Chart chart, File file) throws IOException {
        String fileName = file.getName().toLowerCase();
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1);

        switch (extension) {
            case "png":
                exportToPNG(chart, file);
                break;
            case "jpg":
            case "jpeg":
                exportToJPEG(chart, file);
                break;
            case "svg":
                exportToSVG(chart, file);
                break;
            default:
                throw new IOException("Unsupported file format: " + extension);
        }
    }

    /**
     * Exports chart to PNG format.
     */
    private static void exportToPNG(Chart chart, File file) throws IOException {
        WritableImage image = chart.snapshot(new SnapshotParameters(), null);
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
        ImageIO.write(bufferedImage, "PNG", file);
        logger.info("Chart exported to PNG: {}", file.getAbsolutePath());
    }

    /**
     * Exports chart to JPEG format.
     */
    private static void exportToJPEG(Chart chart, File file) throws IOException {
        WritableImage image = chart.snapshot(new SnapshotParameters(), null);
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);

        // Convert to RGB (JPEG doesn't support transparency)
        BufferedImage rgbImage = new BufferedImage(
            bufferedImage.getWidth(),
            bufferedImage.getHeight(),
            BufferedImage.TYPE_INT_RGB
        );
        rgbImage.getGraphics().drawImage(bufferedImage, 0, 0, null);

        ImageIO.write(rgbImage, "JPEG", file);
        logger.info("Chart exported to JPEG: {}", file.getAbsolutePath());
    }

    /**
     * Exports chart to basic SVG format.
     * Note: This is a simplified SVG export. For advanced SVG features,
     * consider using a dedicated SVG library.
     */
    private static void exportToSVG(Chart chart, File file) throws IOException {
        double width = chart.getWidth();
        double height = chart.getHeight();

        StringBuilder svg = new StringBuilder();
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" ");
        svg.append("width=\"").append(width).append("\" ");
        svg.append("height=\"").append(height).append("\" ");
        svg.append("viewBox=\"0 0 ").append(width).append(" ").append(height).append("\">\n");

        // Add background
        svg.append("  <rect width=\"100%\" height=\"100%\" fill=\"white\"/>\n");

        // For now, embed the chart as a raster image within SVG
        // This provides basic SVG export functionality
        WritableImage image = chart.snapshot(new SnapshotParameters(), null);
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);

        // Convert image to base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "PNG", baos);
        byte[] imageBytes = baos.toByteArray();
        String base64Image = java.util.Base64.getEncoder().encodeToString(imageBytes);

        svg.append("  <image width=\"").append(width).append("\" ");
        svg.append("height=\"").append(height).append("\" ");
        svg.append("href=\"data:image/png;base64,").append(base64Image).append("\"/>\n");

        svg.append("</svg>");

        Files.write(file.toPath(), svg.toString().getBytes());
        logger.info("Chart exported to SVG: {}", file.getAbsolutePath());
    }

    /**
     * Exports chart data to CSV format for further analysis.
     */
    public static void exportChartDataToCSV(Chart chart, File file,
                                           String[] columnNames,
                                           Object[][] data) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            // Write header
            if (columnNames != null) {
                writer.println(String.join(",", columnNames));
            }

            // Write data
            if (data != null) {
                for (Object[] row : data) {
                    StringBuilder line = new StringBuilder();
                    for (int i = 0; i < row.length; i++) {
                        if (i > 0) line.append(",");

                        Object value = row[i];
                        if (value != null) {
                            String strValue = value.toString();
                            // Escape commas and quotes in CSV
                            if (strValue.contains(",") || strValue.contains("\"")) {
                                strValue = "\"" + strValue.replace("\"", "\"\"") + "\"";
                            }
                            line.append(strValue);
                        }
                    }
                    writer.println(line.toString());
                }
            }
        }
        logger.info("Chart data exported to CSV: {}", file.getAbsolutePath());
    }

    /**
     * Creates a detailed chart report in HTML format.
     */
    public static void exportChartReport(Chart chart, File file,
                                       ChartConfiguration config,
                                       String[] columnNames,
                                       Object[][] data) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html>\n<head>\n");
        html.append("  <title>Chart Report: ").append(config.getTitle()).append("</title>\n");
        html.append("  <style>\n");
        html.append("    body { font-family: Arial, sans-serif; margin: 20px; }\n");
        html.append("    .header { border-bottom: 2px solid #ccc; padding-bottom: 10px; }\n");
        html.append("    .config { background: #f9f9f9; padding: 15px; margin: 20px 0; }\n");
        html.append("    table { border-collapse: collapse; width: 100%; margin: 20px 0; }\n");
        html.append("    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        html.append("    th { background-color: #f2f2f2; }\n");
        html.append("  </style>\n");
        html.append("</head>\n<body>\n");

        // Header
        html.append("  <div class=\"header\">\n");
        html.append("    <h1>Chart Report: ").append(config.getTitle()).append("</h1>\n");
        html.append("    <p>Generated on: ").append(new java.util.Date()).append("</p>\n");
        html.append("  </div>\n");

        // Configuration
        html.append("  <div class=\"config\">\n");
        html.append("    <h2>Chart Configuration</h2>\n");
        html.append("    <ul>\n");
        html.append("      <li><strong>Type:</strong> ").append(config.getChartType().displayName).append("</li>\n");
        html.append("      <li><strong>X-Axis:</strong> ").append(config.getXAxisLabel()).append("</li>\n");
        html.append("      <li><strong>Y-Axis:</strong> ").append(config.getYAxisLabel()).append("</li>\n");
        html.append("      <li><strong>Theme:</strong> ").append(config.getTheme()).append("</li>\n");
        html.append("      <li><strong>Animated:</strong> ").append(config.isAnimated()).append("</li>\n");
        html.append("      <li><strong>Show Legend:</strong> ").append(config.isShowLegend()).append("</li>\n");
        html.append("    </ul>\n");
        html.append("  </div>\n");

        // Data table
        if (data != null && columnNames != null) {
            html.append("  <h2>Data</h2>\n");
            html.append("  <table>\n");
            html.append("    <tr>\n");
            for (String col : columnNames) {
                html.append("      <th>").append(col).append("</th>\n");
            }
            html.append("    </tr>\n");

            for (Object[] row : data) {
                html.append("    <tr>\n");
                for (Object cell : row) {
                    html.append("      <td>").append(cell != null ? cell.toString() : "").append("</td>\n");
                }
                html.append("    </tr>\n");
            }
            html.append("  </table>\n");
        }

        // Chart image (if we can export it)
        try {
            String chartFileName = file.getName().replace(".html", "_chart.png");
            File chartFile = new File(file.getParent(), chartFileName);
            exportToPNG(chart, chartFile);

            html.append("  <h2>Chart Visualization</h2>\n");
            html.append("  <img src=\"").append(chartFileName).append("\" alt=\"Chart\" style=\"max-width: 100%;\"/>\n");
        } catch (Exception e) {
            logger.warn("Could not embed chart image in HTML report", e);
        }

        html.append("</body>\n</html>");

        Files.write(file.toPath(), html.toString().getBytes());
        logger.info("Chart report exported to HTML: {}", file.getAbsolutePath());
    }
}