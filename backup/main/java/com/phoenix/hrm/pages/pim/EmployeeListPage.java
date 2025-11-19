package com.phoenix.hrm.pages.pim;

import com.phoenix.hrm.core.base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

import java.util.List;

/**
 * EmployeeListPage represents the PIM Employee List page in OrangeHRM.
 * Provides functionality for searching, viewing, and managing employee records.
 * 
 * Features:
 * - Employee search by ID, name, and other criteria
 * - Employee list display with pagination
 * - Navigation to Add Employee page
 * - Employee record editing and deletion
 * - Bulk operations on employee records
 * - Advanced filtering and sorting
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 1.0
 */
public class EmployeeListPage extends BasePage {
    
    // Page Elements using PageFactory
    @FindBy(how = How.XPATH, using = "//h6[text()='PIM']")
    private WebElement pageTitle;
    
    @FindBy(how = How.XPATH, using = "//button[normalize-space()='Add']")
    private WebElement addButton;
    
    @FindBy(how = How.XPATH, using = "//input[@placeholder='Type for hints...'][1]")
    private WebElement employeeNameSearch;
    
    @FindBy(how = How.XPATH, using = "(//input[@class='oxd-input oxd-input--active'])[2]")
    private WebElement employeeIdSearch;
    
    @FindBy(how = How.XPATH, using = "//button[normalize-space()='Search']")
    private WebElement searchButton;
    
    @FindBy(how = How.XPATH, using = "//button[normalize-space()='Reset']")
    private WebElement resetButton;
    
    @FindBy(how = How.XPATH, using = "//div[@class='oxd-table-body']//div[@class='oxd-table-row oxd-table-row--with-border']")
    private List<WebElement> employeeRows;
    
    @FindBy(how = How.XPATH, using = "//span[text()='No Records Found']")
    private WebElement noRecordsFoundMessage;
    
    @FindBy(how = How.XPATH, using = "//span[text()='Records Found']")
    private WebElement recordsFoundMessage;
    
    @FindBy(how = How.XPATH, using = "//div[@class='oxd-table-header']//div[@class='oxd-table-row']")
    private WebElement tableHeader;
    
    @FindBy(how = How.XPATH, using = "//button[@title='Previous']")
    private WebElement previousPageButton;
    
    @FindBy(how = How.XPATH, using = "//button[@title='Next']")
    private WebElement nextPageButton;
    
    @FindBy(how = How.XPATH, using = "//span[@class='oxd-text oxd-text--span']")
    private WebElement recordCountText;
    
    // Alternative locators for robustness
    private final By pageTitleBy = By.xpath("//h6[text()='PIM']");
    private final By addButtonBy = By.xpath("//button[normalize-space()='Add']");
    private final By employeeNameSearchBy = By.xpath("//input[@placeholder='Type for hints...'][1]");
    private final By employeeIdSearchBy = By.xpath("(//input[@class='oxd-input oxd-input--active'])[2]");
    private final By searchButtonBy = By.xpath("//button[normalize-space()='Search']");
    private final By resetButtonBy = By.xpath("//button[normalize-space()='Reset']");
    private final By employeeRowsBy = By.xpath("//div[@class='oxd-table-body']//div[@class='oxd-table-row oxd-table-row--with-border']");
    private final By noRecordsFoundBy = By.xpath("//span[text()='No Records Found']");
    private final By recordsFoundBy = By.xpath("//span[text()='Records Found']");
    
    /**
     * Constructor initializes EmployeeListPage with WebDriver
     * 
     * @param driver WebDriver instance
     */
    public EmployeeListPage(WebDriver driver) {
        super(driver);
        logger.info("Initialized EmployeeListPage");
        waitForEmployeeListPageToLoad();
    }
    
    /**
     * Waits for employee list page to load completely
     * 
     * @return EmployeeListPage instance for method chaining
     */
    public EmployeeListPage waitForEmployeeListPageToLoad() {
        logger.debug("Waiting for employee list page to load");
        waitForElementToBeVisible(pageTitleBy);
        waitForElementToBeClickable(addButtonBy);
        logger.info("Employee list page loaded successfully");
        return this;
    }
    
