package com.phoenix.hrm.pages;

import com.phoenix.hrm.core.base.BasePage;
import com.phoenix.hrm.pages.pim.EmployeeListPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

/**
 * DashboardPage represents the main OrangeHRM dashboard after successful login.
 * Provides navigation to different modules and user account management.
 * 
 * Features:
 * - Navigation to different HRM modules (PIM, Admin, Leave, etc.)
 * - User profile management
 * - Dashboard widgets and information display
 * - Logout functionality
 * - Module access verification based on user roles
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 1.0
 */
public class DashboardPage extends BasePage {
    
    // Page Elements using PageFactory
    @FindBy(how = How.XPATH, using = "//h6[@class='oxd-text oxd-text--h6 oxd-topbar-header-breadcrumb-module']")
    private WebElement pageTitle;
    
    @FindBy(how = How.XPATH, using = "//p[@class='oxd-userdropdown-name']")
    private WebElement userDropdown;
    
    @FindBy(how = How.XPATH, using = "//a[text()='Logout']")
    private WebElement logoutButton;
    
    @FindBy(how = How.XPATH, using = "//span[text()='PIM']")
    private WebElement pimMenu;
    
    @FindBy(how = How.XPATH, using = "//span[text()='Admin']")
    private WebElement adminMenu;
    
    @FindBy(how = How.XPATH, using = "//span[text()='Leave']")
    private WebElement leaveMenu;
    
    @FindBy(how = How.XPATH, using = "//span[text()='Time']")
    private WebElement timeMenu;
    
    @FindBy(how = How.XPATH, using = "//span[text()='Recruitment']")
    private WebElement recruitmentMenu;
    
    @FindBy(how = How.XPATH, using = "//span[text()='My Info']")
    private WebElement myInfoMenu;
    
    @FindBy(how = How.XPATH, using = "//span[text()='Performance']")
    private WebElement performanceMenu;
    
    @FindBy(how = How.XPATH, using = "//span[text()='Dashboard']")
    private WebElement dashboardMenu;
    
    @FindBy(how = How.XPATH, using = "//span[text()='Directory']")
    private WebElement directoryMenu;
    
    @FindBy(how = How.XPATH, using = "//span[text()='Maintenance']")
    private WebElement maintenanceMenu;
    
    @FindBy(how = How.XPATH, using = "//span[text()='Claim']")
    private WebElement claimMenu;
    
    @FindBy(how = How.XPATH, using = "//span[text()='Buzz']")
    private WebElement buzzMenu;
    
    // Alternative locators for robustness
    private final By pageTitleBy = By.xpath("//h6[contains(@class,'oxd-topbar-header-breadcrumb-module')]");
    private final By userDropdownBy = By.xpath("//p[@class='oxd-userdropdown-name']");
    private final By logoutButtonBy = By.xpath("//a[text()='Logout']");
    private final By pimMenuBy = By.xpath("//span[text()='PIM']");
    private final By adminMenuBy = By.xpath("//span[text()='Admin']");
    private final By dashboardMenuBy = By.xpath("//span[text()='Dashboard']");
    
    /**
     * Constructor initializes DashboardPage with WebDriver
     * 
     * @param driver WebDriver instance
     */
    public DashboardPage(WebDriver driver) {
        super(driver);
        logger.info("Initialized DashboardPage");
        waitForDashboardToLoad();
    }
    
    /**
     * Waits for dashboard to load completely
     * 
     * @return DashboardPage instance for method chaining
     */
    public DashboardPage waitForDashboardToLoad() {
        logger.debug("Waiting for dashboard to load");
        waitForElementToBeVisible(pageTitleBy);
        waitForElementToBeVisible(userDropdownBy);
        logger.info("Dashboard loaded successfully");
        return this;
    }
    
    /**
     * Gets the current page title from the breadcrumb
     * 
     * @return Current page title
     */
    public String getPageTitle() {
        try {
            String title = getElementText(pageTitleBy);
            logger.debug("Current page title: {}", title);
            return title;
        } catch (Exception e) {
            logger.error("Failed to get page title", e);
            return "";
        }
    }
    
