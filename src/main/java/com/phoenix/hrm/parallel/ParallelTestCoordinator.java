package com.phoenix.hrm.parallel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.TestNG;
import org.testng.xml.XmlSuite;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Parallel Test Execution Coordinator for Phoenix HRM Test Automation Framework
 * 
 * Central orchestrator that coordinates all parallel execution components:
 * - Manages multiple test suite execution
 * - Coordinates resource allocation across test suites
 * - Provides centralized monitoring and reporting
 * - Handles cross-suite dependencies and synchronization
 * - Manages test environment provisioning and cleanup
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 3.0
 * @since Phase 3
 */
public class ParallelTestCoordinator {
    
    private static final Logger logger = LoggerFactory.getLogger(ParallelTestCoordinator.class);
    
    // Singleton instance
    private static volatile ParallelTestCoordinator instance;
    private static final Object instanceLock = new Object();
    
    // Core components
    private final ParallelExecutionManager executionManager;
    private final ThreadSafeUtils.SafeRegistry<SuiteExecution> suiteRegistry;
    private final ThreadSafeUtils.SafeEventBus eventBus;
    private final ThreadSafeUtils.ExecutionTracker coordinatorTracker;
    private final ThreadSafeUtils.SafeRateLimiter resourceLimiter;
    private final ThreadSafeUtils.SafeCache<String, Object> sharedCache;
    
    // Execution state
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicInteger activeSuites = new AtomicInteger(0);
    private final AtomicReference<ExecutionPhase> currentPhase = new AtomicReference<>(ExecutionPhase.IDLE);
    
    // Configuration
    private final CoordinatorConfig config;
    
    /**
     * Execution phases
     */
    public enum ExecutionPhase {
        IDLE,
        INITIALIZING,
        EXECUTING,
        FINALIZING,
        COMPLETED,
        ERROR
    }
    
    /**
     * Suite execution context
     */
    public static class SuiteExecution {
        private final String suiteId;
        private final XmlSuite suite;
        private final TestNG testNG;
        private final CompletableFuture<SuiteExecutionResult> future;
        private final long startTime;
        private volatile long endTime;
        private volatile ExecutionStatus status = ExecutionStatus.PENDING;
        
        public enum ExecutionStatus {
            PENDING,
            RUNNING,
            COMPLETED,
            FAILED,
            CANCELLED
        }
        
        public SuiteExecution(String suiteId, XmlSuite suite) {
            this.suiteId = suiteId;
            this.suite = suite;
            this.testNG = new TestNG();
            this.future = new CompletableFuture<>();
            this.startTime = System.currentTimeMillis();
            
            // Configure TestNG
            this.testNG.setXmlSuites(Arrays.asList(suite));
            this.testNG.addListener(new TestNGIntegration.ParallelExecutionListener());
        }
        
        // Getters
        public String getSuiteId() { return suiteId; }
        public XmlSuite getSuite() { return suite; }
        public TestNG getTestNG() { return testNG; }
        public CompletableFuture<SuiteExecutionResult> getFuture() { return future; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public ExecutionStatus getStatus() { return status; }
        
        public void setEndTime(long endTime) { this.endTime = endTime; }
        public void setStatus(ExecutionStatus status) { this.status = status; }
        
        public long getDuration() {
            return endTime > 0 ? endTime - startTime : System.currentTimeMillis() - startTime;
        }
    }
    
    /**
     * Suite execution result
     */
    public static class SuiteExecutionResult {
        private final String suiteId;
        private final boolean success;
        private final int passedTests;
        private final int failedTests;
        private final int skippedTests;
        private final long executionTime;
        private final Map<String, Object> metadata;
        private final Throwable error;
        
        public SuiteExecutionResult(String suiteId, boolean success, int passedTests, int failedTests, 
                                  int skippedTests, long executionTime, Map<String, Object> metadata, Throwable error) {
            this.suiteId = suiteId;
            this.success = success;
            this.passedTests = passedTests;
            this.failedTests = failedTests;
            this.skippedTests = skippedTests;
            this.executionTime = executionTime;
            this.metadata = metadata != null ? metadata : new HashMap<>();
            this.error = error;
        }
        
