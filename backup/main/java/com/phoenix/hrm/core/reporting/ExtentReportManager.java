package com.phoenix.hrm.core.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.markuputils.CodeLanguage;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.phoenix.hrm.core.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ExtentReports Manager for Phoenix HRM Test Framework
 * 
 * Provides centralized management of ExtentReports with enterprise features:
 * - Custom themes and branding
 * - Thread-safe test management
 * - Screenshot attachment
 * - Performance metrics logging
 * - Environment information capture
 * - Real-time reporting
 * 
 * @author Phoenix HRM Test Team
 */
public class ExtentReportManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ExtentReportManager.class);
    private static ExtentReports extentReports;
    private static ExtentSparkReporter sparkReporter;
    private static final ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();
    private static final ConcurrentHashMap<String, ExtentTest> testMap = new ConcurrentHashMap<>();
    
    // Configuration
    private static final ConfigManager config = ConfigManager.getInstance();
    private static String reportPath;
    private static final String REPORT_NAME = "Phoenix_HRM_Test_Report";
    
    // Performance tracking
    private static long suiteStartTime;
    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;
    private static int skippedTests = 0;
    
    /**
     * Initializes ExtentReports with custom configuration
     */
    public static synchronized void initializeExtentReports() {
        if (extentReports == null) {
            logger.info("Initializing ExtentReports...");
            
            // Create report directory
            createReportDirectory();
            
            // Generate report file path
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            reportPath = getReportDirectory() + File.separator + REPORT_NAME + "_" + timestamp + ".html";
            
            // Initialize Spark Reporter
            sparkReporter = new ExtentSparkReporter(reportPath);
            configureSparkReporter();
            
            // Initialize ExtentReports
            extentReports = new ExtentReports();
            extentReports.attachReporter(sparkReporter);
            
            // Set system information
            setSystemInformation();
            
            suiteStartTime = System.currentTimeMillis();
            logger.info("ExtentReports initialized successfully. Report path: {}", reportPath);
        }
    }
    
    /**
     * Configures Spark Reporter with custom themes and settings
     */
    private static void configureSparkReporter() {
        try {
            // Basic configuration
            sparkReporter.config().setTheme(Theme.DARK);
            sparkReporter.config().setDocumentTitle("Phoenix HRM Test Automation Report");
            sparkReporter.config().setReportName("Phoenix HRM Test Execution Report");
            
            // Custom CSS for branding
            String customCSS = """
                .navbar-brand {
                    background-color: #2E7D32 !important;
                }
                .card-header {
                    background: linear-gradient(135deg, #2E7D32, #4CAF50) !important;
                    color: white !important;
                }
                .test-name {
                    color: #1976D2 !important;
                    font-weight: bold !important;
                }
                .badge-primary {
                    background-color: #2E7D32 !important;
                }
                .progress-bar {
                    background-color: #4CAF50 !important;
                }
                """;
            
            sparkReporter.config().setCss(customCSS);
            
            // JavaScript for enhanced functionality
            String customJS = """
                window.onload = function() {
                    // Add custom header
                    document.querySelector('.navbar-brand').innerHTML = 
                        '<i class="fas fa-chart-line"></i> Phoenix HRM Test Report';
                    
                    // Add timestamp to header
                    const timestamp = new Date().toLocaleString();
                    const headerInfo = document.createElement('div');
                    headerInfo.innerHTML = '<small>Generated: ' + timestamp + '</small>';
                    headerInfo.style.color = '#666';
                    document.querySelector('.navbar').appendChild(headerInfo);
                };
                """;
            
            sparkReporter.config().setJs(customJS);
            
            // Time stamp format
            sparkReporter.config().setTimeStampFormat("yyyy-MM-dd HH:mm:ss");
            
            logger.info("Spark Reporter configured with custom theme and branding");
            
        } catch (Exception e) {
            logger.error("Error configuring Spark Reporter: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Sets system information for the report
     */
    private static void setSystemInformation() {
        extentReports.setSystemInfo("Application", "Phoenix HRM Test Automation");
        extentReports.setSystemInfo("Environment", config.getProperty("environment", "Test"));
        extentReports.setSystemInfo("User", System.getProperty("user.name"));
        extentReports.setSystemInfo("OS", System.getProperty("os.name"));
        extentReports.setSystemInfo("Java Version", System.getProperty("java.version"));
        extentReports.setSystemInfo("Browser", config.getProperty("browser", "chrome"));
        extentReports.setSystemInfo("Base URL", config.getProperty("base.url"));
        extentReports.setSystemInfo("API Base URL", config.getProperty("api.base.url", "N/A"));
        
        // Framework information
        extentReports.setSystemInfo("Framework Version", "Phoenix HRM v1.0.0");
        extentReports.setSystemInfo("Test Framework", "TestNG + Selenium + REST Assured + Cucumber");
        extentReports.setSystemInfo("Report Generated", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
    
    /**
     * Creates a new test in the report
     */
    public static synchronized ExtentTest createTest(String testName, String description) {
        ExtentTest test = extentReports.createTest(testName, description);
        extentTest.set(test);
        testMap.put(Thread.currentThread().getName() + "_" + testName, test);
        totalTests++;
        
        logger.debug("Created ExtentTest: {} - {}", testName, description);
        return test;
    }
    
    /**
     * Creates a new test with category tags
     */
    public static synchronized ExtentTest createTest(String testName, String description, String... categories) {
        ExtentTest test = createTest(testName, description);
        
        // Assign categories
        for (String category : categories) {
            test.assignCategory(category);
        }
        
        return test;
    }
    
    /**
     * Gets the current test instance
     */
    public static ExtentTest getTest() {
        return extentTest.get();
    }
    
    /**
     * Gets test by thread and name combination
     */
    public static ExtentTest getTest(String testName) {
        return testMap.get(Thread.currentThread().getName() + "_" + testName);
    }
    
    /**
     * Logs information to the current test
     */
    public static void logInfo(String message) {
        ExtentTest test = getTest();
        if (test != null) {
            test.info(message);
        }
        logger.info(message);
    }
    
    /**
     * Logs a pass step to the current test
     */
    public static void logPass(String message) {
        ExtentTest test = getTest();
        if (test != null) {
            test.pass(MarkupHelper.createLabel(message, ExtentColor.GREEN));
            passedTests++;
        }
        logger.info("PASS: {}", message);
    }
    
    /**
     * Logs a fail step to the current test
     */
    public static void logFail(String message) {
        ExtentTest test = getTest();
        if (test != null) {
            test.fail(MarkupHelper.createLabel(message, ExtentColor.RED));
            failedTests++;
        }
        logger.error("FAIL: {}", message);
    }
    
    /**
     * Logs a fail step with exception details
     */
    public static void logFail(String message, Throwable exception) {
        ExtentTest test = getTest();
        if (test != null) {
            test.fail(message);
            test.fail(exception);
            failedTests++;
        }
        logger.error("FAIL: {} - Exception: {}", message, exception.getMessage());
    }
    
    /**
     * Logs a skip step to the current test
     */
    public static void logSkip(String message) {
        ExtentTest test = getTest();
        if (test != null) {
            test.skip(MarkupHelper.createLabel(message, ExtentColor.ORANGE));
            skippedTests++;
        }
        logger.warn("SKIP: {}", message);
    }
    
    /**
     * Logs a warning to the current test
     */
    public static void logWarning(String message) {
        ExtentTest test = getTest();
        if (test != null) {
            test.warning(MarkupHelper.createLabel(message, ExtentColor.YELLOW));
        }
        logger.warn("WARNING: {}", message);
    }
    
    /**
     * Attaches screenshot to the current test
     */
    public static void attachScreenshot(String screenshotPath, String description) {
        ExtentTest test = getTest();
        if (test != null && screenshotPath != null) {
            try {
                test.addScreenCaptureFromPath(screenshotPath, description);
                logger.debug("Screenshot attached to report: {}", screenshotPath);
            } catch (Exception e) {
                logger.error("Failed to attach screenshot to report: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Attaches screenshot with base64 encoding
     */
    public static void attachScreenshotBase64(String base64Screenshot, String description) {
        ExtentTest test = getTest();
        if (test != null && base64Screenshot != null) {
            try {
                test.info(description, MediaEntityBuilder.createScreenCaptureFromBase64String(base64Screenshot).build());
                logger.debug("Base64 screenshot attached to report");
            } catch (Exception e) {
                logger.error("Failed to attach base64 screenshot: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Logs code snippet to the report
     */
    public static void logCode(String code, CodeLanguage language) {
        ExtentTest test = getTest();
        if (test != null) {
            test.info(MarkupHelper.createCodeBlock(code, language));
        }
    }
    
    /**
     * Logs JSON response to the report
     */
    public static void logJson(String jsonResponse, String description) {
        ExtentTest test = getTest();
        if (test != null) {
            test.info(description);
            test.info(MarkupHelper.createCodeBlock(jsonResponse, CodeLanguage.JSON));
        }
    }
    
    /**
     * Logs performance metrics to the current test
     */
    public static void logPerformanceMetrics(String operation, long responseTime, boolean withinThreshold) {
        ExtentTest test = getTest();
        if (test != null) {
            Status status = withinThreshold ? Status.PASS : Status.WARNING;
            String message = String.format("Performance: %s completed in %d ms %s", 
                operation, responseTime, withinThreshold ? "(✓ Within threshold)" : "(⚠ Exceeds threshold)");
            
            test.log(status, MarkupHelper.createLabel(message, 
                withinThreshold ? ExtentColor.GREEN : ExtentColor.ORANGE));
        }
    }
    
    /**
     * Creates a child test (sub-test)
     */
    public static ExtentTest createChildTest(String testName, String description) {
        ExtentTest parentTest = getTest();
        if (parentTest != null) {
            return parentTest.createNode(testName, description);
        }
        return null;
    }
    
    /**
     * Assigns author to the current test
     */
    public static void assignAuthor(String author) {
        ExtentTest test = getTest();
        if (test != null) {
            test.assignAuthor(author);
        }
    }
    
    /**
     * Assigns device to the current test
     */
    public static void assignDevice(String device) {
        ExtentTest test = getTest();
        if (test != null) {
            test.assignDevice(device);
        }
    }
    
    /**
     * Flushes and finalizes the report
     */
    public static synchronized void flushReports() {
        if (extentReports != null) {
            // Add summary information
            addExecutionSummary();
            
            extentReports.flush();
            
            long totalExecutionTime = System.currentTimeMillis() - suiteStartTime;
            logger.info("ExtentReports flushed successfully");
            logger.info("Total execution time: {} ms", totalExecutionTime);
            logger.info("Report generated at: {}", reportPath);
            
            // Clear ThreadLocal to prevent memory leaks
            extentTest.remove();
        }
    }
    
    /**
     * Adds execution summary to the report
     */
    private static void addExecutionSummary() {
        if (extentReports != null) {
            long executionTimeMinutes = (System.currentTimeMillis() - suiteStartTime) / (1000 * 60);
            
            ExtentTest summaryTest = extentReports.createTest("📊 Execution Summary", "Test execution summary and statistics");
            
            summaryTest.info(MarkupHelper.createLabel("Test Execution Summary", ExtentColor.BLUE));
            summaryTest.info("Total Tests Executed: " + totalTests);
            summaryTest.info("Tests Passed: " + passedTests);
            summaryTest.info("Tests Failed: " + failedTests);
            summaryTest.info("Tests Skipped: " + skippedTests);
            summaryTest.info("Total Execution Time: " + executionTimeMinutes + " minutes");
            
            double passPercentage = totalTests > 0 ? (double) passedTests / totalTests * 100 : 0;
            summaryTest.info("Pass Percentage: " + String.format("%.2f%%", passPercentage));
            
            // Add system resource information
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / (1024 * 1024);
            long totalMemory = runtime.totalMemory() / (1024 * 1024);
            long freeMemory = runtime.freeMemory() / (1024 * 1024);
            
            summaryTest.info("Memory Usage:");
            summaryTest.info("- Max Memory: " + maxMemory + " MB");
            summaryTest.info("- Total Memory: " + totalMemory + " MB");
            summaryTest.info("- Free Memory: " + freeMemory + " MB");
            summaryTest.info("- Used Memory: " + (totalMemory - freeMemory) + " MB");
        }
    }
    
    /**
     * Gets the report file path
     */
    public static String getReportPath() {
        return reportPath;
    }
    
    /**
     * Creates report directory if it doesn't exist
     */
    private static void createReportDirectory() {
        File reportDir = new File(getReportDirectory());
        if (!reportDir.exists()) {
            if (reportDir.mkdirs()) {
                logger.info("Created report directory: {}", reportDir.getAbsolutePath());
            } else {
                logger.error("Failed to create report directory: {}", reportDir.getAbsolutePath());
            }
        }
    }
    
    /**
     * Gets report directory path
     */
    private static String getReportDirectory() {
        return config.getProperty("report.directory", "test-output/extent-reports");
    }
    
    /**
     * Gets execution statistics
     */
    public static ExecutionStats getExecutionStats() {
        return new ExecutionStats(totalTests, passedTests, failedTests, skippedTests, 
            System.currentTimeMillis() - suiteStartTime);
    }
    
    /**
     * Inner class to hold execution statistics
     */
    public static class ExecutionStats {
        private final int totalTests;
        private final int passedTests;
        private final int failedTests;
        private final int skippedTests;
        private final long executionTime;
        
        public ExecutionStats(int totalTests, int passedTests, int failedTests, int skippedTests, long executionTime) {
            this.totalTests = totalTests;
            this.passedTests = passedTests;
            this.failedTests = failedTests;
            this.skippedTests = skippedTests;
            this.executionTime = executionTime;
        }
        
        // Getters
        public int getTotalTests() { return totalTests; }
        public int getPassedTests() { return passedTests; }
        public int getFailedTests() { return failedTests; }
        public int getSkippedTests() { return skippedTests; }
        public long getExecutionTime() { return executionTime; }
        public double getPassPercentage() { 
            return totalTests > 0 ? (double) passedTests / totalTests * 100 : 0; 
        }
    }
}