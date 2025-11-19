package com.phoenix.hrm.bdd.stepdefinitions;

import com.phoenix.hrm.core.driver.WebDriverFactory;
import com.phoenix.hrm.core.reporting.TestReporter;
import com.phoenix.hrm.pages.LoginPage;
import io.cucumber.java.en.*;
import org.testng.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cucumber Step Definitions for Login functionality
 * Integrates BDD scenarios with Phoenix HRM framework components
 */
public class LoginSteps {
    
    private static final Logger logger = LoggerFactory.getLogger(LoginSteps.class);
    
    private LoginPage loginPage;
    private String errorMessage;
    
    @Given("I navigate to the Phoenix HRM login page")
    public void i_navigate_to_the_phoenix_hrm_login_page() {
        logger.info("BDD Step: Navigating to Phoenix HRM login page");
        TestReporter.logInfo("BDD Step: Navigating to login page");
        
        // Create WebDriver instance
        WebDriverFactory.createDriver();
        
        // Initialize login page
        loginPage = new LoginPage();
        loginPage.navigateToLoginPage();
        
        TestReporter.logPass("Successfully navigated to login page");
    }
    
    @Given("the login page is displayed")
    public void the_login_page_is_displayed() {
        logger.info("BDD Step: Verifying login page is displayed");
        TestReporter.logInfo("BDD Step: Verifying login page display");
        
        Assert.assertTrue(loginPage.isPageLoaded(), "Login page should be displayed");
        Assert.assertTrue(loginPage.validateLoginFormElements(), "Login form elements should be present");
        
        TestReporter.logPass("Login page is displayed correctly with all elements");
    }
    
    @When("I enter username {string}")
    public void i_enter_username(String username) {
        logger.info("BDD Step: Entering username: {}", username);
        TestReporter.logInfo("BDD Step: Entering username: " + username);
        
        loginPage.enterUsername(username);
        
        TestReporter.logPass("Username entered successfully");
    }
    
    @When("I enter password {string}")
    public void i_enter_password(String password) {
        logger.info("BDD Step: Entering password");
        TestReporter.logInfo("BDD Step: Entering password");
        
        loginPage.enterPassword(password);
        
        TestReporter.logPass("Password entered successfully");
    }
    
    @When("I click the login button")
    public void i_click_the_login_button() {
        logger.info("BDD Step: Clicking login button");
        TestReporter.logInfo("BDD Step: Clicking login button");
        
        loginPage.clickLoginButton();
        
        TestReporter.logPass("Login button clicked successfully");
    }
    
    @When("I login with admin credentials")
    public void i_login_with_admin_credentials() {
        logger.info("BDD Step: Logging in with admin credentials");
        TestReporter.logInfo("BDD Step: Logging in with admin credentials from test data");
        
        try {
            loginPage.loginAsAdmin();
            TestReporter.logPass("Admin login completed successfully");
        } catch (Exception e) {
            TestReporter.logFail("Admin login failed: " + e.getMessage());
            throw e;
        }
    }
    
    @When("I login with invalid credentials {string} and {string}")
    public void i_login_with_invalid_credentials(String username, String password) {
        logger.info("BDD Step: Attempting login with invalid credentials");
        TestReporter.logInfo("BDD Step: Attempting invalid login with username: " + username);
        
        loginPage.attemptInvalidLogin(username, password);
        
        TestReporter.logInfo("Invalid login attempt completed");
    }
    
    @When("I login with {string} role credentials")
    public void i_login_with_role_credentials(String role) {
        logger.info("BDD Step: Logging in with {} role credentials", role);
        TestReporter.logInfo("BDD Step: Logging in with role: " + role);
        
        try {
            loginPage.loginByRole(role);
            TestReporter.logPass("Login with " + role + " role completed successfully");
        } catch (Exception e) {
            TestReporter.logFail("Login with " + role + " role failed: " + e.getMessage());
            throw e;
        }
    }
    
    @Then("I should be successfully logged in")
    public void i_should_be_successfully_logged_in() {
        logger.info("BDD Step: Verifying successful login");
        TestReporter.logInfo("BDD Step: Verifying successful login");
        
        // Check if we are no longer on the login page (successful login)
        boolean stillOnLoginPage = loginPage.isOnLoginPage();
        Assert.assertFalse(stillOnLoginPage, "Should not be on login page after successful login");
        
        TestReporter.logPass("Login verification completed - User successfully logged in");
    }
    
    @Then("I should see the dashboard page")
    public void i_should_see_the_dashboard_page() {
        logger.info("BDD Step: Verifying dashboard page display");
        TestReporter.logInfo("BDD Step: Verifying dashboard page is displayed");
        
        // Wait for page load
        loginPage.waitForPageLoad();
        
        // Check URL contains dashboard or similar indication
        String currentUrl = loginPage.getCurrentUrl();
        boolean isDashboard = currentUrl.contains("dashboard") || currentUrl.contains("index");
        
        Assert.assertTrue(isDashboard, "Should be on dashboard page after login");
        
        TestReporter.logPass("Dashboard page is displayed correctly");
    }
    