        // Getters
        public String getSuiteId() { return suiteId; }
        public boolean isSuccess() { return success; }
        public int getPassedTests() { return passedTests; }
        public int getFailedTests() { return failedTests; }
        public int getSkippedTests() { return skippedTests; }
        public long getExecutionTime() { return executionTime; }
        public Map<String, Object> getMetadata() { return metadata; }
        public Throwable getError() { return error; }
        public int getTotalTests() { return passedTests + failedTests + skippedTests; }
        public double getSuccessRate() { return getTotalTests() > 0 ? (double) passedTests / getTotalTests() * 100 : 0; }
    }
    
    /**
     * Coordinator configuration
     */
    public static class CoordinatorConfig {
        private int maxConcurrentSuites = 3;
        private int maxResourcesPerSuite = 10;
        private long suiteTimeoutMs = 300000; // 5 minutes
        private long resourceWaitTimeoutMs = 30000; // 30 seconds
        private boolean enableCrossReference = true;
        private boolean enableSharedCache = true;
        private String reportOutputPath = "target/parallel-reports";
        
        // Builder pattern
        public static class Builder {
            private final CoordinatorConfig config = new CoordinatorConfig();
            
            public Builder maxConcurrentSuites(int maxConcurrentSuites) {
                config.maxConcurrentSuites = maxConcurrentSuites;
                return this;
            }
            
            public Builder maxResourcesPerSuite(int maxResourcesPerSuite) {
                config.maxResourcesPerSuite = maxResourcesPerSuite;
                return this;
            }
            
            public Builder suiteTimeoutMs(long suiteTimeoutMs) {
                config.suiteTimeoutMs = suiteTimeoutMs;
                return this;
            }
            
            public Builder resourceWaitTimeoutMs(long resourceWaitTimeoutMs) {
                config.resourceWaitTimeoutMs = resourceWaitTimeoutMs;
                return this;
            }
            
            public Builder enableCrossReference(boolean enableCrossReference) {
                config.enableCrossReference = enableCrossReference;
                return this;
            }
            
            public Builder enableSharedCache(boolean enableSharedCache) {
                config.enableSharedCache = enableSharedCache;
                return this;
            }
            
            public Builder reportOutputPath(String reportOutputPath) {
                config.reportOutputPath = reportOutputPath;
                return this;
            }
            
            public CoordinatorConfig build() {
                return config;
            }
        }
        
        // Getters
        public int getMaxConcurrentSuites() { return maxConcurrentSuites; }
        public int getMaxResourcesPerSuite() { return maxResourcesPerSuite; }
        public long getSuiteTimeoutMs() { return suiteTimeoutMs; }
        public long getResourceWaitTimeoutMs() { return resourceWaitTimeoutMs; }
        public boolean isEnableCrossReference() { return enableCrossReference; }
        public boolean isEnableSharedCache() { return enableSharedCache; }
        public String getReportOutputPath() { return reportOutputPath; }
    }
    
    /**
     * Private constructor for singleton
     */
    private ParallelTestCoordinator(CoordinatorConfig config) {
        this.config = config != null ? config : new CoordinatorConfig.Builder().build();
        this.executionManager = ParallelExecutionManager.getInstance();
        this.suiteRegistry = new ThreadSafeUtils.SafeRegistry<>("SuiteRegistry");
        this.eventBus = new ThreadSafeUtils.SafeEventBus("CoordinatorEventBus");
        this.coordinatorTracker = new ThreadSafeUtils.ExecutionTracker();
        this.resourceLimiter = new ThreadSafeUtils.SafeRateLimiter("ResourceLimiter", 
            this.config.getMaxResourcesPerSuite(), 1000);
        this.sharedCache = this.config.isEnableSharedCache() ? 
            new ThreadSafeUtils.SafeCache<>("SharedCache", 300000) : null; // 5 min TTL
        
        logger.info("ParallelTestCoordinator initialized with config: maxConcurrentSuites={}, maxResourcesPerSuite={}", 
            this.config.getMaxConcurrentSuites(), this.config.getMaxResourcesPerSuite());
    }
    
    /**
     * Get singleton instance
     */
    public static ParallelTestCoordinator getInstance() {
        return getInstance(null);
    }
    
