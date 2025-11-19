package com.phoenix.hrm.listeners;

import com.phoenix.hrm.core.driver.WebDriverFactory;
import com.phoenix.hrm.core.reporting.ExtentReportManager;
import com.phoenix.hrm.core.utils.ScreenshotUtils;
import com.phoenix.hrm.core.config.ConfigManager;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enhanced Screenshot Listener for Phoenix HRM Test Framework
 * 
 * Provides comprehensive screenshot management with:
 * - Automatic screenshot capture on test failure
 * - Optional screenshots on success and skip
 * - Multiple screenshot formats (file and base64)
 * - Integration with ExtentReports
 * - Configurable screenshot policies
 * - Screenshot cleanup and management
 * 
 * @author Phoenix HRM Test Team
 */
public class ScreenshotListener implements ITestListener {
    
    private static final Logger logger = LoggerFactory.getLogger(ScreenshotListener.class);
    
    // Configuration
    private static final ConfigManager config = ConfigManager.getInstance();
    private static final boolean SCREENSHOT_ON_FAILURE = Boolean.parseBoolean(
        config.getProperty("screenshot.on.failure", "true"));
    private static final boolean SCREENSHOT_ON_SUCCESS = Boolean.parseBoolean(
        config.getProperty("screenshot.on.success", "false"));
    private static final boolean SCREENSHOT_ON_SKIP = Boolean.parseBoolean(
        config.getProperty("screenshot.on.skip", "false"));
    private static final boolean SCREENSHOT_ALWAYS = Boolean.parseBoolean(
        config.getProperty("screenshot.always", "false"));
    
    // Screenshot statistics
    private static final AtomicInteger totalScreenshots = new AtomicInteger(0);
    private static final AtomicInteger failureScreenshots = new AtomicInteger(0);
    private static final AtomicInteger successScreenshots = new AtomicInteger(0);
    private static final AtomicInteger skipScreenshots = new AtomicInteger(0);
    
    // Screenshot tracking
    private static final ConcurrentHashMap<String, String> testScreenshots = new ConcurrentHashMap<>();
    private static final String SCREENSHOT_BASE_DIR = "test-output/screenshots";
    
    @Override
    public void onTestStart(ITestResult result) {
        // Optional: Capture screenshot at test start
        if (SCREENSHOT_ALWAYS) {
            captureScreenshot(result, "START", "Test start screenshot");
        }
    }
    
    @Override
    public void onTestSuccess(ITestResult result) {
        if (SCREENSHOT_ON_SUCCESS || SCREENSHOT_ALWAYS) {
            String screenshotPath = captureScreenshot(result, "PASSED", "Test passed screenshot");
            if (screenshotPath != null) {
                successScreenshots.incrementAndGet();
                ExtentReportManager.logPass("✅ Screenshot captured for passed test");
            }
        }
    }
    
    @Override
    public void onTestFailure(ITestResult result) {
        if (SCREENSHOT_ON_FAILURE || SCREENSHOT_ALWAYS) {
            String screenshotPath = captureScreenshot(result, "FAILED", "Test failure screenshot");
            if (screenshotPath != null) {
                failureScreenshots.incrementAndGet();
                ExtentReportManager.logFail("📷 Screenshot captured on test failure");
                
                // Add additional failure context
                captureFailureContext(result);
            }
        }
    }
    
    @Override
    public void onTestSkipped(ITestResult result) {
        if (SCREENSHOT_ON_SKIP || SCREENSHOT_ALWAYS) {
            String screenshotPath = captureScreenshot(result, "SKIPPED", "Test skipped screenshot");
            if (screenshotPath != null) {
                skipScreenshots.incrementAndGet();
                ExtentReportManager.logSkip("⏭️ Screenshot captured for skipped test");
            }
        }
    }
    
