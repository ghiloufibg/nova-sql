package com.novasql.ui.visualization;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration class for chart creation and customization.
 */
public class ChartConfiguration {
    private ChartManager.ChartType chartType = ChartManager.ChartType.BAR_CHART;
    private String title = "Untitled Chart";
    private String xAxisLabel = "X Axis";
    private String yAxisLabel = "Y Axis";
    private int xColumnIndex = 0;
    private List<Integer> yColumnIndices = new ArrayList<>();
    private boolean showLegend = true;
    private boolean animated = true;
    private boolean showDataPoints = true;
    private String theme = "default";
    private String customCss = null;
    private int binCount = 10;

    public ChartConfiguration() {
        yColumnIndices.add(1);
    }

    public ChartConfiguration(ChartManager.ChartType chartType, String title) {
        this();
        this.chartType = chartType;
        this.title = title;
    }

    public ChartManager.ChartType getChartType() {
        return chartType;
    }

    public void setChartType(ChartManager.ChartType chartType) {
        this.chartType = chartType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getXAxisLabel() {
        return xAxisLabel;
    }

    public void setXAxisLabel(String xAxisLabel) {
        this.xAxisLabel = xAxisLabel;
    }

    public String getYAxisLabel() {
        return yAxisLabel;
    }

    public void setYAxisLabel(String yAxisLabel) {
        this.yAxisLabel = yAxisLabel;
    }

    public int getXColumnIndex() {
        return xColumnIndex;
    }

    public void setXColumnIndex(int xColumnIndex) {
        this.xColumnIndex = xColumnIndex;
    }

    public List<Integer> getYColumnIndices() {
        return yColumnIndices;
    }

    public void setYColumnIndices(List<Integer> yColumnIndices) {
        this.yColumnIndices = yColumnIndices;
    }

    public void addYColumnIndex(int index) {
        if (!yColumnIndices.contains(index)) {
            yColumnIndices.add(index);
        }
    }

    public void removeYColumnIndex(int index) {
        yColumnIndices.remove(Integer.valueOf(index));
    }

    public boolean isShowLegend() {
        return showLegend;
    }

    public void setShowLegend(boolean showLegend) {
        this.showLegend = showLegend;
    }

    public boolean isAnimated() {
        return animated;
    }

    public void setAnimated(boolean animated) {
        this.animated = animated;
    }

    public boolean isShowDataPoints() {
        return showDataPoints;
    }

    public void setShowDataPoints(boolean showDataPoints) {
        this.showDataPoints = showDataPoints;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getCustomCss() {
        return customCss;
    }

    public void setCustomCss(String customCss) {
        this.customCss = customCss;
    }

    public int getBinCount() {
        return binCount;
    }

    public void setBinCount(int binCount) {
        this.binCount = Math.max(1, Math.min(100, binCount));
    }

    @Override
    public String toString() {
        return "ChartConfiguration{" +
            "chartType=" + chartType +
            ", title='" + title + '\'' +
            ", xAxisLabel='" + xAxisLabel + '\'' +
            ", yAxisLabel='" + yAxisLabel + '\'' +
            '}';
    }
}