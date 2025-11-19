package com.phoenix.hrm.core.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Advanced API Response Validator for Phoenix HRM Test Automation Framework
 * Provides comprehensive response validation including JSON schema validation
 */
public class ApiResponseValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiResponseValidator.class);
    private final ObjectMapper objectMapper;
    private final Response response;
    
    public ApiResponseValidator(Response response) {
        this.response = response;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Create validator instance
     */
    public static ApiResponseValidator of(Response response) {
        return new ApiResponseValidator(response);
    }
    
    // ==================== STATUS CODE VALIDATIONS ====================
    
    /**
     * Validate response status code
     */
    public ApiResponseValidator validateStatusCode(int expectedStatusCode) {
        int actualStatusCode = response.getStatusCode();
        assertThat(String.format("Status code should be %d", expectedStatusCode), 
                   actualStatusCode, equalTo(expectedStatusCode));
        logger.info("Status code validation passed: {}", expectedStatusCode);
        return this;
    }
    
    /**
     * Validate status code is in success range (200-299)
     */
    public ApiResponseValidator validateSuccessStatusCode() {
        int statusCode = response.getStatusCode();
        assertThat("Status code should be in success range (200-299)", 
                   statusCode, allOf(greaterThanOrEqualTo(200), lessThan(300)));
        logger.info("Success status code validation passed: {}", statusCode);
        return this;
    }
    
    /**
     * Validate status code is in client error range (400-499)
     */
    public ApiResponseValidator validateClientErrorStatusCode() {
        int statusCode = response.getStatusCode();
        assertThat("Status code should be in client error range (400-499)", 
                   statusCode, allOf(greaterThanOrEqualTo(400), lessThan(500)));
        logger.info("Client error status code validation passed: {}", statusCode);
        return this;
    }
    
    /**
     * Validate status code is in server error range (500-599)
     */
    public ApiResponseValidator validateServerErrorStatusCode() {
        int statusCode = response.getStatusCode();
        assertThat("Status code should be in server error range (500-599)", 
                   statusCode, allOf(greaterThanOrEqualTo(500), lessThan(600)));
        logger.info("Server error status code validation passed: {}", statusCode);
        return this;
    }
    
    // ==================== RESPONSE TIME VALIDATIONS ====================
    
    /**
     * Validate response time is less than specified milliseconds
     */
    public ApiResponseValidator validateResponseTime(long maxTimeInMs) {
        long actualTime = response.getTime();
        assertThat(String.format("Response time should be less than %d ms", maxTimeInMs), 
                   actualTime, lessThan(maxTimeInMs));
        logger.info("Response time validation passed: {} ms (max: {} ms)", actualTime, maxTimeInMs);
        return this;
    }
    
    /**
     * Validate response time is within range
     */
    public ApiResponseValidator validateResponseTimeRange(long minTimeInMs, long maxTimeInMs) {
        long actualTime = response.getTime();
        assertThat(String.format("Response time should be between %d and %d ms", minTimeInMs, maxTimeInMs), 
                   actualTime, allOf(greaterThanOrEqualTo(minTimeInMs), lessThanOrEqualTo(maxTimeInMs)));
        logger.info("Response time range validation passed: {} ms ({}-{} ms)", actualTime, minTimeInMs, maxTimeInMs);
        return this;
    }
    
    // ==================== HEADER VALIDATIONS ====================
    
    /**
     * Validate response header exists
     */
    public ApiResponseValidator validateHeaderExists(String headerName) {
        String headerValue = response.getHeader(headerName);
        assertThat(String.format("Header '%s' should exist", headerName), 
                   headerValue, notNullValue());
        logger.info("Header existence validation passed: {}", headerName);
        return this;
    }
    
    /**
     * Validate response header value
     */
    public ApiResponseValidator validateHeaderValue(String headerName, String expectedValue) {
        String actualValue = response.getHeader(headerName);
        assertThat(String.format("Header '%s' should have value '%s'", headerName, expectedValue), 
                   actualValue, equalTo(expectedValue));
        logger.info("Header value validation passed: {} = {}", headerName, expectedValue);
        return this;
    }
    
    /**
     * Validate response header contains value
     */
    public ApiResponseValidator validateHeaderContains(String headerName, String expectedSubstring) {
        String actualValue = response.getHeader(headerName);
        assertThat(String.format("Header '%s' should contain '%s'", headerName, expectedSubstring), 
                   actualValue, containsString(expectedSubstring));
        logger.info("Header contains validation passed: {} contains {}", headerName, expectedSubstring);
        return this;
    }
    
    /**
     * Validate Content-Type header
     */
    public ApiResponseValidator validateContentType(String expectedContentType) {
        return validateHeaderValue("Content-Type", expectedContentType);
    }
    
    /**
     * Validate response is JSON
     */
    public ApiResponseValidator validateJsonContentType() {
        return validateHeaderContains("Content-Type", "application/json");
    }
    
    // ==================== BODY VALIDATIONS ====================
    
    /**
     * Validate response body is not empty
     */
    public ApiResponseValidator validateBodyNotEmpty() {
        String body = response.getBody().asString();
        assertThat("Response body should not be empty", body, not(emptyString()));
        logger.info("Response body not empty validation passed");
        return this;
    }
    
    /**
     * Validate response body contains text
     */
    public ApiResponseValidator validateBodyContains(String expectedText) {
        String body = response.getBody().asString();
        assertThat(String.format("Response body should contain '%s'", expectedText), 
                   body, containsString(expectedText));
        logger.info("Response body contains validation passed: {}", expectedText);
        return this;
    }
    
    /**
     * Validate response body does not contain text
     */
    public ApiResponseValidator validateBodyNotContains(String unexpectedText) {
        String body = response.getBody().asString();
        assertThat(String.format("Response body should not contain '%s'", unexpectedText), 
                   body, not(containsString(unexpectedText)));
        logger.info("Response body not contains validation passed: {}", unexpectedText);
        return this;
    }
    
    /**
     * Validate response body matches regex pattern
     */
    public ApiResponseValidator validateBodyMatchesPattern(String regexPattern) {
        String body = response.getBody().asString();
        Pattern pattern = Pattern.compile(regexPattern);
        assertThat(String.format("Response body should match pattern '%s'", regexPattern), 
                   pattern.matcher(body).find(), equalTo(true));
        logger.info("Response body pattern validation passed: {}", regexPattern);
        return this;
    }
    
    // ==================== JSON PATH VALIDATIONS ====================
    
    /**
     * Validate JSON path exists
     */
    public ApiResponseValidator validateJsonPathExists(String jsonPath) {
        try {
            Object value = response.jsonPath().get(jsonPath);
            assertThat(String.format("JSON path '%s' should exist", jsonPath), 
                       value, notNullValue());
            logger.info("JSON path existence validation passed: {}", jsonPath);
        } catch (Exception e) {
            throw new AssertionError("JSON path '" + jsonPath + "' does not exist in response", e);
        }
        return this;
    }
    
    /**
     * Validate JSON path value
     */
    public <T> ApiResponseValidator validateJsonPathValue(String jsonPath, T expectedValue) {
        try {
            T actualValue = response.jsonPath().get(jsonPath);
            assertThat(String.format("JSON path '%s' should have value '%s'", jsonPath, expectedValue), 
                       actualValue, equalTo(expectedValue));
            logger.info("JSON path value validation passed: {} = {}", jsonPath, expectedValue);
        } catch (Exception e) {
            throw new AssertionError("Failed to validate JSON path '" + jsonPath + "'", e);
        }
        return this;
    }
    
    /**
     * Validate JSON path contains value
     */
    public ApiResponseValidator validateJsonPathContains(String jsonPath, String expectedSubstring) {
        try {
            String actualValue = response.jsonPath().getString(jsonPath);
            assertThat(String.format("JSON path '%s' should contain '%s'", jsonPath, expectedSubstring), 
                       actualValue, containsString(expectedSubstring));
            logger.info("JSON path contains validation passed: {} contains {}", jsonPath, expectedSubstring);
        } catch (Exception e) {
            throw new AssertionError("Failed to validate JSON path '" + jsonPath + "'", e);
        }
        return this;
    }
    
    /**
     * Validate JSON path array size
     */
    public ApiResponseValidator validateJsonPathArraySize(String jsonPath, int expectedSize) {
        try {
            List<Object> array = response.jsonPath().getList(jsonPath);
            assertThat(String.format("JSON path '%s' array should have size %d", jsonPath, expectedSize), 
                       array.size(), equalTo(expectedSize));
            logger.info("JSON path array size validation passed: {} size = {}", jsonPath, expectedSize);
        } catch (Exception e) {
            throw new AssertionError("Failed to validate JSON path array size '" + jsonPath + "'", e);
        }
        return this;
    }
    
    /**
     * Validate JSON path array is not empty
     */
    public ApiResponseValidator validateJsonPathArrayNotEmpty(String jsonPath) {
        try {
            List<Object> array = response.jsonPath().getList(jsonPath);
            assertThat(String.format("JSON path '%s' array should not be empty", jsonPath), 
                       array, not(empty()));
            logger.info("JSON path array not empty validation passed: {}", jsonPath);
        } catch (Exception e) {
            throw new AssertionError("Failed to validate JSON path array '" + jsonPath + "'", e);
        }
        return this;
    }
    
    /**
     * Validate JSON path array contains value
     */
    public <T> ApiResponseValidator validateJsonPathArrayContains(String jsonPath, T expectedValue) {
        try {
            List<T> array = response.jsonPath().getList(jsonPath);
            assertThat(String.format("JSON path '%s' array should contain '%s'", jsonPath, expectedValue), 
                       array, hasItem(expectedValue));
            logger.info("JSON path array contains validation passed: {} contains {}", jsonPath, expectedValue);
        } catch (Exception e) {
            throw new AssertionError("Failed to validate JSON path array contains '" + jsonPath + "'", e);
        }
        return this;
    }
    
    /**
     * Validate JSON path number is greater than
     */
    public ApiResponseValidator validateJsonPathGreaterThan(String jsonPath, Number expectedValue) {
        try {
            Number actualValue = response.jsonPath().get(jsonPath);
            assertThat(String.format("JSON path '%s' should be greater than %s", jsonPath, expectedValue), 
                       actualValue.doubleValue(), greaterThan(expectedValue.doubleValue()));
            logger.info("JSON path greater than validation passed: {} > {}", jsonPath, expectedValue);
        } catch (Exception e) {
            throw new AssertionError("Failed to validate JSON path greater than '" + jsonPath + "'", e);
        }
        return this;
    }
    
    /**
     * Validate JSON path number is less than
     */
    public ApiResponseValidator validateJsonPathLessThan(String jsonPath, Number expectedValue) {
        try {
            Number actualValue = response.jsonPath().get(jsonPath);
            assertThat(String.format("JSON path '%s' should be less than %s", jsonPath, expectedValue), 
                       actualValue.doubleValue(), lessThan(expectedValue.doubleValue()));
            logger.info("JSON path less than validation passed: {} < {}", jsonPath, expectedValue);
        } catch (Exception e) {
            throw new AssertionError("Failed to validate JSON path less than '" + jsonPath + "'", e);
        }
        return this;
    }
    
    // ==================== JSON SCHEMA VALIDATIONS ====================
    
    /**
     * Basic JSON structure validation
     */
    public ApiResponseValidator validateJsonStructure(Map<String, Class<?>> expectedFields) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response.getBody().asString());
            
            for (Map.Entry<String, Class<?>> field : expectedFields.entrySet()) {
                String fieldName = field.getKey();
                Class<?> expectedType = field.getValue();
                
                assertThat(String.format("Field '%s' should exist in JSON response", fieldName), 
                           jsonNode.has(fieldName), equalTo(true));
                
                JsonNode fieldNode = jsonNode.get(fieldName);
                validateJsonFieldType(fieldNode, expectedType, fieldName);
            }
            
            logger.info("JSON structure validation passed for {} fields", expectedFields.size());
        } catch (Exception e) {
            throw new AssertionError("JSON structure validation failed", e);
        }
        return this;
    }
    
    /**
     * Validate JSON field type
     */
    private void validateJsonFieldType(JsonNode fieldNode, Class<?> expectedType, String fieldName) {
        if (expectedType == String.class) {
            assertThat(String.format("Field '%s' should be a string", fieldName), 
                       fieldNode.isTextual(), equalTo(true));
        } else if (expectedType == Integer.class || expectedType == int.class) {
            assertThat(String.format("Field '%s' should be an integer", fieldName), 
                       fieldNode.isInt(), equalTo(true));
        } else if (expectedType == Long.class || expectedType == long.class) {
            assertThat(String.format("Field '%s' should be a long", fieldName), 
                       fieldNode.isLong() || fieldNode.isInt(), equalTo(true));
        } else if (expectedType == Double.class || expectedType == double.class) {
            assertThat(String.format("Field '%s' should be a double", fieldName), 
                       fieldNode.isDouble() || fieldNode.isFloat(), equalTo(true));
        } else if (expectedType == Boolean.class || expectedType == boolean.class) {
            assertThat(String.format("Field '%s' should be a boolean", fieldName), 
                       fieldNode.isBoolean(), equalTo(true));
        } else if (expectedType == List.class) {
            assertThat(String.format("Field '%s' should be an array", fieldName), 
                       fieldNode.isArray(), equalTo(true));
        }
    }
    
    /**
     * Validate response has required fields
     */
    public ApiResponseValidator validateRequiredFields(String... fieldNames) {
        Map<String, Class<?>> fields = new HashMap<>();
        for (String fieldName : fieldNames) {
            fields.put(fieldName, Object.class); // Generic type check
        }
        
        try {
            JsonNode jsonNode = objectMapper.readTree(response.getBody().asString());
            for (String fieldName : fieldNames) {
                assertThat(String.format("Required field '%s' should exist", fieldName), 
                           jsonNode.has(fieldName), equalTo(true));
            }
            logger.info("Required fields validation passed: {}", String.join(", ", fieldNames));
        } catch (Exception e) {
            throw new AssertionError("Required fields validation failed", e);
        }
        return this;
    }
    
    /**
     * Validate response does not have forbidden fields
     */
    public ApiResponseValidator validateForbiddenFields(String... fieldNames) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response.getBody().asString());
            for (String fieldName : fieldNames) {
                assertThat(String.format("Forbidden field '%s' should not exist", fieldName), 
                           jsonNode.has(fieldName), equalTo(false));
            }
            logger.info("Forbidden fields validation passed: {}", String.join(", ", fieldNames));
        } catch (Exception e) {
            throw new AssertionError("Forbidden fields validation failed", e);
        }
        return this;
    }
    
    // ==================== CUSTOM VALIDATIONS ====================
    
    /**
     * Custom validation with lambda expression
     */
    public ApiResponseValidator validateCustom(String description, java.util.function.Predicate<Response> validator) {
        assertThat(description, validator.test(response), equalTo(true));
        logger.info("Custom validation passed: {}", description);
        return this;
    }
    
    /**
     * Validate multiple conditions (AND logic)
     */
    public ApiResponseValidator validateAll(java.util.function.Predicate<Response>... validators) {
        for (int i = 0; i < validators.length; i++) {
            assertThat(String.format("Validation %d should pass", i + 1), 
                       validators[i].test(response), equalTo(true));
        }
        logger.info("All validations passed ({} conditions)", validators.length);
        return this;
    }
    
    /**
     * Validate at least one condition (OR logic)
     */
    public ApiResponseValidator validateAny(java.util.function.Predicate<Response>... validators) {
        boolean anyPassed = false;
        for (java.util.function.Predicate<Response> validator : validators) {
            if (validator.test(response)) {
                anyPassed = true;
                break;
            }
        }
        assertThat("At least one validation should pass", anyPassed, equalTo(true));
        logger.info("At least one validation passed ({} conditions)", validators.length);
        return this;
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Get response for further processing
     */
    public Response getResponse() {
        return response;
    }
    
    /**
     * Extract and validate JSON path in one step
     */
    public <T> T extractAndValidateJsonPath(String jsonPath, Class<T> expectedType) {
        validateJsonPathExists(jsonPath);
        T value = response.jsonPath().get(jsonPath);
        assertThat(String.format("JSON path '%s' should be of type %s", jsonPath, expectedType.getSimpleName()), 
                   expectedType.isInstance(value), equalTo(true));
        logger.info("JSON path extracted and validated: {} = {} ({})", jsonPath, value, expectedType.getSimpleName());
        return value;
    }
    
    /**
     * Validate and log response summary
     */
    public ApiResponseValidator logResponseSummary() {
        logger.info("=== API Response Summary ===");
        logger.info("Status Code: {}", response.getStatusCode());
        logger.info("Response Time: {} ms", response.getTime());
        logger.info("Content Type: {}", response.getContentType());
        logger.info("Response Size: {} bytes", response.getBody().asString().length());
        logger.info("==============================");
        return this;
    }
}
