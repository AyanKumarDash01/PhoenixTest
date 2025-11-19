package com.phoenix.hrm.tests.ui;

import com.phoenix.hrm.pages.DashboardPage;
import com.phoenix.hrm.pages.LoginPage;
import com.phoenix.hrm.tests.BaseTest;
import org.testng.annotations.Test;

/**
 * LoginTests class implementing comprehensive login functionality testing.
 * Covers positive and negative scenarios for OrangeHRM authentication.
 * 
 * Test Coverage:
 * - Admin login with valid credentials
 * - Invalid username scenarios
 * - Invalid password scenarios
 * - Empty credentials handling
 * - Login form validation
 * - Error message verification
 * - Session management
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 1.0
 */
public class LoginTests extends BaseTest {
    
    /**
     * Test admin login functionality (Project Requirement)
     * Verifies successful login with admin credentials from configuration
     */
    @Test(priority = 1, groups = {"smoke", "login", "ui"}, 
          description = "Verify admin login with valid credentials")
    public void testAdminLogin() {
        logger.info("=== Testing Admin Login ===");
        
        // Step 1: Navigate to login page
        LoginPage loginPage = getLoginPage();
        loginPage.navigateToLoginPage();
        
        // Step 2: Verify login page is loaded
        assertTrue(loginPage.isPageLoaded(), "Login page should be loaded");
        assertTrue(loginPage.isOnLoginPage(), "Should be on login page");
        
        // Step 3: Perform admin login
        DashboardPage dashboardPage = loginPage.loginAsAdmin();
        
        // Step 4: Verify successful login
        assertTrue(dashboardPage.isPageLoaded(), "Dashboard page should be loaded after login");
        assertTrue(dashboardPage.isOnDashboard(), "Should be on dashboard page");
        
        // Step 5: Verify user is logged in
        String loggedInUser = dashboardPage.getLoggedInUserName();
        assertTrue(loggedInUser != null && !loggedInUser.trim().isEmpty(), 
                  "Logged in user name should be displayed");
        
        logger.info("Admin login test completed successfully");
        logger.info("Logged in user: {}", loggedInUser);
    }
    
    /**
     * Test login with valid standard user credentials
     */
    @Test(priority = 2, groups = {"smoke", "login", "ui"}, 
          description = "Verify login with valid standard user credentials")
    public void testValidUserLogin() {
        logger.info("=== Testing Valid User Login ===");
        
        LoginPage loginPage = getLoginPage();
        loginPage.navigateToLoginPage();
        
        // Use admin credentials (in real scenario, would use different user types)
        String username = config.getAdminUsername();
        String password = config.getAdminPassword();
        
        // Perform login
        DashboardPage dashboardPage = loginPage.login(username, password);
        
        // Verify successful login
        assertTrue(dashboardPage.isPageLoaded(), "Dashboard should be loaded after valid login");
        
        String pageTitle = dashboardPage.getPageTitle();
        logger.info("Dashboard page title: {}", pageTitle);
        
        // Verify user can access PIM module (basic permission check)
        assertTrue(dashboardPage.isModuleVisible("PIM"), "PIM module should be visible for valid user");
        
        logger.info("Valid user login test completed successfully");
    }
    
    /**
     * Test login with invalid username
     */
    @Test(priority = 3, groups = {"negative", "login", "ui"}, 
          description = "Verify login fails with invalid username")
    public void testInvalidUsernameLogin() {
        logger.info("=== Testing Invalid Username Login ===");
        
        LoginPage loginPage = getLoginPage();
        loginPage.navigateToLoginPage();
        
        // Attempt login with invalid username
        String invalidUsername = "invaliduser123";
        String validPassword = config.getAdminPassword();
        
        loginPage.enterUsername(invalidUsername)
                .enterPassword(validPassword)
                .clickLoginButton();
        
        // Wait a moment for error message to appear
        waitForSeconds(2);
        
        // Verify login failed (should remain on login page)
        assertTrue(loginPage.isOnLoginPage(), "Should remain on login page after invalid login");
        assertTrue(loginPage.isLoginErrorDisplayed(), "Error message should be displayed");
        
        String errorMessage = loginPage.getErrorMessage();
        assertTrue(errorMessage != null && !errorMessage.trim().isEmpty(), 
                  "Error message should not be empty");
        
        logger.info("Invalid username login test completed successfully");
        logger.info("Error message displayed: {}", errorMessage);
    }
    
    /**
     * Test login with invalid password
     */
    @Test(priority = 4, groups = {"negative", "login", "ui"}, 
          description = "Verify login fails with invalid password")
    public void testInvalidPasswordLogin() {
        logger.info("=== Testing Invalid Password Login ===");
        
        LoginPage loginPage = getLoginPage();
        loginPage.navigateToLoginPage();
        
        // Attempt login with invalid password
        String validUsername = config.getAdminUsername();
        String invalidPassword = "wrongpassword123";
        
        loginPage.enterUsername(validUsername)
                .enterPassword(invalidPassword)
                .clickLoginButton();
        
        // Wait for error message
        waitForSeconds(2);
        
        // Verify login failed
        assertTrue(loginPage.isOnLoginPage(), "Should remain on login page after invalid password");
        assertTrue(loginPage.isLoginErrorDisplayed(), "Error message should be displayed for invalid password");
        
        String errorMessage = loginPage.getErrorMessage();
        assertTrue(errorMessage != null && !errorMessage.trim().isEmpty(), 
                  "Error message should not be empty for invalid password");
        
        logger.info("Invalid password login test completed successfully");
        logger.info("Error message displayed: {}", errorMessage);
    }
    
