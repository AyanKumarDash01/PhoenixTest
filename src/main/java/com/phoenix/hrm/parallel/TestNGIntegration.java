package com.phoenix.hrm.parallel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.*;
import org.testng.annotations.Test;
import org.testng.internal.ConstructorOrMethod;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TestNG Integration for Phoenix HRM Parallel Test Execution Framework
 * 
 * Provides seamless integration with TestNG for:
 * - Parallel test execution management
 * - Custom test listeners for parallel execution monitoring
 * - Test result aggregation and reporting
 * - Resource management for test methods
 * - Thread-safe test data management
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 3.0
 * @since Phase 3
 */
public class TestNGIntegration {
    
    private static final Logger logger = LoggerFactory.getLogger(TestNGIntegration.class);
    
    /**
     * Custom TestNG listener for parallel execution monitoring
     */
    public static class ParallelExecutionListener implements ITestListener, ISuiteListener, IInvokedMethodListener {
        
        private final ParallelExecutionManager executionManager;
        private final ThreadSafeUtils.ExecutionTracker executionTracker;
        private final ThreadSafeUtils.SafeRegistry<TestExecutionContext> testRegistry;
        private final AtomicInteger passedTests = new AtomicInteger(0);
        private final AtomicInteger failedTests = new AtomicInteger(0);
        private final AtomicInteger skippedTests = new AtomicInteger(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        
        public ParallelExecutionListener() {
            this.executionManager = ParallelExecutionManager.getInstance();
            this.executionTracker = new ThreadSafeUtils.ExecutionTracker();
            this.testRegistry = new ThreadSafeUtils.SafeRegistry<>("TestRegistry");
        }
        
        @Override
        public void onStart(ISuite suite) {
            logger.info("Suite started: {} - Parallel execution enabled", suite.getName());
            
            // Configure parallel execution based on suite parameters
            Map<String, String> parameters = suite.getXmlSuite().getParameters();
            configureParallelExecution(parameters);
        }
        
        @Override
        public void onFinish(ISuite suite) {
            logger.info("Suite finished: {} - Generating parallel execution report", suite.getName());
            generateSuiteReport(suite);
        }
        
        @Override
        public void onTestStart(ITestResult result) {
            String testName = getTestName(result);
            String threadName = Thread.currentThread().getName();
            
            TestExecutionContext context = new TestExecutionContext(testName, threadName, System.currentTimeMillis());
            testRegistry.register(testName, context);
            
            logger.debug("Test started: {} on thread: {}", testName, threadName);
        }
        
        @Override
        public void onTestSuccess(ITestResult result) {
            handleTestCompletion(result, "PASSED");
            passedTests.incrementAndGet();
        }
        
        @Override
        public void onTestFailure(ITestResult result) {
            handleTestCompletion(result, "FAILED");
            failedTests.incrementAndGet();
            
            // Log failure details
            logger.error("Test failed: {} - {}", getTestName(result), result.getThrowable().getMessage());
        }
        
        @Override
        public void onTestSkipped(ITestResult result) {
            handleTestCompletion(result, "SKIPPED");
            skippedTests.incrementAndGet();
        }
        
        @Override
        public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
            if (method.isTestMethod()) {
                String methodName = method.getTestMethod().getMethodName();
                logger.debug("Invoking test method: {} on thread: {}", methodName, Thread.currentThread().getName());
            }
        }
        
        @Override
        public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
            if (method.isTestMethod()) {
                String methodName = method.getTestMethod().getMethodName();
                long duration = testResult.getEndMillis() - testResult.getStartMillis();
                executionTracker.recordExecution(methodName, duration);
                totalExecutionTime.addAndGet(duration);
                
                logger.debug("Completed test method: {} in {}ms on thread: {}", 
                    methodName, duration, Thread.currentThread().getName());
            }
        }
        
        private void handleTestCompletion(ITestResult result, String status) {
            String testName = getTestName(result);
            TestExecutionContext context = testRegistry.get(testName);
            
            if (context != null) {
                context.setEndTime(System.currentTimeMillis());
                context.setStatus(status);
                context.setResult(result);
            }
        }
        
        private String getTestName(ITestResult result) {
            return result.getTestClass().getName() + "." + result.getMethod().getMethodName();
        }
        
        private void configureParallelExecution(Map<String, String> parameters) {
            // Configure thread pool size
            String threadCount = parameters.get("parallel.thread.count");
            if (threadCount != null) {
                try {
                    int count = Integer.parseInt(threadCount);
                    logger.info("Configuring parallel execution with {} threads", count);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid thread count parameter: {}", threadCount);
                }
            }
            
            // Configure timeout
            String timeout = parameters.get("parallel.test.timeout");
            if (timeout != null) {
                try {
                    long timeoutMs = Long.parseLong(timeout);
                    logger.info("Configuring test timeout: {}ms", timeoutMs);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid timeout parameter: {}", timeout);
                }
            }
        }
        
