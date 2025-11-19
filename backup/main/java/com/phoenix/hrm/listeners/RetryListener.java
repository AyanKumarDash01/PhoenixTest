package com.phoenix.hrm.listeners;

import com.phoenix.hrm.core.reporting.ExtentReportManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IAnnotationTransformer;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Retry Listener for Phoenix HRM Test Framework
 * 
 * Manages the retry mechanism integration with TestNG by:
 * - Automatically applying RetryAnalyzer to all test methods
 * - Tracking retry statistics and outcomes
 * - Integrating with ExtentReports for retry documentation
 * - Managing retry state across parallel execution
 * 
 * @author Phoenix HRM Test Team
 */
public class RetryListener implements IAnnotationTransformer, ITestListener {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryListener.class);
    
    // Retry tracking
    private static final ConcurrentHashMap<String, Boolean> retryResults = new ConcurrentHashMap<>();
    private static final AtomicInteger totalRetriedTests = new AtomicInteger(0);
    private static final AtomicInteger successfullyRetriedTests = new AtomicInteger(0);
    
    // ===================== ANNOTATION TRANSFORMER METHODS =====================
    
    @Override
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        // Automatically apply RetryAnalyzer to all test methods
        if (testMethod != null && annotation.getRetryAnalyzer() == null) {
            annotation.setRetryAnalyzer(RetryAnalyzer.class);
            logger.debug("Applied RetryAnalyzer to test method: {}.{}", 
                testClass.getSimpleName(), testMethod.getName());
        }
    }
    
    // ===================== TEST LISTENER METHODS =====================
    
    @Override
    public void onTestStart(ITestResult result) {
        String testKey = getTestKey(result);
        
        // Check if this is a retry attempt
        int currentRetryCount = RetryAnalyzer.getCurrentRetryCount(result);
        if (currentRetryCount > 0) {
            logger.info("RETRY EXECUTION: Starting retry attempt #{} for test: {}", 
                currentRetryCount, getTestName(result));
            
            ExtentReportManager.logInfo(String.format("🔄 Retry attempt #%d starting", currentRetryCount));
        }
    }
    
    @Override
    public void onTestSuccess(ITestResult result) {
        String testKey = getTestKey(result);
        
        // Check if this test was successful after retry
        int retryCount = RetryAnalyzer.getCurrentRetryCount(result);
        if (retryCount > 0) {
            retryResults.put(testKey, true);
            successfullyRetriedTests.incrementAndGet();
            
            logger.info("RETRY SUCCESS: Test {} succeeded after {} retry attempts", 
                getTestName(result), retryCount);
            
            ExtentReportManager.logPass(String.format("✅ Test succeeded after %d retry attempts", retryCount));
            RetryAnalyzer.markRetrySuccessful();
        }
    }
    
    @Override
    public void onTestFailure(ITestResult result) {
        String testKey = getTestKey(result);
        
        // Check if this test failed after all retries
        int retryCount = RetryAnalyzer.getCurrentRetryCount(result);
        if (retryCount > 0) {
            retryResults.put(testKey, false);
            
            logger.error("RETRY FAILURE: Test {} failed after {} retry attempts", 
                getTestName(result), retryCount);
            
            ExtentReportManager.logFail(String.format("❌ Test failed permanently after %d retry attempts", retryCount));
        }
        
        // Track total retried tests
        if (retryCount > 0 && !retryResults.containsKey(testKey)) {
            totalRetriedTests.incrementAndGet();
        }
    }
    
    @Override
    public void onTestSkipped(ITestResult result) {
        String testKey = getTestKey(result);
        
        // Log skip with retry context if applicable
        int retryCount = RetryAnalyzer.getCurrentRetryCount(result);
        if (retryCount > 0) {
            logger.warn("RETRY SKIP: Test {} was skipped after {} retry attempts", 
                getTestName(result), retryCount);
            
            ExtentReportManager.logSkip(String.format("⏭️ Test skipped after %d retry attempts", retryCount));
        }
    }
    
    // ===================== UTILITY METHODS =====================
    
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
     * Gets retry statistics from this listener
     */
    public static RetryListenerStatistics getRetryListenerStatistics() {
        return new RetryListenerStatistics(
            totalRetriedTests.get(),
            successfullyRetriedTests.get(),
            retryResults.size()
        );
    }
    
    /**
     * Generates comprehensive retry report
     */
    public static String generateRetryReport() {
        RetryAnalyzer.RetryStatistics analyzerStats = RetryAnalyzer.getRetryStatistics();
        RetryListenerStatistics listenerStats = getRetryListenerStatistics();
        
        StringBuilder report = new StringBuilder();
        report.append("=== RETRY MECHANISM REPORT ===\n");
        report.append(String.format("Total Retry Attempts: %d\n", analyzerStats.getTotalRetries()));
        report.append(String.format("Successful Retries: %d\n", analyzerStats.getSuccessfulRetries()));
        report.append(String.format("Failed Retries: %d\n", analyzerStats.getFailedRetries()));
        report.append(String.format("Retry Success Rate: %.2f%%\n", analyzerStats.getRetrySuccessRate()));
        report.append(String.format("Tests That Required Retry: %d\n", listenerStats.getTotalRetriedTests()));
        report.append(String.format("Tests Successfully Recovered: %d\n", listenerStats.getSuccessfullyRetriedTests()));
        
        if (listenerStats.getTotalRetriedTests() > 0) {
            double recoveryRate = (double) listenerStats.getSuccessfullyRetriedTests() / 
                                 listenerStats.getTotalRetriedTests() * 100;
            report.append(String.format("Test Recovery Rate: %.2f%%\n", recoveryRate));
        }
        
        return report.toString();
    }
    
    /**
     * Logs retry summary to ExtentReports
     */
    public static void logRetrySummaryToReport() {
        ExtentReportManager.createTest("🔄 Retry Mechanism Summary", "Summary of retry attempts and outcomes");
        
        RetryAnalyzer.RetryStatistics stats = RetryAnalyzer.getRetryStatistics();
        RetryListenerStatistics listenerStats = getRetryListenerStatistics();
        
        ExtentReportManager.logInfo("=== RETRY MECHANISM SUMMARY ===");
        ExtentReportManager.logInfo("Total Retry Attempts: " + stats.getTotalRetries());
        ExtentReportManager.logInfo("Successful Retries: " + stats.getSuccessfulRetries());
        ExtentReportManager.logInfo("Failed Retries: " + stats.getFailedRetries());
        ExtentReportManager.logInfo(String.format("Retry Success Rate: %.2f%%", stats.getRetrySuccessRate()));
        ExtentReportManager.logInfo("Tests That Required Retry: " + listenerStats.getTotalRetriedTests());
        ExtentReportManager.logInfo("Tests Successfully Recovered: " + listenerStats.getSuccessfullyRetriedTests());
        
        if (listenerStats.getTotalRetriedTests() > 0) {
            double recoveryRate = (double) listenerStats.getSuccessfullyRetriedTests() / 
                                 listenerStats.getTotalRetriedTests() * 100;
            ExtentReportManager.logInfo(String.format("Test Recovery Rate: %.2f%%", recoveryRate));
            
            if (recoveryRate >= 80) {
                ExtentReportManager.logPass("Excellent retry recovery rate! ✅");
            } else if (recoveryRate >= 60) {
                ExtentReportManager.logWarning("Good retry recovery rate ⚠️");
            } else {
                ExtentReportManager.logFail("Poor retry recovery rate - investigate test stability ❌");
            }
        }
        
        // Add retry recommendations
        addRetryRecommendations(stats, listenerStats);
    }
    
    /**
     * Adds retry recommendations based on statistics
     */
    private static void addRetryRecommendations(RetryAnalyzer.RetryStatistics analyzerStats, 
                                               RetryListenerStatistics listenerStats) {
        ExtentReportManager.logInfo("=== RETRY RECOMMENDATIONS ===");
        
        if (analyzerStats.getTotalRetries() == 0) {
            ExtentReportManager.logPass("No retries needed - excellent test stability! 🎉");
            return;
        }
        
        if (analyzerStats.getRetrySuccessRate() < 50) {
            ExtentReportManager.logFail("High retry failure rate detected:");
            ExtentReportManager.logInfo("• Review test environment stability");
            ExtentReportManager.logInfo("• Check for flaky test patterns");
            ExtentReportManager.logInfo("• Consider increasing wait times");
            ExtentReportManager.logInfo("• Review network connectivity");
        }
        
        if (listenerStats.getTotalRetriedTests() > 10) {
            ExtentReportManager.logWarning("High number of tests requiring retries:");
            ExtentReportManager.logInfo("• Consider test refactoring");
            ExtentReportManager.logInfo("• Review test data dependencies");
            ExtentReportManager.logInfo("• Check browser/infrastructure stability");
        }
        
        if (analyzerStats.getRetrySuccessRate() > 80) {
            ExtentReportManager.logPass("Good retry mechanism effectiveness! 👍");
            ExtentReportManager.logInfo("Retry mechanism is working well for infrastructure issues");
        }
    }
    
    /**
     * Clears all retry listener statistics
     */
    public static void clearRetryListenerStatistics() {
        retryResults.clear();
        totalRetriedTests.set(0);
        successfullyRetriedTests.set(0);
        logger.debug("Retry listener statistics cleared");
    }
    
    /**
     * Inner class to hold retry listener statistics
     */
    public static class RetryListenerStatistics {
        private final int totalRetriedTests;
        private final int successfullyRetriedTests;
        private final int trackedTests;
        
        public RetryListenerStatistics(int totalRetriedTests, int successfullyRetriedTests, int trackedTests) {
            this.totalRetriedTests = totalRetriedTests;
            this.successfullyRetriedTests = successfullyRetriedTests;
            this.trackedTests = trackedTests;
        }
        
        // Getters
        public int getTotalRetriedTests() { return totalRetriedTests; }
        public int getSuccessfullyRetriedTests() { return successfullyRetriedTests; }
        public int getFailedRetriedTests() { return totalRetriedTests - successfullyRetriedTests; }
        public int getTrackedTests() { return trackedTests; }
        public double getTestRecoveryRate() { 
            return totalRetriedTests > 0 ? (double) successfullyRetriedTests / totalRetriedTests * 100 : 0; 
        }
        
        @Override
        public String toString() {
            return String.format("Retry Listener Stats: Total Retried=%d, Successful=%d, Failed=%d, Recovery Rate=%.2f%%",
                totalRetriedTests, successfullyRetriedTests, getFailedRetriedTests(), getTestRecoveryRate());
        }
    }
}