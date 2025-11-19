package com.phoenix.hrm.tests;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple Performance Test for Phoenix HRM
 * 
 * This is a standalone performance test that demonstrates load testing
 * without complex dependencies on the framework classes.
 * 
 * @author Phoenix HRM Test Team
 */
public class SimplePerformanceTest {
    
    private static final Logger logger = LoggerFactory.getLogger(SimplePerformanceTest.class);
    
    // Test configuration
    private static final String BASE_URL = "https://opensource-demo.orangehrmlive.com/";
    private static final String USERNAME = "Admin";
    private static final String PASSWORD = "admin123";
    
    // Performance test parameters
    private static final int[] USER_COUNTS = {5, 10, 15}; // Reduced for demo
    private static final int DURATION_MINUTES = 2; // Reduced for demo
    
    // Results storage
    private static final List<PerformanceResult> results = Collections.synchronizedList(new ArrayList<>());
    
    @BeforeClass
    public void setupPerformanceTest() {
        logger.info("=== Phoenix HRM Simple Performance Test ===");
        logger.info("Target URL: {}", BASE_URL);
        logger.info("User Counts: {}", Arrays.toString(USER_COUNTS));
        logger.info("Duration: {} minutes per test", DURATION_MINUTES);
        logger.info("============================================");
        
        // Setup WebDriverManager
        WebDriverManager.chromedriver().setup();
        
        // Clear previous results
        results.clear();
    }
    
    @Test(priority = 1, description = "Login Performance Test - 5 Concurrent Users")
    public void testLogin5Users() {
        executePerformanceTest("Login_5Users", 5, DURATION_MINUTES);
    }
    
    @Test(priority = 2, description = "Login Performance Test - 10 Concurrent Users")
    public void testLogin10Users() {
        executePerformanceTest("Login_10Users", 10, DURATION_MINUTES);
    }
    
    @Test(priority = 3, description = "Login Performance Test - 15 Concurrent Users")
    public void testLogin15Users() {
        executePerformanceTest("Login_15Users", 15, DURATION_MINUTES);
    }
    
    /**
     * Executes performance test with specified parameters
     */
    private void executePerformanceTest(String testName, int concurrentUsers, int durationMinutes) {
        logger.info("Starting performance test: {} with {} users for {} minutes", 
            testName, concurrentUsers, durationMinutes);
        
        Instant startTime = Instant.now();
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        List<Future<UserResult>> futures = new ArrayList<>();
        
        // Submit user scenarios
        for (int i = 0; i < concurrentUsers; i++) {
            final int userId = i + 1;
            futures.add(executor.submit(() -> executeUserScenario(userId, durationMinutes)));
        }
        
        // Wait for completion
        List<UserResult> userResults = new ArrayList<>();
        for (Future<UserResult> future : futures) {
            try {
                UserResult result = future.get();
                if (result != null) {
                    userResults.add(result);
                }
            } catch (Exception e) {
                logger.error("User scenario failed: {}", e.getMessage());
            }
        }
        
        executor.shutdown();
        Duration totalDuration = Duration.between(startTime, Instant.now());
        
        // Calculate performance metrics
        PerformanceResult result = calculateMetrics(testName, concurrentUsers, userResults, totalDuration);
        results.add(result);
        
        // Log results
        logPerformanceResults(result);
        
        logger.info("Completed performance test: {}", testName);
    }
    
    /**
     * Executes individual user scenario
     */
    private UserResult executeUserScenario(int userId, int durationMinutes) {
        logger.debug("Starting user scenario: User {}", userId);
        
        WebDriver driver = null;
        int successfulLogins = 0;
        int failedLogins = 0;
        List<Long> responseTimes = new ArrayList<>();
        
        Instant endTime = Instant.now().plus(Duration.ofMinutes(durationMinutes));
        
        try {
            while (Instant.now().isBefore(endTime)) {
                driver = createDriver();
                Instant actionStart = Instant.now();
                
                try {
                    // Perform login scenario
                    performLoginScenario(driver);
                    
                    long responseTime = Duration.between(actionStart, Instant.now()).toMillis();
                    responseTimes.add(responseTime);
                    successfulLogins++;
                    
                    // Small delay between iterations
                    Thread.sleep(500);
                    
                } catch (Exception e) {
                    logger.debug("User {} login failed: {}", userId, e.getMessage());
                    failedLogins++;
                    Thread.sleep(1000); // Longer delay after failure
                } finally {
                    if (driver != null) {
                        driver.quit();
                        driver = null;
                    }
                }
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
        
        logger.debug("User {} completed - Success: {}, Failed: {}", userId, successfulLogins, failedLogins);
        return new UserResult(userId, successfulLogins, failedLogins, responseTimes);
    }
    
    /**
     * Performs the login scenario
     */
    private void performLoginScenario(WebDriver driver) {
        driver.get(BASE_URL);
        
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        // Wait for login page
        WebElement usernameField = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.name("username")));
        WebElement passwordField = driver.findElement(By.name("password"));
        WebElement loginButton = driver.findElement(By.xpath("//button[@type='submit']"));
        
        // Perform login
        usernameField.clear();
        usernameField.sendKeys(USERNAME);
        passwordField.clear();
        passwordField.sendKeys(PASSWORD);
        loginButton.click();
        
        // Wait for dashboard or login success indicator
        wait.until(ExpectedConditions.or(
            ExpectedConditions.presenceOfElementLocated(By.xpath("//h6[contains(text(), 'Dashboard')]")),
            ExpectedConditions.presenceOfElementLocated(By.className("oxd-userdropdown"))
        ));
        
        // Simple verification that we're logged in
        driver.findElement(By.className("oxd-userdropdown"));
    }
    
