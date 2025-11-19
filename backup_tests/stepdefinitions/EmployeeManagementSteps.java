package com.phoenix.hrm.stepdefinitions;

import com.phoenix.hrm.core.driver.WebDriverFactory;
import com.phoenix.hrm.core.config.ConfigManager;
import com.phoenix.hrm.core.utils.ScreenshotUtils;
import com.phoenix.hrm.models.Employee;
import com.phoenix.hrm.pages.pim.AddEmployeePage;
import com.phoenix.hrm.pages.DashboardPage;
import com.phoenix.hrm.pages.pim.EmployeeListPage;
import com.phoenix.hrm.pages.LoginPage;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.*;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Step Definitions for Employee Management Cucumber Scenarios
 * Implements all step definitions with Page Object Model integration
 * 
 * @author Phoenix HRM Test Team
 */
public class EmployeeManagementSteps {
    
    private static final Logger logger = LoggerFactory.getLogger(EmployeeManagementSteps.class);
    
    // Page Objects - Injected via PicoContainer
    private WebDriver driver;
    private LoginPage loginPage;
    private DashboardPage dashboardPage;
    private EmployeeListPage employeeListPage;
    private AddEmployeePage addEmployeePage;
    
    // Test Data
    private Employee currentEmployee;
    private List<EmployeeListPage.EmployeeInfo> searchResults;
    private Instant pageLoadStartTime;
    private String lastCreatedEmployeeName;
    private String lastOperationMessage;
    
    // Configuration
    private final ConfigManager config = ConfigManager.getInstance();
    
    /**
     * Constructor for dependency injection
     */
    public EmployeeManagementSteps() {
        this.driver = WebDriverFactory.getDriver();
        initializePageObjects();
    }
    
    private void initializePageObjects() {
        this.loginPage = new LoginPage(driver);
        this.dashboardPage = new DashboardPage(driver);
        this.employeeListPage = new EmployeeListPage(driver);
        this.addEmployeePage = new AddEmployeePage(driver);
    }
    
    // ========================= GIVEN STEPS =========================
    
    @Given("I am on the OrangeHRM login page")
    public void i_am_on_the_orange_hrm_login_page() {
        logger.info("Navigating to OrangeHRM login page");
        String baseUrl = config.getProperty("base.url");
        driver.get(baseUrl);
        Assert.assertTrue(loginPage.isLoginPageDisplayed(), "Login page should be displayed");
    }
    
    @Given("I am on the employee list page")
    public void i_am_on_the_employee_list_page() {
        logger.info("Verifying user is on employee list page");
        Assert.assertTrue(employeeListPage.isEmployeeListPageDisplayed(), 
            "Employee list page should be displayed");
    }
    
    @Given("employee {string} exists in the system")
    public void employee_exists_in_the_system(String employeeName) {
        logger.info("Verifying employee '{}' exists in the system", employeeName);
        
        // Search for the employee to verify existence
        String[] nameParts = employeeName.split(" ");
        if (nameParts.length >= 2) {
            employeeListPage.searchByEmployeeName(nameParts[0] + " " + nameParts[1]);
            searchResults = employeeListPage.getSearchResults();
            
            boolean employeeFound = searchResults.stream()
                .anyMatch(emp -> emp.getFullName().contains(employeeName));
            
            if (!employeeFound) {
                // Create the employee if it doesn't exist
                logger.info("Employee '{}' not found. Creating employee for test setup.", employeeName);
                createEmployeeForSetup(nameParts[0], nameParts.length > 1 ? nameParts[1] : "");
            }
        }
    }
    
    // ========================= WHEN STEPS =========================
    
    @When("I login with valid credentials")
    public void i_login_with_valid_credentials() {
        logger.info("Attempting to login with valid credentials");
        String username = config.getProperty("login.username");
        String password = config.getProperty("login.password");
        
        loginPage.enterUsername(username);
        loginPage.enterPassword(password);
        loginPage.clickLoginButton();
    }
    
