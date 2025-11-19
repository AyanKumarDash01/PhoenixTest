package com.phoenix.hrm.core.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced Test Data Manager for Phoenix HRM Test Automation Framework
 * Supports JSON, CSV, Properties, and Excel file formats for test data
 */
public class TestDataManager {
    
    private static final Logger logger = LoggerFactory.getLogger(TestDataManager.class);
    private static TestDataManager instance;
    private static final String TEST_DATA_DIR = "src/test/resources/testdata";
    private final ObjectMapper objectMapper;
    private final Map<String, Object> dataCache;
    
    // Singleton constructor
    private TestDataManager() {
        this.objectMapper = new ObjectMapper();
        this.dataCache = new HashMap<>();
        ensureDataDirectoryExists();
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized TestDataManager getInstance() {
        if (instance == null) {
            instance = new TestDataManager();
        }
        return instance;
    }
    
    /**
     * Ensure test data directory exists
     */
    private void ensureDataDirectoryExists() {
        try {
            Path dataPath = Paths.get(TEST_DATA_DIR);
            if (!Files.exists(dataPath)) {
                Files.createDirectories(dataPath);
                logger.info("Created test data directory: {}", TEST_DATA_DIR);
            }
        } catch (IOException e) {
            logger.error("Failed to create test data directory", e);
        }
    }
    
    // ==================== JSON DATA METHODS ====================
    
    /**
     * Load test data from JSON file
     */
    public JsonNode loadJsonData(String fileName) {
        String cacheKey = "json_" + fileName;
        
        if (dataCache.containsKey(cacheKey)) {
            logger.debug("Returning cached JSON data for: {}", fileName);
            return (JsonNode) dataCache.get(cacheKey);
        }
        
        try {
            String filePath = TEST_DATA_DIR + "/" + fileName;
            if (!fileName.endsWith(".json")) {
                filePath += ".json";
            }
            
            File file = new File(filePath);
            if (!file.exists()) {
                logger.warn("JSON file not found: {}. Creating empty data structure.", filePath);
                return createEmptyJsonFile(filePath);
            }
            
            JsonNode jsonData = objectMapper.readTree(file);
            dataCache.put(cacheKey, jsonData);
            logger.debug("Loaded JSON data from: {}", filePath);
            return jsonData;
            
        } catch (IOException e) {
            logger.error("Failed to load JSON data from: {}", fileName, e);
            return objectMapper.createObjectNode();
        }
    }
    
    /**
     * Create empty JSON file with basic structure
     */
    private JsonNode createEmptyJsonFile(String filePath) {
        try {
            String emptyJson = "{\n  \"users\": [],\n  \"credentials\": {},\n  \"testData\": {}\n}";
            Files.write(Paths.get(filePath), emptyJson.getBytes());
            return objectMapper.readTree(emptyJson);
        } catch (IOException e) {
            logger.error("Failed to create empty JSON file: {}", filePath, e);
            return objectMapper.createObjectNode();
        }
    }
    
    /**
     * Get specific data from JSON by path (e.g., "users.admin.username")
     */
    public String getJsonValue(String fileName, String jsonPath) {
        JsonNode rootNode = loadJsonData(fileName);
        JsonNode currentNode = rootNode;
        
        String[] pathParts = jsonPath.split("\\.");
        for (String part : pathParts) {
            if (currentNode.has(part)) {
                currentNode = currentNode.get(part);
            } else {
                logger.warn("JSON path '{}' not found in file: {}", jsonPath, fileName);
                return null;
            }
        }
        
        return currentNode.isTextual() ? currentNode.asText() : currentNode.toString();
    }
    
    /**
     * Get JSON object as Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getJsonAsMap(String fileName, String jsonPath) {
        try {
            JsonNode node = loadJsonData(fileName);
            if (jsonPath != null && !jsonPath.isEmpty()) {
                String[] pathParts = jsonPath.split("\\.");
                for (String part : pathParts) {
                    if (node.has(part)) {
                        node = node.get(part);
                    } else {
                        return Collections.emptyMap();
                    }
                }
            }
            return objectMapper.convertValue(node, Map.class);
        } catch (Exception e) {
            logger.error("Failed to convert JSON to Map: {} -> {}", fileName, jsonPath, e);
            return Collections.emptyMap();
        }
    }
    
    // ==================== CSV DATA METHODS ====================
    
    /**
     * Load test data from CSV file
     */
    public List<Map<String, String>> loadCsvData(String fileName) {
        String cacheKey = "csv_" + fileName;
        
        if (dataCache.containsKey(cacheKey)) {
            logger.debug("Returning cached CSV data for: {}", fileName);
            return (List<Map<String, String>>) dataCache.get(cacheKey);
        }
        
        List<Map<String, String>> csvData = new ArrayList<>();
        String filePath = TEST_DATA_DIR + "/" + fileName;
        if (!fileName.endsWith(".csv")) {
            filePath += ".csv";
        }
        
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headers = reader.readNext();
            if (headers == null) {
                logger.warn("CSV file is empty: {}", filePath);
                return csvData;
            }
            
            String[] row;
            while ((row = reader.readNext()) != null) {
                Map<String, String> rowData = new HashMap<>();
                for (int i = 0; i < headers.length && i < row.length; i++) {
                    rowData.put(headers[i].trim(), row[i].trim());
                }
                csvData.add(rowData);
            }
            
            dataCache.put(cacheKey, csvData);
            logger.debug("Loaded {} rows from CSV: {}", csvData.size(), filePath);
            
        } catch (IOException | com.opencsv.exceptions.CsvValidationException e) {
            logger.error("Failed to load CSV data from: {}", fileName, e);
            // Create sample CSV file
            createSampleCsvFile(filePath);
        }
        
        return csvData;
    }
    
