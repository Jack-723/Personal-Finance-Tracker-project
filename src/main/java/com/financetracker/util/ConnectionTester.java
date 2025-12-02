package com.financetracker.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Utility class to test database connections
 */
public class ConnectionTester {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionTester.class);

    /**
     * Validate that configuration is loaded properly
     */
    public static void validateConfiguration() {
        ConfigManager config = ConfigManager.getInstance();
        
        String supabaseUrl = config.getSupabaseUrl();
        String dbUrl = config.getDbUrl();
        
        if (supabaseUrl == null || supabaseUrl.isEmpty()) {
            logger.error("❌ Supabase URL is not configured");
            throw new RuntimeException("Supabase URL not found in configuration");
        }
        
        if (dbUrl == null || dbUrl.isEmpty()) {
            logger.error("❌ Database URL is not configured");
            throw new RuntimeException("Database URL not found in configuration");
        }
        
        logger.info("✓ Configuration validated");
        logger.info("Supabase URL: {}", supabaseUrl);
        logger.info("Database URL: {}", dbUrl.replaceAll(":[^:]+@", ":****@")); // Hide password
    }

    /**
     * Test database connection
     */
    public static void testConnection() {
        logger.info("Testing database connection...");
        SupabaseClient client = SupabaseClient.getInstance();
        
        try (Connection conn = client.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                logger.info("✓ Database connection successful");
                logger.info("Database: {}", conn.getMetaData().getDatabaseProductName());
                logger.info("Version: {}", conn.getMetaData().getDatabaseProductVersion());
            } else {
                logger.error("❌ Database connection failed");
            }
        } catch (SQLException e) {
            logger.error("❌ Database connection error", e);
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    /**
     * Test Supabase client initialization
     */
    public static boolean testSupabaseClient() {
        try {
            SupabaseClient client = SupabaseClient.getInstance();
            boolean configured = client.isConfigured();
            
            if (configured) {
                logger.info("✓ Supabase client initialized successfully");
            } else {
                logger.warn("⚠ Supabase client not fully configured");
            }
            
            return configured;
        } catch (Exception e) {
            logger.error("❌ Supabase client initialization failed", e);
            return false;
        }
    }
}