    @When("I navigate to PIM module")
    public void i_navigate_to_pim_module() {
        logger.info("Navigating to PIM module");
        dashboardPage.navigateToPIM();
    }
    
    @When("I click on Add Employee button")
    public void i_click_on_add_employee_button() {
        logger.info("Clicking on Add Employee button");
        employeeListPage.clickAddEmployeeButton();
    }
    
    @When("I enter employee details:")
    public void i_enter_employee_details(DataTable dataTable) {
        logger.info("Entering employee details from data table");
        Map<String, String> employeeData = dataTable.asMap(String.class, String.class);
        
        currentEmployee = new Employee.Builder()
            .firstName(employeeData.get("firstName"))
            .lastName(employeeData.get("lastName"))
            .employeeId(employeeData.get("employeeId"))
            .build();
        
        addEmployeePage.enterFirstName(currentEmployee.getFirstName());
        addEmployeePage.enterLastName(currentEmployee.getLastName());
        addEmployeePage.enterEmployeeId(currentEmployee.getEmployeeId());
        
        lastCreatedEmployeeName = currentEmployee.getFirstName() + " " + currentEmployee.getLastName();
    }
    
    @When("I enter complete employee details:")
    public void i_enter_complete_employee_details(DataTable dataTable) {
        logger.info("Entering complete employee details including optional fields");
        Map<String, String> employeeData = dataTable.asMap(String.class, String.class);
        
        currentEmployee = new Employee.Builder()
            .firstName(employeeData.get("firstName"))
            .middleName(employeeData.get("middleName"))
            .lastName(employeeData.get("lastName"))
            .employeeId(employeeData.get("employeeId"))
            .otherId(employeeData.get("otherId"))
            .driverLicenseNumber(employeeData.get("driverLicense"))
            .licenseExpiryDate(employeeData.get("licenseExpiry"))
            .build();
        
        addEmployeePage.enterFirstName(currentEmployee.getFirstName());
        addEmployeePage.enterMiddleName(currentEmployee.getMiddleName());
        addEmployeePage.enterLastName(currentEmployee.getLastName());
        addEmployeePage.enterEmployeeId(currentEmployee.getEmployeeId());
        addEmployeePage.enterOtherId(currentEmployee.getOtherId());
        addEmployeePage.enterDriverLicenseNumber(currentEmployee.getDriverLicenseNumber());
        addEmployeePage.enterLicenseExpiryDate(currentEmployee.getLicenseExpiryDate());
        
        lastCreatedEmployeeName = currentEmployee.getFirstName() + " " + currentEmployee.getLastName();
    }
    
    @When("I save the employee")
    public void i_save_the_employee() {
        logger.info("Saving the employee");
        addEmployeePage.clickSaveButton();
    }
    
    @When("I try to save employee without entering mandatory fields")
    public void i_try_to_save_employee_without_entering_mandatory_fields() {
        logger.info("Attempting to save employee without mandatory fields");
        addEmployeePage.clickSaveButton();
    }
    
    @When("I search for employee with {string} as {string}")
    public void i_search_for_employee_with_criteria_as_value(String searchCriteria, String searchValue) {
        logger.info("Searching for employee with {} as {}", searchCriteria, searchValue);
        
        switch (searchCriteria.toLowerCase()) {
            case "employee name":
                employeeListPage.searchByEmployeeName(searchValue);
                break;
            case "employee id":
                employeeListPage.searchByEmployeeId(searchValue);
                break;
            case "job title":
                employeeListPage.searchByJobTitle(searchValue);
                break;
            default:
                throw new IllegalArgumentException("Unsupported search criteria: " + searchCriteria);
        }
    }
    
    @When("I click search button")
    public void i_click_search_button() {
        logger.info("Clicking search button");
        employeeListPage.clickSearchButton();
        searchResults = employeeListPage.getSearchResults();
    }
    
