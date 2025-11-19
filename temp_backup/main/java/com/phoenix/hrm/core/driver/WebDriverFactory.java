package com.phoenix.hrm.core.driver;

import com.phoenix.hrm.core.config.ConfigManager;
import com.phoenix.hrm.core.exceptions.FrameworkException;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Enterprise WebDriverFactory implementing ThreadLocal pattern for thread-safe browser management.
 * Supports multiple browsers with automatic driver management via WebDriverManager.
 * 
 * Features:
 * - Thread-safe WebDriver instances using ThreadLocal
 * - Automatic driver download and management
 * - Configurable browser options and capabilities
 * - Support for headless execution
 * - Cross-browser compatibility (Chrome, Firefox, Edge)
 * - Mobile emulation support
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 1.0
 */
public class WebDriverFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(WebDriverFactory.class);
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();
    private static final ConfigManager config = ConfigManager.getInstance();
    
    // Browser type enumeration
    public enum BrowserType {
        CHROME, FIREFOX, EDGE, CHROME_HEADLESS, FIREFOX_HEADLESS, EDGE_HEADLESS
    }
    
    /**
     * Private constructor to prevent instantiation
     */
    private WebDriverFactory() {
        throw new UnsupportedOperationException("WebDriverFactory is a utility class and cannot be instantiated");
    }
    
    /**
     * Creates and sets WebDriver instance for current thread
     * 
     * @param browserType Type of browser to create
     * @return WebDriver instance
     * @throws FrameworkException if driver creation fails
     */
    public static synchronized WebDriver createDriver(BrowserType browserType) {
        try {
            logger.info("Creating WebDriver instance for browser type: {}", browserType);
            
            WebDriver driver = switch (browserType) {
                case CHROME -> createChromeDriver(false);
                case CHROME_HEADLESS -> createChromeDriver(true);
                case FIREFOX -> createFirefoxDriver(false);
                case FIREFOX_HEADLESS -> createFirefoxDriver(true);
                case EDGE -> createEdgeDriver(false);
                case EDGE_HEADLESS -> createEdgeDriver(true);
            };
            
            configureDriver(driver);
            driverThreadLocal.set(driver);
            
            logger.info("WebDriver instance created successfully for thread: {}", Thread.currentThread().getName());
            return driver;
            
        } catch (Exception e) {
            logger.error("Failed to create WebDriver instance for browser type: {}", browserType, e);
            throw new FrameworkException("Failed to create WebDriver instance: " + e.getMessage(), e);
        }
    }
    
    /**
     * Creates Chrome WebDriver with enterprise-grade options
     * 
     * @param headless Whether to run in headless mode
     * @return ChromeDriver instance
     */
    private static WebDriver createChromeDriver(boolean headless) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        
        // Performance optimizations
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-plugins");
        options.addArguments("--disable-images");
        options.addArguments("--disable-javascript");
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-popup-blocking");
        
        // Security and stability
        options.addArguments("--no-first-run");
        options.addArguments("--disable-default-apps");
        options.addArguments("--disable-infobars");
        options.addArguments("--disable-web-security");
        options.addArguments("--allow-running-insecure-content");
        
        // Window management
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--start-maximized");
        
        if (headless) {
            options.addArguments("--headless=new");
            logger.info("Chrome browser configured for headless execution");
        }
        
        // Download preferences
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.default_content_settings.popups", 0);
        prefs.put("profile.default_content_setting_values.notifications", 2);
        options.setExperimentalOption("prefs", prefs);
        
        // Set page load strategy
        options.setPageLoadStrategy(org.openqa.selenium.PageLoadStrategy.NORMAL);
        
        return new ChromeDriver(options);
    }
    
    /**
     * Creates Firefox WebDriver with enterprise-grade options
     * 
     * @param headless Whether to run in headless mode
     * @return FirefoxDriver instance
     */
    private static WebDriver createFirefoxDriver(boolean headless) {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();
        
        // Performance optimizations
        options.addArguments("--width=1920");
        options.addArguments("--height=1080");
        
        if (headless) {
            options.addArguments("--headless");
            logger.info("Firefox browser configured for headless execution");
        }
        
        // Preferences for stability
        options.addPreference("dom.webnotifications.enabled", false);
        options.addPreference("media.volume_scale", "0.0");
        options.addPreference("browser.helperApps.neverAsk.saveToDisk", "application/pdf");
        
        return new FirefoxDriver(options);
    }
    
    /**
     * Creates Edge WebDriver with enterprise-grade options
     * 
     * @param headless Whether to run in headless mode
     * @return EdgeDriver instance
     */
    private static WebDriver createEdgeDriver(boolean headless) {
        WebDriverManager.edgedriver().setup();
        EdgeOptions options = new EdgeOptions();
        
        // Performance optimizations
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--start-maximized");
        
        if (headless) {
            options.addArguments("--headless");
            logger.info("Edge browser configured for headless execution");
        }
        
        return new EdgeDriver(options);
    }
    
    /**
     * Configures WebDriver with common settings
     * 
     * @param driver WebDriver instance to configure
     */
    private static void configureDriver(WebDriver driver) {
        // Set timeouts from configuration
        int implicitWait = config.getImplicitWait();
        int pageLoadTimeout = config.getPageLoadTimeout();
        int scriptTimeout = config.getScriptTimeout();
        
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(pageLoadTimeout));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(scriptTimeout));
        
        // Maximize window if not headless
        if (!config.isHeadless()) {
            driver.manage().window().maximize();
        }
        
        logger.debug("WebDriver configured with timeouts - Implicit: {}s, PageLoad: {}s, Script: {}s", 
                    implicitWait, pageLoadTimeout, scriptTimeout);
    }
    
    /**
     * Gets WebDriver instance for current thread
     * 
     * @return WebDriver instance or null if not set
     */
    public static WebDriver getDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver == null) {
            logger.warn("No WebDriver instance found for thread: {}", Thread.currentThread().getName());
        }
        return driver;
    }
    
    /**
     * Checks if WebDriver instance exists for current thread
     * 
     * @return true if driver exists, false otherwise
     */
    public static boolean hasDriver() {
        return driverThreadLocal.get() != null;
    }
    
    /**
     * Closes and removes WebDriver instance for current thread
     */
    public static void closeDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                logger.info("Closing WebDriver instance for thread: {}", Thread.currentThread().getName());
                driver.quit();
            } catch (Exception e) {
                logger.warn("Error occurred while closing WebDriver: {}", e.getMessage());
            } finally {
                driverThreadLocal.remove();
                logger.info("WebDriver instance removed from ThreadLocal for thread: {}", Thread.currentThread().getName());
            }
        }
    }
    
    /**
     * Creates WebDriver instance based on browser name from configuration
     * 
     * @param browserName Browser name (chrome, firefox, edge)
     * @return WebDriver instance
     */
    public static WebDriver createDriver(String browserName) {
        String browser = browserName.toLowerCase();
        boolean headless = config.isHeadless();
        
        BrowserType browserType = switch (browser) {
            case "chrome" -> headless ? BrowserType.CHROME_HEADLESS : BrowserType.CHROME;
            case "firefox" -> headless ? BrowserType.FIREFOX_HEADLESS : BrowserType.FIREFOX;
            case "edge" -> headless ? BrowserType.EDGE_HEADLESS : BrowserType.EDGE;
            default -> throw new FrameworkException("Unsupported browser: " + browserName);
        };
        
        return createDriver(browserType);
    }
    
    /**
     * Creates WebDriver instance using configuration
     * 
     * @return WebDriver instance
     */
    public static WebDriver createDriver() {
        String browserName = config.getBrowser();
        return createDriver(browserName);
    }
}