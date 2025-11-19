package com.phoenix.hrm.testdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * Advanced Test Data Management Framework for Phoenix HRM Test Automation
 * 
 * Provides comprehensive test data management capabilities including:
 * - Multi-format data loading (JSON, YAML, XML, CSV, Properties)
 * - Environment-specific data handling with inheritance
 * - Data versioning and snapshot management
 * - Dynamic test data generation with patterns and rules
 * - Data masking and security for sensitive information
 * - Template-based data creation with placeholders
 * - Cached data management for performance optimization
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 4.0
 * @since Phase 4
 */
public class TestDataManager {
    
    private static final Logger logger = LoggerFactory.getLogger(TestDataManager.class);
    
    // Singleton instance
    private static volatile TestDataManager instance;
    private static final Object instanceLock = new Object();
    
    // Core components
    private final ObjectMapper jsonMapper;
    private final ObjectMapper yamlMapper;
    private final Map<String, Object> dataCache;
    private final Map<String, DataSet> dataSets;
    private final DataGeneratorEngine generatorEngine;
    private final DataMaskingEngine maskingEngine;
    private final EnvironmentDataManager environmentManager;
    private final DataVersionManager versionManager;
    
    // Configuration
    private final TestDataConfig config;
    private final String currentEnvironment;
    
    /**
     * Test data configuration
     */
    public static class TestDataConfig {
        private String dataRootPath = "src/test/resources/testdata";
        private String environmentsPath = "environments";
        private String generatedDataPath = "generated";
        private String templatesPath = "templates";
        private boolean enableCaching = true;
        private boolean enableVersioning = true;
        private boolean enableDataMasking = true;
        private long cacheExpiryMinutes = 30;
        private String defaultEnvironment = "dev";
        private Set<String> sensitiveFields = new HashSet<>();
        
        // Builder pattern
        public static class Builder {
            private final TestDataConfig config = new TestDataConfig();
            
            public Builder dataRootPath(String dataRootPath) {
                config.dataRootPath = dataRootPath;
                return this;
            }
            
            public Builder environmentsPath(String environmentsPath) {
                config.environmentsPath = environmentsPath;
                return this;
            }
            
            public Builder generatedDataPath(String generatedDataPath) {
                config.generatedDataPath = generatedDataPath;
                return this;
            }
            
            public Builder templatesPath(String templatesPath) {
                config.templatesPath = templatesPath;
                return this;
            }
            
            public Builder enableCaching(boolean enableCaching) {
                config.enableCaching = enableCaching;
                return this;
            }
            
            public Builder enableVersioning(boolean enableVersioning) {
                config.enableVersioning = enableVersioning;
                return this;
            }
            
            public Builder enableDataMasking(boolean enableDataMasking) {
                config.enableDataMasking = enableDataMasking;
                return this;
            }
            
            public Builder cacheExpiryMinutes(long cacheExpiryMinutes) {
                config.cacheExpiryMinutes = cacheExpiryMinutes;
                return this;
            }
            
            public Builder defaultEnvironment(String defaultEnvironment) {
                config.defaultEnvironment = defaultEnvironment;
                return this;
            }
            
            public Builder addSensitiveField(String fieldName) {
                config.sensitiveFields.add(fieldName);
                return this;
            }
            
            public Builder addSensitiveFields(Set<String> fieldNames) {
                config.sensitiveFields.addAll(fieldNames);
                return this;
            }
            
            public TestDataConfig build() {
                // Add default sensitive fields
                config.sensitiveFields.addAll(Arrays.asList(
                    "password", "ssn", "socialSecurityNumber", "creditCard", 
                    "bankAccount", "salary", "email", "phone", "address"
                ));
                return config;
            }
        }
        
