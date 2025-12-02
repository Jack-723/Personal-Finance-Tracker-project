package com.financetracker.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * Utility to test and verify Supabase connection
 */
public class ConnectionTester {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionTester.class);

    /**
     * Test database connection and print detailed information
     */
    public static boolean testConnection() {
        logger.info("=== SUPABASE CONNECTION TEST ===");

        try {
            // Load configuration
            ConfigManager config = ConfigManager.getInstance();

            logger.info("Configuration loaded:");
            logger.info("  - Supabase URL: {}", maskUrl(config.getSupabaseUrl()));
            logger.info("  - Supabase Key: {}", maskKey(config.getSupabaseKey()));
            logger.info("  - Database URL: {}", maskUrl(config.getDbUrl()));
            logger.info("  - Database User: {}", config.getDbUsername());
            logger.info("  - Database Password: {}", maskPassword(config.getDbPassword()));

            // Test database connection
            logger.info("\nTesting database connection...");
            SupabaseClient client = SupabaseClient.getInstance();
            Connection conn = client.getConnection();

            if (conn != null && !conn.isClosed()) {
                DatabaseMetaData metaData = conn.getMetaData();
                logger.info("✓ Connection successful!");
                logger.info("  - Database Product: {}", metaData.getDatabaseProductName());
                logger.info("  - Database Version: {}", metaData.getDatabaseProductVersion());
                logger.info("  - Driver: {}", metaData.getDriverName());
                logger.info("  - Driver Version: {}", metaData.getDriverVersion());
                logger.info("  - Connection URL: {}", maskUrl(metaData.getURL()));

                conn.close();
                logger.info("=== CONNECTION TEST PASSED ===\n");
                return true;
            } else {
                logger.error("✗ Connection is null or closed");
                logger.info("=== CONNECTION TEST FAILED ===\n");
                return false;
            }

        } catch (Exception e) {
            logger.error("✗ Connection test failed", e);
            logger.error("\nCommon issues:");
            logger.error("1. Check application.properties exists in src/main/resources/");
            logger.error("2. Verify Supabase URL is correct");
            logger.error("3. Verify database password is correct");
            logger.error("4. Check if Supabase project is active");
            logger.error("5. Verify internet connection");
            logger.info("=== CONNECTION TEST FAILED ===\n");
            return false;
        }
    }

    /**
     * Mask URL for security
     */
    private static String maskUrl(String url) {
        if (url == null || url.isEmpty()) return "[NOT SET]";
        if (url.length() < 20) return url;
        return url.substring(0, 20) + "..." + url.substring(url.length() - 10);
    }

    /**
     * Mask API key for security
     */
    private static String maskKey(String key) {
        if (key == null || key.isEmpty()) return "[NOT SET]";
        if (key.length() < 20) return "***";
        return key.substring(0, 10) + "..." + key.substring(key.length() - 5);
    }

    /**
     * Mask password for security
     */
    private static String maskPassword(String password) {
        if (password == null || password.isEmpty()) return "[NOT SET]";
        return "***" + password.substring(Math.max(0, password.length() - 3));
    }

    /**
     * Check if all required configuration is present
     */
    public static boolean validateConfiguration() {
        ConfigManager config = ConfigManager.getInstance();

        boolean valid = true;

        if (config.getSupabaseUrl() == null || config.getSupabaseUrl().isEmpty()) {
            logger.error("✗ Supabase URL is not set in application.properties");
            valid = false;
        }

        if (config.getSupabaseKey() == null || config.getSupabaseKey().isEmpty()) {
            logger.error("✗ Supabase Key is not set in application.properties");
            valid = false;
        }

        if (config.getDbUrl() == null || config.getDbUrl().isEmpty()) {
            logger.error("✗ Database URL is not set in application.properties");
            valid = false;
        }

        if (config.getDbPassword() == null || config.getDbPassword().isEmpty()) {
            logger.error("✗ Database Password is not set in application.properties");
            valid = false;
        }

        if (valid) {
            logger.info("✓ All required configuration is present");
        } else {
            logger.error("\nPlease update src/main/resources/application.properties");
        }

        return valid;
    }
}