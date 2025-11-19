package com.phoenix.hrm.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Enhanced Configuration Manager for Phoenix HRM Test Automation Framework
 * Provides centralized configuration management with environment-specific overrides
 */
public class ConfigManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static ConfigManager instance;
    private Properties properties;
    private Properties defaultProperties;
    
    // Configuration file paths
    private static final String DEFAULT_CONFIG = "config/default.properties";
    private static final String ENV_CONFIG_TEMPLATE = "config/%s.properties";
    
    // Default values
    private static final String DEFAULT_BROWSER = "chrome";
    private static final String DEFAULT_TIMEOUT = "10";
    private static final String DEFAULT_ENVIRONMENT = "test";
    private static final String DEFAULT_BASE_URL = "https://opensource-demo.orangehrmlive.com/";
    
    private ConfigManager() {
        loadConfiguration();
    }
    
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    private void loadConfiguration() {
        logger.info("Loading configuration properties...");
        
        properties = new Properties();
        defaultProperties = new Properties();
        
        // Load default properties
        loadPropertiesFile(DEFAULT_CONFIG, defaultProperties);
        
        // Load environment-specific properties
        String environment = System.getProperty("test.env", DEFAULT_ENVIRONMENT);
        String envConfigFile = String.format(ENV_CONFIG_TEMPLATE, environment);
        loadPropertiesFile(envConfigFile, properties);
        
        // Override with system properties
        overrideWithSystemProperties();
        
        logger.info("Configuration loaded successfully. Environment: {}", environment);
        logConfigurationSummary();
    }
    
    private void loadPropertiesFile(String fileName, Properties targetProperties) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream != null) {
                targetProperties.load(inputStream);
                logger.debug("Loaded properties from: {}", fileName);
            } else {
                logger.warn("Configuration file not found: {}", fileName);
            }
        } catch (IOException e) {
            logger.warn("Failed to load configuration file: {} - Error: {}", fileName, e.getMessage());
        }
    }
    
    private void overrideWithSystemProperties() {
        // Override common properties with system properties if provided
        overrideIfSystemPropertyExists("browser");
        overrideIfSystemPropertyExists("base.url");
        overrideIfSystemPropertyExists("implicit.wait");
        overrideIfSystemPropertyExists("explicit.wait");
        overrideIfSystemPropertyExists("headless");
        overrideIfSystemPropertyExists("parallel.execution");
        overrideIfSystemPropertyExists("thread.count");
    }
    
    private void overrideIfSystemPropertyExists(String key) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.trim().isEmpty()) {
            properties.setProperty(key, systemValue);
            logger.debug("Overridden property '{}' with system property value: {}", key, systemValue);
        }
    }
    
    private void logConfigurationSummary() {
        if (logger.isInfoEnabled()) {
            logger.info("=== Configuration Summary ===");
            logger.info("Browser: {}", getBrowser());
            logger.info("Base URL: {}", getBaseUrl());
            logger.info("Headless Mode: {}", isHeadless());
            logger.info("Implicit Wait: {} seconds", getImplicitWait());
            logger.info("Explicit Wait: {} seconds", getExplicitWait());
            logger.info("Parallel Execution: {}", isParallelExecution());
            logger.info("Thread Count: {}", getThreadCount());
            logger.info("=============================");
        }
    }
    
    /**
     * Get property value with fallback to default properties
     */
    public String getProperty(String key, String defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            value = defaultProperties.getProperty(key, defaultValue);
        }
        return value;
    }
    
    /**
     * Get property value with no default
     */
    public String getProperty(String key) {
        return getProperty(key, null);
    }
    
    /**
     * Get integer property with default value
     */
    public int getIntProperty(String key, int defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                logger.warn("Invalid integer value '{}' for property '{}', using default: {}", 
                    value, key, defaultValue);
            }
        }
        return defaultValue;
    }
    
    /**
     * Get boolean property with default value
     */
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value.trim());
        }
        return defaultValue;
    }
    
    // Specific configuration getters
    public String getBrowser() {
        return getProperty("browser", DEFAULT_BROWSER);
    }
    
    public String getBaseUrl() {
        return getProperty("base.url", DEFAULT_BASE_URL);
    }
    
    public int getImplicitWait() {
        return getIntProperty("implicit.wait", Integer.parseInt(DEFAULT_TIMEOUT));
    }
    
    public int getExplicitWait() {
        return getIntProperty("explicit.wait", Integer.parseInt(DEFAULT_TIMEOUT));
    }
    
    public boolean isHeadless() {
        return getBooleanProperty("headless", false);
    }
    
    public boolean isParallelExecution() {
        return getBooleanProperty("parallel.execution", false);
    }
    
    public int getThreadCount() {
        return getIntProperty("thread.count", 1);
    }
    
    public String getUsername() {
        return getProperty("test.username", "Admin");
    }
    
    public String getPassword() {
        return getProperty("test.password", "admin123");
    }
    
    public String getReportPath() {
        return getProperty("report.path", "target/reports");
    }
    
    public String getScreenshotPath() {
        return getProperty("screenshot.path", "target/screenshots");
    }
    
    public boolean isScreenshotEnabled() {
        return getBooleanProperty("screenshot.enabled", true);
    }
    
    public String getApiBaseUrl() {
        return getProperty("api.base.url", "https://jsonplaceholder.typicode.com");
    }
    
    public int getApiTimeout() {
        return getIntProperty("api.timeout", 30);
    }
    
    public String getDbUrl() {
        return getProperty("db.url");
    }
    
    public String getDbUsername() {
        return getProperty("db.username");
    }
    
    /**
     * Get boolean property with default
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return getBooleanProperty(key, defaultValue);
    }
    
    /**
     * Get environment name
     */
    public String getEnvironment() {
        return getProperty("test.environment", "dev");
    }
    
    public String getDbPassword() {
        return getProperty("db.password");
    }
    
    // Performance testing configurations
    public int getPerformanceTestDuration() {
        return getIntProperty("performance.test.duration", 120); // 2 minutes default
    }
    
    public String getPerformanceTestUsers() {
        return getProperty("performance.test.users", "5,10,15");
    }
    
    // Utility methods
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
        logger.debug("Set property '{}' to value: {}", key, value);
    }
    
    public void reloadConfiguration() {
        logger.info("Reloading configuration...");
        loadConfiguration();
    }
    
    /**
     * Get all properties for debugging
     */
    public Properties getAllProperties() {
        Properties allProps = new Properties();
        allProps.putAll(defaultProperties);
        allProps.putAll(properties);
        return allProps;
    }
}