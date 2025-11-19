package com.phoenix.hrm.tests.dataproviders;

import com.phoenix.hrm.models.Employee;
import org.testng.annotations.DataProvider;

/**
 * TestDataProvider class providing data-driven testing support.
 * Supplies test data for various test scenarios including employee management.
 * 
 * Features:
 * - Employee data for CRUD operations testing
 * - Multiple data sets for comprehensive testing
 * - Builder pattern integration for readable data creation
 * - Support for positive and negative test scenarios
 * - Configurable data sets for different test environments
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 1.0
 */
public class TestDataProvider {
    
    /**
     * Provides employee data for adding multiple employees (Project Requirement)
     * Returns array of Employee objects for data-driven testing
     * 
     * @return Object[][] containing Employee data
     */
    @DataProvider(name = "employeeData")
    public static Object[][] getEmployeeData() {
        return new Object[][] {
            {
                Employee.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .jobTitle("Software Engineer")
                    .department("Engineering")
                    .phone("+1-555-0123")
                    .build()
            },
            {
                Employee.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane.smith@example.com")
                    .jobTitle("HR Manager")
                    .department("Human Resources")
                    .phone("+1-555-0124")
                    .build()
            },
            {
                Employee.builder()
                    .firstName("Robert")
                    .lastName("Johnson")
                    .email("robert.johnson@example.com")
                    .jobTitle("Project Manager")
                    .department("Operations")
                    .phone("+1-555-0125")
                    .build()
            },
            {
                Employee.builder()
                    .firstName("Emily")
                    .lastName("Davis")
                    .email("emily.davis@example.com")
                    .jobTitle("QA Engineer")
                    .department("Quality Assurance")
                    .phone("+1-555-0126")
                    .build()
            }
        };
    }
    
    /**
     * Provides basic employee data for simple testing scenarios
     * 
     * @return Object[][] containing basic Employee data
     */
    @DataProvider(name = "basicEmployeeData")
    public static Object[][] getBasicEmployeeData() {
        return new Object[][] {
            {
                Employee.builder()
                    .firstName("Alex")
                    .lastName("Wilson")
                    .build()
            },
            {
                Employee.builder()
                    .firstName("Sarah")
                    .lastName("Brown")
                    .build()
            }
        };
    }
    
    /**
     * Provides employee data with unique identifiers for update/delete testing
     * 
     * @return Object[][] containing Employee data with IDs
     */
    @DataProvider(name = "employeeDataWithIds")
    public static Object[][] getEmployeeDataWithIds() {
        return new Object[][] {
            {
                "EMP001",
                Employee.builder()
                    .employeeId("EMP001")
                    .firstName("Michael")
                    .lastName("Anderson")
                    .email("michael.anderson@example.com")
                    .jobTitle("Senior Developer")
                    .department("Engineering")
                    .build()
            },
            {
                "EMP002",
                Employee.builder()
                    .employeeId("EMP002")
                    .firstName("Lisa")
                    .lastName("Taylor")
                    .email("lisa.taylor@example.com")
                    .jobTitle("Business Analyst")
                    .department("Business")
                    .build()
            }
        };
    }
    
    /**
     * Provides employee search test data
     * 
     * @return Object[][] containing search criteria and expected results
     */
    @DataProvider(name = "employeeSearchData")
    public static Object[][] getEmployeeSearchData() {
        return new Object[][] {
            // {searchType, searchValue, expectedResult}
            {"name", "John Doe", true},
            {"name", "Jane Smith", true},
            {"name", "NonExistent User", false},
            {"id", "EMP001", true},
            {"id", "EMP999", false}
        };
    }
    
    /**
     * Provides invalid employee data for negative testing
     * 
     * @return Object[][] containing invalid Employee data
     */
    @DataProvider(name = "invalidEmployeeData")
    public static Object[][] getInvalidEmployeeData() {
        return new Object[][] {
            {
                // Empty first name
                Employee.builder()
                    .firstName("")
                    .lastName("TestUser")
                    .build()
            },
            {
                // Empty last name  
                Employee.builder()
                    .firstName("Test")
                    .lastName("")
                    .build()
            },
            {
                // Null first name (will fail validation)
                Employee.builder()
                    .firstName(null)
                    .lastName("TestUser")
                    .build()
            }
        };
    }
    