    /**
     * Create sample CSV file
     */
    private void createSampleCsvFile(String filePath) {
        try {
            String sampleCsv = "username,password,role,email\n" +
                              "admin,admin123,Administrator,admin@phoenix.com\n" +
                              "user1,user123,Employee,user1@phoenix.com\n" +
                              "manager,mgr123,Manager,manager@phoenix.com";
            Files.write(Paths.get(filePath), sampleCsv.getBytes());
            logger.info("Created sample CSV file: {}", filePath);
        } catch (IOException e) {
            logger.error("Failed to create sample CSV file: {}", filePath, e);
        }
    }
    
    /**
     * Get specific row from CSV by column value
     */
    public Map<String, String> getCsvRowByValue(String fileName, String columnName, String value) {
        List<Map<String, String>> data = loadCsvData(fileName);
        return data.stream()
                .filter(row -> value.equals(row.get(columnName)))
                .findFirst()
                .orElse(Collections.emptyMap());
    }
    
    /**
     * Get all rows matching criteria
     */
    public List<Map<String, String>> getCsvRowsByCriteria(String fileName, Map<String, String> criteria) {
        List<Map<String, String>> data = loadCsvData(fileName);
        return data.stream()
                .filter(row -> criteria.entrySet().stream()
                        .allMatch(entry -> entry.getValue().equals(row.get(entry.getKey()))))
                .collect(Collectors.toList());
    }
    
    // ==================== PROPERTIES DATA METHODS ====================
    
    /**
     * Load properties file
     */
    public Properties loadProperties(String fileName) {
        String cacheKey = "props_" + fileName;
        
        if (dataCache.containsKey(cacheKey)) {
            logger.debug("Returning cached properties for: {}", fileName);
            return (Properties) dataCache.get(cacheKey);
        }
        
        Properties properties = new Properties();
        String filePath = TEST_DATA_DIR + "/" + fileName;
        if (!fileName.endsWith(".properties")) {
            filePath += ".properties";
        }
        
        try (InputStream input = new FileInputStream(filePath)) {
            properties.load(input);
            dataCache.put(cacheKey, properties);
            logger.debug("Loaded properties from: {}", filePath);
        } catch (IOException e) {
            logger.error("Failed to load properties from: {}", fileName, e);
            // Create sample properties file
            createSamplePropertiesFile(filePath);
        }
        
        return properties;
    }
    
    /**
     * Create sample properties file
     */
    private void createSamplePropertiesFile(String filePath) {
        try {
            String sampleProps = "# Phoenix HRM Test Data\n" +
                               "admin.username=admin\n" +
                               "admin.password=admin123\n" +
                               "user.username=user1\n" +
                               "user.password=user123\n" +
                               "test.timeout=30\n" +
                               "api.base.url=http://localhost:8080/api";
            Files.write(Paths.get(filePath), sampleProps.getBytes());
            logger.info("Created sample properties file: {}", filePath);
        } catch (IOException e) {
            logger.error("Failed to create sample properties file: {}", filePath, e);
        }
    }
    
    /**
     * Get property value with default
     */
    public String getProperty(String fileName, String key, String defaultValue) {
        Properties props = loadProperties(fileName);
        return props.getProperty(key, defaultValue);
    }
    
    // ==================== DYNAMIC DATA GENERATION ====================
    
