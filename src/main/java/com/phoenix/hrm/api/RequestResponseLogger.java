package com.phoenix.hrm.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Request/Response Logger for API Testing Framework
 * 
 * Provides comprehensive logging capabilities for API requests and responses including:
 * - Detailed request logging with headers, body, and timing
 * - Response logging with status codes, headers, and body content
 * - Configurable log levels and filtering
 * - Request/Response correlation and tracking
 * - Performance metrics integration
 * - Log file management and rotation
 * - Sensitive data masking in logs
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 5.0
 * @since Phase 5
 */
public class RequestResponseLogger {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestResponseLogger.class);
    
    private final ApiTestFramework.ApiConfiguration config;
    private final Map<String, RequestLogEntry> requestLog;
    private final AtomicLong requestCounter;
    private final Set<String> sensitiveHeaders;
    private final Set<String> sensitiveBodyFields;
    
    /**
     * Request log entry
     */
    public static class RequestLogEntry {
        private final String requestId;
        private final LocalDateTime timestamp;
        private final String method;
        private final String url;
        private final Map<String, String> headers;
        private final String body;
        private final long startTime;
        private long endTime;
        private int responseStatus;
        private String responseBody;
        private Map<String, List<String>> responseHeaders;
        private long responseTime;
        
        public RequestLogEntry(String requestId, String method, String url, 
                             Map<String, String> headers, String body) {
            this.requestId = requestId;
            this.timestamp = LocalDateTime.now();
            this.method = method;
            this.url = url;
            this.headers = new HashMap<>(headers);
            this.body = body;
            this.startTime = System.currentTimeMillis();
        }
        
        public void setResponse(int status, String responseBody, Map<String, List<String>> responseHeaders) {
            this.endTime = System.currentTimeMillis();
            this.responseStatus = status;
            this.responseBody = responseBody;
            this.responseHeaders = new HashMap<>(responseHeaders);
            this.responseTime = endTime - startTime;
        }
        
        // Getters
        public String getRequestId() { return requestId; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getMethod() { return method; }
        public String getUrl() { return url; }
        public Map<String, String> getHeaders() { return headers; }
        public String getBody() { return body; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public int getResponseStatus() { return responseStatus; }
        public String getResponseBody() { return responseBody; }
        public Map<String, List<String>> getResponseHeaders() { return responseHeaders; }
        public long getResponseTime() { return responseTime; }
        
        @Override
        public String toString() {
            return String.format("RequestLogEntry{id='%s', method='%s', url='%s', status=%d, time=%dms}", 
                requestId, method, url, responseStatus, responseTime);
        }
    }
    
    /**
     * Constructor
     */
    public RequestResponseLogger(ApiTestFramework.ApiConfiguration config) {
        this.config = config;
        this.requestLog = new ConcurrentHashMap<>();
        this.requestCounter = new AtomicLong(0);
        this.sensitiveHeaders = new HashSet<>();
        this.sensitiveBodyFields = new HashSet<>();
        
        initializeSensitiveFields();
    }
    
    /**
     * Log HTTP request
     */
    public String logRequest(HttpRequest request, Object requestBody) {
        if (!config.isEnableRequestLogging()) {
            return null;
        }
        
        String requestId = generateRequestId();
        
        try {
            // Extract request details
            String method = request.method();
            String url = request.uri().toString();
            Map<String, String> headers = extractHeaders(request);
            String body = requestBody != null ? maskSensitiveData(requestBody.toString()) : "";
            
            // Create log entry
            RequestLogEntry logEntry = new RequestLogEntry(requestId, method, url, headers, body);
            requestLog.put(requestId, logEntry);
            
            // Log the request
            logger.info("API Request [{}]: {} {}", requestId, method, url);
            logger.debug("Request Headers [{}]: {}", requestId, headers);
            
            if (body != null && !body.isEmpty()) {
                logger.debug("Request Body [{}]: {}", requestId, body);
            }
            
            return requestId;
            
        } catch (Exception e) {
            logger.warn("Error logging request: {}", e.getMessage());
            return requestId;
        }
    }
    
    /**
     * Log API response
     */
    public void logResponse(ApiTestFramework.ApiResponse<?> response) {
        if (!config.isEnableRequestLogging()) {
            return;
        }
        
        try {
            // Find the corresponding request log entry
            String requestId = findRequestIdByTimestamp(response.getTimestamp());
            
            if (requestId != null) {
                RequestLogEntry logEntry = requestLog.get(requestId);
                if (logEntry != null) {
                    String maskedBody = response.getRawResponse() != null ? 
                        maskSensitiveData(response.getRawResponse()) : "";
                    
                    logEntry.setResponse(response.getStatusCode(), maskedBody, response.getHeaders());
                    
                    // Log the response
                    logger.info("API Response [{}]: {} {} - {} in {}ms", 
                        requestId, logEntry.getMethod(), logEntry.getUrl(), 
                        response.getStatusCode(), response.getResponseTime());
                    
                    logger.debug("Response Headers [{}]: {}", requestId, response.getHeaders());
                    
                    if (maskedBody != null && !maskedBody.isEmpty() && maskedBody.length() < 1000) {
                        logger.debug("Response Body [{}]: {}", requestId, maskedBody);
                    } else if (maskedBody != null && maskedBody.length() >= 1000) {
                        logger.debug("Response Body [{}]: {} (truncated, length: {})", 
                            requestId, maskedBody.substring(0, 1000), maskedBody.length());
                    }
                }
            }
            
        } catch (Exception e) {
            logger.warn("Error logging response: {}", e.getMessage());
        }
    }
    
    /**
     * Get request log entry by ID
     */
    public RequestLogEntry getRequestLogEntry(String requestId) {
        return requestLog.get(requestId);
    }
    
    /**
     * Get all request log entries
     */
    public List<RequestLogEntry> getAllRequestLogEntries() {
        return new ArrayList<>(requestLog.values());
    }
    
    /**
     * Get request log entries by time range
     */
    public List<RequestLogEntry> getRequestLogEntriesByTimeRange(LocalDateTime start, LocalDateTime end) {
        return requestLog.values().stream()
            .filter(entry -> !entry.getTimestamp().isBefore(start) && !entry.getTimestamp().isAfter(end))
            .sorted(Comparator.comparing(RequestLogEntry::getTimestamp))
            .toList();
    }
    
    /**
     * Get request log entries by status code
     */
    public List<RequestLogEntry> getRequestLogEntriesByStatus(int statusCode) {
        return requestLog.values().stream()
            .filter(entry -> entry.getResponseStatus() == statusCode)
            .sorted(Comparator.comparing(RequestLogEntry::getTimestamp))
            .toList();
    }
    
    /**
     * Get failed requests (status codes >= 400)
     */
    public List<RequestLogEntry> getFailedRequests() {
        return requestLog.values().stream()
            .filter(entry -> entry.getResponseStatus() >= 400)
            .sorted(Comparator.comparing(RequestLogEntry::getTimestamp))
            .toList();
    }
    
    /**
     * Generate log summary report
     */
    public Map<String, Object> generateLogSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        List<RequestLogEntry> entries = getAllRequestLogEntries();
        
        // Basic statistics
        summary.put("totalRequests", entries.size());
        
        // Status code distribution
        Map<String, Integer> statusDistribution = new HashMap<>();
        Map<String, Integer> methodDistribution = new HashMap<>();
        long totalResponseTime = 0;
        int successCount = 0;
        int errorCount = 0;
        
        for (RequestLogEntry entry : entries) {
            // Status distribution
            String statusRange = getStatusRange(entry.getResponseStatus());
            statusDistribution.merge(statusRange, 1, Integer::sum);
            
            // Method distribution
            methodDistribution.merge(entry.getMethod(), 1, Integer::sum);
            
            // Performance metrics
            totalResponseTime += entry.getResponseTime();
            
            // Success/Error counts
            if (entry.getResponseStatus() >= 200 && entry.getResponseStatus() < 400) {
                successCount++;
            } else if (entry.getResponseStatus() >= 400) {
                errorCount++;
            }
        }
        
        summary.put("statusDistribution", statusDistribution);
        summary.put("methodDistribution", methodDistribution);
        summary.put("successCount", successCount);
        summary.put("errorCount", errorCount);
        summary.put("successRate", entries.isEmpty() ? 0.0 : (double) successCount / entries.size() * 100);
        summary.put("averageResponseTime", entries.isEmpty() ? 0.0 : (double) totalResponseTime / entries.size());
        
        // Time range
        if (!entries.isEmpty()) {
            LocalDateTime earliest = entries.stream()
                .map(RequestLogEntry::getTimestamp)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
            
            LocalDateTime latest = entries.stream()
                .map(RequestLogEntry::getTimestamp)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
            
            summary.put("timeRange", Map.of(
                "start", earliest.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "end", latest.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ));
        }
        
        return summary;
    }
    
    /**
     * Clear request logs
     */
    public void clearLogs() {
        int size = requestLog.size();
        requestLog.clear();
        requestCounter.set(0);
        logger.debug("Cleared {} request log entries", size);
    }
    
    /**
     * Clear logs older than specified duration
     */
    public void clearOldLogs(long maxAgeMillis) {
        LocalDateTime cutoff = LocalDateTime.now().minusNanos(maxAgeMillis * 1_000_000);
        
        int removedCount = 0;
        Iterator<Map.Entry<String, RequestLogEntry>> iterator = requestLog.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, RequestLogEntry> entry = iterator.next();
            if (entry.getValue().getTimestamp().isBefore(cutoff)) {
                iterator.remove();
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            logger.debug("Removed {} old request log entries", removedCount);
        }
    }
    
    /**
     * Add sensitive header pattern
     */
    public void addSensitiveHeader(String headerName) {
        sensitiveHeaders.add(headerName.toLowerCase());
    }
    
    /**
     * Add sensitive body field pattern
     */
    public void addSensitiveBodyField(String fieldName) {
        sensitiveBodyFields.add(fieldName.toLowerCase());
    }
    
    /**
     * Export logs to JSON format
     */
    public String exportLogsAsJson() {
        try {
            Map<String, Object> exportData = new HashMap<>();
            exportData.put("summary", generateLogSummary());
            exportData.put("entries", getAllRequestLogEntries());
            exportData.put("exportTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // Use Jackson ObjectMapper for JSON serialization
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportData);
            
        } catch (Exception e) {
            logger.error("Error exporting logs as JSON: {}", e.getMessage());
            return "{}";
        }
    }
    
    // Private helper methods
    
    private void initializeSensitiveFields() {
        // Default sensitive headers
        sensitiveHeaders.addAll(Arrays.asList(
            "authorization", "cookie", "set-cookie", "x-api-key", "x-auth-token"
        ));
        
        // Default sensitive body fields
        sensitiveBodyFields.addAll(Arrays.asList(
            "password", "token", "secret", "key", "ssn", "creditcard", "bankaccount"
        ));
    }
    
    private String generateRequestId() {
        return String.format("REQ-%06d", requestCounter.incrementAndGet());
    }
    
    private Map<String, String> extractHeaders(HttpRequest request) {
        Map<String, String> headers = new HashMap<>();
        
        request.headers().map().forEach((key, values) -> {
            if (!values.isEmpty()) {
                String value = String.join(", ", values);
                headers.put(key, isSensitiveHeader(key) ? maskValue(value) : value);
            }
        });
        
        return headers;
    }
    
    private boolean isSensitiveHeader(String headerName) {
        String lowerName = headerName.toLowerCase();
        return sensitiveHeaders.stream().anyMatch(lowerName::contains);
    }
    
    private String maskSensitiveData(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        
        String maskedData = data;
        
        // Mask sensitive body fields
        for (String field : sensitiveBodyFields) {
            maskedData = maskedData.replaceAll(
                "(?i)\"" + field + "\"\\s*:\\s*\"[^\"]*\"",
                "\"" + field + "\":\"***MASKED***\""
            );
        }
        
        return maskedData;
    }
    
    private String maskValue(String value) {
        if (value == null || value.length() <= 8) {
            return "***";
        }
        return value.substring(0, 4) + "***" + value.substring(value.length() - 2);
    }
    
    private String findRequestIdByTimestamp(LocalDateTime responseTimestamp) {
        // Find the most recent request entry that doesn't have a response yet
        // This is a simplified approach - in production, you'd want better correlation
        return requestLog.entrySet().stream()
            .filter(entry -> entry.getValue().getEndTime() == 0)
            .min(Comparator.comparingLong(entry -> 
                Math.abs(entry.getValue().getTimestamp().getNano() - responseTimestamp.getNano())))
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    private String getStatusRange(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) {
            return "2xx Success";
        } else if (statusCode >= 300 && statusCode < 400) {
            return "3xx Redirection";
        } else if (statusCode >= 400 && statusCode < 500) {
            return "4xx Client Error";
        } else if (statusCode >= 500) {
            return "5xx Server Error";
        } else {
            return "Other";
        }
    }
}