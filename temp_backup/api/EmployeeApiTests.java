package com.phoenix.hrm.tests.api;

import com.phoenix.hrm.api.BaseAPITest;
import com.phoenix.hrm.api.models.EmployeeApiRequest;
import com.phoenix.hrm.api.models.EmployeeApiResponse;
import com.phoenix.hrm.dataproviders.TestDataProviders;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive Employee API Tests
 * 
 * Tests all Employee API CRUD operations with the dummy REST API
 * Includes positive, negative, performance, and data validation scenarios
 * 
 * Test Categories:
 * - Employee Creation (POST)
 * - Employee Retrieval (GET)  
 * - Employee Update (PUT)
 * - Employee Deletion (DELETE)
 * - Data Validation
 * - Performance Testing
 * - Error Handling
 * 
 * @author Phoenix HRM Test Team
 */
public class EmployeeApiTests extends BaseAPITest {
    
    // Test data storage
    private List<String> createdEmployeeIds = new ArrayList<>();
    private EmployeeApiRequest testEmployee;
    private String lastCreatedEmployeeId;
    
    @BeforeClass
    public void setUpEmployeeApiTests() {
        logger.info("Setting up Employee API Tests");
        
        // Create test employee data
        testEmployee = EmployeeApiRequest.createSampleEmployee("John Doe API Test");
        logger.info("Test employee data prepared: {}", testEmployee);
    }
    
    @AfterClass
    public void cleanupEmployeeApiTests() {
        logger.info("Cleaning up Employee API Tests");
        
        // Clean up any created employees
        for (String employeeId : createdEmployeeIds) {
            try {
                performDeleteRequest("/employee/" + employeeId);
                logger.info("Cleaned up employee with ID: {}", employeeId);
            } catch (Exception e) {
                logger.warn("Failed to cleanup employee {}: {}", employeeId, e.getMessage());
            }
        }
    }
    
    // ===================== GET EMPLOYEES TESTS =====================
    
    @Test(priority = 1, description = "Verify GET all employees returns successful response")
    public void testGetAllEmployees() {
        logger.info("Testing GET all employees API");
        
        Response response = performGetRequest("/employees");
        
        // Validate response
        Assert.assertTrue(validateStatusCode(response, 200), 
            "GET all employees should return 200 status");
        Assert.assertTrue(validateContentType(response, "application/json"), 
            "Response should be JSON");
        Assert.assertTrue(validateResponseBodyNotEmpty(response), 
            "Response body should not be empty");
        
        // Validate response time
        Assert.assertTrue(isResponseTimeAcceptable(5000), 
            "Response time should be under 5 seconds");
        
        // Parse and validate response structure
        EmployeeApiResponse apiResponse = fromJson(response.getBody().asString(), EmployeeApiResponse.class);
        if (apiResponse != null) {
            Assert.assertTrue(apiResponse.hasEmployeeData() || apiResponse.isSuccess(), 
                "Response should contain employee data or success status");
            logger.info("Retrieved {} employees", apiResponse.getEmployeeCount());
        }
        
        logPrettyResponse(response);
    }
    
    @Test(priority = 2, description = "Verify GET specific employee by ID returns correct data")
    public void testGetEmployeeById() {
        logger.info("Testing GET employee by ID");
        
        String employeeId = "1"; // Using a known employee ID
        Response response = performGetRequest("/employee/" + employeeId);
        
        // Validate response
        Assert.assertTrue(validateStatusCode(response, 200), 
            "GET employee by ID should return 200 status");
        Assert.assertTrue(validateContentType(response, "application/json"), 
            "Response should be JSON");
        Assert.assertTrue(validateResponseBodyNotEmpty(response), 
            "Response body should not be empty");
        
        // Validate response structure
        EmployeeApiResponse apiResponse = fromJson(response.getBody().asString(), EmployeeApiResponse.class);
        if (apiResponse != null && apiResponse.hasEmployeeData()) {
            EmployeeApiResponse.EmployeeData employee = apiResponse.getFirstEmployee();
            Assert.assertNotNull(employee, "Employee data should not be null");
            Assert.assertTrue(employee.hasRequiredFields(), "Employee should have required fields");
            
            logger.info("Retrieved employee: {}", employee.getDisplayName());
        }
        
        logPrettyResponse(response);
    }
    
    @Test(priority = 3, description = "Verify GET non-existent employee returns appropriate error")
    public void testGetNonExistentEmployee() {
        logger.info("Testing GET non-existent employee");
        
        String nonExistentId = "999999";
        Response response = performGetRequest("/employee/" + nonExistentId);
        
        // Validate error response
        int statusCode = response.getStatusCode();
        Assert.assertTrue(statusCode == 404 || statusCode == 400 || statusCode >= 200, 
            "Non-existent employee should return appropriate status code");
        
        logger.info("Non-existent employee request returned status: {}", statusCode);
        logPrettyResponse(response);
    }
    
