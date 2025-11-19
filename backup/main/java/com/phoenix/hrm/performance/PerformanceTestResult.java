package com.phoenix.hrm.performance;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

/**
 * Performance Test Result Data Class
 * 
 * Stores comprehensive performance test metrics including:
 * - Response time statistics (min, max, average, percentiles)
 * - Throughput and error rate analysis
 * - Test execution metadata
 * - Performance threshold validation results
 * 
 * @author Phoenix HRM Test Team
 */
public class PerformanceTestResult {
    
    private final String testName;
    private final int concurrentUsers;
    private final Duration duration;
    private final int totalRequests;
    private final int totalErrors;
    private final long minResponseTime;
    private final long maxResponseTime;
    private final long avgResponseTime;
    private final long p95ResponseTime;
    private final double errorRate;
    private final double throughput;
    private final boolean meetsPerformanceThresholds;
    private final LocalDateTime executionTime;
    
    // Additional percentile data
    private long p50ResponseTime;
    private long p90ResponseTime;
    private long p99ResponseTime;
    
    public PerformanceTestResult(String testName, int concurrentUsers, Duration duration,
                               int totalRequests, int totalErrors, long minResponseTime,
                               long maxResponseTime, long avgResponseTime, long p95ResponseTime,
                               double errorRate, double throughput, boolean meetsPerformanceThresholds) {
        this.testName = testName;
        this.concurrentUsers = concurrentUsers;
        this.duration = duration;
        this.totalRequests = totalRequests;
        this.totalErrors = totalErrors;
        this.minResponseTime = minResponseTime;
        this.maxResponseTime = maxResponseTime;
        this.avgResponseTime = avgResponseTime;
        this.p95ResponseTime = p95ResponseTime;
        this.errorRate = errorRate;
        this.throughput = throughput;
        this.meetsPerformanceThresholds = meetsPerformanceThresholds;
        this.executionTime = LocalDateTime.now();
    }
    
    /**
     * Sets additional percentile data
     */
    public PerformanceTestResult withPercentiles(long p50, long p90, long p95, long p99) {
        this.p50ResponseTime = p50;
        this.p90ResponseTime = p90;
        this.p95ResponseTime = p95;
        this.p99ResponseTime = p99;
        return this;
    }
    
    // Getters
    public String getTestName() { return testName; }
    public int getConcurrentUsers() { return concurrentUsers; }
    public Duration getDuration() { return duration; }
    public int getTotalRequests() { return totalRequests; }
    public int getTotalErrors() { return totalErrors; }
    public long getMinResponseTime() { return minResponseTime; }
    public long getMaxResponseTime() { return maxResponseTime; }
    public long getAvgResponseTime() { return avgResponseTime; }
    public long getP50ResponseTime() { return p50ResponseTime; }
    public long getP90ResponseTime() { return p90ResponseTime; }
    public long getP95ResponseTime() { return p95ResponseTime; }
    public long getP99ResponseTime() { return p99ResponseTime; }
    public double getErrorRate() { return errorRate; }
    public double getThroughput() { return throughput; }
    public boolean meetsPerformanceThresholds() { return meetsPerformanceThresholds; }
    public LocalDateTime getExecutionTime() { return executionTime; }
    
    /**
     * Gets success rate as percentage
     */
    public double getSuccessRate() {
        return totalRequests > 0 ? ((double)(totalRequests - totalErrors) / totalRequests) * 100 : 0;
    }
    
    /**
     * Gets successful requests count
     */
    public int getSuccessfulRequests() {
        return totalRequests - totalErrors;
    }
    
    /**
     * Checks if test had zero errors
     */
    public boolean isErrorFree() {
        return totalErrors == 0;
    }
    
    /**
     * Gets test status based on performance thresholds
     */
    public String getTestStatus() {
        return meetsPerformanceThresholds ? "PASSED" : "FAILED";
    }
    
    /**
     * Gets performance grade based on metrics
     */
    public String getPerformanceGrade() {
        if (!meetsPerformanceThresholds) return "F";
        
        if (errorRate == 0 && avgResponseTime <= 1000 && throughput >= 20) return "A+";
        if (errorRate <= 1 && avgResponseTime <= 2000 && throughput >= 15) return "A";
        if (errorRate <= 3 && avgResponseTime <= 3000 && throughput >= 10) return "B";
        if (errorRate <= 5 && avgResponseTime <= 4000 && throughput >= 5) return "C";
        return "D";
    }
    
