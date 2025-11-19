package com.phoenix.hrm.performance;

import com.phoenix.hrm.core.config.ConfigManager;
import com.phoenix.hrm.core.reporting.ExtentReportManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance Test Manager for Phoenix HRM Test Framework
 * 
 * Provides comprehensive performance testing capabilities including:
 * - Load testing with configurable concurrent users
 * - Performance metrics collection and analysis
 * - Response time monitoring and thresholds
 * - Throughput and error rate calculations
 * - Memory and resource usage tracking
 * - Performance baseline management
 * 
 * @author Phoenix HRM Test Team
 */
public class PerformanceTestManager {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceTestManager.class);
    
    // Configuration
    private static final ConfigManager config = ConfigManager.getInstance();
    
    // Performance thresholds (configurable)
    private static final long DEFAULT_RESPONSE_TIME_THRESHOLD = 3000; // 3 seconds
    private static final double DEFAULT_ERROR_RATE_THRESHOLD = 5.0; // 5%
    private static final long DEFAULT_THROUGHPUT_THRESHOLD = 10; // 10 requests per second
    
    // Performance metrics storage
    private static final ConcurrentHashMap<String, List<Long>> responseTimeData = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicInteger> errorCounts = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicLong> totalExecutionTime = new ConcurrentHashMap<>();
    
    // Test execution tracking
    private static final ConcurrentHashMap<String, Instant> testStartTimes = new ConcurrentHashMap<>();
    private static final List<PerformanceTestResult> testResults = Collections.synchronizedList(new ArrayList<>());
    
    /**
     * Executes performance test with specified concurrent users
     */
    public static PerformanceTestResult executeLoadTest(String testName, Runnable testScenario, 
                                                      int concurrentUsers, int durationMinutes) {
        logger.info("Starting load test: {} with {} concurrent users for {} minutes", 
            testName, concurrentUsers, durationMinutes);
        
        Instant testStartTime = Instant.now();
        testStartTimes.put(testName, testStartTime);
        
        // Initialize metrics
        responseTimeData.put(testName, Collections.synchronizedList(new ArrayList<>()));
        requestCounts.put(testName, new AtomicInteger(0));
        errorCounts.put(testName, new AtomicInteger(0));
        totalExecutionTime.put(testName, new AtomicLong(0));
        
        // Create thread pool for concurrent execution
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        List<Future<?>> futures = new ArrayList<>();
        
        // Schedule test execution
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        AtomicInteger completedTests = new AtomicInteger(0);
        
        try {
            // Submit concurrent user tasks
            for (int i = 0; i < concurrentUsers; i++) {
                final int userId = i + 1;
                Future<?> future = executor.submit(() -> {
                    executeUserScenario(testName, testScenario, userId, durationMinutes);
                    completedTests.incrementAndGet();
                });
                futures.add(future);
            }
            
            // Monitor progress
            monitorTestProgress(testName, concurrentUsers, durationMinutes, completedTests);
            
            // Wait for all threads to complete or timeout
            long timeoutMillis = (durationMinutes + 1) * 60 * 1000; // Add 1 minute buffer
            for (Future<?> future : futures) {
                try {
                    future.get(timeoutMillis, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    logger.warn("User scenario timed out, cancelling...");
                    future.cancel(true);
                } catch (Exception e) {
                    logger.error("User scenario failed: {}", e.getMessage());
                    errorCounts.get(testName).incrementAndGet();
                }
            }
            
        } finally {
            executor.shutdown();
            scheduler.shutdown();
            
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Calculate and return results
        PerformanceTestResult result = calculatePerformanceMetrics(testName, concurrentUsers, 
            Duration.between(testStartTime, Instant.now()));
        
        testResults.add(result);
        logPerformanceResults(result);
        
        return result;
    }
    
    /**
     * Executes individual user scenario
     */
    private static void executeUserScenario(String testName, Runnable testScenario, 
                                          int userId, int durationMinutes) {
        logger.debug("Starting user scenario: {} - User {}", testName, userId);
        
        Instant endTime = Instant.now().plus(Duration.ofMinutes(durationMinutes));
        int iterationCount = 0;
        
        while (Instant.now().isBefore(endTime)) {
            Instant iterationStart = Instant.now();
            
            try {
                // Execute test scenario
                testScenario.run();
                
                // Record successful execution
                long responseTime = Duration.between(iterationStart, Instant.now()).toMillis();
                responseTimeData.get(testName).add(responseTime);
                requestCounts.get(testName).incrementAndGet();
                totalExecutionTime.get(testName).addAndGet(responseTime);
                
                iterationCount++;
                
                // Add small delay to prevent overwhelming
                Thread.sleep(100);
                
            } catch (Exception e) {
                logger.error("User {} scenario iteration failed: {}", userId, e.getMessage());
                errorCounts.get(testName).incrementAndGet();
                
                // Add delay after error to prevent cascading failures
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        logger.debug("User {} completed {} iterations", userId, iterationCount);
    }
    
    /**
     * Monitors test progress and provides real-time updates
     */
    private static void monitorTestProgress(String testName, int concurrentUsers, 
                                          int durationMinutes, AtomicInteger completedTests) {
        ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);
        
        monitor.scheduleAtFixedRate(() -> {
            int requests = requestCounts.get(testName).get();
            int errors = errorCounts.get(testName).get();
            double errorRate = requests > 0 ? (double) errors / requests * 100 : 0;
            
            logger.info("Progress - Test: {}, Requests: {}, Errors: {}, Error Rate: {:.2f}%, " +
                       "Completed Users: {}/{}", 
                testName, requests, errors, errorRate, completedTests.get(), concurrentUsers);
            
        }, 30, 30, TimeUnit.SECONDS); // Report every 30 seconds
        
        // Stop monitoring after test duration + buffer
        monitor.schedule(() -> monitor.shutdown(), (durationMinutes + 1) * 60, TimeUnit.SECONDS);
    }
    
    /**
     * Calculates comprehensive performance metrics
     */
    private static PerformanceTestResult calculatePerformanceMetrics(String testName, 
                                                                   int concurrentUsers, 
                                                                   Duration totalDuration) {
        List<Long> responseTimes = responseTimeData.get(testName);
        int totalRequests = requestCounts.get(testName).get();
        int totalErrors = errorCounts.get(testName).get();
        
        if (responseTimes.isEmpty()) {
            return new PerformanceTestResult(testName, concurrentUsers, totalDuration, 
                0, 0, 0, 0, 0, 0, 100.0, 0, false);
        }
        
        // Sort response times for percentile calculations
        List<Long> sortedTimes = new ArrayList<>(responseTimes);
        Collections.sort(sortedTimes);
        
        // Calculate metrics
        long minResponseTime = sortedTimes.get(0);
        long maxResponseTime = sortedTimes.get(sortedTimes.size() - 1);
        double avgResponseTime = sortedTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        
        // Calculate percentiles
        long p50 = getPercentile(sortedTimes, 50);
        long p90 = getPercentile(sortedTimes, 90);
        long p95 = getPercentile(sortedTimes, 95);
        long p99 = getPercentile(sortedTimes, 99);
        
        // Calculate throughput (requests per second)
        double throughput = totalRequests / (double) totalDuration.getSeconds();
        
        // Calculate error rate
        double errorRate = totalRequests > 0 ? (double) totalErrors / totalRequests * 100 : 0;
        
        // Determine if performance meets thresholds
        boolean meetsThresholds = checkPerformanceThresholds(avgResponseTime, errorRate, throughput);
        
        return new PerformanceTestResult(testName, concurrentUsers, totalDuration, 
            totalRequests, totalErrors, minResponseTime, maxResponseTime, 
            (long) avgResponseTime, p95, errorRate, throughput, meetsThresholds)
            .withPercentiles(p50, p90, p95, p99);
    }
    
    /**
     * Calculates percentile values from sorted response times
     */
    private static long getPercentile(List<Long> sortedTimes, int percentile) {
        if (sortedTimes.isEmpty()) return 0;
        
        int index = (int) Math.ceil(percentile / 100.0 * sortedTimes.size()) - 1;
        index = Math.max(0, Math.min(index, sortedTimes.size() - 1));
        
        return sortedTimes.get(index);
    }
    
    /**
     * Checks if performance metrics meet defined thresholds
     */
    private static boolean checkPerformanceThresholds(double avgResponseTime, 
                                                    double errorRate, double throughput) {
        long responseThreshold = Long.parseLong(config.getProperty("performance.response.time.threshold", 
            String.valueOf(DEFAULT_RESPONSE_TIME_THRESHOLD)));
        double errorThreshold = Double.parseDouble(config.getProperty("performance.error.rate.threshold", 
            String.valueOf(DEFAULT_ERROR_RATE_THRESHOLD)));
        double throughputThreshold = Double.parseDouble(config.getProperty("performance.throughput.threshold", 
            String.valueOf(DEFAULT_THROUGHPUT_THRESHOLD)));
        
        return avgResponseTime <= responseThreshold && 
               errorRate <= errorThreshold && 
               throughput >= throughputThreshold;
    }
    
    /**
     * Logs performance results to logger and ExtentReports
     */
    private static void logPerformanceResults(PerformanceTestResult result) {
        logger.info("=== PERFORMANCE TEST RESULTS ===");
        logger.info("Test: {}", result.getTestName());
        logger.info("Concurrent Users: {}", result.getConcurrentUsers());
        logger.info("Duration: {} minutes", result.getDuration().toMinutes());
        logger.info("Total Requests: {}", result.getTotalRequests());
        logger.info("Total Errors: {}", result.getTotalErrors());
        logger.info("Error Rate: {:.2f}%", result.getErrorRate());
        logger.info("Throughput: {:.2f} requests/sec", result.getThroughput());
        logger.info("Response Times - Min: {}ms, Max: {}ms, Avg: {}ms, 95th: {}ms", 
            result.getMinResponseTime(), result.getMaxResponseTime(), 
            result.getAvgResponseTime(), result.getP95ResponseTime());
        logger.info("Meets Thresholds: {}", result.meetsPerformanceThresholds());
        
        // Log to ExtentReports
        ExtentReportManager.createTest("🚀 " + result.getTestName() + " Performance Results", 
            "Performance test results with " + result.getConcurrentUsers() + " concurrent users");
        
        ExtentReportManager.logInfo("=== PERFORMANCE METRICS ===");
        ExtentReportManager.logInfo("Concurrent Users: " + result.getConcurrentUsers());
        ExtentReportManager.logInfo("Test Duration: " + result.getDuration().toMinutes() + " minutes");
        ExtentReportManager.logInfo("Total Requests: " + result.getTotalRequests());
        ExtentReportManager.logInfo("Successful Requests: " + (result.getTotalRequests() - result.getTotalErrors()));
        ExtentReportManager.logInfo("Failed Requests: " + result.getTotalErrors());
        ExtentReportManager.logInfo(String.format("Error Rate: %.2f%%", result.getErrorRate()));
        ExtentReportManager.logInfo(String.format("Throughput: %.2f requests/second", result.getThroughput()));
        
        ExtentReportManager.logInfo("=== RESPONSE TIME ANALYSIS ===");
        ExtentReportManager.logInfo("Minimum Response Time: " + result.getMinResponseTime() + "ms");
        ExtentReportManager.logInfo("Maximum Response Time: " + result.getMaxResponseTime() + "ms");
        ExtentReportManager.logInfo("Average Response Time: " + result.getAvgResponseTime() + "ms");
        ExtentReportManager.logInfo("95th Percentile: " + result.getP95ResponseTime() + "ms");
        
        // Performance threshold validation
        if (result.meetsPerformanceThresholds()) {
            ExtentReportManager.logPass("✅ Performance thresholds met - Test PASSED");
        } else {
            ExtentReportManager.logFail("❌ Performance thresholds exceeded - Test FAILED");
            
            // Add specific threshold failures
            long responseThreshold = Long.parseLong(config.getProperty("performance.response.time.threshold", 
                String.valueOf(DEFAULT_RESPONSE_TIME_THRESHOLD)));
            double errorThreshold = Double.parseDouble(config.getProperty("performance.error.rate.threshold", 
                String.valueOf(DEFAULT_ERROR_RATE_THRESHOLD)));
            
            if (result.getAvgResponseTime() > responseThreshold) {
                ExtentReportManager.logWarning("Response time threshold exceeded: " + 
                    result.getAvgResponseTime() + "ms > " + responseThreshold + "ms");
            }
            
            if (result.getErrorRate() > errorThreshold) {
                ExtentReportManager.logWarning("Error rate threshold exceeded: " + 
                    String.format("%.2f%% > %.2f%%", result.getErrorRate(), errorThreshold));
            }
        }
    }
    
    /**
     * Executes stress test with gradually increasing load
     */
    public static List<PerformanceTestResult> executeStressTest(String testName, Runnable testScenario,
                                                              int[] userCounts, int durationPerStep) {
        logger.info("Starting stress test: {} with user counts: {}", testName, Arrays.toString(userCounts));
        
        List<PerformanceTestResult> stressResults = new ArrayList<>();
        
        for (int userCount : userCounts) {
            logger.info("Executing stress test step with {} users", userCount);
            
            PerformanceTestResult stepResult = executeLoadTest(
                testName + "_Stress_" + userCount + "Users", 
                testScenario, 
                userCount, 
                durationPerStep
            );
            
            stressResults.add(stepResult);
            
            // Add cool-down period between steps
            try {
                Thread.sleep(30000); // 30 seconds cool-down
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // Generate stress test summary
        generateStressTestSummary(testName, stressResults);
        
        return stressResults;
    }
    
    /**
     * Generates comprehensive stress test summary
     */
    private static void generateStressTestSummary(String testName, List<PerformanceTestResult> results) {
        ExtentReportManager.createTest("📈 " + testName + " Stress Test Summary", 
            "Comprehensive stress test analysis across multiple load levels");
        
        ExtentReportManager.logInfo("=== STRESS TEST ANALYSIS ===");
        ExtentReportManager.logInfo("Test Scenarios Executed: " + results.size());
        
        for (PerformanceTestResult result : results) {
            ExtentReportManager.logInfo(String.format(
                "Users: %d | Avg Response: %dms | Error Rate: %.2f%% | Throughput: %.2f req/s | Status: %s",
                result.getConcurrentUsers(),
                result.getAvgResponseTime(),
                result.getErrorRate(),
                result.getThroughput(),
                result.meetsPerformanceThresholds() ? "PASS" : "FAIL"
            ));
        }
        
        // Find breaking point
        PerformanceTestResult lastPassing = null;
        PerformanceTestResult firstFailing = null;
        
        for (PerformanceTestResult result : results) {
            if (result.meetsPerformanceThresholds()) {
                lastPassing = result;
            } else if (firstFailing == null) {
                firstFailing = result;
                break;
            }
        }
        
        if (lastPassing != null && firstFailing != null) {
            ExtentReportManager.logWarning("Breaking point detected:");
            ExtentReportManager.logInfo("Last passing load: " + lastPassing.getConcurrentUsers() + " users");
            ExtentReportManager.logInfo("First failing load: " + firstFailing.getConcurrentUsers() + " users");
        } else if (lastPassing == null) {
            ExtentReportManager.logFail("System failed at minimum load level");
        } else {
            ExtentReportManager.logPass("System handled all tested load levels successfully");
        }
    }
    
    /**
     * Gets all performance test results
     */
    public static List<PerformanceTestResult> getAllTestResults() {
        return new ArrayList<>(testResults);
    }
    
    /**
     * Clears all performance test data
     */
    public static void clearPerformanceData() {
        responseTimeData.clear();
        requestCounts.clear();
        errorCounts.clear();
        totalExecutionTime.clear();
        testStartTimes.clear();
        testResults.clear();
        
        logger.debug("Performance test data cleared");
    }
    
    /**
     * Gets performance baseline for comparison
     */
    public static PerformanceBaseline getPerformanceBaseline() {
        return new PerformanceBaseline(
            Long.parseLong(config.getProperty("performance.response.time.threshold", String.valueOf(DEFAULT_RESPONSE_TIME_THRESHOLD))),
            Double.parseDouble(config.getProperty("performance.error.rate.threshold", String.valueOf(DEFAULT_ERROR_RATE_THRESHOLD))),
            Double.parseDouble(config.getProperty("performance.throughput.threshold", String.valueOf(DEFAULT_THROUGHPUT_THRESHOLD)))
        );
    }
    
    /**
     * Performance baseline configuration
     */
    public static class PerformanceBaseline {
        private final long responseTimeThreshold;
        private final double errorRateThreshold;
        private final double throughputThreshold;
        
        public PerformanceBaseline(long responseTimeThreshold, double errorRateThreshold, double throughputThreshold) {
            this.responseTimeThreshold = responseTimeThreshold;
            this.errorRateThreshold = errorRateThreshold;
            this.throughputThreshold = throughputThreshold;
        }
        
        // Getters
        public long getResponseTimeThreshold() { return responseTimeThreshold; }
        public double getErrorRateThreshold() { return errorRateThreshold; }
        public double getThroughputThreshold() { return throughputThreshold; }
    }
}