    @Then("I should see an error message")
    public void i_should_see_an_error_message() {
        logger.info("BDD Step: Verifying error message display");
        TestReporter.logInfo("BDD Step: Verifying error message is displayed");
        
        Assert.assertTrue(loginPage.isErrorMessageDisplayed(), "Error message should be displayed");
        
        errorMessage = loginPage.getErrorMessage();
        Assert.assertFalse(errorMessage.isEmpty(), "Error message should not be empty");
        
        TestReporter.logPass("Error message displayed: " + errorMessage);
    }
    
    @Then("the error message should contain {string}")
    public void the_error_message_should_contain(String expectedText) {
        logger.info("BDD Step: Verifying error message contains expected text");
        TestReporter.logInfo("BDD Step: Verifying error message contains: " + expectedText);
        
        if (errorMessage == null) {
            errorMessage = loginPage.getErrorMessage();
        }
        
        Assert.assertTrue(errorMessage.toLowerCase().contains(expectedText.toLowerCase()),
            "Error message should contain: " + expectedText + ". Actual: " + errorMessage);
        
        TestReporter.logPass("Error message validation passed - Contains expected text");
    }
    
    @Then("I should remain on the login page")
    public void i_should_remain_on_the_login_page() {
        logger.info("BDD Step: Verifying still on login page");
        TestReporter.logInfo("BDD Step: Verifying user remains on login page");
        
        Assert.assertTrue(loginPage.isOnLoginPage(), "Should remain on login page after failed login");
        Assert.assertTrue(loginPage.isPageLoaded(), "Login page should still be properly loaded");
        
        TestReporter.logPass("User correctly remains on login page");
    }
    
    @When("I clear the username field")
    public void i_clear_the_username_field() {
        logger.info("BDD Step: Clearing username field");
        TestReporter.logInfo("BDD Step: Clearing username field");
        
        loginPage.clearUsername();
        
        TestReporter.logPass("Username field cleared successfully");
    }
    
    @When("I clear the password field")
    public void i_clear_the_password_field() {
        logger.info("BDD Step: Clearing password field");
        TestReporter.logInfo("BDD Step: Clearing password field");
        
        loginPage.clearPassword();
        
        TestReporter.logPass("Password field cleared successfully");
    }
    
    @Then("the login form should be displayed")
    public void the_login_form_should_be_displayed() {
        logger.info("BDD Step: Verifying login form display");
        TestReporter.logInfo("BDD Step: Verifying login form is displayed");
        
        Assert.assertTrue(loginPage.validateLoginFormElements(), "Login form should be displayed with all elements");
        Assert.assertTrue(loginPage.isLogoDisplayed(), "Company logo should be displayed");
        
        TestReporter.logPass("Login form is displayed correctly with all required elements");
    }
    
    @Then("the page title should contain {string}")
    public void the_page_title_should_contain(String expectedTitle) {
        logger.info("BDD Step: Verifying page title contains expected text");
        TestReporter.logInfo("BDD Step: Verifying page title contains: " + expectedTitle);
        
        String actualTitle = loginPage.getPageTitle();
        Assert.assertTrue(actualTitle.toLowerCase().contains(expectedTitle.toLowerCase()),
            "Page title should contain: " + expectedTitle + ". Actual: " + actualTitle);
        
        TestReporter.logPass("Page title validation passed: " + actualTitle);
    }
    
    @When("I take a screenshot")
    public void i_take_a_screenshot() {
        logger.info("BDD Step: Taking screenshot");
        TestReporter.logInfo("BDD Step: Taking screenshot for documentation");
        
        byte[] screenshot = loginPage.takeLoginPageScreenshot();
        TestReporter.saveAndAddScreenshot("BDD Step Screenshot", screenshot);
        
        TestReporter.logPass("Screenshot captured and added to report");
    }
    
    @Given("I wait for {int} seconds")
    public void i_wait_for_seconds(int seconds) {
        logger.info("BDD Step: Waiting for {} seconds", seconds);
        TestReporter.logInfo("BDD Step: Waiting for " + seconds + " seconds");
        
        loginPage.sleep(seconds * 1000L);
        
        TestReporter.logPass("Wait completed - " + seconds + " seconds");
    }
    
    @Then("I verify the login page URL is correct")
    public void i_verify_the_login_page_url_is_correct() {
        logger.info("BDD Step: Verifying login page URL");
        TestReporter.logInfo("BDD Step: Verifying login page URL is correct");
        
        Assert.assertTrue(loginPage.isOnLoginPage(), "Should be on the correct login page URL");
        
        String currentUrl = loginPage.getCurrentUrl();
        TestReporter.logInfo("Current URL: " + currentUrl);
        TestReporter.logPass("Login page URL verification passed");
    }
}
