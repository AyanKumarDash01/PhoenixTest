package com.phoenix.hrm.parallel;

import com.phoenix.hrm.parallel.ParallelExecutionManager.ThreadContext;
import com.phoenix.hrm.parallel.ParallelExecutionManager.ExecutionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Batch Execution Result for Parallel Test Execution
 * 
 * Collects and manages results from parallel test execution including
 * success/failure statistics, execution times, and detailed results
 * for each test in the batch.
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 3.0
 * @since Phase 3
 */
public class BatchExecutionResult {
    
    private final List<ThreadContext> successfulResults = new ArrayList<>();
    private final List<ThreadContext> failedResults = new ArrayList<>();
    private final List<Throwable> failures = new ArrayList<>();
    private int timeoutCount = 0;
    private long totalExecutionTime = 0;
    private final Map<String, Object> metadata = new ConcurrentHashMap<>();
    
    /**
     * Add a successful result
     * 
     * @param context Thread context from successful execution
     */
    public void addResult(ThreadContext context) {
        if (context.getStatus() == ExecutionStatus.COMPLETED) {
            successfulResults.add(context);
        } else if (context.getStatus() == ExecutionStatus.FAILED) {
            failedResults.add(context);
        } else if (context.getStatus() == ExecutionStatus.TIMEOUT) {
            failedResults.add(context);
            timeoutCount++;
        }
    }
    
    /**
     * Add a failure result
     * 
     * @param cause The cause of the failure
     */
    public void addFailure(Throwable cause) {
        failures.add(cause);
    }
    
    /**
     * Add a timeout
     */
    public void addTimeout() {
        timeoutCount++;
    }
    
    /**
     * Set total execution time for the batch
     * 
     * @param totalExecutionTime Total time in milliseconds
     */
    public void setTotalExecutionTime(long totalExecutionTime) {
        this.totalExecutionTime = totalExecutionTime;
    }
    
    /**
     * Get number of successful executions
     * 
     * @return Success count
     */
    public int getSuccessCount() {
        return successfulResults.size();
    }
    
    /**
     * Get number of failed executions
     * 
     * @return Failure count
     */
    public int getFailureCount() {
        return failedResults.size() + failures.size();
    }
    
    /**
     * Get number of timeout executions
     * 
     * @return Timeout count
     */
    public int getTimeoutCount() {
        return timeoutCount;
    }
    
    /**
     * Get total number of executions
     * 
     * @return Total count
     */
    public int getTotalCount() {
        return getSuccessCount() + getFailureCount();
    }
    
    /**
     * Get success rate as percentage
     * 
     * @return Success rate percentage
     */
    public double getSuccessRate() {
        int total = getTotalCount();
        return total > 0 ? (getSuccessCount() * 100.0 / total) : 0.0;
    }
    
    /**
     * Get total execution time
     * 
     * @return Total execution time in milliseconds
     */
    public long getTotalExecutionTime() {
        return totalExecutionTime;
    }
    
    /**
     * Get average execution time per test
     * 
     * @return Average execution time in milliseconds
     */
    public long getAverageExecutionTime() {
        int total = getTotalCount();
        return total > 0 ? (totalExecutionTime / total) : 0L;
    }
    
    /**
     * Get successful results
     * 
     * @return List of successful thread contexts
     */
    public List<ThreadContext> getSuccessfulResults() {
        return new ArrayList<>(successfulResults);
    }
    
    /**
     * Get failed results
     * 
     * @return List of failed thread contexts
     */
    public List<ThreadContext> getFailedResults() {
        return new ArrayList<>(failedResults);
    }
    
    /**
     * Get failure exceptions
     * 
     * @return List of failure causes
     */
    public List<Throwable> getFailures() {
        return new ArrayList<>(failures);
    }
    
    /**
     * Check if all executions were successful
     * 
     * @return true if all executions succeeded
     */
    public boolean isAllSuccessful() {
        return getFailureCount() == 0 && getSuccessCount() > 0;
    }
    
    /**
     * Check if any executions failed
     * 
     * @return true if any execution failed
     */
    public boolean hasFailures() {
        return getFailureCount() > 0;
    }
    
    /**
     * Check if any executions timed out
     * 
     * @return true if any execution timed out
     */
    public boolean hasTimeouts() {
        return timeoutCount > 0;
    }
    
