Feature: User Login Functionality
  As a Phoenix HRM user
  I want to login to the system with my credentials
  So that I can access the HRM functionalities

  Background:
    Given I am on the login page
    And the login form should be displayed
    And the page title should contain "OrangeHRM"

  @smoke @regression @positive
  Scenario: Successful login with admin credentials
    When I login as an admin
    Then I should be successfully logged in
    And I should see the dashboard page

  @smoke @regression @positive
  Scenario: Successful login with valid role credentials
    When I login with "admin" role credentials
    Then I should be successfully logged in
    And I should see the dashboard page

  @regression @negative
  Scenario: Failed login with invalid username
    When I login with invalid credentials "invaliduser" and "admin123"
    Then I should see an error message
    And the error message should contain "invalid"
    And I should remain on the login page

  @regression @negative  
  Scenario: Failed login with invalid password
    When I login with invalid credentials "Admin" and "wrongpassword"
    Then I should see an error message
    And the error message should contain "invalid"
    And I should remain on the login page

  @regression @negative
  Scenario: Failed login with both invalid credentials
    When I login with invalid credentials "invaliduser" and "wrongpassword"
    Then I should see an error message
    And I should remain on the login page

  @regression @negative
  Scenario: Failed login with empty username
    When I login with invalid credentials "" and "admin123"
    Then I should see an error message
    And I should remain on the login page

  @regression @negative
  Scenario: Failed login with empty password
    When I login with invalid credentials "Admin" and ""
    Then I should see an error message
    And I should remain on the login page

  @regression @negative
  Scenario: Failed login with both empty credentials
    When I login with invalid credentials "" and ""
    Then I should see an error message
    And I should remain on the login page

  @functional
  Scenario: Verify login page elements
    Then the login form should be displayed
    And the page title should contain "OrangeHRM"
    When I take a screenshot
    And I verify the login page URL is correct

  @functional
  Scenario: Clear login form fields
    When I enter username "TestUser"
    And I enter password "TestPass"
    And I clear the username field
    And I clear the password field
    Then the login form should be displayed

  @accessibility
  Scenario: Login page accessibility check
    Then the login form should be displayed
    And I verify the login page URL is correct
    When I take a screenshot

  @data-driven
  Scenario Outline: Login with different invalid credentials
    When I login with invalid credentials "<username>" and "<password>"
    Then I should see an error message
    And I should remain on the login page
    
    Examples:
      | username     | password     | description                    |
      | admin        | wrong123     | Valid username, invalid password|
      | wronguser    | admin123     | Invalid username, valid password|
      | admin123     | wrongpass    | Both invalid credentials       |
      | test@test    | test123      | Email format username          |
      
  @performance
  Scenario: Login performance test
    Given I wait for 1 seconds
    When I login as an admin
    Then I should be successfully logged in
    And I should see the dashboard page

  @security
  Scenario: Multiple failed login attempts
    When I login with invalid credentials "admin" and "wrong1"
    Then I should see an error message
    When I login with invalid credentials "admin" and "wrong2"
    Then I should see an error message
    When I login with invalid credentials "admin" and "wrong3"
    Then I should see an error message
    And I should remain on the login page

  @edge-case
  Scenario: Login with special characters
    When I login with invalid credentials "admin@#$" and "pass!@#"
    Then I should see an error message
    And I should remain on the login page

  @integration
  Scenario: Full login workflow with validation
    Given I am on the login page
    When I take a screenshot
    And I verify the login page URL is correct
    And I login as an admin
    Then I should be successfully logged in
    And I should see the dashboard page
    When I take a screenshot