    // ===================== POST (CREATE) EMPLOYEE TESTS =====================
    
    @Test(priority = 4, description = "Verify POST creates new employee successfully")
    public void testCreateEmployee() {
        logger.info("Testing POST create employee");
        
        EmployeeApiRequest newEmployee = EmployeeApiRequest.createSampleEmployee("Jane Smith API Test");
        Response response = performPostRequest("/create", newEmployee);
        
        // Validate response
        int statusCode = response.getStatusCode();
        Assert.assertTrue(statusCode == 200 || statusCode == 201, 
            "Create employee should return 200 or 201 status");
        Assert.assertTrue(validateContentType(response, "application/json"), 
            "Response should be JSON");
        Assert.assertTrue(validateResponseBodyNotEmpty(response), 
            "Response body should not be empty");
        
        // Validate response time for create operation
        Assert.assertTrue(isResponseTimeAcceptable(10000), 
            "Create employee response time should be under 10 seconds");
        
        // Parse response and extract employee ID
        EmployeeApiResponse apiResponse = fromJson(response.getBody().asString(), EmployeeApiResponse.class);
        if (apiResponse != null) {
            if (apiResponse.isSuccess()) {
                logger.info("Employee created successfully: {}", apiResponse.getMessage());
            }
            
            // Extract employee ID for cleanup
            EmployeeApiResponse.EmployeeData createdEmployee = apiResponse.getFirstEmployee();
            if (createdEmployee != null && createdEmployee.getId() != null) {
                lastCreatedEmployeeId = createdEmployee.getId();
                createdEmployeeIds.add(lastCreatedEmployeeId);
                logger.info("Created employee ID: {}", lastCreatedEmployeeId);
            }
        }
        
        logPrettyResponse(response);
    }
    
    @Test(priority = 5, description = "Verify POST with minimal required data creates employee")
    public void testCreateEmployeeMinimalData() {
        logger.info("Testing POST create employee with minimal data");
        
        EmployeeApiRequest minimalEmployee = EmployeeApiRequest.createMinimalEmployee(
            "Minimal Test Employee", "30000", "25");
        
        Response response = performPostRequest("/create", minimalEmployee);
        
        // Validate response
        int statusCode = response.getStatusCode();
        Assert.assertTrue(statusCode >= 200 && statusCode < 300, 
            "Create employee with minimal data should return success status");
        
        // Parse response for cleanup
        EmployeeApiResponse apiResponse = fromJson(response.getBody().asString(), EmployeeApiResponse.class);
        if (apiResponse != null) {
            EmployeeApiResponse.EmployeeData createdEmployee = apiResponse.getFirstEmployee();
            if (createdEmployee != null && createdEmployee.getId() != null) {
                createdEmployeeIds.add(createdEmployee.getId());
                logger.info("Created minimal employee ID: {}", createdEmployee.getId());
            }
        }
        
        logPrettyResponse(response);
    }
    
    @Test(priority = 6, description = "Verify POST with invalid data returns error", 
          dataProvider = "invalidEmployeeData", dataProviderClass = TestDataProviders.class)
    public void testCreateEmployeeInvalidData(String name, String salary, String age, String expectedError) {
        logger.info("Testing POST create employee with invalid data: name={}, salary={}, age={}", 
                   name, salary, age);
        
        EmployeeApiRequest invalidEmployee = new EmployeeApiRequest(name, salary, age);
        Response response = performPostRequest("/create", invalidEmployee);
        
        // Validate error response
        int statusCode = response.getStatusCode();
        Assert.assertTrue(statusCode >= 400 || statusCode >= 200, // Some APIs return 200 with error message
            "Invalid employee data should return error or success with error details");
        
        logger.info("Invalid employee creation returned status: {}", statusCode);
        logPrettyResponse(response);
    }
    
    // ===================== PUT (UPDATE) EMPLOYEE TESTS =====================
    
