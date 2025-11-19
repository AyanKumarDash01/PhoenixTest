package com.phoenix.hrm.core.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.Markup;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.phoenix.hrm.core.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced Test Reporter for Phoenix HRM Test Automation Framework
 * Provides comprehensive test reporting with screenshots, logs, and metrics
 */
public class TestReporter {
    
    private static final Logger logger = LoggerFactory.getLogger(TestReporter.class);
    private static ExtentReports extentReports;
    private static final ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();
    private static final ConcurrentHashMap<String, ExtentTest> testMap = new ConcurrentHashMap<>();
    private static final String REPORTS_DIR = "test-reports";
    private static final String SCREENSHOTS_DIR = REPORTS_DIR + "/screenshots";
    private static final ConfigManager config = ConfigManager.getInstance();
    
    // Report file paths
    private static String currentReportPath;
    private static String currentScreenshotDir;
    
    // Test execution statistics
    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;
    private static int skippedTests = 0;
    
    /**
     * Initialize ExtentReports
     */
    public static synchronized void initializeReports() {
        if (extentReports == null) {
            try {
                setupReportDirectories();
                
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
                currentReportPath = REPORTS_DIR + "/PhoenixHRM_TestReport_" + timestamp + ".html";
                currentScreenshotDir = SCREENSHOTS_DIR + "/" + timestamp;
                
                // Create screenshot directory for this run
                Files.createDirectories(Paths.get(currentScreenshotDir));
                
                ExtentSparkReporter sparkReporter = new ExtentSparkReporter(currentReportPath);
                configureSparkReporter(sparkReporter);
                
                extentReports = new ExtentReports();
                extentReports.attachReporter(sparkReporter);
                setSystemInfo();
                
                logger.info("ExtentReports initialized. Report path: {}", currentReportPath);
                
            } catch (IOException e) {
                logger.error("Failed to initialize ExtentReports", e);
                throw new RuntimeException("Report initialization failed", e);
            }
        }
    }
    
    /**
     * Setup report directories
     */
    private static void setupReportDirectories() throws IOException {
        Files.createDirectories(Paths.get(REPORTS_DIR));
        Files.createDirectories(Paths.get(SCREENSHOTS_DIR));
        logger.debug("Report directories created successfully");
    }
    
    /**
     * Configure Spark Reporter
     */
    private static void configureSparkReporter(ExtentSparkReporter sparkReporter) {
        sparkReporter.config().setDocumentTitle("Phoenix HRM - Test Automation Report");
        sparkReporter.config().setReportName("Phoenix HRM Test Suite Results");
        sparkReporter.config().setTheme(Theme.STANDARD);
        sparkReporter.config().setTimeStampFormat("EEEE, MMMM dd, yyyy, hh:mm:ss a '('zzz')'");
        sparkReporter.config().setEncoding("utf-8");
        
        // Custom CSS for better styling
        sparkReporter.config().setCss(getCustomCSS());
        
        // Custom JavaScript for enhanced functionality
        sparkReporter.config().setJs(getCustomJS());
    }
    
    /**
     * Set system information in report
     */
    private static void setSystemInfo() {
        extentReports.setSystemInfo("Application", "Phoenix HRM");
        extentReports.setSystemInfo("Test Environment", config.getEnvironment());
        extentReports.setSystemInfo("Browser", config.getBrowser());
        extentReports.setSystemInfo("Operating System", System.getProperty("os.name"));
        extentReports.setSystemInfo("Java Version", System.getProperty("java.version"));
        extentReports.setSystemInfo("User", System.getProperty("user.name"));
        extentReports.setSystemInfo("Base URL", config.getBaseUrl());
        extentReports.setSystemInfo("Test Data Environment", config.getProperty("test.data.environment", "default"));
        extentReports.setSystemInfo("Execution Mode", config.isHeadless() ? "Headless" : "UI");
        extentReports.setSystemInfo("Parallel Execution", config.getProperty("test.parallel.enabled", "false"));
    }
    
    /**
     * Start a test case
     */
    public static synchronized ExtentTest startTest(String testName) {
        return startTest(testName, "");
    }
    
    /**
     * Start a test case with description
     */
    public static synchronized ExtentTest startTest(String testName, String description) {
        if (extentReports == null) {
            initializeReports();
        }
        
        ExtentTest test = extentReports.createTest(testName, description);
        extentTest.set(test);
        testMap.put(Thread.currentThread().getName() + "_" + testName, test);
        totalTests++;
        
        logger.info("Started test: {}", testName);
        return test;
    }
    