    /**
     * Gets the logged-in user name from dropdown
     * 
     * @return Username of logged-in user
     */
    public String getLoggedInUserName() {
        try {
            String username = getElementText(userDropdownBy);
            logger.debug("Logged-in user: {}", username);
            return username;
        } catch (Exception e) {
            logger.error("Failed to get logged-in user name", e);
            return "";
        }
    }
    
    /**
     * Clicks on user dropdown to reveal options
     * 
     * @return DashboardPage instance for method chaining
     */
    public DashboardPage clickUserDropdown() {
        logger.debug("Clicking user dropdown");
        clickElement(userDropdownBy);
        return this;
    }
    
    /**
     * Performs logout operation
     * 
     * @return LoginPage instance after logout
     */
    public LoginPage logout() {
        logger.info("Performing logout");
        clickUserDropdown();
        waitForElementToBeClickable(logoutButtonBy);
        clickElement(logoutButtonBy);
        waitForPageLoad();
        logger.info("Logout completed, navigating to LoginPage");
        return new LoginPage(driver);
    }
    
    /**
     * Navigates to PIM module
     * 
     * @return EmployeeListPage instance
     */
    public EmployeeListPage navigateToPIM() {
        logger.info("Navigating to PIM module");
        try {
            if (pimMenu.isDisplayed()) {
                clickElement(pimMenu);
            } else {
                clickElement(pimMenuBy);
            }
        } catch (Exception e) {
            logger.warn("Primary PIM menu locator failed, trying fallback");
            clickElement(pimMenuBy);
        }
        waitForPageLoad();
        logger.info("Navigated to PIM module");
        return new EmployeeListPage(driver);
    }
    
    /**
     * Navigates to Admin module
     * 
     * @return DashboardPage instance (Admin page not implemented yet)
     */
    public DashboardPage navigateToAdmin() {
        logger.info("Navigating to Admin module");
        try {
            if (adminMenu.isDisplayed()) {
                clickElement(adminMenu);
            } else {
                clickElement(adminMenuBy);
            }
        } catch (Exception e) {
            logger.warn("Primary Admin menu locator failed, trying fallback");
            clickElement(adminMenuBy);
        }
        waitForPageLoad();
        logger.info("Navigated to Admin module");
        return this; // Return this for now, can be changed to AdminPage when implemented
    }
    
    /**
     * Navigates to Leave module
     * 
     * @return DashboardPage instance (Leave page not implemented yet)
     */
    public DashboardPage navigateToLeave() {
        logger.info("Navigating to Leave module");
        clickElement(leaveMenu);
        waitForPageLoad();
        logger.info("Navigated to Leave module");
        return this;
    }
    
    /**
     * Navigates to Time module
     * 
     * @return DashboardPage instance (Time page not implemented yet)
     */
    public DashboardPage navigateToTime() {
        logger.info("Navigating to Time module");
        clickElement(timeMenu);
        waitForPageLoad();
        logger.info("Navigated to Time module");
        return this;
    }
    
    /**
     * Navigates to Recruitment module
     * 
     * @return DashboardPage instance (Recruitment page not implemented yet)
     */
    public DashboardPage navigateToRecruitment() {
        logger.info("Navigating to Recruitment module");
        clickElement(recruitmentMenu);
        waitForPageLoad();
        logger.info("Navigated to Recruitment module");
        return this;
    }
    
    /**
     * Navigates to My Info module
     * 
     * @return DashboardPage instance (MyInfo page not implemented yet)
     */
    public DashboardPage navigateToMyInfo() {
        logger.info("Navigating to My Info module");
        clickElement(myInfoMenu);
        waitForPageLoad();
        logger.info("Navigated to My Info module");
        return this;
    }
    
    /**
     * Navigates to Performance module
     * 
     * @return DashboardPage instance (Performance page not implemented yet)
     */
    public DashboardPage navigateToPerformance() {
        logger.info("Navigating to Performance module");
        clickElement(performanceMenu);
        waitForPageLoad();
        logger.info("Navigated to Performance module");
        return this;
    }
    
    /**
     * Navigates to Dashboard (home) module
     * 
     * @return DashboardPage instance
     */
    public DashboardPage navigateToDashboard() {
        logger.info("Navigating to Dashboard");
        try {
            if (dashboardMenu.isDisplayed()) {
                clickElement(dashboardMenu);
            } else {
                clickElement(dashboardMenuBy);
            }
        } catch (Exception e) {
            logger.warn("Primary Dashboard menu locator failed, trying fallback");
            clickElement(dashboardMenuBy);
        }
        waitForPageLoad();
        logger.info("Navigated to Dashboard");
        return this;
    }
    