    /**
     * Creates a WebDriver instance
     */
    private WebDriver createDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        
        return driver;
    }
    
    /**
     * Calculates performance metrics from user results
     */
    private PerformanceResult calculateMetrics(String testName, int concurrentUsers, 
                                             List<UserResult> userResults, Duration totalDuration) {
        int totalRequests = userResults.stream().mapToInt(ur -> ur.successCount + ur.failCount).sum();
        int totalErrors = userResults.stream().mapToInt(ur -> ur.failCount).sum();
        
        List<Long> allResponseTimes = new ArrayList<>();
        userResults.forEach(ur -> allResponseTimes.addAll(ur.responseTimes));
        
        if (allResponseTimes.isEmpty()) {
            return new PerformanceResult(testName, concurrentUsers, totalDuration,
                totalRequests, totalErrors, 0, 0, 0, 100.0, 0);
        }
        
        Collections.sort(allResponseTimes);
        
        long minResponseTime = allResponseTimes.get(0);
        long maxResponseTime = allResponseTimes.get(allResponseTimes.size() - 1);
        double avgResponseTime = allResponseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double throughput = (double) (totalRequests - totalErrors) / totalDuration.getSeconds();
        double errorRate = totalRequests > 0 ? (double) totalErrors / totalRequests * 100 : 0;
        
        return new PerformanceResult(testName, concurrentUsers, totalDuration,
            totalRequests, totalErrors, minResponseTime, maxResponseTime, 
            (long) avgResponseTime, errorRate, throughput);
    }
    
    /**
     * Logs performance results
     */
    private void logPerformanceResults(PerformanceResult result) {
        logger.info("=== PERFORMANCE TEST RESULTS ===");
        logger.info("Test: {}", result.testName);
        logger.info("Concurrent Users: {}", result.concurrentUsers);
        logger.info("Duration: {} seconds", result.duration.getSeconds());
        logger.info("Total Requests: {}", result.totalRequests);
        logger.info("Total Errors: {}", result.totalErrors);
        logger.info("Error Rate: {:.2f}%", result.errorRate);
        logger.info("Throughput: {:.2f} req/sec", result.throughput);
        logger.info("Response Times - Min: {}ms, Max: {}ms, Avg: {}ms", 
            result.minResponseTime, result.maxResponseTime, result.avgResponseTime);
        
        // Simple pass/fail based on error rate
        boolean passed = result.errorRate <= 10.0; // 10% error threshold
        logger.info("Test Status: {}", passed ? "PASSED" : "FAILED");
        logger.info("=================================");
    }
    
    @AfterClass
    public void generateSummaryReport() {
        logger.info("=== PERFORMANCE TEST SUMMARY ===");
        logger.info("Total Tests Executed: {}", results.size());
        
        int passedTests = 0;
        for (PerformanceResult result : results) {
            boolean passed = result.errorRate <= 10.0;
            if (passed) passedTests++;
            
            logger.info("Test: {} | Users: {} | Requests: {} | Errors: {} | Throughput: {:.2f} | Status: {}",
                result.testName, result.concurrentUsers, result.totalRequests, 
                result.totalErrors, result.throughput, passed ? "PASS" : "FAIL");
        }
        
        logger.info("Tests Passed: {}/{}", passedTests, results.size());
        logger.info("Pass Rate: {:.2f}%", (double) passedTests / results.size() * 100);
        logger.info("=================================");
    }
    
    // Data classes
    private static class UserResult {
        final int userId;
        final int successCount;
        final int failCount;
        final List<Long> responseTimes;
        
        UserResult(int userId, int successCount, int failCount, List<Long> responseTimes) {
            this.userId = userId;
            this.successCount = successCount;
            this.failCount = failCount;
            this.responseTimes = new ArrayList<>(responseTimes);
        }
    }
    
    private static class PerformanceResult {
        final String testName;
        final int concurrentUsers;
        final Duration duration;
        final int totalRequests;
        final int totalErrors;
        final long minResponseTime;
        final long maxResponseTime;
        final long avgResponseTime;
        final double errorRate;
        final double throughput;
        
        PerformanceResult(String testName, int concurrentUsers, Duration duration,
                         int totalRequests, int totalErrors, long minResponseTime,
                         long maxResponseTime, long avgResponseTime, double errorRate,
                         double throughput) {
            this.testName = testName;
            this.concurrentUsers = concurrentUsers;
            this.duration = duration;
            this.totalRequests = totalRequests;
            this.totalErrors = totalErrors;
            this.minResponseTime = minResponseTime;
            this.maxResponseTime = maxResponseTime;
            this.avgResponseTime = avgResponseTime;
            this.errorRate = errorRate;
            this.throughput = throughput;
        }
    }
}