        private void generateSuiteReport(ISuite suite) {
            Map<String, TestExecutionContext> allTests = testRegistry.snapshot();
            Map<String, ThreadSafeUtils.ExecutionTracker.ExecutionMetrics> allMetrics = executionTracker.getAllMetrics();
            
            logger.info("=== Parallel Execution Report for Suite: {} ===", suite.getName());
            logger.info("Total Tests: {}", allTests.size());
            logger.info("Passed: {}, Failed: {}, Skipped: {}", passedTests.get(), failedTests.get(), skippedTests.get());
            logger.info("Total Execution Time: {}ms", totalExecutionTime.get());
            
            // Thread distribution analysis
            Map<String, Integer> threadDistribution = new HashMap<>();
            for (TestExecutionContext context : allTests.values()) {
                threadDistribution.merge(context.getThreadName(), 1, Integer::sum);
            }
            
            logger.info("Thread Distribution:");
            for (Map.Entry<String, Integer> entry : threadDistribution.entrySet()) {
                logger.info("  {}: {} tests", entry.getKey(), entry.getValue());
            }
            
            // Performance metrics
            if (!allMetrics.isEmpty()) {
                logger.info("Performance Metrics:");
                for (Map.Entry<String, ThreadSafeUtils.ExecutionTracker.ExecutionMetrics> entry : allMetrics.entrySet()) {
                    ThreadSafeUtils.ExecutionTracker.ExecutionMetrics metrics = entry.getValue();
                    logger.info("  {}: avg={}ms, min={}ms, max={}ms", 
                        entry.getKey(), metrics.getAverageDuration(), metrics.getMinDuration(), metrics.getMaxDuration());
                }
            }
        }
        