    /**
     * Clicks the Add button to navigate to Add Employee page
     * 
     * @return AddEmployeePage instance
     */
    public AddEmployeePage clickAddEmployee() {
        logger.info("Clicking Add Employee button");
        try {
            if (addButton.isDisplayed()) {
                clickElement(addButton);
            } else {
                clickElement(addButtonBy);
            }
        } catch (Exception e) {
            logger.warn("Primary add button locator failed, trying fallback");
            clickElement(addButtonBy);
        }
        waitForPageLoad();
        logger.info("Navigated to Add Employee page");
        return new AddEmployeePage(driver);
    }
    
    /**
     * Searches for employee by name
     * 
     * @param employeeName Name of employee to search for
     * @return EmployeeListPage instance for method chaining
     */
    public EmployeeListPage searchByEmployeeName(String employeeName) {
        logger.info("Searching for employee by name: {}", employeeName);
        try {
            if (employeeNameSearch.isDisplayed()) {
                sendKeysToElement(employeeNameSearch, employeeName);
            } else {
                sendKeysToElement(employeeNameSearchBy, employeeName);
            }
        } catch (Exception e) {
            logger.warn("Primary employee name search locator failed, trying fallback");
            sendKeysToElement(employeeNameSearchBy, employeeName);
        }
        
        // Wait for autocomplete suggestions and select first one
        try {
            Thread.sleep(2000); // Wait for autocomplete
            // Click on first suggestion
            By firstSuggestion = By.xpath("//div[@role='option'][1]");
            if (isElementDisplayed(firstSuggestion)) {
                clickElement(firstSuggestion);
            }
        } catch (Exception e) {
            logger.debug("No autocomplete suggestions found or error selecting suggestion");
        }
        
        return this;
    }
    
    /**
     * Searches for employee by ID
     * 
     * @param employeeId ID of employee to search for
     * @return EmployeeListPage instance for method chaining
     */
    public EmployeeListPage searchByEmployeeId(String employeeId) {
        logger.info("Searching for employee by ID: {}", employeeId);
        try {
            if (employeeIdSearch.isDisplayed()) {
                sendKeysToElement(employeeIdSearch, employeeId);
            } else {
                sendKeysToElement(employeeIdSearchBy, employeeId);
            }
        } catch (Exception e) {
            logger.warn("Primary employee ID search locator failed, trying fallback");
            sendKeysToElement(employeeIdSearchBy, employeeId);
        }
        return this;
    }
    
    /**
     * Clicks the Search button to perform search
     * 
     * @return EmployeeListPage instance for method chaining
     */
    public EmployeeListPage clickSearch() {
        logger.debug("Clicking Search button");
        try {
            if (searchButton.isDisplayed()) {
                clickElement(searchButton);
            } else {
                clickElement(searchButtonBy);
            }
        } catch (Exception e) {
            logger.warn("Primary search button locator failed, trying fallback");
            clickElement(searchButtonBy);
        }
        waitForPageLoad();
        return this;
    }
    
    /**
     * Clicks the Reset button to clear search criteria
     * 
     * @return EmployeeListPage instance for method chaining
     */
    public EmployeeListPage clickReset() {
        logger.debug("Clicking Reset button");
        try {
            if (resetButton.isDisplayed()) {
                clickElement(resetButton);
            } else {
                clickElement(resetButtonBy);
            }
        } catch (Exception e) {
            logger.warn("Primary reset button locator failed, trying fallback");
            clickElement(resetButtonBy);
        }
        waitForPageLoad();
        return this;
    }
    
    /**
     * Performs complete search by employee name
     * 
     * @param employeeName Name of employee to search for
     * @return EmployeeListPage instance for method chaining
     */
    public EmployeeListPage searchEmployee(String employeeName) {
        logger.info("Performing complete search for employee: {}", employeeName);
        return this.searchByEmployeeName(employeeName)
                  .clickSearch();
    }
    
    /**
     * Performs complete search by employee ID
     * 
     * @param employeeId ID of employee to search for
     * @return EmployeeListPage instance for method chaining
     */
    public EmployeeListPage searchEmployeeById(String employeeId) {
        logger.info("Performing complete search for employee ID: {}", employeeId);
        return this.searchByEmployeeId(employeeId)
                  .clickSearch();
    }
    
