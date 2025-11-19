package com.phoenix.hrm.tests.api;

import com.phoenix.hrm.core.api.ApiClient;
import com.phoenix.hrm.core.api.ApiResponseValidator;
import com.phoenix.hrm.core.config.ConfigManager;
import com.phoenix.hrm.core.data.TestDataManager;
import com.phoenix.hrm.core.reporting.TestReporter;
import io.restassured.response.Response;
import org.testng.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Advanced API Tests demonstrating the new API testing framework
 * Tests JSONPlaceholder API endpoints with comprehensive validation
 */
public class AdvancedApiTest {
    
    private static final Logger logger = LoggerFactory.getLogger(AdvancedApiTest.class);
    
    private ApiClient apiClient;
    private ConfigManager config;
    private TestDataManager testDataManager;
    
    @BeforeSuite
    public void setUpSuite() {
        logger.info("Setting up Advanced API Test Suite");
        TestReporter.initializeReports();
        
        config = ConfigManager.getInstance();
        testDataManager = TestDataManager.getInstance();
        
        logger.info("API Test Suite setup completed. Base URL: {}", config.getApiBaseUrl());
    }
    
    @BeforeClass
    public void setUpClass() {
        logger.info("Setting up Advanced API Test class");
        
        // Initialize API client with JSONPlaceholder API
        apiClient = ApiClient.getInstance()
            .setBaseUri("https://jsonplaceholder.typicode.com")
            .setBasePath("")
            .addHeader("Custom-Test-Header", "Phoenix-HRM-API-Tests");
    }
    
    @BeforeMethod
    public void setUp() {
        logger.info("Setting up API test method");
    }
    
    @Test(priority = 1, description = "Test GET /posts endpoint with comprehensive validation")
    public void testGetAllPosts() {
        TestReporter.startTest("Get All Posts API Test", "Validate GET /posts endpoint with comprehensive checks");
        TestReporter.setCategory("API", "GET", "Posts", "Smoke");
        TestReporter.setAuthor("API Test Team");
        
        try {
            TestReporter.logInfo("Starting GET /posts API test");
            
            // Execute GET request
            Response response = apiClient.get("/posts");
            
            // Comprehensive response validation
            ApiResponseValidator.of(response)
                .logResponseSummary()
                .validateSuccessStatusCode()
                .validateJsonContentType()
                .validateResponseTime(3000L)
                .validateBodyNotEmpty()
                .validateJsonPathArrayNotEmpty("$")
                .validateJsonPathArraySize("$", 100)
                .validateJsonPathExists("[0].id")
                .validateJsonPathExists("[0].userId")
                .validateJsonPathExists("[0].title")
                .validateJsonPathExists("[0].body")
                .validateCustom("First post has valid ID", 
                    r -> r.jsonPath().getInt("[0].id") > 0)
                .validateCustom("All posts have required fields",
                    r -> r.jsonPath().getList("findAll { it.id != null && it.userId != null && it.title != null && it.body != null }").size() == 100);
            
            TestReporter.logPass("GET /posts API test passed with comprehensive validation");
            
            // Extract sample data for reporting
            Integer firstPostId = apiClient.extractJsonPath(response, "[0].id");
            String firstPostTitle = apiClient.extractJsonPath(response, "[0].title");
            
            String[][] apiDataTable = {
                {"Field", "Value", "Validation"},
                {"Status Code", String.valueOf(response.getStatusCode()), "200 (Success)"},
                {"Response Time", response.getTime() + " ms", "< 3000 ms"},
                {"Total Posts", "100", "Expected count"},
                {"First Post ID", String.valueOf(firstPostId), "Valid ID"},
                {"First Post Title", firstPostTitle, "Not Empty"}
            };
            
            TestReporter.addTable("API Response Analysis", apiDataTable);
            
        } catch (Exception e) {
            TestReporter.logFail("GET /posts API test failed: " + e.getMessage());
            TestReporter.logException(e);
            throw e;
        } finally {
            TestReporter.endTest();
        }
    }
    
