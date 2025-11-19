package com.phoenix.hrm.core.api;

import com.phoenix.hrm.core.config.ConfigManager;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.*;

/**
 * Advanced API Client for Phoenix HRM Test Automation Framework
 * Provides comprehensive REST API testing capabilities with RestAssured
 */
public class ApiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiClient.class);
    private static final ConfigManager config = ConfigManager.getInstance();
    
    private RequestSpecification requestSpec;
    private ResponseSpecification responseSpec;
    private String baseUri;
    private String basePath;
    private Map<String, String> defaultHeaders;
    private Map<String, Object> defaultQueryParams;
    
    // Singleton instance for thread-safe operations
    private static final ThreadLocal<ApiClient> apiClientThreadLocal = new ThreadLocal<>();
    
    /**
     * Private constructor to initialize API client
     */
    private ApiClient() {
        initializeClient();
    }
    
    /**
     * Get thread-safe API client instance
     */
    public static ApiClient getInstance() {
        ApiClient client = apiClientThreadLocal.get();
        if (client == null) {
            client = new ApiClient();
            apiClientThreadLocal.set(client);
        }
        return client;
    }
    
    /**
     * Initialize API client with default configurations
     */
    private void initializeClient() {
        this.baseUri = config.getApiBaseUrl();
        this.basePath = config.getProperty("api.base.path", "/api/v1");
        this.defaultHeaders = new HashMap<>();
        this.defaultQueryParams = new HashMap<>();
        
        // Set default headers
        defaultHeaders.put("Content-Type", "application/json");
        defaultHeaders.put("Accept", "application/json");
        defaultHeaders.put("User-Agent", "Phoenix-HRM-TestFramework/1.0");
        
        buildRequestSpecification();
        buildResponseSpecification();
        
        logger.info("API Client initialized with base URI: {}{}", baseUri, basePath);
    }
    
    /**
     * Build request specification with default settings
     */
    private void buildRequestSpecification() {
        RequestSpecBuilder requestBuilder = new RequestSpecBuilder();
        
        requestBuilder
            .setBaseUri(baseUri)
            .setBasePath(basePath)
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .addHeaders(defaultHeaders)
            .addQueryParams(defaultQueryParams)
            .setRelaxedHTTPSValidation()
            .log(LogDetail.ALL);
        
        // Set timeout configurations
        requestBuilder.setConfig(
            RestAssured.config()
                .httpClient(
                    RestAssured.config().getHttpClientConfig()
                        .setParam("http.connection.timeout", config.getApiTimeout() * 1000)
                        .setParam("http.socket.timeout", config.getApiTimeout() * 1000)
                )
        );
        
        this.requestSpec = requestBuilder.build();
    }
    
    /**
     * Build response specification with default validations
     */
    private void buildResponseSpecification() {
        ResponseSpecBuilder responseBuilder = new ResponseSpecBuilder();
        
        responseBuilder
            .expectResponseTime(org.hamcrest.Matchers.lessThan(config.getApiTimeout() * 1000L), TimeUnit.MILLISECONDS)
            .log(LogDetail.ALL);
        
        this.responseSpec = responseBuilder.build();
    }
    
    /**
     * Set authentication token
     */
    public ApiClient setAuthToken(String token) {
        if (token != null && !token.isEmpty()) {
            defaultHeaders.put("Authorization", "Bearer " + token);
            buildRequestSpecification();
            logger.debug("Authentication token set");
        }
        return this;
    }
    
    /**
     * Set basic authentication
     */
    public ApiClient setBasicAuth(String username, String password) {
        this.requestSpec = given(requestSpec).auth().basic(username, password);
        logger.debug("Basic authentication set for user: {}", username);
        return this;
    }
    
    /**
     * Add custom header
     */
    public ApiClient addHeader(String name, String value) {
        defaultHeaders.put(name, value);
        buildRequestSpecification();
        return this;
    }
    
    /**
     * Add multiple headers
     */
    public ApiClient addHeaders(Map<String, String> headers) {
        defaultHeaders.putAll(headers);
        buildRequestSpecification();
        return this;
    }
    
    /**
     * Add query parameter
     */
    public ApiClient addQueryParam(String name, Object value) {
        defaultQueryParams.put(name, value);
        buildRequestSpecification();
        return this;
    }
    
    /**
     * Add multiple query parameters
     */
    public ApiClient addQueryParams(Map<String, Object> queryParams) {
        defaultQueryParams.putAll(queryParams);
        buildRequestSpecification();
        return this;
    }
    
    /**
     * Set base URI
     */
    public ApiClient setBaseUri(String baseUri) {
        this.baseUri = baseUri;
        buildRequestSpecification();
        return this;
    }
    
    /**
     * Set base path
     */
    public ApiClient setBasePath(String basePath) {
        this.basePath = basePath;
        buildRequestSpecification();
        return this;
    }
    
    /**
     * GET request
     */
    public Response get(String endpoint) {
        logger.info("Executing GET request to: {}", endpoint);
        Response response = given(requestSpec)
            .when()
            .get(endpoint)
            .then()
            .spec(responseSpec)
            .extract()
            .response();
        
        logResponse(response);
        return response;
    }
    
    /**
     * GET request with path parameters
     */
    public Response get(String endpoint, Map<String, Object> pathParams) {
        logger.info("Executing GET request to: {} with path params: {}", endpoint, pathParams);
        Response response = given(requestSpec)
            .pathParams(pathParams)
            .when()
            .get(endpoint)
            .then()
            .spec(responseSpec)
            .extract()
            .response();
        
        logResponse(response);
        return response;
    }
    
    /**
     * POST request with JSON body
     */
    public Response post(String endpoint, Object requestBody) {
        logger.info("Executing POST request to: {}", endpoint);
        Response response = given(requestSpec)
            .body(requestBody)
            .when()
            .post(endpoint)
            .then()
            .spec(responseSpec)
            .extract()
            .response();
        
        logResponse(response);
        return response;
    }
    
    /**
     * POST request without body
     */
    public Response post(String endpoint) {
        return post(endpoint, "");
    }
    
    /**
     * PUT request with JSON body
     */
    public Response put(String endpoint, Object requestBody) {
        logger.info("Executing PUT request to: {}", endpoint);
        Response response = given(requestSpec)
            .body(requestBody)
            .when()
            .put(endpoint)
            .then()
            .spec(responseSpec)
            .extract()
            .response();
        
        logResponse(response);
        return response;
    }
    
    /**
     * PUT request with path parameters
     */
    public Response put(String endpoint, Object requestBody, Map<String, Object> pathParams) {
        logger.info("Executing PUT request to: {} with path params: {}", endpoint, pathParams);
        Response response = given(requestSpec)
            .pathParams(pathParams)
            .body(requestBody)
            .when()
            .put(endpoint)
            .then()
            .spec(responseSpec)
            .extract()
            .response();
        
        logResponse(response);
        return response;
    }
    
    /**
     * PATCH request with JSON body
     */
    public Response patch(String endpoint, Object requestBody) {
        logger.info("Executing PATCH request to: {}", endpoint);
        Response response = given(requestSpec)
            .body(requestBody)
            .when()
            .patch(endpoint)
            .then()
            .spec(responseSpec)
            .extract()
            .response();
        
        logResponse(response);
        return response;
    }
    
    /**
     * DELETE request
     */
    public Response delete(String endpoint) {
        logger.info("Executing DELETE request to: {}", endpoint);
        Response response = given(requestSpec)
            .when()
            .delete(endpoint)
            .then()
            .spec(responseSpec)
            .extract()
            .response();
        
        logResponse(response);
        return response;
    }
    
    /**
     * DELETE request with path parameters
     */
    public Response delete(String endpoint, Map<String, Object> pathParams) {
        logger.info("Executing DELETE request to: {} with path params: {}", endpoint, pathParams);
        Response response = given(requestSpec)
            .pathParams(pathParams)
            .when()
            .delete(endpoint)
            .then()
            .spec(responseSpec)
            .extract()
            .response();
        
        logResponse(response);
        return response;
    }
    
    /**
     * HEAD request
     */
    public Response head(String endpoint) {
        logger.info("Executing HEAD request to: {}", endpoint);
        Response response = given(requestSpec)
            .when()
            .head(endpoint)
            .then()
            .spec(responseSpec)
            .extract()
            .response();
        
        logResponse(response);
        return response;
    }
    
    /**
     * OPTIONS request
     */
    public Response options(String endpoint) {
        logger.info("Executing OPTIONS request to: {}", endpoint);
        Response response = given(requestSpec)
            .when()
            .options(endpoint)
            .then()
            .spec(responseSpec)
            .extract()
            .response();
        
        logResponse(response);
        return response;
    }
    
    /**
     * Custom request with full control
     */
    public Response customRequest(String method, String endpoint, Object requestBody, 
                                Map<String, String> headers, Map<String, Object> queryParams) {
        logger.info("Executing {} request to: {}", method.toUpperCase(), endpoint);
        
        RequestSpecification spec = given(requestSpec);
        
        if (headers != null && !headers.isEmpty()) {
            spec = spec.headers(headers);
        }
        
        if (queryParams != null && !queryParams.isEmpty()) {
            spec = spec.queryParams(queryParams);
        }
        
        if (requestBody != null) {
            spec = spec.body(requestBody);
        }
        
        Response response = spec
            .when()
            .request(method.toUpperCase(), endpoint)
            .then()
            .spec(responseSpec)
            .extract()
            .response();
        
        logResponse(response);
        return response;
    }
    
    /**
     * Log response details
     */
    private void logResponse(Response response) {
        logger.info("Response Status: {} - {}", response.getStatusCode(), response.getStatusLine());
        logger.debug("Response Time: {} ms", response.getTime());
        logger.debug("Response Headers: {}", response.getHeaders());
        
        if (response.getBody() != null) {
            logger.debug("Response Body: {}", response.getBody().asString());
        }
    }
    
    /**
     * Validate response status code
     */
    public ApiClient validateStatusCode(Response response, int expectedStatusCode) {
        int actualStatusCode = response.getStatusCode();
        if (actualStatusCode != expectedStatusCode) {
            throw new AssertionError(
                String.format("Expected status code %d but got %d. Response: %s", 
                    expectedStatusCode, actualStatusCode, response.getBody().asString())
            );
        }
        logger.info("Status code validation passed: {}", expectedStatusCode);
        return this;
    }
    
    /**
     * Validate response contains specific text
     */
    public ApiClient validateResponseContains(Response response, String expectedText) {
        String responseBody = response.getBody().asString();
        if (!responseBody.contains(expectedText)) {
            throw new AssertionError(
                String.format("Response body does not contain expected text: '%s'. Actual response: %s", 
                    expectedText, responseBody)
            );
        }
        logger.info("Response content validation passed for text: {}", expectedText);
        return this;
    }
    
    /**
     * Validate response time
     */
    public ApiClient validateResponseTime(Response response, long maxTimeInMs) {
        long actualTime = response.getTime();
        if (actualTime > maxTimeInMs) {
            throw new AssertionError(
                String.format("Response time %d ms exceeded maximum allowed time %d ms", 
                    actualTime, maxTimeInMs)
            );
        }
        logger.info("Response time validation passed: {} ms (max: {} ms)", actualTime, maxTimeInMs);
        return this;
    }
    
    /**
     * Extract JSON path value from response
     */
    public <T> T extractJsonPath(Response response, String jsonPath) {
        try {
            T value = response.jsonPath().get(jsonPath);
            logger.debug("Extracted value from JSON path '{}': {}", jsonPath, value);
            return value;
        } catch (Exception e) {
            logger.error("Failed to extract JSON path '{}' from response", jsonPath, e);
            throw new RuntimeException("JSON path extraction failed", e);
        }
    }
    
    /**
     * Reset client to default state
     */
    public ApiClient reset() {
        apiClientThreadLocal.remove();
        logger.info("API client reset to default state");
        return getInstance();
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        apiClientThreadLocal.remove();
        logger.debug("API client resources cleaned up");
    }
}