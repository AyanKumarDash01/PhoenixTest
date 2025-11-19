package com.phoenix.hrm.pages;

import com.phoenix.hrm.core.pages.BasePage;
import com.phoenix.hrm.core.data.TestDataManager;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Enhanced Login Page Object for Phoenix HRM
 * Demonstrates the usage of enhanced framework components
 */
public class LoginPage extends BasePage {
    
    private static final Logger logger = LoggerFactory.getLogger(LoginPage.class);
    private final TestDataManager testDataManager = TestDataManager.getInstance();
    
    // Page Elements using PageFactory
    @FindBy(name = "username")
    private WebElement usernameField;
    
    @FindBy(name = "password")
    private WebElement passwordField;
    
    @FindBy(css = "button[type='submit']")
    private WebElement loginButton;
    
    @FindBy(xpath = "//p[contains(@class, 'alert-content-text')]")
    private WebElement errorMessage;
    
    @FindBy(css = ".orangehrm-login-branding img")
    private WebElement logoImage;
    
    @FindBy(css = ".orangehrm-login-title")
    private WebElement loginTitle;
    
    @FindBy(css = ".orangehrm-login-forgot-password")
    private WebElement forgotPasswordLink;
    
    @FindBy(css = ".orangehrm-login-footer-sm")
    private WebElement socialMediaLinks;
    
    /**
     * Navigate to login page
     */
    public LoginPage navigateToLoginPage() {
        String loginUrl = config.getBaseUrl();
        navigateTo(loginUrl);
        waitForPageLoad();
        logger.info("Navigated to login page: {}", loginUrl);
        return this;
    }
    
    /**
     * Login with provided credentials
     */
    public DashboardPage login(String username, String password) {
        logger.info("Attempting to login with username: {}", username);
        
        // Clear and enter username
        safeType(usernameField, username);
        
        // Clear and enter password
        safeType(passwordField, password);
        
        // Click login button
        safeClick(loginButton);
        
        // Wait for navigation
        waitForPageLoad();
        
        logger.info("Login attempt completed for user: {}", username);
        return new DashboardPage();
    }
    
    /**
     * Login with admin credentials from test data
     */
    public DashboardPage loginAsAdmin() {
        Map<String, String> adminCredentials = testDataManager.getAdminCredentials();
        
        if (adminCredentials.isEmpty()) {
            logger.error("Admin credentials not found in test data");
            throw new RuntimeException("Admin credentials not available");
        }
        
        String username = adminCredentials.get("username");
        String password = adminCredentials.get("password");
        
        return login(username, password);
    }
    
    /**
     * Login with user credentials by role
     */
    public DashboardPage loginByRole(String role) {
        Map<String, String> userCredentials = testDataManager.getUserCredentials(role);
        
        if (userCredentials.isEmpty()) {
            logger.error("Credentials not found for role: {}", role);
            throw new RuntimeException("User credentials not available for role: " + role);
        }
        
        String username = userCredentials.get("username");
        String password = userCredentials.get("password");
        
        return login(username, password);
    }
    
    /**
     * Attempt invalid login
     */
    public LoginPage attemptInvalidLogin(String username, String password) {
        logger.info("Attempting invalid login with username: {}", username);
        
        safeType(usernameField, username);
        safeType(passwordField, password);
        safeClick(loginButton);
        
        // Wait for error message
        waitForVisibility(errorMessage);
        
        logger.info("Invalid login attempt completed");
        return this;
    }
    
    /**
     * Get error message text
     */
    public String getErrorMessage() {
        if (isElementDisplayed(errorMessage)) {
            String errorText = getElementText(errorMessage);
            logger.debug("Error message displayed: {}", errorText);
            return errorText;
        }
        return "";
    }
    
    /**
     * Check if error message is displayed
     */
    public boolean isErrorMessageDisplayed() {
        return isElementDisplayed(errorMessage);
    }
    
    /**
     * Get login page title
     */
    public String getLoginTitle() {
        return getElementText(loginTitle);
    }
    
    /**
     * Check if logo is displayed
     */
    public boolean isLogoDisplayed() {
        return isElementDisplayed(logoImage);
    }
    
