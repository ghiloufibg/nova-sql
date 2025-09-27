package com.novasql.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DiskManager {
    private static final Logger logger = LoggerFactory.getLogger(DiskManager.class);
    private static final String DATA_FILE_EXTENSION = ".ndb";

    private final String dataDirectory;
    private final String databaseName;
    private final RandomAccessFile dataFile;

    public DiskManager(String dataDirectory, String databaseName) throws IOException {
        this.dataDirectory = dataDirectory;
        this.databaseName = databaseName;

        Path dirPath = Paths.get(dataDirectory);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
            logger.info("Created data directory: {}", dataDirectory);
        }

        String fileName = Paths.get(dataDirectory, databaseName + DATA_FILE_EXTENSION).toString();
        this.dataFile = new RandomAccessFile(fileName, "rw");
        logger.info("Opened database file: {}", fileName);
    }

    public Page readPage(int pageId) {
        try {
            long offset = (long) pageId * Page.PAGE_SIZE;
            if (offset >= dataFile.length()) {
                return null; // Page doesn't exist
            }

            dataFile.seek(offset);
            byte[] pageData = new byte[Page.PAGE_SIZE];
            int bytesRead = dataFile.read(pageData);

            if (bytesRead < Page.PAGE_SIZE) {
                logger.warn("Incomplete page read for page {}, only {} bytes", pageId, bytesRead);
                return null;
            }

            logger.debug("Read page {} from disk", pageId);
            return new Page(pageId, pageData);

        } catch (IOException e) {
            logger.error("Failed to read page {}", pageId, e);
            return null;
        }
    }

    public void writePage(Page page) {
        try {
            long offset = (long) page.getPageId() * Page.PAGE_SIZE;
            dataFile.seek(offset);
            dataFile.write(page.getData());
            dataFile.getFD().sync(); // Force write to disk
            logger.debug("Wrote page {} to disk", page.getPageId());

        } catch (IOException e) {
            logger.error("Failed to write page {}", page.getPageId(), e);
            throw new RuntimeException("Disk write failed", e);
        }
    }

    public int allocateNewPage() {
        try {
            long fileLength = dataFile.length();
            int newPageId = (int) (fileLength / Page.PAGE_SIZE);

            // Extend file to accommodate new page
            dataFile.setLength(fileLength + Page.PAGE_SIZE);

            logger.debug("Allocated new page with ID {}", newPageId);
            return newPageId;

        } catch (IOException e) {
            logger.error("Failed to allocate new page", e);
            throw new RuntimeException("Page allocation failed", e);
        }
    }

    public void close() {
        try {
            dataFile.close();
            logger.info("Closed database file for {}", databaseName);
        } catch (IOException e) {
            logger.error("Failed to close database file", e);
        }
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getDataDirectory() {
        return dataDirectory;
    }
}