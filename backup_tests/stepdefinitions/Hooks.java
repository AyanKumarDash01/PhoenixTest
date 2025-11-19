package com.phoenix.hrm.stepdefinitions;

import com.phoenix.hrm.core.driver.WebDriverFactory;
import com.phoenix.hrm.core.config.ConfigManager;
import com.phoenix.hrm.core.utils.ScreenshotUtils;
import io.cucumber.java.*;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Cucumber Hooks for BDD Test Setup and Teardown
 * Handles WebDriver lifecycle, screenshot capture, and test environment setup
 * 
 * @author Phoenix HRM Test Team
 */
public class Hooks {
    
    private static final Logger logger = LoggerFactory.getLogger(Hooks.class);
    private final ConfigManager config = ConfigManager.getInstance();
    
    // Test execution tracking
    private static int scenarioCounter = 0;
    private static int passedScenarios = 0;
    private static int failedScenarios = 0;
    
    /**
     * Executed before all scenarios in the feature file
     */
    @BeforeAll
    public static void beforeAllScenarios() {
        logger.info("=".repeat(80));
        logger.info("STARTING BDD TEST EXECUTION - Employee Management Feature");
        logger.info("Execution Time: {}", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        logger.info("=".repeat(80));
        
        scenarioCounter = 0;
        passedScenarios = 0;
        failedScenarios = 0;
    }
    
    /**
     * Executed after all scenarios in the feature file
     */
    @AfterAll
    public static void afterAllScenarios() {
        logger.info("=".repeat(80));
        logger.info("BDD TEST EXECUTION COMPLETED");
        logger.info("Total Scenarios Executed: {}", scenarioCounter);
        logger.info("Passed Scenarios: {}", passedScenarios);
        logger.info("Failed Scenarios: {}", failedScenarios);
        logger.info("Success Rate: {}%", 
            scenarioCounter > 0 ? (passedScenarios * 100.0) / scenarioCounter : 0);
        logger.info("Execution Completed: {}", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        logger.info("=".repeat(80));
    }
    
    /**
     * Executed before each scenario
     * Initializes WebDriver and sets up test environment
     */
    @Before
    public void beforeScenario(Scenario scenario) {
        scenarioCounter++;
        
        logger.info("-".repeat(60));
        logger.info("STARTING SCENARIO #{}: {}", scenarioCounter, scenario.getName());
        logger.info("Tags: {}", scenario.getSourceTagNames());
        logger.info("Status: RUNNING");
        logger.info("-".repeat(60));
        
        try {
            // Initialize WebDriver based on configuration
            String browser = config.getProperty("browser", "chrome");
            boolean headless = Boolean.parseBoolean(config.getProperty("headless", "false"));
            
            logger.info("Initializing WebDriver - Browser: {}, Headless: {}", browser, headless);
            
            WebDriver driver = WebDriverFactory.createDriver(browser, headless);
            
            // Set implicit wait and page load timeout
            int implicitWait = Integer.parseInt(config.getProperty("implicit.wait", "10"));
            int pageLoadTimeout = Integer.parseInt(config.getProperty("page.load.timeout", "30"));
            
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(pageLoadTimeout));
            
            // Maximize window if not in headless mode
            if (!headless) {
                driver.manage().window().maximize();
            }
            
            logger.info("WebDriver initialized successfully for scenario: {}", scenario.getName());
            
        } catch (Exception e) {
            logger.error("Failed to initialize WebDriver for scenario: {}", scenario.getName(), e);
            throw new RuntimeException("WebDriver initialization failed", e);
        }
    }
    
    /**
     * Executed after each scenario
     * Handles screenshot capture for failed scenarios and WebDriver cleanup
     */
    @After
    public void afterScenario(Scenario scenario) {
        String scenarioStatus = scenario.getStatus().toString();
        
        try {
            // Take screenshot for failed scenarios or if configured to take always
            boolean takeScreenshotOnFailure = Boolean.parseBoolean(
                config.getProperty("screenshot.on.failure", "true"));
            boolean takeScreenshotAlways = Boolean.parseBoolean(
                config.getProperty("screenshot.always", "false"));
            
            if (scenario.isFailed() && takeScreenshotOnFailure || takeScreenshotAlways) {
                captureScreenshotForScenario(scenario);
            }
            
            // Update statistics
            if (scenario.isFailed()) {
                failedScenarios++;
                logger.error("SCENARIO FAILED: {}", scenario.getName());
                
                // Log error details if available
                if (scenario.getSourceTagNames().contains("@ErrorHandling")) {
                    logger.info("Error handling scenario - failure expected for testing");
                }
            } else {
                passedScenarios++;
                logger.info("SCENARIO PASSED: {}", scenario.getName());
            }
            
        } catch (Exception e) {
            logger.error("Error during scenario cleanup: {}", e.getMessage(), e);
        } finally {
            // Always quit WebDriver to free resources
            cleanupWebDriver(scenario);
        }
        
        logger.info("-".repeat(60));
        logger.info("SCENARIO COMPLETED: {}", scenario.getName());
        logger.info("Final Status: {}", scenarioStatus);
        logger.info("Duration: WebDriver cleanup completed");
        logger.info("-".repeat(60));
    }
    
    /**
     * Hook for scenarios tagged with @Smoke
     * Performs additional setup for smoke tests
     */
    @Before("@Smoke")
    public void beforeSmokeTest(Scenario scenario) {
        logger.info("SMOKE TEST SETUP: Performing additional smoke test initialization");
        logger.info("Smoke Test Scenario: {}", scenario.getName());
        
        // Additional setup for smoke tests if needed
        // For example: clearing cache, resetting test data, etc.
    }
    
    /**
     * Hook for scenarios tagged with @Performance
     * Sets up performance monitoring
     */
    @Before("@Performance")
    public void beforePerformanceTest(Scenario scenario) {
        logger.info("PERFORMANCE TEST SETUP: Initializing performance monitoring");
        logger.info("Performance Test Scenario: {}", scenario.getName());
        
        // Set shorter timeouts for performance tests
        WebDriver driver = WebDriverFactory.getDriver();
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(5));
        
        logger.info("Performance test timeouts configured");
    }
    
