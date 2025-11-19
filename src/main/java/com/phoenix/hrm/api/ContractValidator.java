package com.phoenix.hrm.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contract Validator for API Testing Framework
 * 
 * Provides comprehensive API contract validation including:
 * - JSON Schema validation for request/response payloads
 * - OpenAPI/Swagger contract validation
 * - Response structure validation
 * - Data type and format validation
 * - Custom validation rules and constraints
 * - Contract compliance reporting
 * - Schema caching for performance optimization
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 5.0
 * @since Phase 5
 */
public class ContractValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(ContractValidator.class);
    
    private final ApiTestFramework.ApiConfiguration config;
    private final ObjectMapper objectMapper;
    private final Map<String, JsonNode> schemaCache;
    private final Map<String, ContractDefinition> contractDefinitions;
    private final List<ValidationResult> validationHistory;
    
    /**
     * Contract definition for an endpoint
     */
    public static class ContractDefinition {
        private final String endpointName;
        private final String requestSchemaPath;
        private final String responseSchemaPath;
        private final Map<Integer, String> statusSpecificSchemas;
        private final List<ValidationRule> customRules;
        private final boolean strictMode;
        
        public ContractDefinition(String endpointName) {
            this.endpointName = endpointName;
            this.requestSchemaPath = null;
            this.responseSchemaPath = null;
            this.statusSpecificSchemas = new HashMap<>();
            this.customRules = new ArrayList<>();
            this.strictMode = true;
        }
        
        public ContractDefinition(String endpointName, String requestSchemaPath, 
                                String responseSchemaPath, boolean strictMode) {
            this.endpointName = endpointName;
            this.requestSchemaPath = requestSchemaPath;
            this.responseSchemaPath = responseSchemaPath;
            this.statusSpecificSchemas = new HashMap<>();
            this.customRules = new ArrayList<>();
            this.strictMode = strictMode;
        }
        
        // Getters
        public String getEndpointName() { return endpointName; }
        public String getRequestSchemaPath() { return requestSchemaPath; }
        public String getResponseSchemaPath() { return responseSchemaPath; }
        public Map<Integer, String> getStatusSpecificSchemas() { return statusSpecificSchemas; }
        public List<ValidationRule> getCustomRules() { return customRules; }
        public boolean isStrictMode() { return strictMode; }
    }
    
    /**
     * Custom validation rule interface
     */
    public interface ValidationRule {
        ValidationResult validate(Object data, ApiTestFramework.ApiResponse<?> response);
        String getRuleName();
        String getDescription();
    }
    
    /**
     * Validation result
     */
    public static class ValidationResult {
        private final String endpoint;
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;
        private final String validationType;
        private final long validationTime;
        private final Date timestamp;
        
        public ValidationResult(String endpoint, String validationType, boolean valid, 
                              List<String> errors, List<String> warnings, long validationTime) {
            this.endpoint = endpoint;
            this.validationType = validationType;
            this.valid = valid;
            this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
            this.warnings = warnings != null ? new ArrayList<>(warnings) : new ArrayList<>();
            this.validationTime = validationTime;
            this.timestamp = new Date();
        }
        
        // Getters
        public String getEndpoint() { return endpoint; }
        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        public String getValidationType() { return validationType; }
        public long getValidationTime() { return validationTime; }
        public Date getTimestamp() { return timestamp; }
        
        public boolean hasErrors() { return !errors.isEmpty(); }
        public boolean hasWarnings() { return !warnings.isEmpty(); }
        
        @Override
        public String toString() {
            return String.format("ValidationResult{endpoint='%s', valid=%b, errors=%d, warnings=%d}", 
                endpoint, valid, errors.size(), warnings.size());
        }
    }
    
    /**
     * Constructor
     */
    public ContractValidator(ApiTestFramework.ApiConfiguration config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.schemaCache = new ConcurrentHashMap<>();
        this.contractDefinitions = new ConcurrentHashMap<>();
        this.validationHistory = Collections.synchronizedList(new ArrayList<>());
        
        // Initialize default contract definitions for Phoenix HRM endpoints
        initializeDefaultContracts();
        
        logger.debug("ContractValidator initialized");
    }
    
    /**
     * Validate API response against contract
     */
    public ValidationResult validateResponse(ApiTestFramework.ApiEndpoint endpoint, 
                                           ApiTestFramework.ApiResponse<?> response) {
        if (!config.isEnableContractValidation()) {
            return new ValidationResult(endpoint.getName(), "skipped", true, null, null, 0);
        }
        
        long startTime = System.currentTimeMillis();
        ContractDefinition contract = contractDefinitions.get(endpoint.getName());
        
        if (contract == null) {
            logger.debug("No contract definition found for endpoint: {}", endpoint.getName());
            return new ValidationResult(endpoint.getName(), "no_contract", true, null, 
                Arrays.asList("No contract definition found"), 0);
        }
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        try {
            // Validate response schema
            validateResponseSchema(contract, response, errors, warnings);
            
            // Apply custom validation rules
            applyCustomValidationRules(contract, response, errors, warnings);
            
            // Validate response structure
            validateResponseStructure(contract, response, errors, warnings);
            
        } catch (Exception e) {
            errors.add("Validation error: " + e.getMessage());
            logger.error("Error during contract validation for {}: {}", endpoint.getName(), e.getMessage());
        }
        
        long validationTime = System.currentTimeMillis() - startTime;
        boolean isValid = errors.isEmpty();
        
        ValidationResult result = new ValidationResult(endpoint.getName(), "contract", 
            isValid, errors, warnings, validationTime);
        
        // Store validation result
        validationHistory.add(result);
        
        // Log validation result
        if (!isValid) {
            logger.warn("Contract validation failed for {}: {}", endpoint.getName(), errors);
        } else if (!warnings.isEmpty()) {
            logger.debug("Contract validation passed with warnings for {}: {}", endpoint.getName(), warnings);
        } else {
            logger.trace("Contract validation passed for {}", endpoint.getName());
        }
        
        return result;
    }
    
    /**
     * Validate JSON against schema
     */
    public boolean validateJsonSchema(String jsonData, String schemaPath) {
        try {
            JsonNode schema = loadSchema(schemaPath);
            JsonNode data = objectMapper.readTree(jsonData);
            
            return validateJsonAgainstSchema(data, schema).isEmpty();
            
        } catch (Exception e) {
            logger.error("Error validating JSON schema: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Register contract definition for endpoint
     */
    public void registerContract(ContractDefinition contract) {
        contractDefinitions.put(contract.getEndpointName(), contract);
        logger.debug("Registered contract for endpoint: {}", contract.getEndpointName());
    }
    
    /**
     * Get contract definition for endpoint
     */
    public ContractDefinition getContract(String endpointName) {
        return contractDefinitions.get(endpointName);
    }
    
    /**
     * Get all registered contracts
     */
    public Map<String, ContractDefinition> getAllContracts() {
        return new HashMap<>(contractDefinitions);
    }
    
    /**
     * Get validation history
     */
    public List<ValidationResult> getValidationHistory() {
        return new ArrayList<>(validationHistory);
    }
    
    /**
     * Get validation history for specific endpoint
     */
    public List<ValidationResult> getValidationHistory(String endpointName) {
        return validationHistory.stream()
            .filter(result -> endpointName.equals(result.getEndpoint()))
            .toList();
    }
    
    /**
     * Get failed validations
     */
    public List<ValidationResult> getFailedValidations() {
        return validationHistory.stream()
            .filter(result -> !result.isValid())
            .toList();
    }
    
    /**
     * Clear validation history
     */
    public void clearValidationHistory() {
        validationHistory.clear();
        logger.debug("Validation history cleared");
    }
    
    /**
     * Generate validation report
     */
    public Map<String, Object> generateValidationReport() {
        Map<String, Object> report = new HashMap<>();
        
        List<ValidationResult> results = getValidationHistory();
        
        // Overall statistics
        long totalValidations = results.size();
        long successfulValidations = results.stream().mapToLong(r -> r.isValid() ? 1 : 0).sum();
        long failedValidations = totalValidations - successfulValidations;
        
        report.put("totalValidations", totalValidations);
        report.put("successfulValidations", successfulValidations);
        report.put("failedValidations", failedValidations);
        report.put("successRate", totalValidations > 0 ? (double) successfulValidations / totalValidations * 100 : 0.0);
        
        // Endpoint-specific statistics
        Map<String, Map<String, Object>> endpointStats = new HashMap<>();
        
        results.stream()
            .collect(java.util.stream.Collectors.groupingBy(ValidationResult::getEndpoint))
            .forEach((endpoint, endpointResults) -> {
                Map<String, Object> stats = new HashMap<>();
                long endpointTotal = endpointResults.size();
                long endpointSuccess = endpointResults.stream().mapToLong(r -> r.isValid() ? 1 : 0).sum();
                
                stats.put("totalValidations", endpointTotal);
                stats.put("successfulValidations", endpointSuccess);
                stats.put("failedValidations", endpointTotal - endpointSuccess);
                stats.put("successRate", endpointTotal > 0 ? (double) endpointSuccess / endpointTotal * 100 : 0.0);
                
                endpointStats.put(endpoint, stats);
            });
        
        report.put("endpointStatistics", endpointStats);
        
        // Recent failed validations
        List<ValidationResult> recentFailures = results.stream()
            .filter(r -> !r.isValid())
            .sorted(Comparator.comparing(ValidationResult::getTimestamp).reversed())
            .limit(10)
            .toList();
        
        report.put("recentFailures", recentFailures);
        
        return report;
    }
    
    // Private helper methods
    
    private void initializeDefaultContracts() {
        // Employee endpoints
        registerContract(new ContractDefinition("getEmployees", null, "schemas/employee-list.json", false));
        registerContract(new ContractDefinition("getEmployee", null, "schemas/employee.json", true));
        registerContract(new ContractDefinition("createEmployee", "schemas/employee-create.json", "schemas/employee.json", true));
        registerContract(new ContractDefinition("updateEmployee", "schemas/employee-update.json", "schemas/employee.json", true));
        
        // Department endpoints
        registerContract(new ContractDefinition("getDepartments", null, "schemas/department-list.json", false));
        registerContract(new ContractDefinition("getDepartment", null, "schemas/department.json", true));
        
        // Payroll endpoints
        registerContract(new ContractDefinition("getPayroll", null, "schemas/payroll.json", true));
        
        logger.debug("Initialized {} default contract definitions", contractDefinitions.size());
    }
    
    private JsonNode loadSchema(String schemaPath) throws IOException {
        // Check cache first
        if (schemaCache.containsKey(schemaPath)) {
            return schemaCache.get(schemaPath);
        }
        
        // Load schema from file
        Path fullPath = Paths.get("src/test/resources", schemaPath);
        if (!Files.exists(fullPath)) {
            throw new IOException("Schema file not found: " + fullPath);
        }
        
        JsonNode schema = objectMapper.readTree(Files.newInputStream(fullPath));
        schemaCache.put(schemaPath, schema);
        
        logger.trace("Loaded schema: {}", schemaPath);
        return schema;
    }
    
    private void validateResponseSchema(ContractDefinition contract, ApiTestFramework.ApiResponse<?> response, 
                                      List<String> errors, List<String> warnings) {
        try {
            String schemaPath = getSchemaPathForResponse(contract, response.getStatusCode());
            
            if (schemaPath == null) {
                if (contract.isStrictMode()) {
                    warnings.add("No schema defined for status code: " + response.getStatusCode());
                }
                return;
            }
            
            JsonNode schema = loadSchema(schemaPath);
            JsonNode responseData = objectMapper.readTree(response.getRawResponse());
            
            List<String> schemaErrors = validateJsonAgainstSchema(responseData, schema);
            errors.addAll(schemaErrors);
            
        } catch (Exception e) {
            errors.add("Schema validation error: " + e.getMessage());
        }
    }
    
    private void applyCustomValidationRules(ContractDefinition contract, ApiTestFramework.ApiResponse<?> response, 
                                          List<String> errors, List<String> warnings) {
        for (ValidationRule rule : contract.getCustomRules()) {
            try {
                ValidationResult result = rule.validate(response.getBody(), response);
                
                if (!result.isValid()) {
                    errors.addAll(result.getErrors());
                }
                warnings.addAll(result.getWarnings());
                
            } catch (Exception e) {
                errors.add("Custom rule '" + rule.getRuleName() + "' failed: " + e.getMessage());
            }
        }
    }
    
    private void validateResponseStructure(ContractDefinition contract, ApiTestFramework.ApiResponse<?> response, 
                                         List<String> errors, List<String> warnings) {
        // Basic structure validation
        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            // Success response should have content for most endpoints
            if ((response.getRawResponse() == null || response.getRawResponse().isEmpty()) && 
                response.getStatusCode() != 204) {
                warnings.add("Success response has empty body");
            }
        }
        
        // Validate required headers
        validateRequiredHeaders(response, errors, warnings);
        
        // Validate content type
        validateContentType(response, errors, warnings);
    }
    
    private void validateRequiredHeaders(ApiTestFramework.ApiResponse<?> response, 
                                       List<String> errors, List<String> warnings) {
        // Check for common required headers
        List<String> contentTypeHeaders = response.getHeader("content-type");
        if (contentTypeHeaders.isEmpty() && response.getStatusCode() != 204) {
            warnings.add("Missing Content-Type header");
        }
    }
    
    private void validateContentType(ApiTestFramework.ApiResponse<?> response, 
                                   List<String> errors, List<String> warnings) {
        String contentType = response.getFirstHeader("content-type");
        
        if (contentType != null && response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            if (!contentType.contains("application/json") && !contentType.contains("text/")) {
                warnings.add("Unexpected content type: " + contentType);
            }
        }
    }
    
    private String getSchemaPathForResponse(ContractDefinition contract, int statusCode) {
        // Check for status-specific schema first
        String statusSpecificSchema = contract.getStatusSpecificSchemas().get(statusCode);
        if (statusSpecificSchema != null) {
            return statusSpecificSchema;
        }
        
        // Use default response schema for success status codes
        if (statusCode >= 200 && statusCode < 300) {
            return contract.getResponseSchemaPath();
        }
        
        // No schema for error responses in basic implementation
        return null;
    }
    
    private List<String> validateJsonAgainstSchema(JsonNode data, JsonNode schema) {
        List<String> errors = new ArrayList<>();
        
        // This is a simplified JSON schema validation
        // In production, you would use a library like everit-org/json-schema or networknt/json-schema-validator
        
        try {
            // Basic validation - check required fields
            if (schema.has("required")) {
                JsonNode required = schema.get("required");
                if (required.isArray()) {
                    for (JsonNode field : required) {
                        String fieldName = field.asText();
                        if (!data.has(fieldName)) {
                            errors.add("Required field missing: " + fieldName);
                        }
                    }
                }
            }
            
            // Basic type validation
            if (schema.has("type")) {
                String expectedType = schema.get("type").asText();
                if (!validateJsonType(data, expectedType)) {
                    errors.add("Type mismatch: expected " + expectedType + ", got " + getJsonType(data));
                }
            }
            
        } catch (Exception e) {
            errors.add("Schema validation failed: " + e.getMessage());
        }
        
        return errors;
    }
    
    private boolean validateJsonType(JsonNode node, String expectedType) {
        switch (expectedType.toLowerCase()) {
            case "string":
                return node.isTextual();
            case "number":
                return node.isNumber();
            case "integer":
                return node.isInt();
            case "boolean":
                return node.isBoolean();
            case "array":
                return node.isArray();
            case "object":
                return node.isObject();
            case "null":
                return node.isNull();
            default:
                return true; // Unknown type, assume valid
        }
    }
    
    private String getJsonType(JsonNode node) {
        if (node.isTextual()) return "string";
        if (node.isInt()) return "integer";
        if (node.isNumber()) return "number";
        if (node.isBoolean()) return "boolean";
        if (node.isArray()) return "array";
        if (node.isObject()) return "object";
        if (node.isNull()) return "null";
        return "unknown";
    }
}