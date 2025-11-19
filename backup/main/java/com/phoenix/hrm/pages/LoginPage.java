package com.phoenix.hrm.pages;

import com.phoenix.hrm.core.base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

/**
 * LoginPage represents the OrangeHRM login page with enterprise POM patterns.
 * Implements fluent interface and robust element interactions for login functionality.
 * 
 * Features:
 * - PageFactory initialization with @FindBy annotations
 * - Fluent interface for method chaining
 * - Robust locator strategies with multiple fallback options
 * - Login validation and error handling
 * - Support for different user types (Admin, ESS, etc.)
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 1.0
 */
public class LoginPage extends BasePage {
    
    // Page Elements using PageFactory
    @FindBy(how = How.NAME, using = "username")
    private WebElement usernameField;
    
    @FindBy(how = How.NAME, using = "password")
    private WebElement passwordField;
    
    @FindBy(how = How.XPATH, using = "//button[@type='submit']")
    private WebElement loginButton;
    
    @FindBy(how = How.XPATH, using = "//p[@class='oxd-text oxd-text--p oxd-alert-content-text']")
    private WebElement errorMessage;
    
    @FindBy(how = How.XPATH, using = "//h5[@class='oxd-text oxd-text--h5 orangehrm-login-title']")
    private WebElement loginTitle;
    
    @FindBy(how = How.XPATH, using = "//p[@class='oxd-text oxd-text--p orangehrm-login-forgot-header']")
    private WebElement forgotPasswordLink;
    
    @FindBy(how = How.XPATH, using = "//div[@class='oxd-sheet oxd-sheet--rounded oxd-sheet--gutters oxd-sheet--gray-lighten-2 orangehrm-demo-credentials']")
    private WebElement credentialsSection;
    
    // Alternative locators for robustness
    private final By usernameFieldBy = By.name("username");
    private final By passwordFieldBy = By.name("password");
    private final By loginButtonBy = By.xpath("//button[@type='submit']");
    private final By errorMessageBy = By.xpath("//p[@class='oxd-text oxd-text--p oxd-alert-content-text']");
    private final By loginTitleBy = By.xpath("//h5[contains(@class,'orangehrm-login-title')]");
    
    /**
     * Constructor initializes LoginPage with WebDriver
     * 
     * @param driver WebDriver instance
     */
    public LoginPage(WebDriver driver) {
        super(driver);
        logger.info("Initialized LoginPage");
    }
    
    /**
     * Navigates to login page
     * 
     * @return LoginPage instance for method chaining
     */
    public LoginPage navigateToLoginPage() {
        String loginUrl = config.getBaseUrl();
        logger.info("Navigating to login page: {}", loginUrl);
        driver.get(loginUrl);
        waitForPageLoad();
        return this;
    }
    
    /**
     * Enters username in the username field
     * 
     * @param username Username to enter
     * @return LoginPage instance for method chaining
     */
    public LoginPage enterUsername(String username) {
        logger.debug("Entering username: {}", username);
        try {
            // Try PageFactory element first
            if (usernameField.isDisplayed()) {
                sendKeysToElement(usernameField, username);
            } else {
                // Fallback to By locator
                sendKeysToElement(usernameFieldBy, username);
            }
        } catch (Exception e) {
            logger.warn("Primary username locator failed, trying fallback");
            sendKeysToElement(usernameFieldBy, username);
        }
        return this;
    }
    
    /**
     * Enters password in the password field
     * 
     * @param password Password to enter
     * @return LoginPage instance for method chaining
     */
    public LoginPage enterPassword(String password) {
        logger.debug("Entering password");
        try {
            // Try PageFactory element first
            if (passwordField.isDisplayed()) {
                sendKeysToElement(passwordField, password);
            } else {
                // Fallback to By locator
                sendKeysToElement(passwordFieldBy, password);
            }
        } catch (Exception e) {
            logger.warn("Primary password locator failed, trying fallback");
            sendKeysToElement(passwordFieldBy, password);
        }
        return this;
    }
    
    /**
     * Clicks the login button
     * 
     * @return DashboardPage instance after successful login, or LoginPage if login fails
     */
    public <T extends BasePage> T clickLoginButton() {
        logger.debug("Clicking login button");
        try {
            // Try PageFactory element first
            if (loginButton.isDisplayed()) {
                clickElement(loginButton);
            } else {
                // Fallback to By locator
                clickElement(loginButtonBy);
            }
        } catch (Exception e) {
            logger.warn("Primary login button locator failed, trying fallback");
            clickElement(loginButtonBy);
        }
        
        // Wait for page transition
        waitForPageLoad();
        
        // Check if login was successful by looking for error message
        if (isLoginErrorDisplayed()) {
            logger.info("Login failed, staying on LoginPage");
            return (T) this;
        } else {
            logger.info("Login successful, navigating to DashboardPage");
            return (T) new DashboardPage(driver);
        }
    }
    
    /**
     * Performs complete login with username and password
     * 
     * @param username Username for login
     * @param password Password for login
     * @return DashboardPage instance after successful login
     */
    public DashboardPage login(String username, String password) {
        logger.info("Performing login with username: {}", username);
        return (DashboardPage) this.enterUsername(username)
                                  .enterPassword(password)
                                  .clickLoginButton();
    }
    