    /**
     * Generate random string
     */
    public String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return sb.toString();
    }
    
    /**
     * Generate random email
     */
    public String generateRandomEmail() {
        String username = generateRandomString(8).toLowerCase();
        String[] domains = {"gmail.com", "yahoo.com", "outlook.com", "phoenix.com"};
        String domain = domains[new Random().nextInt(domains.length)];
        return username + "@" + domain;
    }
    
    /**
     * Generate random phone number
     */
    public String generateRandomPhoneNumber() {
        Random random = new Random();
        return String.format("(%03d) %03d-%04d", 
                random.nextInt(900) + 100,
                random.nextInt(900) + 100,
                random.nextInt(9000) + 1000);
    }
    
    /**
     * Generate random employee ID
     */
    public String generateEmployeeId() {
        return "EMP" + String.format("%06d", new Random().nextInt(999999) + 1);
    }
    
    /**
     * Generate current timestamp
     */
    public String getCurrentTimestamp() {
        return String.valueOf(System.currentTimeMillis());
    }
    
    /**
     * Generate date string in format
     */
    public String generateDateString(String format) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(format);
        return sdf.format(new Date());
    }
    
    // ==================== USER CREDENTIALS ====================
    
    /**
     * Get user credentials for role
     */
    public Map<String, String> getUserCredentials(String role) {
        // Try JSON first
        JsonNode users = loadJsonData("users").get("credentials");
        if (users != null && users.has(role)) {
            Map<String, Object> jsonMap = getJsonAsMap("users", "credentials." + role);
            Map<String, String> stringMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
                stringMap.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
            return stringMap;
        }
        
        // Fallback to CSV
        return getCsvRowByValue("users", "role", role);
    }
    
    /**
     * Get admin credentials
     */
    public Map<String, String> getAdminCredentials() {
        return getUserCredentials("admin");
    }
    
    /**
     * Get manager credentials
     */
    public Map<String, String> getManagerCredentials() {
        return getUserCredentials("manager");
    }
    
    /**
     * Get employee credentials
     */
    public Map<String, String> getEmployeeCredentials() {
        return getUserCredentials("employee");
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Clear data cache
     */
    public void clearCache() {
        dataCache.clear();
        logger.info("Test data cache cleared");
    }
    
    /**
     * Get cache size
     */
    public int getCacheSize() {
        return dataCache.size();
    }
    
    /**
     * Check if file exists
     */
    public boolean fileExists(String fileName) {
        String filePath = TEST_DATA_DIR + "/" + fileName;
        return Files.exists(Paths.get(filePath));
    }
    
    /**
     * List all test data files
     */
    public List<String> listTestDataFiles() {
        try {
            return Files.list(Paths.get(TEST_DATA_DIR))
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Failed to list test data files", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Replace placeholders in text with dynamic values
     */
    public String replacePlaceholders(String text) {
        if (text == null) return null;
        
        String result = text;
        
        // Replace common placeholders
        result = result.replace("${timestamp}", getCurrentTimestamp());
        result = result.replace("${random.string.5}", generateRandomString(5));
        result = result.replace("${random.string.10}", generateRandomString(10));
        result = result.replace("${random.email}", generateRandomEmail());
        result = result.replace("${random.phone}", generateRandomPhoneNumber());
        result = result.replace("${employee.id}", generateEmployeeId());
        result = result.replace("${date.today}", generateDateString("yyyy-MM-dd"));
        result = result.replace("${datetime.now}", generateDateString("yyyy-MM-dd HH:mm:ss"));
        
        return result;
    }
    
    /**
     * Validate test data structure
     */
    public boolean validateTestDataStructure() {
        boolean isValid = true;
        
        // Check if test data directory exists
        if (!Files.exists(Paths.get(TEST_DATA_DIR))) {
            logger.error("Test data directory does not exist: {}", TEST_DATA_DIR);
            isValid = false;
        }
        
        // Check for essential files
        String[] essentialFiles = {"users.json", "credentials.csv", "config.properties"};
        for (String file : essentialFiles) {
            if (!fileExists(file)) {
                logger.warn("Essential test data file missing: {}", file);
                // Create the file with sample data
                createSampleDataFile(file);
            }
        }
        
        logger.info("Test data structure validation complete. Valid: {}", isValid);
        return isValid;
    }
    
    /**
     * Create sample data file
     */
    private void createSampleDataFile(String fileName) {
        try {
            if (fileName.endsWith(".json")) {
                createEmptyJsonFile(TEST_DATA_DIR + "/" + fileName);
            } else if (fileName.endsWith(".csv")) {
                createSampleCsvFile(TEST_DATA_DIR + "/" + fileName);
            } else if (fileName.endsWith(".properties")) {
                createSamplePropertiesFile(TEST_DATA_DIR + "/" + fileName);
            }
        } catch (Exception e) {
            logger.error("Failed to create sample data file: {}", fileName, e);
        }
    }
}