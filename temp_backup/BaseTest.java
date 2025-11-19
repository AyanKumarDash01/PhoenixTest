package com.phoenix.hrm.tests;

import com.phoenix.hrm.core.config.ConfigManager;
import com.phoenix.hrm.core.driver.WebDriverFactory;
import com.phoenix.hrm.core.utils.ScreenshotUtils;
import com.phoenix.hrm.pages.DashboardPage;
import com.phoenix.hrm.pages.LoginPage;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.lang.reflect.Method;

/**
 * BaseTest class providing common test infrastructure and setup/teardown operations.
 * All test classes should extend this base class for consistent test execution.
 * 
 * Features:
 * - WebDriver lifecycle management with proper cleanup
 * - Test method logging and reporting
 * - Screenshot capture on test failures
 * - Configuration management integration
 * - Cross-browser testing support
 * - Parallel execution support with ThreadLocal
 * - Test retry mechanism support
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 1.0
 */
public class BaseTest {
    
    protected static final Logger logger = LoggerFactory.getLogger(BaseTest.class);
    protected static final ConfigManager config = ConfigManager.getInstance();
    
    // ThreadLocal instances for parallel execution
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<LoginPage> loginPageThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<DashboardPage> dashboardPageThreadLocal = new ThreadLocal<>();
    
    /**
     * Gets WebDriver instance for current thread
     * 
     * @return WebDriver instance
     */
    protected WebDriver getDriver() {
        return driverThreadLocal.get();
    }
    
    /**
     * Gets LoginPage instance for current thread
     * 
     * @return LoginPage instance
     */
    protected LoginPage getLoginPage() {
        return loginPageThreadLocal.get();
    }
    
    /**
     * Gets DashboardPage instance for current thread
     * 
     * @return DashboardPage instance
     */
    protected DashboardPage getDashboardPage() {
        return dashboardPageThreadLocal.get();
    }
    
    /**
     * Suite-level setup - runs once before all tests
     */
    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() {
        logger.info("=== Phoenix HRM Test Suite Starting ===");
        logger.info("Environment: {}", System.getProperty("env", "dev"));
        logger.info("Browser: {}", config.getBrowser());
        logger.info("Base URL: {}", config.getBaseUrl());
        logger.info("Headless Mode: {}", config.isHeadless());
        logger.info("Parallel Execution: {}", config.isParallelExecution());
        logger.info("==========================================");
        
        // Clean up old screenshots if needed
        try {
            int deletedCount = ScreenshotUtils.cleanupOldScreenshots(7); // Keep last 7 days
            if (deletedCount > 0) {
                logger.info("Cleaned up {} old screenshots", deletedCount);
            }
        } catch (Exception e) {
            logger.warn("Failed to cleanup old screenshots", e);
        }
    }
    
