package com.phoenix.hrm.parallel;

import com.phoenix.hrm.config.ConfigurationManager;
import com.phoenix.hrm.reporting.TestReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Advanced Parallel Execution Manager for Phoenix HRM Test Automation Framework
 * 
 * Provides comprehensive parallel execution capabilities including:
 * - Thread-safe test execution with resource isolation
 * - Dynamic thread pool management
 * - Distributed test execution support
 * - Resource contention management
 * - Performance monitoring and optimization
 * - Fault tolerance and recovery mechanisms
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 3.0
 * @since Phase 3
 */
public class ParallelExecutionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ParallelExecutionManager.class);
    
    // Singleton instance
    private static volatile ParallelExecutionManager instance;
    private static final Object instanceLock = new Object();
    
    // Thread pool management
    private ExecutorService mainExecutorService;
    private ScheduledExecutorService scheduledExecutorService;
    private ExecutorService callbackExecutorService;
    
    // Execution monitoring
    private final AtomicInteger activeThreads = new AtomicInteger(0);
    private final AtomicInteger completedTasks = new AtomicInteger(0);
    private final AtomicInteger failedTasks = new AtomicInteger(0);
    private final AtomicLong totalExecutionTime = new AtomicLong(0);
    
    // Resource management
    private final Map<String, ResourcePool> resourcePools = new ConcurrentHashMap<>();
    private final Map<Long, ThreadContext> threadContexts = new ConcurrentHashMap<>();
    
    // Configuration
    private int maxThreads;
    private int coreThreads;
    private long threadKeepAliveTime;
    private boolean enableResourcePooling;
    private boolean enableDistributedExecution;
    
    /**
     * Thread context for isolation
     */
    public static class ThreadContext {
        private final long threadId;
        private final String testName;
        private final Map<String, Object> attributes;
        private final long startTime;
        private volatile ExecutionStatus status;
        
        public ThreadContext(String testName) {
            this.threadId = Thread.currentThread().getId();
            this.testName = testName;
            this.attributes = new ConcurrentHashMap<>();
            this.startTime = System.currentTimeMillis();
            this.status = ExecutionStatus.RUNNING;
        }
        
        // Getters and setters
        public long getThreadId() { return threadId; }
        public String getTestName() { return testName; }
        public Map<String, Object> getAttributes() { return attributes; }
        public long getStartTime() { return startTime; }
        public ExecutionStatus getStatus() { return status; }
        public void setStatus(ExecutionStatus status) { this.status = status; }
        
        public void setAttribute(String key, Object value) {
            attributes.put(key, value);
        }
        
        public Object getAttribute(String key) {
            return attributes.get(key);
        }
        
        public long getExecutionTime() {
            return System.currentTimeMillis() - startTime;
        }
    }
    
    /**
     * Execution status enumeration
     */
    public enum ExecutionStatus {
        RUNNING, COMPLETED, FAILED, CANCELLED, TIMEOUT
    }
    
    /**
     * Resource pool for managing shared resources
     */
    public static class ResourcePool {
        private final String resourceType;
        private final BlockingQueue<Object> resources;
        private final int maxSize;
        private final AtomicInteger currentSize = new AtomicInteger(0);
        
        public ResourcePool(String resourceType, int maxSize) {
            this.resourceType = resourceType;
            this.maxSize = maxSize;
            this.resources = new ArrayBlockingQueue<>(maxSize);
        }
        
        public Object acquireResource(long timeout, TimeUnit unit) throws InterruptedException {
            return resources.poll(timeout, unit);
        }
        
        public void releaseResource(Object resource) {
            if (!resources.offer(resource)) {
                logger.warn("Failed to return resource to pool: {}", resourceType);
            }
        }
        
        public void addResource(Object resource) {
            if (currentSize.get() < maxSize && resources.offer(resource)) {
                currentSize.incrementAndGet();
            }
        }
        
        public int getAvailableCount() {
            return resources.size();
        }
        
        public int getTotalCount() {
            return currentSize.get();
        }
    }
    
    /**
     * Test execution task
     */
    public interface ParallelTestTask {
        void execute(ThreadContext context) throws Exception;
    }
    
    /**
     * Test completion callback
     */
    public interface TestCompletionCallback {
        void onSuccess(ThreadContext context);
        void onFailure(ThreadContext context, Exception exception);
        void onTimeout(ThreadContext context);
    }
    
    /**
     * Private constructor for singleton
     */
    private ParallelExecutionManager() {
        initializeConfiguration();
        initializeExecutors();
        initializeResourcePools();
    }
    
    /**
     * Get singleton instance
     * 
     * @return ParallelExecutionManager instance
     */
    public static ParallelExecutionManager getInstance() {
        if (instance == null) {
            synchronized (instanceLock) {
                if (instance == null) {
                    instance = new ParallelExecutionManager();
                    logger.info("ParallelExecutionManager initialized");
                }
            }
        }
        return instance;
    }
    
    /**
     * Initialize configuration from properties
     */
    private void initializeConfiguration() {
        maxThreads = ConfigurationManager.getIntProperty("parallel.max.threads", 
            Math.max(4, Runtime.getRuntime().availableProcessors() * 2));
        coreThreads = ConfigurationManager.getIntProperty("parallel.core.threads", 
            Math.max(2, Runtime.getRuntime().availableProcessors()));
        threadKeepAliveTime = ConfigurationManager.getLongProperty("parallel.thread.keepalive.seconds", 60L);
        enableResourcePooling = ConfigurationManager.getBooleanProperty("parallel.resource.pooling.enabled", true);
        enableDistributedExecution = ConfigurationManager.getBooleanProperty("parallel.distributed.enabled", false);
        
        logger.info("Parallel execution configuration - Core: {}, Max: {}, KeepAlive: {}s", 
            coreThreads, maxThreads, threadKeepAliveTime);
    }
    
    /**
     * Initialize executor services
     */
    private void initializeExecutors() {
        // Main thread pool for test execution
        ThreadFactory mainThreadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "Phoenix-Test-" + threadNumber.getAndIncrement());
                t.setDaemon(false);
                t.setPriority(Thread.NORM_PRIORITY);
                return t;
            }
        };
        
        mainExecutorService = new ThreadPoolExecutor(
            coreThreads,
            maxThreads,
            threadKeepAliveTime,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            mainThreadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // Scheduled executor for timeouts and monitoring
        scheduledExecutorService = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "Phoenix-Scheduler");
            t.setDaemon(true);
            return t;
        });
        
        // Callback executor for completion handlers
        callbackExecutorService = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "Phoenix-Callback");
            t.setDaemon(true);
            return t;
        });
        
        // Start monitoring
        startPerformanceMonitoring();
    }
    
    /**
     * Initialize resource pools
     */
    private void initializeResourcePools() {
        if (enableResourcePooling) {
            // WebDriver pool
            createResourcePool("webdriver", ConfigurationManager.getIntProperty("parallel.webdriver.pool.size", maxThreads));
            
            // Database connection pool
            createResourcePool("database", ConfigurationManager.getIntProperty("parallel.database.pool.size", maxThreads * 2));
            
            // API client pool
            createResourcePool("apiclient", ConfigurationManager.getIntProperty("parallel.apiclient.pool.size", maxThreads));
            
            logger.info("Resource pools initialized: webdriver, database, apiclient");
        }
    }
    
    /**
     * Create a resource pool
     * 
     * @param resourceType Resource type name
     * @param maxSize Maximum pool size
     */
    public void createResourcePool(String resourceType, int maxSize) {
        ResourcePool pool = new ResourcePool(resourceType, maxSize);
        resourcePools.put(resourceType, pool);
        logger.debug("Created resource pool: {} with max size: {}", resourceType, maxSize);
    }
    
    /**
     * Execute test task in parallel
     * 
     * @param testName Test name for identification
     * @param task Task to execute
     * @param callback Completion callback (optional)
     * @param timeoutSeconds Timeout in seconds (0 for no timeout)
     * @return Future representing the execution
     */
    public Future<ThreadContext> executeParallel(String testName, ParallelTestTask task, 
                                                 TestCompletionCallback callback, long timeoutSeconds) {
        
        logger.debug("Submitting parallel test execution: {}", testName);
        
        return mainExecutorService.submit(() -> {
            ThreadContext context = new ThreadContext(testName);
            threadContexts.put(context.getThreadId(), context);
            activeThreads.incrementAndGet();
            
            try {
                TestReporter.logInfo("Starting parallel test execution: " + testName);
                
                // Set up timeout if specified
                Future<?> timeoutFuture = null;
                if (timeoutSeconds > 0) {
                    timeoutFuture = scheduledExecutorService.schedule(() -> {
                        context.setStatus(ExecutionStatus.TIMEOUT);
                        logger.warn("Test execution timeout: {} after {} seconds", testName, timeoutSeconds);
                        if (callback != null) {
                            callbackExecutorService.submit(() -> callback.onTimeout(context));
                        }
                    }, timeoutSeconds, TimeUnit.SECONDS);
                }
                
                // Execute the task
                long startTime = System.currentTimeMillis();
                task.execute(context);
                long executionTime = System.currentTimeMillis() - startTime;
                
                // Cancel timeout if task completed
                if (timeoutFuture != null && !timeoutFuture.isDone()) {
                    timeoutFuture.cancel(false);
                }
                
                // Update context and statistics
                if (context.getStatus() == ExecutionStatus.RUNNING) {
                    context.setStatus(ExecutionStatus.COMPLETED);
                    completedTasks.incrementAndGet();
                    totalExecutionTime.addAndGet(executionTime);
                    
                    TestReporter.logPass("Parallel test execution completed: " + testName + 
                        " in " + executionTime + "ms");
                    
                    // Execute success callback
                    if (callback != null) {
                        callbackExecutorService.submit(() -> callback.onSuccess(context));
                    }
                }
                
            } catch (Exception e) {
                context.setStatus(ExecutionStatus.FAILED);
                failedTasks.incrementAndGet();
                
                logger.error("Parallel test execution failed: {} - {}", testName, e.getMessage());
                TestReporter.logFail("Parallel test execution failed: " + testName + " - " + e.getMessage());
                
                // Execute failure callback
                if (callback != null) {
                    callbackExecutorService.submit(() -> callback.onFailure(context, e));
                }
                
                throw e;
                
            } finally {
                activeThreads.decrementAndGet();
                threadContexts.remove(context.getThreadId());
            }
            
            return context;
        });
    }
    
    /**
     * Execute test task in parallel with default timeout
     * 
     * @param testName Test name
     * @param task Task to execute
     * @return Future representing the execution
     */
    public Future<ThreadContext> executeParallel(String testName, ParallelTestTask task) {
        long defaultTimeout = ConfigurationManager.getLongProperty("parallel.default.timeout.seconds", 300L);
        return executeParallel(testName, task, null, defaultTimeout);
    }
    
    /**
     * Execute multiple tests in parallel
     * 
     * @param tasks Map of test name to task
     * @param callback Completion callback (optional)
     * @param timeoutSeconds Timeout per task in seconds
     * @return List of futures representing executions
     */
    public List<Future<ThreadContext>> executeParallelBatch(Map<String, ParallelTestTask> tasks,
                                                            TestCompletionCallback callback, long timeoutSeconds) {
        
        logger.info("Executing parallel batch with {} tests", tasks.size());
        TestReporter.logInfo("Starting parallel batch execution: " + tasks.size() + " tests");
        
        List<Future<ThreadContext>> futures = new ArrayList<>();
        
        for (Map.Entry<String, ParallelTestTask> entry : tasks.entrySet()) {
            Future<ThreadContext> future = executeParallel(entry.getKey(), entry.getValue(), callback, timeoutSeconds);
            futures.add(future);
        }
        
        return futures;
    }
    
    /**
     * Wait for all parallel executions to complete
     * 
     * @param futures List of futures to wait for
     * @param timeoutSeconds Total timeout in seconds
     * @return Batch execution results
     */
    public BatchExecutionResult waitForBatchCompletion(List<Future<ThreadContext>> futures, long timeoutSeconds) {
        
        logger.info("Waiting for batch completion: {} tasks with timeout: {} seconds", futures.size(), timeoutSeconds);
        
        BatchExecutionResult result = new BatchExecutionResult();
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (timeoutSeconds * 1000);
        
        for (Future<ThreadContext> future : futures) {
            try {
                long remainingTime = Math.max(1000, endTime - System.currentTimeMillis());
                ThreadContext context = future.get(remainingTime, TimeUnit.MILLISECONDS);
                result.addResult(context);
                
            } catch (TimeoutException e) {
                logger.warn("Task execution timeout in batch");
                result.addTimeout();
                future.cancel(true);
                
            } catch (ExecutionException e) {
                logger.error("Task execution failed in batch: {}", e.getMessage());
                result.addFailure(e.getCause());
                
            } catch (InterruptedException e) {
                logger.warn("Batch execution interrupted");
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        result.setTotalExecutionTime(totalTime);
        
        logger.info("Batch execution completed: {} successful, {} failed, {} timeout in {}ms",
            result.getSuccessCount(), result.getFailureCount(), result.getTimeoutCount(), totalTime);
        
        TestReporter.logInfo("Parallel batch execution completed: " + result.getSummary());
        
        return result;
    }
    
    /**
     * Acquire resource from pool
     * 
     * @param resourceType Resource type
     * @param timeoutSeconds Timeout in seconds
     * @return Resource object or null if timeout
     */
    public Object acquireResource(String resourceType, long timeoutSeconds) {
        ResourcePool pool = resourcePools.get(resourceType);
        if (pool == null) {
            logger.warn("Resource pool not found: {}", resourceType);
            return null;
        }
        
        try {
            Object resource = pool.acquireResource(timeoutSeconds, TimeUnit.SECONDS);
            if (resource == null) {
                logger.warn("Resource acquisition timeout: {} after {} seconds", resourceType, timeoutSeconds);
            } else {
                logger.debug("Resource acquired: {} from pool: {}", resource, resourceType);
            }
            return resource;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Resource acquisition interrupted: {}", resourceType);
            return null;
        }
    }
    
    /**
     * Release resource back to pool
     * 
     * @param resourceType Resource type
     * @param resource Resource to release
     */
    public void releaseResource(String resourceType, Object resource) {
        ResourcePool pool = resourcePools.get(resourceType);
        if (pool != null && resource != null) {
            pool.releaseResource(resource);
            logger.debug("Resource released: {} to pool: {}", resource, resourceType);
        }
    }
    
    /**
     * Add resource to pool
     * 
     * @param resourceType Resource type
     * @param resource Resource to add
     */
    public void addResourceToPool(String resourceType, Object resource) {
        ResourcePool pool = resourcePools.get(resourceType);
        if (pool != null) {
            pool.addResource(resource);
            logger.debug("Resource added to pool: {} - {}", resourceType, resource);
        }
    }
    
    /**
     * Get current execution statistics
     * 
     * @return Execution statistics map
     */
    public Map<String, Object> getExecutionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Thread statistics
        stats.put("activeThreads", activeThreads.get());
        stats.put("completedTasks", completedTasks.get());
        stats.put("failedTasks", failedTasks.get());
        stats.put("totalTasks", completedTasks.get() + failedTasks.get());
        stats.put("successRate", calculateSuccessRate());
        
        // Performance statistics
        stats.put("totalExecutionTimeMs", totalExecutionTime.get());
        stats.put("averageExecutionTimeMs", calculateAverageExecutionTime());
        
        // Resource pool statistics
        Map<String, Object> resourceStats = new HashMap<>();
        for (Map.Entry<String, ResourcePool> entry : resourcePools.entrySet()) {
            ResourcePool pool = entry.getValue();
            Map<String, Integer> poolStats = new HashMap<>();
            poolStats.put("available", pool.getAvailableCount());
            poolStats.put("total", pool.getTotalCount());
            resourceStats.put(entry.getKey(), poolStats);
        }
        stats.put("resourcePools", resourceStats);
        
        // Thread pool statistics
        if (mainExecutorService instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) mainExecutorService;
            Map<String, Object> threadPoolStats = new HashMap<>();
            threadPoolStats.put("corePoolSize", tpe.getCorePoolSize());
            threadPoolStats.put("maximumPoolSize", tpe.getMaximumPoolSize());
            threadPoolStats.put("activeCount", tpe.getActiveCount());
            threadPoolStats.put("queueSize", tpe.getQueue().size());
            threadPoolStats.put("completedTaskCount", tpe.getCompletedTaskCount());
            stats.put("threadPool", threadPoolStats);
        }
        
        return stats;
    }
    
    /**
     * Start performance monitoring
     */
    private void startPerformanceMonitoring() {
        int monitoringInterval = ConfigurationManager.getIntProperty("parallel.monitoring.interval.seconds", 30);
        
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                Map<String, Object> stats = getExecutionStatistics();
                logger.info("Parallel execution stats: Active={}, Completed={}, Failed={}, Success Rate={}%",
                    stats.get("activeThreads"), stats.get("completedTasks"), 
                    stats.get("failedTasks"), String.format("%.1f", (Double) stats.get("successRate")));
                
            } catch (Exception e) {
                logger.warn("Error in performance monitoring: {}", e.getMessage());
            }
        }, monitoringInterval, monitoringInterval, TimeUnit.SECONDS);
    }
    
    /**
     * Calculate success rate percentage
     * 
     * @return Success rate as percentage
     */
    private double calculateSuccessRate() {
        int total = completedTasks.get() + failedTasks.get();
        return total > 0 ? (completedTasks.get() * 100.0 / total) : 0.0;
    }
    
    /**
     * Calculate average execution time
     * 
     * @return Average execution time in milliseconds
     */
    private long calculateAverageExecutionTime() {
        int completed = completedTasks.get();
        return completed > 0 ? (totalExecutionTime.get() / completed) : 0L;
    }
    
    /**
     * Get current active thread contexts
     * 
     * @return Map of thread ID to context
     */
    public Map<Long, ThreadContext> getActiveThreadContexts() {
        return new HashMap<>(threadContexts);
    }
    
    /**
     * Shutdown execution manager gracefully
     * 
     * @param timeoutSeconds Timeout for graceful shutdown
     */
    public void shutdown(long timeoutSeconds) {
        logger.info("Shutting down ParallelExecutionManager with timeout: {} seconds", timeoutSeconds);
        
        try {
            // Shutdown main executor
            mainExecutorService.shutdown();
            if (!mainExecutorService.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                logger.warn("Main executor did not terminate gracefully, forcing shutdown");
                mainExecutorService.shutdownNow();
            }
            
            // Shutdown scheduled executor
            scheduledExecutorService.shutdown();
            if (!scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutorService.shutdownNow();
            }
            
            // Shutdown callback executor
            callbackExecutorService.shutdown();
            if (!callbackExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
                callbackExecutorService.shutdownNow();
            }
            
            logger.info("ParallelExecutionManager shutdown completed");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Shutdown interrupted");
            
            // Force shutdown
            mainExecutorService.shutdownNow();
            scheduledExecutorService.shutdownNow();
            callbackExecutorService.shutdownNow();
        }
        
        // Reset singleton
        synchronized (instanceLock) {
            instance = null;
        }
    }
    
    /**
     * Emergency shutdown (immediate)
     */
    public void forceShutdown() {
        logger.warn("Force shutdown initiated");
        
        mainExecutorService.shutdownNow();
        scheduledExecutorService.shutdownNow();
        callbackExecutorService.shutdownNow();
        
        // Reset singleton
        synchronized (instanceLock) {
            instance = null;
        }
    }
}