        public Map<String, Object> getExecutionStatistics() {
            Map<String, Object> stats = new HashMap<>();
            stats.put("passedTests", passedTests.get());
            stats.put("failedTests", failedTests.get());
            stats.put("skippedTests", skippedTests.get());
            stats.put("totalExecutionTime", totalExecutionTime.get());
            stats.put("testRegistry", testRegistry.snapshot());
            stats.put("executionMetrics", executionTracker.getAllMetrics());
            return stats;
        }
    }
    
    /**
     * Test execution context for tracking test execution details
     */
    public static class TestExecutionContext {
        private final String testName;
        private final String threadName;
        private final long startTime;
        private long endTime;
        private String status;
        private ITestResult result;
        
        public TestExecutionContext(String testName, String threadName, long startTime) {
            this.testName = testName;
            this.threadName = threadName;
            this.startTime = startTime;
        }
        
        // Getters and setters
        public String getTestName() { return testName; }
        public String getThreadName() { return threadName; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public void setEndTime(long endTime) { this.endTime = endTime; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public ITestResult getResult() { return result; }
        public void setResult(ITestResult result) { this.result = result; }
        
        public long getDuration() {
            return endTime > 0 ? endTime - startTime : 0;
        }
        
        @Override
        public String toString() {
            return String.format("TestExecutionContext{testName='%s', threadName='%s', duration=%dms, status='%s'}", 
                testName, threadName, getDuration(), status);
        }
    }
    
    /**
     * Parallel test data provider for TestNG
     */
    public static class ParallelDataProvider {
        
        private final ThreadSafeUtils.SafeRegistry<Object[]> dataRegistry = 
            new ThreadSafeUtils.SafeRegistry<>("ParallelDataProvider");
        
        public Object[][] provideParallelData(String dataSetName, Object[]... dataSets) {
            // Register data sets for thread-safe access
            for (int i = 0; i < dataSets.length; i++) {
                dataRegistry.register(dataSetName + "_" + i, dataSets[i]);
            }
            
            return dataSets;
        }
        
        public Object[] getThreadSafeData(String key) {
            return dataRegistry.get(key);
        }
        
        public void clearData() {
            dataRegistry.clear();
        }
    }
    
    /**
     * Custom TestNG factory for parallel test creation
     */
    public static class ParallelTestFactory implements ITestNGMethod {
        
        private final Method testMethod;
        private final Object testInstance;
        private final String testName;
        private final ITestClass testClass;
        
        public ParallelTestFactory(Method testMethod, Object testInstance, String testName, ITestClass testClass) {
            this.testMethod = testMethod;
            this.testInstance = testInstance;
            this.testName = testName;
            this.testClass = testClass;
        }
        
        @Override
        public Class getRealClass() {
            return testInstance.getClass();
        }
        
        @Override
        public ITestClass getTestClass() {
            return testClass;
        }
        
        @Override
        public void setTestClass(ITestClass cls) {
            // Implementation if needed
        }
        
        @Override
        public Method getMethod() {
            return testMethod;
        }
        
        @Override
        public String getMethodName() {
            return testName;
        }
        
        @Override
        public Object[] getInstances() {
            return new Object[]{testInstance};
        }
        
        @Override
        public long[] getInstanceHashCodes() {
            return new long[]{testInstance.hashCode()};
        }
        
        @Override
        public String[] getGroups() {
            Test testAnnotation = testMethod.getAnnotation(Test.class);
            return testAnnotation != null ? testAnnotation.groups() : new String[0];
        }
        
        @Override
        public String[] getGroupsDependedUpon() {
            Test testAnnotation = testMethod.getAnnotation(Test.class);
            return testAnnotation != null ? testAnnotation.dependsOnGroups() : new String[0];
        }
        
        @Override
        public String getMissingGroup() {
            return null;
        }
        
        @Override
        public void setMissingGroup(String group) {
            // Implementation if needed
        }
        
        @Override
        public String[] getBeforeGroups() {
            return new String[0];
        }
        
        @Override
        public String[] getAfterGroups() {
            return new String[0];
        }
        
        @Override
        public String[] getMethodsDependedUpon() {
            Test testAnnotation = testMethod.getAnnotation(Test.class);
            return testAnnotation != null ? testAnnotation.dependsOnMethods() : new String[0];
        }
        
        @Override
        public void addMethodDependedUpon(String methodName) {
            // Implementation if needed
        }
        
        @Override
        public boolean isTest() {
            return testMethod.isAnnotationPresent(Test.class);
        }
        
        @Override
        public boolean isBeforeMethodConfiguration() {
            return false;
        }
        
        @Override
        public boolean isAfterMethodConfiguration() {
            return false;
        }
        
        @Override
        public boolean isBeforeClassConfiguration() {
            return false;
        }
        
        @Override
        public boolean isAfterClassConfiguration() {
            return false;
        }
        
        @Override
        public boolean isBeforeSuiteConfiguration() {
            return false;
        }
        
        @Override
        public boolean isAfterSuiteConfiguration() {
            return false;
        }
        
        @Override
        public boolean isBeforeTestConfiguration() {
            return false;
        }
        
        @Override
        public boolean isAfterTestConfiguration() {
            return false;
        }
        
        @Override
        public boolean isBeforeGroupsConfiguration() {
            return false;
        }
        
        @Override
        public boolean isAfterGroupsConfiguration() {
            return false;
        }
        
        @Override
        public long getTimeOut() {
            Test testAnnotation = testMethod.getAnnotation(Test.class);
            return testAnnotation != null ? testAnnotation.timeOut() : 0;
        }
        
        @Override
        public void setTimeOut(long timeOut) {
            // Implementation if needed
        }
        
        @Override
        public int getInvocationCount() {
            Test testAnnotation = testMethod.getAnnotation(Test.class);
            return testAnnotation != null ? testAnnotation.invocationCount() : 1;
        }
        
        @Override
        public void setInvocationCount(int count) {
            // Implementation if needed
        }
        
        @Override
        public int getSuccessPercentage() {
            Test testAnnotation = testMethod.getAnnotation(Test.class);
            return testAnnotation != null ? testAnnotation.successPercentage() : 100;
        }
        
        @Override
        public String getId() {
            return testName;
        }
        
        @Override
        public void setId(String id) {
            // Implementation if needed
        }
        
        @Override
        public long getDate() {
            return System.currentTimeMillis();
        }
        
        @Override
        public void setDate(long date) {
            // Implementation if needed
        }
        
        @Override
        public boolean canRunFromClass(IClass testClass) {
            return true;
        }
        
        @Override
        public boolean isAlwaysRun() {
            Test testAnnotation = testMethod.getAnnotation(Test.class);
            return testAnnotation != null && testAnnotation.alwaysRun();
        }
        
        @Override
        public int getThreadPoolSize() {
            Test testAnnotation = testMethod.getAnnotation(Test.class);
            return testAnnotation != null ? testAnnotation.threadPoolSize() : 1;
        }
        
        @Override
        public void setThreadPoolSize(int threadPoolSize) {
            // Implementation if needed
        }
        
        @Override
        public String getDescription() {
            Test testAnnotation = testMethod.getAnnotation(Test.class);
            return testAnnotation != null ? testAnnotation.description() : "";
        }
        
        @Override
        public void incrementCurrentInvocationCount() {
            // Implementation if needed
        }
        
        @Override
        public int getCurrentInvocationCount() {
            return 0;
        }
        
        @Override
        public void setParameterInvocationCount(int n) {
            // Implementation if needed
        }
        
        @Override
        public int getParameterInvocationCount() {
            return 0;
        }
        
        @Override
        public ITestNGMethod clone() {
            return new ParallelTestFactory(testMethod, testInstance, testName, testClass);
        }
        
        @Override
        public IRetryAnalyzer getRetryAnalyzer() {
            return null;
        }
        
        @Override
        public void setRetryAnalyzer(IRetryAnalyzer retryAnalyzer) {
            // Implementation if needed
        }
        
        @Override
        public boolean skipFailedInvocations() {
            Test testAnnotation = testMethod.getAnnotation(Test.class);
            return testAnnotation != null && testAnnotation.skipFailedInvocations();
        }
        
        @Override
        public void setSkipFailedInvocations(boolean skip) {
            // Implementation if needed
        }
        
        @Override
        public long getInvocationTimeOut() {
            return getTimeOut();
        }
        
        @Override
        public boolean ignoreMissingDependencies() {
            Test testAnnotation = testMethod.getAnnotation(Test.class);
            return testAnnotation != null && testAnnotation.ignoreMissingDependencies();
        }
        
        @Override
        public void setIgnoreMissingDependencies(boolean ignore) {
            // Implementation if needed
        }
        
        @Override
        public List<Integer> getInvocationNumbers() {
            return Arrays.asList(0);
        }
        
        @Override
        public void setInvocationNumbers(List<Integer> numbers) {
            // Implementation if needed
        }
        
        @Override
        public void addFailedInvocationNumber(int number) {
            // Implementation if needed
        }
        
        @Override
        public List<Integer> getFailedInvocationNumbers() {
            return new ArrayList<>();
        }
        
        @Override
        public int getPriority() {
            Test testAnnotation = testMethod.getAnnotation(Test.class);
            return testAnnotation != null ? testAnnotation.priority() : 0;
        }
        
        @Override
        public void setPriority(int priority) {
            // Implementation if needed
        }
        
        @Override
        public int getInterceptedPriority() {
            return getPriority();
        }
        
        @Override
        public void setInterceptedPriority(int priority) {
            // Implementation if needed
        }
        
        @Override
        public XmlTest getXmlTest() {
            return null;
        }
        
        @Override
        public ConstructorOrMethod getConstructorOrMethod() {
            return new ConstructorOrMethod(testMethod);
        }
        
        @Override
        public Map<String, String> findMethodParameters(XmlTest test) {
            return new HashMap<>();
        }
        
        @Override
        public String getQualifiedName() {
            return testClass.getName() + "." + testName;
        }
    }
    
    /**
     * Utility methods for TestNG integration
     */
    public static class TestNGUtils {
        
        /**
         * Create a parallel test suite configuration
         */
        public static XmlSuite createParallelSuite(String suiteName, int threadCount) {
            XmlSuite suite = new XmlSuite();
            suite.setName(suiteName);
            suite.setParallel(XmlSuite.ParallelMode.METHODS);
            suite.setThreadCount(threadCount);
            suite.setPreserveOrder(false);
            suite.setGroupByInstances(true);
            
            // Add custom parameters
            suite.setParameter("parallel.thread.count", String.valueOf(threadCount));
            suite.setParameter("parallel.test.timeout", "60000"); // 60 seconds default
            
            return suite;
        }
        
        /**
         * Add a test class to the suite
         */
        public static void addTestClass(XmlSuite suite, Class<?> testClass) {
            XmlTest test = new XmlTest(suite);
            test.setName(testClass.getSimpleName() + "_Test");
            
            XmlClass xmlClass = new XmlClass(testClass);
            test.setXmlClasses(Arrays.asList(xmlClass));
        }
        
        /**
         * Run tests programmatically with parallel execution
         */
        public static TestListenerAdapter runParallelTests(XmlSuite suite) {
            TestNG testng = new TestNG();
            testng.setXmlSuites(Arrays.asList(suite));
            
            // Add parallel execution listener
            ParallelExecutionListener listener = new ParallelExecutionListener();
            testng.addListener(listener);
            
            TestListenerAdapter adapter = new TestListenerAdapter();
            testng.addListener(adapter);
            
            testng.run();
            
            return adapter;
        }
    }
}