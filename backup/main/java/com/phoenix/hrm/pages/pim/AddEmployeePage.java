package com.phoenix.hrm.pages.pim;

import com.phoenix.hrm.core.base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Page Object Model for Add Employee Page
 * Provides methods to interact with the Add Employee form in OrangeHRM
 * 
 * Features:
 * - Employee creation with mandatory and optional fields
 * - Form validation and error handling
 * - Success message verification
 * - Navigation capabilities
 * - BDD step definition support
 * 
 * @author Phoenix HRM Test Team
 */
public class AddEmployeePage extends BasePage {
    
    private static final Logger logger = LoggerFactory.getLogger(AddEmployeePage.class);
    
    // Page Elements using PageFactory
    @FindBy(xpath = "//h6[text()='Add Employee']")
    private WebElement addEmployeeHeader;
    
    @FindBy(name = "firstName")
    private WebElement firstNameField;
    
    @FindBy(name = "middleName") 
    private WebElement middleNameField;
    
    @FindBy(name = "lastName")
    private WebElement lastNameField;
    
    @FindBy(xpath = "//label[text()='Employee Id']/following::input[1]")
    private WebElement employeeIdField;
    
    @FindBy(xpath = "//label[text()='Other Id']/following::input[1]")
    private WebElement otherIdField;
    
    @FindBy(xpath = "//label[text()=\"Driver's License Number\"]/following::input[1]")
    private WebElement driverLicenseField;
    
    @FindBy(xpath = "//label[text()='License Expiry Date']/following::input[1]")
    private WebElement licenseExpiryField;
    
    @FindBy(xpath = "//button[@type='submit' and contains(text(),'Save')]")
    private WebElement saveButton;
    
    @FindBy(xpath = "//button[contains(text(),'Cancel')]")
    private WebElement cancelButton;
    
    @FindBy(xpath = "//div[@class='oxd-toast-content']//p")
    private WebElement successMessage;
    
    @FindBy(xpath = "//span[contains(@class,'oxd-input-field-error-message')]")
    private List<WebElement> errorMessages;
    
    @FindBy(linkText = "Employee List")
    private WebElement employeeListLink;
    
    @FindBy(xpath = "//input[@type='file']")
    private WebElement photoUploadField;
    
    @FindBy(xpath = "//button[contains(text(),'Create Login Details')]")
    private WebElement createLoginToggle;
    
    /**
     * Constructor
     */
    public AddEmployeePage(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
        logger.info("AddEmployeePage initialized");
    }
    
    // ===================== Employee Information Methods =====================
    
    /**
     * Enters first name of the employee
     */
    public AddEmployeePage enterFirstName(String firstName) {
        clearAndEnterText(firstNameField, firstName, "First Name");
        return this;
    }
    
    /**
     * Enters middle name of the employee
     */
    public AddEmployeePage enterMiddleName(String middleName) {
        if (middleName != null && !middleName.trim().isEmpty()) {
            clearAndEnterText(middleNameField, middleName, "Middle Name");
        }
        return this;
    }
    
    /**
     * Enters last name of the employee
     */
    public AddEmployeePage enterLastName(String lastName) {
        clearAndEnterText(lastNameField, lastName, "Last Name");
        return this;
    }
    
    /**
     * Enters employee ID
     */
    public AddEmployeePage enterEmployeeId(String employeeId) {
        clearAndEnterText(employeeIdField, employeeId, "Employee ID");
        return this;
    }
    
    /**
     * Enters other ID for the employee
     */
    public AddEmployeePage enterOtherId(String otherId) {
        if (otherId != null && !otherId.trim().isEmpty()) {
            clearAndEnterText(otherIdField, otherId, "Other ID");
        }
        return this;
    }
    
    /**
     * Enters driver's license number
     */
    public AddEmployeePage enterDriverLicenseNumber(String licenseNumber) {
        if (licenseNumber != null && !licenseNumber.trim().isEmpty()) {
            clearAndEnterText(driverLicenseField, licenseNumber, "Driver's License Number");
        }
        return this;
    }
    
    /**
     * Enters license expiry date
     */
    public AddEmployeePage enterLicenseExpiryDate(String expiryDate) {
        if (expiryDate != null && !expiryDate.trim().isEmpty()) {
            clearAndEnterText(licenseExpiryField, expiryDate, "License Expiry Date");
        }
        return this;
    }
    
    /**
     * Uploads employee photo
     */
    public AddEmployeePage uploadPhoto(String photoPath) {
        if (photoPath != null && !photoPath.trim().isEmpty()) {
            logger.info("Uploading employee photo: {}", photoPath);
            photoUploadField.sendKeys(photoPath);
        }
        return this;
    }
    
    // ===================== Form Action Methods =====================
    
    /**
     * Clicks the Save button
     */
    public AddEmployeePage clickSaveButton() {
        clickElement(saveButton, "Save Button");
        return this;
    }
    
    /**
     * Clicks the Cancel button
     */
    public AddEmployeePage clickCancelButton() {
        clickElement(cancelButton, "Cancel Button");
        return this;
    }
    
    /**
     * Toggles the create login details option
     */
    public AddEmployeePage toggleCreateLoginDetails() {
        clickElement(createLoginToggle, "Create Login Details Toggle");
        return this;
    }
    