    /**
     * Test login with empty credentials
     */
    @Test(priority = 5, groups = {"negative", "login", "ui"}, 
          description = "Verify login fails with empty credentials")
    public void testEmptyCredentialsLogin() {
        logger.info("=== Testing Empty Credentials Login ===");
        
        LoginPage loginPage = getLoginPage();
        loginPage.navigateToLoginPage();
        
        // Attempt login with empty credentials
        loginPage.clearUsername()
                .clearPassword()
                .clickLoginButton();
        
        // Wait for validation
        waitForSeconds(1);
        
        // Verify still on login page
        assertTrue(loginPage.isOnLoginPage(), "Should remain on login page with empty credentials");
        
        // Login form should still be functional
        assertTrue(loginPage.isLoginFormFunctional(), "Login form should remain functional");
        
        logger.info("Empty credentials login test completed successfully");
    }
    
    /**
     * Test login with empty username
     */
    @Test(priority = 6, groups = {"negative", "login", "ui"}, 
          description = "Verify login fails with empty username")
    public void testEmptyUsernameLogin() {
        logger.info("=== Testing Empty Username Login ===");
        
        LoginPage loginPage = getLoginPage();
        loginPage.navigateToLoginPage();
        
        // Attempt login with empty username
        String validPassword = config.getAdminPassword();
        
        loginPage.clearUsername()
                .enterPassword(validPassword)
                .clickLoginButton();
        
        // Wait for validation
        waitForSeconds(1);
        
        // Verify still on login page
        assertTrue(loginPage.isOnLoginPage(), "Should remain on login page with empty username");
        
        logger.info("Empty username login test completed successfully");
    }
    
    /**
     * Test login with empty password
     */
    @Test(priority = 7, groups = {"negative", "login", "ui"}, 
          description = "Verify login fails with empty password")
    public void testEmptyPasswordLogin() {
        logger.info("=== Testing Empty Password Login ===");
        
        LoginPage loginPage = getLoginPage();
        loginPage.navigateToLoginPage();
        
        // Attempt login with empty password
        String validUsername = config.getAdminUsername();
        
        loginPage.enterUsername(validUsername)
                .clearPassword()
                .clickLoginButton();
        
        // Wait for validation
        waitForSeconds(1);
        
        // Verify still on login page
        assertTrue(loginPage.isOnLoginPage(), "Should remain on login page with empty password");
        
        logger.info("Empty password login test completed successfully");
    }
    
    /**
     * Test login page elements and functionality
     */
    @Test(priority = 8, groups = {"ui", "login"}, 
          description = "Verify login page elements and functionality")
    public void testLoginPageElements() {
        logger.info("=== Testing Login Page Elements ===");
        
        LoginPage loginPage = getLoginPage();
        loginPage.navigateToLoginPage();
        
        // Verify page elements are present
        assertTrue(loginPage.isPageLoaded(), "Login page should be loaded");
        assertTrue(loginPage.isLoginFormFunctional(), "Login form should be functional");
        
        // Verify login title
        String loginTitle = loginPage.getLoginTitle();
        assertTrue(loginTitle != null && !loginTitle.trim().isEmpty(), 
                  "Login title should be displayed");
        
        logger.info("Login page title: {}", loginTitle);
        
        // Test clearing functionality
        loginPage.enterUsername("test")
                .enterPassword("test")
                .clearUsername()
                .clearPassword();
        
        logger.info("Login page elements test completed successfully");
    }
    
    /**
     * Test session management and logout
     */
    @Test(priority = 9, groups = {"smoke", "login", "ui"}, 
          description = "Verify session management and logout functionality")
    public void testSessionManagement() {
        logger.info("=== Testing Session Management ===");
        
        // Step 1: Login
        DashboardPage dashboardPage = loginAsAdmin();
        assertTrue(dashboardPage.isPageLoaded(), "Should be logged in");
        
        String loggedInUser = dashboardPage.getLoggedInUserName();
        logger.info("Logged in as: {}", loggedInUser);
        
        // Step 2: Logout
        LoginPage loginPage = dashboardPage.logout();
        
        // Step 3: Verify logout successful
        assertTrue(loginPage.isOnLoginPage(), "Should be redirected to login page after logout");
        assertTrue(loginPage.isPageLoaded(), "Login page should be loaded after logout");
        
        logger.info("Session management test completed successfully");
    }
    
    /**
     * Test login performance (response time validation)
     */
    @Test(priority = 10, groups = {"performance", "login", "ui"}, 
          description = "Verify login response time is within acceptable limits")
    public void testLoginPerformance() {
        logger.info("=== Testing Login Performance ===");
        
        LoginPage loginPage = getLoginPage();
        loginPage.navigateToLoginPage();
        
        // Measure login performance
        long startTime = System.currentTimeMillis();
        
        DashboardPage dashboardPage = loginPage.loginAsAdmin();
        
        long endTime = System.currentTimeMillis();
        long loginDuration = endTime - startTime;
        
        // Verify login success
        assertTrue(dashboardPage.isPageLoaded(), "Login should be successful");
        
        // Verify performance (should complete within 10 seconds)
        assertTrue(loginDuration < 10000, 
                  String.format("Login should complete within 10 seconds. Actual: %d ms", loginDuration));
        
        logger.info("Login completed in {} ms", loginDuration);
        logger.info("Login performance test completed successfully");
    }
}