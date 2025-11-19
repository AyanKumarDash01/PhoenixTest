package com.phoenix.hrm.tests;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Simple standalone API test demonstrating REST API testing
 * This test can be run independently without the full framework
 * Tests against publicly available REST API endpoints
 */
public class SimpleAPITest {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleAPITest.class);
    
    // Using JSONPlaceholder - a free fake REST API for testing
    private static final String BASE_URL = "https://jsonplaceholder.typicode.com";
    
    @BeforeClass
    public void setUp() {
        logger.info("Setting up API tests");
        RestAssured.baseURI = BASE_URL;
    }
    
    @Test(priority = 1)
    public void testGetAllUsers() {
        logger.info("Testing GET /users - Get all users");
        
        Response response = RestAssured
            .given()
                .header("Content-Type", "application/json")
            .when()
                .get("/users")
            .then()
                .extract().response();
        
        // Validate status code
        int statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode, 200, "Status code should be 200");
        logger.info("✓ Status code: {}", statusCode);
        
        // Validate response time
        long responseTime = response.getTime();
        Assert.assertTrue(responseTime < 5000, "Response time should be less than 5000ms");
        logger.info("✓ Response time: {} ms", responseTime);
        
        // Validate response body structure
        Assert.assertTrue(response.jsonPath().getList("$").size() > 0, "Response should contain users");
        logger.info("✓ Found {} users in response", response.jsonPath().getList("$").size());
        
        // Validate first user has required fields
        Assert.assertNotNull(response.jsonPath().getString("[0].id"), "First user should have an id");
        Assert.assertNotNull(response.jsonPath().getString("[0].name"), "First user should have a name");
        Assert.assertNotNull(response.jsonPath().getString("[0].email"), "First user should have an email");
        logger.info("✓ First user validation passed");
    }
    
    @Test(priority = 2)
    public void testGetUserById() {
        logger.info("Testing GET /users/1 - Get user by ID");
        
        int userId = 1;
        Response response = RestAssured
            .given()
                .header("Content-Type", "application/json")
                .pathParam("id", userId)
            .when()
                .get("/users/{id}")
            .then()
                .extract().response();
        
        // Validate status code
        Assert.assertEquals(response.getStatusCode(), 200, "Status code should be 200");
        logger.info("✓ Status code: {}", response.getStatusCode());
        
        // Validate user ID matches request
        int responseUserId = response.jsonPath().getInt("id");
        Assert.assertEquals(responseUserId, userId, "Response user ID should match requested ID");
        logger.info("✓ User ID matches: {}", responseUserId);
        
        // Validate user has all required fields
        Assert.assertNotNull(response.jsonPath().getString("name"), "User should have a name");
        Assert.assertNotNull(response.jsonPath().getString("email"), "User should have an email");
        Assert.assertNotNull(response.jsonPath().getString("username"), "User should have a username");
        logger.info("✓ User details validation passed for: {}", response.jsonPath().getString("name"));
    }
    
    @Test(priority = 3)
    public void testCreateUser() {
        logger.info("Testing POST /users - Create new user");
        
        String requestBody = """
            {
                "name": "Test User",
                "username": "testuser",
                "email": "test@example.com"
            }
            """;
        
        Response response = RestAssured
            .given()
                .header("Content-Type", "application/json")
                .body(requestBody)
            .when()
                .post("/users")
            .then()
                .extract().response();
        
        // Validate status code (201 for created)
        Assert.assertEquals(response.getStatusCode(), 201, "Status code should be 201 for created resource");
        logger.info("✓ Status code: {}", response.getStatusCode());
        
        // Validate response contains created user data
        Assert.assertEquals(response.jsonPath().getString("name"), "Test User", "Name should match request");
        Assert.assertEquals(response.jsonPath().getString("email"), "test@example.com", "Email should match request");
        Assert.assertNotNull(response.jsonPath().getString("id"), "Created user should have an ID");
        logger.info("✓ User created successfully with ID: {}", response.jsonPath().getString("id"));
    }
    
    @Test(priority = 4)
    public void testUpdateUser() {
        logger.info("Testing PUT /users/1 - Update user");
        
        String requestBody = """
            {
                "id": 1,
                "name": "Updated User Name",
                "username": "updateduser",
                "email": "updated@example.com"
            }
            """;
        
        Response response = RestAssured
            .given()
                .header("Content-Type", "application/json")
                .pathParam("id", 1)
                .body(requestBody)
            .when()
                .put("/users/{id}")
            .then()
                .extract().response();
        
        // Validate status code
        Assert.assertEquals(response.getStatusCode(), 200, "Status code should be 200");
        logger.info("✓ Status code: {}", response.getStatusCode());
        
        // Validate updated data
        Assert.assertEquals(response.jsonPath().getString("name"), "Updated User Name", "Name should be updated");
        Assert.assertEquals(response.jsonPath().getString("email"), "updated@example.com", "Email should be updated");
        logger.info("✓ User updated successfully: {}", response.jsonPath().getString("name"));
    }
    
    @Test(priority = 5)
    public void testDeleteUser() {
        logger.info("Testing DELETE /users/1 - Delete user");
        
        Response response = RestAssured
            .given()
                .pathParam("id", 1)
            .when()
                .delete("/users/{id}")
            .then()
                .extract().response();
        
        // Validate status code (200 for successful delete)
        Assert.assertEquals(response.getStatusCode(), 200, "Status code should be 200 for successful delete");
        logger.info("✓ User deleted successfully, status code: {}", response.getStatusCode());
    }
    
    @Test(priority = 6)
    public void testGetPosts() {
        logger.info("Testing GET /posts - Get all posts");
        
        Response response = RestAssured
            .when()
                .get("/posts")
            .then()
                .extract().response();
        
        // Validate status code
        Assert.assertEquals(response.getStatusCode(), 200, "Status code should be 200");
        
        // Validate response contains posts
        Assert.assertTrue(response.jsonPath().getList("$").size() > 0, "Should return posts");
        logger.info("✓ Found {} posts", response.jsonPath().getList("$").size());
        
        // Validate first post structure
        Assert.assertNotNull(response.jsonPath().getString("[0].id"), "Post should have an id");
        Assert.assertNotNull(response.jsonPath().getString("[0].title"), "Post should have a title");
        Assert.assertNotNull(response.jsonPath().getString("[0].body"), "Post should have a body");
        logger.info("✓ Posts API test passed");
    }
    
    @Test(priority = 7)
    public void testInvalidEndpoint() {
        logger.info("Testing invalid endpoint - should return 404");
        
        Response response = RestAssured
            .when()
                .get("/invalid-endpoint")
            .then()
                .extract().response();
        
        // Validate 404 status code
        Assert.assertEquals(response.getStatusCode(), 404, "Status code should be 404 for invalid endpoint");
        logger.info("✓ Invalid endpoint correctly returned 404");
    }
    
    @Test(priority = 8)
    public void testResponseHeaders() {
        logger.info("Testing response headers");
        
        Response response = RestAssured
            .when()
                .get("/users")
            .then()
                .extract().response();
        
        // Validate common headers
        Assert.assertNotNull(response.getHeader("content-type"), "Response should have content-type header");
        Assert.assertTrue(response.getHeader("content-type").contains("application/json"), 
            "Content-type should be application/json");
        logger.info("✓ Content-Type header: {}", response.getHeader("content-type"));
        
        // Check if server header exists
        String serverHeader = response.getHeader("server");
        if (serverHeader != null) {
            logger.info("✓ Server header: {}", serverHeader);
        }
        
        logger.info("✓ Response headers validation completed");
    }
}