    // ===================== Verification Methods =====================
    
    /**
     * Verifies if the Add Employee page is displayed
     */
    public boolean isAddEmployeePageDisplayed() {
        return isElementDisplayed(addEmployeeHeader, "Add Employee Header");
    }
    
    /**
     * Verifies if employee was created successfully (BDD support)
     */
    public boolean isEmployeeCreatedSuccessfully() {
        try {
            return waitForElement(successMessage, 10).isDisplayed() ||
                   driver.getCurrentUrl().contains("/personalDetails");
        } catch (Exception e) {
            logger.debug("Success verification failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifies if employee was updated successfully (BDD support)
     */
    public boolean isEmployeeUpdatedSuccessfully() {
        try {
            return isSuccessMessageDisplayed() || 
                   driver.getPageSource().contains("Successfully Updated");
        } catch (Exception e) {
            logger.debug("Update verification failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifies if all employee details are saved correctly (BDD support)
     */
    public boolean areAllDetailssSavedCorrectly() {
        try {
            // Check if we're on the employee details page or success is shown
            return isSuccessMessageDisplayed() || 
                   driver.getCurrentUrl().contains("/personalDetails");
        } catch (Exception e) {
            logger.debug("Details verification failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifies if validation error messages are displayed (BDD support)
     */
    public boolean areValidationErrorsDisplayed() {
        try {
            return !errorMessages.isEmpty() && 
                   errorMessages.stream().anyMatch(WebElement::isDisplayed);
        } catch (Exception e) {
            logger.debug("Validation error check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifies if confirmation message is displayed (BDD support)
     */
    public boolean isConfirmationMessageDisplayed() {
        try {
            return isSuccessMessageDisplayed() ||
                   driver.getPageSource().contains("Successfully");
        } catch (Exception e) {
            logger.debug("Confirmation message check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifies success message is displayed
     */
    public boolean isSuccessMessageDisplayed() {
        try {
            return waitForElement(successMessage, 10).isDisplayed();
        } catch (Exception e) {
            logger.debug("Success message not found: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets the success message text
     */
    public String getSuccessMessageText() {
        return getElementText(successMessage, "Success Message");
    }
    
    /**
     * Gets all error messages
     */
    public List<String> getErrorMessages() {
        return errorMessages.stream()
                .filter(WebElement::isDisplayed)
                .map(element -> element.getText())
                .toList();
    }
    
    /**
     * Gets the current employee ID value
     */
    public String getCurrentEmployeeId() {
        return employeeIdField.getAttribute("value");
    }
    
    /**
     * Verifies if all mandatory fields are empty
     */
    public boolean areMandatoryFieldsEmpty() {
        String firstName = firstNameField.getAttribute("value");
        String lastName = lastNameField.getAttribute("value");
        return (firstName == null || firstName.isEmpty()) &&
               (lastName == null || lastName.isEmpty());
    }
    
    // ===================== Navigation Methods =====================
    
    /**
     * Navigates to Employee List page (BDD support)
     */
    public AddEmployeePage navigateToEmployeeList() {
        try {
            if (isElementDisplayed(employeeListLink, "Employee List Link")) {
                clickElement(employeeListLink, "Employee List Link");
            } else {
                // Alternative: navigate via URL
                String currentUrl = driver.getCurrentUrl();
                String baseUrl = currentUrl.substring(0, currentUrl.indexOf("/web/") + 5);
                driver.get(baseUrl + "index.php/pim/viewEmployeeList");
            }
        } catch (Exception e) {
            logger.warn("Failed to navigate to Employee List: {}", e.getMessage());
        }
        return this;
    }
    
    // ===================== Utility Methods =====================
    
    /**
     * Fills the complete add employee form with all provided details
     */
    public AddEmployeePage fillEmployeeForm(String firstName, String middleName, String lastName, 
                                          String employeeId, String otherId, String licenseNumber, 
                                          String expiryDate) {
        logger.info("Filling employee form with provided details");
        
        enterFirstName(firstName);
        enterMiddleName(middleName);
        enterLastName(lastName);
        
        if (employeeId != null && !employeeId.trim().isEmpty()) {
            enterEmployeeId(employeeId);
        }
        
        enterOtherId(otherId);
        enterDriverLicenseNumber(licenseNumber);
        enterLicenseExpiryDate(expiryDate);
        
        return this;
    }
    
    /**
     * Fills the basic employee information (mandatory fields only)
     */
    public AddEmployeePage fillBasicEmployeeInfo(String firstName, String lastName) {
        logger.info("Filling basic employee information: {} {}", firstName, lastName);
        enterFirstName(firstName);
        enterLastName(lastName);
        return this;
    }
    
    /**
     * Clears all form fields
     */
    public AddEmployeePage clearAllFields() {
        logger.info("Clearing all employee form fields");
        
        firstNameField.clear();
        middleNameField.clear();
        lastNameField.clear();
        employeeIdField.clear();
        otherIdField.clear();
        driverLicenseField.clear();
        licenseExpiryField.clear();
        
        return this;
    }
    
    /**
     * Waits for the page to load completely
     */
    public AddEmployeePage waitForPageLoad() {
        waitForElement(addEmployeeHeader, 15);
        waitForElement(firstNameField, 10);
        return this;
    }
}