    /**
     * Get singleton instance with configuration
     */
    public static ParallelTestCoordinator getInstance(CoordinatorConfig config) {
        if (instance == null) {
            synchronized (instanceLock) {
                if (instance == null) {
                    instance = new ParallelTestCoordinator(config);
                }
            }
        }
        return instance;
    }
    
    /**
     * Execute multiple test suites in parallel
     */
    public CompletableFuture<Map<String, SuiteExecutionResult>> executeSuites(List<XmlSuite> suites) {
        if (suites == null || suites.isEmpty()) {
            return CompletableFuture.completedFuture(new HashMap<>());
        }
        
        if (!isRunning.compareAndSet(false, true)) {
            throw new IllegalStateException("Coordinator is already running");
        }
        
        try {
            currentPhase.set(ExecutionPhase.INITIALIZING);
            logger.info("Starting parallel execution of {} test suites", suites.size());
            
            // Create suite executions
            List<SuiteExecution> suiteExecutions = new ArrayList<>();
            for (XmlSuite suite : suites) {
                String suiteId = generateSuiteId(suite);
                SuiteExecution execution = new SuiteExecution(suiteId, suite);
                suiteExecutions.add(execution);
                suiteRegistry.register(suiteId, execution);
            }
            
            currentPhase.set(ExecutionPhase.EXECUTING);
            
            // Execute suites with concurrency control
            return executeSuitesWithConcurrencyControl(suiteExecutions)
                .whenComplete((results, throwable) -> {
                    currentPhase.set(throwable != null ? ExecutionPhase.ERROR : ExecutionPhase.FINALIZING);
                    finalizeSuiteExecution(results, throwable);
                    currentPhase.set(ExecutionPhase.COMPLETED);
                    isRunning.set(false);
                });
                
        } catch (Exception e) {
            currentPhase.set(ExecutionPhase.ERROR);
            isRunning.set(false);
            logger.error("Error starting suite execution", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Execute a single test suite
     */
    public CompletableFuture<SuiteExecutionResult> executeSuite(XmlSuite suite) {
        return executeSuites(Arrays.asList(suite))
            .thenApply(results -> results.values().iterator().next());
    }
    
    /**
     * Add a test suite to the execution queue
     */
    public String addSuite(XmlSuite suite) {
        String suiteId = generateSuiteId(suite);
        SuiteExecution execution = new SuiteExecution(suiteId, suite);
        suiteRegistry.register(suiteId, execution);
        
        logger.debug("Added suite to registry: {}", suiteId);
        return suiteId;
    }
    
    /**
     * Get suite execution status
     */
    public Optional<SuiteExecution> getSuiteExecution(String suiteId) {
        return Optional.ofNullable(suiteRegistry.get(suiteId));
    }
    
    /**
     * Get all active suite executions
     */
    public Map<String, SuiteExecution> getActiveSuiteExecutions() {
        Map<String, SuiteExecution> activeSuites = new HashMap<>();
        Map<String, SuiteExecution> allSuites = suiteRegistry.snapshot();
        
        for (Map.Entry<String, SuiteExecution> entry : allSuites.entrySet()) {
            SuiteExecution execution = entry.getValue();
            if (execution.getStatus() == SuiteExecution.ExecutionStatus.RUNNING ||
                execution.getStatus() == SuiteExecution.ExecutionStatus.PENDING) {
                activeSuites.put(entry.getKey(), execution);
            }
        }
        
        return activeSuites;
    }
    
    /**
     * Cancel suite execution
     */
    public boolean cancelSuite(String suiteId) {
        SuiteExecution execution = suiteRegistry.get(suiteId);
        if (execution != null) {
            execution.setStatus(SuiteExecution.ExecutionStatus.CANCELLED);
            execution.getFuture().cancel(true);
            logger.info("Cancelled suite execution: {}", suiteId);
            return true;
        }
        return false;
    }
    
    /**
     * Get coordinator statistics
     */
    public Map<String, Object> getCoordinatorStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("isRunning", isRunning.get());
        stats.put("currentPhase", currentPhase.get());
        stats.put("activeSuites", activeSuites.get());
        stats.put("totalSuites", suiteRegistry.size());
        stats.put("executionMetrics", coordinatorTracker.getAllMetrics());
        
        // Suite status distribution
        Map<String, Integer> statusDistribution = new HashMap<>();
        Map<String, SuiteExecution> allSuites = suiteRegistry.snapshot();
        for (SuiteExecution execution : allSuites.values()) {
            String status = execution.getStatus().name();
            statusDistribution.merge(status, 1, Integer::sum);
        }
        stats.put("suiteStatusDistribution", statusDistribution);
        
        return stats;
    }
    
    /**
     * Get shared cache (if enabled)
     */
    public Optional<ThreadSafeUtils.SafeCache<String, Object>> getSharedCache() {
        return Optional.ofNullable(sharedCache);
    }
    
    /**
     * Subscribe to coordinator events
     */
    public <T> void subscribe(Class<T> eventType, ThreadSafeUtils.EventListener<T> listener) {
        eventBus.subscribe(eventType, listener);
    }
    
    /**
     * Publish coordinator event
     */
    public <T> void publishEvent(T event) {
        eventBus.publish(event);
    }
    
    /**
     * Shutdown coordinator
     */
    public void shutdown() {
        if (isRunning.get()) {
            logger.warn("Shutting down coordinator while execution is in progress");
            
            // Cancel all active suites
            Map<String, SuiteExecution> activeSuites = getActiveSuiteExecutions();
            for (String suiteId : activeSuites.keySet()) {
                cancelSuite(suiteId);
            }
        }
        
        // Shutdown components
        eventBus.shutdown();
        resourceLimiter.shutdown();
        if (sharedCache != null) {
            sharedCache.shutdown();
        }
        
        currentPhase.set(ExecutionPhase.IDLE);
        isRunning.set(false);
        
        logger.info("ParallelTestCoordinator shut down");
    }
    
    // Private helper methods
    
    private CompletableFuture<Map<String, SuiteExecutionResult>> executeSuitesWithConcurrencyControl(
            List<SuiteExecution> suiteExecutions) {
        
        Semaphore concurrencyLimiter = new Semaphore(config.getMaxConcurrentSuites());
        Map<String, CompletableFuture<SuiteExecutionResult>> futures = new ConcurrentHashMap<>();
        
        for (SuiteExecution execution : suiteExecutions) {
            CompletableFuture<SuiteExecutionResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    concurrencyLimiter.acquire();
                    return executeSingleSuite(execution);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return createErrorResult(execution.getSuiteId(), e);
                } finally {
                    concurrencyLimiter.release();
                }
            }, executionManager.getExecutorService());
            
            futures.put(execution.getSuiteId(), future);
            execution.getFuture().completeAsync(() -> {
                try {
                    return future.get();
                } catch (Exception e) {
                    return createErrorResult(execution.getSuiteId(), e);
                }
            });
        }
        
