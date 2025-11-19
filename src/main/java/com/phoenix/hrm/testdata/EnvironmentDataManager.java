package com.phoenix.hrm.testdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Environment Data Manager for Phoenix HRM Test Automation Framework
 * 
 * Provides environment-specific data management including:
 * - Environment configuration loading and validation
 * - Data inheritance from parent environments
 * - Environment-specific data overrides
 * - Multi-tier environment support (dev -> test -> stage -> prod)
 * - Environment variable substitution
 * - Dynamic environment switching during test execution
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 4.0
 * @since Phase 4
 */
public class EnvironmentDataManager {
    
    private static final Logger logger = LoggerFactory.getLogger(EnvironmentDataManager.class);
    
    private final TestDataManager.TestDataConfig config;
    private final String currentEnvironment;
    private final ObjectMapper objectMapper;
    private final Map<String, EnvironmentConfig> environmentConfigs;
    private final Map<String, Map<String, Object>> environmentOverrides;
    
    /**
     * Environment configuration
     */
    public static class EnvironmentConfig {
        private String name;
        private String parentEnvironment;
        private String description;
        private Map<String, Object> properties;
        private Map<String, String> connectionStrings;
        private Map<String, Object> dataOverrides;
        private List<String> allowedOperations;
        private boolean isProduction;
        