    /**
     * Navigates to Directory module
     * 
     * @return DashboardPage instance (Directory page not implemented yet)
     */
    public DashboardPage navigateToDirectory() {
        logger.info("Navigating to Directory module");
        clickElement(directoryMenu);
        waitForPageLoad();
        logger.info("Navigated to Directory module");
        return this;
    }
    
    /**
     * Navigates to Maintenance module
     * 
     * @return DashboardPage instance (Maintenance page not implemented yet)
     */
    public DashboardPage navigateToMaintenance() {
        logger.info("Navigating to Maintenance module");
        clickElement(maintenanceMenu);
        waitForPageLoad();
        logger.info("Navigated to Maintenance module");
        return this;
    }
    
    /**
     * Navigates to Claim module
     * 
     * @return DashboardPage instance (Claim page not implemented yet)
     */
    public DashboardPage navigateToClaim() {
        logger.info("Navigating to Claim module");
        clickElement(claimMenu);
        waitForPageLoad();
        logger.info("Navigated to Claim module");
        return this;
    }
    
    /**
     * Navigates to Buzz module
     * 
     * @return DashboardPage instance (Buzz page not implemented yet)
     */
    public DashboardPage navigateToBuzz() {
        logger.info("Navigating to Buzz module");
        clickElement(buzzMenu);
        waitForPageLoad();
        logger.info("Navigated to Buzz module");
        return this;
    }
    
    /**
     * Checks if specific module is visible/accessible
     * 
     * @param moduleName Name of the module to check
     * @return true if module is visible
     */
    public boolean isModuleVisible(String moduleName) {
        try {
            By moduleLocator = By.xpath("//span[text()='" + moduleName + "']");
            boolean isVisible = isElementDisplayed(moduleLocator);
            logger.debug("Module '{}' visibility status: {}", moduleName, isVisible);
            return isVisible;
        } catch (Exception e) {
            logger.error("Error checking module visibility for: {}", moduleName, e);
            return false;
        }
    }
    
    /**
     * Gets list of all visible modules for current user
     * 
     * @return Array of visible module names
     */
    public String[] getVisibleModules() {
        String[] modules = {"Admin", "PIM", "Leave", "Time", "Recruitment", "My Info", 
                           "Performance", "Dashboard", "Directory", "Maintenance", "Claim", "Buzz"};
        return java.util.Arrays.stream(modules)
                .filter(this::isModuleVisible)
                .toArray(String[]::new);
    }
    
    /**
     * Verifies if dashboard page is loaded by checking key elements
     * 
     * @return true if dashboard page is loaded correctly
     */
    @Override
    public boolean isPageLoaded() {
        try {
            boolean titleVisible = isElementDisplayed(pageTitleBy);
            boolean userDropdownVisible = isElementDisplayed(userDropdownBy);
            boolean dashboardMenuVisible = isElementDisplayed(dashboardMenuBy);
            
            boolean isLoaded = titleVisible && userDropdownVisible && dashboardMenuVisible;
            logger.debug("Dashboard page loaded status: {}", isLoaded);
            return isLoaded;
        } catch (Exception e) {
            logger.error("Error checking if dashboard page is loaded", e);
            return false;
        }
    }
    
    /**
     * Gets current URL to verify we're on dashboard
     * 
     * @return true if current URL indicates dashboard page
     */
    public boolean isOnDashboard() {
        String currentUrl = getCurrentUrl();
        boolean onDashboard = currentUrl.contains("dashboard") || 
                             (currentUrl.contains("orangehrmlive.com") && !currentUrl.contains("auth"));
        logger.debug("On dashboard status: {} (URL: {})", onDashboard, currentUrl);
        return onDashboard;
    }
    
    /**
     * Refreshes the dashboard page
     * 
     * @return DashboardPage instance for method chaining
     */
    public DashboardPage refreshDashboard() {
        logger.info("Refreshing dashboard page");
        refreshPage();
        waitForDashboardToLoad();
        return this;
    }
}