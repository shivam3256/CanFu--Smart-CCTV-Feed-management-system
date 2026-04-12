package com.camfu.surveillance.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Configuration Loader
 * Loads application configuration from properties file
 */
public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    
    private static Properties properties;
    private static final String CONFIG_FILE = "config/application.properties";

    static {
        properties = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            properties.load(fis);
            logger.info("Configuration loaded from: " + CONFIG_FILE);
        } catch (IOException e) {
            logger.warn("Failed to load configuration from " + CONFIG_FILE + ". Using defaults.", e);
        }
    }

    /**
     * Get a configuration value
     */
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Get a configuration value with default fallback
     */
    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Get a configuration value as integer
     */
    public static int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer value for property: " + key);
            return defaultValue;
        }
    }

    /**
     * Get a configuration value as boolean
     */
    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
}