package com.phoenix.hrm.factory;

import com.phoenix.hrm.config.BrowserType;
import com.phoenix.hrm.config.ConfigurationManager;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced WebDriver Factory for Cross-Browser Testing
 * 
 * Provides comprehensive WebDriver creation and management with support for:
 * - Local browsers (Chrome, Firefox, Edge, Safari)
 * - Remote browsers (Selenium Grid, cloud services)
 * - Docker containers
 * - Mobile browsers
 * - Browser-specific configurations and capabilities
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 3.0
 * @since Phase 3
 */
public class EnhancedWebDriverFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedWebDriverFactory.class);
    
    // Thread-safe storage for WebDriver instances
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();
    
    // Cache for browser capabilities
    private static final Map<String, DesiredCapabilities> capabilitiesCache = new ConcurrentHashMap<>();
    
    // Default timeouts
    private static final Duration DEFAULT_IMPLICIT_WAIT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_PAGE_LOAD_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_SCRIPT_TIMEOUT = Duration.ofSeconds(20);
    
    /**
     * Create WebDriver instance based on browser type
     * 
     * @param browserType The type of browser to create
     * @return WebDriver instance
     */
    public static WebDriver createDriver(BrowserType browserType) {
        logger.info("Creating WebDriver for browser: {}", browserType.getDisplayName());
        
        WebDriver driver;
        
        try {
            if (browserType.isRemote()) {
                driver = createRemoteDriver(browserType);
            } else {
                driver = createLocalDriver(browserType);
            }
            
            // Configure default timeouts
            configureTimeouts(driver);
            
            // Store in thread-local storage
            driverThreadLocal.set(driver);
            
            logger.info("WebDriver created successfully: {}", browserType.getDisplayName());
            return driver;
            
        } catch (Exception e) {
            logger.error("Failed to create WebDriver for browser: {} - {}", browserType.getDisplayName(), e.getMessage());
            throw new RuntimeException("WebDriver creation failed", e);
        }
    }
    
    /**
     * Create local WebDriver instance
     * 
     * @param browserType The browser type
     * @return WebDriver instance
     */
    private static WebDriver createLocalDriver(BrowserType browserType) {
        switch (browserType.getBaseType()) {
            case CHROME:
                return createChromeDriver(browserType);
            case FIREFOX:
                return createFirefoxDriver(browserType);
            case EDGE:
                return createEdgeDriver(browserType);
            case SAFARI:
                return createSafariDriver(browserType);
            default:
                logger.warn("Unsupported browser type: {}. Falling back to Chrome.", browserType);
                return createChromeDriver(BrowserType.CHROME);
        }
    }
    
    /**
     * Create Chrome WebDriver with enhanced options
     * 
     * @param browserType The browser type (includes headless, mobile variants)
     * @return Chrome WebDriver instance
     */
    private static WebDriver createChromeDriver(BrowserType browserType) {
        logger.info("Setting up Chrome WebDriver: {}", browserType.getBrowserKey());
        
        // Setup WebDriverManager
        WebDriverManager.chromedriver().setup();
        
        ChromeOptions options = new ChromeOptions();
        
        // Basic Chrome arguments for stability and performance
        options.addArguments(
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--disable-extensions",
            "--disable-infobars",
            "--disable-notifications",
            "--disable-web-security",
            "--allow-running-insecure-content",
            "--ignore-certificate-errors",
            "--ignore-ssl-errors",
            "--ignore-certificate-errors-spki",
            "--ignore-insecure-certs-errors"
        );
        
        // Handle headless mode
        if (browserType.isHeadless() || ConfigurationManager.isHeadless()) {
            options.addArguments("--headless=new");
            logger.info("Chrome running in headless mode");
        }
        
        // Window size configuration
        String windowSize = ConfigurationManager.getProperty("browser.window.size", "1920,1080");
        options.addArguments("--window-size=" + windowSize);
        
        // Download directory configuration
        if (browserType.supportsDownloads()) {
            String downloadDir = ConfigurationManager.getProperty("browser.download.dir", 
                System.getProperty("user.dir") + "/test-output/downloads");
            
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("download.default_directory", downloadDir);
            prefs.put("download.prompt_for_download", false);
            prefs.put("download.directory_upgrade", true);
            prefs.put("plugins.always_open_pdf_externally", true);
            options.setExperimentalOption("prefs", prefs);
        }
        
        // Mobile emulation for Chrome Mobile
        if (browserType.isMobile()) {
            Map<String, Object> mobileEmulation = new HashMap<>();
            String deviceName = ConfigurationManager.getProperty("mobile.device.name", "iPhone 12 Pro");
            mobileEmulation.put("deviceName", deviceName);
            options.setExperimentalOption("mobileEmulation", mobileEmulation);
            logger.info("Chrome configured for mobile emulation: {}", deviceName);
        }
        
        // Performance and debugging options
        options.addArguments("--enable-automation");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("useAutomationExtension", false);
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        
        return new ChromeDriver(options);
    }
    
    /**
     * Create Firefox WebDriver with enhanced options
     * 
     * @param browserType The browser type
     * @return Firefox WebDriver instance
     */
    private static WebDriver createFirefoxDriver(BrowserType browserType) {
        logger.info("Setting up Firefox WebDriver: {}", browserType.getBrowserKey());
        
        // Setup WebDriverManager
        WebDriverManager.firefoxdriver().setup();
        
        FirefoxOptions options = new FirefoxOptions();
        
        // Handle headless mode
        if (browserType.isHeadless() || ConfigurationManager.isHeadless()) {
            options.addArguments("--headless");
            logger.info("Firefox running in headless mode");
        }
        
        // Firefox preferences for stability
        options.addPreference("browser.download.folderList", 2);
        options.addPreference("browser.download.manager.showWhenStarting", false);
        options.addPreference("browser.helperApps.neverAsk.saveToDisk", 
            "application/pdf,application/octet-stream,text/csv,application/vnd.ms-excel");
        
        // Download directory
        if (browserType.supportsDownloads()) {
            String downloadDir = ConfigurationManager.getProperty("browser.download.dir",
                System.getProperty("user.dir") + "/test-output/downloads");
            options.addPreference("browser.download.dir", downloadDir);
        }
        
        // Security and performance preferences
        options.addPreference("security.tls.insecure_fallback_hosts", "localhost");
        options.addPreference("security.tls.hello_downgrade_check", false);
        options.addPreference("dom.webnotifications.enabled", false);
        options.addPreference("media.volume_scale", "0.0");
        
        return new FirefoxDriver(options);
    }
    
    /**
     * Create Edge WebDriver with enhanced options
     * 
     * @param browserType The browser type
     * @return Edge WebDriver instance
     */
    private static WebDriver createEdgeDriver(BrowserType browserType) {
        logger.info("Setting up Edge WebDriver: {}", browserType.getBrowserKey());
        
        // Setup WebDriverManager
        WebDriverManager.edgedriver().setup();
        
        EdgeOptions options = new EdgeOptions();
        
        // Basic Edge arguments
        options.addArguments(
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--disable-extensions"
        );
        
        // Handle headless mode
        if (browserType.isHeadless() || ConfigurationManager.isHeadless()) {
            options.addArguments("--headless");
            logger.info("Edge running in headless mode");
        }
        
        // Window size configuration
        String windowSize = ConfigurationManager.getProperty("browser.window.size", "1920,1080");
        options.addArguments("--window-size=" + windowSize);
        
        // Download preferences
        if (browserType.supportsDownloads()) {
            String downloadDir = ConfigurationManager.getProperty("browser.download.dir",
                System.getProperty("user.dir") + "/test-output/downloads");
            
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("download.default_directory", downloadDir);
            prefs.put("download.prompt_for_download", false);
            options.setExperimentalOption("prefs", prefs);
        }
        
        return new EdgeDriver(options);
    }
    
    /**
     * Create Safari WebDriver with options
     * 
     * @param browserType The browser type
     * @return Safari WebDriver instance
     */
    private static WebDriver createSafariDriver(BrowserType browserType) {
        logger.info("Setting up Safari WebDriver: {}", browserType.getBrowserKey());
        
        // Check if running on macOS
        String osName = System.getProperty("os.name").toLowerCase();
        if (!osName.contains("mac")) {
            throw new UnsupportedOperationException("Safari WebDriver is only supported on macOS");
        }
        
        SafariOptions options = new SafariOptions();
        options.setAutomaticInspection(false);
        options.setAutomaticProfiling(false);
        
        return new SafariDriver(options);
    }
    
    /**
     * Create remote WebDriver instance
     * 
     * @param browserType The browser type
     * @return RemoteWebDriver instance
     */
    private static WebDriver createRemoteDriver(BrowserType browserType) {
        logger.info("Setting up Remote WebDriver: {}", browserType.getBrowserKey());
        
        String remoteUrl = ConfigurationManager.getProperty("selenium.grid.url", "http://localhost:4444/wd/hub");
        
        try {
            DesiredCapabilities capabilities = getCapabilities(browserType);
            URL gridUrl = new URL(remoteUrl);
            
            logger.info("Connecting to Selenium Grid at: {}", remoteUrl);
            RemoteWebDriver driver = new RemoteWebDriver(gridUrl, capabilities);
            
            logger.info("Remote WebDriver session created: {}", driver.getSessionId());
            return driver;
            
        } catch (MalformedURLException e) {
            logger.error("Invalid Selenium Grid URL: {}", remoteUrl);
            throw new RuntimeException("Invalid Grid URL", e);
        }
    }
    
    /**
     * Get browser capabilities for remote execution
     * 
     * @param browserType The browser type
     * @return DesiredCapabilities for the browser
     */
    private static DesiredCapabilities getCapabilities(BrowserType browserType) {
        String cacheKey = browserType.getBrowserKey();
        
        return capabilitiesCache.computeIfAbsent(cacheKey, key -> {
            DesiredCapabilities capabilities = new DesiredCapabilities();
            
            BrowserType baseType = browserType.getBaseType();
            
            switch (baseType) {
                case CHROME:
                    capabilities.setBrowserName("chrome");
                    capabilities.setCapability(ChromeOptions.CAPABILITY, getChromeOptions(browserType));
                    break;
                case FIREFOX:
                    capabilities.setBrowserName("firefox");
                    capabilities.setCapability(FirefoxOptions.FIREFOX_OPTIONS, getFirefoxOptions(browserType));
                    break;
                case EDGE:
                    capabilities.setBrowserName("MicrosoftEdge");
                    capabilities.setCapability(EdgeOptions.CAPABILITY, getEdgeOptions(browserType));
                    break;
                case SAFARI:
                    capabilities.setBrowserName("safari");
                    break;
            }
            
            // Add common capabilities
            capabilities.setCapability("platformName", getPlatformName());
            capabilities.setCapability("browserVersion", "latest");
            
            // Cloud provider specific capabilities
            addCloudCapabilities(capabilities, browserType);
            
            return capabilities;
        });
    }
    
    /**
     * Get Chrome options for remote execution
     */
    private static ChromeOptions getChromeOptions(BrowserType browserType) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage");
        
        if (browserType.isHeadless() || ConfigurationManager.isHeadless()) {
            options.addArguments("--headless=new");
        }
        
        return options;
    }
    
    /**
     * Get Firefox options for remote execution
     */
    private static FirefoxOptions getFirefoxOptions(BrowserType browserType) {
        FirefoxOptions options = new FirefoxOptions();
        
        if (browserType.isHeadless() || ConfigurationManager.isHeadless()) {
            options.addArguments("--headless");
        }
        
        return options;
    }
    
    /**
     * Get Edge options for remote execution
     */
    private static EdgeOptions getEdgeOptions(BrowserType browserType) {
        EdgeOptions options = new EdgeOptions();
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage");
        
        if (browserType.isHeadless() || ConfigurationManager.isHeadless()) {
            options.addArguments("--headless");
        }
        
        return options;
    }
    
    /**
     * Add cloud provider specific capabilities
     */
    private static void addCloudCapabilities(DesiredCapabilities capabilities, BrowserType browserType) {
        String cloudProvider = ConfigurationManager.getProperty("cloud.provider", "");
        
        switch (cloudProvider.toLowerCase()) {
            case "browserstack":
                addBrowserStackCapabilities(capabilities, browserType);
                break;
            case "saucelabs":
                addSauceLabsCapabilities(capabilities, browserType);
                break;
            case "lambdatest":
                addLambdaTestCapabilities(capabilities, browserType);
                break;
        }
    }
    
    /**
     * Add BrowserStack specific capabilities
     */
    private static void addBrowserStackCapabilities(DesiredCapabilities capabilities, BrowserType browserType) {
        capabilities.setCapability("bstack:options", Map.of(
            "os", ConfigurationManager.getProperty("browserstack.os", "Windows"),
            "osVersion", ConfigurationManager.getProperty("browserstack.os.version", "11"),
            "projectName", "Phoenix HRM Test Automation",
            "buildName", "Cross-Browser Testing Build",
            "sessionName", "Phoenix HRM - " + browserType.getDisplayName(),
            "local", ConfigurationManager.getBooleanProperty("browserstack.local", false),
            "debug", ConfigurationManager.getBooleanProperty("browserstack.debug", true),
            "networkLogs", true,
            "consoleLogs", "verbose"
        ));
    }
    
    /**
     * Add Sauce Labs specific capabilities
     */
    private static void addSauceLabsCapabilities(DesiredCapabilities capabilities, BrowserType browserType) {
        capabilities.setCapability("sauce:options", Map.of(
            "name", "Phoenix HRM - " + browserType.getDisplayName(),
            "build", "Cross-Browser Testing Build",
            "tags", new String[]{"phoenix-hrm", "cross-browser"},
            "recordVideo", true,
            "recordScreenshots", true,
            "maxDuration", 3600,
            "commandTimeout", 300
        ));
    }
    
    /**
     * Add LambdaTest specific capabilities
     */
    private static void addLambdaTestCapabilities(DesiredCapabilities capabilities, BrowserType browserType) {
        capabilities.setCapability("LT:Options", Map.of(
            "name", "Phoenix HRM - " + browserType.getDisplayName(),
            "build", "Cross-Browser Testing Build",
            "platformName", ConfigurationManager.getProperty("lambdatest.os", "Windows 11"),
            "video", true,
            "screenshot", true,
            "network", true,
            "console", true,
            "tunnel", ConfigurationManager.getBooleanProperty("lambdatest.tunnel", false)
        ));
    }
    
    /**
     * Get platform name based on current OS
     */
    private static String getPlatformName() {
        String osName = System.getProperty("os.name").toLowerCase();
        
        if (osName.contains("windows")) {
            return "Windows";
        } else if (osName.contains("mac")) {
            return "macOS";
        } else if (osName.contains("linux")) {
            return "Linux";
        } else {
            return "ANY";
        }
    }
    
    /**
     * Configure default timeouts for WebDriver
     * 
     * @param driver The WebDriver instance
     */
    private static void configureTimeouts(WebDriver driver) {
        driver.manage().timeouts().implicitlyWait(DEFAULT_IMPLICIT_WAIT);
        driver.manage().timeouts().pageLoadTimeout(DEFAULT_PAGE_LOAD_TIMEOUT);
        driver.manage().timeouts().scriptTimeout(DEFAULT_SCRIPT_TIMEOUT);
        
        // Maximize window if not headless and not mobile
        BrowserType currentType = BrowserType.fromString(ConfigurationManager.getBrowser());
        if (!currentType.isHeadless() && !currentType.isMobile() && !ConfigurationManager.isHeadless()) {
            try {
                driver.manage().window().maximize();
            } catch (Exception e) {
                logger.warn("Could not maximize browser window: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Get current WebDriver instance from thread local storage
     * 
     * @return Current WebDriver instance
     */
    public static WebDriver getDriver() {
        return driverThreadLocal.get();
    }
    
    /**
     * Quit WebDriver and remove from thread local storage
     */
    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                logger.info("Quitting WebDriver session");
                driver.quit();
            } catch (Exception e) {
                logger.warn("Error while quitting WebDriver: {}", e.getMessage());
            } finally {
                driverThreadLocal.remove();
            }
        }
    }
    
    /**
     * Clear capabilities cache
     */
    public static void clearCapabilitiesCache() {
        capabilitiesCache.clear();
        logger.info("Capabilities cache cleared");
    }
}