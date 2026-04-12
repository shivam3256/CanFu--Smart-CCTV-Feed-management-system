package com.camfu.surveillance.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database Connection Pool Manager
 * Manages MySQL database connections for the application
 */
public class DatabaseConnectionPool {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionPool.class);
    
    private static final String DB_URL = "jdbc:mysql://localhost:3306/camfu_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Shivam@9797";
    private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";

    static {
        try {
            Class.forName(DB_DRIVER);
            logger.info("MySQL JDBC Driver loaded successfully");
        } catch (ClassNotFoundException e) {
            logger.error("Failed to load MySQL JDBC Driver", e);
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Get a database connection
     */
    public static Connection getConnection() throws SQLException {
        try {
            logger.info("Attempting to connect to: " + DB_URL);
            logger.info("Using user: " + DB_USER);
            long startTime = System.currentTimeMillis();
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            long elapsed = System.currentTimeMillis() - startTime;
            logger.info("Database connection established successfully in " + elapsed + "ms");
            return conn;
        } catch (SQLException e) {
            logger.error("Failed to establish database connection to " + DB_URL + " with user " + DB_USER, e);
            logger.error("Error code: " + e.getErrorCode());
            logger.error("SQL State: " + e.getSQLState());
            throw e;
        }
    }

    /**
     * Test database connectivity
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            logger.info("Database connection test successful");
            return true;
        } catch (SQLException e) {
            logger.error("Database connection test failed", e);
            return false;
        }
    }
}