    /**
     * Class-level setup - runs once before each test class
     */
    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        logger.info("Setting up test class: {}", this.getClass().getSimpleName());
    }
    
    /**
     * Method-level setup - runs before each test method
     * 
     * @param method Test method being executed
     */
    @BeforeMethod(alwaysRun = true)
    public void beforeMethod(Method method) {
        String testName = method.getName();
        String className = this.getClass().getSimpleName();
        
        logger.info("==========================================");
        logger.info("Starting Test: {}.{}", className, testName);
        logger.info("Thread: {}", Thread.currentThread().getName());
        logger.info("==========================================");
        
        try {
            // Create WebDriver instance for current thread
            WebDriver driver = WebDriverFactory.createDriver();
            driverThreadLocal.set(driver);
            
            // Initialize page objects
            LoginPage loginPage = new LoginPage(driver);
            loginPageThreadLocal.set(loginPage);
            
            logger.info("WebDriver and page objects initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize WebDriver for test: {}", testName, e);
            throw new RuntimeException("Test setup failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Method-level teardown - runs after each test method
     * 
     * @param result Test result containing test execution information
     */
    @AfterMethod(alwaysRun = true)
    public void afterMethod(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String className = result.getTestClass().getRealClass().getSimpleName();
        
        try {
            // Handle test failure
            if (result.getStatus() == ITestResult.FAILURE) {
                logger.error("Test FAILED: {}.{}", className, testName);
                logger.error("Failure reason: {}", result.getThrowable().getMessage());
                
                // Capture screenshot on failure
                if (config.isScreenshotOnFailure() && WebDriverFactory.hasDriver()) {
                    String screenshotPath = ScreenshotUtils.captureFailureScreenshot(
                        className + "_" + testName, result.getThrowable());
                    if (screenshotPath != null) {
                        logger.info("Failure screenshot captured: {}", screenshotPath);
                        // Set screenshot path for reporting tools
                        System.setProperty("screenshot.path", screenshotPath);
                    }
                }
            } else if (result.getStatus() == ITestResult.SUCCESS) {
                logger.info("Test PASSED: {}.{}", className, testName);
            } else if (result.getStatus() == ITestResult.SKIP) {
                logger.warn("Test SKIPPED: {}.{}", className, testName);
            }
            
        } catch (Exception e) {
            logger.warn("Error in afterMethod for test: {}", testName, e);
        } finally {
            // Clean up WebDriver and ThreadLocal variables
            cleanup();
            
            long duration = result.getEndMillis() - result.getStartMillis();
            logger.info("Test completed: {}.{} - Duration: {}ms", className, testName, duration);
            logger.info("==========================================");
        }
    }
    
    /**
     * Class-level teardown - runs once after each test class
     */
    @AfterClass(alwaysRun = true)
    public void afterClass() {
        logger.info("Tearing down test class: {}", this.getClass().getSimpleName());
    }
    
    /**
     * Suite-level teardown - runs once after all tests
     */
    @AfterSuite(alwaysRun = true)
    public void afterSuite() {
        logger.info("=== Phoenix HRM Test Suite Completed ===");
    }
    
    /**
     * Performs login as admin user
     * 
     * @return DashboardPage after successful login
     */
    protected DashboardPage loginAsAdmin() {
        logger.info("Performing admin login");
        LoginPage loginPage = getLoginPage();
        
        // Navigate to login page
        loginPage.navigateToLoginPage();
        
        // Perform admin login
        DashboardPage dashboardPage = loginPage.loginAsAdmin();
        dashboardPageThreadLocal.set(dashboardPage);
        
        // Verify login success
        if (dashboardPage.isPageLoaded()) {
            logger.info("Admin login successful");
            return dashboardPage;
        } else {
            throw new RuntimeException("Admin login failed - Dashboard page not loaded");
        }
    }
    
    /**
     * Performs login with custom credentials
     * 
     * @param username Username for login
     * @param password Password for login
     * @return DashboardPage after successful login
     */
    protected DashboardPage loginWithCredentials(String username, String password) {
        logger.info("Performing login with username: {}", username);
        LoginPage loginPage = getLoginPage();
        
        // Navigate to login page
        loginPage.navigateToLoginPage();
        
        // Perform login
        DashboardPage dashboardPage = loginPage.login(username, password);
        dashboardPageThreadLocal.set(dashboardPage);
        
        // Verify login success
        if (dashboardPage.isPageLoaded()) {
            logger.info("Login successful for user: {}", username);
            return dashboardPage;
        } else {
            throw new RuntimeException("Login failed for user: " + username);
        }
    }
    
    /**
     * Performs logout operation
     * 
     * @return LoginPage after logout
     */
    protected LoginPage logout() {
        logger.info("Performing logout");
        DashboardPage dashboardPage = getDashboardPage();
        
        if (dashboardPage != null) {
            LoginPage loginPage = dashboardPage.logout();
            loginPageThreadLocal.set(loginPage);
            logger.info("Logout completed");
            return loginPage;
        } else {
            logger.warn("No dashboard page available for logout");
            return new LoginPage(getDriver());
        }
    }
    
    /**
     * Navigates to application base URL
     */
    protected void navigateToApplication() {
        String baseUrl = config.getBaseUrl();
        logger.info("Navigating to application: {}", baseUrl);
        getDriver().get(baseUrl);
    }
    
    /**
     * Waits for specified seconds (use sparingly, prefer explicit waits)
     * 
     * @param seconds Seconds to wait
     */
    protected void waitForSeconds(int seconds) {
        try {
            logger.debug("Waiting for {} seconds", seconds);
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Wait interrupted", e);
        }
    }
    
    /**
     * Gets current test method name
     * 
     * @return Current test method name
     */
    protected String getCurrentTestMethodName() {
        return Thread.currentThread().getStackTrace()[2].getMethodName();
    }
    
    /**
     * Asserts that a condition is true with custom message
     * 
     * @param condition Condition to check
     * @param message Custom error message
     */
    protected void assertTrue(boolean condition, String message) {
        if (!condition) {
            logger.error("Assertion failed: {}", message);
            throw new AssertionError(message);
        }
    }
    
    /**
     * Asserts that a condition is false with custom message
     * 
     * @param condition Condition to check
     * @param message Custom error message
     */
    protected void assertFalse(boolean condition, String message) {
        if (condition) {
            logger.error("Assertion failed: {}", message);
            throw new AssertionError(message);
        }
    }
    
    /**
     * Asserts that two strings are equal
     * 
     * @param actual Actual value
     * @param expected Expected value
     * @param message Custom error message
     */
    protected void assertEquals(String actual, String expected, String message) {
        if (!java.util.Objects.equals(actual, expected)) {
            logger.error("Assertion failed: {}. Expected: '{}', Actual: '{}'", message, expected, actual);
            throw new AssertionError(String.format("%s. Expected: '%s', Actual: '%s'", message, expected, actual));
        }
    }
    
    /**
     * Clean up resources for current thread
     */
    private void cleanup() {
        try {
            // Close WebDriver
            WebDriverFactory.closeDriver();
            
            // Clear ThreadLocal variables
            driverThreadLocal.remove();
            loginPageThreadLocal.remove();
            dashboardPageThreadLocal.remove();
            
        } catch (Exception e) {
            logger.warn("Error during cleanup", e);
        }
    }
    
    /**
     * Skip test with reason
     * 
     * @param reason Reason for skipping
     */
    protected void skipTest(String reason) {
        logger.warn("Skipping test: {}", reason);
        org.testng.SkipException skipException = new org.testng.SkipException(reason);
        throw skipException;
    }
    
    /**
     * Fails test with custom message
     * 
     * @param message Failure message
     */
    protected void failTest(String message) {
        logger.error("Test failed: {}", message);
        org.testng.Assert.fail(message);
    }
}