    /**
     * Captures screenshot for the given test result
     */
    private String captureScreenshot(ITestResult result, String status, String description) {
        try {
            WebDriver driver = WebDriverFactory.getDriver();
            if (driver == null) {
                logger.debug("No WebDriver instance available for screenshot capture");
                return null;
            }
            
            String testName = getTestName(result);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS"));
            String screenshotName = String.format("%s_%s_%s", status, testName, timestamp);
            
            // Capture screenshot to file
            String screenshotPath = ScreenshotUtils.captureScreenshot(driver, screenshotName);
            
            if (screenshotPath != null) {
                totalScreenshots.incrementAndGet();
                testScreenshots.put(getTestKey(result), screenshotPath);
                
                // Attach to ExtentReports
                ExtentReportManager.attachScreenshot(screenshotPath, description);
                
                // Also capture as base64 for better report integration
                String base64Screenshot = ScreenshotUtils.captureScreenshotAsBase64String(driver);
                if (base64Screenshot != null) {
                    ExtentReportManager.attachScreenshotBase64(base64Screenshot, description + " (Base64)");
                }
                
                logger.info("Screenshot captured: {} -> {}", testName, screenshotPath);
                return screenshotPath;
            }
            
        } catch (Exception e) {
            logger.error("Failed to capture screenshot for test {}: {}", getTestName(result), e.getMessage());
            ExtentReportManager.logWarning("Failed to capture screenshot: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Captures additional context for failed tests
     */
    private void captureFailureContext(ITestResult result) {
        try {
            WebDriver driver = WebDriverFactory.getDriver();
            if (driver == null) {
                return;
            }
            
            // Capture page source
            String pageSource = driver.getPageSource();
            if (pageSource != null && !pageSource.trim().isEmpty()) {
                // Save page source to file
                String testName = getTestName(result);
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
                String sourceFileName = String.format("FAILED_%s_%s_source.html", testName, timestamp);
                
                File sourceFile = new File(SCREENSHOT_BASE_DIR, sourceFileName);
                sourceFile.getParentFile().mkdirs();
                
                try (java.io.FileWriter writer = new java.io.FileWriter(sourceFile)) {
                    writer.write(pageSource);
                    ExtentReportManager.logInfo("Page source saved: " + sourceFile.getAbsolutePath());
                } catch (Exception e) {
                    logger.error("Failed to save page source: {}", e.getMessage());
                }
            }
            
            // Capture browser information
            captureBrowserInfo(driver, result);
            
            // Capture console logs (if supported)
            captureConsoleLogs(driver, result);
            
        } catch (Exception e) {
            logger.error("Failed to capture failure context: {}", e.getMessage());
        }
    }
    
    /**
     * Captures browser information
     */
    private void captureBrowserInfo(WebDriver driver, ITestResult result) {
        try {
            String currentUrl = driver.getCurrentUrl();
            String pageTitle = driver.getTitle();
            String windowHandle = driver.getWindowHandle();
            
            ExtentReportManager.logInfo("=== BROWSER CONTEXT ===");
            ExtentReportManager.logInfo("Current URL: " + currentUrl);
            ExtentReportManager.logInfo("Page Title: " + pageTitle);
            ExtentReportManager.logInfo("Window Handle: " + windowHandle);
            ExtentReportManager.logInfo("Browser: " + driver.getClass().getSimpleName());
            
            // Get window size
            try {
                org.openqa.selenium.Dimension windowSize = driver.manage().window().getSize();
                ExtentReportManager.logInfo("Window Size: " + windowSize.width + "x" + windowSize.height);
            } catch (Exception e) {
                logger.debug("Could not get window size: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            logger.error("Failed to capture browser info: {}", e.getMessage());
        }
    }
    
    /**
     * Captures console logs (if supported by the browser)
     */
    private void captureConsoleLogs(WebDriver driver, ITestResult result) {
        try {
            if (driver instanceof org.openqa.selenium.remote.RemoteWebDriver) {
                org.openqa.selenium.logging.LogEntries logEntries = 
                    driver.manage().logs().get(org.openqa.selenium.logging.LogType.BROWSER);
                
                if (logEntries != null && logEntries.getAll().size() > 0) {
                    ExtentReportManager.logInfo("=== BROWSER CONSOLE LOGS ===");
                    for (org.openqa.selenium.logging.LogEntry entry : logEntries) {
                        String logMessage = String.format("[%s] %s: %s", 
                            entry.getLevel(), 
                            new java.util.Date(entry.getTimestamp()),
                            entry.getMessage());
                        ExtentReportManager.logInfo(logMessage);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Console logs not available or failed to capture: {}", e.getMessage());
        }
    }
    
    /**
     * Gets formatted test name for screenshot naming
     */
    private String getTestName(ITestResult result) {
        String className = result.getTestClass().getName();
        String methodName = result.getMethod().getMethodName();
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        
        // Clean name for file system
        String testName = simpleName + "_" + methodName;
        return testName.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
    
    /**
     * Gets unique test key
     */
    private String getTestKey(ITestResult result) {
        return result.getTestClass().getName() + "#" + result.getMethod().getMethodName() +
               "#" + Thread.currentThread().getId();
    }
    
    /**
     * Gets screenshot statistics
     */
    public static ScreenshotStatistics getScreenshotStatistics() {
        return new ScreenshotStatistics(
            totalScreenshots.get(),
            failureScreenshots.get(),
            successScreenshots.get(),
            skipScreenshots.get()
        );
    }
    
    /**
     * Cleans up old screenshots based on retention policy
     */
    public static void cleanupOldScreenshots() {
        try {
            File screenshotDir = new File(SCREENSHOT_BASE_DIR);
            if (!screenshotDir.exists()) {
                return;
            }
            
            // Get retention days from config
            int retentionDays = Integer.parseInt(config.getProperty("screenshot.retention.days", "7"));
            long retentionMillis = retentionDays * 24 * 60 * 60 * 1000L;
            long cutoffTime = System.currentTimeMillis() - retentionMillis;
            
            File[] files = screenshotDir.listFiles();
            if (files != null) {
                int deletedCount = 0;
                for (File file : files) {
                    if (file.isFile() && file.lastModified() < cutoffTime) {
                        if (file.delete()) {
                            deletedCount++;
                        }
                    }
                }
                
                if (deletedCount > 0) {
                    logger.info("Cleaned up {} old screenshot files", deletedCount);
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to cleanup old screenshots: {}", e.getMessage());
        }
    }
    
    /**
     * Logs screenshot summary to ExtentReports
     */
    public static void logScreenshotSummaryToReport() {
        ExtentReportManager.createTest("📷 Screenshot Capture Summary", "Summary of screenshot capture activities");
        
        ScreenshotStatistics stats = getScreenshotStatistics();
        
        ExtentReportManager.logInfo("=== SCREENSHOT CAPTURE SUMMARY ===");
        ExtentReportManager.logInfo("Total Screenshots Captured: " + stats.getTotalScreenshots());
        ExtentReportManager.logInfo("Failure Screenshots: " + stats.getFailureScreenshots());
        ExtentReportManager.logInfo("Success Screenshots: " + stats.getSuccessScreenshots());
        ExtentReportManager.logInfo("Skip Screenshots: " + stats.getSkipScreenshots());
        
        // Add configuration info
        ExtentReportManager.logInfo("=== SCREENSHOT CONFIGURATION ===");
        ExtentReportManager.logInfo("Screenshot on Failure: " + SCREENSHOT_ON_FAILURE);
        ExtentReportManager.logInfo("Screenshot on Success: " + SCREENSHOT_ON_SUCCESS);
        ExtentReportManager.logInfo("Screenshot on Skip: " + SCREENSHOT_ON_SKIP);
        ExtentReportManager.logInfo("Screenshot Always: " + SCREENSHOT_ALWAYS);
        
        // Add recommendations
        if (stats.getTotalScreenshots() == 0) {
            ExtentReportManager.logWarning("No screenshots captured - check configuration");
        } else if (stats.getFailureScreenshots() > 0) {
            ExtentReportManager.logInfo("Screenshots available for failure analysis 📸");
        }
        
        if (stats.getTotalScreenshots() > 100) {
            ExtentReportManager.logWarning("High number of screenshots - consider cleanup policy");
        }
    }
    
    /**
     * Clears screenshot statistics
     */
    public static void clearScreenshotStatistics() {
        totalScreenshots.set(0);
        failureScreenshots.set(0);
        successScreenshots.set(0);
        skipScreenshots.set(0);
        testScreenshots.clear();
        logger.debug("Screenshot statistics cleared");
    }
    
    /**
     * Inner class to hold screenshot statistics
     */
    public static class ScreenshotStatistics {
        private final int totalScreenshots;
        private final int failureScreenshots;
        private final int successScreenshots;
        private final int skipScreenshots;
        
        public ScreenshotStatistics(int totalScreenshots, int failureScreenshots, 
                                  int successScreenshots, int skipScreenshots) {
            this.totalScreenshots = totalScreenshots;
            this.failureScreenshots = failureScreenshots;
            this.successScreenshots = successScreenshots;
            this.skipScreenshots = skipScreenshots;
        }
        
        // Getters
        public int getTotalScreenshots() { return totalScreenshots; }
        public int getFailureScreenshots() { return failureScreenshots; }
        public int getSuccessScreenshots() { return successScreenshots; }
        public int getSkipScreenshots() { return skipScreenshots; }
        
        public double getFailureScreenshotPercentage() {
            return totalScreenshots > 0 ? (double) failureScreenshots / totalScreenshots * 100 : 0;
        }
        
        @Override
        public String toString() {
            return String.format("Screenshot Stats: Total=%d, Failure=%d, Success=%d, Skip=%d",
                totalScreenshots, failureScreenshots, successScreenshots, skipScreenshots);
        }
    }
}