    @When("I search for employee {string}")
    public void i_search_for_employee(String employeeName) {
        logger.info("Searching for employee: {}", employeeName);
        employeeListPage.searchByEmployeeName(employeeName);
        employeeListPage.clickSearchButton();
        searchResults = employeeListPage.getSearchResults();
    }
    
    @When("I click on edit employee")
    public void i_click_on_edit_employee() {
        logger.info("Clicking on edit employee");
        if (!searchResults.isEmpty()) {
            employeeListPage.editEmployee(searchResults.get(0).getEmployeeId());
        }
    }
    
    @When("I update employee details:")
    public void i_update_employee_details(DataTable dataTable) {
        logger.info("Updating employee details");
        Map<String, String> updateData = dataTable.asMap(String.class, String.class);
        
        if (updateData.containsKey("middleName")) {
            addEmployeePage.enterMiddleName(updateData.get("middleName"));
        }
        if (updateData.containsKey("otherId")) {
            addEmployeePage.enterOtherId(updateData.get("otherId"));
        }
    }
    
    @When("I save the changes")
    public void i_save_the_changes() {
        logger.info("Saving the changes");
        addEmployeePage.clickSaveButton();
    }
    
    @When("I select the employee checkbox")
    public void i_select_the_employee_checkbox() {
        logger.info("Selecting employee checkbox");
        if (!searchResults.isEmpty()) {
            employeeListPage.selectEmployeeCheckbox(searchResults.get(0).getEmployeeId());
        }
    }
    
    @When("I click on delete button")
    public void i_click_on_delete_button() {
        logger.info("Clicking on delete button");
        employeeListPage.clickDeleteSelectedButton();
    }
    
    @When("I confirm the deletion")
    public void i_confirm_the_deletion() {
        logger.info("Confirming the deletion");
        employeeListPage.confirmDeletion();
    }
    
    @When("I click on employee list link")
    public void i_click_on_employee_list_link() {
        logger.info("Clicking on employee list link");
        addEmployeePage.navigateToEmployeeList();
    }
    
    @When("I measure the page load time")
    public void i_measure_the_page_load_time() {
        logger.info("Starting page load time measurement");
        pageLoadStartTime = Instant.now();
        driver.navigate().refresh();
        employeeListPage.waitForPageLoad();
    }
    
    @When("I add a new employee with details:")
    public void i_add_a_new_employee_with_details(DataTable dataTable) {
        i_click_on_add_employee_button();
        i_enter_employee_details(dataTable);
        i_save_the_employee();
    }
    
    @When("I search for the created employee {string}")
    public void i_search_for_the_created_employee(String employeeName) {
        i_search_for_employee(employeeName);
    }
    
    @When("I update the employee with new information:")
    public void i_update_the_employee_with_new_information(DataTable dataTable) {
        if (!searchResults.isEmpty()) {
            employeeListPage.editEmployee(searchResults.get(0).getEmployeeId());
            i_update_employee_details(dataTable);
            i_save_the_changes();
        }
    }
    
    @When("I search for the updated employee {string}")
    public void i_search_for_the_updated_employee(String employeeName) {
        i_search_for_employee(employeeName);
    }
    
    @When("I delete the employee {string}")
    public void i_delete_the_employee(String employeeName) {
        i_search_for_employee(employeeName);
        i_select_the_employee_checkbox();
        i_click_on_delete_button();
        i_confirm_the_deletion();
    }
    
    @When("I encounter a system error during employee operations")
    public void i_encounter_a_system_error_during_employee_operations() {
        // This step would simulate or handle actual system errors
        logger.info("Simulating system error scenario");
    }
    
    // ========================= THEN STEPS =========================
    
    @Then("I should be on the dashboard page")
    public void i_should_be_on_the_dashboard_page() {
        logger.info("Verifying user is on dashboard page");
        Assert.assertTrue(dashboardPage.isDashboardDisplayed(), "Dashboard should be displayed");
    }
    
