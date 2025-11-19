package com.phoenix.hrm.tests.performance;

import com.phoenix.hrm.tests.BaseTest;
import com.phoenix.hrm.performance.PerformanceTestManager;
import com.phoenix.hrm.performance.PerformanceTestResult;
import com.phoenix.hrm.core.driver.WebDriverFactory;
import org.openqa.selenium.WebDriver;
import com.phoenix.hrm.pages.EmployeeListPage;
import com.phoenix.hrm.pages.LoginPage;
import com.phoenix.hrm.core.config.ConfigManager;
import com.phoenix.hrm.core.reporting.ExtentReportManager;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Employee List Performance Test Suite
 * 
 * Comprehensive performance testing for Employee List page including:
 * - Load testing with 25, 50, 100 concurrent users
 * - Response time measurement and analysis
 * - Throughput and error rate validation
 * - Stress testing with gradual load increase
 * - Memory and resource usage monitoring
 * - Performance baseline comparison
 * 
 * @author Phoenix HRM Test Team
 */
public class EmployeeListPerformanceTest extends BaseTest {
    
    private static final Logger logger = LoggerFactory.getLogger(EmployeeListPerformanceTest.class);
    private static final ConfigManager config = ConfigManager.getInstance();
    
    // Test configuration
    private static final int[] STANDARD_USER_COUNTS = {25, 50, 100};
    private static final int[] STRESS_USER_COUNTS = {10, 25, 50, 75, 100, 150, 200};
    private static final int DEFAULT_DURATION_MINUTES = 5;
    private static final int STRESS_STEP_DURATION = 3;
    
    @BeforeClass
    public void setupPerformanceTests() {
        logger.info("Initializing Employee List Performance Test Suite");
        
        // Clear any existing performance data
        PerformanceTestManager.clearPerformanceData();
        
        // Create performance test report
        ExtentReportManager.createTest("🚀 Employee List Performance Test Suite", 
            "Comprehensive performance testing for Employee List functionality");
        
        ExtentReportManager.logInfo("=== PERFORMANCE TEST CONFIGURATION ===");
        ExtentReportManager.logInfo("Target Page: Employee List");
        ExtentReportManager.logInfo("User Counts: " + java.util.Arrays.toString(STANDARD_USER_COUNTS));
        ExtentReportManager.logInfo("Test Duration: " + DEFAULT_DURATION_MINUTES + " minutes per scenario");
        ExtentReportManager.logInfo("Performance Thresholds:");
        
        PerformanceTestManager.PerformanceBaseline baseline = PerformanceTestManager.getPerformanceBaseline();
        ExtentReportManager.logInfo("- Response Time: ≤ " + baseline.getResponseTimeThreshold() + "ms");
        ExtentReportManager.logInfo("- Error Rate: ≤ " + baseline.getErrorRateThreshold() + "%");
        ExtentReportManager.logInfo("- Throughput: ≥ " + baseline.getThroughputThreshold() + " req/s");
        
        // Warm up system
        warmUpSystem();
    }
    
    @Test(priority = 1, description = "Employee List Load Test - 25 Concurrent Users", groups = {"performance", "load"})
    public void testEmployeeList25Users() {
        logger.info("Starting Employee List performance test with 25 concurrent users");
        
        Runnable employeeListScenario = createEmployeeListScenario();
        
        PerformanceTestResult result = PerformanceTestManager.executeLoadTest(
            "EmployeeList_25Users",
            employeeListScenario,
            25,
            DEFAULT_DURATION_MINUTES
        );
        
        validatePerformanceResults(result, "25 Users Load Test");
    }
    
    @Test(priority = 2, description = "Employee List Load Test - 50 Concurrent Users", groups = {"performance", "load"})
    public void testEmployeeList50Users() {
        logger.info("Starting Employee List performance test with 50 concurrent users");
        
        Runnable employeeListScenario = createEmployeeListScenario();
        
        PerformanceTestResult result = PerformanceTestManager.executeLoadTest(
            "EmployeeList_50Users",
            employeeListScenario,
            50,
            DEFAULT_DURATION_MINUTES
        );
        
        validatePerformanceResults(result, "50 Users Load Test");
    }
    
    @Test(priority = 3, description = "Employee List Load Test - 100 Concurrent Users", groups = {"performance", "load"})
    public void testEmployeeList100Users() {
        logger.info("Starting Employee List performance test with 100 concurrent users");
        
        Runnable employeeListScenario = createEmployeeListScenario();
        
        PerformanceTestResult result = PerformanceTestManager.executeLoadTest(
            "EmployeeList_100Users",
            employeeListScenario,
            100,
            DEFAULT_DURATION_MINUTES
        );
        
        validatePerformanceResults(result, "100 Users Load Test");
    }
    
