package com.novasql.ui.theme;

/**
 * Represents a color scheme for custom themes.
 */
public class ColorScheme {
    private String background = "#ffffff";
    private String textPrimary = "#333333";
    private String textSecondary = "#666666";
    private String menuBackground = "#f8f9fa";
    private String toolbarBackground = "#ffffff";
    private String buttonBackground = "#007acc";
    private String buttonText = "#ffffff";
    private String buttonBorder = "#005a9e";
    private String buttonHover = "#106ebb";
    private String editorBackground = "#ffffff";
    private String tableBackground = "#ffffff";
    private String tableHeaderBackground = "#f8f9fa";
    private String tableHeaderText = "#333333";
    private String treeBackground = "#ffffff";
    private String borderColor = "#dee2e6";
    private String sqlKeyword = "#0000ff";
    private String sqlString = "#008000";
    private String sqlComment = "#808080";
    private String sqlNumber = "#ff6600";

    // Constructors
    public ColorScheme() {}

    public ColorScheme(String name) {
        // Predefined color schemes
        switch (name.toLowerCase()) {
            case "dark":
                setDarkScheme();
                break;
            case "blue":
                setBlueScheme();
                break;
            case "green":
                setGreenScheme();
                break;
            default:
                // Light scheme (default)
                break;
        }
    }

    // Predefined schemes
    private void setDarkScheme() {
        background = "#2b2b2b";
        textPrimary = "#ffffff";
        textSecondary = "#cccccc";
        menuBackground = "#3c3c3c";
        toolbarBackground = "#2b2b2b";
        buttonBackground = "#0e639c";
        buttonText = "#ffffff";
        buttonBorder = "#004578";
        buttonHover = "#1177bb";
        editorBackground = "#1e1e1e";
        tableBackground = "#2b2b2b";
        tableHeaderBackground = "#3c3c3c";
        tableHeaderText = "#ffffff";
        treeBackground = "#2b2b2b";
        borderColor = "#555555";
        sqlKeyword = "#569cd6";
        sqlString = "#ce9178";
        sqlComment = "#6a9955";
        sqlNumber = "#b5cea8";
    }

    private void setBlueScheme() {
        background = "#f0f8ff";
        textPrimary = "#1e3a8a";
        menuBackground = "#dbeafe";
        toolbarBackground = "#eff6ff";
        buttonBackground = "#3b82f6";
        buttonHover = "#2563eb";
        editorBackground = "#fafbff";
        sqlKeyword = "#1d4ed8";
        sqlString = "#059669";
        sqlComment = "#6b7280";
        sqlNumber = "#dc2626";
    }

    private void setGreenScheme() {
        background = "#f0fdf4";
        textPrimary = "#166534";
        menuBackground = "#dcfce7";
        toolbarBackground = "#f7fee7";
        buttonBackground = "#16a34a";
        buttonHover = "#15803d";
        editorBackground = "#fafdf9";
        sqlKeyword = "#15803d";
        sqlString = "#1d4ed8";
        sqlComment = "#6b7280";
        sqlNumber = "#dc2626";
    }

    // Getters and Setters
    public String getBackground() { return background; }
    public void setBackground(String background) { this.background = background; }

    public String getTextPrimary() { return textPrimary; }
    public void setTextPrimary(String textPrimary) { this.textPrimary = textPrimary; }

    public String getTextSecondary() { return textSecondary; }
    public void setTextSecondary(String textSecondary) { this.textSecondary = textSecondary; }

    public String getMenuBackground() { return menuBackground; }
    public void setMenuBackground(String menuBackground) { this.menuBackground = menuBackground; }

    public String getToolbarBackground() { return toolbarBackground; }
    public void setToolbarBackground(String toolbarBackground) { this.toolbarBackground = toolbarBackground; }

    public String getButtonBackground() { return buttonBackground; }
    public void setButtonBackground(String buttonBackground) { this.buttonBackground = buttonBackground; }

    public String getButtonText() { return buttonText; }
    public void setButtonText(String buttonText) { this.buttonText = buttonText; }

    public String getButtonBorder() { return buttonBorder; }
    public void setButtonBorder(String buttonBorder) { this.buttonBorder = buttonBorder; }

    public String getButtonHover() { return buttonHover; }
    public void setButtonHover(String buttonHover) { this.buttonHover = buttonHover; }

    public String getEditorBackground() { return editorBackground; }
    public void setEditorBackground(String editorBackground) { this.editorBackground = editorBackground; }

    public String getTableBackground() { return tableBackground; }
    public void setTableBackground(String tableBackground) { this.tableBackground = tableBackground; }

    public String getTableHeaderBackground() { return tableHeaderBackground; }
    public void setTableHeaderBackground(String tableHeaderBackground) { this.tableHeaderBackground = tableHeaderBackground; }

    public String getTableHeaderText() { return tableHeaderText; }
    public void setTableHeaderText(String tableHeaderText) { this.tableHeaderText = tableHeaderText; }

    public String getTreeBackground() { return treeBackground; }
    public void setTreeBackground(String treeBackground) { this.treeBackground = treeBackground; }

    public String getBorderColor() { return borderColor; }
    public void setBorderColor(String borderColor) { this.borderColor = borderColor; }

    public String getSqlKeyword() { return sqlKeyword; }
    public void setSqlKeyword(String sqlKeyword) { this.sqlKeyword = sqlKeyword; }

    public String getSqlString() { return sqlString; }
    public void setSqlString(String sqlString) { this.sqlString = sqlString; }

    public String getSqlComment() { return sqlComment; }
    public void setSqlComment(String sqlComment) { this.sqlComment = sqlComment; }

    public String getSqlNumber() { return sqlNumber; }
    public void setSqlNumber(String sqlNumber) { this.sqlNumber = sqlNumber; }
}