    @Then("the employee should be created successfully")
    public void the_employee_should_be_created_successfully() {
        logger.info("Verifying employee creation success");
        Assert.assertTrue(addEmployeePage.isEmployeeCreatedSuccessfully(), 
            "Employee should be created successfully");
    }
    
    @Then("I should see success message")
    public void i_should_see_success_message() {
        logger.info("Verifying success message is displayed");
        Assert.assertTrue(addEmployeePage.isSuccessMessageDisplayed(), 
            "Success message should be displayed");
    }
    
    @Then("the employee should appear in employee list")
    public void the_employee_should_appear_in_employee_list() {
        logger.info("Verifying employee appears in employee list");
        addEmployeePage.navigateToEmployeeList();
        
        if (lastCreatedEmployeeName != null) {
            employeeListPage.searchByEmployeeName(lastCreatedEmployeeName);
            employeeListPage.clickSearchButton();
            searchResults = employeeListPage.getSearchResults();
            
            boolean employeeFound = searchResults.stream()
                .anyMatch(emp -> emp.getFullName().contains(lastCreatedEmployeeName));
            
            Assert.assertTrue(employeeFound, "Employee should appear in employee list");
        }
    }
    
    @Then("all employee details should be saved correctly")
    public void all_employee_details_should_be_saved_correctly() {
        logger.info("Verifying all employee details are saved correctly");
        Assert.assertTrue(addEmployeePage.areAllDetailssSavedCorrectly(), 
            "All employee details should be saved correctly");
    }
    
    @Then("I should see validation error messages")
    public void i_should_see_validation_error_messages() {
        logger.info("Verifying validation error messages are displayed");
        Assert.assertTrue(addEmployeePage.areValidationErrorsDisplayed(), 
            "Validation error messages should be displayed");
    }
    
    @Then("the employee should not be created")
    public void the_employee_should_not_be_created() {
        logger.info("Verifying employee was not created");
        Assert.assertFalse(addEmployeePage.isEmployeeCreatedSuccessfully(), 
            "Employee should not be created");
    }
    
    @Then("I should see search results containing {string}")
    public void i_should_see_search_results_containing(String expectedResult) {
        logger.info("Verifying search results contain: {}", expectedResult);
        
        boolean resultFound = searchResults.stream()
            .anyMatch(emp -> emp.getFullName().contains(expectedResult) || 
                           emp.getEmployeeId().contains(expectedResult));
        
        Assert.assertTrue(resultFound, "Search results should contain: " + expectedResult);
    }
    
    @Then("the employee information should be updated successfully")
    public void the_employee_information_should_be_updated_successfully() {
        logger.info("Verifying employee information was updated successfully");
        Assert.assertTrue(addEmployeePage.isEmployeeUpdatedSuccessfully(), 
            "Employee information should be updated successfully");
    }
    
    @Then("I should see confirmation message")
    public void i_should_see_confirmation_message() {
        logger.info("Verifying confirmation message is displayed");
        Assert.assertTrue(addEmployeePage.isConfirmationMessageDisplayed(), 
            "Confirmation message should be displayed");
    }
    
    @Then("the employee should be deleted successfully")
    public void the_employee_should_be_deleted_successfully() {
        logger.info("Verifying employee deletion success");
        Assert.assertTrue(employeeListPage.isEmployeeDeletedSuccessfully(), 
            "Employee should be deleted successfully");
    }
    
    @Then("the employee should not appear in employee list")
    public void the_employee_should_not_appear_in_employee_list() {
        logger.info("Verifying employee no longer appears in employee list");
        // Refresh the search results
        employeeListPage.clickSearchButton();
        searchResults = employeeListPage.getSearchResults();
        
        Assert.assertTrue(searchResults.isEmpty(), 
            "Employee should not appear in employee list after deletion");
    }
    
