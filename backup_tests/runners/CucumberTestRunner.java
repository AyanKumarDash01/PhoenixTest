package com.phoenix.hrm.runners;

import io.cucumber.junit.platform.engine.Constants;
import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Cucumber TestNG Runner for Employee Management BDD Tests
 * 
 * Integrates Cucumber BDD scenarios with TestNG framework
 * Supports parallel execution, multiple output formats, and tag-based filtering
 * 
 * Features:
 * - Parallel execution capability
 * - Multiple report formats (JSON, XML, HTML)
 * - Tag-based test filtering
 * - TestNG integration for enterprise reporting
 * - Rerun capability for failed scenarios
 * 
 * @author Phoenix HRM Test Team
 */
@CucumberOptions(
    // Feature files location
    features = {
        "src/test/resources/features"
    },
    
    // Step definitions packages
    glue = {
        "com.phoenix.hrm.stepdefinitions"
    },
    
    // Output plugins for various report formats
    plugin = {
        "pretty",                                           // Console output
        "html:target/cucumber-reports/html",               // HTML report
        "json:target/cucumber-reports/cucumber.json",      // JSON report for advanced processing
        "xml:target/cucumber-reports/cucumber.xml",        // XML report for CI/CD integration
        "junit:target/cucumber-reports/cucumber-junit.xml", // JUnit XML for TestNG integration
        "rerun:target/cucumber-reports/rerun.txt"          // Failed scenarios for rerun
    },
    
    // Default tags - can be overridden via system properties
    tags = "@EmployeeManagement and not @Skip",
    
    // Additional options
    monochrome = true,      // Clean console output
    snippets = io.cucumber.junit.platform.engine.Constants.SNIPPET_TYPE_CAMELCASE,
    publish = false         // Don't publish to Cucumber Cloud by default
)
public class CucumberTestRunner extends AbstractTestNGCucumberTests {
    
    /**
     * Enables parallel execution of scenarios
     * Each scenario will run in a separate thread
     * 
     * @return DataProvider for parallel execution
     */
    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}

/**
 * Smoke Test Runner - Executes only smoke test scenarios
 * Used for quick validation of core functionality
 */
@CucumberOptions(
    features = "src/test/resources/features",
    glue = "com.phoenix.hrm.stepdefinitions",
    plugin = {
        "pretty",
        "html:target/cucumber-reports/smoke/html",
        "json:target/cucumber-reports/smoke/cucumber-smoke.json"
    },
    tags = "@Smoke",
    monochrome = true
)
class CucumberSmokeTestRunner extends AbstractTestNGCucumberTests {
    
    @Override
    @DataProvider(parallel = false) // Smoke tests run sequentially for stability
    public Object[][] scenarios() {
        return super.scenarios();
    }
}

/**
 * Regression Test Runner - Executes comprehensive regression test suite
 * Includes all regression and integration scenarios
 */
@CucumberOptions(
    features = "src/test/resources/features",
    glue = "com.phoenix.hrm.stepdefinitions",
    plugin = {
        "pretty",
        "html:target/cucumber-reports/regression/html",
        "json:target/cucumber-reports/regression/cucumber-regression.json",
        "xml:target/cucumber-reports/regression/cucumber-regression.xml"
    },
    tags = "@Regression or @Integration",
    monochrome = true
)
class CucumberRegressionTestRunner extends AbstractTestNGCucumberTests {
    
    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}

/**
 * Performance Test Runner - Executes performance-focused scenarios
 * Runs with specific configuration for performance monitoring
 */
@CucumberOptions(
    features = "src/test/resources/features",
    glue = "com.phoenix.hrm.stepdefinitions",
    plugin = {
        "pretty",
        "html:target/cucumber-reports/performance/html",
        "json:target/cucumber-reports/performance/cucumber-performance.json"
    },
    tags = "@Performance",
    monochrome = true
)
class CucumberPerformanceTestRunner extends AbstractTestNGCucumberTests {
    
    @Override
    @DataProvider(parallel = false) // Performance tests run sequentially for accurate measurements
    public Object[][] scenarios() {
        return super.scenarios();
    }
}

/**
 * API-focused Test Runner - Executes scenarios that test API integration points
 * While primary API tests are in Phase 7, some BDD scenarios validate API responses
 */
@CucumberOptions(
    features = "src/test/resources/features",
    glue = "com.phoenix.hrm.stepdefinitions",
    plugin = {
        "pretty",
        "html:target/cucumber-reports/api/html",
        "json:target/cucumber-reports/api/cucumber-api.json"
    },
    tags = "@API",
    monochrome = true
)
class CucumberAPITestRunner extends AbstractTestNGCucumberTests {
    
    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}

/**
 * Failed Scenario Rerun Runner
 * Reruns only the scenarios that failed in the previous execution
 * Uses the rerun.txt file generated by the main runner
 */
@CucumberOptions(
    features = "@target/cucumber-reports/rerun.txt", // Reads failed scenarios from rerun file
    glue = "com.phoenix.hrm.stepdefinitions",
    plugin = {
        "pretty",
        "html:target/cucumber-reports/rerun/html",
        "json:target/cucumber-reports/rerun/cucumber-rerun.json"
    },
    monochrome = true
)
class CucumberRerunTestRunner extends AbstractTestNGCucumberTests {
    
    @Override
    @DataProvider(parallel = false) // Rerun tests sequentially for better debugging
    public Object[][] scenarios() {
        return super.scenarios();
    }
}