    @Test(priority = 2, description = "Test GET /posts/{id} endpoint with path parameters")
    public void testGetSinglePost() {
        TestReporter.startTest("Get Single Post API Test", "Validate GET /posts/{id} with path parameters");
        TestReporter.setCategory("API", "GET", "Posts", "Functional");
        TestReporter.setAuthor("API Test Team");
        
        try {
            TestReporter.logInfo("Starting GET /posts/{id} API test");
            
            int postId = 1;
            Map<String, Object> pathParams = new HashMap<>();
            pathParams.put("id", postId);
            
            // Execute GET request with path parameters
            Response response = apiClient.get("/posts/{id}", pathParams);
            
            // Validate single post response
            ApiResponseValidator.of(response)
                .validateStatusCode(200)
                .validateJsonContentType()
                .validateResponseTime(2000L)
                .validateJsonPathValue("id", postId)
                .validateJsonPathValue("userId", 1)
                .validateJsonPathExists("title")
                .validateJsonPathExists("body")
                .validateRequiredFields("id", "userId", "title", "body")
                .validateCustom("Post ID matches request", 
                    r -> r.jsonPath().getInt("id") == postId);
            
            // JSON structure validation
            Map<String, Class<?>> expectedStructure = new HashMap<>();
            expectedStructure.put("id", Integer.class);
            expectedStructure.put("userId", Integer.class);
            expectedStructure.put("title", String.class);
            expectedStructure.put("body", String.class);
            
            ApiResponseValidator.of(response)
                .validateJsonStructure(expectedStructure);
            
            TestReporter.logPass("GET /posts/{id} API test passed with structure validation");
            
            // Log post details
            String title = apiClient.extractJsonPath(response, "title");
            String body = apiClient.extractJsonPath(response, "body");
            
            TestReporter.logInfo("Post Details - Title: " + title);
            TestReporter.logInfo("Post Details - Body: " + body.substring(0, Math.min(50, body.length())) + "...");
            
        } catch (Exception e) {
            TestReporter.logFail("GET /posts/{id} API test failed: " + e.getMessage());
            TestReporter.logException(e);
            throw e;
        } finally {
            TestReporter.endTest();
        }
    }
    
    @Test(priority = 3, description = "Test POST /posts endpoint with request body")
    public void testCreatePost() {
        TestReporter.startTest("Create Post API Test", "Validate POST /posts with JSON request body");
        TestReporter.setCategory("API", "POST", "Posts", "CRUD");
        TestReporter.setAuthor("API Test Team");
        
        try {
            TestReporter.logInfo("Starting POST /posts API test");
            
            // Create test data
            Map<String, Object> newPost = new HashMap<>();
            newPost.put("title", "Phoenix HRM Test Post - " + testDataManager.getCurrentTimestamp());
            newPost.put("body", "This is a test post created by Phoenix HRM automation framework at " + 
                        testDataManager.generateDateString("yyyy-MM-dd HH:mm:ss"));
            newPost.put("userId", 1);
            
            TestReporter.addJsonData("Request Payload", testDataManager.replacePlaceholders(
                "{\n  \"title\": \"${random.string.10} Test Post\",\n  \"body\": \"Test body created at ${datetime.now}\",\n  \"userId\": 1\n}"));
            
            // Execute POST request
            Response response = apiClient.post("/posts", newPost);
            
            // Validate creation response
            ApiResponseValidator.of(response)
                .validateStatusCode(201)
                .validateJsonContentType()
                .validateResponseTime(3000L)
                .validateJsonPathExists("id")
                .validateJsonPathValue("title", newPost.get("title"))
                .validateJsonPathValue("body", newPost.get("body"))
                .validateJsonPathValue("userId", newPost.get("userId"))
                .validateJsonPathGreaterThan("id", 0)
                .validateCustom("Created post has all required fields",
                    r -> r.jsonPath().get("id") != null && 
                         r.jsonPath().get("title").equals(newPost.get("title")));
            
            TestReporter.logPass("POST /posts API test passed - Post created successfully");
            
            // Extract created post ID
            Integer createdPostId = apiClient.extractJsonPath(response, "id");
            TestReporter.logInfo("Created post with ID: " + createdPostId);
            
        } catch (Exception e) {
            TestReporter.logFail("POST /posts API test failed: " + e.getMessage());
            TestReporter.logException(e);
            throw e;
        } finally {
            TestReporter.endTest();
        }
    }
    
    @Test(priority = 4, description = "Test PUT /posts/{id} endpoint for updating")
    public void testUpdatePost() {
        TestReporter.startTest("Update Post API Test", "Validate PUT /posts/{id} for updating existing post");
        TestReporter.setCategory("API", "PUT", "Posts", "CRUD");
        TestReporter.setAuthor("API Test Team");
        
        try {
            TestReporter.logInfo("Starting PUT /posts/{id} API test");
            
            int postId = 1;
            
            // Create updated post data
            Map<String, Object> updatedPost = new HashMap<>();
            updatedPost.put("id", postId);
            updatedPost.put("title", "Updated Phoenix HRM Test Post - " + testDataManager.getCurrentTimestamp());
            updatedPost.put("body", "This post was updated by Phoenix HRM automation framework");
            updatedPost.put("userId", 1);
            
            Map<String, Object> pathParams = new HashMap<>();
            pathParams.put("id", postId);
            
            // Execute PUT request
            Response response = apiClient.put("/posts/{id}", updatedPost, pathParams);
            
            // Validate update response
            ApiResponseValidator.of(response)
                .validateStatusCode(200)
                .validateJsonContentType()
                .validateResponseTime(3000L)
                .validateJsonPathValue("id", postId)
                .validateJsonPathValue("title", updatedPost.get("title"))
                .validateJsonPathValue("body", updatedPost.get("body"))
                .validateJsonPathValue("userId", updatedPost.get("userId"));
            
            TestReporter.logPass("PUT /posts/{id} API test passed - Post updated successfully");
            
        } catch (Exception e) {
            TestReporter.logFail("PUT /posts/{id} API test failed: " + e.getMessage());
            TestReporter.logException(e);
            throw e;
        } finally {
            TestReporter.endTest();
        }
    }
    
