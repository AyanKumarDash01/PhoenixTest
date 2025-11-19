package com.phoenix.hrm.core.base;

import com.phoenix.hrm.core.config.ConfigManager;
import com.phoenix.hrm.core.exceptions.FrameworkException;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Set;

/**
 * Enterprise BasePage class providing robust web element interactions and explicit wait strategies.
 * Implements Page Object Model best practices with fluent interface pattern.
 * 
 * Features:
 * - Explicit wait strategies with configurable timeouts
 * - Robust element interaction methods with error handling
 * - JavaScript execution capabilities
 * - Fluent interface for method chaining
 * - Page load verification
 * - Mobile-friendly actions
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 1.0
 */
public abstract class BasePage {
    
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final WebDriver driver;
    protected final WebDriverWait wait;
    protected final Actions actions;
    protected final ConfigManager config;
    protected final JavascriptExecutor jsExecutor;
    
    /**
     * Constructor initializes page with WebDriver and configures wait strategies
     * 
     * @param driver WebDriver instance
     */
    public BasePage(WebDriver driver) {
        this.driver = driver;
        this.config = ConfigManager.getInstance();
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(config.getExplicitWait()));
        this.actions = new Actions(driver);
        this.jsExecutor = (JavascriptExecutor) driver;
        
        // Initialize page elements using PageFactory
        PageFactory.initElements(driver, this);
        