    /**
     * Gets the number of employee rows displayed in the table
     * 
     * @return Number of employee rows
     */
    public int getEmployeeRowCount() {
        try {
            List<WebElement> rows = driver.findElements(employeeRowsBy);
            int count = rows.size();
            logger.debug("Found {} employee rows", count);
            return count;
        } catch (Exception e) {
            logger.error("Error getting employee row count", e);
            return 0;
        }
    }
    
    /**
     * Checks if "No Records Found" message is displayed
     * 
     * @return true if no records found message is visible
     */
    public boolean isNoRecordsFoundDisplayed() {
        try {
            boolean isDisplayed = isElementDisplayed(noRecordsFoundBy);
            logger.debug("No records found message displayed: {}", isDisplayed);
            return isDisplayed;
        } catch (Exception e) {
            logger.debug("Error checking no records found message", e);
            return false;
        }
    }
    
    /**
     * Checks if records are found (table has data)
     * 
     * @return true if employee records are displayed
     */
    public boolean areRecordsFound() {
        int rowCount = getEmployeeRowCount();
        boolean recordsFound = rowCount > 0 && !isNoRecordsFoundDisplayed();
        logger.debug("Records found status: {} (Row count: {})", recordsFound, rowCount);
        return recordsFound;
    }
    
    /**
     * Gets employee information from specific row
     * 
     * @param rowIndex Index of the row (0-based)
     * @return EmployeeInfo object with employee details
     */
    public EmployeeInfo getEmployeeInfoFromRow(int rowIndex) {
        try {
            List<WebElement> rows = driver.findElements(employeeRowsBy);
            if (rowIndex >= 0 && rowIndex < rows.size()) {
                WebElement row = rows.get(rowIndex);
                
                // Extract employee information from row cells
                List<WebElement> cells = row.findElements(By.xpath(".//div[@class='oxd-table-cell']"));
                
                EmployeeInfo employeeInfo = new EmployeeInfo();
                if (cells.size() >= 4) {
                    employeeInfo.setEmployeeId(cells.get(1).getText());
                    employeeInfo.setFirstName(cells.get(2).getText().split(" ")[0]);
                    if (cells.get(2).getText().split(" ").length > 1) {
                        employeeInfo.setLastName(cells.get(2).getText().split(" ")[1]);
                    }
                    employeeInfo.setJobTitle(cells.get(3).getText());
                }
                
                logger.debug("Retrieved employee info from row {}: {}", rowIndex, employeeInfo);
                return employeeInfo;
            } else {
                logger.warn("Invalid row index: {} (Total rows: {})", rowIndex, rows.size());
                return null;
            }
        } catch (Exception e) {
            logger.error("Error getting employee info from row {}", rowIndex, e);
            return null;
        }
    }
    
    /**
     * Clicks on employee row to view/edit employee details
     * 
     * @param rowIndex Index of the row to click (0-based)
     * @return EmployeeDetailsPage instance
     */
    public EmployeeDetailsPage clickEmployeeRow(int rowIndex) {
        try {
            List<WebElement> rows = driver.findElements(employeeRowsBy);
            if (rowIndex >= 0 && rowIndex < rows.size()) {
                WebElement row = rows.get(rowIndex);
                logger.info("Clicking on employee row at index: {}", rowIndex);
                clickElement(row);
                waitForPageLoad();
                return new EmployeeDetailsPage(driver);
            } else {
                logger.error("Invalid row index: {} (Total rows: {})", rowIndex, rows.size());
                return null;
            }
        } catch (Exception e) {
            logger.error("Error clicking employee row at index {}", rowIndex, e);
            return null;
        }
    }
    
    /**
     * Searches for specific employee and clicks on first result
     * 
     * @param employeeName Name of employee to find and select
     * @return EmployeeDetailsPage if employee found, null otherwise
     */
    public EmployeeDetailsPage searchAndSelectEmployee(String employeeName) {
        logger.info("Searching and selecting employee: {}", employeeName);
        searchEmployee(employeeName);
        
        if (areRecordsFound()) {
            return clickEmployeeRow(0); // Click first result
        } else {
            logger.warn("Employee not found: {}", employeeName);
            return null;
        }
    }
    
