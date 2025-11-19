package com.phoenix.hrm.config;

/**
 * Browser Type Enumeration for Cross-Browser Testing
 * 
 * Defines all supported browsers with their identifiers and capabilities
 * for the Phoenix HRM Test Automation Framework.
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 3.0
 * @since Phase 3
 */
public enum BrowserType {
    
    // Desktop Browsers
    CHROME("chrome", "Google Chrome", true, true, true),
    FIREFOX("firefox", "Mozilla Firefox", true, true, true),
    EDGE("edge", "Microsoft Edge", true, true, true),
    SAFARI("safari", "Safari", false, true, false), // macOS only
    OPERA("opera", "Opera", true, true, false),
    INTERNET_EXPLORER("ie", "Internet Explorer", false, false, false), // Legacy support
    
    // Mobile Browsers
    CHROME_MOBILE("chrome-mobile", "Chrome Mobile", false, false, false),
    FIREFOX_MOBILE("firefox-mobile", "Firefox Mobile", false, false, false),
    SAFARI_MOBILE("safari-mobile", "Safari Mobile", false, false, false),
    
    // Remote/Cloud Browsers
    CHROME_REMOTE("chrome-remote", "Chrome (Remote)", true, true, true),
    FIREFOX_REMOTE("firefox-remote", "Firefox (Remote)", true, true, true),
    EDGE_REMOTE("edge-remote", "Edge (Remote)", true, true, true),
    SAFARI_REMOTE("safari-remote", "Safari (Remote)", false, true, false),
    
    // Headless Browsers
    CHROME_HEADLESS("chrome-headless", "Chrome Headless", true, false, true),
    FIREFOX_HEADLESS("firefox-headless", "Firefox Headless", true, false, true),
    EDGE_HEADLESS("edge-headless", "Edge Headless", true, false, true),
    
    // Docker Browsers
    CHROME_DOCKER("chrome-docker", "Chrome (Docker)", true, true, true),
    FIREFOX_DOCKER("firefox-docker", "Firefox (Docker)", true, true, true),
    EDGE_DOCKER("edge-docker", "Edge (Docker)", true, true, true);
    
    private final String browserKey;
    private final String displayName;
    private final boolean supportsHeadless;
    private final boolean supportsScreenshots;
    private final boolean supportsDownloads;
    
    /**
     * Constructor for BrowserType enum
     * 
     * @param browserKey The key used to identify the browser
     * @param displayName The human-readable name of the browser
     * @param supportsHeadless Whether the browser supports headless mode
     * @param supportsScreenshots Whether the browser supports taking screenshots
     * @param supportsDownloads Whether the browser supports file downloads
     */
    BrowserType(String browserKey, String displayName, boolean supportsHeadless, 
               boolean supportsScreenshots, boolean supportsDownloads) {
        this.browserKey = browserKey;
        this.displayName = displayName;
        this.supportsHeadless = supportsHeadless;
        this.supportsScreenshots = supportsScreenshots;
        this.supportsDownloads = supportsDownloads;
    }
    
    /**
     * Get the browser key identifier
     * 
     * @return The browser key
     */
    public String getBrowserKey() {
        return browserKey;
    }
    
    /**
     * Get the display name of the browser
     * 
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if the browser supports headless mode
     * 
     * @return true if headless mode is supported
     */
    public boolean supportsHeadless() {
        return supportsHeadless;
    }
    
    /**
     * Check if the browser supports taking screenshots
     * 
     * @return true if screenshots are supported
     */
    public boolean supportsScreenshots() {
        return supportsScreenshots;
    }
    
    /**
     * Check if the browser supports file downloads
     * 
     * @return true if downloads are supported
     */
    public boolean supportsDownloads() {
        return supportsDownloads;
    }
    
    /**
     * Check if this is a mobile browser
     * 
     * @return true if it's a mobile browser
     */
    public boolean isMobile() {
        return browserKey.contains("mobile");
    }
    
    /**
     * Check if this is a remote browser
     * 
     * @return true if it's a remote browser
     */
    public boolean isRemote() {
        return browserKey.contains("remote") || browserKey.contains("docker");
    }
    
    /**
     * Check if this is a headless browser
     * 
     * @return true if it's configured for headless execution
     */
    public boolean isHeadless() {
        return browserKey.contains("headless");
    }
    
    /**
     * Check if this is a Docker-based browser
     * 
     * @return true if it's a Docker browser
     */
    public boolean isDocker() {
        return browserKey.contains("docker");
    }
    
    /**
     * Get the base browser type (removes modifiers like -headless, -remote, etc.)
     * 
     * @return The base browser type
     */
    public BrowserType getBaseType() {
        String baseName = browserKey.split("-")[0];
        
        switch (baseName.toLowerCase()) {
            case "chrome":
                return CHROME;
            case "firefox":
                return FIREFOX;
            case "edge":
                return EDGE;
            case "safari":
                return SAFARI;
            case "opera":
                return OPERA;
            case "ie":
                return INTERNET_EXPLORER;
            default:
                return this;
        }
    }
    
    /**
     * Find BrowserType by browser key
     * 
     * @param browserKey The browser key to search for
     * @return The matching BrowserType, or CHROME as default
     */
    public static BrowserType fromString(String browserKey) {
        if (browserKey == null || browserKey.trim().isEmpty()) {
            return CHROME; // Default browser
        }
        
        String normalizedKey = browserKey.toLowerCase().trim();
        
        for (BrowserType type : values()) {
            if (type.getBrowserKey().equals(normalizedKey)) {
                return type;
            }
        }
        
        // Fallback matching for common browser names
        switch (normalizedKey) {
            case "google chrome":
            case "googlechrome":
                return CHROME;
            case "mozilla firefox":
            case "mozillafirefox":
                return FIREFOX;
            case "microsoft edge":
            case "microsoftedge":
                return EDGE;
            case "internet explorer":
            case "internetexplorer":
                return INTERNET_EXPLORER;
            default:
                return CHROME; // Default fallback
        }
    }
    
    /**
     * Get all desktop browser types
     * 
     * @return Array of desktop browser types
     */
    public static BrowserType[] getDesktopBrowsers() {
        return new BrowserType[]{CHROME, FIREFOX, EDGE, SAFARI, OPERA};
    }
    
    /**
     * Get all mobile browser types
     * 
     * @return Array of mobile browser types
     */
    public static BrowserType[] getMobileBrowsers() {
        return new BrowserType[]{CHROME_MOBILE, FIREFOX_MOBILE, SAFARI_MOBILE};
    }
    
    /**
     * Get all headless browser types
     * 
     * @return Array of headless browser types
     */
    public static BrowserType[] getHeadlessBrowsers() {
        return new BrowserType[]{CHROME_HEADLESS, FIREFOX_HEADLESS, EDGE_HEADLESS};
    }
    
    /**
     * Get all remote/cloud browser types
     * 
     * @return Array of remote browser types
     */
    public static BrowserType[] getRemoteBrowsers() {
        return new BrowserType[]{CHROME_REMOTE, FIREFOX_REMOTE, EDGE_REMOTE, SAFARI_REMOTE};
    }
    
    /**
     * Get all Docker-based browser types
     * 
     * @return Array of Docker browser types
     */
    public static BrowserType[] getDockerBrowsers() {
        return new BrowserType[]{CHROME_DOCKER, FIREFOX_DOCKER, EDGE_DOCKER};
    }
    
    @Override
    public String toString() {
        return String.format("%s (%s)", displayName, browserKey);
    }
}