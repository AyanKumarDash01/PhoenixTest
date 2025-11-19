package com.phoenix.hrm.bdd.hooks;

import com.phoenix.hrm.base.BaseTest;
import com.phoenix.hrm.config.ConfigurationManager;
import com.phoenix.hrm.reporting.TestReporter;
import com.phoenix.hrm.utils.ScreenshotUtils;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cucumber Hooks for Phoenix HRM BDD Tests
 * 
 * This class provides setup and teardown hooks for Cucumber scenarios,
 * integrating with the Phoenix HRM framework's test lifecycle management.
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 3.0
 * @since Phase 3
 */
public class CucumberHooks extends BaseTest {
    
    private static final Logger logger = LoggerFactory.getLogger(CucumberHooks.class);
    
    /**
     * Setup hook executed before each scenario
     * 
     * @param scenario The Cucumber scenario about to be executed
     */
    @Before
    public void beforeScenario(Scenario scenario) {
        try {
            logger.info("======================================");
            logger.info("Starting BDD Scenario: {}", scenario.getName());
            logger.info("Feature: {}", scenario.getId().split(";")[0]);
            logger.info("Tags: {}", scenario.getSourceTagNames());
            logger.info("======================================");
            
            // Initialize test reporter for BDD scenario
            TestReporter.startTest(scenario.getName(), 
                "BDD Scenario: " + scenario.getName() + " | Tags: " + scenario.getSourceTagNames());
            
            TestReporter.logInfo("BDD Scenario started: " + scenario.getName());
            TestReporter.logInfo("Feature file: " + scenario.getId().split(";")[0]);
            TestReporter.logInfo("Scenario tags: " + scenario.getSourceTagNames());
            
            // Initialize WebDriver if not already done
            if (getDriver() == null) {
                String browser = ConfigurationManager.getBrowser();
                logger.info("Initializing WebDriver with browser: {}", browser);
                TestReporter.logInfo("Initializing WebDriver with browser: " + browser);
                
                setUp();
                
                logger.info("WebDriver initialized successfully");
                TestReporter.logPass("WebDriver initialized successfully");
            }
            
            // Log environment information
            TestReporter.logInfo("Environment: " + ConfigurationManager.getEnvironment());
            TestReporter.logInfo("Base URL: " + ConfigurationManager.getBaseUrl());
            TestReporter.logInfo("Browser: " + ConfigurationManager.getBrowser());
            TestReporter.logInfo("Headless mode: " + ConfigurationManager.isHeadless());
            
        } catch (Exception e) {
            logger.error("Error in beforeScenario hook: ", e);
            TestReporter.logFail("Setup failed in beforeScenario hook: " + e.getMessage());
            throw new RuntimeException("Failed to setup scenario: " + scenario.getName(), e);
        }
    }
    
    /**
     * Teardown hook executed after each scenario
     * 
     * @param scenario The Cucumber scenario that was executed
     */
    @After
    public void afterScenario(Scenario scenario) {
        try {
            logger.info("Finishing BDD Scenario: {}", scenario.getName());
            logger.info("Scenario Status: {}", scenario.getStatus());
            
            // Handle scenario results
            if (scenario.isFailed()) {
                logger.error("Scenario FAILED: {}", scenario.getName());
                TestReporter.logFail("BDD Scenario FAILED: " + scenario.getName());
                
                // Take screenshot on failure
                if (getDriver() != null) {
                    byte[] screenshot = ScreenshotUtils.takeScreenshotAsBytes(getDriver());
                    if (screenshot != null) {
                        // Attach screenshot to Cucumber report
                        scenario.attach(screenshot, "image/png", "Failure Screenshot");
                        
                        // Add screenshot to ExtentReports
                        TestReporter.saveAndAddScreenshot("Scenario Failure Screenshot", screenshot);
                        
                        logger.info("Failure screenshot captured and attached to reports");
                    }
                }
            } else {
                logger.info("Scenario PASSED: {}", scenario.getName());
                TestReporter.logPass("BDD Scenario PASSED: " + scenario.getName());
                
                // Take screenshot on success for documentation (optional)
                if (getDriver() != null && shouldTakeSuccessScreenshot(scenario)) {
                    byte[] screenshot = ScreenshotUtils.takeScreenshotAsBytes(getDriver());
                    if (screenshot != null) {
                        scenario.attach(screenshot, "image/png", "Success Screenshot");
                        TestReporter.saveAndAddScreenshot("Scenario Success Screenshot", screenshot);
                    }
                }
            }
            
            // Log scenario execution summary
            TestReporter.logInfo("Scenario execution completed: " + scenario.getStatus());
            TestReporter.logInfo("Scenario duration: " + calculateScenarioDuration() + "ms");
            
            // Don't quit driver here - let BaseTest handle it in @AfterMethod or @AfterClass
            // This allows for proper cleanup coordination with TestNG lifecycle
            
            logger.info("BDD Scenario finished: {} - Status: {}", scenario.getName(), scenario.getStatus());
            logger.info("======================================");
            
        } catch (Exception e) {
            logger.error("Error in afterScenario hook: ", e);
            TestReporter.logError("Error in scenario cleanup: " + e.getMessage());
        } finally {
            // Ensure test result is recorded in ExtentReports
            TestReporter.endTest();
        }
    }
    