        // Constructor
        public EnvironmentConfig() {
            this.properties = new HashMap<>();
            this.connectionStrings = new HashMap<>();
            this.dataOverrides = new HashMap<>();
            this.allowedOperations = new ArrayList<>();
            this.isProduction = false;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getParentEnvironment() { return parentEnvironment; }
        public void setParentEnvironment(String parentEnvironment) { this.parentEnvironment = parentEnvironment; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public Map<String, Object> getProperties() { return properties; }
        public void setProperties(Map<String, Object> properties) { this.properties = properties; }
        
        public Map<String, String> getConnectionStrings() { return connectionStrings; }
        public void setConnectionStrings(Map<String, String> connectionStrings) { this.connectionStrings = connectionStrings; }
        
        public Map<String, Object> getDataOverrides() { return dataOverrides; }
        public void setDataOverrides(Map<String, Object> dataOverrides) { this.dataOverrides = dataOverrides; }
        
        public List<String> getAllowedOperations() { return allowedOperations; }
        public void setAllowedOperations(List<String> allowedOperations) { this.allowedOperations = allowedOperations; }
        
        public boolean isProduction() { return isProduction; }
        public void setProduction(boolean production) { isProduction = production; }
    }
    
    /**
     * Constructor
     */
    public EnvironmentDataManager(TestDataManager.TestDataConfig config, String currentEnvironment) {
        this.config = config;
        this.currentEnvironment = currentEnvironment;
        this.objectMapper = new ObjectMapper();
        this.environmentConfigs = new HashMap<>();
        this.environmentOverrides = new HashMap<>();
        
        logger.debug("Initialized EnvironmentDataManager for environment: {}", currentEnvironment);
    }
    
    /**
     * Load environment configuration
     */
    public void loadEnvironmentConfig() {
        try {
            Path envConfigPath = getEnvironmentConfigPath(currentEnvironment);
            
            if (Files.exists(envConfigPath)) {
                EnvironmentConfig envConfig = objectMapper.readValue(envConfigPath.toFile(), EnvironmentConfig.class);
                environmentConfigs.put(currentEnvironment, envConfig);
                
                // Load parent environment configurations
                loadParentEnvironments(envConfig);
                
                logger.info("Loaded environment configuration for: {}", currentEnvironment);
            } else {
                // Create default environment configuration
                createDefaultEnvironmentConfig();
                logger.warn("Environment configuration not found, created default for: {}", currentEnvironment);
            }
            
        } catch (IOException e) {
            logger.error("Error loading environment configuration for: {}", currentEnvironment, e);
            createDefaultEnvironmentConfig();
        }
    }
    
    /**
     * Apply environment-specific overrides to dataset
     */
    public TestDataManager.DataSet applyEnvironmentOverrides(TestDataManager.DataSet originalDataSet) {
        EnvironmentConfig envConfig = environmentConfigs.get(currentEnvironment);
        
        if (envConfig == null || envConfig.getDataOverrides().isEmpty()) {
            return originalDataSet;
        }
        
        logger.debug("Applying environment overrides for: {}", currentEnvironment);
        
        Map<String, Object> mergedData = new HashMap<>(originalDataSet.getData());
        
        // Apply overrides from parent environments first (inheritance)
        applyParentOverrides(envConfig, mergedData);
        
        // Apply current environment overrides
        Map<String, Object> overrides = envConfig.getDataOverrides();
        for (Map.Entry<String, Object> override : overrides.entrySet()) {
            String key = override.getKey();
            Object value = processEnvironmentVariable(override.getValue());
            mergedData.put(key, value);
            logger.debug("Applied override: {} = {}", key, value);
        }
        
        return new TestDataManager.DataSet(
            originalDataSet.getName(),
            currentEnvironment,
            originalDataSet.getVersion(),
            mergedData,
            originalDataSet.getSource() + " + EnvironmentOverrides"
        );
    }
    
    /**
     * Get environment property
     */
    public Object getEnvironmentProperty(String propertyName) {
        EnvironmentConfig envConfig = environmentConfigs.get(currentEnvironment);
        
        if (envConfig != null && envConfig.getProperties().containsKey(propertyName)) {
            return processEnvironmentVariable(envConfig.getProperties().get(propertyName));
        }
        
        // Check parent environments
        return getPropertyFromParent(envConfig, propertyName);
    }
    
    /**
     * Get connection string for environment
     */
    public String getConnectionString(String connectionName) {
        EnvironmentConfig envConfig = environmentConfigs.get(currentEnvironment);
        
        if (envConfig != null && envConfig.getConnectionStrings().containsKey(connectionName)) {
            return processEnvironmentVariable(envConfig.getConnectionStrings().get(connectionName)).toString();
        }
        
        // Check parent environments
        String parentConnection = getConnectionFromParent(envConfig, connectionName);
        if (parentConnection != null) {
            return parentConnection;
        }
        
        // Return default connection string
        return getDefaultConnectionString(connectionName);
    }
    
    /**
     * Check if operation is allowed in current environment
     */
    public boolean isOperationAllowed(String operation) {
        EnvironmentConfig envConfig = environmentConfigs.get(currentEnvironment);
        
        if (envConfig != null) {
            // If production environment, be more restrictive
            if (envConfig.isProduction()) {
                return envConfig.getAllowedOperations().contains(operation) && 
                       !isDestructiveOperation(operation);
            }
            
            // For non-production, check allowed operations list
            return envConfig.getAllowedOperations().isEmpty() || 
                   envConfig.getAllowedOperations().contains(operation);
        }
        
        return true; // Default allow for unknown environments
    }
    
    /**
     * Get all available environments
     */
    public Set<String> getAvailableEnvironments() {
        Set<String> environments = new HashSet<>();
        
        try {
            Path environmentsPath = Paths.get(config.getDataRootPath(), config.getEnvironmentsPath());
            
            if (Files.exists(environmentsPath)) {
                Files.list(environmentsPath)
                    .filter(Files::isDirectory)
                    .forEach(path -> environments.add(path.getFileName().toString()));
            }
            
        } catch (IOException e) {
            logger.warn("Error listing available environments", e);
        }
        
        return environments;
    }
    
    /**
     * Validate environment configuration
     */
    public List<String> validateEnvironmentConfig(String environmentName) {
        List<String> validationErrors = new ArrayList<>();
        
        EnvironmentConfig envConfig = environmentConfigs.get(environmentName);
        if (envConfig == null) {
            validationErrors.add("Environment configuration not found: " + environmentName);
            return validationErrors;
        }
        
        // Validate parent environment exists
        if (envConfig.getParentEnvironment() != null && 
            !environmentConfigs.containsKey(envConfig.getParentEnvironment())) {
            validationErrors.add("Parent environment not found: " + envConfig.getParentEnvironment());
        }
        
        // Validate required connection strings
        List<String> requiredConnections = Arrays.asList("database", "api", "web");
        for (String connection : requiredConnections) {
            if (getConnectionString(connection) == null) {
                validationErrors.add("Missing required connection string: " + connection);
            }
        }
        
        // Validate environment-specific rules
        if (envConfig.isProduction()) {
            if (envConfig.getAllowedOperations().isEmpty()) {
                validationErrors.add("Production environment must have explicitly defined allowed operations");
            }
        }
        
        return validationErrors;
    }
    
    /**
     * Get environment statistics
     */
    public Map<String, Object> getEnvironmentStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("currentEnvironment", currentEnvironment);
        stats.put("loadedConfigurations", environmentConfigs.size());
        stats.put("availableEnvironments", getAvailableEnvironments().size());
        
        EnvironmentConfig currentConfig = environmentConfigs.get(currentEnvironment);
        if (currentConfig != null) {
            stats.put("isProduction", currentConfig.isProduction());
            stats.put("hasParent", currentConfig.getParentEnvironment() != null);
            stats.put("propertiesCount", currentConfig.getProperties().size());
            stats.put("overridesCount", currentConfig.getDataOverrides().size());
            stats.put("allowedOperationsCount", currentConfig.getAllowedOperations().size());
        }
        
        return stats;
    }
    
