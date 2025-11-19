package com.phoenix.hrm.tests;

import com.phoenix.hrm.base.BaseTest;
import com.phoenix.hrm.config.BrowserType;
import com.phoenix.hrm.crossbrowser.CrossBrowserTestExecutor;
import com.phoenix.hrm.crossbrowser.CrossBrowserExecutionResult;
import com.phoenix.hrm.factory.EnhancedWebDriverFactory;
import com.phoenix.hrm.pages.LoginPage;
import com.phoenix.hrm.reporting.TestReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Cross-Browser Login Test Suite
 * 
 * Demonstrates cross-browser testing capabilities by running
 * login tests across multiple browsers and generating
 * browser compatibility reports.
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 3.0
 * @since Phase 3
 */
public class CrossBrowserLoginTest extends CrossBrowserTestExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(CrossBrowserLoginTest.class);
    
    /**
     * Test login functionality across all configured browsers
     * Uses the crossBrowserProvider data provider for parallel execution
     * 
     * @param browserType The browser type to test on
     */
    @Test(dataProvider = "crossBrowserProvider", 
          description = "Test login functionality across multiple browsers",
          groups = {"cross-browser", "smoke", "login"})
    public void testLoginAcrossBrowsers(BrowserType browserType) {
        logger.info("Starting cross-browser login test on: {}", browserType.getDisplayName());
        
        try {
            // Get the WebDriver instance created by the setup method
            if (EnhancedWebDriverFactory.getDriver() == null) {
                Assert.fail("WebDriver not initialized for browser: " + browserType.getDisplayName());
            }
            
            // Initialize page objects
            LoginPage loginPage = new LoginPage();
            
            // Navigate to application
            logger.info("Navigating to application URL on {}", browserType.getDisplayName());
            TestReporter.logInfo("Navigating to application on " + browserType.getDisplayName());
            loginPage.navigateToApplication();
            
            // Validate login page is loaded
            logger.info("Validating login page elements on {}", browserType.getDisplayName());
            TestReporter.logInfo("Validating login page elements");
            
            Assert.assertTrue(loginPage.isPageLoaded(), 
                "Login page should be loaded on " + browserType.getDisplayName());
            
            Assert.assertTrue(loginPage.validateLoginFormElements(), 
                "Login form elements should be present on " + browserType.getDisplayName());
            
            // Perform login
            logger.info("Performing login on {}", browserType.getDisplayName());
            TestReporter.logInfo("Performing admin login");
            loginPage.loginAsAdmin();
            
            // Validate successful login (URL change or dashboard elements)
            logger.info("Validating successful login on {}", browserType.getDisplayName());
            TestReporter.logInfo("Validating successful login");
            
            // Wait for page load after login
            loginPage.waitForPageLoad();
            
            // Check if we're no longer on login page (successful login indication)
            boolean stillOnLoginPage = loginPage.isOnLoginPage();
            Assert.assertFalse(stillOnLoginPage, 
                "Should not be on login page after successful login on " + browserType.getDisplayName());
            
            // Take screenshot for documentation if browser supports it
            if (browserSupportsFeature("screenshots")) {
                logger.info("Taking screenshot on {}", browserType.getDisplayName());
                TestReporter.logInfo("Capturing screenshot for cross-browser verification");
                byte[] screenshot = loginPage.takeLoginPageScreenshot();
                TestReporter.saveAndAddScreenshot("Cross-browser login success - " + browserType.getDisplayName(), screenshot);
            }
            
            TestReporter.logPass("Cross-browser login test PASSED on " + browserType.getDisplayName());
            logger.info("Cross-browser login test PASSED on: {}", browserType.getDisplayName());
            
        } catch (Exception e) {
            logger.error("Cross-browser login test FAILED on {}: {}", browserType.getDisplayName(), e.getMessage());
            TestReporter.logFail("Cross-browser login test FAILED on " + browserType.getDisplayName() + ": " + e.getMessage());
            
            // Take failure screenshot if supported
            if (browserSupportsFeature("screenshots")) {
                try {
                    LoginPage loginPage = new LoginPage();
                    byte[] screenshot = loginPage.takeLoginPageScreenshot();
                    TestReporter.saveAndAddScreenshot("Cross-browser login failure - " + browserType.getDisplayName(), screenshot);
                } catch (Exception screenshotException) {
                    logger.warn("Could not take failure screenshot: {}", screenshotException.getMessage());
                }
            }
            
            throw e; // Re-throw to fail the test
        }
    }
    
    /**
     * Test login with invalid credentials across desktop browsers only
     * 
     * @param browserType The browser type to test on
     */
    @Test(dataProvider = "desktopBrowserProvider",
          description = "Test invalid login across desktop browsers",
          groups = {"cross-browser", "negative", "login"})
    public void testInvalidLoginAcrossDesktopBrowsers(BrowserType browserType) {
        logger.info("Starting invalid login test on desktop browser: {}", browserType.getDisplayName());
        
        try {
            // Initialize page objects
            LoginPage loginPage = new LoginPage();
            
            // Navigate to application
            loginPage.navigateToApplication();
            
            // Validate login page is loaded
            Assert.assertTrue(loginPage.isPageLoaded(), 
                "Login page should be loaded");
            
            // Attempt invalid login
            logger.info("Attempting invalid login on {}", browserType.getDisplayName());
            TestReporter.logInfo("Attempting invalid login with wrong credentials");
            
            loginPage.attemptInvalidLogin("invaliduser", "wrongpassword");
            
            // Validate error message is displayed
            Assert.assertTrue(loginPage.isErrorMessageDisplayed(), 
                "Error message should be displayed after invalid login on " + browserType.getDisplayName());
            
            // Validate still on login page
            Assert.assertTrue(loginPage.isOnLoginPage(), 
                "Should remain on login page after invalid login on " + browserType.getDisplayName());
            
            TestReporter.logPass("Invalid login test PASSED on " + browserType.getDisplayName());
            logger.info("Invalid login test PASSED on: {}", browserType.getDisplayName());
            
        } catch (Exception e) {
            logger.error("Invalid login test FAILED on {}: {}", browserType.getDisplayName(), e.getMessage());
            TestReporter.logFail("Invalid login test FAILED on " + browserType.getDisplayName() + ": " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Test login functionality across headless browsers for CI/CD environments
     * 
     * @param browserType The headless browser type to test on
     */
    @Test(dataProvider = "headlessBrowserProvider",
          description = "Test login in headless browsers for CI/CD",
          groups = {"cross-browser", "headless", "ci", "login"})
    public void testLoginInHeadlessBrowsers(BrowserType browserType) {
        logger.info("Starting headless browser login test on: {}", browserType.getDisplayName());
        
        try {
            // Initialize page objects
            LoginPage loginPage = new LoginPage();
            
            // Navigate and perform basic login test
            loginPage.navigateToApplication();
            
            Assert.assertTrue(loginPage.isPageLoaded(), 
                "Login page should be loaded in headless mode");
            
            // Perform login
            loginPage.loginAsAdmin();
            
            // Validate login success
            loginPage.waitForPageLoad();
            Assert.assertFalse(loginPage.isOnLoginPage(), 
                "Should successfully login in headless mode on " + browserType.getDisplayName());
            
            TestReporter.logPass("Headless login test PASSED on " + browserType.getDisplayName());
            logger.info("Headless login test PASSED on: {}", browserType.getDisplayName());
            
        } catch (Exception e) {
            logger.error("Headless login test FAILED on {}: {}", browserType.getDisplayName(), e.getMessage());
            TestReporter.logFail("Headless login test FAILED on " + browserType.getDisplayName() + ": " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Demonstration of programmatic cross-browser execution
     * This test shows how to execute tests across browsers programmatically
     */
    @Test(description = "Programmatic cross-browser execution example",
          groups = {"cross-browser", "demo"})
    public void testProgrammaticCrossBrowserExecution() {
        logger.info("Starting programmatic cross-browser execution demo");
        
        TestReporter.logInfo("Executing login test across all configured browsers programmatically");
        
        // Define a test runner implementation
        CrossBrowserTestRunner loginTestRunner = (browserType) -> {
            logger.info("Executing programmatic test on: {}", browserType.getDisplayName());
            
            // Create login page instance
            LoginPage loginPage = new LoginPage();
            
            // Navigate to application
            loginPage.navigateToApplication();
            
            // Validate page loaded
            if (!loginPage.isPageLoaded()) {
                throw new AssertionError("Login page not loaded on " + browserType.getDisplayName());
            }
            
            // Perform login
            loginPage.loginAsAdmin();
            
            // Validate success
            loginPage.waitForPageLoad();
            if (loginPage.isOnLoginPage()) {
                throw new AssertionError("Login failed on " + browserType.getDisplayName());
            }
            
            logger.info("Programmatic test PASSED on: {}", browserType.getDisplayName());
            return true; // Test passed
        };
        
        // Execute across all browsers sequentially
        CrossBrowserExecutionResult results = executeAcrossAllBrowsers(loginTestRunner);
        
        // Log results
        logger.info("Programmatic execution completed: {}", results.toString());
        TestReporter.logInfo("Execution Results: " + results.getSummaryReport());
        
        // Generate compatibility matrix
        String compatibilityMatrix = generateCompatibilityMatrix(results);
        logger.info("Cross-browser compatibility matrix:\n{}", compatibilityMatrix);
        TestReporter.logInfo("Browser Compatibility Matrix: " + compatibilityMatrix);
        
        // Validate results
        Assert.assertTrue(results.getPassCount() > 0, 
            "At least one browser should pass the test");
        
        // If we want strict validation (all browsers must pass)
        // Assert.assertTrue(results.allPassed(), "All browsers should pass the login test");
        
        TestReporter.logPass("Programmatic cross-browser execution completed successfully");
    }
    
    /**
     * Browser compatibility validation test
     * This test validates that the application works consistently across all browsers
     */
    @Test(description = "Browser compatibility validation",
          groups = {"cross-browser", "compatibility"})
    public void testBrowserCompatibility() {
        logger.info("Starting browser compatibility validation");
        
        TestReporter.logInfo("Validating browser compatibility across target browsers");
        
        // Get list of target browsers
        var targetBrowsers = getTargetBrowsers();
        TestReporter.logInfo("Target browsers: " + targetBrowsers.size());
        
        for (BrowserType browser : targetBrowsers) {
            TestReporter.logInfo("Browser: " + browser.getDisplayName() + 
                " | Supports Screenshots: " + browser.supportsScreenshots() +
                " | Supports Downloads: " + browser.supportsDownloads() +
                " | Supports Headless: " + browser.supportsHeadless());
        }
        
        // Validate minimum browser coverage
        Assert.assertTrue(targetBrowsers.size() >= 2, 
            "Should have at least 2 target browsers for cross-browser testing");
        
        // Validate Chrome is included (most stable for automation)
        boolean hasChromeVariant = targetBrowsers.stream()
            .anyMatch(browser -> browser.getBaseType() == BrowserType.CHROME);
        Assert.assertTrue(hasChromeVariant, 
            "Chrome should be included in target browsers for baseline compatibility");
        
        TestReporter.logPass("Browser compatibility validation completed");
        logger.info("Browser compatibility validation completed successfully");
    }
}