    /**
     * Start a test node (sub-test)
     */
    public static ExtentTest startTestNode(String nodeName) {
        return startTestNode(nodeName, "");
    }
    
    /**
     * Start a test node with description
     */
    public static ExtentTest startTestNode(String nodeName, String description) {
        ExtentTest parentTest = getExtentTest();
        if (parentTest != null) {
            ExtentTest childTest = parentTest.createNode(nodeName, description);
            logger.debug("Started test node: {} under parent: {}", nodeName, parentTest.getModel().getName());
            return childTest;
        } else {
            logger.warn("No parent test found for node: {}", nodeName);
            return startTest(nodeName, description);
        }
    }
    
    /**
     * Get current ExtentTest instance
     */
    public static ExtentTest getExtentTest() {
        return extentTest.get();
    }
    
    /**
     * Log info message
     */
    public static void logInfo(String message) {
        ExtentTest test = getExtentTest();
        if (test != null) {
            test.log(Status.INFO, message);
            logger.info(message);
        }
    }
    
    /**
     * Log pass message
     */
    public static void logPass(String message) {
        ExtentTest test = getExtentTest();
        if (test != null) {
            test.log(Status.PASS, MarkupHelper.createLabel(message, ExtentColor.GREEN));
            logger.info("PASS: {}", message);
        }
    }
    
    /**
     * Log fail message
     */
    public static void logFail(String message) {
        ExtentTest test = getExtentTest();
        if (test != null) {
            test.log(Status.FAIL, MarkupHelper.createLabel(message, ExtentColor.RED));
            logger.error("FAIL: {}", message);
        }
    }
    
    /**
     * Log warning message
     */
    public static void logWarning(String message) {
        ExtentTest test = getExtentTest();
        if (test != null) {
            test.log(Status.WARNING, MarkupHelper.createLabel(message, ExtentColor.YELLOW));
            logger.warn("WARNING: {}", message);
        }
    }
    
    /**
     * Log skip message
     */
    public static void logSkip(String message) {
        ExtentTest test = getExtentTest();
        if (test != null) {
            test.log(Status.SKIP, MarkupHelper.createLabel(message, ExtentColor.ORANGE));
            logger.info("SKIP: {}", message);
        }
    }
    
    /**
     * Log exception
     */
    public static void logException(Throwable throwable) {
        ExtentTest test = getExtentTest();
        if (test != null) {
            test.log(Status.FAIL, throwable);
            logger.error("Exception logged in test report", throwable);
        }
    }
    
    /**
     * Add screenshot to report
     */
    public static void addScreenshot(String description) {
        addScreenshot(description, null);
    }
    
    /**
     * Add screenshot with custom image data
     */
    public static void addScreenshot(String description, byte[] imageData) {
        ExtentTest test = getExtentTest();
        if (test != null && imageData != null && imageData.length > 0) {
            try {
                String base64Image = Base64.getEncoder().encodeToString(imageData);
                test.log(Status.INFO, description, 
                    MediaEntityBuilder.createScreenCaptureFromBase64String(base64Image).build());
                logger.debug("Screenshot added to report: {}", description);
            } catch (Exception e) {
                logger.error("Failed to add screenshot to report", e);
            }
        }
    }
    
    /**
     * Save screenshot to file and add to report
     */
    public static void saveAndAddScreenshot(String description, byte[] imageData) {
        if (imageData != null && imageData.length > 0) {
            try {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
                String fileName = "screenshot_" + timestamp + ".png";
                String filePath = currentScreenshotDir + "/" + fileName;
                
                Files.write(Paths.get(filePath), imageData);
                
                ExtentTest test = getExtentTest();
                if (test != null) {
                    test.log(Status.INFO, description, 
                        MediaEntityBuilder.createScreenCaptureFromPath(filePath).build());
                    logger.debug("Screenshot saved and added to report: {}", filePath);
                }
                
            } catch (IOException e) {
                logger.error("Failed to save screenshot", e);
                // Fallback to base64 embedding
                addScreenshot(description, imageData);
            }
        }
    }
    
    /**
     * Add code block to report
     */
    public static void addCodeBlock(String title, String code, String language) {
        ExtentTest test = getExtentTest();
        if (test != null) {
            test.log(Status.INFO, title);
            test.log(Status.INFO, MarkupHelper.createCodeBlock(code, language));
        }
    }
    
