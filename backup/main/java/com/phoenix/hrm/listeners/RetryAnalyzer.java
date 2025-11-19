package com.phoenix.hrm.listeners;

import com.phoenix.hrm.core.config.ConfigManager;
import com.phoenix.hrm.core.reporting.ExtentReportManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Intelligent Retry Analyzer for Phoenix HRM Test Framework
 * 
 * Provides smart retry mechanism for failed tests with:
 * - Configurable retry counts
 * - Test-specific retry logic
 * - Retry tracking and reporting
 * - Exception-based retry decisions
 * - Performance-aware retries
 * 
 * @author Phoenix HRM Test Team
 */
public class RetryAnalyzer implements IRetryAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryAnalyzer.class);
    
    // Configuration
    private static final ConfigManager config = ConfigManager.getInstance();
    private static final int MAX_RETRY_COUNT = Integer.parseInt(
        config.getProperty("retry.count", "2")
    );
    private static final long RETRY_DELAY = Long.parseLong(
        config.getProperty("retry.delay", "1000")
    );
    
    // Thread-safe retry counters
    private static final ConcurrentHashMap<String, AtomicInteger> retryCounts = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> lastRetryTime = new ConcurrentHashMap<>();
    
    // Retry statistics
    private static final AtomicInteger totalRetries = new AtomicInteger(0);
    private static final AtomicInteger successfulRetries = new AtomicInteger(0);
    
    @Override
    public boolean retry(ITestResult result) {
        String testKey = getTestKey(result);
        AtomicInteger currentRetryCount = retryCounts.computeIfAbsent(testKey, k -> new AtomicInteger(0));
        
        int retryAttempt = currentRetryCount.incrementAndGet();
        
        if (retryAttempt <= MAX_RETRY_COUNT && shouldRetry(result)) {
            totalRetries.incrementAndGet();
            
            logger.warn("RETRY ATTEMPT #{} for test: {} | Reason: {}", 
                retryAttempt, getTestName(result), result.getThrowable().getMessage());
            
            // Log retry to ExtentReports
            ExtentReportManager.logWarning(String.format("Retry attempt #%d due to: %s", 
                retryAttempt, result.getThrowable().getMessage()));
            
            // Add delay between retries to avoid race conditions
            addRetryDelay(testKey);
            
            // Update retry statistics
            updateRetryStatistics(result, retryAttempt);
            
            return true;
        }
        
        // Log final failure after all retries exhausted
        if (retryAttempt > MAX_RETRY_COUNT) {
            logger.error("RETRY EXHAUSTED for test: {} after {} attempts", 
                getTestName(result), MAX_RETRY_COUNT);
            
            ExtentReportManager.logFail(String.format("Test failed after %d retry attempts", MAX_RETRY_COUNT));
        }
        
        return false;
    }
    
    /**
     * Determines if a test should be retried based on failure type
     */
    private boolean shouldRetry(ITestResult result) {
        Throwable throwable = result.getThrowable();
        
        if (throwable == null) {
            return false;
        }
        
        String exceptionMessage = throwable.getMessage();
        String exceptionClass = throwable.getClass().getSimpleName();
        
        // Don't retry for assertion failures (test logic errors)
        if (exceptionClass.contains("AssertionError") || exceptionClass.contains("AssertionFailedError")) {
            logger.debug("Skipping retry for assertion failure: {}", exceptionMessage);
            return false;
        }
        
        // Don't retry for test data issues
        if (exceptionMessage != null && (
            exceptionMessage.contains("DataProvider") ||
            exceptionMessage.contains("Parameter") ||
            exceptionMessage.contains("Invalid test data")
        )) {
            logger.debug("Skipping retry for test data issue: {}", exceptionMessage);
            return false;
        }
        
        // Retry for infrastructure-related failures
        if (isInfrastructureFailure(throwable)) {
            logger.debug("Retrying due to infrastructure failure: {}", exceptionMessage);
            return true;
        }
        
        // Retry for timing-related issues
        if (isTimingIssue(throwable)) {
            logger.debug("Retrying due to timing issue: {}", exceptionMessage);
            return true;
        }
        
        // Retry for network-related issues
        if (isNetworkIssue(throwable)) {
            logger.debug("Retrying due to network issue: {}", exceptionMessage);
            return true;
        }
        
        // Default: retry for most exceptions except assertion failures
        return true;
    }
    
    /**
     * Checks if the failure is related to infrastructure issues
     */
    private boolean isInfrastructureFailure(Throwable throwable) {
        String message = throwable.getMessage();
        String className = throwable.getClass().getSimpleName();
        
        return className.contains("WebDriverException") ||
               className.contains("SessionNotCreatedException") ||
               className.contains("NoSuchSessionException") ||
               (message != null && (
                   message.contains("session deleted because of page crash") ||
                   message.contains("chrome not reachable") ||
                   message.contains("Session info: chrome") ||
                   message.contains("unknown error: cannot determine loading status")
               ));
    }
    
    /**
     * Checks if the failure is related to timing issues
     */
    private boolean isTimingIssue(Throwable throwable) {
        String message = throwable.getMessage();
        String className = throwable.getClass().getSimpleName();
        
        return className.contains("TimeoutException") ||
               className.contains("StaleElementReferenceException") ||
               className.contains("ElementNotInteractableException") ||
               (message != null && (
                   message.contains("timeout") ||
                   message.contains("not clickable") ||
                   message.contains("element not found") ||
                   message.contains("element is not attached")
               ));
    }
    
    /**
     * Checks if the failure is related to network issues
     */
    private boolean isNetworkIssue(Throwable throwable) {
        String message = throwable.getMessage();
        String className = throwable.getClass().getSimpleName();
        
        return className.contains("ConnectException") ||
               className.contains("SocketTimeoutException") ||
               className.contains("UnknownHostException") ||
               className.contains("NoRouteToHostException") ||
               (message != null && (
                   message.contains("connection refused") ||
                   message.contains("connection reset") ||
                   message.contains("network is unreachable") ||
                   message.contains("timeout")
               ));
    }
    
    /**
     * Adds delay between retry attempts
     */
    private void addRetryDelay(String testKey) {
        try {
            long currentTime = System.currentTimeMillis();
            Long lastRetry = lastRetryTime.get(testKey);
            
            if (lastRetry != null && (currentTime - lastRetry) < RETRY_DELAY) {
                long sleepTime = RETRY_DELAY - (currentTime - lastRetry);
                logger.debug("Adding retry delay of {} ms for test: {}", sleepTime, testKey);
                Thread.sleep(sleepTime);
            }
            
            lastRetryTime.put(testKey, currentTime);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Retry delay interrupted for test: {}", testKey);
        }
    }
    
    /**
     * Updates retry statistics
     */
    private void updateRetryStatistics(ITestResult result, int retryAttempt) {
        String testName = getTestName(result);
        
        // Check if this is the first retry
        if (retryAttempt == 1) {
            ExtentReportManager.logInfo("Starting retry mechanism for failed test");
        }
        
        ExtentReportManager.logInfo(String.format("Retry attempt: %d/%d", retryAttempt, MAX_RETRY_COUNT));
        
        // Log retry details
        logger.debug("Retry Statistics - Test: {}, Attempt: {}, Total Retries: {}", 
            testName, retryAttempt, totalRetries.get());
    }
    
    /**
     * Generates unique test key for retry tracking
     */
    private String getTestKey(ITestResult result) {
        return result.getTestClass().getName() + "#" + result.getMethod().getMethodName() +
               "#" + Thread.currentThread().getId();
    }
    
    /**
     * Gets formatted test name
     */
    private String getTestName(ITestResult result) {
        String className = result.getTestClass().getName();
        String methodName = result.getMethod().getMethodName();
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        return simpleName + "." + methodName;
    }
    
    /**
     * Marks a retry as successful (called from RetryListener)
     */
    public static void markRetrySuccessful() {
        successfulRetries.incrementAndGet();
    }
    
    /**
     * Gets retry statistics
     */
    public static RetryStatistics getRetryStatistics() {
        return new RetryStatistics(
            totalRetries.get(), 
            successfulRetries.get(),
            MAX_RETRY_COUNT
        );
    }
    
    /**
     * Clears retry counters for a specific test (useful for data-driven tests)
     */
    public static void clearRetryCount(String testKey) {
        retryCounts.remove(testKey);
        lastRetryTime.remove(testKey);
    }
    
    /**
     * Clears all retry statistics (useful for suite cleanup)
     */
    public static void clearAllRetryStatistics() {
        retryCounts.clear();
        lastRetryTime.clear();
        totalRetries.set(0);
        successfulRetries.set(0);
        logger.debug("Retry statistics cleared");
    }
    
    /**
     * Gets current retry count for a specific test
     */
    public static int getCurrentRetryCount(ITestResult result) {
        String testKey = result.getTestClass().getName() + "#" + result.getMethod().getMethodName() +
                        "#" + Thread.currentThread().getId();
        AtomicInteger count = retryCounts.get(testKey);
        return count != null ? count.get() : 0;
    }
    
    /**
     * Configures retry behavior for specific test types
     */
    public static class RetryConfig {
        private static final ConcurrentHashMap<String, Integer> testSpecificRetryCounts = new ConcurrentHashMap<>();
        
        /**
         * Sets custom retry count for specific test classes or methods
         */
        public static void setRetryCount(String testPattern, int retryCount) {
            testSpecificRetryCounts.put(testPattern, retryCount);
            logger.debug("Custom retry count set: {} -> {}", testPattern, retryCount);
        }
        
        /**
         * Gets retry count for a specific test
         */
        public static int getRetryCount(String testName) {
            // Check for exact match first
            Integer customCount = testSpecificRetryCounts.get(testName);
            if (customCount != null) {
                return customCount;
            }
            
            // Check for pattern matches
            for (String pattern : testSpecificRetryCounts.keySet()) {
                if (testName.contains(pattern)) {
                    return testSpecificRetryCounts.get(pattern);
                }
            }
            
            // Return default
            return MAX_RETRY_COUNT;
        }
    }
    
    /**
     * Inner class to hold retry statistics
     */
    public static class RetryStatistics {
        private final int totalRetries;
        private final int successfulRetries;
        private final int maxRetryCount;
        
        public RetryStatistics(int totalRetries, int successfulRetries, int maxRetryCount) {
            this.totalRetries = totalRetries;
            this.successfulRetries = successfulRetries;
            this.maxRetryCount = maxRetryCount;
        }
        
        // Getters
        public int getTotalRetries() { return totalRetries; }
        public int getSuccessfulRetries() { return successfulRetries; }
        public int getFailedRetries() { return totalRetries - successfulRetries; }
        public int getMaxRetryCount() { return maxRetryCount; }
        public double getRetrySuccessRate() { 
            return totalRetries > 0 ? (double) successfulRetries / totalRetries * 100 : 0; 
        }
        
        @Override
        public String toString() {
            return String.format("Retry Statistics: Total=%d, Successful=%d, Failed=%d, Success Rate=%.2f%%",
                totalRetries, successfulRetries, getFailedRetries(), getRetrySuccessRate());
        }
    }
}