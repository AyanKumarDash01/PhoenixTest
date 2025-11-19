package com.phoenix.hrm.core.driver;

import com.phoenix.hrm.core.config.ConfigManager;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced WebDriver Factory for Phoenix HRM Test Automation Framework
 * Supports multiple browsers, remote execution, and advanced configurations
 */
public class WebDriverFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(WebDriverFactory.class);
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();
    private static final ConfigManager config = ConfigManager.getInstance();
    
    // Browser types
    public enum BrowserType {
        CHROME, FIREFOX, EDGE, SAFARI, REMOTE
    }
    
    // Prevent instantiation
    private WebDriverFactory() {}
    
    /**
     * Create WebDriver based on configuration
     */
    public static WebDriver createDriver() {
        String browserName = config.getBrowser().toLowerCase();
        return createDriver(BrowserType.valueOf(browserName.toUpperCase()));
    }
    
    /**
     * Create WebDriver for specific browser type
     */
    public static WebDriver createDriver(BrowserType browserType) {
        logger.info("Creating WebDriver for browser: {}", browserType);
        
        WebDriver driver;
        
        switch (browserType) {
            case CHROME:
                driver = createChromeDriver();
                break;
            case FIREFOX:
                driver = createFirefoxDriver();
                break;
            case EDGE:
                driver = createEdgeDriver();
                break;
            case SAFARI:
                driver = createSafariDriver();
                break;
            case REMOTE:
                driver = createRemoteDriver();
                break;
            default:
                logger.warn("Unsupported browser type: {}. Defaulting to Chrome.", browserType);
                driver = createChromeDriver();
        }
        
        configureDriver(driver);
        setDriver(driver);
        
        logger.info("WebDriver created successfully: {}", driver.getClass().getSimpleName());
        return driver;
    }
    
    /**
     * Create Chrome WebDriver with optimized options
     */
    private static WebDriver createChromeDriver() {
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
        options.addArguments("--disable-popup-blocking");
        
        // Window size
        options.addArguments("--window-size=1920,1080");
        
        // Headless mode
        if (config.isHeadless()) {
            options.addArguments("--headless");
            logger.info("Chrome running in headless mode");
        }
        
        // Additional Chrome preferences
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.default_content_setting_values.notifications", 2);
        prefs.put("profile.default_content_settings.popups", 0);
        prefs.put("profile.managed_default_content_settings.images", 2);
        options.setExperimentalOption("prefs", prefs);
        
        // Disable logging
        options.addArguments("--log-level=3");
        options.addArguments("--silent");
        
        return new ChromeDriver(options);
    }
    
    /**
     * Create Firefox WebDriver with optimized options
     */
    private static WebDriver createFirefoxDriver() {
        WebDriverManager.firefoxdriver().setup();
        
        FirefoxOptions options = new FirefoxOptions();
        
        // Headless mode
        if (config.isHeadless()) {
            options.addArguments("--headless");
            logger.info("Firefox running in headless mode");
        }
        
        // Performance optimizations
        options.addArguments("--width=1920");
        options.addArguments("--height=1080");
        
        // Firefox preferences for performance
        options.addPreference("dom.webnotifications.enabled", false);
        options.addPreference("dom.push.enabled", false);
        options.addPreference("media.navigator.permission.disabled", true);
        options.addPreference("permissions.default.image", 2);
        
        return new FirefoxDriver(options);
    }
    
    /**
     * Create Edge WebDriver with optimized options
     */
    private static WebDriver createEdgeDriver() {
        WebDriverManager.edgedriver().setup();
        
        EdgeOptions options = new EdgeOptions();
        
        // Performance optimizations
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        
        // Headless mode
        if (config.isHeadless()) {
            options.addArguments("--headless");
            logger.info("Edge running in headless mode");
        }
        
        return new EdgeDriver(options);
    }
    
    /**
     * Create Safari WebDriver
     */
    private static WebDriver createSafariDriver() {
        if (!System.getProperty("os.name").toLowerCase().contains("mac")) {
            throw new UnsupportedOperationException("Safari is only supported on macOS");
        }
        
        logger.info("Safari WebDriver created - Note: Safari doesn't support headless mode");
        return new SafariDriver();
    }
    
    /**
     * Create Remote WebDriver for Selenium Grid
     */
    private static WebDriver createRemoteDriver() {
        String gridUrl = config.getProperty("grid.url", "http://localhost:4444/wd/hub");
        String browser = config.getBrowser().toLowerCase();
        
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setBrowserName(browser);
        capabilities.setPlatform(Platform.ANY);
        
        // Add browser-specific options to capabilities
        switch (browser) {
            case "chrome":
                ChromeOptions chromeOptions = new ChromeOptions();
                if (config.isHeadless()) {
                    chromeOptions.addArguments("--headless");
                }
                chromeOptions.addArguments("--no-sandbox", "--disable-dev-shm-usage");
                capabilities.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
                break;
                
            case "firefox":
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                if (config.isHeadless()) {
                    firefoxOptions.addArguments("--headless");
                }
                capabilities.setCapability(FirefoxOptions.FIREFOX_OPTIONS, firefoxOptions);
                break;
        }
        
        try {
            logger.info("Creating Remote WebDriver with Grid URL: {}", gridUrl);
            return new RemoteWebDriver(new URL(gridUrl), capabilities);
        } catch (MalformedURLException e) {
            logger.error("Invalid Grid URL: {}", gridUrl, e);
            throw new RuntimeException("Failed to create Remote WebDriver", e);
        }
    }
    
    /**
     * Configure WebDriver with timeouts and other settings
     */
    private static void configureDriver(WebDriver driver) {
        logger.debug("Configuring WebDriver settings...");
        
        // Set timeouts
        driver.manage().timeouts()
            .implicitlyWait(Duration.ofSeconds(config.getImplicitWait()));
        driver.manage().timeouts()
            .pageLoadTimeout(Duration.ofSeconds(30));
        driver.manage().timeouts()
            .scriptTimeout(Duration.ofSeconds(30));
        
        // Maximize window if not headless
        if (!config.isHeadless()) {
            driver.manage().window().maximize();
        }
        
        logger.debug("WebDriver configured successfully");
    }
    
    /**
     * Get current WebDriver instance for this thread
     */
    public static WebDriver getDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver == null) {
            logger.warn("No WebDriver found for current thread. Creating new instance.");
            driver = createDriver();
        }
        return driver;
    }
    
    /**
     * Set WebDriver instance for this thread
     */
    public static void setDriver(WebDriver driver) {
        driverThreadLocal.set(driver);
    }
    
    /**
     * Quit WebDriver and clean up thread local
     */
    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                logger.info("Quitting WebDriver: {}", driver.getClass().getSimpleName());
                driver.quit();
            } catch (Exception e) {
                logger.warn("Error occurred while quitting WebDriver: {}", e.getMessage());
            } finally {
                driverThreadLocal.remove();
            }
        } else {
            logger.warn("No WebDriver found to quit for current thread");
        }
    }
    
    /**
     * Close current WebDriver instance
     */
    public static void closeDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                driver.close();
            } catch (Exception e) {
                logger.warn("Error occurred while closing WebDriver: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Get driver information for logging
     */
    public static String getDriverInfo() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            return String.format("Driver: %s, Session: %s", 
                driver.getClass().getSimpleName(),
                driver.toString().substring(driver.toString().lastIndexOf('(') + 1, driver.toString().lastIndexOf(')')));
        }
        return "No active driver";
    }
    
    /**
     * Check if WebDriver is active
     */
    public static boolean isDriverActive() {
        WebDriver driver = driverThreadLocal.get();
        if (driver == null) {
            return false;
        }
        
        try {
            driver.getCurrentUrl();
            return true;
        } catch (Exception e) {
            logger.debug("WebDriver is not active: {}", e.getMessage());
            return false;
        }
    }
}