package com.novasql.ui.theme;

import javafx.application.Application;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Manages application themes and user customization preferences.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Dark/Light theme toggle</li>
 *   <li>Custom color schemes</li>
 *   <li>Font customization</li>
 *   <li>Layout preferences</li>
 *   <li>Theme persistence</li>
 *   <li>Real-time theme switching</li>
 * </ul>
 */
public class ThemeManager {
    private static final Logger logger = LoggerFactory.getLogger(ThemeManager.class);

    public enum Theme {
        LIGHT("Light", "/css/nova-sql-light.css"),
        DARK("Dark", "/css/nova-sql-dark.css"),
        HIGH_CONTRAST("High Contrast", "/css/nova-sql-high-contrast.css"),
        CUSTOM("Custom", "/css/nova-sql-custom.css");

        public final String displayName;
        public final String cssFile;

        Theme(String displayName, String cssFile) {
            this.displayName = displayName;
            this.cssFile = cssFile;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static final String PREFERENCES_FILE = "theme_preferences.properties";
    private static final String CUSTOM_CSS_FILE = "custom_theme.css";

    private final File dataDirectory;
    private final Properties preferences;
    private final List<Scene> managedScenes = new ArrayList<>();

    private Theme currentTheme = Theme.LIGHT;
    private String customFontFamily = "System";
    private int customFontSize = 12;
    private double customOpacity = 1.0;

    // Theme change listeners
    private final List<ThemeChangeListener> listeners = new ArrayList<>();

    public ThemeManager(String dataDirectory) {
        this.dataDirectory = new File(dataDirectory);
        this.dataDirectory.mkdirs();
        this.preferences = new Properties();

        loadPreferences();
        createThemeFiles();
    }

    /**
     * Registers a scene to be managed by the theme manager.
     */
    public void registerScene(Scene scene) {
        managedScenes.add(scene);
        applyThemeToScene(scene, currentTheme);
    }

    /**
     * Unregisters a scene from theme management.
     */
    public void unregisterScene(Scene scene) {
        managedScenes.remove(scene);
    }

    /**
     * Sets the current theme and applies it to all managed scenes.
     */
    public void setTheme(Theme theme) {
        if (theme != currentTheme) {
            Theme oldTheme = currentTheme;
            currentTheme = theme;

            // Apply to all managed scenes
            for (Scene scene : managedScenes) {
                applyThemeToScene(scene, theme);
            }

            // Save preference
            preferences.setProperty("theme", theme.name());
            savePreferences();

            // Notify listeners
            notifyThemeChanged(oldTheme, theme);

            logger.info("Theme changed to: {}", theme.displayName);
        }
    }

    /**
     * Gets the current theme.
     */
    public Theme getCurrentTheme() {
        return currentTheme;
    }

    /**
     * Toggles between light and dark themes.
     */
    public void toggleTheme() {
        Theme newTheme = currentTheme == Theme.LIGHT ? Theme.DARK : Theme.LIGHT;
        setTheme(newTheme);
    }

    /**
     * Sets custom font family.
     */
    public void setFontFamily(String fontFamily) {
        this.customFontFamily = fontFamily;
        preferences.setProperty("font.family", fontFamily);
        savePreferences();
        updateCustomTheme();
    }

    /**
     * Sets custom font size.
     */
    public void setFontSize(int fontSize) {
        this.customFontSize = fontSize;
        preferences.setProperty("font.size", String.valueOf(fontSize));
        savePreferences();
        updateCustomTheme();
    }

    /**
     * Sets application opacity.
     */
    public void setOpacity(double opacity) {
        this.customOpacity = Math.max(0.1, Math.min(1.0, opacity));
        preferences.setProperty("opacity", String.valueOf(this.customOpacity));
        savePreferences();

        // Apply opacity to all managed scenes
        for (Scene scene : managedScenes) {
            if (scene.getWindow() != null) {
                scene.getWindow().setOpacity(this.customOpacity);
            }
        }
    }

    /**
     * Gets available font families.
     */
    public List<String> getAvailableFontFamilies() {
        return javafx.scene.text.Font.getFamilies();
    }

    /**
     * Gets current font family.
     */
    public String getFontFamily() {
        return customFontFamily;
    }

    /**
     * Gets current font size.
     */
    public int getFontSize() {
        return customFontSize;
    }

    /**
     * Gets current opacity.
     */
    public double getOpacity() {
        return customOpacity;
    }

    /**
     * Creates a custom color scheme.
     */
    public void createCustomColorScheme(ColorScheme colorScheme) {
        generateCustomCSS(colorScheme);
        if (currentTheme == Theme.CUSTOM) {
            setTheme(Theme.CUSTOM); // Refresh
        }
    }

    /**
     * Exports current theme settings.
     */
    public void exportTheme(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            Properties exportProps = new Properties();
            exportProps.putAll(preferences);
            exportProps.store(writer, "Nova SQL Theme Export");
        }
    }

