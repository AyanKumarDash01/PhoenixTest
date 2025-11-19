package com.phoenix.hrm.core.pages;

import com.phoenix.hrm.core.config.ConfigManager;
import com.phoenix.hrm.core.driver.WebDriverFactory;
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
import java.util.List;
import java.util.Set;

/**
 * Enhanced Base Page Object for Phoenix HRM Test Automation Framework
 * Provides common functionality and utilities for all page objects
 */
public abstract class BasePage {
    
    protected static final Logger logger = LoggerFactory.getLogger(BasePage.class);
    protected WebDriver driver;
    protected WebDriverWait wait;
    protected Actions actions;
    protected ConfigManager config;
    
    // Wait timeout constants
    private static final int DEFAULT_TIMEOUT = 30;
    private static final int SHORT_TIMEOUT = 10;
    private static final int LONG_TIMEOUT = 60;
    
    /**
     * Constructor - initializes page with WebDriver
     */
    protected BasePage() {
        this.driver = WebDriverFactory.getDriver();
        this.config = ConfigManager.getInstance();
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(config.getExplicitWait()));
        this.actions = new Actions(driver);
        PageFactory.initElements(driver, this);
        logger.debug("Initialized page: {}", this.getClass().getSimpleName());
    }
    
    /**
     * Constructor with custom timeout
     */
    protected BasePage(int timeoutInSeconds) {
        this.driver = WebDriverFactory.getDriver();
        this.config = ConfigManager.getInstance();
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds));
        this.actions = new Actions(driver);
        PageFactory.initElements(driver, this);
        logger.debug("Initialized page: {} with timeout: {}s", this.getClass().getSimpleName(), timeoutInSeconds);
    }
    
    // ==================== ELEMENT INTERACTION METHODS ====================
    
    /**
     * Safe click with wait for element to be clickable
     */
    protected void safeClick(WebElement element) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(element));
            highlightElement(element);
            element.click();
            logger.debug("Clicked element: {}", getElementDescription(element));
        } catch (Exception e) {
            logger.error("Failed to click element: {}", getElementDescription(element), e);
            throw new RuntimeException("Click failed", e);
        }
    }
    
    /**
     * Safe click with retry mechanism
     */
    protected void safeClickWithRetry(WebElement element, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            try {
                safeClick(element);
                return;
            } catch (Exception e) {
                logger.warn("Click attempt {} failed for element: {}", i + 1, getElementDescription(element));
                if (i == maxRetries - 1) {
                    throw new RuntimeException("All click attempts failed", e);
                }
                sleep(1000);
            }
        }
    }
    
    /**
     * Safe text input with clear and validation
     */
    protected void safeType(WebElement element, String text) {
        try {
            wait.until(ExpectedConditions.visibilityOf(element));
            highlightElement(element);
            element.clear();
            element.sendKeys(text);
            
            // Validate input
            String actualValue = element.getAttribute("value");
            if (!text.equals(actualValue)) {
                logger.warn("Expected text '{}' but found '{}' in element", text, actualValue);
            }
            
            logger.debug("Typed '{}' into element: {}", text, getElementDescription(element));
        } catch (Exception e) {
            logger.error("Failed to type '{}' into element: {}", text, getElementDescription(element), e);
            throw new RuntimeException("Type operation failed", e);
        }
    }
    
    /**
     * Safe text input with JavaScript
     */
    protected void safeTypeWithJS(WebElement element, String text) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].value = arguments[1];", element, text);
            js.executeScript("arguments[0].dispatchEvent(new Event('input', { bubbles: true }));", element);
            logger.debug("Typed '{}' into element using JS: {}", text, getElementDescription(element));
        } catch (Exception e) {
            logger.error("Failed to type '{}' using JS: {}", text, getElementDescription(element), e);
            throw new RuntimeException("JS type operation failed", e);
        }
    }
    
    /**
     * Select dropdown option by visible text
     */
    protected void selectByVisibleText(WebElement dropdown, String text) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(dropdown));
            Select select = new Select(dropdown);
            select.selectByVisibleText(text);
            logger.debug("Selected option '{}' from dropdown: {}", text, getElementDescription(dropdown));
        } catch (Exception e) {
            logger.error("Failed to select option '{}' from dropdown: {}", text, getElementDescription(dropdown), e);
            throw new RuntimeException("Dropdown selection failed", e);
        }
    }
    
    /**
     * Select dropdown option by value
     */
    protected void selectByValue(WebElement dropdown, String value) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(dropdown));
            Select select = new Select(dropdown);
            select.selectByValue(value);
            logger.debug("Selected value '{}' from dropdown: {}", value, getElementDescription(dropdown));
        } catch (Exception e) {
            logger.error("Failed to select value '{}' from dropdown: {}", value, getElementDescription(dropdown), e);
            throw new RuntimeException("Dropdown selection failed", e);
        }
    }
    
    // ==================== WAIT METHODS ====================
    
    /**
     * Wait for element to be visible
     */
    protected WebElement waitForVisibility(WebElement element) {
        return wait.until(ExpectedConditions.visibilityOf(element));
    }
    
    /**
     * Wait for element to be visible by locator
     */
    protected WebElement waitForVisibility(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }
    
    /**
     * Wait for element to be clickable
     */
    protected WebElement waitForClickable(WebElement element) {
        return wait.until(ExpectedConditions.elementToBeClickable(element));
    }
    
    /**
     * Wait for element to be invisible
     */
    protected boolean waitForInvisibility(WebElement element) {
        try {
            return wait.until(ExpectedConditions.invisibilityOf(element));
        } catch (TimeoutException e) {
            return false;
        }
    }
    
    /**
     * Wait for text to be present in element
     */
    protected boolean waitForTextInElement(WebElement element, String text) {
        try {
            return wait.until(ExpectedConditions.textToBePresentInElement(element, text));
        } catch (TimeoutException e) {
            logger.warn("Timeout waiting for text '{}' in element: {}", text, getElementDescription(element));
            return false;
        }
    }
    
    /**
     * Wait for page title to contain text
     */
    protected boolean waitForTitleContains(String title) {
        try {
            return wait.until(ExpectedConditions.titleContains(title));
        } catch (TimeoutException e) {
            logger.warn("Timeout waiting for title to contain: {}", title);
            return false;
        }
    }
    
    /**
     * Fluent wait for custom condition
     */
    protected <T> T fluentWait(java.util.function.Function<WebDriver, T> condition, int timeoutSeconds, int pollIntervalSeconds) {
        FluentWait<WebDriver> fluentWait = new FluentWait<>(driver)
            .withTimeout(Duration.ofSeconds(timeoutSeconds))
            .pollingEvery(Duration.ofSeconds(pollIntervalSeconds))
            .ignoring(NoSuchElementException.class)
            .ignoring(StaleElementReferenceException.class);
        
        return fluentWait.until(condition);
    }
    
    // ==================== VALIDATION METHODS ====================
    
    /**
     * Check if element is displayed
     */
    protected boolean isElementDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }
    
    /**
     * Check if element exists
     */
    protected boolean isElementPresent(By locator) {
        try {
            driver.findElement(locator);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }
    
    /**
     * Get element text safely
     */
    protected String getElementText(WebElement element) {
        try {
            wait.until(ExpectedConditions.visibilityOf(element));
            return element.getText().trim();
        } catch (Exception e) {
            logger.warn("Could not get text from element: {}", getElementDescription(element), e);
            return "";
        }
    }
    
    /**
     * Get element attribute safely
     */
    protected String getElementAttribute(WebElement element, String attribute) {
        try {
            wait.until(ExpectedConditions.visibilityOf(element));
            return element.getAttribute(attribute);
        } catch (Exception e) {
            logger.warn("Could not get attribute '{}' from element: {}", attribute, getElementDescription(element), e);
            return "";
        }
    }
    
    // ==================== JAVASCRIPT METHODS ====================
    
    /**
     * Execute JavaScript
     */
    protected Object executeScript(String script, Object... args) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        return js.executeScript(script, args);
    }
    
    /**
     * Scroll to element
     */
    protected void scrollToElement(WebElement element) {
        executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
        sleep(500); // Wait for scroll animation
    }
    
    /**
     * Click element using JavaScript
     */
    protected void clickWithJS(WebElement element) {
        executeScript("arguments[0].click();", element);
        logger.debug("Clicked element using JS: {}", getElementDescription(element));
    }
    
    /**
     * Highlight element for debugging
     */
    protected void highlightElement(WebElement element) {
        if (config.getBoolean("debug.highlight.elements", false)) {
            executeScript("arguments[0].style.border='3px solid red';", element);
            sleep(200);
            executeScript("arguments[0].style.border='';", element);
        }
    }
    
    // ==================== NAVIGATION METHODS ====================
    
    /**
     * Navigate to URL
     */
    protected void navigateTo(String url) {
        driver.get(url);
        logger.info("Navigated to: {}", url);
    }
    
    /**
     * Refresh page
     */
    protected void refreshPage() {
        driver.navigate().refresh();
        logger.debug("Page refreshed");
    }
    
    /**
     * Go back
     */
    protected void goBack() {
        driver.navigate().back();
        logger.debug("Navigated back");
    }
    
    /**
     * Switch to window by title
     */
    protected boolean switchToWindow(String windowTitle) {
        Set<String> windows = driver.getWindowHandles();
        for (String window : windows) {
            driver.switchTo().window(window);
            if (driver.getTitle().contains(windowTitle)) {
                logger.debug("Switched to window: {}", windowTitle);
                return true;
            }
        }
        logger.warn("Window not found with title: {}", windowTitle);
        return false;
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Take screenshot
     */
    protected byte[] takeScreenshot() {
        try {
            TakesScreenshot screenshot = (TakesScreenshot) driver;
            return screenshot.getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            logger.error("Failed to take screenshot", e);
            return new byte[0];
        }
    }
    
    /**
     * Get page title
     */
    protected String getPageTitle() {
        return driver.getTitle();
    }
    
    /**
     * Get current URL
     */
    protected String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
    
    /**
     * Sleep for specified milliseconds
     */
    protected void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Sleep interrupted", e);
        }
    }
    
    /**
     * Get element description for logging
     */
    private String getElementDescription(WebElement element) {
        try {
            String tagName = element.getTagName();
            String id = element.getAttribute("id");
            String className = element.getAttribute("class");
            
            StringBuilder description = new StringBuilder(tagName);
            if (id != null && !id.isEmpty()) {
                description.append("#").append(id);
            }
            if (className != null && !className.isEmpty()) {
                description.append(".").append(className.split(" ")[0]);
            }
            
            return description.toString();
        } catch (Exception e) {
            return "Unknown element";
        }
    }
    
    /**
     * Wait for page to load completely
     */
    protected void waitForPageLoad() {
        wait.until(webDriver -> executeScript("return document.readyState").equals("complete"));
        logger.debug("Page loaded completely");
    }
    
    /**
     * Clear browser cache
     */
    protected void clearBrowserData() {
        driver.manage().deleteAllCookies();
        executeScript("window.localStorage.clear();");
        executeScript("window.sessionStorage.clear();");
        logger.debug("Browser data cleared");
    }
    
    /**
     * Abstract method to be implemented by page classes for validation
     */
    public abstract boolean isPageLoaded();
    
    /**
     * Get page URL for validation
     */
    public abstract String getExpectedUrl();
}