    /**
     * Add metadata
     * 
     * @param key Metadata key
     * @param value Metadata value
     */
    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    /**
     * Get metadata
     * 
     * @param key Metadata key
     * @return Metadata value
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * Get all metadata
     * 
     * @return Metadata map
     */
    public Map<String, Object> getAllMetadata() {
        return new ConcurrentHashMap<>(metadata);
    }
    
    /**
     * Get execution summary as string
     * 
     * @return Summary string
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Batch Execution Results: %d total, %d successful (%.1f%%), %d failed", 
            getTotalCount(), getSuccessCount(), getSuccessRate(), getFailureCount()));
        
        if (hasTimeouts()) {
            summary.append(String.format(", %d timeout", getTimeoutCount()));
        }
        
        summary.append(String.format(" | Total time: %dms, Average: %dms", 
            getTotalExecutionTime(), getAverageExecutionTime()));
        
        return summary.toString();
    }
    
    /**
     * Get detailed execution report
     * 
     * @return Detailed report as string
     */
    public String getDetailedReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("=== Parallel Execution Detailed Report ===\n");
        report.append(getSummary()).append("\n\n");
        
        if (!successfulResults.isEmpty()) {
            report.append("Successful Executions:\n");
            for (ThreadContext context : successfulResults) {
                report.append(String.format("  ✓ %s - %dms (Thread: %d)\n", 
                    context.getTestName(), context.getExecutionTime(), context.getThreadId()));
            }
            report.append("\n");
        }
        
        if (!failedResults.isEmpty()) {
            report.append("Failed Executions:\n");
            for (ThreadContext context : failedResults) {
                report.append(String.format("  ✗ %s - %s after %dms (Thread: %d)\n", 
                    context.getTestName(), context.getStatus(), context.getExecutionTime(), context.getThreadId()));
            }
            report.append("\n");
        }
        
        if (!failures.isEmpty()) {
            report.append("Exception Details:\n");
            for (int i = 0; i < failures.size(); i++) {
                Throwable failure = failures.get(i);
                report.append(String.format("  [%d] %s: %s\n", 
                    i + 1, failure.getClass().getSimpleName(), failure.getMessage()));
            }
            report.append("\n");
        }
        
        if (!metadata.isEmpty()) {
            report.append("Metadata:\n");
            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                report.append(String.format("  %s: %s\n", entry.getKey(), entry.getValue()));
            }
        }
        
        report.append("=============================================");
        
        return report.toString();
    }
    
    /**
     * Get performance statistics
     * 
     * @return Performance statistics map
     */
    public Map<String, Object> getPerformanceStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        // Basic statistics
        stats.put("totalTests", getTotalCount());
        stats.put("successfulTests", getSuccessCount());
        stats.put("failedTests", getFailureCount());
        stats.put("timedOutTests", getTimeoutCount());
        stats.put("successRate", getSuccessRate());
        
        // Timing statistics
        stats.put("totalExecutionTimeMs", getTotalExecutionTime());
        stats.put("averageExecutionTimeMs", getAverageExecutionTime());
        
        // Detailed timing analysis
        if (!successfulResults.isEmpty()) {
            long minTime = successfulResults.stream().mapToLong(ThreadContext::getExecutionTime).min().orElse(0);
            long maxTime = successfulResults.stream().mapToLong(ThreadContext::getExecutionTime).max().orElse(0);
            double avgTime = successfulResults.stream().mapToLong(ThreadContext::getExecutionTime).average().orElse(0);
            
            stats.put("minExecutionTimeMs", minTime);
            stats.put("maxExecutionTimeMs", maxTime);
            stats.put("avgSuccessfulExecutionTimeMs", Math.round(avgTime));
        }
        
        // Thread distribution
        Map<Long, String> threadDistribution = new ConcurrentHashMap<>();
        for (ThreadContext context : successfulResults) {
            threadDistribution.put(context.getThreadId(), context.getTestName());
        }
        for (ThreadContext context : failedResults) {
            threadDistribution.put(context.getThreadId(), context.getTestName() + " (FAILED)");
        }
        stats.put("threadDistribution", threadDistribution);
        
        return stats;
    }
    
    @Override
    public String toString() {
        return getSummary();
    }
}