    /**
     * Gets formatted execution time
     */
    public String getFormattedExecutionTime() {
        return executionTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    /**
     * Gets formatted duration
     */
    public String getFormattedDuration() {
        long minutes = duration.toMinutes();
        long seconds = duration.getSeconds() % 60;
        return String.format("%d min %d sec", minutes, seconds);
    }
    
    /**
     * Generates summary report
     */
    public String generateSummaryReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== PERFORMANCE TEST SUMMARY ===\n");
        report.append("Test Name: ").append(testName).append("\n");
        report.append("Execution Time: ").append(getFormattedExecutionTime()).append("\n");
        report.append("Duration: ").append(getFormattedDuration()).append("\n");
        report.append("Concurrent Users: ").append(concurrentUsers).append("\n");
        report.append("Total Requests: ").append(totalRequests).append("\n");
        report.append("Successful Requests: ").append(getSuccessfulRequests()).append("\n");
        report.append("Failed Requests: ").append(totalErrors).append("\n");
        report.append("Success Rate: ").append(String.format("%.2f%%", getSuccessRate())).append("\n");
        report.append("Error Rate: ").append(String.format("%.2f%%", errorRate)).append("\n");
        report.append("Throughput: ").append(String.format("%.2f", throughput)).append(" req/sec\n");
        report.append("\n=== RESPONSE TIME METRICS ===\n");
        report.append("Min Response Time: ").append(minResponseTime).append("ms\n");
        report.append("Max Response Time: ").append(maxResponseTime).append("ms\n");
        report.append("Avg Response Time: ").append(avgResponseTime).append("ms\n");
        report.append("50th Percentile: ").append(p50ResponseTime).append("ms\n");
        report.append("90th Percentile: ").append(p90ResponseTime).append("ms\n");
        report.append("95th Percentile: ").append(p95ResponseTime).append("ms\n");
        report.append("99th Percentile: ").append(p99ResponseTime).append("ms\n");
        report.append("\n=== PERFORMANCE ASSESSMENT ===\n");
        report.append("Status: ").append(getTestStatus()).append("\n");
        report.append("Grade: ").append(getPerformanceGrade()).append("\n");
        report.append("Meets Thresholds: ").append(meetsPerformanceThresholds ? "Yes" : "No").append("\n");
        
        return report.toString();
    }
    
    /**
     * Generates CSV row for reporting
     */
    public String toCsvRow() {
        return String.join(",",
            testName,
            String.valueOf(concurrentUsers),
            getFormattedDuration(),
            String.valueOf(totalRequests),
            String.valueOf(totalErrors),
            String.format("%.2f", errorRate),
            String.format("%.2f", throughput),
            String.valueOf(avgResponseTime),
            String.valueOf(p95ResponseTime),
            getTestStatus(),
            getPerformanceGrade()
        );
    }
    
    /**
     * Gets CSV header for reporting
     */
    public static String getCsvHeader() {
        return "Test Name,Concurrent Users,Duration,Total Requests,Total Errors," +
               "Error Rate (%),Throughput (req/s),Avg Response Time (ms)," +
               "95th Percentile (ms),Status,Grade";
    }
    
    /**
     * Compares this result with another for performance regression analysis
     */
    public PerformanceComparison compareWith(PerformanceTestResult baseline) {
        return new PerformanceComparison(baseline, this);
    }
    
    @Override
    public String toString() {
        return String.format(
            "PerformanceTestResult{testName='%s', users=%d, requests=%d, errors=%d, " +
            "avgResponse=%dms, throughput=%.2f req/s, status=%s}",
            testName, concurrentUsers, totalRequests, totalErrors, 
            avgResponseTime, throughput, getTestStatus()
        );
    }
    
    /**
     * Performance comparison between baseline and current test
     */
    public static class PerformanceComparison {
        private final PerformanceTestResult baseline;
        private final PerformanceTestResult current;
        
        public PerformanceComparison(PerformanceTestResult baseline, PerformanceTestResult current) {
            this.baseline = baseline;
            this.current = current;
        }
        
        public double getResponseTimeChange() {
            if (baseline.getAvgResponseTime() == 0) return 0;
            return ((double)(current.getAvgResponseTime() - baseline.getAvgResponseTime()) / baseline.getAvgResponseTime()) * 100;
        }
        
        public double getThroughputChange() {
            if (baseline.getThroughput() == 0) return 0;
            return ((current.getThroughput() - baseline.getThroughput()) / baseline.getThroughput()) * 100;
        }
        
        public double getErrorRateChange() {
            return current.getErrorRate() - baseline.getErrorRate();
        }
        
        public boolean isRegression() {
            return getResponseTimeChange() > 10 || getThroughputChange() < -10 || getErrorRateChange() > 2;
        }
        
        public String generateComparisonReport() {
            StringBuilder report = new StringBuilder();
            report.append("=== PERFORMANCE COMPARISON ===\n");
            report.append("Baseline Test: ").append(baseline.getTestName()).append("\n");
            report.append("Current Test: ").append(current.getTestName()).append("\n");
            report.append("\n=== CHANGES ===\n");
            report.append("Response Time: ").append(String.format("%.2f%%", getResponseTimeChange()));
            report.append(getResponseTimeChange() > 0 ? " (WORSE)" : " (BETTER)").append("\n");
            report.append("Throughput: ").append(String.format("%.2f%%", getThroughputChange()));
            report.append(getThroughputChange() < 0 ? " (WORSE)" : " (BETTER)").append("\n");
            report.append("Error Rate: ").append(String.format("%.2f%%", getErrorRateChange()));
            report.append(getErrorRateChange() > 0 ? " (WORSE)" : " (BETTER)").append("\n");
            report.append("\n=== VERDICT ===\n");
            report.append("Performance Regression: ").append(isRegression() ? "YES" : "NO").append("\n");
            
            return report.toString();
        }
    }
}