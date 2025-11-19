package com.phoenix.hrm.database;

import com.phoenix.hrm.config.ConfigurationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Database Connection Manager for Phoenix HRM Test Automation Framework
 * 
 * Provides centralized database connection management with support for:
 * - Multiple database types (MySQL, PostgreSQL, Oracle, SQL Server, H2)
 * - Connection pooling
 * - Thread-safe operations
 * - Configuration-based connection parameters
 * - Connection health monitoring
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 3.0
 * @since Phase 3
 */
public class DatabaseConnectionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionManager.class);
    
    // Thread-safe connection storage
    private static final ThreadLocal<Connection> connectionThreadLocal = new ThreadLocal<>();
    
    // Connection pool for reuse
    private static final Map<String, Connection> connectionPool = new ConcurrentHashMap<>();
    
    // Database type enumeration
    public enum DatabaseType {
        MYSQL("com.mysql.cj.jdbc.Driver", "jdbc:mysql://"),
        POSTGRESQL("org.postgresql.Driver", "jdbc:postgresql://"),
        ORACLE("oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@"),
        SQL_SERVER("com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://"),
        H2("org.h2.Driver", "jdbc:h2:"),
        SQLITE("org.sqlite.JDBC", "jdbc:sqlite:");
        
        private final String driverClassName;
        private final String urlPrefix;
        
        DatabaseType(String driverClassName, String urlPrefix) {
            this.driverClassName = driverClassName;
            this.urlPrefix = urlPrefix;
        }
        
        public String getDriverClassName() {
            return driverClassName;
        }
        
        public String getUrlPrefix() {
            return urlPrefix;
        }
    }
    
    /**
     * Get database connection based on configuration
     * 
     * @return Database connection
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        Connection connection = connectionThreadLocal.get();
        
        if (connection == null || connection.isClosed()) {
            connection = createConnection();
            connectionThreadLocal.set(connection);
        }
        
        return connection;
    }
    
    /**
     * Get connection for specific database configuration
     * 
     * @param configPrefix Configuration prefix (e.g., "primary", "secondary")
     * @return Database connection
     * @throws SQLException if connection fails
     */
    public static Connection getConnection(String configPrefix) throws SQLException {
        String connectionKey = Thread.currentThread().getId() + "_" + configPrefix;
        Connection connection = connectionPool.get(connectionKey);
        
        if (connection == null || connection.isClosed()) {
            connection = createConnection(configPrefix);
            connectionPool.put(connectionKey, connection);
        }
        
        return connection;
    }
    
    /**
     * Create new database connection using default configuration
     * 
     * @return New database connection
     * @throws SQLException if connection fails
     */
    private static Connection createConnection() throws SQLException {
        return createConnection("database");
    }
    
    /**
     * Create new database connection using specified configuration prefix
     * 
     * @param configPrefix Configuration prefix
     * @return New database connection
     * @throws SQLException if connection fails
     */
    private static Connection createConnection(String configPrefix) throws SQLException {
        String dbTypeStr = ConfigurationManager.getProperty(configPrefix + ".type", "mysql");
        DatabaseType dbType = DatabaseType.valueOf(dbTypeStr.toUpperCase());
        
        String host = ConfigurationManager.getProperty(configPrefix + ".host", "localhost");
        String port = ConfigurationManager.getProperty(configPrefix + ".port", getDefaultPort(dbType));
        String database = ConfigurationManager.getProperty(configPrefix + ".database", "phoenix_hrm");
        String username = ConfigurationManager.getProperty(configPrefix + ".username", "root");
        String password = ConfigurationManager.getProperty(configPrefix + ".password", "");
        
        String url = buildConnectionUrl(dbType, host, port, database);
        
        logger.info("Creating database connection: {} to {}", dbType, host);
        
        try {
            // Load database driver
            Class.forName(dbType.getDriverClassName());
            
            // Create connection properties
            Properties props = new Properties();
            props.setProperty("user", username);
            props.setProperty("password", password);
            
            // Add database-specific properties
            addDatabaseSpecificProperties(props, dbType, configPrefix);
            
            // Create connection
            Connection connection = DriverManager.getConnection(url, props);
            
            // Configure connection
            configureConnection(connection, configPrefix);
            
            logger.info("Database connection established successfully: {}", url);
            return connection;
            
        } catch (ClassNotFoundException e) {
            logger.error("Database driver not found: {}", dbType.getDriverClassName());
            throw new SQLException("Database driver not found: " + dbType.getDriverClassName(), e);
        } catch (SQLException e) {
            logger.error("Failed to create database connection: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Build connection URL based on database type and parameters
     * 
     * @param dbType Database type
     * @param host Database host
     * @param port Database port
     * @param database Database name
     * @return Connection URL
     */
    private static String buildConnectionUrl(DatabaseType dbType, String host, String port, String database) {
        switch (dbType) {
            case MYSQL:
                return String.format("%s%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", 
                    dbType.getUrlPrefix(), host, port, database);
            case POSTGRESQL:
                return String.format("%s%s:%s/%s", dbType.getUrlPrefix(), host, port, database);
            case ORACLE:
                return String.format("%s%s:%s:%s", dbType.getUrlPrefix(), host, port, database);
            case SQL_SERVER:
                return String.format("%s%s:%s;databaseName=%s;trustServerCertificate=true", 
                    dbType.getUrlPrefix(), host, port, database);
            case H2:
                return String.format("%smem:%s;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", 
                    dbType.getUrlPrefix(), database);
            case SQLITE:
                return String.format("%s%s", dbType.getUrlPrefix(), database);
            default:
                throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
    }
    
    /**
     * Get default port for database type
     * 
     * @param dbType Database type
     * @return Default port as string
     */
    private static String getDefaultPort(DatabaseType dbType) {
        switch (dbType) {
            case MYSQL:
                return "3306";
            case POSTGRESQL:
                return "5432";
            case ORACLE:
                return "1521";
            case SQL_SERVER:
                return "1433";
            default:
                return "";
        }
    }
    
    /**
     * Add database-specific connection properties
     * 
     * @param props Properties object to modify
     * @param dbType Database type
     * @param configPrefix Configuration prefix
     */
    private static void addDatabaseSpecificProperties(Properties props, DatabaseType dbType, String configPrefix) {
        switch (dbType) {
            case MYSQL:
                props.setProperty("useUnicode", "true");
                props.setProperty("characterEncoding", "UTF-8");
                props.setProperty("autoReconnect", "true");
                props.setProperty("failOverReadOnly", "false");
                break;
            case POSTGRESQL:
                props.setProperty("ssl", ConfigurationManager.getProperty(configPrefix + ".ssl", "false"));
                break;
            case ORACLE:
                props.setProperty("oracle.jdbc.ReadTimeout", "30000");
                break;
            case SQL_SERVER:
                props.setProperty("loginTimeout", "30");
                break;
        }
        
        // Add custom properties from configuration
        String customProps = ConfigurationManager.getProperty(configPrefix + ".connection.properties", "");
        if (!customProps.isEmpty()) {
            String[] propPairs = customProps.split(";");
            for (String pair : propPairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    props.setProperty(keyValue[0].trim(), keyValue[1].trim());
                }
            }
        }
    }
    
    /**
     * Configure connection settings
     * 
     * @param connection Database connection
     * @param configPrefix Configuration prefix
     * @throws SQLException if configuration fails
     */
    private static void configureConnection(Connection connection, String configPrefix) throws SQLException {
        // Set auto-commit mode
        boolean autoCommit = ConfigurationManager.getBooleanProperty(configPrefix + ".auto.commit", true);
        connection.setAutoCommit(autoCommit);
        
        // Set transaction isolation level
        String isolationLevel = ConfigurationManager.getProperty(configPrefix + ".isolation.level", "READ_COMMITTED");
        connection.setTransactionIsolation(getTransactionIsolationLevel(isolationLevel));
        
        // Set read-only mode
        boolean readOnly = ConfigurationManager.getBooleanProperty(configPrefix + ".read.only", false);
        connection.setReadOnly(readOnly);
        
        // Set network timeout
        int networkTimeout = ConfigurationManager.getIntProperty(configPrefix + ".network.timeout", 30000);
        if (networkTimeout > 0) {
            connection.setNetworkTimeout(null, networkTimeout);
        }
    }
    
    /**
     * Convert isolation level string to constant
     * 
     * @param isolationLevel Isolation level string
     * @return Transaction isolation level constant
     */
    private static int getTransactionIsolationLevel(String isolationLevel) {
        switch (isolationLevel.toUpperCase()) {
            case "READ_UNCOMMITTED":
                return Connection.TRANSACTION_READ_UNCOMMITTED;
            case "READ_COMMITTED":
                return Connection.TRANSACTION_READ_COMMITTED;
            case "REPEATABLE_READ":
                return Connection.TRANSACTION_REPEATABLE_READ;
            case "SERIALIZABLE":
                return Connection.TRANSACTION_SERIALIZABLE;
            default:
                return Connection.TRANSACTION_READ_COMMITTED;
        }
    }
    
    /**
     * Test database connection
     * 
     * @return true if connection is valid
     */
    public static boolean testConnection() {
        return testConnection("database");
    }
    
    /**
     * Test database connection for specific configuration
     * 
     * @param configPrefix Configuration prefix
     * @return true if connection is valid
     */
    public static boolean testConnection(String configPrefix) {
        try (Connection connection = createConnection(configPrefix)) {
            return connection.isValid(5);
        } catch (SQLException e) {
            logger.error("Database connection test failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get database metadata
     * 
     * @return Database metadata
     * @throws SQLException if metadata retrieval fails
     */
    public static DatabaseMetaData getMetaData() throws SQLException {
        Connection connection = getConnection();
        return connection.getMetaData();
    }
    
    /**
     * Get database product information
     * 
     * @return Map containing database product information
     * @throws SQLException if information retrieval fails
     */
    public static Map<String, String> getDatabaseInfo() throws SQLException {
        DatabaseMetaData metaData = getMetaData();
        Map<String, String> info = new HashMap<>();
        
        info.put("productName", metaData.getDatabaseProductName());
        info.put("productVersion", metaData.getDatabaseProductVersion());
        info.put("driverName", metaData.getDriverName());
        info.put("driverVersion", metaData.getDriverVersion());
        info.put("url", metaData.getURL());
        info.put("userName", metaData.getUserName());
        
        return info;
    }
    
    /**
     * Close current thread's database connection
     */
    public static void closeConnection() {
        Connection connection = connectionThreadLocal.get();
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    logger.debug("Database connection closed for thread: {}", Thread.currentThread().getId());
                }
            } catch (SQLException e) {
                logger.warn("Error closing database connection: {}", e.getMessage());
            } finally {
                connectionThreadLocal.remove();
            }
        }
    }
    
    /**
     * Close specific database connection
     * 
     * @param configPrefix Configuration prefix
     */
    public static void closeConnection(String configPrefix) {
        String connectionKey = Thread.currentThread().getId() + "_" + configPrefix;
        Connection connection = connectionPool.get(connectionKey);
        
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    logger.debug("Database connection closed: {}", connectionKey);
                }
            } catch (SQLException e) {
                logger.warn("Error closing database connection {}: {}", connectionKey, e.getMessage());
            } finally {
                connectionPool.remove(connectionKey);
            }
        }
    }
    
    /**
     * Close all database connections
     */
    public static void closeAllConnections() {
        // Close thread-local connection
        closeConnection();
        
        // Close all pooled connections
        for (Map.Entry<String, Connection> entry : connectionPool.entrySet()) {
            try {
                Connection connection = entry.getValue();
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                    logger.debug("Closed pooled connection: {}", entry.getKey());
                }
            } catch (SQLException e) {
                logger.warn("Error closing pooled connection {}: {}", entry.getKey(), e.getMessage());
            }
        }
        
        connectionPool.clear();
        logger.info("All database connections closed");
    }
    
    /**
     * Execute database health check
     * 
     * @return Health check results
     */
    public static Map<String, Object> performHealthCheck() {
        Map<String, Object> healthCheck = new HashMap<>();
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Test connection
            boolean connectionValid = testConnection();
            healthCheck.put("connectionValid", connectionValid);
            
            if (connectionValid) {
                // Get database info
                Map<String, String> dbInfo = getDatabaseInfo();
                healthCheck.put("databaseInfo", dbInfo);
                
                // Test query execution
                try (Connection connection = getConnection();
                     Statement statement = connection.createStatement();
                     ResultSet resultSet = statement.executeQuery("SELECT 1")) {
                    
                    boolean queryExecuted = resultSet.next();
                    healthCheck.put("queryExecutionWorking", queryExecuted);
                }
                
                long responseTime = System.currentTimeMillis() - startTime;
                healthCheck.put("responseTimeMs", responseTime);
                healthCheck.put("status", "HEALTHY");
                
            } else {
                healthCheck.put("status", "UNHEALTHY");
                healthCheck.put("error", "Connection validation failed");
            }
            
        } catch (Exception e) {
            healthCheck.put("status", "UNHEALTHY");
            healthCheck.put("error", e.getMessage());
            logger.error("Database health check failed: {}", e.getMessage());
        }
        
        return healthCheck;
    }
}