    @Test(priority = 4, description = "Employee List Stress Test - Gradual Load Increase", groups = {"performance", "stress"})
    public void testEmployeeListStressTest() {
        logger.info("Starting Employee List stress test with gradual load increase");
        
        ExtentReportManager.createTest("💪 Employee List Stress Test", 
            "Stress testing with gradually increasing load to find breaking point");
        
        Runnable employeeListScenario = createEmployeeListScenario();
        
        List<PerformanceTestResult> stressResults = PerformanceTestManager.executeStressTest(
            "EmployeeList_StressTest",
            employeeListScenario,
            STRESS_USER_COUNTS,
            STRESS_STEP_DURATION
        );
        
        analyzeStressTestResults(stressResults);
    }
    
    @Test(priority = 5, description = "Employee List Search Performance Test", groups = {"performance", "load"})
    public void testEmployeeListSearchPerformance() {
        logger.info("Starting Employee List search functionality performance test");
        
        Runnable searchScenario = createEmployeeSearchScenario();
        
        PerformanceTestResult result = PerformanceTestManager.executeLoadTest(
            "EmployeeList_SearchPerformance",
            searchScenario,
            50,
            DEFAULT_DURATION_MINUTES
        );
        
        validatePerformanceResults(result, "Search Performance Test");
    }
    
    @Test(priority = 6, description = "Employee List Pagination Performance Test", groups = {"performance", "load"})
    public void testEmployeeListPaginationPerformance() {
        logger.info("Starting Employee List pagination performance test");
        
        Runnable paginationScenario = createPaginationScenario();
        
        PerformanceTestResult result = PerformanceTestManager.executeLoadTest(
            "EmployeeList_PaginationPerformance",
            paginationScenario,
            50,
            DEFAULT_DURATION_MINUTES
        );
        
        validatePerformanceResults(result, "Pagination Performance Test");
    }
    
    @Test(priority = 7, description = "Employee List Sorting Performance Test", groups = {"performance", "load"})
    public void testEmployeeListSortingPerformance() {
        logger.info("Starting Employee List sorting performance test");
        
        Runnable sortingScenario = createSortingScenario();
        
        PerformanceTestResult result = PerformanceTestManager.executeLoadTest(
            "EmployeeList_SortingPerformance",
            sortingScenario,
            50,
            DEFAULT_DURATION_MINUTES
        );
        
        validatePerformanceResults(result, "Sorting Performance Test");
    }
    
    /**
     * Creates the main Employee List test scenario
     */
    private Runnable createEmployeeListScenario() {
        return () -> {
            WebDriver scenarioDriver = null;
            try {
                // Create fresh browser session for each user simulation
                scenarioDriver = WebDriverFactory.createDriver();
                
                // Navigate to application and login
                LoginPage loginPage = new LoginPage(scenarioDriver);
                loginPage.navigateToLogin();
                loginPage.performLogin(
                    config.getProperty("admin.username"),
                    config.getProperty("admin.password")
                );
                
                // Access Employee List page
                EmployeeListPage employeeListPage = new EmployeeListPage(scenarioDriver);
                employeeListPage.navigateToEmployeeList();
                
                // Verify page load
                employeeListPage.waitForPageLoad();
                
                // Perform random user actions
                simulateRandomUserBehavior(employeeListPage, scenarioDriver);
                
            } catch (Exception e) {
                logger.error("Employee List scenario failed: {}", e.getMessage());
                throw new RuntimeException("Scenario execution failed", e);
            } finally {
                if (scenarioDriver != null) {
                    scenarioDriver.quit();
                }
            }
        };
    }
    
    /**
     * Creates Employee search scenario
     */
    private Runnable createEmployeeSearchScenario() {
        return () -> {
            WebDriver scenarioDriver = null;
            try {
                scenarioDriver = WebDriverFactory.createDriver();
                
                LoginPage loginPage = new LoginPage(scenarioDriver);
                loginPage.navigateToLogin();
                loginPage.performLogin(
                    config.getProperty("admin.username"),
                    config.getProperty("admin.password")
                );
                
                EmployeeListPage employeeListPage = new EmployeeListPage(scenarioDriver);
                employeeListPage.navigateToEmployeeList();
                employeeListPage.waitForPageLoad();
                
                // Perform search operations
                String[] searchTerms = {"John", "Admin", "Developer", "Manager", "Smith"};
                String searchTerm = searchTerms[ThreadLocalRandom.current().nextInt(searchTerms.length)];
                
                employeeListPage.searchEmployee(searchTerm);
                employeeListPage.waitForSearchResults();
                employeeListPage.clearSearch();
                
            } catch (Exception e) {
                logger.error("Employee search scenario failed: {}", e.getMessage());
                throw new RuntimeException("Search scenario execution failed", e);
            } finally {
                if (scenarioDriver != null) {
                    scenarioDriver.quit();
                }
            }
        };
    }
    