        // Getters
        public String getDataRootPath() { return dataRootPath; }
        public String getEnvironmentsPath() { return environmentsPath; }
        public String getGeneratedDataPath() { return generatedDataPath; }
        public String getTemplatesPath() { return templatesPath; }
        public boolean isEnableCaching() { return enableCaching; }
        public boolean isEnableVersioning() { return enableVersioning; }
        public boolean isEnableDataMasking() { return enableDataMasking; }
        public long getCacheExpiryMinutes() { return cacheExpiryMinutes; }
        public String getDefaultEnvironment() { return defaultEnvironment; }
        public Set<String> getSensitiveFields() { return sensitiveFields; }
    }
    
    /**
     * Data set representation
     */
    public static class DataSet {
        private final String name;
        private final String environment;
        private final String version;
        private final Map<String, Object> data;
        private final long timestamp;
        private final String source;
        
        public DataSet(String name, String environment, String version, Map<String, Object> data, String source) {
            this.name = name;
            this.environment = environment;
            this.version = version;
            this.data = new HashMap<>(data);
            this.timestamp = System.currentTimeMillis();
            this.source = source;
        }
        
        // Getters
        public String getName() { return name; }
        public String getEnvironment() { return environment; }
        public String getVersion() { return version; }
        public Map<String, Object> getData() { return new HashMap<>(data); }
        public long getTimestamp() { return timestamp; }
        public String getSource() { return source; }
        
        public <T> T getValue(String key, Class<T> type) {
            Object value = data.get(key);
            if (value != null && type.isAssignableFrom(value.getClass())) {
                return type.cast(value);
            }
            return null;
        }
        
        public Object getValue(String key) {
            return data.get(key);
        }
        
        public boolean containsKey(String key) {
            return data.containsKey(key);
        }
        
        public Set<String> getKeys() {
            return data.keySet();
        }
    }
    
    /**
     * Private constructor for singleton
     */
    private TestDataManager(TestDataConfig config, String environment) {
        this.config = config != null ? config : new TestDataConfig.Builder().build();
        this.currentEnvironment = environment != null ? environment : this.config.getDefaultEnvironment();
        
        this.jsonMapper = new ObjectMapper();
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.dataCache = this.config.isEnableCaching() ? new ConcurrentHashMap<>() : new HashMap<>();
        this.dataSets = new ConcurrentHashMap<>();
        this.generatorEngine = new DataGeneratorEngine(this.config);
        this.maskingEngine = new DataMaskingEngine(this.config);
        this.environmentManager = new EnvironmentDataManager(this.config, this.currentEnvironment);
        this.versionManager = new DataVersionManager(this.config);
        
        initializeDataManager();
    }
    
    /**
     * Get singleton instance
     */
    public static TestDataManager getInstance() {
        return getInstance(null, null);
    }
    
    /**
     * Get singleton instance with configuration and environment
     */
    public static TestDataManager getInstance(TestDataConfig config, String environment) {
        if (instance == null) {
            synchronized (instanceLock) {
                if (instance == null) {
                    instance = new TestDataManager(config, environment);
                }
            }
        }
        return instance;
    }
    
    /**
     * Load test data by name
     */
    public DataSet loadData(String dataName) {
        return loadData(dataName, currentEnvironment, null);
    }
    
    /**
     * Load test data by name and environment
     */
    public DataSet loadData(String dataName, String environment) {
        return loadData(dataName, environment, null);
    }
    
