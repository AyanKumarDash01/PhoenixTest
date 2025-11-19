package com.phoenix.hrm.listeners;

import com.phoenix.hrm.core.reporting.ExtentReportManager;
import com.phoenix.hrm.core.driver.WebDriverFactory;
import com.phoenix.hrm.core.utils.ScreenshotUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.*;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced TestNG Listener with ExtentReports Integration
 * 
 * Provides comprehensive test execution monitoring with:
 * - ExtentReports integration
 * - Screenshot capture on failure
 * - Performance monitoring
 * - Test categorization
 * - Real-time reporting
 * - Suite-level statistics
 * 
 * @author Phoenix HRM Test Team
 */
public class TestListener implements ISuiteListener, ITestListener, IInvokedMethodListener {
    
    private static final Logger logger = LoggerFactory.getLogger(TestListener.class);
    
    // Performance tracking
    private static final ConcurrentHashMap<String, Instant> testStartTimes = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Duration> testExecutionTimes = new ConcurrentHashMap<>();
    
    // Suite-level statistics
    private static Instant suiteStartTime;
    private static int totalMethods = 0;
    private static int passedMethods = 0;
    private static int failedMethods = 0;
    private static int skippedMethods = 0;
    
    // ===================== SUITE LEVEL METHODS =====================
    
    @Override
    public void onStart(ISuite suite) {
        suiteStartTime = Instant.now();
        logger.info("=".repeat(80));
        logger.info("SUITE STARTED: {}", suite.getName());
        logger.info("Suite XML File: {}", suite.getXmlSuite().getFileName());
        logger.info("Parallel Mode: {}", suite.getXmlSuite().getParallel());
        logger.info("Thread Count: {}", suite.getXmlSuite().getThreadCount());
        logger.info("=".repeat(80));
        
        // Initialize ExtentReports
        ExtentReportManager.initializeExtentReports();
        
        // Log suite configuration
        logSuiteConfiguration(suite);
    }
    
    @Override
    public void onFinish(ISuite suite) {
        Duration suiteExecutionTime = Duration.between(suiteStartTime, Instant.now());
        
        logger.info("=".repeat(80));
        logger.info("SUITE FINISHED: {}", suite.getName());
        logger.info("Total Execution Time: {} minutes {} seconds", 
            suiteExecutionTime.toMinutes(), suiteExecutionTime.getSeconds() % 60);
        logger.info("Total Methods Executed: {}", totalMethods);
        logger.info("Methods Passed: {}", passedMethods);
        logger.info("Methods Failed: {}", failedMethods);
        logger.info("Methods Skipped: {}", skippedMethods);
        
        if (totalMethods > 0) {
            double passPercentage = (double) passedMethods / totalMethods * 100;
            logger.info("Pass Percentage: {:.2f}%", passPercentage);
        }
        
        logger.info("=".repeat(80));
        
        // Flush ExtentReports
        ExtentReportManager.flushReports();
        
        // Log final report location
        logger.info("ExtentReports generated at: {}", ExtentReportManager.getReportPath());
    }
    
    // ===================== TEST LEVEL METHODS =====================
    
    @Override
    public void onTestStart(ITestResult result) {
        String testName = getTestName(result);
        testStartTimes.put(testName, Instant.now());
        
        logger.info("-".repeat(60));
        logger.info("TEST STARTED: {}", testName);
        logger.info("Test Class: {}", result.getTestClass().getName());
        logger.info("Test Method: {}", result.getMethod().getMethodName());
        logger.info("Parameters: {}", Arrays.toString(result.getParameters()));
        logger.info("Thread: {}", Thread.currentThread().getName());
        logger.info("-".repeat(60));
        
        // Create ExtentTest
        String description = getTestDescription(result);
        String[] categories = getTestCategories(result);
        
        ExtentReportManager.createTest(testName, description, categories);
        
        // Add test information
        ExtentReportManager.logInfo("Test Class: " + result.getTestClass().getName());
        ExtentReportManager.logInfo("Test Method: " + result.getMethod().getMethodName());
        
        if (result.getParameters().length > 0) {
            ExtentReportManager.logInfo("Parameters: " + Arrays.toString(result.getParameters()));
        }
        
        ExtentReportManager.assignAuthor("Phoenix HRM Test Team");
        
        // Log browser information for UI tests
        try {
            if (WebDriverFactory.getDriver() != null) {
                ExtentReportManager.assignDevice("Browser: " + 
                    WebDriverFactory.getDriver().getClass().getSimpleName());
            }
        } catch (Exception e) {
            logger.debug("No WebDriver instance found for test: {}", testName);
        }
    }
    