        return CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                Map<String, SuiteExecutionResult> results = new HashMap<>();
                for (Map.Entry<String, CompletableFuture<SuiteExecutionResult>> entry : futures.entrySet()) {
                    try {
                        results.put(entry.getKey(), entry.getValue().get());
                    } catch (Exception e) {
                        results.put(entry.getKey(), createErrorResult(entry.getKey(), e));
                    }
                }
                return results;
            });
    }
    
    private SuiteExecutionResult executeSingleSuite(SuiteExecution execution) {
        String suiteId = execution.getSuiteId();
        activeSuites.incrementAndGet();
        execution.setStatus(SuiteExecution.ExecutionStatus.RUNNING);
        
        try {
            logger.info("Executing test suite: {}", suiteId);
            long startTime = System.currentTimeMillis();
            
            // Execute with timeout
            CompletableFuture<Void> testExecution = CompletableFuture.runAsync(() -> {
                execution.getTestNG().run();
            }, executionManager.getExecutorService());
            
            testExecution.get(config.getSuiteTimeoutMs(), TimeUnit.MILLISECONDS);
            
            long endTime = System.currentTimeMillis();
            execution.setEndTime(endTime);
            execution.setStatus(SuiteExecution.ExecutionStatus.COMPLETED);
            
            long executionTime = endTime - startTime;
            coordinatorTracker.recordExecution(suiteId, executionTime);
            
            // Extract results from TestNG
            // This is a simplified version - in practice, you'd extract actual results
            SuiteExecutionResult result = new SuiteExecutionResult(
                suiteId, true, 0, 0, 0, executionTime, 
                new HashMap<>(), null
            );
            
            logger.info("Completed test suite: {} in {}ms", suiteId, executionTime);
            return result;
            
        } catch (TimeoutException e) {
            execution.setStatus(SuiteExecution.ExecutionStatus.FAILED);
            logger.error("Test suite timed out: {}", suiteId);
            return createErrorResult(suiteId, e);
        } catch (Exception e) {
            execution.setStatus(SuiteExecution.ExecutionStatus.FAILED);
            logger.error("Error executing test suite: {}", suiteId, e);
            return createErrorResult(suiteId, e);
        } finally {
            activeSuites.decrementAndGet();
        }
    }
    
    private SuiteExecutionResult createErrorResult(String suiteId, Throwable error) {
        return new SuiteExecutionResult(suiteId, false, 0, 0, 0, 0, new HashMap<>(), error);
    }
    
    private String generateSuiteId(XmlSuite suite) {
        return suite.getName() + "_" + System.currentTimeMillis() + "_" + 
               Integer.toHexString(suite.hashCode());
    }
    
    private void finalizeSuiteExecution(Map<String, SuiteExecutionResult> results, Throwable error) {
        if (error != null) {
            logger.error("Suite execution completed with errors", error);
        } else {
            logger.info("Suite execution completed successfully");
        }
        
        if (results != null) {
            // Generate consolidated report
            generateConsolidatedReport(results);
            
            // Cleanup completed suites from registry
            cleanupCompletedSuites();
        }
    }
    
    private void generateConsolidatedReport(Map<String, SuiteExecutionResult> results) {
        logger.info("=== Consolidated Parallel Execution Report ===");
        
        int totalPassed = 0;
        int totalFailed = 0;
        int totalSkipped = 0;
        long totalExecutionTime = 0;
        int successfulSuites = 0;
        
        for (SuiteExecutionResult result : results.values()) {
            totalPassed += result.getPassedTests();
            totalFailed += result.getFailedTests();
            totalSkipped += result.getSkippedTests();
            totalExecutionTime += result.getExecutionTime();
            if (result.isSuccess()) {
                successfulSuites++;
            }
        }
        
        int totalTests = totalPassed + totalFailed + totalSkipped;
        double overallSuccessRate = totalTests > 0 ? (double) totalPassed / totalTests * 100 : 0;
        double suiteSuccessRate = results.size() > 0 ? (double) successfulSuites / results.size() * 100 : 0;
        
        logger.info("Total Suites: {}", results.size());
        logger.info("Successful Suites: {} ({}%)", successfulSuites, String.format("%.2f", suiteSuccessRate));
        logger.info("Total Tests: {}", totalTests);
        logger.info("Passed: {}, Failed: {}, Skipped: {}", totalPassed, totalFailed, totalSkipped);
        logger.info("Overall Success Rate: {}%", String.format("%.2f", overallSuccessRate));
        logger.info("Total Execution Time: {}ms", totalExecutionTime);
        
        // Individual suite results
        logger.info("Individual Suite Results:");
        for (Map.Entry<String, SuiteExecutionResult> entry : results.entrySet()) {
            SuiteExecutionResult result = entry.getValue();
            logger.info("  {} - Success: {}, Tests: {}, Time: {}ms", 
                entry.getKey(), result.isSuccess(), result.getTotalTests(), result.getExecutionTime());
        }
    }
    
    private void cleanupCompletedSuites() {
        Map<String, SuiteExecution> allSuites = suiteRegistry.snapshot();
        int removedCount = 0;
        
        for (Map.Entry<String, SuiteExecution> entry : allSuites.entrySet()) {
            SuiteExecution execution = entry.getValue();
            if (execution.getStatus() == SuiteExecution.ExecutionStatus.COMPLETED ||
                execution.getStatus() == SuiteExecution.ExecutionStatus.FAILED ||
                execution.getStatus() == SuiteExecution.ExecutionStatus.CANCELLED) {
                
                suiteRegistry.remove(entry.getKey());
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            logger.debug("Cleaned up {} completed suite executions", removedCount);
        }
    }
}