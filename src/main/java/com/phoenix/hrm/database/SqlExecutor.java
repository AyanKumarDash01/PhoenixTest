package com.phoenix.hrm.database;

import com.phoenix.hrm.reporting.TestReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * SQL Execution Utility for Phoenix HRM Test Automation Framework
 * 
 * Provides comprehensive SQL execution capabilities with support for:
 * - SELECT, INSERT, UPDATE, DELETE operations
 * - Batch operations
 * - Transaction management
 * - Result set processing
 * - Query performance monitoring
 * - Parameter binding
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 3.0
 * @since Phase 3
 */
public class SqlExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(SqlExecutor.class);
    
    /**
     * Execute SELECT query and return results as List of Maps
     * 
     * @param query SQL SELECT query
     * @param parameters Query parameters
     * @return List of rows, each row as Map of column name to value
     * @throws SQLException if query execution fails
     */
    public static List<Map<String, Object>> executeQuery(String query, Object... parameters) throws SQLException {
        logger.debug("Executing SELECT query: {}", query);
        TestReporter.logInfo("Executing database query: " + query);
        
        long startTime = System.currentTimeMillis();
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            
            // Set parameters
            setParameters(statement, parameters);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                while (resultSet.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnLabel(i);
                        Object value = resultSet.getObject(i);
                        row.put(columnName, value);
                    }
                    results.add(row);
                }
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            logger.debug("Query executed in {} ms, returned {} rows", executionTime, results.size());
            TestReporter.logPass("Query executed successfully: " + results.size() + " rows returned in " + executionTime + "ms");
            
            return results;
            
        } catch (SQLException e) {
            logger.error("Error executing SELECT query: {}", e.getMessage());
            TestReporter.logFail("Database query failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Execute INSERT, UPDATE, or DELETE query
     * 
     * @param query SQL DML query
     * @param parameters Query parameters
     * @return Number of affected rows
     * @throws SQLException if query execution fails
     */
    public static int executeUpdate(String query, Object... parameters) throws SQLException {
        logger.debug("Executing DML query: {}", query);
        TestReporter.logInfo("Executing database update: " + query);
        
        long startTime = System.currentTimeMillis();
        
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            
            // Set parameters
            setParameters(statement, parameters);
            
            int affectedRows = statement.executeUpdate();
            
            long executionTime = System.currentTimeMillis() - startTime;
            logger.debug("Update executed in {} ms, affected {} rows", executionTime, affectedRows);
            TestReporter.logPass("Database update successful: " + affectedRows + " rows affected in " + executionTime + "ms");
            
            return affectedRows;
            
        } catch (SQLException e) {
            logger.error("Error executing DML query: {}", e.getMessage());
            TestReporter.logFail("Database update failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Execute INSERT and return generated keys
     * 
     * @param query SQL INSERT query
     * @param parameters Query parameters
     * @return List of generated keys
     * @throws SQLException if query execution fails
     */
    public static List<Long> executeInsertWithGeneratedKeys(String query, Object... parameters) throws SQLException {
        logger.debug("Executing INSERT with generated keys: {}", query);
        TestReporter.logInfo("Executing database insert with key generation: " + query);
        
        long startTime = System.currentTimeMillis();
        List<Long> generatedKeys = new ArrayList<>();
        
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            // Set parameters
            setParameters(statement, parameters);
            
            int affectedRows = statement.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet keyResultSet = statement.getGeneratedKeys()) {
                    while (keyResultSet.next()) {
                        generatedKeys.add(keyResultSet.getLong(1));
                    }
                }
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            logger.debug("Insert executed in {} ms, affected {} rows, generated {} keys", 
                executionTime, affectedRows, generatedKeys.size());
            TestReporter.logPass("Database insert successful: " + affectedRows + " rows inserted, " + 
                generatedKeys.size() + " keys generated in " + executionTime + "ms");
            
            return generatedKeys;
            
        } catch (SQLException e) {
            logger.error("Error executing INSERT with generated keys: {}", e.getMessage());
            TestReporter.logFail("Database insert failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Execute batch operations
     * 
     * @param query SQL query template
     * @param batchParameters List of parameter arrays for batch execution
     * @return Array of update counts for each batch operation
     * @throws SQLException if batch execution fails
     */
    public static int[] executeBatch(String query, List<Object[]> batchParameters) throws SQLException {
        logger.debug("Executing batch operation: {} with {} batches", query, batchParameters.size());
        TestReporter.logInfo("Executing database batch operation: " + batchParameters.size() + " batches");
        
        long startTime = System.currentTimeMillis();
        
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            
            connection.setAutoCommit(false); // Use transaction for batch
            
            for (Object[] parameters : batchParameters) {
                setParameters(statement, parameters);
                statement.addBatch();
            }
            
            int[] results = statement.executeBatch();
            connection.commit();
            
            long executionTime = System.currentTimeMillis() - startTime;
            int totalAffectedRows = Arrays.stream(results).sum();
            
            logger.debug("Batch executed in {} ms, total affected rows: {}", executionTime, totalAffectedRows);
            TestReporter.logPass("Database batch operation successful: " + totalAffectedRows + 
                " total rows affected in " + executionTime + "ms");
            
            return results;
            
        } catch (SQLException e) {
            logger.error("Error executing batch operation: {}", e.getMessage());
            TestReporter.logFail("Database batch operation failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Execute query and return single value
     * 
     * @param query SQL query that returns single value
     * @param parameters Query parameters
     * @return Single value from query result
     * @throws SQLException if query execution fails
     */
    @SuppressWarnings("unchecked")
    public static <T> T executeSingleValue(String query, Class<T> expectedType, Object... parameters) throws SQLException {
        logger.debug("Executing single value query: {}", query);
        
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            
            setParameters(statement, parameters);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Object value = resultSet.getObject(1);
                    if (value != null && expectedType.isAssignableFrom(value.getClass())) {
                        logger.debug("Single value query returned: {}", value);
                        return (T) value;
                    } else if (value != null) {
                        // Try to convert basic types
                        return convertValue(value, expectedType);
                    }
                }
            }
            
            logger.debug("Single value query returned null");
            return null;
            
        } catch (SQLException e) {
            logger.error("Error executing single value query: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Check if record exists based on query
     * 
     * @param query SQL query to check existence
     * @param parameters Query parameters
     * @return true if at least one record exists
     * @throws SQLException if query execution fails
     */
    public static boolean recordExists(String query, Object... parameters) throws SQLException {
        logger.debug("Checking record existence: {}", query);
        
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            
            setParameters(statement, parameters);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                boolean exists = resultSet.next();
                logger.debug("Record exists check returned: {}", exists);
                return exists;
            }
            
        } catch (SQLException e) {
            logger.error("Error checking record existence: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Get record count from query
     * 
     * @param query SQL COUNT query
     * @param parameters Query parameters
     * @return Record count
     * @throws SQLException if query execution fails
     */
    public static long getRecordCount(String query, Object... parameters) throws SQLException {
        logger.debug("Getting record count: {}", query);
        
        Long count = executeSingleValue(query, Long.class, parameters);
        return count != null ? count : 0L;
    }
    
    /**
     * Execute DDL statement (CREATE, ALTER, DROP)
     * 
     * @param ddlStatement DDL statement
     * @return true if execution was successful
     * @throws SQLException if execution fails
     */
    public static boolean executeDDL(String ddlStatement) throws SQLException {
        logger.debug("Executing DDL statement: {}", ddlStatement);
        TestReporter.logInfo("Executing DDL statement: " + ddlStatement);
        
        try (Connection connection = DatabaseConnectionManager.getConnection();
             Statement statement = connection.createStatement()) {
            
            boolean result = statement.execute(ddlStatement);
            
            logger.debug("DDL statement executed successfully");
            TestReporter.logPass("DDL statement executed successfully");
            
            return result;
            
        } catch (SQLException e) {
            logger.error("Error executing DDL statement: {}", e.getMessage());
            TestReporter.logFail("DDL statement failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Execute stored procedure
     * 
     * @param procedureName Stored procedure name
     * @param parameters Procedure parameters
     * @return Map containing output parameters and result sets
     * @throws SQLException if procedure execution fails
     */
    public static Map<String, Object> executeStoredProcedure(String procedureName, Object... parameters) throws SQLException {
        logger.debug("Executing stored procedure: {}", procedureName);
        TestReporter.logInfo("Executing stored procedure: " + procedureName);
        
        String callSql = buildProcedureCall(procedureName, parameters.length);
        Map<String, Object> results = new HashMap<>();
        
        try (Connection connection = DatabaseConnectionManager.getConnection();
             CallableStatement statement = connection.prepareCall(callSql)) {
            
            // Set input parameters
            setParameters(statement, parameters);
            
            boolean hasResultSet = statement.execute();
            
            if (hasResultSet) {
                List<Map<String, Object>> resultSet = new ArrayList<>();
                try (ResultSet rs = statement.getResultSet()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnLabel(i);
                            Object value = rs.getObject(i);
                            row.put(columnName, value);
                        }
                        resultSet.add(row);
                    }
                }
                results.put("resultSet", resultSet);
            }
            
            logger.debug("Stored procedure executed successfully");
            TestReporter.logPass("Stored procedure executed successfully");
            
            return results;
            
        } catch (SQLException e) {
            logger.error("Error executing stored procedure: {}", e.getMessage());
            TestReporter.logFail("Stored procedure failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Execute query with transaction
     * 
     * @param queries List of SQL queries to execute in transaction
     * @return true if all queries executed successfully
     * @throws SQLException if any query fails
     */
    public static boolean executeTransaction(List<String> queries) throws SQLException {
        logger.debug("Executing transaction with {} queries", queries.size());
        TestReporter.logInfo("Executing database transaction: " + queries.size() + " queries");
        
        long startTime = System.currentTimeMillis();
        
        try (Connection connection = DatabaseConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            
            try (Statement statement = connection.createStatement()) {
                for (String query : queries) {
                    logger.debug("Executing transaction query: {}", query);
                    statement.executeUpdate(query);
                }
                
                connection.commit();
                
                long executionTime = System.currentTimeMillis() - startTime;
                logger.debug("Transaction executed successfully in {} ms", executionTime);
                TestReporter.logPass("Database transaction successful: " + queries.size() + 
                    " queries executed in " + executionTime + "ms");
                
                return true;
                
            } catch (SQLException e) {
                connection.rollback();
                logger.error("Transaction rolled back due to error: {}", e.getMessage());
                TestReporter.logFail("Database transaction failed and rolled back: " + e.getMessage());
                throw e;
            }
            
        } catch (SQLException e) {
            logger.error("Error in transaction execution: {}", e.getMessage());
            TestReporter.logFail("Database transaction error: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Set parameters for prepared statement
     * 
     * @param statement Prepared statement
     * @param parameters Parameters to set
     * @throws SQLException if parameter setting fails
     */
    private static void setParameters(PreparedStatement statement, Object... parameters) throws SQLException {
        for (int i = 0; i < parameters.length; i++) {
            Object parameter = parameters[i];
            int parameterIndex = i + 1;
            
            if (parameter == null) {
                statement.setNull(parameterIndex, Types.NULL);
            } else if (parameter instanceof String) {
                statement.setString(parameterIndex, (String) parameter);
            } else if (parameter instanceof Integer) {
                statement.setInt(parameterIndex, (Integer) parameter);
            } else if (parameter instanceof Long) {
                statement.setLong(parameterIndex, (Long) parameter);
            } else if (parameter instanceof Double) {
                statement.setDouble(parameterIndex, (Double) parameter);
            } else if (parameter instanceof Boolean) {
                statement.setBoolean(parameterIndex, (Boolean) parameter);
            } else if (parameter instanceof Date) {
                statement.setDate(parameterIndex, (Date) parameter);
            } else if (parameter instanceof Timestamp) {
                statement.setTimestamp(parameterIndex, (Timestamp) parameter);
            } else if (parameter instanceof LocalDateTime) {
                statement.setTimestamp(parameterIndex, Timestamp.valueOf((LocalDateTime) parameter));
            } else {
                statement.setObject(parameterIndex, parameter);
            }
        }
    }
    
    /**
     * Convert value to expected type
     * 
     * @param value Value to convert
     * @param expectedType Expected type
     * @return Converted value
     */
    @SuppressWarnings("unchecked")
    private static <T> T convertValue(Object value, Class<T> expectedType) {
        if (expectedType == String.class) {
            return (T) value.toString();
        } else if (expectedType == Integer.class && value instanceof Number) {
            return (T) Integer.valueOf(((Number) value).intValue());
        } else if (expectedType == Long.class && value instanceof Number) {
            return (T) Long.valueOf(((Number) value).longValue());
        } else if (expectedType == Double.class && value instanceof Number) {
            return (T) Double.valueOf(((Number) value).doubleValue());
        } else if (expectedType == Boolean.class) {
            if (value instanceof Number) {
                return (T) Boolean.valueOf(((Number) value).intValue() != 0);
            } else {
                return (T) Boolean.valueOf(value.toString());
            }
        }
        
        return (T) value;
    }
    
    /**
     * Build stored procedure call SQL
     * 
     * @param procedureName Procedure name
     * @param parameterCount Number of parameters
     * @return Callable statement SQL
     */
    private static String buildProcedureCall(String procedureName, int parameterCount) {
        StringBuilder callSql = new StringBuilder("{call ");
        callSql.append(procedureName);
        
        if (parameterCount > 0) {
            callSql.append("(");
            for (int i = 0; i < parameterCount; i++) {
                if (i > 0) {
                    callSql.append(",");
                }
                callSql.append("?");
            }
            callSql.append(")");
        }
        
        callSql.append("}");
        return callSql.toString();
    }
    
    /**
     * Get table structure information
     * 
     * @param tableName Table name
     * @return List of column information
     * @throws SQLException if metadata retrieval fails
     */
    public static List<Map<String, Object>> getTableStructure(String tableName) throws SQLException {
        logger.debug("Getting table structure for: {}", tableName);
        
        List<Map<String, Object>> columns = new ArrayList<>();
        
        try (Connection connection = DatabaseConnectionManager.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            try (ResultSet resultSet = metaData.getColumns(null, null, tableName, null)) {
                while (resultSet.next()) {
                    Map<String, Object> column = new HashMap<>();
                    column.put("columnName", resultSet.getString("COLUMN_NAME"));
                    column.put("dataType", resultSet.getString("TYPE_NAME"));
                    column.put("columnSize", resultSet.getInt("COLUMN_SIZE"));
                    column.put("nullable", resultSet.getBoolean("NULLABLE"));
                    column.put("defaultValue", resultSet.getString("COLUMN_DEF"));
                    columns.add(column);
                }
            }
        }
        
        logger.debug("Retrieved structure for table {}: {} columns", tableName, columns.size());
        return columns;
    }
}