    /**
     * Load test data by name, environment, and version
     */
    public DataSet loadData(String dataName, String environment, String version) {
        String cacheKey = buildCacheKey(dataName, environment, version);
        
        // Check cache first
        if (config.isEnableCaching() && dataCache.containsKey(cacheKey)) {
            DataSet cachedData = (DataSet) dataCache.get(cacheKey);
            if (isCacheValid(cachedData)) {
                logger.debug("Returning cached data: {}", cacheKey);
                return cachedData;
            } else {
                dataCache.remove(cacheKey);
            }
        }
        
        try {
            // Load data from file system
            DataSet dataSet = loadDataFromSource(dataName, environment, version);
            
            // Apply environment-specific overrides
            dataSet = environmentManager.applyEnvironmentOverrides(dataSet);
            
            // Apply data masking if enabled
            if (config.isEnableDataMasking()) {
                dataSet = maskingEngine.maskSensitiveData(dataSet);
            }
            
            // Cache the loaded data
            if (config.isEnableCaching()) {
                dataCache.put(cacheKey, dataSet);
            }
            
            // Store in data sets registry
            dataSets.put(cacheKey, dataSet);
            
            logger.info("Loaded test data: {} for environment: {}", dataName, environment);
            return dataSet;
            
        } catch (Exception e) {
            logger.error("Error loading test data: {} for environment: {}", dataName, environment, e);
            throw new TestDataException("Failed to load test data: " + dataName, e);
        }
    }
    
    /**
     * Generate test data using patterns and rules
     */
    public DataSet generateData(String dataName, Map<String, Object> generationRules) {
        return generatorEngine.generateData(dataName, currentEnvironment, generationRules);
    }
    
    /**
     * Create data from template with variable substitution
     */
    public DataSet createFromTemplate(String templateName, Map<String, Object> variables) {
        try {
            String templatePath = Paths.get(config.getDataRootPath(), config.getTemplatesPath(), templateName + ".json").toString();
            String templateContent = Files.readString(Paths.get(templatePath));
            
            // Replace template variables
            String processedContent = replaceTemplateVariables(templateContent, variables);
            
            // Parse as JSON
            Map<String, Object> data = jsonMapper.readValue(processedContent, Map.class);
            
            return new DataSet(templateName, currentEnvironment, "template", data, templatePath);
            
        } catch (Exception e) {
            logger.error("Error creating data from template: {}", templateName, e);
            throw new TestDataException("Failed to create data from template: " + templateName, e);
        }
    }
    
    /**
     * Save generated data to file
     */
    public void saveGeneratedData(DataSet dataSet, String fileName) {
        try {
            Path outputPath = Paths.get(config.getDataRootPath(), config.getGeneratedDataPath(), 
                currentEnvironment, fileName + ".json");
            
            // Create directories if they don't exist
            Files.createDirectories(outputPath.getParent());
            
            // Save data as JSON
            jsonMapper.writerWithDefaultPrettyPrinter().writeValue(outputPath.toFile(), dataSet.getData());
            
            logger.info("Saved generated data to: {}", outputPath);
            
        } catch (Exception e) {
            logger.error("Error saving generated data: {}", fileName, e);
            throw new TestDataException("Failed to save generated data: " + fileName, e);
        }
    }
    
    /**
     * Get all loaded data sets
     */
    public Map<String, DataSet> getAllDataSets() {
        return new HashMap<>(dataSets);
    }
    
    /**
     * Get data sets by environment
     */
    public Map<String, DataSet> getDataSetsByEnvironment(String environment) {
        Map<String, DataSet> environmentData = new HashMap<>();
        for (Map.Entry<String, DataSet> entry : dataSets.entrySet()) {
            if (entry.getValue().getEnvironment().equals(environment)) {
                environmentData.put(entry.getKey(), entry.getValue());
            }
        }
        return environmentData;
    }
    
    /**
     * Clear all cached data
     */
    public void clearCache() {
        dataCache.clear();
        dataSets.clear();
        logger.info("Cleared all cached test data");
    }
    
    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", dataCache.size());
        stats.put("dataSetsCount", dataSets.size());
        stats.put("currentEnvironment", currentEnvironment);
        
        // Environment distribution
        Map<String, Integer> envDistribution = new HashMap<>();
        for (DataSet dataSet : dataSets.values()) {
            envDistribution.merge(dataSet.getEnvironment(), 1, Integer::sum);
        }
        stats.put("environmentDistribution", envDistribution);
        