    /**
     * Creates pagination test scenario
     */
    private Runnable createPaginationScenario() {
        return () -> {
            WebDriver scenarioDriver = null;
            try {
                scenarioDriver = WebDriverFactory.createDriver();
                
                LoginPage loginPage = new LoginPage(scenarioDriver);
                loginPage.navigateToLogin();
                loginPage.performLogin(
                    config.getProperty("admin.username"),
                    config.getProperty("admin.password")
                );
                
                EmployeeListPage employeeListPage = new EmployeeListPage(scenarioDriver);
                employeeListPage.navigateToEmployeeList();
                employeeListPage.waitForPageLoad();
                
                // Navigate through pages
                int pagesToVisit = ThreadLocalRandom.current().nextInt(3, 6);
                for (int i = 0; i < pagesToVisit; i++) {
                    if (employeeListPage.hasNextPage()) {
                        employeeListPage.goToNextPage();
                        employeeListPage.waitForPageLoad();
                    } else {
                        break;
                    }
                }
                
            } catch (Exception e) {
                logger.error("Pagination scenario failed: {}", e.getMessage());
                throw new RuntimeException("Pagination scenario execution failed", e);
            } finally {
                if (scenarioDriver != null) {
                    scenarioDriver.quit();
                }
            }
        };
    }
    
    /**
     * Creates sorting test scenario
     */
    private Runnable createSortingScenario() {
        return () -> {
            WebDriver scenarioDriver = null;
            try {
                scenarioDriver = WebDriverFactory.createDriver();
                
                LoginPage loginPage = new LoginPage(scenarioDriver);
                loginPage.navigateToLogin();
                loginPage.performLogin(
                    config.getProperty("admin.username"),
                    config.getProperty("admin.password")
                );
                
                EmployeeListPage employeeListPage = new EmployeeListPage(scenarioDriver);
                employeeListPage.navigateToEmployeeList();
                employeeListPage.waitForPageLoad();
                
                // Perform sorting operations
                String[] sortColumns = {"name", "jobTitle", "department", "status"};
                String sortColumn = sortColumns[ThreadLocalRandom.current().nextInt(sortColumns.length)];
                
                employeeListPage.sortByColumn(sortColumn);
                employeeListPage.waitForPageLoad();
                
                // Sort in reverse order
                employeeListPage.sortByColumn(sortColumn);
                employeeListPage.waitForPageLoad();
                
            } catch (Exception e) {
                logger.error("Sorting scenario failed: {}", e.getMessage());
                throw new RuntimeException("Sorting scenario execution failed", e);
            } finally {
                if (scenarioDriver != null) {
                    scenarioDriver.quit();
                }
            }
        };
    }
    
    /**
     * Simulates random user behavior on Employee List page
     */
    private void simulateRandomUserBehavior(EmployeeListPage employeeListPage, WebDriver driver) {
        try {
            int actions = ThreadLocalRandom.current().nextInt(3, 8);
            
            for (int i = 0; i < actions; i++) {
                int actionType = ThreadLocalRandom.current().nextInt(4);
                
                switch (actionType) {
                    case 0: // View employee details
                        if (employeeListPage.hasEmployees()) {
                            employeeListPage.clickFirstEmployee();
                            Thread.sleep(1000);
                            driver.navigate().back();
                            employeeListPage.waitForPageLoad();
                        }
                        break;
                        
                    case 1: // Change page size
                        employeeListPage.changePageSize(20);
                        employeeListPage.waitForPageLoad();
                        break;
                        
                    case 2: // Navigate pages
                        if (employeeListPage.hasNextPage()) {
                            employeeListPage.goToNextPage();
                            employeeListPage.waitForPageLoad();
                        }
                        break;
                        
                    case 3: // Refresh page
                        driver.navigate().refresh();
                        employeeListPage.waitForPageLoad();
                        break;
                        
                    default:
                        Thread.sleep(500);
                        break;
                }
                
                // Random wait between actions
                Thread.sleep(ThreadLocalRandom.current().nextInt(500, 1500));
            }
            
        } catch (Exception e) {
            logger.warn("Random behavior simulation failed: {}", e.getMessage());
        }
    }
    
