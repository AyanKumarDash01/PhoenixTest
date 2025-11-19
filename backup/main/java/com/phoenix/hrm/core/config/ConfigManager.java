package com.phoenix.hrm.core.config;

import com.phoenix.hrm.core.exceptions.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Enterprise ConfigManager implementing singleton pattern for centralized configuration management.
 * Manages environment-specific configurations and runtime parameter overrides.
 * 
 * Features:
 * - Singleton pattern implementation
 * - Environment-specific property files (dev, test, prod)
 * - Runtime parameter override capability
 * - Type-safe configuration getters
 * - Default fallback values
 * - Thread-safe lazy initialization
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 1.0
 */
public class ConfigManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static volatile ConfigManager instance;
    private final Properties properties;
    
    // Configuration file names
    private static final String DEFAULT_CONFIG_FILE = "config/config.properties";
    private static final String DEV_CONFIG_FILE = "config/dev.properties";
    private static final String TEST_CONFIG_FILE = "config/test.properties";
    private static final String PROD_CONFIG_FILE = "config/prod.properties";
    
    // Configuration keys
    private static final String BROWSER_KEY = "browser";
    private static final String HEADLESS_KEY = "headless";
    private static final String BASE_URL_KEY = "base.url";
    private static final String API_BASE_URL_KEY = "api.base.url";
    private static final String IMPLICIT_WAIT_KEY = "implicit.wait";
    private static final String EXPLICIT_WAIT_KEY = "explicit.wait";
    private static final String PAGE_LOAD_TIMEOUT_KEY = "page.load.timeout";
    private static final String SCRIPT_TIMEOUT_KEY = "script.timeout";
    private static final String SCREENSHOT_ON_FAILURE_KEY = "screenshot.on.failure";
    private static final String EXTENT_REPORT_PATH_KEY = "extent.report.path";
    private static final String TEST_DATA_PATH_KEY = "test.data.path";
    private static final String ADMIN_USERNAME_KEY = "admin.username";
    private static final String ADMIN_PASSWORD_KEY = "admin.password";
    private static final String RETRY_COUNT_KEY = "retry.count";
    private static final String PARALLEL_EXECUTION_KEY = "parallel.execution";
    private static final String THREAD_COUNT_KEY = "thread.count";
    
    // Default values
    private static final String DEFAULT_BROWSER = "chrome";
    private static final boolean DEFAULT_HEADLESS = false;
    private static final String DEFAULT_BASE_URL = "https://opensource-demo.orangehrmlive.com/";
    private static final String DEFAULT_API_BASE_URL = "https://dummy.restapiexample.com/api/v1";
    private static final int DEFAULT_IMPLICIT_WAIT = 10;
    private static final int DEFAULT_EXPLICIT_WAIT = 20;
    private static final int DEFAULT_PAGE_LOAD_TIMEOUT = 30;
    private static final int DEFAULT_SCRIPT_TIMEOUT = 30;
    private static final boolean DEFAULT_SCREENSHOT_ON_FAILURE = true;
    private static final String DEFAULT_EXTENT_REPORT_PATH = "src/test/resources/reports/ExtentReport.html";
    private static final String DEFAULT_TEST_DATA_PATH = "src/test/resources/testdata";
    private static final String DEFAULT_ADMIN_USERNAME = "Admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";
    private static final int DEFAULT_RETRY_COUNT = 1;
    private static final boolean DEFAULT_PARALLEL_EXECUTION = false;
    private static final int DEFAULT_THREAD_COUNT = 3;
    
    /**
     * Private constructor for singleton pattern
     * Loads configuration based on environment
     */
    private ConfigManager() {
        properties = new Properties();
        loadConfiguration();
        overrideWithSystemProperties();
        logConfigurationValues();
    }
    
    /**
     * Gets singleton instance with thread-safe lazy initialization
     * 
     * @return ConfigManager instance
     */
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
    
    /**
     * Loads configuration from environment-specific property files
     */
    private void loadConfiguration() {
        String environment = System.getProperty("env", "dev");
        logger.info("Loading configuration for environment: {}", environment);
        
        try {
            // Load default configuration first
            loadPropertiesFile(DEFAULT_CONFIG_FILE);
            
            // Override with environment-specific configuration
            String envConfigFile = getEnvironmentConfigFile(environment);
            if (envConfigFile != null) {
                loadPropertiesFile(envConfigFile);
                logger.info("Environment-specific configuration loaded: {}", envConfigFile);
            }
            
        } catch (Exception e) {
            logger.error("Failed to load configuration", e);
            throw new FrameworkException("Configuration loading failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gets environment-specific configuration file path
     * 
     * @param environment Environment name
     * @return Configuration file path or null
     */
    private String getEnvironmentConfigFile(String environment) {
        return switch (environment.toLowerCase()) {
            case "dev" -> DEV_CONFIG_FILE;
            case "test" -> TEST_CONFIG_FILE;
            case "prod" -> PROD_CONFIG_FILE;
            default -> null;
        };
    }
    
    /**
     * Loads properties from file
     * 
     * @param fileName Property file name
     */
    private void loadPropertiesFile(String fileName) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream != null) {
                properties.load(inputStream);
                logger.debug("Loaded properties from: {}", fileName);
            } else {
                logger.warn("Property file not found: {}", fileName);
            }
        } catch (IOException e) {
            logger.warn("Failed to load property file: {}", fileName, e);
        }
    }
    
    /**
     * Overrides configuration with system properties and Maven parameters
     */
    private void overrideWithSystemProperties() {
        System.getProperties().forEach((key, value) -> {
            String keyStr = key.toString();
            if (isConfigurationKey(keyStr)) {
                properties.setProperty(keyStr, value.toString());
                logger.debug("Overridden property: {} = {}", keyStr, value);
            }
        });
    }
    
    /**
     * Checks if key is a valid configuration key
     * 
     * @param key Property key
     * @return true if valid configuration key
     */
    private boolean isConfigurationKey(String key) {
        return key.equals(BROWSER_KEY) ||
               key.equals(HEADLESS_KEY) ||
               key.equals(BASE_URL_KEY) ||
               key.equals(API_BASE_URL_KEY) ||
               key.contains("wait") ||
               key.contains("timeout") ||
               key.contains("username") ||
               key.contains("password") ||
               key.contains("retry") ||
               key.contains("parallel") ||
               key.contains("thread");
    }
    
    /**
     * Logs current configuration values for debugging
     */
    private void logConfigurationValues() {
        logger.info("=== Phoenix HRM Configuration ===");
        logger.info("Browser: {}", getBrowser());
        logger.info("Headless: {}", isHeadless());
        logger.info("Base URL: {}", getBaseUrl());
        logger.info("API Base URL: {}", getApiBaseUrl());
        logger.info("Implicit Wait: {}s", getImplicitWait());
        logger.info("Explicit Wait: {}s", getExplicitWait());
        logger.info("Page Load Timeout: {}s", getPageLoadTimeout());
        logger.info("Screenshot on Failure: {}", isScreenshotOnFailure());
        logger.info("Retry Count: {}", getRetryCount());
        logger.info("Parallel Execution: {}", isParallelExecution());
        logger.info("Thread Count: {}", getThreadCount());
        logger.info("===============================");
    }
    
    // Configuration getter methods with type safety and default values
    
    public String getBrowser() {
        return getProperty(BROWSER_KEY, DEFAULT_BROWSER);
    }
    
    public boolean isHeadless() {
        return getBooleanProperty(HEADLESS_KEY, DEFAULT_HEADLESS);
    }
    
    public String getBaseUrl() {
        return getProperty(BASE_URL_KEY, DEFAULT_BASE_URL);
    }
    
    public String getApiBaseUrl() {
        return getProperty(API_BASE_URL_KEY, DEFAULT_API_BASE_URL);
    }
    
    public int getImplicitWait() {
        return getIntProperty(IMPLICIT_WAIT_KEY, DEFAULT_IMPLICIT_WAIT);
    }
    
    public int getExplicitWait() {
        return getIntProperty(EXPLICIT_WAIT_KEY, DEFAULT_EXPLICIT_WAIT);
    }
    
    public int getPageLoadTimeout() {
        return getIntProperty(PAGE_LOAD_TIMEOUT_KEY, DEFAULT_PAGE_LOAD_TIMEOUT);
    }
    
    public int getScriptTimeout() {
        return getIntProperty(SCRIPT_TIMEOUT_KEY, DEFAULT_SCRIPT_TIMEOUT);
    }
    
    public boolean isScreenshotOnFailure() {
        return getBooleanProperty(SCREENSHOT_ON_FAILURE_KEY, DEFAULT_SCREENSHOT_ON_FAILURE);
    }
    
    public String getExtentReportPath() {
        return getProperty(EXTENT_REPORT_PATH_KEY, DEFAULT_EXTENT_REPORT_PATH);
    }
    
    public String getTestDataPath() {
        return getProperty(TEST_DATA_PATH_KEY, DEFAULT_TEST_DATA_PATH);
    }
    
    public String getAdminUsername() {
        return getProperty(ADMIN_USERNAME_KEY, DEFAULT_ADMIN_USERNAME);
    }
    
    public String getAdminPassword() {
        return getProperty(ADMIN_PASSWORD_KEY, DEFAULT_ADMIN_PASSWORD);
    }
    
    public int getRetryCount() {
        return getIntProperty(RETRY_COUNT_KEY, DEFAULT_RETRY_COUNT);
    }
    
    public boolean isParallelExecution() {
        return getBooleanProperty(PARALLEL_EXECUTION_KEY, DEFAULT_PARALLEL_EXECUTION);
    }
    
    public int getThreadCount() {
        return getIntProperty(THREAD_COUNT_KEY, DEFAULT_THREAD_COUNT);
    }
    
    /**
     * Gets string property with default value
     * 
     * @param key Property key
     * @param defaultValue Default value if key not found
     * @return Property value or default
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Gets boolean property with default value
     * 
     * @param key Property key
     * @param defaultValue Default value if key not found
     * @return Property value or default
     */
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
    
    /**
     * Gets integer property with default value
     * 
     * @param key Property key
     * @param defaultValue Default value if key not found
     * @return Property value or default
     */
    public int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer value for key '{}': '{}'. Using default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Sets property value at runtime
     * 
     * @param key Property key
     * @param value Property value
     */
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
        logger.debug("Set property: {} = {}", key, value);
    }
    
    /**
     * Gets all properties
     * 
     * @return Properties object
     */
    public Properties getAllProperties() {
        return new Properties(properties);
    }
}