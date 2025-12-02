package com.financetracker.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration Manager for loading application properties
 */
public class ConfigManager {
    private static ConfigManager instance;
    private Properties properties;
    
    private ConfigManager() {
        properties = new Properties();
        loadProperties();
    }
    
    public static ConfigManager getInstance() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }
    
    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) {
                System.err.println("Unable to find application.properties");
                return;
            }
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    public int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    // Supabase Configuration
    public String getSupabaseUrl() {
        return getProperty("supabase.url");
    }
    
    public String getSupabaseKey() {
        return getProperty("supabase.key");
    }
    
    public String getSupabaseJwtSecret() {
        return getProperty("supabase.jwt.secret");
    }
    
    // Database Configuration
    public String getDbUrl() {
        return getProperty("db.url");
    }
    
    public String getDbUsername() {
        return getProperty("db.username");
    }
    
    public String getDbPassword() {
        return getProperty("db.password");
    }
    
    public String getDbSchema() {
        return getProperty("db.schema", "public");
    }
    
    // Budget Alert Thresholds
    public int getWarningThreshold() {
        return getIntProperty("budget.alert.warning", 80);
    }
    
    public int getDangerThreshold() {
        return getIntProperty("budget.alert.danger", 90);
    }
    
    public int getExceededThreshold() {
        return getIntProperty("budget.alert.exceeded", 100);
    }
    
    // Currency Settings
    public String getDefaultCurrency() {
        return getProperty("currency.default", "USD");
    }
    
    public String getCurrencySymbol() {
        return getProperty("currency.symbol", "$");
    }
}
