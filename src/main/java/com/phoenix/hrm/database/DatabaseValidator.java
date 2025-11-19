package com.phoenix.hrm.database;

import com.phoenix.hrm.reporting.TestReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Database Data Validation Framework for Phoenix HRM Test Automation
 * 
 * Provides comprehensive database validation capabilities including:
 * - Data existence validation
 * - Data integrity checks
 * - Value range validation
 * - Pattern matching validation
 * - Cross-table relationship validation
 * - Data consistency checks
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 3.0
 * @since Phase 3
 */
public class DatabaseValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseValidator.class);
    
    /**
     * Validation result class to hold validation outcomes
     */
    public static class ValidationResult {
        private boolean passed;
        private String message;
        private Object actualValue;
        private Object expectedValue;
        private String validationType;
        
        public ValidationResult(boolean passed, String message, String validationType) {
            this.passed = passed;
            this.message = message;
            this.validationType = validationType;
        }
        
        public ValidationResult(boolean passed, String message, Object actualValue, Object expectedValue, String validationType) {
            this.passed = passed;
            this.message = message;
            this.actualValue = actualValue;
            this.expectedValue = expectedValue;
            this.validationType = validationType;
        }
        
        // Getters
        public boolean isPassed() { return passed; }
        public String getMessage() { return message; }
        public Object getActualValue() { return actualValue; }
        public Object getExpectedValue() { return expectedValue; }
        public String getValidationType() { return validationType; }
        
        @Override
        public String toString() {
            return String.format("ValidationResult{type=%s, passed=%s, message='%s'}", 
                validationType, passed, message);
        }
    }
    
    /**
     * Validate that a record exists in the database
     * 
     * @param tableName Table name
     * @param whereCondition WHERE condition
     * @param parameters Query parameters
     * @return Validation result
     */
    public static ValidationResult validateRecordExists(String tableName, String whereCondition, Object... parameters) {
        logger.debug("Validating record exists in table: {} with condition: {}", tableName, whereCondition);
        TestReporter.logInfo("Validating record existence in " + tableName);
        
        try {
            String query = String.format("SELECT COUNT(*) FROM %s WHERE %s", tableName, whereCondition);
            long count = SqlExecutor.getRecordCount(query, parameters);
            
            if (count > 0) {
                String message = String.format("Record exists in table %s (found %d records)", tableName, count);
                logger.debug(message);
                TestReporter.logPass(message);
                return new ValidationResult(true, message, "RECORD_EXISTS");
            } else {
                String message = String.format("No records found in table %s with condition: %s", tableName, whereCondition);
                logger.warn(message);
                TestReporter.logFail(message);
                return new ValidationResult(false, message, "RECORD_EXISTS");
            }
            
        } catch (SQLException e) {
            String message = String.format("Error validating record existence: %s", e.getMessage());
            logger.error(message);
            TestReporter.logFail(message);
            return new ValidationResult(false, message, "RECORD_EXISTS");
        }
    }
    
    /**
     * Validate that a record does not exist in the database
     * 
     * @param tableName Table name
     * @param whereCondition WHERE condition
     * @param parameters Query parameters
     * @return Validation result
     */
    public static ValidationResult validateRecordNotExists(String tableName, String whereCondition, Object... parameters) {
        logger.debug("Validating record does not exist in table: {} with condition: {}", tableName, whereCondition);
        TestReporter.logInfo("Validating record does not exist in " + tableName);
        
        try {
            String query = String.format("SELECT COUNT(*) FROM %s WHERE %s", tableName, whereCondition);
            long count = SqlExecutor.getRecordCount(query, parameters);
            
            if (count == 0) {
                String message = String.format("Record correctly does not exist in table %s", tableName);
                logger.debug(message);
                TestReporter.logPass(message);
                return new ValidationResult(true, message, "RECORD_NOT_EXISTS");
            } else {
                String message = String.format("Unexpected records found in table %s (found %d records)", tableName, count);
                logger.warn(message);
                TestReporter.logFail(message);
                return new ValidationResult(false, message, "RECORD_NOT_EXISTS");
            }
            
        } catch (SQLException e) {
            String message = String.format("Error validating record non-existence: %s", e.getMessage());
            logger.error(message);
            TestReporter.logFail(message);
            return new ValidationResult(false, message, "RECORD_NOT_EXISTS");
        }
    }
    
    /**
     * Validate field value in database
     * 
     * @param tableName Table name
     * @param columnName Column name
     * @param expectedValue Expected value
     * @param whereCondition WHERE condition
     * @param parameters Query parameters
     * @return Validation result
     */
    public static ValidationResult validateFieldValue(String tableName, String columnName, Object expectedValue, 
                                                     String whereCondition, Object... parameters) {
        logger.debug("Validating field value in {}.{} = {}", tableName, columnName, expectedValue);
        TestReporter.logInfo(String.format("Validating field %s.%s equals %s", tableName, columnName, expectedValue));
        
        try {
            String query = String.format("SELECT %s FROM %s WHERE %s", columnName, tableName, whereCondition);
            Object actualValue = SqlExecutor.executeSingleValue(query, Object.class, parameters);
            
            if (Objects.equals(actualValue, expectedValue)) {
                String message = String.format("Field %s.%s has expected value: %s", tableName, columnName, expectedValue);
                logger.debug(message);
                TestReporter.logPass(message);
                return new ValidationResult(true, message, actualValue, expectedValue, "FIELD_VALUE");
            } else {
                String message = String.format("Field %s.%s has unexpected value. Expected: %s, Actual: %s", 
                    tableName, columnName, expectedValue, actualValue);
                logger.warn(message);
                TestReporter.logFail(message);
                return new ValidationResult(false, message, actualValue, expectedValue, "FIELD_VALUE");
            }
            
        } catch (SQLException e) {
            String message = String.format("Error validating field value: %s", e.getMessage());
            logger.error(message);
            TestReporter.logFail(message);
            return new ValidationResult(false, message, "FIELD_VALUE");
        }
    }
    
    /**
     * Validate field value is within expected range
     * 
     * @param tableName Table name
     * @param columnName Column name
     * @param minValue Minimum expected value
     * @param maxValue Maximum expected value
     * @param whereCondition WHERE condition
     * @param parameters Query parameters
     * @return Validation result
     */
    public static ValidationResult validateFieldRange(String tableName, String columnName, Comparable minValue, 
                                                     Comparable maxValue, String whereCondition, Object... parameters) {
        logger.debug("Validating field range in {}.{} between {} and {}", tableName, columnName, minValue, maxValue);
        TestReporter.logInfo(String.format("Validating field %s.%s is between %s and %s", 
            tableName, columnName, minValue, maxValue));
        
        try {
            String query = String.format("SELECT %s FROM %s WHERE %s", columnName, tableName, whereCondition);
            Object actualValue = SqlExecutor.executeSingleValue(query, Object.class, parameters);
            
            if (actualValue instanceof Comparable) {
                @SuppressWarnings("unchecked")
                Comparable<Object> comparableValue = (Comparable<Object>) actualValue;
                
                if (comparableValue.compareTo(minValue) >= 0 && comparableValue.compareTo(maxValue) <= 0) {
                    String message = String.format("Field %s.%s is within expected range [%s, %s]: %s", 
                        tableName, columnName, minValue, maxValue, actualValue);
                    logger.debug(message);
                    TestReporter.logPass(message);
                    return new ValidationResult(true, message, actualValue, 
                        String.format("[%s, %s]", minValue, maxValue), "FIELD_RANGE");
                } else {
                    String message = String.format("Field %s.%s is outside expected range [%s, %s]: %s", 
                        tableName, columnName, minValue, maxValue, actualValue);
                    logger.warn(message);
                    TestReporter.logFail(message);
                    return new ValidationResult(false, message, actualValue, 
                        String.format("[%s, %s]", minValue, maxValue), "FIELD_RANGE");
                }
            } else {
                String message = String.format("Field %s.%s value is not comparable: %s", tableName, columnName, actualValue);
                logger.error(message);
                TestReporter.logFail(message);
                return new ValidationResult(false, message, "FIELD_RANGE");
            }
            
        } catch (SQLException e) {
            String message = String.format("Error validating field range: %s", e.getMessage());
            logger.error(message);
            TestReporter.logFail(message);
            return new ValidationResult(false, message, "FIELD_RANGE");
        }
    }
    
    /**
     * Validate field value matches pattern
     * 
     * @param tableName Table name
     * @param columnName Column name
     * @param pattern Regular expression pattern
     * @param whereCondition WHERE condition
     * @param parameters Query parameters
     * @return Validation result
     */
    public static ValidationResult validateFieldPattern(String tableName, String columnName, String pattern, 
                                                       String whereCondition, Object... parameters) {
        logger.debug("Validating field pattern in {}.{} matches: {}", tableName, columnName, pattern);
        TestReporter.logInfo(String.format("Validating field %s.%s matches pattern: %s", tableName, columnName, pattern));
        
        try {
            String query = String.format("SELECT %s FROM %s WHERE %s", columnName, tableName, whereCondition);
            Object actualValue = SqlExecutor.executeSingleValue(query, String.class, parameters);
            
            if (actualValue != null) {
                String stringValue = actualValue.toString();
                Pattern compiledPattern = Pattern.compile(pattern);
                
                if (compiledPattern.matcher(stringValue).matches()) {
                    String message = String.format("Field %s.%s matches expected pattern '%s': %s", 
                        tableName, columnName, pattern, stringValue);
                    logger.debug(message);
                    TestReporter.logPass(message);
                    return new ValidationResult(true, message, actualValue, pattern, "FIELD_PATTERN");
                } else {
                    String message = String.format("Field %s.%s does not match expected pattern '%s': %s", 
                        tableName, columnName, pattern, stringValue);
                    logger.warn(message);
                    TestReporter.logFail(message);
                    return new ValidationResult(false, message, actualValue, pattern, "FIELD_PATTERN");
                }
            } else {
                String message = String.format("Field %s.%s is null, cannot match pattern", tableName, columnName);
                logger.warn(message);
                TestReporter.logFail(message);
                return new ValidationResult(false, message, "FIELD_PATTERN");
            }
            
        } catch (SQLException e) {
            String message = String.format("Error validating field pattern: %s", e.getMessage());
            logger.error(message);
            TestReporter.logFail(message);
            return new ValidationResult(false, message, "FIELD_PATTERN");
        }
    }
    
    /**
     * Validate record count in table
     * 
     * @param tableName Table name
     * @param expectedCount Expected record count
     * @param whereCondition WHERE condition (optional, can be null)
     * @param parameters Query parameters
     * @return Validation result
     */
    public static ValidationResult validateRecordCount(String tableName, long expectedCount, 
                                                      String whereCondition, Object... parameters) {
        logger.debug("Validating record count in table: {} expected: {}", tableName, expectedCount);
        TestReporter.logInfo(String.format("Validating record count in %s equals %d", tableName, expectedCount));
        
        try {
            String query = whereCondition != null ? 
                String.format("SELECT COUNT(*) FROM %s WHERE %s", tableName, whereCondition) :
                String.format("SELECT COUNT(*) FROM %s", tableName);
            
            long actualCount = SqlExecutor.getRecordCount(query, parameters);
            
            if (actualCount == expectedCount) {
                String message = String.format("Table %s has expected record count: %d", tableName, expectedCount);
                logger.debug(message);
                TestReporter.logPass(message);
                return new ValidationResult(true, message, actualCount, expectedCount, "RECORD_COUNT");
            } else {
                String message = String.format("Table %s has unexpected record count. Expected: %d, Actual: %d", 
                    tableName, expectedCount, actualCount);
                logger.warn(message);
                TestReporter.logFail(message);
                return new ValidationResult(false, message, actualCount, expectedCount, "RECORD_COUNT");
            }
            
        } catch (SQLException e) {
            String message = String.format("Error validating record count: %s", e.getMessage());
            logger.error(message);
            TestReporter.logFail(message);
            return new ValidationResult(false, message, "RECORD_COUNT");
        }
    }
    
    /**
     * Validate referential integrity between tables
     * 
     * @param parentTable Parent table name
     * @param parentColumn Parent table column
     * @param childTable Child table name
     * @param childColumn Child table column
     * @return Validation result
     */
    public static ValidationResult validateReferentialIntegrity(String parentTable, String parentColumn,
                                                               String childTable, String childColumn) {
        logger.debug("Validating referential integrity: {}.{} -> {}.{}", 
            childTable, childColumn, parentTable, parentColumn);
        TestReporter.logInfo(String.format("Validating referential integrity: %s.%s references %s.%s", 
            childTable, childColumn, parentTable, parentColumn));
        
        try {
            String query = String.format(
                "SELECT COUNT(*) FROM %s c WHERE c.%s IS NOT NULL AND c.%s NOT IN (SELECT p.%s FROM %s p WHERE p.%s IS NOT NULL)",
                childTable, childColumn, childColumn, parentColumn, parentTable, parentColumn);
            
            long orphanedRecords = SqlExecutor.getRecordCount(query);
            
            if (orphanedRecords == 0) {
                String message = String.format("Referential integrity valid: %s.%s -> %s.%s", 
                    childTable, childColumn, parentTable, parentColumn);
                logger.debug(message);
                TestReporter.logPass(message);
                return new ValidationResult(true, message, "REFERENTIAL_INTEGRITY");
            } else {
                String message = String.format("Referential integrity violation: %d orphaned records in %s.%s", 
                    orphanedRecords, childTable, childColumn);
                logger.warn(message);
                TestReporter.logFail(message);
                return new ValidationResult(false, message, "REFERENTIAL_INTEGRITY");
            }
            
        } catch (SQLException e) {
            String message = String.format("Error validating referential integrity: %s", e.getMessage());
            logger.error(message);
            TestReporter.logFail(message);
            return new ValidationResult(false, message, "REFERENTIAL_INTEGRITY");
        }
    }
    
    /**
     * Validate data consistency across multiple tables
     * 
     * @param query SQL query to check data consistency
     * @param parameters Query parameters
     * @param description Description of the consistency check
     * @return Validation result
     */
    public static ValidationResult validateDataConsistency(String query, Object[] parameters, String description) {
        logger.debug("Validating data consistency: {}", description);
        TestReporter.logInfo("Validating data consistency: " + description);
        
        try {
            List<Map<String, Object>> results = SqlExecutor.executeQuery(query, parameters);
            
            if (results.isEmpty()) {
                String message = String.format("Data consistency check passed: %s", description);
                logger.debug(message);
                TestReporter.logPass(message);
                return new ValidationResult(true, message, "DATA_CONSISTENCY");
            } else {
                String message = String.format("Data consistency check failed: %s (found %d inconsistent records)", 
                    description, results.size());
                logger.warn(message);
                TestReporter.logFail(message);
                return new ValidationResult(false, message, "DATA_CONSISTENCY");
            }
            
        } catch (SQLException e) {
            String message = String.format("Error validating data consistency: %s", e.getMessage());
            logger.error(message);
            TestReporter.logFail(message);
            return new ValidationResult(false, message, "DATA_CONSISTENCY");
        }
    }
    
    /**
     * Validate field is not null
     * 
     * @param tableName Table name
     * @param columnName Column name
     * @param whereCondition WHERE condition
     * @param parameters Query parameters
     * @return Validation result
     */
    public static ValidationResult validateFieldNotNull(String tableName, String columnName, 
                                                       String whereCondition, Object... parameters) {
        logger.debug("Validating field not null: {}.{}", tableName, columnName);
        TestReporter.logInfo(String.format("Validating field %s.%s is not null", tableName, columnName));
        
        try {
            String query = String.format("SELECT %s FROM %s WHERE %s", columnName, tableName, whereCondition);
            Object actualValue = SqlExecutor.executeSingleValue(query, Object.class, parameters);
            
            if (actualValue != null) {
                String message = String.format("Field %s.%s is not null: %s", tableName, columnName, actualValue);
                logger.debug(message);
                TestReporter.logPass(message);
                return new ValidationResult(true, message, actualValue, "NOT NULL", "FIELD_NOT_NULL");
            } else {
                String message = String.format("Field %s.%s is null when it should not be", tableName, columnName);
                logger.warn(message);
                TestReporter.logFail(message);
                return new ValidationResult(false, message, actualValue, "NOT NULL", "FIELD_NOT_NULL");
            }
            
        } catch (SQLException e) {
            String message = String.format("Error validating field not null: %s", e.getMessage());
            logger.error(message);
            TestReporter.logFail(message);
            return new ValidationResult(false, message, "FIELD_NOT_NULL");
        }
    }
    
    /**
     * Run multiple validations and return summary result
     * 
     * @param validations List of validation results
     * @return Summary validation result
     */
    public static ValidationResult validateAll(List<ValidationResult> validations) {
        logger.debug("Running validation summary for {} validations", validations.size());
        TestReporter.logInfo("Running validation summary for " + validations.size() + " validations");
        
        int passedCount = 0;
        int failedCount = 0;
        StringBuilder summary = new StringBuilder();
        
        for (ValidationResult result : validations) {
            if (result.isPassed()) {
                passedCount++;
            } else {
                failedCount++;
                summary.append(result.getMessage()).append("; ");
            }
        }
        
        boolean allPassed = failedCount == 0;
        String message = String.format("Validation summary: %d passed, %d failed out of %d total validations", 
            passedCount, failedCount, validations.size());
        
        if (!allPassed) {
            message += ". Failures: " + summary.toString();
        }
        
        if (allPassed) {
            logger.debug(message);
            TestReporter.logPass(message);
        } else {
            logger.warn(message);
            TestReporter.logFail(message);
        }
        
        return new ValidationResult(allPassed, message, "VALIDATION_SUMMARY");
    }
    
    /**
     * Validate timestamp field is recent (within specified minutes)
     * 
     * @param tableName Table name
     * @param columnName Column name
     * @param withinMinutes Number of minutes for recency check
     * @param whereCondition WHERE condition
     * @param parameters Query parameters
     * @return Validation result
     */
    public static ValidationResult validateTimestampRecent(String tableName, String columnName, int withinMinutes,
                                                          String whereCondition, Object... parameters) {
        logger.debug("Validating timestamp recency: {}.{} within {} minutes", tableName, columnName, withinMinutes);
        TestReporter.logInfo(String.format("Validating %s.%s is within last %d minutes", 
            tableName, columnName, withinMinutes));
        
        try {
            String query = String.format("SELECT %s FROM %s WHERE %s", columnName, tableName, whereCondition);
            Object actualValue = SqlExecutor.executeSingleValue(query, Object.class, parameters);
            
            if (actualValue instanceof Timestamp) {
                Timestamp timestamp = (Timestamp) actualValue;
                LocalDateTime recordTime = timestamp.toLocalDateTime();
                LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(withinMinutes);
                
                if (recordTime.isAfter(cutoffTime)) {
                    String message = String.format("Timestamp %s.%s is recent: %s (within %d minutes)", 
                        tableName, columnName, recordTime, withinMinutes);
                    logger.debug(message);
                    TestReporter.logPass(message);
                    return new ValidationResult(true, message, actualValue, cutoffTime, "TIMESTAMP_RECENT");
                } else {
                    String message = String.format("Timestamp %s.%s is not recent: %s (older than %d minutes)", 
                        tableName, columnName, recordTime, withinMinutes);
                    logger.warn(message);
                    TestReporter.logFail(message);
                    return new ValidationResult(false, message, actualValue, cutoffTime, "TIMESTAMP_RECENT");
                }
            } else {
                String message = String.format("Field %s.%s is not a timestamp: %s", tableName, columnName, actualValue);
                logger.error(message);
                TestReporter.logFail(message);
                return new ValidationResult(false, message, "TIMESTAMP_RECENT");
            }
            
        } catch (SQLException e) {
            String message = String.format("Error validating timestamp recency: %s", e.getMessage());
            logger.error(message);
            TestReporter.logFail(message);
            return new ValidationResult(false, message, "TIMESTAMP_RECENT");
        }
    }
}