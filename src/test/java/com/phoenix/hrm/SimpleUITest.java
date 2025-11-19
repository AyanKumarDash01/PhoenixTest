package com.phoenix.hrm.tests;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;

/**
 * Simple standalone UI test demonstrating login functionality testing
 * This test can be run independently without the full framework
 */
public class SimpleUITest {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleUITest.class);
    private WebDriver driver;
    private WebDriverWait wait;
    
    // Test Data
    private static final String BASE_URL = "https://opensource-demo.orangehrmlive.com/";
    private static final String VALID_USERNAME = "Admin";
    private static final String VALID_PASSWORD = "admin123";
    
    @BeforeMethod
    public void setUp() {
        logger.info("Setting up WebDriver for UI test");
        
        // Setup ChromeDriver using WebDriverManager
        WebDriverManager.chromedriver().setup();
        
        // Configure Chrome options
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // Run in headless mode
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }
    
    @Test(priority = 1)
    public void testValidLogin() {
        logger.info("Starting valid login test");
        
        try {
            // Navigate to login page
            driver.get(BASE_URL);
            logger.info("Navigated to: {}", BASE_URL);
            
            // Wait for login form to be visible
            WebElement usernameField = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.name("username"))
            );
            
            // Enter valid credentials
            usernameField.clear();
            usernameField.sendKeys(VALID_USERNAME);
            logger.info("Entered username: {}", VALID_USERNAME);
            
            WebElement passwordField = driver.findElement(By.name("password"));
            passwordField.clear();
            passwordField.sendKeys(VALID_PASSWORD);
            logger.info("Entered password");
            
            // Click login button
            WebElement loginButton = driver.findElement(By.xpath("//button[@type='submit']"));
            loginButton.click();
            logger.info("Clicked login button");
            
            // Wait for dashboard to load
            WebElement dashboardHeader = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.xpath("//h6[contains(text(),'Dashboard')]"))
            );
            
            // Verify successful login
            Assert.assertTrue(dashboardHeader.isDisplayed(), "Dashboard should be visible after successful login");
            logger.info("✓ Valid login test passed - Dashboard is visible");
            
        } catch (Exception e) {
            logger.error("Valid login test failed: {}", e.getMessage());
            throw e;
        }
    }
    
    @Test(priority = 2)
    public void testInvalidLogin() {
        logger.info("Starting invalid login test");
        
        try {
            // Navigate to login page
            driver.get(BASE_URL);
            logger.info("Navigated to: {}", BASE_URL);
            
            // Wait for login form to be visible
            WebElement usernameField = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.name("username"))
            );
            
            // Enter invalid credentials
            usernameField.clear();
            usernameField.sendKeys("invalid_user");
            logger.info("Entered invalid username");
            
            WebElement passwordField = driver.findElement(By.name("password"));
            passwordField.clear();
            passwordField.sendKeys("invalid_password");
            logger.info("Entered invalid password");
            
            // Click login button
            WebElement loginButton = driver.findElement(By.xpath("//button[@type='submit']"));
            loginButton.click();
            logger.info("Clicked login button");
            
            // Wait for error message
            WebElement errorMessage = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.xpath("//p[contains(@class,'error-message') or contains(text(),'Invalid')]"))
            );
            
            // Verify error message is displayed
            Assert.assertTrue(errorMessage.isDisplayed(), "Error message should be visible for invalid login");
            logger.info("✓ Invalid login test passed - Error message is displayed: {}", errorMessage.getText());
            
        } catch (Exception e) {
            logger.error("Invalid login test failed: {}", e.getMessage());
            throw e;
        }
    }
    
    @Test(priority = 3)
    public void testPageTitle() {
        logger.info("Starting page title test");
        
        try {
            driver.get(BASE_URL);
            logger.info("Navigated to: {}", BASE_URL);
            
            // Wait for page to load and check title
            wait.until(ExpectedConditions.titleContains("OrangeHRM"));
            String actualTitle = driver.getTitle();
            
            Assert.assertTrue(actualTitle.contains("OrangeHRM"), 
                "Page title should contain 'OrangeHRM', but was: " + actualTitle);
            logger.info("✓ Page title test passed - Title: {}", actualTitle);
            
        } catch (Exception e) {
            logger.error("Page title test failed: {}", e.getMessage());
            throw e;
        }
    }
    
    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            try {
                driver.quit();
                logger.info("WebDriver closed successfully");
            } catch (Exception e) {
                logger.warn("Error closing WebDriver: {}", e.getMessage());
            }
        }
    }
}