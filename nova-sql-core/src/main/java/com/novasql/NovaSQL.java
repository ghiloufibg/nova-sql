package com.novasql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NovaSQL {
    private static final Logger logger = LoggerFactory.getLogger(NovaSQL.class);

    public static void main(String[] args) {
        logger.info("Starting Nova SQL Database Engine");

        DatabaseEngine engine = new DatabaseEngine();
        engine.start();

        logger.info("Nova SQL Database Engine started successfully");
    }
}