    /**
     * Provides login credentials for testing different user types
     * 
     * @return Object[][] containing username, password, and expected result
     */
    @DataProvider(name = "loginCredentials")
    public static Object[][] getLoginCredentials() {
        return new Object[][] {
            // {username, password, shouldSucceed}
            {"Admin", "admin123", true},
            {"invaliduser", "admin123", false},
            {"Admin", "wrongpassword", false},
            {"", "", false},
            {" ", " ", false}
        };
    }
    
    /**
     * Provides employee data for editing tests
     * 
     * @return Object[][] containing original and updated employee data
     */
    @DataProvider(name = "employeeEditData")
    public static Object[][] getEmployeeEditData() {
        return new Object[][] {
            {
                // Original employee
                Employee.builder()
                    .firstName("Original")
                    .lastName("Employee")
                    .email("original@example.com")
                    .build(),
                // Updated employee data
                Employee.builder()
                    .firstName("Updated")
                    .middleName("Middle")
                    .lastName("Employee")
                    .email("updated@example.com")
                    .jobTitle("Updated Title")
                    .build()
            },
            {
                // Another test case
                Employee.builder()
                    .firstName("TestFirst")
                    .lastName("TestLast")
                    .build(),
                Employee.builder()
                    .firstName("TestFirst")
                    .middleName("TestMiddle")
                    .lastName("TestLast")
                    .phone("+1-555-9999")
                    .build()
            }
        };
    }
    
    /**
     * Provides performance test data with larger datasets
     * 
     * @return Object[][] containing bulk employee data
     */
    @DataProvider(name = "bulkEmployeeData")
    public static Object[][] getBulkEmployeeData() {
        Object[][] data = new Object[10][1];
        
        for (int i = 0; i < 10; i++) {
            data[i][0] = Employee.builder()
                .firstName("BulkTest" + (i + 1))
                .lastName("Employee" + (i + 1))
                .email("bulktest" + (i + 1) + "@example.com")
                .jobTitle("Test Position " + (i + 1))
                .department("Test Department")
                .build();
        }
        
        return data;
    }
    
    /**
     * Provides cross-browser testing data
     * 
     * @return Object[][] containing browser configurations
     */
    @DataProvider(name = "crossBrowserData")
    public static Object[][] getCrossBrowserData() {
        return new Object[][] {
            {"chrome"},
            {"firefox"},
            {"edge"}
        };
    }
    
    /**
     * Provides test data for API testing
     * 
     * @return Object[][] containing API test scenarios
     */
    @DataProvider(name = "apiEmployeeData")
    public static Object[][] getApiEmployeeData() {
        return new Object[][] {
            {
                Employee.builder()
                    .firstName("API")
                    .lastName("TestUser1")
                    .email("api.test1@example.com")
                    .salary("50000")
                    .build()
            },
            {
                Employee.builder()
                    .firstName("API")
                    .lastName("TestUser2")
                    .email("api.test2@example.com")
                    .salary("60000")
                    .build()
            }
        };
    }
    
    /**
     * Provides employee department data for filtering tests
     * 
     * @return Object[][] containing department filter data
     */
    @DataProvider(name = "departmentFilterData")
    public static Object[][] getDepartmentFilterData() {
        return new Object[][] {
            {"Engineering"},
            {"Human Resources"},
            {"Operations"},
            {"Quality Assurance"},
            {"Finance"},
            {"Marketing"}
        };
    }
    
    /**
     * Provides random employee data generator
     * Used for stress testing and large data set scenarios
     * 
     * @return Object[][] containing randomly generated employee data
     */
    @DataProvider(name = "randomEmployeeData")
    public static Object[][] getRandomEmployeeData() {
        String[] firstNames = {"James", "Mary", "John", "Patricia", "Robert", "Jennifer", 
                              "Michael", "Linda", "William", "Elizabeth"};
        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", 
                             "Miller", "Davis", "Rodriguez", "Martinez"};
        String[] departments = {"Engineering", "HR", "Finance", "Marketing", "Operations"};
        String[] jobTitles = {"Manager", "Developer", "Analyst", "Coordinator", "Specialist"};
        
        Object[][] data = new Object[5][1];
        
        for (int i = 0; i < 5; i++) {
            String firstName = firstNames[i % firstNames.length];
            String lastName = lastNames[i % lastNames.length];
            String department = departments[i % departments.length];
            String jobTitle = jobTitles[i % jobTitles.length];
            
            data[i][0] = Employee.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com")
                .department(department)
                .jobTitle(jobTitle)
                .phone("+1-555-" + String.format("%04d", 1000 + i))
                .build();
        }
        
        return data;
    }
}