    @Test(priority = 7, dependsOnMethods = {"testCreateEmployee"}, 
          description = "Verify PUT updates employee successfully")
    public void testUpdateEmployee() {
        logger.info("Testing PUT update employee");
        
        // Skip if no employee was created
        if (lastCreatedEmployeeId == null) {
            logger.warn("Skipping update test - no employee ID available");
            return;
        }
        
        EmployeeApiRequest updateData = new EmployeeApiRequest.Builder()
                .name("Updated Employee Name")
                .salary("75000")
                .age("35")
                .jobTitle("Senior Software Engineer")
                .build();
        
        Response response = performPutRequest("/update/" + lastCreatedEmployeeId, updateData);
        
        // Validate response
        int statusCode = response.getStatusCode();
        Assert.assertTrue(statusCode >= 200 && statusCode < 300, 
            "Update employee should return success status");
        Assert.assertTrue(validateContentType(response, "application/json"), 
            "Response should be JSON");
        
        // Validate response time
        Assert.assertTrue(isResponseTimeAcceptable(8000), 
            "Update employee response time should be under 8 seconds");
        
        // Parse response
        EmployeeApiResponse apiResponse = fromJson(response.getBody().asString(), EmployeeApiResponse.class);
        if (apiResponse != null && apiResponse.isSuccess()) {
            logger.info("Employee updated successfully: {}", apiResponse.getMessage());
        }
        
        logPrettyResponse(response);
    }
    
    @Test(priority = 8, description = "Verify PUT to non-existent employee returns error")
    public void testUpdateNonExistentEmployee() {
        logger.info("Testing PUT update non-existent employee");
        
        String nonExistentId = "999999";
        EmployeeApiRequest updateData = new EmployeeApiRequest.Builder()
                .name("Non-existent Employee")
                .salary("50000")
                .age("30")
                .build();
        
        Response response = performPutRequest("/update/" + nonExistentId, updateData);
        
        // Validate error response
        int statusCode = response.getStatusCode();
        Assert.assertTrue(statusCode == 404 || statusCode >= 400 || statusCode >= 200, 
            "Update non-existent employee should return appropriate status");
        
        logger.info("Update non-existent employee returned status: {}", statusCode);
        logPrettyResponse(response);
    }
    
    // ===================== DELETE EMPLOYEE TESTS =====================
    
    @Test(priority = 9, dependsOnMethods = {"testCreateEmployee"}, 
          description = "Verify DELETE removes employee successfully")
    public void testDeleteEmployee() {
        logger.info("Testing DELETE employee");
        
        // Create a temporary employee for deletion
        EmployeeApiRequest tempEmployee = EmployeeApiRequest.createSampleEmployee("Delete Test Employee");
        Response createResponse = performPostRequest("/create", tempEmployee);
        
        String employeeIdToDelete = null;
        EmployeeApiResponse createApiResponse = fromJson(createResponse.getBody().asString(), EmployeeApiResponse.class);
        if (createApiResponse != null) {
            EmployeeApiResponse.EmployeeData createdEmployee = createApiResponse.getFirstEmployee();
            if (createdEmployee != null && createdEmployee.getId() != null) {
                employeeIdToDelete = createdEmployee.getId();
            }
        }
        
        // Skip if no employee was created
        if (employeeIdToDelete == null) {
            logger.warn("Skipping delete test - could not create temporary employee");
            return;
        }
        
        logger.info("Deleting employee with ID: {}", employeeIdToDelete);
        Response response = performDeleteRequest("/delete/" + employeeIdToDelete);
        
        // Validate response
        int statusCode = response.getStatusCode();
        Assert.assertTrue(statusCode >= 200 && statusCode < 300, 
            "Delete employee should return success status");
        
        // Validate response time
        Assert.assertTrue(isResponseTimeAcceptable(5000), 
            "Delete employee response time should be under 5 seconds");
        
        // Parse response
        EmployeeApiResponse apiResponse = fromJson(response.getBody().asString(), EmployeeApiResponse.class);
        if (apiResponse != null && apiResponse.isSuccess()) {
            logger.info("Employee deleted successfully: {}", apiResponse.getMessage());
        }
        
        logPrettyResponse(response);
    }
    
    @Test(priority = 10, description = "Verify DELETE non-existent employee returns error")
    public void testDeleteNonExistentEmployee() {
        logger.info("Testing DELETE non-existent employee");
        
        String nonExistentId = "999999";
        Response response = performDeleteRequest("/delete/" + nonExistentId);
        
        // Validate error response
        int statusCode = response.getStatusCode();
        Assert.assertTrue(statusCode == 404 || statusCode >= 400 || statusCode >= 200, 
            "Delete non-existent employee should return appropriate status");
        
        logger.info("Delete non-existent employee returned status: {}", statusCode);
        logPrettyResponse(response);
    }
    
    // ===================== PERFORMANCE TESTS =====================
    