    // Private helper methods
    
    private Path getEnvironmentConfigPath(String environmentName) {
        return Paths.get(config.getDataRootPath(), config.getEnvironmentsPath(), 
                        environmentName, "environment.json");
    }
    
    private void loadParentEnvironments(EnvironmentConfig envConfig) {
        String parentEnv = envConfig.getParentEnvironment();
        
        while (parentEnv != null && !environmentConfigs.containsKey(parentEnv)) {
            try {
                Path parentConfigPath = getEnvironmentConfigPath(parentEnv);
                
                if (Files.exists(parentConfigPath)) {
                    EnvironmentConfig parentConfig = objectMapper.readValue(parentConfigPath.toFile(), EnvironmentConfig.class);
                    environmentConfigs.put(parentEnv, parentConfig);
                    parentEnv = parentConfig.getParentEnvironment();
                    
                    logger.debug("Loaded parent environment configuration: {}", parentEnv);
                } else {
                    logger.warn("Parent environment configuration not found: {}", parentEnv);
                    break;
                }
                
            } catch (IOException e) {
                logger.error("Error loading parent environment configuration: {}", parentEnv, e);
                break;
            }
        }
    }
    
    private void applyParentOverrides(EnvironmentConfig envConfig, Map<String, Object> data) {
        if (envConfig.getParentEnvironment() != null) {
            EnvironmentConfig parentConfig = environmentConfigs.get(envConfig.getParentEnvironment());
            
            if (parentConfig != null) {
                // Recursively apply parent overrides first
                applyParentOverrides(parentConfig, data);
                
                // Apply parent's overrides
                Map<String, Object> parentOverrides = parentConfig.getDataOverrides();
                for (Map.Entry<String, Object> override : parentOverrides.entrySet()) {
                    String key = override.getKey();
                    if (!data.containsKey(key)) { // Don't override child values
                        Object value = processEnvironmentVariable(override.getValue());
                        data.put(key, value);
                        logger.debug("Applied parent override from {}: {} = {}", 
                            parentConfig.getName(), key, value);
                    }
                }
            }
        }
    }
    
    private Object getPropertyFromParent(EnvironmentConfig envConfig, String propertyName) {
        if (envConfig != null && envConfig.getParentEnvironment() != null) {
            EnvironmentConfig parentConfig = environmentConfigs.get(envConfig.getParentEnvironment());
            
            if (parentConfig != null && parentConfig.getProperties().containsKey(propertyName)) {
                return processEnvironmentVariable(parentConfig.getProperties().get(propertyName));
            }
            
            // Recursively check parent's parents
            return getPropertyFromParent(parentConfig, propertyName);
        }
        
        return null;
    }
    
