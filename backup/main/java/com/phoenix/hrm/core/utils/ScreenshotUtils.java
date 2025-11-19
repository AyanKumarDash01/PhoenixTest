package com.phoenix.hrm.core.utils;

import com.phoenix.hrm.core.config.ConfigManager;
import com.phoenix.hrm.core.driver.WebDriverFactory;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for capturing screenshots during test execution.
 * Provides methods for taking screenshots on test failures and saving them with timestamps.
 * 
 * Features:
 * - Automatic screenshot capture on test failures
 * - Timestamped screenshot file names
 * - Configurable screenshot directory
 * - Base64 encoded screenshots for reports
 * - Error handling for screenshot failures
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 1.0
 */
public class ScreenshotUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(ScreenshotUtils.class);
    private static final ConfigManager config = ConfigManager.getInstance();
    private static final String SCREENSHOT_DIR = "screenshots";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS");
    
    /**
     * Private constructor to prevent instantiation
     */
    private ScreenshotUtils() {
        throw new UnsupportedOperationException("ScreenshotUtils is a utility class and cannot be instantiated");
    }
    
    /**
     * Captures screenshot and saves it to file
     * 
     * @param testName Name of the test for screenshot file naming
     * @return File path of captured screenshot
     */
    public static String captureScreenshot(String testName) {
        WebDriver driver = WebDriverFactory.getDriver();
        if (driver == null) {
            logger.warn("No WebDriver instance available for screenshot capture");
            return null;
        }
        
        return captureScreenshot(driver, testName);
    }
    
    /**
     * Captures screenshot with specific WebDriver instance
     * 
     * @param driver WebDriver instance
     * @param testName Name of the test for screenshot file naming
     * @return File path of captured screenshot
     */
    public static String captureScreenshot(WebDriver driver, String testName) {
        try {
            // Create screenshots directory if it doesn't exist
            createScreenshotDirectory();
            
            // Generate timestamped filename
            String timestamp = LocalDateTime.now().format(DATE_FORMAT);
            String fileName = String.format("%s_%s.png", sanitizeFileName(testName), timestamp);
            String filePath = SCREENSHOT_DIR + File.separator + fileName;
            
            // Capture screenshot
            TakesScreenshot takesScreenshot = (TakesScreenshot) driver;
            File sourceFile = takesScreenshot.getScreenshotAs(OutputType.FILE);
            File destFile = new File(filePath);
            
            // Save screenshot to file
            FileUtils.copyFile(sourceFile, destFile);
            
            logger.info("Screenshot captured successfully: {}", filePath);
            return destFile.getAbsolutePath();
            
        } catch (IOException e) {
            logger.error("Failed to capture screenshot for test: {}", testName, e);
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error while capturing screenshot for test: {}", testName, e);
            return null;
        }
    }
    
    /**
     * Captures screenshot and returns as Base64 string for embedding in reports
     * 
     * @param testName Name of the test
     * @return Base64 encoded screenshot string
     */
    public static String captureScreenshotAsBase64(String testName) {
        WebDriver driver = WebDriverFactory.getDriver();
        if (driver == null) {
            logger.warn("No WebDriver instance available for screenshot capture");
            return null;
        }
        
        return captureScreenshotAsBase64(driver, testName);
    }
    
    /**
     * Captures screenshot as Base64 string with specific WebDriver instance
     * 
     * @param driver WebDriver instance
     * @param testName Name of the test
     * @return Base64 encoded screenshot string
     */
    public static String captureScreenshotAsBase64(WebDriver driver, String testName) {
        try {
            TakesScreenshot takesScreenshot = (TakesScreenshot) driver;
            String base64Screenshot = takesScreenshot.getScreenshotAs(OutputType.BASE64);
            
            logger.debug("Screenshot captured as Base64 for test: {}", testName);
            return base64Screenshot;
            
        } catch (Exception e) {
            logger.error("Failed to capture screenshot as Base64 for test: {}", testName, e);
            return null;
        }
    }
    
    /**
     * Captures screenshot for test failure
     * 
     * @param testName Name of the failed test
     * @param throwable Exception that caused the failure
     * @return File path of captured screenshot
     */
    public static String captureFailureScreenshot(String testName, Throwable throwable) {
        if (!config.isScreenshotOnFailure()) {
            logger.debug("Screenshot on failure is disabled");
            return null;
        }
        
        logger.info("Capturing failure screenshot for test: {} - Error: {}", testName, throwable.getMessage());
        String screenshotPath = captureScreenshot(testName + "_FAILED");
        
        if (screenshotPath != null) {
            logger.info("Failure screenshot saved: {}", screenshotPath);
        }
        
        return screenshotPath;
    }
    
    /**
     * Creates screenshot directory if it doesn't exist
     */
    private static void createScreenshotDirectory() {
        File screenshotDir = new File(SCREENSHOT_DIR);
        if (!screenshotDir.exists()) {
            boolean created = screenshotDir.mkdirs();
            if (created) {
                logger.debug("Created screenshot directory: {}", screenshotDir.getAbsolutePath());
            } else {
                logger.warn("Failed to create screenshot directory: {}", screenshotDir.getAbsolutePath());
            }
        }
    }
    
    /**
     * Sanitizes file name by removing invalid characters
     * 
     * @param fileName Original file name
     * @return Sanitized file name
     */
    private static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "screenshot";
        }
        
        // Replace invalid characters with underscores
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
    
    /**
     * Gets screenshot directory path
     * 
     * @return Screenshot directory path
     */
    public static String getScreenshotDirectory() {
        return new File(SCREENSHOT_DIR).getAbsolutePath();
    }
    
    /**
     * Cleans up old screenshots (older than specified days)
     * 
     * @param daysToKeep Number of days to keep screenshots
     * @return Number of files deleted
     */
    public static int cleanupOldScreenshots(int daysToKeep) {
        File screenshotDir = new File(SCREENSHOT_DIR);
        if (!screenshotDir.exists()) {
            return 0;
        }
        
        long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60L * 60L * 1000L);
        File[] files = screenshotDir.listFiles();
        int deletedCount = 0;
        
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        deletedCount++;
                        logger.debug("Deleted old screenshot: {}", file.getName());
                    }
                }
            }
        }
        
        if (deletedCount > 0) {
            logger.info("Cleaned up {} old screenshots (older than {} days)", deletedCount, daysToKeep);
        }
        
        return deletedCount;
    }
}