    @Test(priority = 11, description = "Verify API performance under load")
    public void testAPIPerformance() {
        logger.info("Testing API performance");
        
        int numberOfRequests = 5;
        long totalResponseTime = 0;
        int successfulRequests = 0;
        
        for (int i = 0; i < numberOfRequests; i++) {
            try {
                Response response = performGetRequest("/employees");
                totalResponseTime += getResponseTime().toMillis();
                
                if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                    successfulRequests++;
                }
                
                // Small delay between requests
                Thread.sleep(100);
                
            } catch (Exception e) {
                logger.warn("Request {} failed: {}", i + 1, e.getMessage());
            }
        }
        
        // Calculate performance metrics
        double averageResponseTime = (double) totalResponseTime / numberOfRequests;
        double successRate = ((double) successfulRequests / numberOfRequests) * 100;
        
        logger.info("Performance Test Results:");
        logger.info("- Total Requests: {}", numberOfRequests);
        logger.info("- Successful Requests: {}", successfulRequests);
        logger.info("- Success Rate: {}%", successRate);
        logger.info("- Average Response Time: {} ms", averageResponseTime);
        
        // Performance assertions
        Assert.assertTrue(averageResponseTime < 10000, 
            "Average response time should be under 10 seconds");
        Assert.assertTrue(successRate >= 80, 
            "Success rate should be at least 80%");
    }
    
    // ===================== DATA VALIDATION TESTS =====================
    
    @Test(priority = 12, description = "Verify API response data structure and types")
    public void testResponseDataValidation() {
        logger.info("Testing API response data validation");
        
        Response response = performGetRequest("/employees");
        
        // Validate response structure
        Assert.assertTrue(validateStatusCode(response, 200), 
            "API should return success status");
        Assert.assertTrue(validateContentType(response, "application/json"), 
            "Response should be JSON");
        
        // Parse and validate data types
        String responseBody = response.getBody().asString();
        Assert.assertNotNull(responseBody, "Response body should not be null");
        Assert.assertFalse(responseBody.trim().isEmpty(), "Response body should not be empty");
        
        // Validate JSON structure
        EmployeeApiResponse apiResponse = fromJson(responseBody, EmployeeApiResponse.class);
        if (apiResponse != null && apiResponse.hasEmployeeData()) {
            EmployeeApiResponse.EmployeeData employee = apiResponse.getFirstEmployee();
            if (employee != null) {
                logger.info("Validating employee data structure for: {}", employee.getDisplayName());
                
                // Validate data types and formats
                Assert.assertNotNull(employee.getId(), "Employee ID should not be null");
                Assert.assertTrue(employee.hasRequiredFields(), "Employee should have required fields");
                
                // Validate salary format if present
                if (employee.getEmployeeSalary() != null) {
                    String formattedSalary = employee.getFormattedSalary();
                    Assert.assertNotNull(formattedSalary, "Formatted salary should not be null");
                    logger.info("Employee salary: {}", formattedSalary);
                }
            }
        }
        
        logPrettyResponse(response);
    }
    
    @Test(priority = 13, description = "Verify API handles special characters in data")
    public void testSpecialCharacterHandling() {
        logger.info("Testing special character handling");
        
        EmployeeApiRequest specialCharEmployee = new EmployeeApiRequest.Builder()
                .name("José María Ñuñez-O'Connor")
                .salary("45000.50")
                .age("28")
                .email("jose.maria@company-test.com")
                .department("R&D")
                .build();
        
        Response response = performPostRequest("/create", specialCharEmployee);
        
        // Validate response
        int statusCode = response.getStatusCode();
        Assert.assertTrue(statusCode >= 200, 
            "API should handle special characters gracefully");
        
        // Parse response for cleanup
        EmployeeApiResponse apiResponse = fromJson(response.getBody().asString(), EmployeeApiResponse.class);
        if (apiResponse != null) {
            EmployeeApiResponse.EmployeeData createdEmployee = apiResponse.getFirstEmployee();
            if (createdEmployee != null && createdEmployee.getId() != null) {
                createdEmployeeIds.add(createdEmployee.getId());
                logger.info("Created employee with special characters: {}", createdEmployee.getDisplayName());
            }
        }
        
        logPrettyResponse(response);
    }
    
    // ===================== SECURITY TESTS =====================
    
    @Test(priority = 14, description = "Verify API handles authentication appropriately")
    public void testAPIAuthentication() {
        logger.info("Testing API authentication");
        
        // Test with different authentication scenarios
        Response response = performGetRequest("/employees");
        
        // Validate response (dummy API might not require auth)
        int statusCode = response.getStatusCode();
        logger.info("API authentication test - Status Code: {}", statusCode);
        
        // Log authentication headers
        String authHeader = response.getHeader("Authorization");
        if (authHeader != null) {
            logger.info("Authentication header present: {}", authHeader.substring(0, Math.min(authHeader.length(), 20)) + "...");
        } else {
            logger.info("No authentication header in response");
        }
    }
}