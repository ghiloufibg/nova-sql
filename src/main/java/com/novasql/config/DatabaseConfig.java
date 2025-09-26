package com.novasql.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DatabaseConfig {
    private static final String DEFAULT_CONFIG_FILE = "nova.properties";

    private final Properties properties;

    // Default values
    public static final int DEFAULT_BUFFER_POOL_SIZE = 1000;
    public static final int DEFAULT_MAX_CONNECTIONS = 100;
    public static final String DEFAULT_LOG_LEVEL = "INFO";
    public static final String DEFAULT_DATA_DIRECTORY = "./data";
    public static final int DEFAULT_PAGE_SIZE = 4096;

    public DatabaseConfig() {
        this.properties = new Properties();
        loadDefaultProperties();
        loadConfigFile();
    }

    public DatabaseConfig(String configFile) {
        this.properties = new Properties();
        loadDefaultProperties();
        loadConfigFile(configFile);
    }

    private void loadDefaultProperties() {
        properties.setProperty("buffer.pool.size", String.valueOf(DEFAULT_BUFFER_POOL_SIZE));
        properties.setProperty("max.connections", String.valueOf(DEFAULT_MAX_CONNECTIONS));
        properties.setProperty("log.level", DEFAULT_LOG_LEVEL);
        properties.setProperty("data.directory", DEFAULT_DATA_DIRECTORY);
        properties.setProperty("page.size", String.valueOf(DEFAULT_PAGE_SIZE));
        properties.setProperty("enable.wal", "true");
        properties.setProperty("wal.sync.interval", "1000");
        properties.setProperty("auto.create.indexes", "true");
    }

    private void loadConfigFile() {
        loadConfigFile(DEFAULT_CONFIG_FILE);
    }

    private void loadConfigFile(String configFile) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(configFile)) {
            if (is != null) {
                properties.load(is);
            }
        } catch (IOException e) {
            // Config file not found or couldn't be read, use defaults
        }
    }

    public int getBufferPoolSize() {
        return Integer.parseInt(properties.getProperty("buffer.pool.size", String.valueOf(DEFAULT_BUFFER_POOL_SIZE)));
    }

    public int getMaxConnections() {
        return Integer.parseInt(properties.getProperty("max.connections", String.valueOf(DEFAULT_MAX_CONNECTIONS)));
    }

    public String getLogLevel() {
        return properties.getProperty("log.level", DEFAULT_LOG_LEVEL);
    }

    public String getDataDirectory() {
        return properties.getProperty("data.directory", DEFAULT_DATA_DIRECTORY);
    }

    public int getPageSize() {
        return Integer.parseInt(properties.getProperty("page.size", String.valueOf(DEFAULT_PAGE_SIZE)));
    }

    public boolean isWALEnabled() {
        return Boolean.parseBoolean(properties.getProperty("enable.wal", "true"));
    }

    public long getWALSyncInterval() {
        return Long.parseLong(properties.getProperty("wal.sync.interval", "1000"));
    }

    public boolean isAutoCreateIndexes() {
        return Boolean.parseBoolean(properties.getProperty("auto.create.indexes", "true"));
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    public Properties getAllProperties() {
        return new Properties(properties);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DatabaseConfig{\n");
        sb.append("  bufferPoolSize=").append(getBufferPoolSize()).append("\n");
        sb.append("  maxConnections=").append(getMaxConnections()).append("\n");
        sb.append("  logLevel=").append(getLogLevel()).append("\n");
        sb.append("  dataDirectory=").append(getDataDirectory()).append("\n");
        sb.append("  pageSize=").append(getPageSize()).append("\n");
        sb.append("  walEnabled=").append(isWALEnabled()).append("\n");
        sb.append("  autoCreateIndexes=").append(isAutoCreateIndexes()).append("\n");
        sb.append("}");
        return sb.toString();
    }
}