    /**
     * Deletes employee from specific row
     * 
     * @param rowIndex Index of the row to delete (0-based)
     * @return EmployeeListPage instance for method chaining
     */
    public EmployeeListPage deleteEmployeeFromRow(int rowIndex) {
        try {
            List<WebElement> rows = driver.findElements(employeeRowsBy);
            if (rowIndex >= 0 && rowIndex < rows.size()) {
                WebElement row = rows.get(rowIndex);
                
                // Find delete button in the row
                WebElement deleteButton = row.findElement(By.xpath(".//button[@title='Delete']"));
                logger.info("Deleting employee from row: {}", rowIndex);
                clickElement(deleteButton);
                
                // Confirm deletion in popup
                By confirmDeleteBy = By.xpath("//button[normalize-space()='Yes, Delete']");
                waitForElementToBeClickable(confirmDeleteBy);
                clickElement(confirmDeleteBy);
                waitForPageLoad();
                
                logger.info("Employee deleted successfully");
            } else {
                logger.error("Invalid row index for deletion: {} (Total rows: {})", rowIndex, rows.size());
            }
        } catch (Exception e) {
            logger.error("Error deleting employee from row {}", rowIndex, e);
        }
        return this;
    }
    
    /**
     * Validates that employee data appears correctly in search results
     * 
     * @param expectedEmployeeId Expected employee ID
     * @param expectedEmployeeName Expected employee name
     * @return true if employee data is correct
     */
    public boolean validateEmployeeInResults(String expectedEmployeeId, String expectedEmployeeName) {
        logger.info("Validating employee in results - ID: {}, Name: {}", expectedEmployeeId, expectedEmployeeName);
        
        if (!areRecordsFound()) {
            logger.warn("No records found to validate");
            return false;
        }
        
        EmployeeInfo firstEmployee = getEmployeeInfoFromRow(0);
        if (firstEmployee != null) {
            boolean idMatches = expectedEmployeeId.equals(firstEmployee.getEmployeeId());
            boolean nameMatches = expectedEmployeeName.contains(firstEmployee.getFirstName()) || 
                                 expectedEmployeeName.contains(firstEmployee.getLastName());
            
            boolean isValid = idMatches || nameMatches;
            logger.debug("Employee validation result: {} (ID match: {}, Name match: {})", 
                        isValid, idMatches, nameMatches);
            return isValid;
        }
        
        return false;
    }
    
    /**
     * Gets the page title text
     * 
     * @return Page title
     */
    public String getPageTitle() {
        try {
            String title = getElementText(pageTitleBy);
            logger.debug("Page title: {}", title);
            return title;
        } catch (Exception e) {
            logger.error("Failed to get page title", e);
            return "";
        }
    }
    
    /**
     * Verifies if employee list page is loaded by checking key elements
     * 
     * @return true if employee list page is loaded correctly
     */
    @Override
    public boolean isPageLoaded() {
        try {
            boolean titleVisible = isElementDisplayed(pageTitleBy);
            boolean addButtonVisible = isElementDisplayed(addButtonBy);
            boolean searchVisible = isElementDisplayed(searchButtonBy);
            
            boolean isLoaded = titleVisible && addButtonVisible && searchVisible;
            logger.debug("Employee list page loaded status: {}", isLoaded);
            return isLoaded;
        } catch (Exception e) {
            logger.error("Error checking if employee list page is loaded", e);
            return false;
        }
    }
    
    /**
     * Inner class to represent Employee information
     */
    public static class EmployeeInfo {
        private String employeeId;
        private String firstName;
        private String lastName;
        private String jobTitle;
        
        // Constructors
        public EmployeeInfo() {}
        
        public EmployeeInfo(String employeeId, String firstName, String lastName, String jobTitle) {
            this.employeeId = employeeId;
            this.firstName = firstName;
            this.lastName = lastName;
            this.jobTitle = jobTitle;
        }
        
        // Getters and Setters
        public String getEmployeeId() { return employeeId; }
        public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
        
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        
        public String getJobTitle() { return jobTitle; }
        public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
        
        public String getFullName() {
            return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
        }
        
        @Override
        public String toString() {
            return String.format("EmployeeInfo{id='%s', name='%s %s', jobTitle='%s'}", 
                               employeeId, firstName, lastName, jobTitle);
        }
    }
}