    /**
     * Imports theme settings.
     */
    public void importTheme(File file) throws IOException {
        Properties importProps = new Properties();
        try (FileReader reader = new FileReader(file)) {
            importProps.load(reader);
        }

        // Apply imported settings
        preferences.putAll(importProps);
        loadPreferencesFromProperties();
        savePreferences();

        // Apply current theme
        setTheme(currentTheme);
    }

    /**
     * Resets theme to defaults.
     */
    public void resetToDefaults() {
        currentTheme = Theme.LIGHT;
        customFontFamily = "System";
        customFontSize = 12;
        customOpacity = 1.0;

        preferences.clear();
        savePreferences();

        setTheme(currentTheme);
    }

    /**
     * Adds a theme change listener.
     */
    public void addThemeChangeListener(ThemeChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a theme change listener.
     */
    public void removeThemeChangeListener(ThemeChangeListener listener) {
        listeners.remove(listener);
    }

    private void applyThemeToScene(Scene scene, Theme theme) {
        // Clear existing stylesheets
        scene.getStylesheets().clear();

        // Add theme stylesheet
        String cssResource = theme.cssFile;
        String cssUrl = getClass().getResource(cssResource) != null ?
                       getClass().getResource(cssResource).toExternalForm() :
                       createThemeFileUrl(theme);

        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl);
        }

        // Apply custom styles
        applyCustomStyles(scene);
    }

    private void applyCustomStyles(Scene scene) {
        // Apply font settings
        String fontStyle = String.format(
            ".root { -fx-font-family: '%s'; -fx-font-size: %dpx; }",
            customFontFamily, customFontSize
        );

        // Create or update inline styles
        scene.getRoot().setStyle(fontStyle);

        // Apply opacity
        if (scene.getWindow() != null) {
            scene.getWindow().setOpacity(customOpacity);
        }
    }

    private void updateCustomTheme() {
        if (currentTheme == Theme.CUSTOM) {
            setTheme(Theme.CUSTOM); // Refresh custom theme
        }
    }

    private void generateCustomCSS(ColorScheme colorScheme) {
        File customCssFile = new File(dataDirectory, CUSTOM_CSS_FILE);

        try (FileWriter writer = new FileWriter(customCssFile)) {
            writer.write(generateCSSContent(colorScheme));
            logger.info("Generated custom CSS theme");
        } catch (IOException e) {
            logger.error("Error generating custom CSS", e);
        }
    }

    private String generateCSSContent(ColorScheme colorScheme) {
        return String.format("""
            /* Nova SQL Custom Theme */
            .root {
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-font-family: '%s';
                -fx-font-size: %dpx;
            }

            /* Menu Bar */
            .menu-bar {
                -fx-background-color: %s;
                -fx-border-color: %s;
            }

            /* Toolbar */
            .tool-bar {
                -fx-background-color: %s;
                -fx-border-color: %s;
            }

            /* Buttons */
            .button {
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-border-color: %s;
            }

            .button:hover {
                -fx-background-color: %s;
            }

            /* Text Areas and Code Areas */
            .text-area, .code-area {
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-border-color: %s;
            }

            /* Table Views */
            .table-view {
                -fx-background-color: %s;
                -fx-text-fill: %s;
            }

            .table-view .column-header {
                -fx-background-color: %s;
                -fx-text-fill: %s;
            }

            /* Tree Views */
            .tree-view {
                -fx-background-color: %s;
                -fx-text-fill: %s;
            }

            /* SQL Syntax Highlighting */
            .sql-keyword { -fx-fill: %s; }
            .sql-string { -fx-fill: %s; }
            .sql-comment { -fx-fill: %s; }
            .sql-number { -fx-fill: %s; }
            """,
            colorScheme.getBackground(),
            colorScheme.getTextPrimary(),
            customFontFamily,
            customFontSize,
            colorScheme.getMenuBackground(),
            colorScheme.getBorderColor(),
            colorScheme.getToolbarBackground(),
            colorScheme.getBorderColor(),
            colorScheme.getButtonBackground(),
            colorScheme.getButtonText(),
            colorScheme.getButtonBorder(),
            colorScheme.getButtonHover(),
            colorScheme.getEditorBackground(),
            colorScheme.getTextPrimary(),
            colorScheme.getBorderColor(),
            colorScheme.getTableBackground(),
            colorScheme.getTextPrimary(),
            colorScheme.getTableHeaderBackground(),
            colorScheme.getTableHeaderText(),
            colorScheme.getTreeBackground(),
            colorScheme.getTextPrimary(),
            colorScheme.getSqlKeyword(),
            colorScheme.getSqlString(),
            colorScheme.getSqlComment(),
            colorScheme.getSqlNumber()
        );
    }

    private String createThemeFileUrl(Theme theme) {
        // Create theme file if it doesn't exist in resources
        File themeFile = new File(dataDirectory, theme.name().toLowerCase() + ".css");

        if (!themeFile.exists()) {
            createDefaultThemeFile(themeFile, theme);
        }

        return themeFile.toURI().toString();
    }

