package com.phoenix.hrm.dataproviders;

import org.testng.annotations.DataProvider;

/**
 * Test Data Providers for Phoenix HRM Test Framework
 * 
 * Centralized location for all test data providers used across different test classes
 * Includes data for UI tests, API tests, and cross-browser testing
 * 
 * @author Phoenix HRM Test Team
 */
public class TestDataProviders {
    
    /**
     * Provides invalid employee data for API negative testing
     */
    @DataProvider(name = "invalidEmployeeData")
    public static Object[][] getInvalidEmployeeData() {
        return new Object[][]{
            {"", "50000", "30", "Empty name should be rejected"},
            {null, "50000", "30", "Null name should be rejected"},
            {"John Doe", "", "30", "Empty salary should be rejected"},
            {"John Doe", "50000", "", "Empty age should be rejected"},
            {"John Doe", "invalid_salary", "30", "Invalid salary format should be rejected"},
            {"John Doe", "50000", "invalid_age", "Invalid age format should be rejected"},
            {"John Doe", "-50000", "30", "Negative salary should be rejected"},
            {"John Doe", "50000", "-30", "Negative age should be rejected"},
            {"John Doe", "50000", "200", "Unrealistic age should be rejected"},
            {generateLongString(200), "50000", "30", "Very long name should be rejected"}
        };
    }
    
    /**
     * Provides valid employee data for API positive testing
     */
    @DataProvider(name = "validEmployeeData")
    public static Object[][] getValidEmployeeData() {
        return new Object[][]{
            {"John Smith", "50000", "30", "Software Engineer", "IT"},
            {"Jane Doe", "60000", "28", "Senior Developer", "Engineering"},
            {"Mike Johnson", "45000", "25", "QA Engineer", "Quality Assurance"},
            {"Sarah Wilson", "70000", "35", "Product Manager", "Product"},
            {"David Brown", "55000", "32", "DevOps Engineer", "Operations"}
        };
    }
    
    /**
     * Provides cross-browser test data for parallel execution
     */
    @DataProvider(name = "crossBrowserData", parallel = true)
    public static Object[][] getCrossBrowserData() {
        return new Object[][]{
            {"chrome", "Test User Chrome"},
            {"firefox", "Test User Firefox"},
            {"edge", "Test User Edge"}
        };
    }
    
    /**
     * Provides login credentials for different scenarios
     */
    @DataProvider(name = "loginCredentials")
    public static Object[][] getLoginCredentials() {
        return new Object[][]{
            {"Admin", "admin123", true, "Valid admin credentials"},
            {"admin", "admin123", false, "Case sensitive username"},
            {"Admin", "Admin123", false, "Case sensitive password"},
            {"Admin", "wrongpass", false, "Invalid password"},
            {"WrongUser", "admin123", false, "Invalid username"},
            {"", "admin123", false, "Empty username"},
            {"Admin", "", false, "Empty password"}
        };
    }
    
    /**
     * Provides employee search criteria for UI testing
     */
    @DataProvider(name = "employeeSearchData")
    public static Object[][] getEmployeeSearchData() {
        return new Object[][]{
            {"firstName", "John", "Search by first name"},
            {"lastName", "Smith", "Search by last name"},
            {"employeeId", "EMP001", "Search by employee ID"},
            {"jobTitle", "Engineer", "Search by job title"},
            {"department", "IT", "Search by department"}
        };
    }
    
    /**
     * Provides performance test data with different load scenarios
     */
    @DataProvider(name = "performanceTestData")
    public static Object[][] getPerformanceTestData() {
        return new Object[][]{
            {5, 2000, "Light load - 5 requests in 2 seconds"},
            {10, 5000, "Medium load - 10 requests in 5 seconds"},
            {25, 10000, "Heavy load - 25 requests in 10 seconds"}
        };
    }
    
    /**
     * Provides API endpoint data for testing different endpoints
     */
    @DataProvider(name = "apiEndpoints")
    public static Object[][] getApiEndpoints() {
        return new Object[][]{
            {"/employees", "GET", "Get all employees endpoint"},
            {"/employee/1", "GET", "Get specific employee endpoint"},
            {"/create", "POST", "Create employee endpoint"},
            {"/update/1", "PUT", "Update employee endpoint"},
            {"/delete/1", "DELETE", "Delete employee endpoint"}
        };
    }
    
    /**
     * Provides boundary value test data for API testing
     */
    @DataProvider(name = "boundaryValueData")
    public static Object[][] getBoundaryValueData() {
        return new Object[][]{
            {"A", "1", "18", "Minimum valid values"},
            {generateString(50), "999999", "65", "Normal maximum values"},
            {generateString(100), "9999999", "99", "Extended maximum values"},
            {"Test Employee", "0", "18", "Zero salary boundary"},
            {"Test Employee", "100", "100", "Age boundary values"}
        };
    }
    
    /**
     * Provides security test data for API testing
     */
    @DataProvider(name = "securityTestData")
    public static Object[][] getSecurityTestData() {
        return new Object[][]{
            {"<script>alert('xss')</script>", "50000", "30", "XSS injection test"},
            {"'; DROP TABLE employees; --", "50000", "30", "SQL injection test"},
            {"../../../etc/passwd", "50000", "30", "Path traversal test"},
            {"John\nDoe", "50000", "30", "Line break injection"},
            {"John\tDoe", "50000", "30", "Tab injection"}
        };
    }
    
    /**
     * Provides test data for different content types
     */
    @DataProvider(name = "contentTypeData")
    public static Object[][] getContentTypeData() {
        return new Object[][]{
            {"application/json", "JSON content type"},
            {"application/xml", "XML content type"},
            {"text/plain", "Plain text content type"},
            {"application/x-www-form-urlencoded", "Form encoded content type"}
        };
    }
    
    // ===================== UTILITY METHODS =====================
    
    /**
     * Generates a string of specified length for boundary testing
     */
    private static String generateString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append((char) ('A' + (i % 26)));
        }
        return sb.toString();
    }
    
    /**
     * Generates a very long string for testing limits
     */
    private static String generateLongString(int length) {
        StringBuilder sb = new StringBuilder();
        String pattern = "TestDataPattern";
        for (int i = 0; i < length; i++) {
            sb.append(pattern.charAt(i % pattern.length()));
        }
        return sb.toString();
    }
    
    /**
     * Generates random employee data for load testing
     */
    public static Object[][] generateRandomEmployeeData(int count) {
        Object[][] data = new Object[count][5];
        String[] firstNames = {"John", "Jane", "Mike", "Sarah", "David", "Lisa", "Tom", "Emma", "Alex", "Maria"};
        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez"};
        String[] departments = {"IT", "HR", "Finance", "Marketing", "Operations", "Sales", "Engineering", "Support"};
        String[] jobTitles = {"Engineer", "Manager", "Analyst", "Specialist", "Coordinator", "Lead", "Director", "Associate"};
        
        for (int i = 0; i < count; i++) {
            String firstName = firstNames[i % firstNames.length];
            String lastName = lastNames[i % lastNames.length];
            String salary = String.valueOf(30000 + (i * 1000));
            String age = String.valueOf(25 + (i % 35));
            String department = departments[i % departments.length];
            
            data[i] = new Object[]{
                firstName + " " + lastName + " " + i,
                salary,
                age,
                jobTitles[i % jobTitles.length],
                department
            };
        }
        
        return data;
    }
}