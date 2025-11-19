package com.phoenix.hrm.crossbrowser;

import com.phoenix.hrm.config.BrowserType;

import java.util.ArrayList;
import java.util.List;

/**
 * Cross-Browser Execution Result Container
 * 
 * Holds results from cross-browser test execution including
 * success/failure status for each browser and overall statistics.
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 3.0
 * @since Phase 3
 */
public class CrossBrowserExecutionResult {
    
    private final List<BrowserExecutionResult> browserResults;
    private int passCount;
    private int failCount;
    
    /**
     * Constructor for CrossBrowserExecutionResult
     */
    public CrossBrowserExecutionResult() {
        this.browserResults = new ArrayList<>();
        this.passCount = 0;
        this.failCount = 0;
    }
    
    /**
     * Add a browser execution result
     * 
     * @param browser The browser type
     * @param passed Whether the test passed
     * @param exception Exception if test failed
     */
    public void addBrowserResult(BrowserType browser, boolean passed, Exception exception) {
        browserResults.add(new BrowserExecutionResult(browser, passed, exception));
        
        if (passed) {
            passCount++;
        } else {
            failCount++;
        }
    }
    
    /**
     * Get all browser results
     * 
     * @return List of browser execution results
     */
    public List<BrowserExecutionResult> getBrowserResults() {
        return new ArrayList<>(browserResults);
    }
    
    /**
     * Get total number of browsers tested
     * 
     * @return Total count
     */
    public int getTotalCount() {
        return browserResults.size();
    }
    
    /**
     * Get number of passed tests
     * 
     * @return Pass count
     */
    public int getPassCount() {
        return passCount;
    }
    
    /**
     * Get number of failed tests
     * 
     * @return Fail count
     */
    public int getFailCount() {
        return failCount;
    }
    
    /**
     * Get success rate as percentage
     * 
     * @return Success rate percentage
     */
    public double getSuccessRate() {
        if (getTotalCount() == 0) {
            return 0.0;
        }
        return (double) passCount / getTotalCount() * 100.0;
    }
    
    /**
     * Check if all browsers passed
     * 
     * @return true if all tests passed
     */
    public boolean allPassed() {
        return failCount == 0 && passCount > 0;
    }
    
    /**
     * Check if any browsers failed
     * 
     * @return true if any test failed
     */
    public boolean anyFailed() {
        return failCount > 0;
    }
    
    /**
     * Get browsers that passed
     * 
     * @return List of browser types that passed
     */
    public List<BrowserType> getPassedBrowsers() {
        return browserResults.stream()
                .filter(BrowserExecutionResult::isPassed)
                .map(BrowserExecutionResult::getBrowser)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Get browsers that failed
     * 
     * @return List of browser types that failed
     */
    public List<BrowserType> getFailedBrowsers() {
        return browserResults.stream()
                .filter(result -> !result.isPassed())
                .map(BrowserExecutionResult::getBrowser)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Get detailed summary report
     * 
     * @return Detailed summary as string
     */
    public String getSummaryReport() {
        StringBuilder report = new StringBuilder();
        report.append("Cross-Browser Execution Summary:\n");
        report.append("================================\n");
        report.append(String.format("Total Browsers: %d\n", getTotalCount()));
        report.append(String.format("Passed: %d\n", getPassCount()));
        report.append(String.format("Failed: %d\n", getFailCount()));
        report.append(String.format("Success Rate: %.1f%%\n\n", getSuccessRate()));
        
        if (getPassCount() > 0) {
            report.append("Passed Browsers:\n");
            for (BrowserType browser : getPassedBrowsers()) {
                report.append(String.format("  ✓ %s\n", browser.getDisplayName()));
            }
            report.append("\n");
        }
        
        if (getFailCount() > 0) {
            report.append("Failed Browsers:\n");
            for (BrowserExecutionResult result : browserResults) {
                if (!result.isPassed()) {
                    report.append(String.format("  ✗ %s", result.getBrowser().getDisplayName()));
                    if (result.getException() != null) {
                        report.append(String.format(" - %s", result.getException().getMessage()));
                    }
                    report.append("\n");
                }
            }
        }
        
        return report.toString();
    }
    
    @Override
    public String toString() {
        return String.format("CrossBrowserExecutionResult{total=%d, passed=%d, failed=%d, successRate=%.1f%%}", 
                getTotalCount(), getPassCount(), getFailCount(), getSuccessRate());
    }
}