package com.phoenix.hrm.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Advanced API Testing Framework for Phoenix HRM Test Automation
 * 
 * Provides comprehensive API testing capabilities including:
 * - HTTP client with advanced configuration and connection pooling
 * - Request/Response validation with JSON Schema support
 * - Contract testing with OpenAPI/Swagger integration
 * - Performance testing integration with metrics collection
 * - API mocking and virtualization capabilities
 * - Authentication and authorization testing support
 * - Comprehensive assertion framework for API responses
 * - Test data integration with the TestDataManager
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 5.0
 * @since Phase 5
 */
public class ApiTestFramework {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiTestFramework.class);
    
    // Singleton instance
    private static volatile ApiTestFramework instance;
    private static final Object instanceLock = new Object();
    
    // Core components
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ApiConfiguration config;
    private final Map<String, ApiEndpoint> endpoints;
    private final RequestResponseLogger requestLogger;
    private final PerformanceMetrics performanceMetrics;
    private final ContractValidator contractValidator;
    private final AuthenticationManager authManager;
    
    /**
     * API Framework Configuration
     */
    public static class ApiConfiguration {
        private String baseUrl = "http://localhost:8080/api";
        private Duration connectTimeout = Duration.ofSeconds(30);
        private Duration requestTimeout = Duration.ofMinutes(2);
        private boolean followRedirects = true;
        private boolean enableSslValidation = true;
        private boolean enableRequestLogging = true;
        private boolean enablePerformanceMetrics = true;
        private boolean enableContractValidation = true;
        private String authenticationScheme = "Bearer";
        private Map<String, String> defaultHeaders = new HashMap<>();
        private int maxRetryAttempts = 3;
        private Duration retryDelay = Duration.ofSeconds(1);
        
        // Builder pattern
        public static class Builder {
            private final ApiConfiguration config = new ApiConfiguration();
            
            public Builder baseUrl(String baseUrl) {
                config.baseUrl = baseUrl;
                return this;
            }
            
            public Builder connectTimeout(Duration connectTimeout) {
                config.connectTimeout = connectTimeout;
                return this;
            }
            
            public Builder requestTimeout(Duration requestTimeout) {
                config.requestTimeout = requestTimeout;
                return this;
            }
            
            public Builder followRedirects(boolean followRedirects) {
                config.followRedirects = followRedirects;
                return this;
            }
            
            public Builder enableSslValidation(boolean enableSslValidation) {
                config.enableSslValidation = enableSslValidation;
                return this;
            }
            
            public Builder enableRequestLogging(boolean enableRequestLogging) {
                config.enableRequestLogging = enableRequestLogging;
                return this;
            }
            
            public Builder enablePerformanceMetrics(boolean enablePerformanceMetrics) {
                config.enablePerformanceMetrics = enablePerformanceMetrics;
                return this;
            }
            
            public Builder enableContractValidation(boolean enableContractValidation) {
                config.enableContractValidation = enableContractValidation;
                return this;
            }
            
            public Builder authenticationScheme(String authenticationScheme) {
                config.authenticationScheme = authenticationScheme;
                return this;
            }
            
            public Builder addDefaultHeader(String name, String value) {
                config.defaultHeaders.put(name, value);
                return this;
            }
            
            public Builder maxRetryAttempts(int maxRetryAttempts) {
                config.maxRetryAttempts = maxRetryAttempts;
                return this;
            }
            
            public Builder retryDelay(Duration retryDelay) {
                config.retryDelay = retryDelay;
                return this;
            }
            
            public ApiConfiguration build() {
                // Set default headers
                config.defaultHeaders.putIfAbsent("Content-Type", "application/json");
                config.defaultHeaders.putIfAbsent("Accept", "application/json");
                config.defaultHeaders.putIfAbsent("User-Agent", "Phoenix-HRM-ApiTest/5.0");
                
                return config;
            }
        }
        
        // Getters
        public String getBaseUrl() { return baseUrl; }
        public Duration getConnectTimeout() { return connectTimeout; }
        public Duration getRequestTimeout() { return requestTimeout; }
        public boolean isFollowRedirects() { return followRedirects; }
        public boolean isEnableSslValidation() { return enableSslValidation; }
        public boolean isEnableRequestLogging() { return enableRequestLogging; }
        public boolean isEnablePerformanceMetrics() { return enablePerformanceMetrics; }
        public boolean isEnableContractValidation() { return enableContractValidation; }
        public String getAuthenticationScheme() { return authenticationScheme; }
        public Map<String, String> getDefaultHeaders() { return defaultHeaders; }
        public int getMaxRetryAttempts() { return maxRetryAttempts; }
        public Duration getRetryDelay() { return retryDelay; }
    }
    
    /**
     * API Endpoint definition
     */
    public static class ApiEndpoint {
        private final String name;
        private final String path;
        private final HttpMethod method;
        private final Map<String, String> headers;
        private final Map<String, Object> queryParams;
        private final String requestBody;
        private final Class<?> responseType;
        private final Duration customTimeout;
        private final boolean requiresAuth;
        private final Set<String> requiredScopes;
        
        public enum HttpMethod {
            GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
        }
        
        public ApiEndpoint(String name, String path, HttpMethod method) {
            this.name = name;
            this.path = path;
            this.method = method;
            this.headers = new HashMap<>();
            this.queryParams = new HashMap<>();
            this.requestBody = null;
            this.responseType = String.class;
            this.customTimeout = null;
            this.requiresAuth = false;
            this.requiredScopes = new HashSet<>();
        }
        
        // Builder for endpoint creation
        public static class Builder {
            private final ApiEndpoint endpoint;
            
            public Builder(String name, String path, HttpMethod method) {
                this.endpoint = new ApiEndpoint(name, path, method);
            }
            
            public Builder addHeader(String name, String value) {
                endpoint.headers.put(name, value);
                return this;
            }
            
            public Builder addQueryParam(String name, Object value) {
                endpoint.queryParams.put(name, value);
                return this;
            }
            
            public Builder requestBody(String body) {
                return this; // Implementation would set request body
            }
            
            public Builder responseType(Class<?> type) {
                return this; // Implementation would set response type
            }
            
            public Builder timeout(Duration timeout) {
                return this; // Implementation would set custom timeout
            }
            
            public Builder requiresAuth(boolean requiresAuth) {
                return this; // Implementation would set auth requirement
            }
            
            public Builder addRequiredScope(String scope) {
                endpoint.requiredScopes.add(scope);
                return this;
            }
            
            public ApiEndpoint build() {
                return endpoint;
            }
        }
        
        // Getters
        public String getName() { return name; }
        public String getPath() { return path; }
        public HttpMethod getMethod() { return method; }
        public Map<String, String> getHeaders() { return headers; }
        public Map<String, Object> getQueryParams() { return queryParams; }
        public String getRequestBody() { return requestBody; }
        public Class<?> getResponseType() { return responseType; }
        public Duration getCustomTimeout() { return customTimeout; }
        public boolean requiresAuth() { return requiresAuth; }
        public Set<String> getRequiredScopes() { return requiredScopes; }
    }
    
    /**
     * API Response wrapper
     */
    public static class ApiResponse<T> {
        private final int statusCode;
        private final String reasonPhrase;
        private final Map<String, List<String>> headers;
        private final T body;
        private final long responseTime;
        private final LocalDateTime timestamp;
        private final boolean success;
        private final String rawResponse;
        
        public ApiResponse(int statusCode, String reasonPhrase, Map<String, List<String>> headers, 
                          T body, long responseTime, String rawResponse) {
            this.statusCode = statusCode;
            this.reasonPhrase = reasonPhrase;
            this.headers = headers;
            this.body = body;
            this.responseTime = responseTime;
            this.timestamp = LocalDateTime.now();
            this.success = statusCode >= 200 && statusCode < 300;
            this.rawResponse = rawResponse;
        }
        
        // Getters
        public int getStatusCode() { return statusCode; }
        public String getReasonPhrase() { return reasonPhrase; }
        public Map<String, List<String>> getHeaders() { return headers; }
        public T getBody() { return body; }
        public long getResponseTime() { return responseTime; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public boolean isSuccess() { return success; }
        public String getRawResponse() { return rawResponse; }
        
        public List<String> getHeader(String name) {
            return headers.getOrDefault(name.toLowerCase(), Collections.emptyList());
        }
        
        public String getFirstHeader(String name) {
            List<String> values = getHeader(name);
            return values.isEmpty() ? null : values.get(0);
        }
        
        @Override
        public String toString() {
            return String.format("ApiResponse{status=%d, time=%dms, success=%b}", 
                statusCode, responseTime, success);
        }
    }
    
    /**
     * Private constructor for singleton
     */
    private ApiTestFramework(ApiConfiguration config) {
        this.config = config != null ? config : new ApiConfiguration.Builder().build();
        this.objectMapper = new ObjectMapper();
        this.endpoints = new ConcurrentHashMap<>();
        this.requestLogger = new RequestResponseLogger(this.config);
        this.performanceMetrics = new PerformanceMetrics();
        this.contractValidator = new ContractValidator(this.config);
        this.authManager = new AuthenticationManager(this.config);
        
        // Initialize HTTP client
        this.httpClient = createHttpClient();
        
        // Register default HRM API endpoints
        registerDefaultEndpoints();
        
        logger.info("ApiTestFramework initialized with base URL: {}", this.config.getBaseUrl());
    }
    
    /**
     * Get singleton instance
     */
    public static ApiTestFramework getInstance() {
        return getInstance(null);
    }
    
    /**
     * Get singleton instance with configuration
     */
    public static ApiTestFramework getInstance(ApiConfiguration config) {
        if (instance == null) {
            synchronized (instanceLock) {
                if (instance == null) {
                    instance = new ApiTestFramework(config);
                }
            }
        }
        return instance;
    }
    
    /**
     * Execute API request synchronously
     */
    public <T> ApiResponse<T> executeRequest(String endpointName, Map<String, Object> pathParams, 
                                           Map<String, Object> queryParams, Object requestBody, 
                                           Class<T> responseType) {
        ApiEndpoint endpoint = endpoints.get(endpointName);
        if (endpoint == null) {
            throw new ApiTestException("Endpoint not found: " + endpointName);
        }
        
        try {
            // Build request
            HttpRequest request = buildHttpRequest(endpoint, pathParams, queryParams, requestBody);
            
            // Log request if enabled
            if (config.isEnableRequestLogging()) {
                requestLogger.logRequest(request, requestBody);
            }
            
            // Execute request with retry logic
            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = executeWithRetry(request);
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Parse response
            T parsedBody = parseResponseBody(response.body(), responseType);
            
            // Create API response
            ApiResponse<T> apiResponse = new ApiResponse<>(
                response.statusCode(),
                getReasonPhrase(response.statusCode()),
                response.headers().map(),
                parsedBody,
                responseTime,
                response.body()
            );
            
            // Log response if enabled
            if (config.isEnableRequestLogging()) {
                requestLogger.logResponse(apiResponse);
            }
            
            // Record performance metrics
            if (config.isEnablePerformanceMetrics()) {
                performanceMetrics.recordRequest(endpointName, responseTime, response.statusCode());
            }
            
            // Validate contract if enabled
            if (config.isEnableContractValidation()) {
                contractValidator.validateResponse(endpoint, apiResponse);
            }
            
            logger.debug("API request completed: {} - {} in {}ms", 
                endpointName, response.statusCode(), responseTime);
            
            return apiResponse;
            
        } catch (Exception e) {
            logger.error("Error executing API request: {}", endpointName, e);
            throw new ApiTestException("Failed to execute API request: " + endpointName, e);
        }
    }
    
    /**
     * Execute API request asynchronously
     */
    public <T> CompletableFuture<ApiResponse<T>> executeRequestAsync(String endpointName, 
                                                                   Map<String, Object> pathParams,
                                                                   Map<String, Object> queryParams, 
                                                                   Object requestBody, Class<T> responseType) {
        return CompletableFuture.supplyAsync(() -> 
            executeRequest(endpointName, pathParams, queryParams, requestBody, responseType));
    }
    
    /**
     * Register API endpoint
     */
    public void registerEndpoint(ApiEndpoint endpoint) {
        endpoints.put(endpoint.getName(), endpoint);
        logger.debug("Registered API endpoint: {} - {} {}", 
            endpoint.getName(), endpoint.getMethod(), endpoint.getPath());
    }
    
    /**
     * Get registered endpoint
     */
    public ApiEndpoint getEndpoint(String name) {
        return endpoints.get(name);
    }
    
    /**
     * Get all registered endpoints
     */
    public Map<String, ApiEndpoint> getAllEndpoints() {
        return new HashMap<>(endpoints);
    }
    
    /**
     * Set authentication token
     */
    public void setAuthToken(String token) {
        authManager.setToken(token);
        logger.debug("Authentication token updated");
    }
    
    /**
     * Clear authentication token
     */
    public void clearAuthToken() {
        authManager.clearToken();
        logger.debug("Authentication token cleared");
    }
    
    /**
     * Get performance metrics
     */
    public Map<String, Object> getPerformanceMetrics() {
        return performanceMetrics.getMetrics();
    }
    
    /**
     * Reset performance metrics
     */
    public void resetPerformanceMetrics() {
        performanceMetrics.reset();
        logger.debug("Performance metrics reset");
    }
    
    /**
     * Validate JSON schema
     */
    public boolean validateJsonSchema(String jsonData, String schemaPath) {
        return contractValidator.validateJsonSchema(jsonData, schemaPath);
    }
    
    /**
     * Get framework statistics
     */
    public Map<String, Object> getFrameworkStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("registeredEndpoints", endpoints.size());
        stats.put("baseUrl", config.getBaseUrl());
        stats.put("performanceMetrics", getPerformanceMetrics());
        stats.put("requestLoggingEnabled", config.isEnableRequestLogging());
        stats.put("contractValidationEnabled", config.isEnableContractValidation());
        stats.put("authenticationEnabled", authManager.hasToken());
        
        return stats;
    }
    
    // Private helper methods
    
    private HttpClient createHttpClient() {
        HttpClient.Builder clientBuilder = HttpClient.newBuilder()
            .connectTimeout(config.getConnectTimeout())
            .followRedirects(config.isFollowRedirects() ? 
                HttpClient.Redirect.NORMAL : HttpClient.Redirect.NEVER);
        
        // Configure SSL validation
        if (!config.isEnableSslValidation()) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{new TrustAllTrustManager()}, null);
                clientBuilder.sslContext(sslContext);
                logger.warn("SSL validation disabled - not recommended for production");
            } catch (Exception e) {
                logger.warn("Failed to disable SSL validation: {}", e.getMessage());
            }
        }
        
        return clientBuilder.build();
    }
    
    private HttpRequest buildHttpRequest(ApiEndpoint endpoint, Map<String, Object> pathParams,
                                       Map<String, Object> queryParams, Object requestBody) throws Exception {
        // Build URL
        String url = buildUrl(endpoint, pathParams, queryParams);
        
        // Build request
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(endpoint.getCustomTimeout() != null ? endpoint.getCustomTimeout() : config.getRequestTimeout());
        
        // Add headers
        for (Map.Entry<String, String> header : config.getDefaultHeaders().entrySet()) {
            requestBuilder.header(header.getKey(), header.getValue());
        }
        
        for (Map.Entry<String, String> header : endpoint.getHeaders().entrySet()) {
            requestBuilder.header(header.getKey(), header.getValue());
        }
        
        // Add authentication if required
        if (endpoint.requiresAuth() && authManager.hasToken()) {
            String authHeader = config.getAuthenticationScheme() + " " + authManager.getToken();
            requestBuilder.header("Authorization", authHeader);
        }
        
        // Set request body and method
        String bodyString = requestBody != null ? objectMapper.writeValueAsString(requestBody) : "";
        
        switch (endpoint.getMethod()) {
            case GET:
                requestBuilder.GET();
                break;
            case POST:
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(bodyString));
                break;
            case PUT:
                requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(bodyString));
                break;
            case DELETE:
                requestBuilder.DELETE();
                break;
            case PATCH:
                requestBuilder.method("PATCH", HttpRequest.BodyPublishers.ofString(bodyString));
                break;
            case HEAD:
                requestBuilder.method("HEAD", HttpRequest.BodyPublishers.noBody());
                break;
            case OPTIONS:
                requestBuilder.method("OPTIONS", HttpRequest.BodyPublishers.noBody());
                break;
        }
        
        return requestBuilder.build();
    }
    
    private String buildUrl(ApiEndpoint endpoint, Map<String, Object> pathParams, Map<String, Object> queryParams) {
        StringBuilder url = new StringBuilder(config.getBaseUrl());
        
        // Add endpoint path
        String path = endpoint.getPath();
        
        // Replace path parameters
        if (pathParams != null) {
            for (Map.Entry<String, Object> param : pathParams.entrySet()) {
                path = path.replace("{" + param.getKey() + "}", String.valueOf(param.getValue()));
            }
        }
        
        url.append(path);
        
        // Add query parameters
        Map<String, Object> allQueryParams = new HashMap<>(endpoint.getQueryParams());
        if (queryParams != null) {
            allQueryParams.putAll(queryParams);
        }
        
        if (!allQueryParams.isEmpty()) {
            url.append("?");
            boolean first = true;
            for (Map.Entry<String, Object> param : allQueryParams.entrySet()) {
                if (!first) {
                    url.append("&");
                }
                url.append(param.getKey()).append("=").append(param.getValue());
                first = false;
            }
        }
        
        return url.toString();
    }
    
    private HttpResponse<String> executeWithRetry(HttpRequest request) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= config.getMaxRetryAttempts(); attempt++) {
            try {
                return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                lastException = e;
                if (attempt < config.getMaxRetryAttempts()) {
                    logger.debug("Request attempt {} failed, retrying in {}ms", 
                        attempt, config.getRetryDelay().toMillis());
                    Thread.sleep(config.getRetryDelay().toMillis());
                }
            }
        }
        
        throw new ApiTestException("Request failed after " + config.getMaxRetryAttempts() + " attempts", lastException);
    }
    
    @SuppressWarnings("unchecked")
    private <T> T parseResponseBody(String responseBody, Class<T> responseType) throws Exception {
        if (responseType == String.class) {
            return (T) responseBody;
        } else if (responseType == JsonNode.class) {
            return (T) objectMapper.readTree(responseBody);
        } else {
            return objectMapper.readValue(responseBody, responseType);
        }
    }
    
    private String getReasonPhrase(int statusCode) {
        switch (statusCode) {
            case 200: return "OK";
            case 201: return "Created";
            case 204: return "No Content";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 500: return "Internal Server Error";
            default: return "Unknown";
        }
    }
    
    private void registerDefaultEndpoints() {
        // Employee endpoints
        registerEndpoint(new ApiEndpoint.Builder("getEmployees", "/employees", ApiEndpoint.HttpMethod.GET).build());
        registerEndpoint(new ApiEndpoint.Builder("getEmployee", "/employees/{id}", ApiEndpoint.HttpMethod.GET).build());
        registerEndpoint(new ApiEndpoint.Builder("createEmployee", "/employees", ApiEndpoint.HttpMethod.POST).build());
        registerEndpoint(new ApiEndpoint.Builder("updateEmployee", "/employees/{id}", ApiEndpoint.HttpMethod.PUT).build());
        registerEndpoint(new ApiEndpoint.Builder("deleteEmployee", "/employees/{id}", ApiEndpoint.HttpMethod.DELETE).build());
        
        // Department endpoints
        registerEndpoint(new ApiEndpoint.Builder("getDepartments", "/departments", ApiEndpoint.HttpMethod.GET).build());
        registerEndpoint(new ApiEndpoint.Builder("getDepartment", "/departments/{id}", ApiEndpoint.HttpMethod.GET).build());
        registerEndpoint(new ApiEndpoint.Builder("createDepartment", "/departments", ApiEndpoint.HttpMethod.POST).build());
        
        // Payroll endpoints
        registerEndpoint(new ApiEndpoint.Builder("getPayroll", "/payroll/{employeeId}", ApiEndpoint.HttpMethod.GET).build());
        registerEndpoint(new ApiEndpoint.Builder("createPayroll", "/payroll", ApiEndpoint.HttpMethod.POST).build());
        
        // Authentication endpoints
        registerEndpoint(new ApiEndpoint.Builder("login", "/auth/login", ApiEndpoint.HttpMethod.POST).build());
        registerEndpoint(new ApiEndpoint.Builder("logout", "/auth/logout", ApiEndpoint.HttpMethod.POST).build());
        registerEndpoint(new ApiEndpoint.Builder("refreshToken", "/auth/refresh", ApiEndpoint.HttpMethod.POST).build());
        
        logger.debug("Registered {} default API endpoints", endpoints.size());
    }
    
    /**
     * Trust all SSL certificates (for testing only)
     */
    private static class TrustAllTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {}
        
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {}
        
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
    
    /**
     * Custom exception for API testing
     */
    public static class ApiTestException extends RuntimeException {
        public ApiTestException(String message) {
            super(message);
        }
        
        public ApiTestException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}