    /**
     * Hook for scenarios tagged with @NegativeTest
     * Prepares environment for negative testing
     */
    @Before("@NegativeTest")
    public void beforeNegativeTest(Scenario scenario) {
        logger.info("NEGATIVE TEST SETUP: Preparing for negative test scenario");
        logger.info("Negative Test Scenario: {}", scenario.getName());
        
        // Additional setup for negative tests
        // For example: preparing invalid data, setting up error conditions
    }
    
    /**
     * Hook for scenarios tagged with @DataDriven
     * Prepares data-driven test environment
     */
    @Before("@DataDriven")
    public void beforeDataDrivenTest(Scenario scenario) {
        logger.info("DATA-DRIVEN TEST SETUP: Preparing data-driven test environment");
        logger.info("Data-Driven Test Scenario: {}", scenario.getName());
        
        // Setup for data-driven tests
        // For example: loading test data, preparing data sources
    }
    
    /**
     * Captures screenshot for the given scenario
     */
    private void captureScreenshotForScenario(Scenario scenario) {
        try {
            WebDriver driver = WebDriverFactory.getDriver();
            if (driver != null) {
                String screenshotName = generateScreenshotName(scenario);
                String screenshotPath = ScreenshotUtils.captureScreenshot(driver, screenshotName);
                
                // Attach screenshot to Cucumber report
                byte[] screenshot = ScreenshotUtils.captureScreenshotAsBytes(driver);
                scenario.attach(screenshot, "image/png", "Screenshot");
                
                logger.info("Screenshot captured: {}", screenshotPath);
            }
        } catch (Exception e) {
            logger.error("Failed to capture screenshot for scenario: {}", scenario.getName(), e);
        }
    }
    
    /**
     * Generates a unique screenshot name for the scenario
     */
    private String generateScreenshotName(Scenario scenario) {
        String scenarioName = scenario.getName().replaceAll("[^a-zA-Z0-9]", "_");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("BDD_%s_%s", scenarioName, timestamp);
    }
    
    /**
     * Cleans up WebDriver resources
     */
    private void cleanupWebDriver(Scenario scenario) {
        try {
            WebDriver driver = WebDriverFactory.getDriver();
            if (driver != null) {
                logger.info("Cleaning up WebDriver for scenario: {}", scenario.getName());
                WebDriverFactory.quitDriver();
                logger.info("WebDriver cleanup completed successfully");
            }
        } catch (Exception e) {
            logger.error("Error during WebDriver cleanup for scenario: {}", scenario.getName(), e);
        }
    }
    
    /**
     * Step hook - executed before each step (optional, for detailed logging)
     */
    @BeforeStep
    public void beforeStep(Scenario scenario) {
        // Optional: Log each step for detailed debugging
        // Enable only if detailed step-by-step logging is needed
        if (Boolean.parseBoolean(config.getProperty("log.steps", "false"))) {
            logger.debug("Executing step in scenario: {}", scenario.getName());
        }
    }
    
    /**
     * Step hook - executed after each step (optional, for detailed logging)
     */
    @AfterStep
    public void afterStep(Scenario scenario) {
        // Optional: Take screenshot after each step if configured
        if (Boolean.parseBoolean(config.getProperty("screenshot.each.step", "false"))) {
            try {
                WebDriver driver = WebDriverFactory.getDriver();
                if (driver != null) {
                    byte[] screenshot = ScreenshotUtils.captureScreenshotAsBytes(driver);
                    scenario.attach(screenshot, "image/png", "Step Screenshot");
                }
            } catch (Exception e) {
                logger.debug("Could not capture step screenshot: {}", e.getMessage());
            }
        }
    }
}