    /**
     * Validates performance test results against thresholds
     */
    private void validatePerformanceResults(PerformanceTestResult result, String testDescription) {
        ExtentReportManager.createTest("📊 " + testDescription + " Results", 
            "Performance analysis for " + testDescription);
        
        // Log key metrics
        ExtentReportManager.logInfo("=== TEST SUMMARY ===");
        ExtentReportManager.logInfo("Test: " + result.getTestName());
        ExtentReportManager.logInfo("Duration: " + result.getFormattedDuration());
        ExtentReportManager.logInfo("Concurrent Users: " + result.getConcurrentUsers());
        ExtentReportManager.logInfo("Total Requests: " + result.getTotalRequests());
        
        // Validate thresholds
        if (result.meetsPerformanceThresholds()) {
            ExtentReportManager.logPass("✅ " + testDescription + " PASSED - All thresholds met");
            ExtentReportManager.logInfo("Performance Grade: " + result.getPerformanceGrade());
        } else {
            ExtentReportManager.logFail("❌ " + testDescription + " FAILED - Performance thresholds exceeded");
            ExtentReportManager.logInfo("Performance Grade: " + result.getPerformanceGrade());
        }
        
        // Log detailed metrics
        ExtentReportManager.logInfo("=== DETAILED METRICS ===");
        ExtentReportManager.logInfo("Average Response Time: " + result.getAvgResponseTime() + "ms");
        ExtentReportManager.logInfo("95th Percentile Response Time: " + result.getP95ResponseTime() + "ms");
        ExtentReportManager.logInfo("Error Rate: " + String.format("%.2f%%", result.getErrorRate()));
        ExtentReportManager.logInfo("Throughput: " + String.format("%.2f", result.getThroughput()) + " req/s");
        ExtentReportManager.logInfo("Success Rate: " + String.format("%.2f%%", result.getSuccessRate()));
        
        // Assert for TestNG
        org.testng.Assert.assertTrue(result.meetsPerformanceThresholds(), 
            testDescription + " failed to meet performance thresholds. Grade: " + result.getPerformanceGrade());
    }
    
    /**
     * Analyzes stress test results to find system limits
     */
    private void analyzeStressTestResults(List<PerformanceTestResult> results) {
        ExtentReportManager.createTest("📈 Stress Test Analysis", 
            "Analysis of system behavior under increasing load");
        
        ExtentReportManager.logInfo("=== STRESS TEST SUMMARY ===");
        ExtentReportManager.logInfo("Total Test Steps: " + results.size());
        
        // Find breaking point
        PerformanceTestResult lastSuccess = null;
        PerformanceTestResult firstFailure = null;
        
        for (PerformanceTestResult result : results) {
            if (result.meetsPerformanceThresholds()) {
                lastSuccess = result;
            } else if (firstFailure == null) {
                firstFailure = result;
                break;
            }
        }
        
        // Log breaking point analysis
        if (lastSuccess != null && firstFailure != null) {
            ExtentReportManager.logWarning("🔥 BREAKING POINT DETECTED");
            ExtentReportManager.logInfo("Maximum Stable Load: " + lastSuccess.getConcurrentUsers() + " users");
            ExtentReportManager.logInfo("- Response Time: " + lastSuccess.getAvgResponseTime() + "ms");
            ExtentReportManager.logInfo("- Error Rate: " + String.format("%.2f%%", lastSuccess.getErrorRate()));
            ExtentReportManager.logInfo("- Throughput: " + String.format("%.2f", lastSuccess.getThroughput()) + " req/s");
            
            ExtentReportManager.logWarning("System Degradation Started At: " + firstFailure.getConcurrentUsers() + " users");
            ExtentReportManager.logInfo("- Response Time: " + firstFailure.getAvgResponseTime() + "ms");
            ExtentReportManager.logInfo("- Error Rate: " + String.format("%.2f%%", firstFailure.getErrorRate()));
            ExtentReportManager.logInfo("- Throughput: " + String.format("%.2f", firstFailure.getThroughput()) + " req/s");
            
        } else if (lastSuccess == null) {
            ExtentReportManager.logFail("⚠️ SYSTEM FAILURE - Unable to handle minimum load");
        } else {
            ExtentReportManager.logPass("✅ EXCELLENT PERFORMANCE - System handled all test loads successfully");
            ExtentReportManager.logInfo("Maximum Tested Load: " + results.get(results.size() - 1).getConcurrentUsers() + " users");
        }
        
        // Performance trend analysis
        ExtentReportManager.logInfo("=== PERFORMANCE TRENDS ===");
        for (PerformanceTestResult result : results) {
            ExtentReportManager.logInfo(String.format(
                "Users: %3d | Response: %4dms | Error: %5.2f%% | Throughput: %6.2f req/s | %s",
                result.getConcurrentUsers(),
                result.getAvgResponseTime(),
                result.getErrorRate(),
                result.getThroughput(),
                result.meetsPerformanceThresholds() ? "PASS" : "FAIL"
            ));
        }
    }
    
