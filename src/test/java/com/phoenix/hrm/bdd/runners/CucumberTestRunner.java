package com.phoenix.hrm.bdd.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * Cucumber Test Runner for Phoenix HRM BDD Tests
 * 
 * This class configures and executes Cucumber BDD tests with comprehensive
 * reporting, tag-based execution, and integration with the Phoenix HRM framework.
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 3.0
 * @since Phase 3
 */
@CucumberOptions(
    // Feature file locations
    features = {
        "src/test/resources/features"
    },
    
    // Step definition package locations
    glue = {
        "com.phoenix.hrm.bdd.stepdefinitions",
        "com.phoenix.hrm.bdd.hooks"
    },
    
    // Report generation plugins
    plugin = {
        "pretty",                                                    // Console output formatting
        "html:test-output/cucumber-reports/html-reports",           // HTML report
        "json:test-output/cucumber-reports/json-reports/cucumber.json", // JSON report for integration
        "junit:test-output/cucumber-reports/junit-reports/cucumber.xml", // JUnit XML report
        "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:" // ExtentReports integration
    },
    
    // Test execution configuration
    tags = "@smoke or @regression",  // Run smoke and regression tests by default
    
    // Additional options
    monochrome = true,              // Clean console output
    publish = true,                 // Publish reports to Cucumber Reports
    dryRun = false                  // Set to true to validate step definitions without execution
)
public class CucumberTestRunner extends AbstractTestNGCucumberTests {
    
    /**
     * Enable parallel execution of scenarios
     * This method overrides the default behavior to support parallel execution
     * 
     * @return DataProvider for parallel execution
     */
    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
    
    /*
     * Execution Instructions:
     * 
     * 1. Run all tests:
     *    mvn test -Dtest=CucumberTestRunner
     * 
     * 2. Run specific tags:
     *    mvn test -Dtest=CucumberTestRunner -Dcucumber.filter.tags="@smoke"
     *    mvn test -Dtest=CucumberTestRunner -Dcucumber.filter.tags="@regression"
     *    mvn test -Dtest=CucumberTestRunner -Dcucumber.filter.tags="@negative"
     * 
     * 3. Run with parallel execution:
     *    mvn test -Dtest=CucumberTestRunner -Dcucumber.execution.parallel.enabled=true
     * 
     * 4. Run with specific browser:
     *    mvn test -Dtest=CucumberTestRunner -Dbrowser=chrome
     *    mvn test -Dtest=CucumberTestRunner -Dbrowser=firefox
     * 
     * 5. Run with specific environment:
     *    mvn test -Dtest=CucumberTestRunner -Denvironment=staging
     *    mvn test -Dtest=CucumberTestRunner -Denvironment=production
     * 
     * 6. Dry run to validate step definitions:
     *    Change dryRun to true and run tests to validate all steps have implementations
     * 
     * 7. Generate only reports without execution:
     *    mvn test -Dtest=CucumberTestRunner -Dcucumber.plugin="json:target/cucumber.json"
     */
}