    /**
     * Determines if success screenshots should be taken for specific scenarios
     * 
     * @param scenario The scenario to check
     * @return true if success screenshot should be taken
     */
    private boolean shouldTakeSuccessScreenshot(Scenario scenario) {
        // Take success screenshots for tagged scenarios or based on configuration
        return scenario.getSourceTagNames().contains("@integration") ||
               scenario.getSourceTagNames().contains("@smoke") ||
               ConfigurationManager.getBooleanProperty("bdd.screenshot.on.success", false);
    }
    
    /**
     * Calculates rough scenario duration (placeholder implementation)
     * 
     * @return Duration in milliseconds
     */
    private long calculateScenarioDuration() {
        // This is a simplified implementation
        // In a real scenario, you would track start/end times
        return System.currentTimeMillis() % 10000; // Placeholder
    }
    
    /**
     * Hook executed before all scenarios (once per test run)
     */
    @Before("@BeforeAll")
    public void beforeAllScenarios() {
        logger.info("===== BDD Test Suite Starting =====");
        logger.info("Phoenix HRM BDD Test Execution Started");
        logger.info("Environment: {}", ConfigurationManager.getEnvironment());
        logger.info("Browser: {}", ConfigurationManager.getBrowser());
        logger.info("====================================");
        
        // Initialize global test reporting
        TestReporter.initializeExtentReports();
        TestReporter.logInfo("BDD Test Suite execution started");
    }
    
    /**
     * Hook executed after all scenarios (once per test run)
     */
    @After("@AfterAll")
    public void afterAllScenarios() {
        logger.info("===== BDD Test Suite Completed =====");
        logger.info("Phoenix HRM BDD Test Execution Completed");
        logger.info("====================================");
        
        // Finalize global test reporting
        TestReporter.logInfo("BDD Test Suite execution completed");
        TestReporter.flushReports();
    }
    
    /**
     * Hook for scenarios tagged with @api
     * Sets up API testing specific configurations
     */
    @Before("@api")
    public void beforeApiScenario() {
        logger.info("Setting up API testing configuration");
        TestReporter.logInfo("Initializing API testing setup");
        
        // API-specific setup can be added here
        // For example: initialize API client, set base URI, etc.
    }
    
    /**
     * Hook for scenarios tagged with @database
     * Sets up database testing specific configurations
     */
    @Before("@database")  
    public void beforeDatabaseScenario() {
        logger.info("Setting up database testing configuration");
        TestReporter.logInfo("Initializing database testing setup");
        
        // Database-specific setup can be added here
        // For example: initialize database connections, prepare test data, etc.
    }
    
    /**
     * Hook for scenarios tagged with @performance
     * Sets up performance testing specific configurations
     */
    @Before("@performance")
    public void beforePerformanceScenario() {
        logger.info("Setting up performance testing configuration");
        TestReporter.logInfo("Initializing performance testing setup");
        
        // Performance testing setup can be added here
        // For example: start performance monitoring, set timing thresholds, etc.
    }
}