    /**
     * Warms up the system before performance testing
     */
    private void warmUpSystem() {
        logger.info("Warming up system before performance tests");
        
        WebDriver warmupDriver = null;
        try {
            warmupDriver = WebDriverFactory.createDriver();
            
            LoginPage loginPage = new LoginPage(warmupDriver);
            loginPage.navigateToLogin();
            loginPage.performLogin(
                config.getProperty("admin.username"),
                config.getProperty("admin.password")
            );
            
            EmployeeListPage employeeListPage = new EmployeeListPage(warmupDriver);
            employeeListPage.navigateToEmployeeList();
            employeeListPage.waitForPageLoad();
            
            // Perform some warm-up operations
            for (int i = 0; i < 3; i++) {
                warmupDriver.navigate().refresh();
                employeeListPage.waitForPageLoad();
                Thread.sleep(1000);
            }
            
            logger.info("System warm-up completed");
            ExtentReportManager.logInfo("✅ System warm-up completed successfully");
            
        } catch (Exception e) {
            logger.warn("System warm-up failed: {}", e.getMessage());
            ExtentReportManager.logWarning("⚠️ System warm-up encountered issues: " + e.getMessage());
        } finally {
            if (warmupDriver != null) {
                warmupDriver.quit();
            }
        }
    }
    
    @AfterClass
    public void generatePerformanceReport() {
        logger.info("Generating comprehensive performance test report");
        
        List<PerformanceTestResult> allResults = PerformanceTestManager.getAllTestResults();
        
        ExtentReportManager.createTest("📋 Performance Test Summary Report", 
            "Complete summary of all performance test executions");
        
        ExtentReportManager.logInfo("=== OVERALL PERFORMANCE SUMMARY ===");
        ExtentReportManager.logInfo("Total Tests Executed: " + allResults.size());
        
        // Calculate overall statistics
        long totalRequests = allResults.stream().mapToInt(PerformanceTestResult::getTotalRequests).sum();
        long totalErrors = allResults.stream().mapToInt(PerformanceTestResult::getTotalErrors).sum();
        double overallErrorRate = totalRequests > 0 ? (double) totalErrors / totalRequests * 100 : 0;
        
        ExtentReportManager.logInfo("Total Requests Processed: " + totalRequests);
        ExtentReportManager.logInfo("Total Errors: " + totalErrors);
        ExtentReportManager.logInfo("Overall Error Rate: " + String.format("%.2f%%", overallErrorRate));
        
        // Test pass/fail summary
        long passedTests = allResults.stream().mapToInt(r -> r.meetsPerformanceThresholds() ? 1 : 0).sum();
        long failedTests = allResults.size() - passedTests;
        
        ExtentReportManager.logInfo("Tests Passed: " + passedTests);
        ExtentReportManager.logInfo("Tests Failed: " + failedTests);
        ExtentReportManager.logInfo("Pass Rate: " + String.format("%.2f%%", (double) passedTests / allResults.size() * 100));
        
        if (failedTests == 0) {
            ExtentReportManager.logPass("🎉 ALL PERFORMANCE TESTS PASSED!");
        } else {
            ExtentReportManager.logFail("⚠️ " + failedTests + " performance test(s) failed");
        }
        
        // Generate CSV report for analysis
        generateCsvReport(allResults);
        
        logger.info("Performance test suite completed. Results available in ExtentReports.");
    }
    
    /**
     * Generates CSV report for external analysis
     */
    private void generateCsvReport(List<PerformanceTestResult> results) {
        try {
            StringBuilder csvContent = new StringBuilder();
            csvContent.append(PerformanceTestResult.getCsvHeader()).append("\n");
            
            for (PerformanceTestResult result : results) {
                csvContent.append(result.toCsvRow()).append("\n");
            }
            
            // In a real implementation, you would write this to a file
            logger.info("CSV Report Generated:");
            logger.info(csvContent.toString());
            
        } catch (Exception e) {
            logger.error("Failed to generate CSV report: {}", e.getMessage());
        }
    }
}