        return stats;
    }
    
    /**
     * Create version snapshot
     */
    public String createVersionSnapshot(String dataName) {
        if (!config.isEnableVersioning()) {
            throw new UnsupportedOperationException("Versioning is not enabled");
        }
        
        return versionManager.createSnapshot(dataName, currentEnvironment);
    }
    
    /**
     * List available versions for data
     */
    public List<String> getAvailableVersions(String dataName) {
        if (!config.isEnableVersioning()) {
            return Collections.emptyList();
        }
        
        return versionManager.getAvailableVersions(dataName, currentEnvironment);
    }
    
    // Private helper methods
    
    private void initializeDataManager() {
        logger.info("Initializing TestDataManager for environment: {}", currentEnvironment);
        
        // Create necessary directories
        createDirectoryStructure();
        
        // Load environment configuration
        environmentManager.loadEnvironmentConfig();
        
        logger.info("TestDataManager initialized successfully");
    }
    
    private void createDirectoryStructure() {
        try {
            Path rootPath = Paths.get(config.getDataRootPath());
            Files.createDirectories(rootPath);
            Files.createDirectories(rootPath.resolve(config.getEnvironmentsPath()));
            Files.createDirectories(rootPath.resolve(config.getGeneratedDataPath()).resolve(currentEnvironment));
            Files.createDirectories(rootPath.resolve(config.getTemplatesPath()));
            
        } catch (IOException e) {
            logger.warn("Error creating directory structure: {}", e.getMessage());
        }
    }
    
    private DataSet loadDataFromSource(String dataName, String environment, String version) throws IOException {
        // Try different file formats
        String[] extensions = {".json", ".yaml", ".yml", ".properties"};
        
        for (String ext : extensions) {
            Path dataPath = buildDataPath(dataName, environment, version, ext);
            
            if (Files.exists(dataPath)) {
                Map<String, Object> data = loadDataFromFile(dataPath, ext);
                return new DataSet(dataName, environment, version, data, dataPath.toString());
            }
        }
        
        throw new FileNotFoundException("Data file not found: " + dataName + " for environment: " + environment);
    }
    
    private Path buildDataPath(String dataName, String environment, String version, String extension) {
        Path basePath = Paths.get(config.getDataRootPath(), config.getEnvironmentsPath(), environment);
        
        if (version != null && config.isEnableVersioning()) {
            return basePath.resolve("versions").resolve(version).resolve(dataName + extension);
        } else {
            return basePath.resolve(dataName + extension);
        }
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadDataFromFile(Path filePath, String extension) throws IOException {
        switch (extension.toLowerCase()) {
            case ".json":
                return jsonMapper.readValue(Files.newInputStream(filePath), Map.class);
            
            case ".yaml":
            case ".yml":
                return yamlMapper.readValue(Files.newInputStream(filePath), Map.class);
            
            case ".properties":
                Properties props = new Properties();
                props.load(Files.newInputStream(filePath));
                return new HashMap<>((Map) props);
            
            default:
                throw new UnsupportedOperationException("Unsupported file format: " + extension);
        }
    }
    
    private String buildCacheKey(String dataName, String environment, String version) {
        return String.format("%s_%s_%s", dataName, environment, version != null ? version : "latest");
    }
    
    private boolean isCacheValid(DataSet dataSet) {
        if (!config.isEnableCaching()) {
            return false;
        }
        
        long expiryTime = dataSet.getTimestamp() + (config.getCacheExpiryMinutes() * 60 * 1000);
        return System.currentTimeMillis() < expiryTime;
    }
    
    private String replaceTemplateVariables(String template, Map<String, Object> variables) {
        String result = template;
        
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = String.valueOf(entry.getValue());
            result = result.replace(placeholder, value);
        }
        
        return result;
    }
    
    /**
     * Custom exception for test data operations
     */
    public static class TestDataException extends RuntimeException {
        public TestDataException(String message) {
            super(message);
        }
        
        public TestDataException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}