    private String getConnectionFromParent(EnvironmentConfig envConfig, String connectionName) {
        if (envConfig != null && envConfig.getParentEnvironment() != null) {
            EnvironmentConfig parentConfig = environmentConfigs.get(envConfig.getParentEnvironment());
            
            if (parentConfig != null && parentConfig.getConnectionStrings().containsKey(connectionName)) {
                return processEnvironmentVariable(parentConfig.getConnectionStrings().get(connectionName)).toString();
            }
            
            // Recursively check parent's parents
            return getConnectionFromParent(parentConfig, connectionName);
        }
        
        return null;
    }
    
    private Object processEnvironmentVariable(Object value) {
        if (value instanceof String) {
            String stringValue = (String) value;
            
            // Replace environment variables like ${ENV_VAR} or %ENV_VAR%
            if (stringValue.contains("${") || stringValue.contains("%")) {
                return replaceEnvironmentVariables(stringValue);
            }
        }
        
        return value;
    }
    
    private String replaceEnvironmentVariables(String value) {
        String result = value;
        
        // Replace ${VAR} format
        while (result.contains("${")) {
            int start = result.indexOf("${");
            int end = result.indexOf("}", start);
            
            if (end > start) {
                String varName = result.substring(start + 2, end);
                String envValue = System.getenv(varName);
                
                if (envValue != null) {
                    result = result.substring(0, start) + envValue + result.substring(end + 1);
                } else {
                    // Keep original if environment variable not found
                    break;
                }
            } else {
                break;
            }
        }
        
        // Replace %VAR% format (Windows style)
        while (result.contains("%") && result.lastIndexOf("%") > result.indexOf("%")) {
            int start = result.indexOf("%");
            int end = result.indexOf("%", start + 1);
            
            if (end > start) {
                String varName = result.substring(start + 1, end);
                String envValue = System.getenv(varName);
                
                if (envValue != null) {
                    result = result.substring(0, start) + envValue + result.substring(end + 1);
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        
        return result;
    }
    
    private void createDefaultEnvironmentConfig() {
        EnvironmentConfig defaultConfig = new EnvironmentConfig();
        defaultConfig.setName(currentEnvironment);
        defaultConfig.setDescription("Default configuration for " + currentEnvironment);
        
        // Set default properties
        defaultConfig.getProperties().put("baseUrl", "http://localhost:8080");
        defaultConfig.getProperties().put("timeout", 30);
        defaultConfig.getProperties().put("retryAttempts", 3);
        
        // Set default connection strings
        defaultConfig.getConnectionStrings().put("database", "jdbc:h2:mem:testdb");
        defaultConfig.getConnectionStrings().put("api", "http://localhost:8080/api");
        defaultConfig.getConnectionStrings().put("web", "http://localhost:8080");
        
        // Set default allowed operations based on environment name
        if (currentEnvironment.toLowerCase().contains("prod")) {
            defaultConfig.setProduction(true);
            defaultConfig.getAllowedOperations().addAll(Arrays.asList("read", "validate"));
        } else {
            defaultConfig.getAllowedOperations().addAll(Arrays.asList(
                "create", "read", "update", "delete", "validate", "generate"
            ));
        }
        
        environmentConfigs.put(currentEnvironment, defaultConfig);
        
        // Save default configuration to file
        saveEnvironmentConfig(defaultConfig);
    }
    
    private void saveEnvironmentConfig(EnvironmentConfig config) {
        try {
            Path configPath = getEnvironmentConfigPath(config.getName());
            Files.createDirectories(configPath.getParent());
            
            objectMapper.writerWithDefaultPrettyPrinter()
                      .writeValue(configPath.toFile(), config);
            
            logger.info("Saved environment configuration: {}", configPath);
            
        } catch (IOException e) {
            logger.error("Error saving environment configuration for: {}", config.getName(), e);
        }
    }
    
    private String getDefaultConnectionString(String connectionName) {
        switch (connectionName.toLowerCase()) {
            case "database":
                return "jdbc:h2:mem:testdb";
            case "api":
                return "http://localhost:8080/api";
            case "web":
                return "http://localhost:8080";
            default:
                return null;
        }
    }
    
    private boolean isDestructiveOperation(String operation) {
        List<String> destructiveOps = Arrays.asList("delete", "truncate", "drop", "clear", "reset");
        return destructiveOps.contains(operation.toLowerCase());
    }
}