    /**
     * Performs admin login using configured credentials
     * 
     * @return DashboardPage instance after successful login
     */
    public DashboardPage loginAsAdmin() {
        String adminUsername = config.getAdminUsername();
        String adminPassword = config.getAdminPassword();
        logger.info("Performing admin login");
        return login(adminUsername, adminPassword);
    }
    
    /**
     * Clears username field
     * 
     * @return LoginPage instance for method chaining
     */
    public LoginPage clearUsername() {
        logger.debug("Clearing username field");
        try {
            if (usernameField.isDisplayed()) {
                usernameField.clear();
            } else {
                waitForElementToBeVisible(usernameFieldBy).clear();
            }
        } catch (Exception e) {
            waitForElementToBeVisible(usernameFieldBy).clear();
        }
        return this;
    }
    
    /**
     * Clears password field
     * 
     * @return LoginPage instance for method chaining
     */
    public LoginPage clearPassword() {
        logger.debug("Clearing password field");
        try {
            if (passwordField.isDisplayed()) {
                passwordField.clear();
            } else {
                waitForElementToBeVisible(passwordFieldBy).clear();
            }
        } catch (Exception e) {
            waitForElementToBeVisible(passwordFieldBy).clear();
        }
        return this;
    }
    
    /**
     * Gets error message text when login fails
     * 
     * @return Error message text or empty string if no error
     */
    public String getErrorMessage() {
        try {
            if (errorMessage.isDisplayed()) {
                String error = getElementText(errorMessage);
                logger.debug("Retrieved error message: {}", error);
                return error;
            } else {
                return getElementText(errorMessageBy);
            }
        } catch (Exception e) {
            logger.debug("No error message found");
            return "";
        }
    }
    
    /**
     * Checks if login error is displayed
     * 
     * @return true if error message is displayed
     */
    public boolean isLoginErrorDisplayed() {
        try {
            return errorMessage.isDisplayed() || isElementDisplayed(errorMessageBy);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Gets login page title text
     * 
     * @return Login page title
     */
    public String getLoginTitle() {
        try {
            if (loginTitle.isDisplayed()) {
                return getElementText(loginTitle);
            } else {
                return getElementText(loginTitleBy);
            }
        } catch (Exception e) {
            return getElementText(loginTitleBy);
        }
    }
    
    /**
     * Clicks forgot password link
     * 
     * @return LoginPage instance for method chaining
     */
    public LoginPage clickForgotPassword() {
        logger.debug("Clicking forgot password link");
        clickElement(forgotPasswordLink);
        return this;
    }
    
    /**
     * Checks if credentials section is displayed
     * 
     * @return true if credentials section is visible
     */
    public boolean isCredentialsSectionDisplayed() {
        try {
            return credentialsSection.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Waits for login page to load completely
     * 
     * @return LoginPage instance for method chaining
     */
    public LoginPage waitForLoginPageToLoad() {
        logger.debug("Waiting for login page to load");
        waitForElementToBeVisible(usernameFieldBy);
        waitForElementToBeVisible(passwordFieldBy);
        waitForElementToBeClickable(loginButtonBy);
        logger.info("Login page loaded successfully");
        return this;
    }
    
    /**
     * Verifies if login page is loaded by checking key elements
     * 
     * @return true if login page is loaded correctly
     */
    @Override
    public boolean isPageLoaded() {
        try {
            boolean usernameVisible = isElementDisplayed(usernameFieldBy);
            boolean passwordVisible = isElementDisplayed(passwordFieldBy);
            boolean loginButtonVisible = isElementDisplayed(loginButtonBy);
            
            boolean isLoaded = usernameVisible && passwordVisible && loginButtonVisible;
            logger.debug("Login page loaded status: {}", isLoaded);
            return isLoaded;
        } catch (Exception e) {
            logger.error("Error checking if login page is loaded", e);
            return false;
        }
    }
    
    /**
     * Gets current URL to verify we're on login page
     * 
     * @return true if current URL contains login page indicators
     */
    public boolean isOnLoginPage() {
        String currentUrl = getCurrentUrl();
        boolean onLoginPage = currentUrl.contains("orangehrmlive.com") && 
                             (currentUrl.endsWith("/") || currentUrl.contains("auth/login"));
        logger.debug("On login page status: {} (URL: {})", onLoginPage, currentUrl);
        return onLoginPage;
    }
    
    /**
     * Validates login form elements are enabled and interactive
     * 
     * @return true if all login form elements are functional
     */
    public boolean isLoginFormFunctional() {
        try {
            boolean usernameEnabled = waitForElementToBePresent(usernameFieldBy).isEnabled();
            boolean passwordEnabled = waitForElementToBePresent(passwordFieldBy).isEnabled();
            boolean loginButtonEnabled = waitForElementToBePresent(loginButtonBy).isEnabled();
            
            boolean isFunctional = usernameEnabled && passwordEnabled && loginButtonEnabled;
            logger.debug("Login form functional status: {}", isFunctional);
            return isFunctional;
        } catch (Exception e) {
            logger.error("Error checking login form functionality", e);
            return false;
        }
    }
}