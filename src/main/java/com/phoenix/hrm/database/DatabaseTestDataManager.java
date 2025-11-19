package com.phoenix.hrm.database;

import com.phoenix.hrm.reporting.TestReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Database Test Data Management Framework for Phoenix HRM Test Automation
 * 
 * Provides comprehensive test data management capabilities including:
 * - Test data creation and seeding
 * - Test data cleanup and rollback
 * - Data fixtures management
 * - Test isolation and data consistency
 * - Bulk data operations for testing
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 3.0
 * @since Phase 3
 */
public class DatabaseTestDataManager {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseTestDataManager.class);
    
    // Thread-safe storage for test data tracking
    private static final Map<String, Set<TestDataRecord>> testDataRegistry = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> currentTestContext = new ThreadLocal<>();
    
    /**
     * Test data record for tracking created data
     */
    public static class TestDataRecord {
        private String tableName;
        private String primaryKeyColumn;
        private Object primaryKeyValue;
        private LocalDateTime createdAt;
        private Map<String, Object> originalData;
        
        public TestDataRecord(String tableName, String primaryKeyColumn, Object primaryKeyValue) {
            this.tableName = tableName;
            this.primaryKeyColumn = primaryKeyColumn;
            this.primaryKeyValue = primaryKeyValue;
            this.createdAt = LocalDateTime.now();
            this.originalData = new HashMap<>();
        }
        
        // Getters and setters
        public String getTableName() { return tableName; }
        public String getPrimaryKeyColumn() { return primaryKeyColumn; }
        public Object getPrimaryKeyValue() { return primaryKeyValue; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public Map<String, Object> getOriginalData() { return originalData; }
        public void setOriginalData(Map<String, Object> originalData) { this.originalData = originalData; }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestDataRecord that = (TestDataRecord) obj;
            return Objects.equals(tableName, that.tableName) && 
                   Objects.equals(primaryKeyColumn, that.primaryKeyColumn) && 
                   Objects.equals(primaryKeyValue, that.primaryKeyValue);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(tableName, primaryKeyColumn, primaryKeyValue);
        }
        
        @Override
        public String toString() {
            return String.format("TestDataRecord{table=%s, key=%s=%s, created=%s}", 
                tableName, primaryKeyColumn, primaryKeyValue, createdAt);
        }
    }
    
    /**
     * Start test data context for current test
     * 
     * @param testContext Unique test context identifier
     */
    public static void startTestContext(String testContext) {
        currentTestContext.set(testContext);
        testDataRegistry.putIfAbsent(testContext, ConcurrentHashMap.newKeySet());
        
        logger.debug("Started test data context: {}", testContext);
        TestReporter.logInfo("Started test data management for context: " + testContext);
    }
    
    /**
     * Get current test context
     * 
     * @return Current test context or null if not set
     */
    public static String getCurrentTestContext() {
        return currentTestContext.get();
    }
    
    /**
     * Insert test data record and track for cleanup
     * 
     * @param tableName Table name
     * @param data Data to insert (column -> value mapping)
     * @param primaryKeyColumn Primary key column name
     * @return Generated primary key value
     * @throws SQLException if insert fails
     */
    public static Object insertTestData(String tableName, Map<String, Object> data, String primaryKeyColumn) throws SQLException {
        logger.debug("Inserting test data into table: {} with {} columns", tableName, data.size());
        TestReporter.logInfo("Inserting test data into " + tableName);
        
        // Build INSERT query
        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        List<Object> values = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (columns.length() > 0) {
                columns.append(", ");
                placeholders.append(", ");
            }
            columns.append(entry.getKey());
            placeholders.append("?");
            values.add(entry.getValue());
        }
        
        String query = String.format("INSERT INTO %s (%s) VALUES (%s)", 
            tableName, columns.toString(), placeholders.toString());
        
        // Execute insert and get generated key
        List<Long> generatedKeys = SqlExecutor.executeInsertWithGeneratedKeys(query, values.toArray());
        Object primaryKeyValue = generatedKeys.isEmpty() ? null : generatedKeys.get(0);
        
        // If no auto-generated key, try to get the primary key from provided data
        if (primaryKeyValue == null && data.containsKey(primaryKeyColumn)) {
            primaryKeyValue = data.get(primaryKeyColumn);
        }
        
        // Register test data for cleanup
        if (primaryKeyValue != null) {
            registerTestData(tableName, primaryKeyColumn, primaryKeyValue, data);
        }
        
        logger.debug("Test data inserted with primary key: {}", primaryKeyValue);
        TestReporter.logPass("Test data inserted successfully with key: " + primaryKeyValue);
        
        return primaryKeyValue;
    }
    
    /**
     * Update test data record and track original values
     * 
     * @param tableName Table name
     * @param updateData Data to update (column -> value mapping)
     * @param whereClause WHERE clause for update
     * @param whereParameters WHERE clause parameters
     * @return Number of affected rows
     * @throws SQLException if update fails
     */
    public static int updateTestData(String tableName, Map<String, Object> updateData, 
                                    String whereClause, Object... whereParameters) throws SQLException {
        logger.debug("Updating test data in table: {} with {} columns", tableName, updateData.size());
        TestReporter.logInfo("Updating test data in " + tableName);
        
        // Build UPDATE query
        StringBuilder setClause = new StringBuilder();
        List<Object> allParameters = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : updateData.entrySet()) {
            if (setClause.length() > 0) {
                setClause.append(", ");
            }
            setClause.append(entry.getKey()).append(" = ?");
            allParameters.add(entry.getValue());
        }
        
        // Add WHERE parameters
        Collections.addAll(allParameters, whereParameters);
        
        String query = String.format("UPDATE %s SET %s WHERE %s", 
            tableName, setClause.toString(), whereClause);
        
        int affectedRows = SqlExecutor.executeUpdate(query, allParameters.toArray());
        
        logger.debug("Test data updated: {} rows affected", affectedRows);
        TestReporter.logPass("Test data updated successfully: " + affectedRows + " rows affected");
        
        return affectedRows;
    }
    
    /**
     * Delete test data record
     * 
     * @param tableName Table name
     * @param whereClause WHERE clause for deletion
     * @param whereParameters WHERE clause parameters
     * @return Number of affected rows
     * @throws SQLException if deletion fails
     */
    public static int deleteTestData(String tableName, String whereClause, Object... whereParameters) throws SQLException {
        logger.debug("Deleting test data from table: {}", tableName);
        TestReporter.logInfo("Deleting test data from " + tableName);
        
        String query = String.format("DELETE FROM %s WHERE %s", tableName, whereClause);
        int affectedRows = SqlExecutor.executeUpdate(query, whereParameters);
        
        logger.debug("Test data deleted: {} rows affected", affectedRows);
        TestReporter.logPass("Test data deleted successfully: " + affectedRows + " rows affected");
        
        return affectedRows;
    }
    
    /**
     * Register test data for tracking and cleanup
     * 
     * @param tableName Table name
     * @param primaryKeyColumn Primary key column
     * @param primaryKeyValue Primary key value
     * @param originalData Original data values
     */
    private static void registerTestData(String tableName, String primaryKeyColumn, 
                                       Object primaryKeyValue, Map<String, Object> originalData) {
        String testContext = getCurrentTestContext();
        if (testContext == null) {
            testContext = "default";
            currentTestContext.set(testContext);
            testDataRegistry.putIfAbsent(testContext, ConcurrentHashMap.newKeySet());
        }
        
        TestDataRecord record = new TestDataRecord(tableName, primaryKeyColumn, primaryKeyValue);
        record.setOriginalData(new HashMap<>(originalData));
        
        testDataRegistry.get(testContext).add(record);
        logger.debug("Registered test data: {}", record);
    }
    
    /**
     * Cleanup all test data for current context
     * 
     * @throws SQLException if cleanup fails
     */
    public static void cleanupTestData() throws SQLException {
        String testContext = getCurrentTestContext();
        if (testContext != null) {
            cleanupTestData(testContext);
        }
    }
    
    /**
     * Cleanup test data for specific context
     * 
     * @param testContext Test context to cleanup
     * @throws SQLException if cleanup fails
     */
    public static void cleanupTestData(String testContext) throws SQLException {
        Set<TestDataRecord> records = testDataRegistry.get(testContext);
        if (records == null || records.isEmpty()) {
            logger.debug("No test data to cleanup for context: {}", testContext);
            return;
        }
        
        logger.info("Cleaning up {} test data records for context: {}", records.size(), testContext);
        TestReporter.logInfo("Cleaning up test data: " + records.size() + " records");
        
        int cleanedCount = 0;
        int errorCount = 0;
        
        // Sort records by creation time (cleanup in reverse order)
        List<TestDataRecord> sortedRecords = new ArrayList<>(records);
        sortedRecords.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        
        for (TestDataRecord record : sortedRecords) {
            try {
                String deleteQuery = String.format("DELETE FROM %s WHERE %s = ?", 
                    record.getTableName(), record.getPrimaryKeyColumn());
                
                int deletedRows = SqlExecutor.executeUpdate(deleteQuery, record.getPrimaryKeyValue());
                
                if (deletedRows > 0) {
                    cleanedCount++;
                    logger.debug("Cleaned up test data: {}", record);
                } else {
                    logger.warn("No rows deleted for test data: {}", record);
                }
                
            } catch (SQLException e) {
                errorCount++;
                logger.error("Error cleaning up test data {}: {}", record, e.getMessage());
                TestReporter.logWarn("Failed to cleanup test data: " + record + " - " + e.getMessage());
            }
        }
        
        // Clear the registry for this context
        testDataRegistry.remove(testContext);
        
        String summary = String.format("Test data cleanup completed: %d cleaned, %d errors", cleanedCount, errorCount);
        logger.info(summary);
        TestReporter.logInfo(summary);
        
        if (errorCount > 0) {
            throw new SQLException("Test data cleanup completed with " + errorCount + " errors");
        }
    }
    
    /**
     * Create employee test data
     * 
     * @param firstName Employee first name
     * @param lastName Employee last name
     * @param employeeId Employee ID (optional, can be null for auto-generation)
     * @return Created employee primary key
     * @throws SQLException if creation fails
     */
    public static Object createEmployeeTestData(String firstName, String lastName, String employeeId) throws SQLException {
        Map<String, Object> employeeData = new HashMap<>();
        employeeData.put("first_name", firstName);
        employeeData.put("last_name", lastName);
        
        if (employeeId != null) {
            employeeData.put("employee_id", employeeId);
        }
        
        employeeData.put("hire_date", new java.sql.Date(System.currentTimeMillis()));
        employeeData.put("status", "Active");
        employeeData.put("created_date", new java.sql.Timestamp(System.currentTimeMillis()));
        
        return insertTestData("hs_hr_employees", employeeData, "emp_number");
    }
    
    /**
     * Create user account test data
     * 
     * @param username Username
     * @param password Password
     * @param userRole User role
     * @param employeeNumber Associated employee number (can be null)
     * @return Created user primary key
     * @throws SQLException if creation fails
     */
    public static Object createUserTestData(String username, String password, String userRole, Object employeeNumber) throws SQLException {
        Map<String, Object> userData = new HashMap<>();
        userData.put("user_name", username);
        userData.put("user_password", password); // In real scenario, this should be hashed
        userData.put("user_role_id", getUserRoleId(userRole));
        userData.put("emp_number", employeeNumber);
        userData.put("created_date", new java.sql.Timestamp(System.currentTimeMillis()));
        userData.put("status", 1); // Active
        
        return insertTestData("ohrm_user", userData, "id");
    }
    
    /**
     * Create leave request test data
     * 
     * @param employeeNumber Employee number
     * @param leaveTypeId Leave type ID
     * @param fromDate Leave from date
     * @param toDate Leave to date
     * @param status Leave status
     * @return Created leave request primary key
     * @throws SQLException if creation fails
     */
    public static Object createLeaveTestData(Object employeeNumber, int leaveTypeId, 
                                           java.sql.Date fromDate, java.sql.Date toDate, String status) throws SQLException {
        Map<String, Object> leaveData = new HashMap<>();
        leaveData.put("emp_number", employeeNumber);
        leaveData.put("leave_type_id", leaveTypeId);
        leaveData.put("date_applied", new java.sql.Date(System.currentTimeMillis()));
        leaveData.put("leave_comments", "Test leave request");
        
        Object leaveRequestId = insertTestData("ohrm_leave", leaveData, "id");
        
        // Create leave details
        createLeaveDetailsTestData(leaveRequestId, fromDate, toDate, status);
        
        return leaveRequestId;
    }
    
    /**
     * Create leave details test data
     * 
     * @param leaveId Leave request ID
     * @param fromDate From date
     * @param toDate To date
     * @param status Status
     * @throws SQLException if creation fails
     */
    private static void createLeaveDetailsTestData(Object leaveId, java.sql.Date fromDate, 
                                                  java.sql.Date toDate, String status) throws SQLException {
        // Calculate number of days
        long diffInMillies = toDate.getTime() - fromDate.getTime();
        int days = (int) (diffInMillies / (1000 * 60 * 60 * 24)) + 1;
        
        for (int i = 0; i < days; i++) {
            Map<String, Object> detailData = new HashMap<>();
            detailData.put("leave_id", leaveId);
            detailData.put("leave_date", new java.sql.Date(fromDate.getTime() + (i * 24 * 60 * 60 * 1000L)));
            detailData.put("status", status);
            detailData.put("duration", 8.0); // Full day
            
            insertTestData("ohrm_leave_leave_entitlement", detailData, "id");
        }
    }
    
    /**
     * Get user role ID by role name
     * 
     * @param roleName Role name
     * @return Role ID
     * @throws SQLException if role lookup fails
     */
    private static int getUserRoleId(String roleName) throws SQLException {
        String query = "SELECT id FROM ohrm_user_role WHERE name = ?";
        Integer roleId = SqlExecutor.executeSingleValue(query, Integer.class, roleName);
        
        if (roleId == null) {
            // Create role if it doesn't exist
            Map<String, Object> roleData = new HashMap<>();
            roleData.put("name", roleName);
            roleData.put("display_name", roleName);
            roleData.put("is_assignable", 1);
            roleData.put("is_predefined", 0);
            
            Object newRoleId = insertTestData("ohrm_user_role", roleData, "id");
            return ((Number) newRoleId).intValue();
        }
        
        return roleId;
    }
    
    /**
     * Seed database with common test data
     * 
     * @throws SQLException if seeding fails
     */
    public static void seedCommonTestData() throws SQLException {
        logger.info("Seeding database with common test data");
        TestReporter.logInfo("Seeding database with common test data");
        
        // Create common leave types if they don't exist
        seedLeaveTypes();
        
        // Create common job titles if they don't exist
        seedJobTitles();
        
        // Create common departments if they don't exist
        seedDepartments();
        
        logger.info("Common test data seeding completed");
        TestReporter.logPass("Common test data seeding completed");
    }
    
    /**
     * Seed leave types
     * 
     * @throws SQLException if seeding fails
     */
    private static void seedLeaveTypes() throws SQLException {
        String[] leaveTypes = {"Annual", "Sick", "Maternity", "Paternity", "Personal"};
        
        for (String leaveType : leaveTypes) {
            // Check if leave type already exists
            String checkQuery = "SELECT COUNT(*) FROM ohrm_leave_type WHERE name = ?";
            long count = SqlExecutor.getRecordCount(checkQuery, leaveType);
            
            if (count == 0) {
                Map<String, Object> leaveTypeData = new HashMap<>();
                leaveTypeData.put("name", leaveType);
                leaveTypeData.put("deleted", 0);
                
                insertTestData("ohrm_leave_type", leaveTypeData, "id");
                logger.debug("Created leave type: {}", leaveType);
            }
        }
    }
    
    /**
     * Seed job titles
     * 
     * @throws SQLException if seeding fails
     */
    private static void seedJobTitles() throws SQLException {
        String[] jobTitles = {"Software Engineer", "QA Engineer", "Project Manager", "HR Manager", "System Admin"};
        
        for (String jobTitle : jobTitles) {
            String checkQuery = "SELECT COUNT(*) FROM ohrm_job_title WHERE job_title = ?";
            long count = SqlExecutor.getRecordCount(checkQuery, jobTitle);
            
            if (count == 0) {
                Map<String, Object> jobTitleData = new HashMap<>();
                jobTitleData.put("job_title", jobTitle);
                jobTitleData.put("job_description", "Test " + jobTitle + " description");
                jobTitleData.put("is_deleted", 0);
                
                insertTestData("ohrm_job_title", jobTitleData, "id");
                logger.debug("Created job title: {}", jobTitle);
            }
        }
    }
    
    /**
     * Seed departments
     * 
     * @throws SQLException if seeding fails
     */
    private static void seedDepartments() throws SQLException {
        String[] departments = {"Engineering", "Quality Assurance", "Human Resources", "Administration"};
        
        for (String department : departments) {
            String checkQuery = "SELECT COUNT(*) FROM ohrm_subunit WHERE name = ?";
            long count = SqlExecutor.getRecordCount(checkQuery, department);
            
            if (count == 0) {
                Map<String, Object> deptData = new HashMap<>();
                deptData.put("name", department);
                deptData.put("unit_id", "DEPT_" + department.toUpperCase().replace(" ", "_"));
                deptData.put("description", "Test " + department + " department");
                
                insertTestData("ohrm_subunit", deptData, "id");
                logger.debug("Created department: {}", department);
            }
        }
    }
    
    /**
     * Get all registered test data for current context
     * 
     * @return Set of test data records
     */
    public static Set<TestDataRecord> getRegisteredTestData() {
        String testContext = getCurrentTestContext();
        return testContext != null ? testDataRegistry.getOrDefault(testContext, Collections.emptySet()) : Collections.emptySet();
    }
    
    /**
     * End test data context and cleanup
     * 
     * @throws SQLException if cleanup fails
     */
    public static void endTestContext() throws SQLException {
        try {
            cleanupTestData();
        } finally {
            currentTestContext.remove();
        }
    }
    
    /**
     * Get test data statistics for current context
     * 
     * @return Map containing statistics
     */
    public static Map<String, Object> getTestDataStatistics() {
        String testContext = getCurrentTestContext();
        Set<TestDataRecord> records = testContext != null ? 
            testDataRegistry.getOrDefault(testContext, Collections.emptySet()) : Collections.emptySet();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("testContext", testContext);
        stats.put("totalRecords", records.size());
        
        // Group by table
        Map<String, Long> tableStats = new HashMap<>();
        for (TestDataRecord record : records) {
            tableStats.merge(record.getTableName(), 1L, Long::sum);
        }
        stats.put("recordsByTable", tableStats);
        
        // Age statistics
        if (!records.isEmpty()) {
            LocalDateTime oldestRecord = records.stream()
                .map(TestDataRecord::getCreatedAt)
                .min(LocalDateTime::compareTo)
                .orElse(null);
            stats.put("oldestRecordAge", oldestRecord);
        }
        
        return stats;
    }
}