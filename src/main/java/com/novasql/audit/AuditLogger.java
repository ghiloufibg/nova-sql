package com.novasql.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AuditLogger {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogger.class);

    private final String auditLogFile;
    private final BlockingQueue<AuditEntry> auditQueue;
    private final Thread auditThread;
    private volatile boolean running = false;

    public AuditLogger(String auditLogFile) {
        this.auditLogFile = auditLogFile;
        this.auditQueue = new LinkedBlockingQueue<>();
        this.auditThread = new Thread(this::processAuditEntries, "AuditLogger");
    }

    public void start() {
        running = true;
        auditThread.start();
        logger.info("Audit logging started, writing to: {}", auditLogFile);
    }

    public void stop() {
        running = false;
        auditThread.interrupt();
        try {
            auditThread.join(5000); // Wait up to 5 seconds for thread to finish
        } catch (InterruptedException e) {
            logger.warn("Audit thread did not stop cleanly", e);
        }
    }

    public void logDML(String operation, String tableName, String sql, String user, boolean success, String error) {
        AuditEntry entry = new AuditEntry(
            Instant.now(),
            operation,
            tableName,
            sql,
            user != null ? user : "system",
            success,
            error
        );

        try {
            auditQueue.offer(entry);
        } catch (Exception e) {
            logger.error("Failed to queue audit entry", e);
        }
    }

    public void logDDL(String operation, String objectName, String sql, String user, boolean success, String error) {
        logDML(operation, objectName, sql, user, success, error);
    }

    private void processAuditEntries() {
        logger.debug("Audit processing thread started");

        while (running || !auditQueue.isEmpty()) {
            try {
                AuditEntry entry = auditQueue.take();
                writeAuditEntry(entry);
            } catch (InterruptedException e) {
                if (running) {
                    logger.warn("Audit thread interrupted while running", e);
                }
                break;
            } catch (Exception e) {
                logger.error("Error processing audit entry", e);
            }
        }

        logger.debug("Audit processing thread stopped");
    }

    private void writeAuditEntry(AuditEntry entry) {
        try (FileWriter writer = new FileWriter(auditLogFile, true)) {
            String logLine = String.format("%s|%s|%s|%s|%s|%s|%s%n",
                DateTimeFormatter.ISO_INSTANT.format(entry.getTimestamp()),
                entry.getOperation(),
                entry.getTableName() != null ? entry.getTableName() : "",
                entry.getUser(),
                entry.isSuccess() ? "SUCCESS" : "FAILURE",
                entry.getSql().replace('\n', ' ').replace('\r', ' '),
                entry.getError() != null ? entry.getError() : ""
            );

            writer.write(logLine);
            writer.flush();

        } catch (IOException e) {
            logger.error("Failed to write audit entry to file: {}", auditLogFile, e);
        }
    }

    public static class AuditEntry {
        private final Instant timestamp;
        private final String operation;
        private final String tableName;
        private final String sql;
        private final String user;
        private final boolean success;
        private final String error;

        public AuditEntry(Instant timestamp, String operation, String tableName, String sql, String user, boolean success, String error) {
            this.timestamp = timestamp;
            this.operation = operation;
            this.tableName = tableName;
            this.sql = sql;
            this.user = user;
            this.success = success;
            this.error = error;
        }

        public Instant getTimestamp() { return timestamp; }
        public String getOperation() { return operation; }
        public String getTableName() { return tableName; }
        public String getSql() { return sql; }
        public String getUser() { return user; }
        public boolean isSuccess() { return success; }
        public String getError() { return error; }
    }
}