    /**
     * Click forgot password link
     */
    public ForgotPasswordPage clickForgotPassword() {
        safeClick(forgotPasswordLink);
        waitForPageLoad();
        return new ForgotPasswordPage();
    }
    
    /**
     * Clear username field
     */
    public LoginPage clearUsername() {
        waitForVisibility(usernameField);
        usernameField.clear();
        return this;
    }
    
    /**
     * Clear password field
     */
    public LoginPage clearPassword() {
        waitForVisibility(passwordField);
        passwordField.clear();
        return this;
    }
    
    /**
     * Enter username
     */
    public LoginPage enterUsername(String username) {
        safeType(usernameField, username);
        return this;
    }
    
    /**
     * Enter password
     */
    public LoginPage enterPassword(String password) {
        safeType(passwordField, password);
        return this;
    }
    
    /**
     * Click login button
     */
    public void clickLoginButton() {
        safeClick(loginButton);
        waitForPageLoad();
    }
    
    /**
     * Get username field placeholder
     */
    public String getUsernamePlaceholder() {
        return getElementAttribute(usernameField, "placeholder");
    }
    
    /**
     * Get password field placeholder
     */
    public String getPasswordPlaceholder() {
        return getElementAttribute(passwordField, "placeholder");
    }
    
    /**
     * Check if social media links are displayed
     */
    public boolean areSocialMediaLinksDisplayed() {
        return isElementDisplayed(socialMediaLinks);
    }
    
    /**
     * Validate login form elements are present
     */
    public boolean validateLoginFormElements() {
        boolean usernamePresent = isElementDisplayed(usernameField);
        boolean passwordPresent = isElementDisplayed(passwordField);
        boolean loginButtonPresent = isElementDisplayed(loginButton);
        boolean logoPresent = isElementDisplayed(logoImage);
        
        logger.debug("Login form validation - Username: {}, Password: {}, Button: {}, Logo: {}",
            usernamePresent, passwordPresent, loginButtonPresent, logoPresent);
        
        return usernamePresent && passwordPresent && loginButtonPresent && logoPresent;
    }
    
    /**
     * Take screenshot of login page
     */
    public byte[] takeLoginPageScreenshot() {
        return takeScreenshot();
    }
    
    // Abstract method implementations from BasePage
    
    @Override
    public boolean isPageLoaded() {
        try {
            waitForVisibility(usernameField);
            waitForVisibility(passwordField);
            waitForVisibility(loginButton);
            
            boolean elementsVisible = isElementDisplayed(usernameField) && 
                                    isElementDisplayed(passwordField) && 
                                    isElementDisplayed(loginButton);
            
            logger.debug("Login page loaded status: {}", elementsVisible);
            return elementsVisible;
            
        } catch (Exception e) {
            logger.warn("Failed to verify login page load status", e);
            return false;
        }
    }
    
    @Override
    public String getExpectedUrl() {
        return config.getBaseUrl() + "/auth/login";
    }
    
    /**
     * Verify current URL matches expected login URL
     */
    public boolean isOnLoginPage() {
        String currentUrl = getCurrentUrl();
        String expectedUrl = getExpectedUrl();
        
        boolean urlMatches = currentUrl.contains("/auth/login") || currentUrl.equals(config.getBaseUrl());
        logger.debug("URL verification - Current: {}, Expected pattern: login, Matches: {}", 
            currentUrl, urlMatches);
        
        return urlMatches;
    }
}

/**
 * Placeholder classes for demonstration - these would be implemented as full page objects
 */
class DashboardPage extends BasePage {
    @Override
    public boolean isPageLoaded() {
        // Implementation would check for dashboard-specific elements
        return getCurrentUrl().contains("/dashboard");
    }
    
    @Override
    public String getExpectedUrl() {
        return config.getBaseUrl() + "/web/index.php/dashboard/index";
    }
}

class ForgotPasswordPage extends BasePage {
    @Override
    public boolean isPageLoaded() {
        // Implementation would check for forgot password form elements
        return getCurrentUrl().contains("/requestPasswordResetCode");
    }
    
    @Override
    public String getExpectedUrl() {
        return config.getBaseUrl() + "/auth/requestPasswordResetCode";
    }
}