        logger.debug("Initialized {} page", this.getClass().getSimpleName());
    }
    
    /**
     * Waits for element to be clickable with default timeout
     * 
     * @param locator Element locator
     * @return WebElement when clickable
     */
    protected WebElement waitForElementToBeClickable(By locator) {
        return waitForElementToBeClickable(locator, config.getExplicitWait());
    }
    
    /**
     * Waits for element to be clickable with custom timeout
     * 
     * @param locator Element locator
     * @param timeoutSeconds Custom timeout in seconds
     * @return WebElement when clickable
     */
    protected WebElement waitForElementToBeClickable(By locator, int timeoutSeconds) {
        try {
            WebDriverWait customWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            return customWait.until(ExpectedConditions.elementToBeClickable(locator));
        } catch (TimeoutException e) {
            logger.error("Element not clickable within {} seconds: {}", timeoutSeconds, locator);
            throw new FrameworkException("Element not clickable: " + locator, e);
        }
    }
    
    /**
     * Waits for element to be clickable (WebElement version)
     * 
     * @param element WebElement
     * @return WebElement when clickable
     */
    protected WebElement waitForElementToBeClickable(WebElement element) {
        try {
            return wait.until(ExpectedConditions.elementToBeClickable(element));
        } catch (TimeoutException e) {
            logger.error("Element not clickable within {} seconds", config.getExplicitWait());
            throw new FrameworkException("Element not clickable", e);
        }
    }
    
    /**
     * Waits for element to be visible
     * 
     * @param locator Element locator
     * @return WebElement when visible
     */
    protected WebElement waitForElementToBeVisible(By locator) {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (TimeoutException e) {
            logger.error("Element not visible within {} seconds: {}", config.getExplicitWait(), locator);
            throw new FrameworkException("Element not visible: " + locator, e);
        }
    }
    
    /**
     * Waits for element to be present in DOM
     * 
     * @param locator Element locator
     * @return WebElement when present
     */
    protected WebElement waitForElementToBePresent(By locator) {
        try {
            return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        } catch (TimeoutException e) {
            logger.error("Element not present within {} seconds: {}", config.getExplicitWait(), locator);
            throw new FrameworkException("Element not present: " + locator, e);
        }
    }
    
    /**
     * Waits for text to be present in element
     * 
     * @param locator Element locator
     * @param text Expected text
     * @return true when text is present
     */
    protected boolean waitForTextToBePresentInElement(By locator, String text) {
        try {
            return wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
        } catch (TimeoutException e) {
            logger.error("Text '{}' not present in element within {} seconds: {}", text, config.getExplicitWait(), locator);
            return false;
        }
    }
    
    /**
     * Clicks element with explicit wait and error handling
     * 
     * @param locator Element locator
     * @return Current page instance for method chaining
     */
    protected BasePage clickElement(By locator) {
        try {
            WebElement element = waitForElementToBeClickable(locator);
            highlightElement(element);
            element.click();
            logger.debug("Clicked element: {}", locator);
        } catch (Exception e) {
            logger.error("Failed to click element: {}", locator, e);
            throw new FrameworkException("Click operation failed: " + locator, e);
        }
        return this;
    }
    
    /**
     * Clicks element using WebElement
     * 
     * @param element WebElement to click
     * @return Current page instance for method chaining
     */
    protected BasePage clickElement(WebElement element) {
        try {
            waitForElementToBeClickable(element);
            highlightElement(element);
            element.click();
            logger.debug("Clicked element");
        } catch (Exception e) {
            logger.error("Failed to click element", e);
            throw new FrameworkException("Click operation failed", e);
        }
        return this;
    }
    
    /**
     * Clicks element using JavaScript (bypass click intercepted issues)
     * 
     * @param element WebElement to click
     * @return Current page instance for method chaining
     */
    protected BasePage clickElementWithJS(WebElement element) {
        try {
            highlightElement(element);
            jsExecutor.executeScript("arguments[0].click();", element);
            logger.debug("Clicked element with JavaScript");
        } catch (Exception e) {
            logger.error("Failed to click element with JavaScript", e);
            throw new FrameworkException("JavaScript click operation failed", e);
        }
        return this;
    }
    
    /**
     * Sends keys to element with explicit wait
     * 
     * @param locator Element locator
     * @param text Text to send
     * @return Current page instance for method chaining
     */
    protected BasePage sendKeysToElement(By locator, String text) {
        try {
            WebElement element = waitForElementToBeVisible(locator);
            element.clear();
            highlightElement(element);
            element.sendKeys(text);
            logger.debug("Sent keys '{}' to element: {}", text, locator);
        } catch (Exception e) {
            logger.error("Failed to send keys to element: {}", locator, e);
            throw new FrameworkException("Send keys operation failed: " + locator, e);
        }
        return this;
    }
    
    /**
     * Sends keys to WebElement
     * 
     * @param element WebElement
     * @param text Text to send
     * @return Current page instance for method chaining
     */
    protected BasePage sendKeysToElement(WebElement element, String text) {
        try {
            element.clear();
            highlightElement(element);
            element.sendKeys(text);
            logger.debug("Sent keys '{}' to element", text);
        } catch (Exception e) {
            logger.error("Failed to send keys to element", e);
            throw new FrameworkException("Send keys operation failed", e);
        }
        return this;
    }
    
    /**
     * Gets text from element
     * 
     * @param locator Element locator
     * @return Element text
     */
    protected String getElementText(By locator) {
        try {
            WebElement element = waitForElementToBeVisible(locator);
            String text = element.getText();
            logger.debug("Retrieved text '{}' from element: {}", text, locator);
            return text;
        } catch (Exception e) {
            logger.error("Failed to get text from element: {}", locator, e);
            throw new FrameworkException("Get text operation failed: " + locator, e);
        }
    }
    
    /**
     * Gets text from WebElement
     * 
     * @param element WebElement
     * @return Element text
     */
    protected String getElementText(WebElement element) {
        try {
            String text = element.getText();
            logger.debug("Retrieved text '{}' from element", text);
            return text;
        } catch (Exception e) {
            logger.error("Failed to get text from element", e);
            throw new FrameworkException("Get text operation failed", e);
        }
    }
    
    /**
     * Gets attribute value from element
     * 
     * @param locator Element locator
     * @param attributeName Attribute name
     * @return Attribute value
     */
    protected String getElementAttribute(By locator, String attributeName) {
        try {
            WebElement element = waitForElementToBePresent(locator);
            String attributeValue = element.getAttribute(attributeName);
            logger.debug("Retrieved attribute '{}' = '{}' from element: {}", attributeName, attributeValue, locator);
            return attributeValue;
        } catch (Exception e) {
            logger.error("Failed to get attribute '{}' from element: {}", attributeName, locator, e);
            throw new FrameworkException("Get attribute operation failed: " + locator, e);
        }
    }
    
    /**
     * Checks if element is displayed
     * 
     * @param locator Element locator
     * @return true if element is displayed
     */
    protected boolean isElementDisplayed(By locator) {
        try {
            WebElement element = driver.findElement(locator);
            boolean isDisplayed = element.isDisplayed();
            logger.debug("Element displayed status: {} for locator: {}", isDisplayed, locator);
            return isDisplayed;
        } catch (NoSuchElementException e) {
            logger.debug("Element not found: {}", locator);
            return false;
        } catch (Exception e) {
            logger.error("Error checking element display status: {}", locator, e);
            return false;
        }
    }
    
    /**
     * Highlights element for visual debugging
     * 
     * @param element WebElement to highlight
     */
    protected void highlightElement(WebElement element) {
        try {
            String originalStyle = element.getAttribute("style");
            jsExecutor.executeScript(
                "arguments[0].setAttribute('style', arguments[1]);",
                element,
                "border: 2px solid red; background-color: yellow;"
            );
            Thread.sleep(200);
            jsExecutor.executeScript(
                "arguments[0].setAttribute('style', arguments[1]);",
                element,
                originalStyle
            );
        } catch (Exception e) {
            // Ignore highlighting errors
            logger.debug("Could not highlight element", e);
        }
    }
    
    /**
     * Waits for page to load completely
     * 
     * @return Current page instance for method chaining
     */
    protected BasePage waitForPageLoad() {
        try {
            FluentWait<WebDriver> fluentWait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(config.getPageLoadTimeout()))
                .pollingEvery(Duration.ofMillis(500))
                .ignoring(JavascriptException.class);
                
            fluentWait.until(driver -> {
                String readyState = jsExecutor.executeScript("return document.readyState").toString();
                return "complete".equals(readyState);
            });
            
            logger.debug("Page loaded completely");
        } catch (TimeoutException e) {
            logger.warn("Page load timeout reached");
        }
        return this;
    }
    
    /**
     * Gets current page title
     * 
     * @return Page title
     */
    protected String getPageTitle() {
        String title = driver.getTitle();
        logger.debug("Page title: {}", title);
        return title;
    }
    
    /**
     * Gets current page URL
     * 
     * @return Current URL
     */
    protected String getCurrentUrl() {
        String url = driver.getCurrentUrl();
        logger.debug("Current URL: {}", url);
        return url;
    }
    
    /**
     * Abstract method to verify page load - must be implemented by each page
     * 
     * @return true if page is loaded correctly
     */
    public abstract boolean isPageLoaded();
    
    /**
     * Gets page load verification timeout
     * 
     * @return Timeout in seconds
     */
    protected int getPageLoadTimeout() {
        return config.getExplicitWait();
    }
}