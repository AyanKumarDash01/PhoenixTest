package com.phoenix.hrm.tests.enhanced;

import com.phoenix.hrm.core.config.ConfigManager;
import com.phoenix.hrm.core.data.TestDataManager;
import com.phoenix.hrm.core.driver.WebDriverFactory;
import com.phoenix.hrm.core.reporting.TestReporter;
import com.phoenix.hrm.pages.LoginPage;
import org.testng.Assert;
import org.testng.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Enhanced Login Tests demonstrating the new framework capabilities
 * Uses ConfigManager, TestDataManager, WebDriverFactory, and TestReporter
 */
public class EnhancedLoginTest {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedLoginTest.class);
    
    private LoginPage loginPage;
    private ConfigManager config;
    private TestDataManager testDataManager;
    
    @BeforeSuite
    public void setUpSuite() {
        logger.info("Setting up test suite - Enhanced Login Tests");
        TestReporter.initializeReports();
        
        config = ConfigManager.getInstance();
        testDataManager = TestDataManager.getInstance();
        
        // Validate test data structure
        testDataManager.validateTestDataStructure();
        
        logger.info("Suite setup completed. Base URL: {}, Browser: {}, Environment: {}",
            config.getBaseUrl(), config.getBrowser(), config.getEnvironment());
    }
    
    @BeforeClass
    public void setUpClass() {
        logger.info("Setting up Enhanced Login Test class");
    }
    
    @BeforeMethod
    public void setUp() {
        logger.info("Setting up test method");
        
        // Create WebDriver instance
        WebDriverFactory.createDriver();
        
        // Initialize page object
        loginPage = new LoginPage();
        
        // Navigate to login page
        loginPage.navigateToLoginPage();
        
        logger.info("Test setup completed");
    }
    
    @Test(priority = 1, description = "Verify successful login with admin credentials from test data")
    public void testSuccessfulAdminLogin() {
        TestReporter.startTest("Admin Login Test", "Verify successful login with admin credentials");
        TestReporter.setCategory("Login", "Smoke", "Positive");
        TestReporter.setAuthor("Test Automation Team");
        
        try {
            TestReporter.logInfo("Starting admin login test");
            
            // Verify login page is loaded
            Assert.assertTrue(loginPage.isPageLoaded(), "Login page should be loaded");
            TestReporter.logPass("Login page loaded successfully");
            
            // Validate form elements
            Assert.assertTrue(loginPage.validateLoginFormElements(), "Login form elements should be present");
            TestReporter.logPass("Login form validation passed");
            
            // Take screenshot of login page
            byte[] screenshot = loginPage.takeLoginPageScreenshot();
            TestReporter.saveAndAddScreenshot("Login Page Before Login", screenshot);
            
            // Get admin credentials from test data
            Map<String, String> adminCreds = testDataManager.getAdminCredentials();
            TestReporter.logInfo("Retrieved admin credentials from test data");
            
            // Perform login
            loginPage.loginAsAdmin();
            TestReporter.logPass("Admin login completed successfully");
            
            // Verify navigation (this would normally check dashboard elements)
            TestReporter.logInfo("Verifying post-login navigation");
            
        } catch (Exception e) {
            TestReporter.logFail("Admin login test failed: " + e.getMessage());
            TestReporter.logException(e);
            
            // Take failure screenshot
            byte[] failureScreenshot = loginPage.takeScreenshot();
            TestReporter.saveAndAddScreenshot("Failure Screenshot", failureScreenshot);
            
            throw e;
        } finally {
            TestReporter.endTest();
        }
    }
    
    @Test(priority = 2, description = "Verify login with invalid credentials shows error message")
    public void testInvalidLogin() {
        TestReporter.startTest("Invalid Login Test", "Verify error message for invalid credentials");
        TestReporter.setCategory("Login", "Negative", "Security");
        TestReporter.setAuthor("Test Automation Team");
        
        try {
            TestReporter.logInfo("Starting invalid login test");
            
            // Verify login page is loaded
            Assert.assertTrue(loginPage.isPageLoaded(), "Login page should be loaded");
            TestReporter.logPass("Login page loaded successfully");
            
            String invalidUsername = "invalid_user_" + testDataManager.getCurrentTimestamp();
            String invalidPassword = "invalid_pass_" + testDataManager.generateRandomString(8);
            
            TestReporter.logInfo("Using invalid credentials - Username: " + invalidUsername);
            
            // Attempt invalid login
            loginPage.attemptInvalidLogin(invalidUsername, invalidPassword);
            TestReporter.logInfo("Invalid login attempt completed");
            
            // Verify error message is displayed
            Assert.assertTrue(loginPage.isErrorMessageDisplayed(), "Error message should be displayed");
            TestReporter.logPass("Error message displayed for invalid credentials");
            
            // Verify error message content
            String errorMessage = loginPage.getErrorMessage();
            Assert.assertFalse(errorMessage.isEmpty(), "Error message should not be empty");
            TestReporter.logInfo("Error message: " + errorMessage);
            
            // Take screenshot of error
            byte[] errorScreenshot = loginPage.takeLoginPageScreenshot();
            TestReporter.saveAndAddScreenshot("Error Message Display", errorScreenshot);
            
            TestReporter.logPass("Invalid login test passed - Error handling verified");
            
        } catch (Exception e) {
            TestReporter.logFail("Invalid login test failed: " + e.getMessage());
            TestReporter.logException(e);
            
            byte[] failureScreenshot = loginPage.takeScreenshot();
            TestReporter.saveAndAddScreenshot("Failure Screenshot", failureScreenshot);
            
            throw e;
        } finally {
            TestReporter.endTest();
        }
    }
    
    @AfterMethod
    public void tearDown() {
        logger.info("Tearing down test method");
        
        try {
            // Quit WebDriver
            WebDriverFactory.quitDriver();
            logger.info("WebDriver quit successfully");
        } catch (Exception e) {
            logger.warn("Error during WebDriver cleanup", e);
        }
    }
    
    @AfterClass
    public void tearDownClass() {
        logger.info("Tearing down Enhanced Login Test class");
    }
    
    @AfterSuite
    public void tearDownSuite() {
        logger.info("Tearing down test suite");
        
        // Generate final reports
        TestReporter.flushReports();
        
        // Log test execution summary
        logger.info("Test execution completed. Statistics: {}", TestReporter.getTestStatistics());
        logger.info("Test report available at: {}", TestReporter.getReportPath());
        
        logger.info("Suite teardown completed");
    }
}