    @Test(priority = 5, description = "Test DELETE /posts/{id} endpoint")
    public void testDeletePost() {
        TestReporter.startTest("Delete Post API Test", "Validate DELETE /posts/{id} endpoint");
        TestReporter.setCategory("API", "DELETE", "Posts", "CRUD");
        TestReporter.setAuthor("API Test Team");
        
        try {
            TestReporter.logInfo("Starting DELETE /posts/{id} API test");
            
            int postId = 1;
            Map<String, Object> pathParams = new HashMap<>();
            pathParams.put("id", postId);
            
            // Execute DELETE request
            Response response = apiClient.delete("/posts/{id}", pathParams);
            
            // Validate deletion response
            ApiResponseValidator.of(response)
                .validateStatusCode(200)
                .validateResponseTime(2000L);
            
            TestReporter.logPass("DELETE /posts/{id} API test passed - Post deleted successfully");
            
        } catch (Exception e) {
            TestReporter.logFail("DELETE /posts/{id} API test failed: " + e.getMessage());
            TestReporter.logException(e);
            throw e;
        } finally {
            TestReporter.endTest();
        }
    }
    
    @Test(priority = 6, description = "Test API error handling with invalid endpoint")
    public void testApiErrorHandling() {
        TestReporter.startTest("API Error Handling Test", "Validate error responses for invalid endpoints");
        TestReporter.setCategory("API", "ErrorHandling", "Negative");
        TestReporter.setAuthor("API Test Team");
        
        try {
            TestReporter.logInfo("Starting API error handling test");
            
            // Test invalid endpoint
            Response response = apiClient.get("/invalid-endpoint");
            
            // Validate error response
            ApiResponseValidator.of(response)
                .validateClientErrorStatusCode()
                .validateResponseTime(3000L);
            
            TestReporter.logPass("API error handling test passed - Error response handled correctly");
            
        } catch (Exception e) {
            TestReporter.logFail("API error handling test failed: " + e.getMessage());
            TestReporter.logException(e);
            throw e;
        } finally {
            TestReporter.endTest();
        }
    }
    
    @Test(priority = 7, description = "Test API performance with multiple requests")
    public void testApiPerformance() {
        TestReporter.startTest("API Performance Test", "Validate API performance with multiple sequential requests");
        TestReporter.setCategory("API", "Performance", "Load");
        TestReporter.setAuthor("API Test Team");
        
        try {
            TestReporter.logInfo("Starting API performance test");
            
            long totalTime = 0;
            int requestCount = 10;
            long maxAllowedTime = 500; // ms per request
            
            for (int i = 1; i <= requestCount; i++) {
                long startTime = System.currentTimeMillis();
                
                Response response = apiClient.get("/posts/" + i);
                
                long requestTime = System.currentTimeMillis() - startTime;
                totalTime += requestTime;
                
                ApiResponseValidator.of(response)
                    .validateSuccessStatusCode()
                    .validateResponseTime(maxAllowedTime);
                
                TestReporter.logInfo("Request " + i + " completed in " + requestTime + " ms");
            }
            
            long averageTime = totalTime / requestCount;
            
            // Performance validation
            if (averageTime > maxAllowedTime) {
                throw new AssertionError("Average response time " + averageTime + " ms exceeds maximum " + maxAllowedTime + " ms");
            }
            
            TestReporter.logPass("API performance test passed - Average response time: " + averageTime + " ms");
            
            // Performance summary table
            String[][] performanceData = {
                {"Metric", "Value", "Status"},
                {"Total Requests", String.valueOf(requestCount), "Completed"},
                {"Total Time", totalTime + " ms", "Measured"},
                {"Average Time", averageTime + " ms", "< " + maxAllowedTime + " ms"},
                {"Max Allowed Time", maxAllowedTime + " ms", "Threshold"},
                {"Performance Status", averageTime <= maxAllowedTime ? "PASS" : "FAIL", "Result"}
            };
            
            TestReporter.addTable("API Performance Summary", performanceData);
            
        } catch (Exception e) {
            TestReporter.logFail("API performance test failed: " + e.getMessage());
            TestReporter.logException(e);
            throw e;
        } finally {
            TestReporter.endTest();
        }
    }
    
    @AfterMethod
    public void tearDown() {
        logger.info("Tearing down API test method");
        
        // Clean up API client if needed
        if (apiClient != null) {
            // Reset client state for next test
            apiClient.reset();
        }
    }
    
    @AfterClass
    public void tearDownClass() {
        logger.info("Tearing down Advanced API Test class");
        
        // Clean up API client resources
        if (apiClient != null) {
            apiClient.cleanup();
        }
    }
    
    @AfterSuite
    public void tearDownSuite() {
        logger.info("Tearing down Advanced API Test suite");
        
        // Generate final reports
        TestReporter.flushReports();
        
        logger.info("API test execution completed. Statistics: {}", TestReporter.getTestStatistics());
        logger.info("API test report available at: {}", TestReporter.getReportPath());
    }
}