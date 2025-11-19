package com.phoenix.hrm.pages.pim;

import com.phoenix.hrm.core.base.BasePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Employee Details Page - Represents the employee details/profile page
 * 
 * This page provides functionality to view and manage individual employee details
 * including personal information, job details, and other employee-related data.
 * 
 * @author Phoenix HRM Test Team
 */
public class EmployeeDetailsPage extends BasePage {
    
    // Page elements
    @FindBy(xpath = "//h6[contains(@class, 'oxd-text--h6') and contains(text(), 'Personal Details')]")
    private WebElement personalDetailsHeader;
    
    @FindBy(name = "firstName")
    private WebElement firstNameField;
    
    @FindBy(name = "lastName")
    private WebElement lastNameField;
    
    @FindBy(xpath = "//input[@placeholder='Employee Id']")
    private WebElement employeeIdField;
    
    @FindBy(xpath = "//button[@type='submit' and contains(text(), 'Save')]")
    private WebElement saveButton;
    
    @FindBy(xpath = "//button[contains(text(), 'Cancel')]")
    private WebElement cancelButton;
    
    @FindBy(xpath = "//div[@class='orangehrm-edit-employee']")
    private WebElement employeeDetailsContainer;
    
    /**
     * Constructor
     * @param driver WebDriver instance
     */
    public EmployeeDetailsPage(WebDriver driver) {
        super(driver);
    }
    
    /**
     * Checks if the employee details page is displayed
     * @return true if page is displayed
     */
    public boolean isEmployeeDetailsDisplayed() {
        return isElementDisplayed(personalDetailsHeader);
    }
    
    /**
     * Gets the first name from the form
     * @return first name value
     */
    public String getFirstName() {
        return getElementValue(firstNameField);
    }
    
    /**
     * Gets the last name from the form
     * @return last name value
     */
    public String getLastName() {
        return getElementValue(lastNameField);
    }
    
    /**
     * Gets the employee ID from the form
     * @return employee ID value
     */
    public String getEmployeeId() {
        return getElementValue(employeeIdField);
    }
    
    /**
     * Updates employee first name
     * @param firstName new first name
     */
    public void updateFirstName(String firstName) {
        clearAndType(firstNameField, firstName);
    }
    
    /**
     * Updates employee last name
     * @param lastName new last name
     */
    public void updateLastName(String lastName) {
        clearAndType(lastNameField, lastName);
    }
    
    /**
     * Saves the employee details
     */
    public void saveEmployeeDetails() {
        click(saveButton);
        waitForPageLoad();
    }
    
    /**
     * Cancels the employee details editing
     */
    public void cancelEdit() {
        click(cancelButton);
        waitForPageLoad();
    }
    
    /**
     * Waits for the page to load completely
     */
    public void waitForPageLoad() {
        waitForElementToBeVisible(personalDetailsHeader, 10);
        waitForElementToBeVisible(employeeDetailsContainer, 10);
    }
    
    /**
     * Gets the full employee name (first + last)
     * @return full name
     */
    public String getFullEmployeeName() {
        return getFirstName() + " " + getLastName();
    }
    
    /**
     * Checks if the save button is enabled
     * @return true if save button is enabled
     */
    public boolean isSaveButtonEnabled() {
        return isElementEnabled(saveButton);
    }
    
    /**
     * Checks if the page is in edit mode
     * @return true if page is in edit mode
     */
    public boolean isInEditMode() {
        return isElementEnabled(firstNameField) && isElementEnabled(lastNameField);
    }
}