    @Override
    public void onTestSuccess(ITestResult result) {
        String testName = getTestName(result);
        Duration executionTime = calculateExecutionTime(testName);
        passedMethods++;
        
        logger.info("✅ TEST PASSED: {} (Execution Time: {} ms)", testName, executionTime.toMillis());
        
        ExtentReportManager.logPass("Test completed successfully");
        ExtentReportManager.logPerformanceMetrics("Test Execution", executionTime.toMillis(), 
            executionTime.toMillis() < 30000); // 30 second threshold
        
        logTestCompletion(result, "PASSED");
    }
    
    @Override
    public void onTestFailure(ITestResult result) {
        String testName = getTestName(result);
        Duration executionTime = calculateExecutionTime(testName);
        failedMethods++;
        
        logger.error("❌ TEST FAILED: {} (Execution Time: {} ms)", testName, executionTime.toMillis());
        logger.error("Failure Reason: {}", result.getThrowable().getMessage());
        
        ExtentReportManager.logFail("Test failed: " + result.getThrowable().getMessage(), 
            result.getThrowable());
        
        // Capture screenshot on failure
        captureScreenshotOnFailure(result);
        
        logTestCompletion(result, "FAILED");
    }
    
    @Override
    public void onTestSkipped(ITestResult result) {
        String testName = getTestName(result);
        Duration executionTime = calculateExecutionTime(testName);
        skippedMethods++;
        
        logger.warn("⏭️ TEST SKIPPED: {} (Execution Time: {} ms)", testName, executionTime.toMillis());
        
        String skipReason = result.getThrowable() != null ? 
            result.getThrowable().getMessage() : "Test was skipped";
        
        ExtentReportManager.logSkip("Test skipped: " + skipReason);
        
        logTestCompletion(result, "SKIPPED");
    }
    
    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        String testName = getTestName(result);
        logger.warn("⚠️ TEST FAILED BUT WITHIN SUCCESS PERCENTAGE: {}", testName);
        
