package com.phoenix.hrm.crossbrowser;

import com.phoenix.hrm.config.BrowserType;

import java.time.LocalDateTime;

/**
 * Individual Browser Execution Result
 * 
 * Represents the result of test execution on a single browser,
 * including success/failure status, browser information, and any exceptions.
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 3.0
 * @since Phase 3
 */
public class BrowserExecutionResult {
    
    private final BrowserType browser;
    private final boolean passed;
    private final Exception exception;
    private final LocalDateTime executionTime;
    private final String executionId;
    
    /**
     * Constructor for BrowserExecutionResult
     * 
     * @param browser The browser type
     * @param passed Whether the test passed
     * @param exception Exception if test failed (can be null)
     */
    public BrowserExecutionResult(BrowserType browser, boolean passed, Exception exception) {
        this.browser = browser;
        this.passed = passed;
        this.exception = exception;
        this.executionTime = LocalDateTime.now();
        this.executionId = generateExecutionId();
    }
    
    /**
     * Get the browser type
     * 
     * @return Browser type
     */
    public BrowserType getBrowser() {
        return browser;
    }
    
    /**
     * Check if test passed
     * 
     * @return true if test passed
     */
    public boolean isPassed() {
        return passed;
    }
    
    /**
     * Check if test failed
     * 
     * @return true if test failed
     */
    public boolean isFailed() {
        return !passed;
    }
    
    /**
     * Get exception if test failed
     * 
     * @return Exception or null if test passed
     */
    public Exception getException() {
        return exception;
    }
    
    /**
     * Get execution timestamp
     * 
     * @return Execution time
     */
    public LocalDateTime getExecutionTime() {
        return executionTime;
    }
    
    /**
     * Get unique execution ID
     * 
     * @return Execution ID
     */
    public String getExecutionId() {
        return executionId;
    }
    
    /**
     * Get status as string
     * 
     * @return "PASS" or "FAIL"
     */
    public String getStatusString() {
        return passed ? "PASS" : "FAIL";
    }
    
    /**
     * Get error message if test failed
     * 
     * @return Error message or "N/A" if test passed
     */
    public String getErrorMessage() {
        if (exception != null) {
            return exception.getMessage();
        }
        return passed ? "N/A" : "Unknown error";
    }
    
    /**
     * Get detailed result information
     * 
     * @return Detailed result as string
     */
    public String getDetailedResult() {
        StringBuilder result = new StringBuilder();
        result.append("Browser: ").append(browser.getDisplayName()).append("\n");
        result.append("Status: ").append(getStatusString()).append("\n");
        result.append("Execution Time: ").append(executionTime).append("\n");
        result.append("Execution ID: ").append(executionId).append("\n");
        
        if (!passed && exception != null) {
            result.append("Error: ").append(exception.getMessage()).append("\n");
        }
        
        return result.toString();
    }
    
    /**
     * Generate unique execution ID
     * 
     * @return Unique execution ID
     */
    private String generateExecutionId() {
        return String.format("%s_%d", 
            browser.getBrowserKey().toUpperCase(), 
            System.currentTimeMillis());
    }
    
    @Override
    public String toString() {
        return String.format("BrowserExecutionResult{browser=%s, passed=%s, executionTime=%s}", 
                browser.getDisplayName(), passed, executionTime);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        BrowserExecutionResult that = (BrowserExecutionResult) obj;
        return passed == that.passed && 
               browser == that.browser && 
               executionId.equals(that.executionId);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(browser, passed, executionId);
    }
}