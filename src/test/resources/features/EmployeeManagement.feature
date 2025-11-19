@EmployeeManagement
Feature: Employee Management System
  As an HR Administrator
  I want to manage employee records in OrangeHRM
  So that I can maintain accurate employee information

  Background:
    Given I am on the OrangeHRM login page
    When I login with valid credentials
    Then I should be on the dashboard page
    And I navigate to PIM module

  @Smoke @EmployeeCreation
  Scenario: Successfully add a new employee with mandatory fields
    Given I am on the employee list page
    When I click on Add Employee button
    And I enter employee details:
      | firstName | lastName | employeeId |
      | John      | Doe      | EMP001     |
    And I save the employee
    Then the employee should be created successfully
    And I should see success message
    And the employee should appear in employee list

  @Regression @EmployeeCreation
  Scenario: Add employee with all optional fields
    Given I am on the employee list page
    When I click on Add Employee button
    And I enter complete employee details:
      | firstName | middleName | lastName | employeeId | otherId | driverLicense | licenseExpiry |
      | Jane      | Marie      | Smith    | EMP002     | ALT001  | DL123456789   | 2025-12-31    |
    And I save the employee
    Then the employee should be created successfully
    And all employee details should be saved correctly

  @NegativeTest @EmployeeCreation
  Scenario: Attempt to add employee without mandatory fields
    Given I am on the employee list page
    When I click on Add Employee button
    And I try to save employee without entering mandatory fields
    Then I should see validation error messages
    And the employee should not be created

  @Regression @EmployeeSearch
  Scenario Outline: Search employees by different criteria
    Given I am on the employee list page
    When I search for employee with "<searchCriteria>" as "<searchValue>"
    And I click search button
    Then I should see search results containing "<expectedResult>"

    Examples:
      | searchCriteria | searchValue | expectedResult |
      | Employee Name  | John        | John           |
      | Employee Id    | EMP001      | EMP001         |
      | Job Title      | Developer   | Developer      |

  @Smoke @EmployeeUpdate
  Scenario: Update existing employee information
    Given I am on the employee list page
    And employee "John Doe" exists in the system
    When I search for employee "John Doe"
    And I click on edit employee
    And I update employee details:
      | middleName | otherId |
      | William    | ALT002  |
    And I save the changes
    Then the employee information should be updated successfully
    And I should see confirmation message

  @Regression @EmployeeDelete
  Scenario: Delete an employee record
    Given I am on the employee list page
    And employee "Jane Smith" exists in the system
    When I search for employee "Jane Smith"
    And I select the employee checkbox
    And I click on delete button
    And I confirm the deletion
    Then the employee should be deleted successfully
    And the employee should not appear in employee list

  @DataDriven @EmployeeCreation
  Scenario Outline: Create multiple employees with different data sets
    Given I am on the employee list page
    When I click on Add Employee button
    And I enter employee details:
      | firstName   | lastName   | employeeId   |
      | <firstName> | <lastName> | <employeeId> |
    And I save the employee
    Then the employee "<firstName> <lastName>" should be created successfully

    Examples:
      | firstName | lastName  | employeeId |
      | Alice     | Johnson   | EMP003     |
      | Bob       | Williams  | EMP004     |
      | Carol     | Brown     | EMP005     |
      | David     | Davis     | EMP006     |

  @Smoke @EmployeeNavigation
  Scenario: Navigate through employee management pages
    Given I am on the employee list page
    When I click on Add Employee button
    Then I should be on add employee page
    When I click on employee list link
    Then I should be back to employee list page

  @Performance @EmployeeList
  Scenario: Employee list page performance validation
    Given I am on the employee list page
    When I measure the page load time
    Then the page should load within 3 seconds
    And all employee records should be displayed correctly

  @Integration @EmployeeWorkflow
  Scenario: Complete employee lifecycle workflow
    Given I am on the employee list page
    # Create employee
    When I add a new employee with details:
      | firstName | lastName | employeeId |
      | Mike      | Wilson   | EMP007     |
    Then the employee should be created successfully
    
    # Search and verify
    When I search for the created employee "Mike Wilson"
    Then I should find the employee in search results
    
    # Update employee
    When I update the employee with new information:
      | middleName | otherId |
      | Alexander  | ALT003  |
    Then the employee information should be updated
    
    # Final verification
    When I search for the updated employee "Mike Alexander Wilson"
    Then I should see the updated employee details
    
    # Cleanup - Delete employee
    When I delete the employee "Mike Alexander Wilson"
    Then the employee should be removed from the system

  @ErrorHandling @EmployeeManagement
  Scenario: Handle system errors gracefully
    Given I am on the employee list page
    When I encounter a system error during employee operations
    Then I should see appropriate error messages
    And the system should remain stable
    And I should be able to retry the operation