        ExtentReportManager.logWarning("Test failed but within success percentage threshold");
        ExtentReportManager.logFail("Test failed: " + result.getThrowable().getMessage(), 
            result.getThrowable());
    }
    
    // ===================== INVOKED METHOD METHODS =====================
    
    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        if (method.isTestMethod()) {
            totalMethods++;
            String methodName = method.getTestMethod().getMethodName();
            logger.debug("About to invoke test method: {}", methodName);
            
            ExtentReportManager.logInfo("Starting test method: " + methodName);
        }
    }
    
    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        if (method.isTestMethod()) {
            String methodName = method.getTestMethod().getMethodName();
            logger.debug("Finished invoking test method: {}", methodName);
        }
    }
    
    // ===================== UTILITY METHODS =====================
    
    /**
     * Gets formatted test name
     */
    private String getTestName(ITestResult result) {
        String className = result.getTestClass().getName();
        String methodName = result.getMethod().getMethodName();
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        
        if (result.getParameters().length > 0) {
            return String.format("%s.%s[%s]", simpleName, methodName, 
                Arrays.toString(result.getParameters()));
        }
        
        return String.format("%s.%s", simpleName, methodName);
    }
    
    /**
     * Gets test description from method or class annotations
     */
    private String getTestDescription(ITestResult result) {
        // Try to get description from Test annotation
        Method method = result.getMethod().getConstructorOrMethod().getMethod();
        org.testng.annotations.Test testAnnotation = method.getAnnotation(org.testng.annotations.Test.class);
        
        if (testAnnotation != null && !testAnnotation.description().isEmpty()) {
            return testAnnotation.description();
        }
        
        // Fallback to method name
        return "Test method: " + result.getMethod().getMethodName();
    }
    
    /**
     * Gets test categories from groups and custom annotations
     */
    private String[] getTestCategories(ITestResult result) {
        String[] groups = result.getMethod().getGroups();
        
        if (groups != null && groups.length > 0) {
            return groups;
        }
        
        // Default category based on package
        String packageName = result.getTestClass().getName();
        if (packageName.contains("api")) {
            return new String[]{"API Tests"};
        } else if (packageName.contains("ui")) {
            return new String[]{"UI Tests"};
        } else if (packageName.contains("bdd")) {
            return new String[]{"BDD Tests"};
        }
        
        return new String[]{"General"};
    }
    
    /**
     * Calculates execution time for a test
     */
    private Duration calculateExecutionTime(String testName) {
        Instant startTime = testStartTimes.get(testName);
        if (startTime != null) {
            Duration duration = Duration.between(startTime, Instant.now());
            testExecutionTimes.put(testName, duration);
            return duration;
        }
        return Duration.ZERO;
    }
    
    /**
     * Captures screenshot on test failure
     */
    private void captureScreenshotOnFailure(ITestResult result) {
        try {
            if (WebDriverFactory.getDriver() != null) {
                String testName = getTestName(result);
                String screenshotPath = ScreenshotUtils.captureScreenshot(
                    WebDriverFactory.getDriver(), 
                    "FAILED_" + testName.replaceAll("[^a-zA-Z0-9]", "_")
                );
                
                if (screenshotPath != null) {
                    ExtentReportManager.attachScreenshot(screenshotPath, "Screenshot on failure");
                    logger.info("Screenshot captured for failed test: {}", screenshotPath);
                }
                
                // Also capture as base64 for better report integration
                String base64Screenshot = ScreenshotUtils.captureScreenshotAsBase64String(
                    WebDriverFactory.getDriver()
                );
                if (base64Screenshot != null) {
                    ExtentReportManager.attachScreenshotBase64(base64Screenshot, "Failure Screenshot");
                }
                
            }
        } catch (Exception e) {
            logger.error("Failed to capture screenshot on failure: {}", e.getMessage());
            ExtentReportManager.logWarning("Failed to capture screenshot: " + e.getMessage());
        }
    }
    
    /**
     * Logs test completion details
     */
    private void logTestCompletion(ITestResult result, String status) {
        String testName = getTestName(result);
        Duration executionTime = testExecutionTimes.get(testName);
        
        logger.info("Test Completed: {} | Status: {} | Duration: {} ms | Thread: {}", 
            testName, status, 
            executionTime != null ? executionTime.toMillis() : 0,
            Thread.currentThread().getName());
    }
    
    /**
     * Logs suite configuration details
     */
    private void logSuiteConfiguration(ISuite suite) {
        ExtentReportManager.createTest("📋 Suite Configuration", "Test suite configuration and parameters");
        
        ExtentReportManager.logInfo("Suite Name: " + suite.getName());
        ExtentReportManager.logInfo("XML File: " + suite.getXmlSuite().getFileName());
        ExtentReportManager.logInfo("Parallel Mode: " + suite.getXmlSuite().getParallel());
        ExtentReportManager.logInfo("Thread Count: " + suite.getXmlSuite().getThreadCount());
        ExtentReportManager.logInfo("Verbose Level: " + suite.getXmlSuite().getVerbose());
        
        // Log parameters
        if (!suite.getXmlSuite().getParameters().isEmpty()) {
            ExtentReportManager.logInfo("Suite Parameters:");
            suite.getXmlSuite().getParameters().forEach((key, value) -> 
                ExtentReportManager.logInfo("  " + key + ": " + value));
        }
        
        // Log included groups
        if (!suite.getXmlSuite().getIncludedGroups().isEmpty()) {
            ExtentReportManager.logInfo("Included Groups: " + suite.getXmlSuite().getIncludedGroups());
        }
        
        // Log excluded groups
        if (!suite.getXmlSuite().getExcludedGroups().isEmpty()) {
            ExtentReportManager.logInfo("Excluded Groups: " + suite.getXmlSuite().getExcludedGroups());
        }
    }
    
    /**
     * Gets current test statistics
     */
    public static TestStatistics getTestStatistics() {
        return new TestStatistics(totalMethods, passedMethods, failedMethods, skippedMethods);
    }
    
    /**
     * Inner class to hold test statistics
     */
    public static class TestStatistics {
        private final int totalMethods;
        private final int passedMethods;
        private final int failedMethods;
        private final int skippedMethods;
        
        public TestStatistics(int totalMethods, int passedMethods, int failedMethods, int skippedMethods) {
            this.totalMethods = totalMethods;
            this.passedMethods = passedMethods;
            this.failedMethods = failedMethods;
            this.skippedMethods = skippedMethods;
        }
        
        // Getters
        public int getTotalMethods() { return totalMethods; }
        public int getPassedMethods() { return passedMethods; }
        public int getFailedMethods() { return failedMethods; }
        public int getSkippedMethods() { return skippedMethods; }
        public double getPassPercentage() { 
            return totalMethods > 0 ? (double) passedMethods / totalMethods * 100 : 0; 
        }
    }
}