    private void createDefaultThemeFile(File file, Theme theme) {
        try (FileWriter writer = new FileWriter(file)) {
            switch (theme) {
                case LIGHT:
                    writer.write(getDefaultLightTheme());
                    break;
                case DARK:
                    writer.write(getDefaultDarkTheme());
                    break;
                case HIGH_CONTRAST:
                    writer.write(getDefaultHighContrastTheme());
                    break;
                case CUSTOM:
                    writer.write(getDefaultCustomTheme());
                    break;
            }
        } catch (IOException e) {
            logger.error("Error creating theme file: " + file.getName(), e);
        }
    }

    private void createThemeFiles() {
        // Create all theme files if they don't exist
        for (Theme theme : Theme.values()) {
            createThemeFileUrl(theme);
        }
    }

    private void loadPreferences() {
        File prefsFile = new File(dataDirectory, PREFERENCES_FILE);
        if (prefsFile.exists()) {
            try (FileReader reader = new FileReader(prefsFile)) {
                preferences.load(reader);
                loadPreferencesFromProperties();
            } catch (IOException e) {
                logger.error("Error loading theme preferences", e);
            }
        }
    }

    private void loadPreferencesFromProperties() {
        // Load theme
        String themeName = preferences.getProperty("theme", Theme.LIGHT.name());
        try {
            currentTheme = Theme.valueOf(themeName);
        } catch (IllegalArgumentException e) {
            currentTheme = Theme.LIGHT;
        }

        // Load font settings
        customFontFamily = preferences.getProperty("font.family", "System");
        customFontSize = Integer.parseInt(preferences.getProperty("font.size", "12"));
        customOpacity = Double.parseDouble(preferences.getProperty("opacity", "1.0"));
    }

    private void savePreferences() {
        File prefsFile = new File(dataDirectory, PREFERENCES_FILE);
        try (FileWriter writer = new FileWriter(prefsFile)) {
            preferences.store(writer, "Nova SQL Theme Preferences");
        } catch (IOException e) {
            logger.error("Error saving theme preferences", e);
        }
    }

    private void notifyThemeChanged(Theme oldTheme, Theme newTheme) {
        for (ThemeChangeListener listener : listeners) {
            try {
                listener.onThemeChanged(oldTheme, newTheme);
            } catch (Exception e) {
                logger.error("Error notifying theme change listener", e);
            }
        }
    }

    // Default theme content methods
    private String getDefaultLightTheme() {
        return """
            /* Nova SQL Light Theme */
            .root {
                -fx-background-color: #ffffff;
                -fx-text-fill: #333333;
            }

            .menu-bar {
                -fx-background-color: #f8f9fa;
                -fx-border-color: #dee2e6;
            }

            .tool-bar {
                -fx-background-color: #ffffff;
                -fx-border-color: #dee2e6;
            }

            .button {
                -fx-background-color: #007acc;
                -fx-text-fill: white;
            }

            .text-area, .code-area {
                -fx-background-color: #ffffff;
                -fx-text-fill: #333333;
            }

            .sql-keyword { -fx-fill: #0000ff; }
            .sql-string { -fx-fill: #008000; }
            .sql-comment { -fx-fill: #808080; }
            .sql-number { -fx-fill: #ff6600; }
            """;
    }

    private String getDefaultDarkTheme() {
        return """
            /* Nova SQL Dark Theme */
            .root {
                -fx-background-color: #2b2b2b;
                -fx-text-fill: #ffffff;
            }

            .menu-bar {
                -fx-background-color: #3c3c3c;
                -fx-border-color: #555555;
            }

            .tool-bar {
                -fx-background-color: #2b2b2b;
                -fx-border-color: #555555;
            }

            .button {
                -fx-background-color: #0e639c;
                -fx-text-fill: white;
            }

            .text-area, .code-area {
                -fx-background-color: #1e1e1e;
                -fx-text-fill: #ffffff;
            }

            .sql-keyword { -fx-fill: #569cd6; }
            .sql-string { -fx-fill: #ce9178; }
            .sql-comment { -fx-fill: #6a9955; }
            .sql-number { -fx-fill: #b5cea8; }
            """;
    }

    private String getDefaultHighContrastTheme() {
        return """
            /* Nova SQL High Contrast Theme */
            .root {
                -fx-background-color: #000000;
                -fx-text-fill: #ffffff;
            }

            .menu-bar {
                -fx-background-color: #000000;
                -fx-border-color: #ffffff;
            }

            .button {
                -fx-background-color: #000000;
                -fx-text-fill: #ffffff;
                -fx-border-color: #ffffff;
            }

            .text-area, .code-area {
                -fx-background-color: #000000;
                -fx-text-fill: #ffffff;
            }

            .sql-keyword { -fx-fill: #ffff00; }
            .sql-string { -fx-fill: #00ff00; }
            .sql-comment { -fx-fill: #808080; }
            .sql-number { -fx-fill: #ff00ff; }
            """;
    }

    private String getDefaultCustomTheme() {
        return "/* Nova SQL Custom Theme - Edit this file to customize */\n";
    }

    /**
     * Interface for theme change listeners.
     */
    @FunctionalInterface
    public interface ThemeChangeListener {
        void onThemeChanged(Theme oldTheme, Theme newTheme);
    }
}