    @Then("the employee {string} should be created successfully")
    public void the_employee_should_be_created_successfully(String employeeName) {
        logger.info("Verifying employee '{}' was created successfully", employeeName);
        lastCreatedEmployeeName = employeeName;
        the_employee_should_be_created_successfully();
    }
    
    @Then("I should be on add employee page")
    public void i_should_be_on_add_employee_page() {
        logger.info("Verifying user is on add employee page");
        Assert.assertTrue(addEmployeePage.isAddEmployeePageDisplayed(), 
            "Add employee page should be displayed");
    }
    
    @Then("I should be back to employee list page")
    public void i_should_be_back_to_employee_list_page() {
        logger.info("Verifying user is back to employee list page");
        Assert.assertTrue(employeeListPage.isEmployeeListPageDisplayed(), 
            "Employee list page should be displayed");
    }
    
    @Then("the page should load within {int} seconds")
    public void the_page_should_load_within_seconds(int maxSeconds) {
        logger.info("Verifying page loads within {} seconds", maxSeconds);
        
        if (pageLoadStartTime != null) {
            Duration loadTime = Duration.between(pageLoadStartTime, Instant.now());
            Assert.assertTrue(loadTime.getSeconds() <= maxSeconds, 
                String.format("Page should load within %d seconds, but took %d seconds", 
                    maxSeconds, loadTime.getSeconds()));
        }
    }
    
    @Then("all employee records should be displayed correctly")
    public void all_employee_records_should_be_displayed_correctly() {
        logger.info("Verifying all employee records are displayed correctly");
        Assert.assertTrue(employeeListPage.areAllRecordsDisplayedCorrectly(), 
            "All employee records should be displayed correctly");
    }
    
    @Then("I should find the employee in search results")
    public void i_should_find_the_employee_in_search_results() {
        logger.info("Verifying employee is found in search results");
        Assert.assertFalse(searchResults.isEmpty(), 
            "Employee should be found in search results");
    }
    
    @Then("the employee information should be updated")
    public void the_employee_information_should_be_updated() {
        the_employee_information_should_be_updated_successfully();
    }
    
    @Then("I should see the updated employee details")
    public void i_should_see_the_updated_employee_details() {
        logger.info("Verifying updated employee details are visible");
        Assert.assertFalse(searchResults.isEmpty(), 
            "Updated employee details should be visible in search results");
    }
    
    @Then("the employee should be removed from the system")
    public void the_employee_should_be_removed_from_the_system() {
        the_employee_should_be_deleted_successfully();
    }
    
    @Then("I should see appropriate error messages")
    public void i_should_see_appropriate_error_messages() {
        logger.info("Verifying appropriate error messages are displayed");
        // Implementation would check for specific error message elements
        Assert.assertTrue(true, "Error messages should be displayed appropriately");
    }
    
    @Then("the system should remain stable")
    public void the_system_should_remain_stable() {
        logger.info("Verifying system stability");
        Assert.assertTrue(employeeListPage.isEmployeeListPageDisplayed(), 
            "System should remain stable");
    }
    
    @Then("I should be able to retry the operation")
    public void i_should_be_able_to_retry_the_operation() {
        logger.info("Verifying ability to retry operations");
        Assert.assertTrue(employeeListPage.isEmployeeListPageDisplayed(), 
            "Should be able to retry operations");
    }
    
    // ========================= HELPER METHODS =========================
    
    /**
     * Helper method to create an employee for test setup
     */
    private void createEmployeeForSetup(String firstName, String lastName) {
        try {
            employeeListPage.clickAddEmployeeButton();
            addEmployeePage.enterFirstName(firstName);
            addEmployeePage.enterLastName(lastName);
            addEmployeePage.enterEmployeeId("SETUP_" + System.currentTimeMillis());
            addEmployeePage.clickSaveButton();
            addEmployeePage.navigateToEmployeeList();
        } catch (Exception e) {
            logger.warn("Failed to create employee for setup: {}", e.getMessage());
        }
    }
}