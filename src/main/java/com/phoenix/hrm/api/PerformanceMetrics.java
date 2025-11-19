package com.phoenix.hrm.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Performance Metrics Component for API Testing Framework
 * 
 * Provides comprehensive performance tracking and analysis including:
 * - Response time metrics (min, max, average, percentiles)
 * - Request throughput and rate tracking
 * - Status code distribution analysis
 * - Endpoint-specific performance tracking
 * - Performance trend analysis over time
 * - Performance threshold monitoring and alerting
 * - Statistical analysis and reporting
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 5.0
 * @since Phase 5
 */
public class PerformanceMetrics {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMetrics.class);
    
    private final Map<String, EndpointMetrics> endpointMetrics;
    private final AtomicLong totalRequests;
    private final AtomicLong totalResponseTime;
    private final AtomicReference<LocalDateTime> firstRequestTime;
    private final AtomicReference<LocalDateTime> lastRequestTime;
    private final List<RequestMetric> requestHistory;
    private final Object historyLock = new Object();
    
    /**
     * Individual request metric
     */
    public static class RequestMetric {
        private final String endpoint;
        private final long responseTime;
        private final int statusCode;
        private final LocalDateTime timestamp;
        private final boolean success;
        
        public RequestMetric(String endpoint, long responseTime, int statusCode) {
            this.endpoint = endpoint;
            this.responseTime = responseTime;
            this.statusCode = statusCode;
            this.timestamp = LocalDateTime.now();
            this.success = statusCode >= 200 && statusCode < 400;
        }
        
        // Getters
        public String getEndpoint() { return endpoint; }
        public long getResponseTime() { return responseTime; }
        public int getStatusCode() { return statusCode; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public boolean isSuccess() { return success; }
        
        @Override
        public String toString() {
            return String.format("RequestMetric{endpoint='%s', time=%dms, status=%d, success=%b}", 
                endpoint, responseTime, statusCode, success);
        }
    }
    
    /**
     * Endpoint-specific metrics
     */
    public static class EndpointMetrics {
        private final String endpointName;
        private final AtomicLong requestCount;
        private final AtomicLong totalResponseTime;
        private final AtomicLong minResponseTime;
        private final AtomicLong maxResponseTime;
        private final AtomicLong successCount;
        private final AtomicLong errorCount;
        private final Map<Integer, AtomicLong> statusCodeDistribution;
        private final List<Long> responseTimes;
        private final Object responseTimesLock = new Object();
        private volatile LocalDateTime firstRequest;
        private volatile LocalDateTime lastRequest;
        
        public EndpointMetrics(String endpointName) {
            this.endpointName = endpointName;
            this.requestCount = new AtomicLong(0);
            this.totalResponseTime = new AtomicLong(0);
            this.minResponseTime = new AtomicLong(Long.MAX_VALUE);
            this.maxResponseTime = new AtomicLong(0);
            this.successCount = new AtomicLong(0);
            this.errorCount = new AtomicLong(0);
            this.statusCodeDistribution = new ConcurrentHashMap<>();
            this.responseTimes = new ArrayList<>();
        }
        
        public void recordRequest(long responseTime, int statusCode) {
            LocalDateTime now = LocalDateTime.now();
            
            // Update counters
            requestCount.incrementAndGet();
            totalResponseTime.addAndGet(responseTime);
            
            // Update min/max response times
            minResponseTime.updateAndGet(current -> Math.min(current, responseTime));
            maxResponseTime.updateAndGet(current -> Math.max(current, responseTime));
            
            // Update success/error counts
            if (statusCode >= 200 && statusCode < 400) {
                successCount.incrementAndGet();
            } else {
                errorCount.incrementAndGet();
            }
            
            // Update status code distribution
            statusCodeDistribution.computeIfAbsent(statusCode, k -> new AtomicLong(0)).incrementAndGet();
            
            // Update response times for percentile calculations
            synchronized (responseTimesLock) {
                responseTimes.add(responseTime);
                // Keep only the last 1000 response times for memory efficiency
                if (responseTimes.size() > 1000) {
                    responseTimes.remove(0);
                }
            }
            
            // Update timestamps
            if (firstRequest == null) {
                firstRequest = now;
            }
            lastRequest = now;
            
            logger.trace("Recorded request for {}: {}ms, status={}", endpointName, responseTime, statusCode);
        }
        
        public double getAverageResponseTime() {
            long count = requestCount.get();
            return count > 0 ? (double) totalResponseTime.get() / count : 0.0;
        }
        
        public double getSuccessRate() {
            long total = requestCount.get();
            return total > 0 ? (double) successCount.get() / total * 100 : 0.0;
        }
        
        public double getRequestsPerSecond() {
            if (firstRequest == null || lastRequest == null) {
                return 0.0;
            }
            
            long durationSeconds = java.time.Duration.between(firstRequest, lastRequest).getSeconds();
            return durationSeconds > 0 ? (double) requestCount.get() / durationSeconds : 0.0;
        }
        
        public long getPercentile(double percentile) {
            synchronized (responseTimesLock) {
                if (responseTimes.isEmpty()) {
                    return 0;
                }
                
                List<Long> sortedTimes = new ArrayList<>(responseTimes);
                sortedTimes.sort(Long::compareTo);
                
                int index = (int) Math.ceil(percentile / 100.0 * sortedTimes.size()) - 1;
                index = Math.max(0, Math.min(index, sortedTimes.size() - 1));
                
                return sortedTimes.get(index);
            }
        }
        
        // Getters
        public String getEndpointName() { return endpointName; }
        public long getRequestCount() { return requestCount.get(); }
        public long getTotalResponseTime() { return totalResponseTime.get(); }
        public long getMinResponseTime() { 
            long min = minResponseTime.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }
        public long getMaxResponseTime() { return maxResponseTime.get(); }
        public long getSuccessCount() { return successCount.get(); }
        public long getErrorCount() { return errorCount.get(); }
        public Map<Integer, Long> getStatusCodeDistribution() {
            Map<Integer, Long> distribution = new HashMap<>();
            statusCodeDistribution.forEach((key, value) -> distribution.put(key, value.get()));
            return distribution;
        }
        public LocalDateTime getFirstRequest() { return firstRequest; }
        public LocalDateTime getLastRequest() { return lastRequest; }
        
        @Override
        public String toString() {
            return String.format("EndpointMetrics{endpoint='%s', requests=%d, avgTime=%.2fms, successRate=%.1f%%}", 
                endpointName, getRequestCount(), getAverageResponseTime(), getSuccessRate());
        }
    }
    
    /**
     * Performance summary report
     */
    public static class PerformanceSummary {
        private final Map<String, Object> overallMetrics;
        private final Map<String, EndpointMetrics> endpointMetrics;
        private final List<String> performanceIssues;
        private final String generatedAt;
        
        public PerformanceSummary(Map<String, Object> overallMetrics, 
                                Map<String, EndpointMetrics> endpointMetrics,
                                List<String> performanceIssues) {
            this.overallMetrics = overallMetrics;
            this.endpointMetrics = endpointMetrics;
            this.performanceIssues = performanceIssues;
            this.generatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        
        // Getters
        public Map<String, Object> getOverallMetrics() { return overallMetrics; }
        public Map<String, EndpointMetrics> getEndpointMetrics() { return endpointMetrics; }
        public List<String> getPerformanceIssues() { return performanceIssues; }
        public String getGeneratedAt() { return generatedAt; }
    }
    
    /**
     * Constructor
     */
    public PerformanceMetrics() {
        this.endpointMetrics = new ConcurrentHashMap<>();
        this.totalRequests = new AtomicLong(0);
        this.totalResponseTime = new AtomicLong(0);
        this.firstRequestTime = new AtomicReference<>();
        this.lastRequestTime = new AtomicReference<>();
        this.requestHistory = new ArrayList<>();
        
        logger.debug("PerformanceMetrics initialized");
    }
    
    /**
     * Record a request for performance tracking
     */
    public void recordRequest(String endpointName, long responseTime, int statusCode) {
        LocalDateTime now = LocalDateTime.now();
        
        // Update overall metrics
        totalRequests.incrementAndGet();
        totalResponseTime.addAndGet(responseTime);
        firstRequestTime.compareAndSet(null, now);
        lastRequestTime.set(now);
        
        // Update endpoint-specific metrics
        EndpointMetrics metrics = endpointMetrics.computeIfAbsent(endpointName, EndpointMetrics::new);
        metrics.recordRequest(responseTime, statusCode);
        
        // Add to request history
        RequestMetric requestMetric = new RequestMetric(endpointName, responseTime, statusCode);
        synchronized (historyLock) {
            requestHistory.add(requestMetric);
            // Keep only the last 10,000 requests for memory efficiency
            if (requestHistory.size() > 10000) {
                requestHistory.remove(0);
            }
        }
        
        logger.trace("Recorded performance metric: {} - {}ms, status={}", endpointName, responseTime, statusCode);
    }
    
    /**
     * Get overall performance metrics
     */
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        long totalReqs = totalRequests.get();
        long totalTime = totalResponseTime.get();
        
        // Overall statistics
        metrics.put("totalRequests", totalReqs);
        metrics.put("averageResponseTime", totalReqs > 0 ? (double) totalTime / totalReqs : 0.0);
        
        // Time range
        LocalDateTime first = firstRequestTime.get();
        LocalDateTime last = lastRequestTime.get();
        
        if (first != null && last != null) {
            metrics.put("firstRequest", first.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            metrics.put("lastRequest", last.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            long durationSeconds = java.time.Duration.between(first, last).getSeconds();
            metrics.put("durationSeconds", durationSeconds);
            metrics.put("requestsPerSecond", durationSeconds > 0 ? (double) totalReqs / durationSeconds : 0.0);
        }
        
        // Overall success/error statistics
        long totalSuccess = 0;
        long totalErrors = 0;
        
        for (EndpointMetrics endpointMetric : endpointMetrics.values()) {
            totalSuccess += endpointMetric.getSuccessCount();
            totalErrors += endpointMetric.getErrorCount();
        }
        
        metrics.put("totalSuccessfulRequests", totalSuccess);
        metrics.put("totalFailedRequests", totalErrors);
        metrics.put("overallSuccessRate", totalReqs > 0 ? (double) totalSuccess / totalReqs * 100 : 0.0);
        
        // Endpoint count
        metrics.put("numberOfEndpoints", endpointMetrics.size());
        
        return metrics;
    }
    
    /**
     * Get metrics for specific endpoint
     */
    public EndpointMetrics getEndpointMetrics(String endpointName) {
        return endpointMetrics.get(endpointName);
    }
    
    /**
     * Get all endpoint metrics
     */
    public Map<String, EndpointMetrics> getAllEndpointMetrics() {
        return new HashMap<>(endpointMetrics);
    }
    
    /**
     * Get performance summary report
     */
    public PerformanceSummary getPerformanceSummary() {
        Map<String, Object> overallMetrics = getMetrics();
        Map<String, EndpointMetrics> allEndpointMetrics = getAllEndpointMetrics();
        List<String> performanceIssues = analyzePerformanceIssues();
        
        return new PerformanceSummary(overallMetrics, allEndpointMetrics, performanceIssues);
    }
    
    /**
     * Get top slowest endpoints
     */
    public List<EndpointMetrics> getSlowestEndpoints(int limit) {
        return endpointMetrics.values().stream()
            .filter(metrics -> metrics.getRequestCount() > 0)
            .sorted(Comparator.comparingDouble(EndpointMetrics::getAverageResponseTime).reversed())
            .limit(limit)
            .toList();
    }
    
    /**
     * Get endpoints with highest error rates
     */
    public List<EndpointMetrics> getHighestErrorRateEndpoints(int limit) {
        return endpointMetrics.values().stream()
            .filter(metrics -> metrics.getRequestCount() > 0)
            .sorted(Comparator.comparingDouble((EndpointMetrics m) -> 100.0 - m.getSuccessRate()).reversed())
            .limit(limit)
            .toList();
    }
    
    /**
     * Get request history
     */
    public List<RequestMetric> getRequestHistory() {
        synchronized (historyLock) {
            return new ArrayList<>(requestHistory);
        }
    }
    
    /**
     * Get request history for specific endpoint
     */
    public List<RequestMetric> getRequestHistory(String endpointName) {
        synchronized (historyLock) {
            return requestHistory.stream()
                .filter(metric -> endpointName.equals(metric.getEndpoint()))
                .toList();
        }
    }
    
    /**
     * Get request history within time range
     */
    public List<RequestMetric> getRequestHistory(LocalDateTime start, LocalDateTime end) {
        synchronized (historyLock) {
            return requestHistory.stream()
                .filter(metric -> !metric.getTimestamp().isBefore(start) && 
                                !metric.getTimestamp().isAfter(end))
                .toList();
        }
    }
    
    /**
     * Calculate percentile response time across all endpoints
     */
    public long calculateOverallPercentile(double percentile) {
        synchronized (historyLock) {
            if (requestHistory.isEmpty()) {
                return 0;
            }
            
            List<Long> allResponseTimes = requestHistory.stream()
                .map(RequestMetric::getResponseTime)
                .sorted()
                .toList();
            
            int index = (int) Math.ceil(percentile / 100.0 * allResponseTimes.size()) - 1;
            index = Math.max(0, Math.min(index, allResponseTimes.size() - 1));
            
            return allResponseTimes.get(index);
        }
    }
    
    /**
     * Reset all performance metrics
     */
    public void reset() {
        endpointMetrics.clear();
        totalRequests.set(0);
        totalResponseTime.set(0);
        firstRequestTime.set(null);
        lastRequestTime.set(null);
        
        synchronized (historyLock) {
            requestHistory.clear();
        }
        
        logger.debug("Performance metrics reset");
    }
    
    /**
     * Export metrics to JSON
     */
    public String exportMetricsAsJson() {
        try {
            Map<String, Object> exportData = new HashMap<>();
            exportData.put("summary", getPerformanceSummary());
            exportData.put("overallMetrics", getMetrics());
            exportData.put("endpointMetrics", getAllEndpointMetrics());
            exportData.put("exportTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportData);
            
        } catch (Exception e) {
            logger.error("Error exporting metrics as JSON: {}", e.getMessage());
            return "{}";
        }
    }
    
    // Private helper methods
    
    private List<String> analyzePerformanceIssues() {
        List<String> issues = new ArrayList<>();
        
        // Define performance thresholds
        final double SLOW_RESPONSE_THRESHOLD = 5000.0; // 5 seconds
        final double LOW_SUCCESS_RATE_THRESHOLD = 95.0; // 95%
        final double HIGH_ERROR_RATE_THRESHOLD = 5.0; // 5%
        
        for (EndpointMetrics metrics : endpointMetrics.values()) {
            String endpointName = metrics.getEndpointName();
            
            // Check for slow average response time
            if (metrics.getAverageResponseTime() > SLOW_RESPONSE_THRESHOLD) {
                issues.add(String.format("Endpoint '%s' has slow average response time: %.2fms", 
                    endpointName, metrics.getAverageResponseTime()));
            }
            
            // Check for low success rate
            if (metrics.getSuccessRate() < LOW_SUCCESS_RATE_THRESHOLD) {
                issues.add(String.format("Endpoint '%s' has low success rate: %.1f%%", 
                    endpointName, metrics.getSuccessRate()));
            }
            
            // Check for high error rate
            double errorRate = 100.0 - metrics.getSuccessRate();
            if (errorRate > HIGH_ERROR_RATE_THRESHOLD) {
                issues.add(String.format("Endpoint '%s' has high error rate: %.1f%%", 
                    endpointName, errorRate));
            }
            
            // Check for high maximum response time
            if (metrics.getMaxResponseTime() > SLOW_RESPONSE_THRESHOLD * 2) {
                issues.add(String.format("Endpoint '%s' has very high maximum response time: %dms", 
                    endpointName, metrics.getMaxResponseTime()));
            }
        }
        
        // Check overall performance
        Map<String, Object> overallMetrics = getMetrics();
        Double overallAvgTime = (Double) overallMetrics.get("averageResponseTime");
        Double overallSuccessRate = (Double) overallMetrics.get("overallSuccessRate");
        
        if (overallAvgTime != null && overallAvgTime > SLOW_RESPONSE_THRESHOLD) {
            issues.add(String.format("Overall average response time is slow: %.2fms", overallAvgTime));
        }
        
        if (overallSuccessRate != null && overallSuccessRate < LOW_SUCCESS_RATE_THRESHOLD) {
            issues.add(String.format("Overall success rate is low: %.1f%%", overallSuccessRate));
        }
        
        return issues;
    }
}