    /**
     * Add table to report
     */
    public static void addTable(String title, String[][] tableData) {
        ExtentTest test = getExtentTest();
        if (test != null) {
            test.log(Status.INFO, title);
            test.log(Status.INFO, MarkupHelper.createTable(tableData));
        }
    }
    
    /**
     * Add JSON data to report
     */
    public static void addJsonData(String title, String jsonData) {
        ExtentTest test = getExtentTest();
        if (test != null) {
            test.log(Status.INFO, title);
            test.log(Status.INFO, MarkupHelper.createCodeBlock(jsonData, "json"));
        }
    }
    
    /**
     * Set test category
     */
    public static void setCategory(String... categories) {
        ExtentTest test = getExtentTest();
        if (test != null) {
            test.assignCategory(categories);
            logger.debug("Test categories assigned: {}", String.join(", ", categories));
        }
    }
    
    /**
     * Set test author
     */
    public static void setAuthor(String... authors) {
        ExtentTest test = getExtentTest();
        if (test != null) {
            test.assignAuthor(authors);
            logger.debug("Test authors assigned: {}", String.join(", ", authors));
        }
    }
    
    /**
     * Set test device
     */
    public static void setDevice(String... devices) {
        ExtentTest test = getExtentTest();
        if (test != null) {
            test.assignDevice(devices);
        }
    }
    
    /**
     * End current test
     */
    public static void endTest() {
        extentTest.remove();
        String threadName = Thread.currentThread().getName();
        testMap.entrySet().removeIf(entry -> entry.getKey().startsWith(threadName + "_"));
    }
    
    /**
     * End test with status
     */
    public static void endTest(Status status, String message) {
        ExtentTest test = getExtentTest();
        if (test != null) {
            switch (status) {
                case PASS:
                    logPass(message);
                    passedTests++;
                    break;
                case FAIL:
                    logFail(message);
                    failedTests++;
                    break;
                case SKIP:
                    logSkip(message);
                    skippedTests++;
                    break;
                default:
                    logInfo(message);
            }
        }
        endTest();
    }
    
    /**
     * Flush and finalize reports
     */
    public static synchronized void flushReports() {
        if (extentReports != null) {
            extentReports.flush();
            logger.info("ExtentReports flushed. Report available at: {}", currentReportPath);
            logTestSummary();
        }
    }
    
    /**
     * Log test execution summary
     */
    private static void logTestSummary() {
        logger.info("\n" +
            "========================================\n" +
            "          TEST EXECUTION SUMMARY       \n" +
            "========================================\n" +
            "Total Tests: {}\n" +
            "Passed: {} ({}%)\n" +
            "Failed: {} ({}%)\n" +
            "Skipped: {} ({}%)\n" +
            "Report: {}\n" +
            "========================================",
            totalTests,
            passedTests, totalTests > 0 ? (passedTests * 100 / totalTests) : 0,
            failedTests, totalTests > 0 ? (failedTests * 100 / totalTests) : 0,
            skippedTests, totalTests > 0 ? (skippedTests * 100 / totalTests) : 0,
            new File(currentReportPath).getAbsolutePath()
        );
    }
    
    /**
     * Get report path
     */
    public static String getReportPath() {
        return currentReportPath;
    }
    
    /**
     * Get test statistics
     */
    public static String getTestStatistics() {
        return String.format("Total: %d, Passed: %d, Failed: %d, Skipped: %d", 
            totalTests, passedTests, failedTests, skippedTests);
    }
    
    /**
     * Reset statistics (useful for multiple test runs)
     */
    public static void resetStatistics() {
        totalTests = 0;
        passedTests = 0;
        failedTests = 0;
        skippedTests = 0;
    }
    
    /**
     * Custom CSS for report styling
     */
    private static String getCustomCSS() {
        return 
            ".brand-logo { color: #1976d2 !important; }" +
            ".nav-wrapper { background-color: #1976d2 !important; }" +
            ".test-view .test-list .test-item.active { border-left: 3px solid #1976d2; }" +
            ".collapsible-header { background-color: #f5f5f5; }" +
            ".pass { color: #4caf50 !important; }" +
            ".fail { color: #f44336 !important; }" +
            ".warning { color: #ff9800 !important; }" +
            ".skip { color: #9e9e9e !important; }";
    }
    
    /**
     * Custom JavaScript for enhanced functionality
     */
    private static String getCustomJS() {
        return 
            "$(document).ready(function() {" +
            "  $('body').addClass('phoenix-hrm-report');" +
            "  $('.brand-logo').text('Phoenix HRM - Test Report');" +
            "});"; 
    }
}
