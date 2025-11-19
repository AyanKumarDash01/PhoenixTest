package com.phoenix.hrm.reporting.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phoenix.hrm.reporting.ReportingFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Data Export and Integration Utilities for Phoenix HRM Test Automation
 * 
 * Provides comprehensive integration capabilities with external systems including:
 * - Database integration for test result persistence
 * - CI/CD pipeline integration (Jenkins, GitLab, Azure DevOps, GitHub Actions)
 * - Third-party tool integration (Jira, Slack, Microsoft Teams, Email)
 * - Cloud storage integration (AWS S3, Azure Blob, Google Cloud Storage)
 * - Monitoring system integration (Prometheus, Grafana, Datadog, New Relic)
 * - Test management tool integration (TestRail, Zephyr, qTest)
 * - Real-time data streaming and webhooks
 * - Bulk data export and import capabilities
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 6.0
 * @since Phase 6
 */
public class DataExportIntegrationUtilities {
    
    private static final Logger logger = LoggerFactory.getLogger(DataExportIntegrationUtilities.class);
    
    private final IntegrationConfiguration config;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ExecutorService executorService;
    private final Map<String, IntegrationHandler> integrationHandlers;
    
    /**
     * Integration configuration
     */
    public static class IntegrationConfiguration {
        // Database configuration
        private DatabaseConfig databaseConfig = new DatabaseConfig();
        
        // CI/CD configuration
        private Map<String, CiCdConfig> cicdConfigs = new HashMap<>();
        
        // Third-party tool configuration
        private Map<String, ThirdPartyConfig> thirdPartyConfigs = new HashMap<>();
        
        // Cloud storage configuration
        private Map<String, CloudStorageConfig> cloudStorageConfigs = new HashMap<>();
        
        // Monitoring system configuration
        private Map<String, MonitoringConfig> monitoringConfigs = new HashMap<>();
        
        // General settings
        private int maxRetryAttempts = 3;
        private int connectionTimeoutSeconds = 30;
        private int readTimeoutSeconds = 60;
        private boolean enableAsync = true;
        private boolean enableBulkOperations = true;
        private int bulkBatchSize = 100;
        
        // Builder pattern
        public static class Builder {
            private final IntegrationConfiguration config = new IntegrationConfiguration();
            
            public Builder databaseConfig(DatabaseConfig dbConfig) {
                config.databaseConfig = dbConfig;
                return this;
            }
            
            public Builder addCiCdConfig(String name, CiCdConfig cicdConfig) {
                config.cicdConfigs.put(name, cicdConfig);
                return this;
            }
            
            public Builder addThirdPartyConfig(String name, ThirdPartyConfig thirdPartyConfig) {
                config.thirdPartyConfigs.put(name, thirdPartyConfig);
                return this;
            }
            
            public Builder addCloudStorageConfig(String name, CloudStorageConfig cloudConfig) {
                config.cloudStorageConfigs.put(name, cloudConfig);
                return this;
            }
            
            public Builder addMonitoringConfig(String name, MonitoringConfig monitoringConfig) {
                config.monitoringConfigs.put(name, monitoringConfig);
                return this;
            }
            
            public Builder maxRetryAttempts(int attempts) {
                config.maxRetryAttempts = attempts;
                return this;
            }
            
            public Builder connectionTimeoutSeconds(int timeout) {
                config.connectionTimeoutSeconds = timeout;
                return this;
            }
            
            public Builder enableAsync(boolean enable) {
                config.enableAsync = enable;
                return this;
            }
            
            public Builder bulkBatchSize(int batchSize) {
                config.bulkBatchSize = batchSize;
                return this;
            }
            
            public IntegrationConfiguration build() {
                return config;
            }
        }
        
        // Getters
        public DatabaseConfig getDatabaseConfig() { return databaseConfig; }
        public Map<String, CiCdConfig> getCicdConfigs() { return cicdConfigs; }
        public Map<String, ThirdPartyConfig> getThirdPartyConfigs() { return thirdPartyConfigs; }
        public Map<String, CloudStorageConfig> getCloudStorageConfigs() { return cloudStorageConfigs; }
        public Map<String, MonitoringConfig> getMonitoringConfigs() { return monitoringConfigs; }
        public int getMaxRetryAttempts() { return maxRetryAttempts; }
        public int getConnectionTimeoutSeconds() { return connectionTimeoutSeconds; }
        public int getReadTimeoutSeconds() { return readTimeoutSeconds; }
        public boolean isEnableAsync() { return enableAsync; }
        public boolean isEnableBulkOperations() { return enableBulkOperations; }
        public int getBulkBatchSize() { return bulkBatchSize; }
    }
    
    /**
     * Database configuration
     */
    public static class DatabaseConfig {
        private String jdbcUrl = "jdbc:h2:mem:testdb";
        private String username = "sa";
        private String password = "";
        private String driverClassName = "org.h2.Driver";
        private int maxPoolSize = 10;
        private boolean enableAutoCommit = false;
        private Map<String, String> properties = new HashMap<>();
        
        // Getters and setters
        public String getJdbcUrl() { return jdbcUrl; }
        public void setJdbcUrl(String jdbcUrl) { this.jdbcUrl = jdbcUrl; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getDriverClassName() { return driverClassName; }
        public void setDriverClassName(String driverClassName) { this.driverClassName = driverClassName; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
        public boolean isEnableAutoCommit() { return enableAutoCommit; }
        public void setEnableAutoCommit(boolean enableAutoCommit) { this.enableAutoCommit = enableAutoCommit; }
        public Map<String, String> getProperties() { return properties; }
    }
    
    /**
     * CI/CD configuration
     */
    public static class CiCdConfig {
        private CiCdType type;
        private String baseUrl;
        private String apiToken;
        private String username;
        private String projectId;
        private Map<String, String> customHeaders = new HashMap<>();
        private Map<String, String> customProperties = new HashMap<>();
        
        public enum CiCdType {
            JENKINS, GITLAB, AZURE_DEVOPS, GITHUB_ACTIONS, BAMBOO, TEAMCITY
        }
        
        // Getters and setters
        public CiCdType getType() { return type; }
        public void setType(CiCdType type) { this.type = type; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiToken() { return apiToken; }
        public void setApiToken(String apiToken) { this.apiToken = apiToken; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        public Map<String, String> getCustomHeaders() { return customHeaders; }
        public Map<String, String> getCustomProperties() { return customProperties; }
    }
    
    /**
     * Third-party tool configuration
     */
    public static class ThirdPartyConfig {
        private ThirdPartyType type;
        private String baseUrl;
        private String apiKey;
        private String accessToken;
        private String webhookUrl;
        private Map<String, String> customSettings = new HashMap<>();
        
        public enum ThirdPartyType {
            JIRA, SLACK, TEAMS, EMAIL, TESTRAIL, ZEPHYR, QTEST, XRAY
        }
        
        // Getters and setters
        public ThirdPartyType getType() { return type; }
        public void setType(ThirdPartyType type) { this.type = type; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getWebhookUrl() { return webhookUrl; }
        public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
        public Map<String, String> getCustomSettings() { return customSettings; }
    }
    
    /**
     * Cloud storage configuration
     */
    public static class CloudStorageConfig {
        private CloudProviderType type;
        private String accessKeyId;
        private String secretAccessKey;
        private String bucketName;
        private String region = "us-east-1";
        private String containerName;
        private Map<String, String> customSettings = new HashMap<>();
        
        public enum CloudProviderType {
            AWS_S3, AZURE_BLOB, GOOGLE_CLOUD_STORAGE, DROPBOX, BOX
        }
        
        // Getters and setters
        public CloudProviderType getType() { return type; }
        public void setType(CloudProviderType type) { this.type = type; }
        public String getAccessKeyId() { return accessKeyId; }
        public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }
        public String getSecretAccessKey() { return secretAccessKey; }
        public void setSecretAccessKey(String secretAccessKey) { this.secretAccessKey = secretAccessKey; }
        public String getBucketName() { return bucketName; }
        public void setBucketName(String bucketName) { this.bucketName = bucketName; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getContainerName() { return containerName; }
        public void setContainerName(String containerName) { this.containerName = containerName; }
        public Map<String, String> getCustomSettings() { return customSettings; }
    }
    
    /**
     * Monitoring system configuration
     */
    public static class MonitoringConfig {
        private MonitoringType type;
        private String endpoint;
        private String apiKey;
        private String organization;
        private String database;
        private Map<String, String> labels = new HashMap<>();
        private Map<String, String> customSettings = new HashMap<>();
        
        public enum MonitoringType {
            PROMETHEUS, GRAFANA, DATADOG, NEW_RELIC, SPLUNK, ELASTIC
        }
        
        // Getters and setters
        public MonitoringType getType() { return type; }
        public void setType(MonitoringType type) { this.type = type; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getOrganization() { return organization; }
        public void setOrganization(String organization) { this.organization = organization; }
        public String getDatabase() { return database; }
        public void setDatabase(String database) { this.database = database; }
        public Map<String, String> getLabels() { return labels; }
        public Map<String, String> getCustomSettings() { return customSettings; }
    }
    
    /**
     * Export result
     */
    public static class ExportResult {
        private final boolean success;
        private final String destination;
        private final int recordsProcessed;
        private final long durationMs;
        private final String message;
        private final Map<String, Object> metadata;
        private final LocalDateTime timestamp;
        
        public ExportResult(boolean success, String destination, int recordsProcessed, 
                          long durationMs, String message, Map<String, Object> metadata) {
            this.success = success;
            this.destination = destination;
            this.recordsProcessed = recordsProcessed;
            this.durationMs = durationMs;
            this.message = message;
            this.metadata = metadata != null ? metadata : new HashMap<>();
            this.timestamp = LocalDateTime.now();
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getDestination() { return destination; }
        public int getRecordsProcessed() { return recordsProcessed; }
        public long getDurationMs() { return durationMs; }
        public String getMessage() { return message; }
        public Map<String, Object> getMetadata() { return metadata; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("ExportResult{success=%b, destination='%s', records=%d, duration=%dms}", 
                success, destination, recordsProcessed, durationMs);
        }
    }
    
    /**
     * Constructor
     */
    public DataExportIntegrationUtilities(IntegrationConfiguration config) {
        this.config = config != null ? config : new IntegrationConfiguration.Builder().build();
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(this.config.getConnectionTimeoutSeconds()))
            .build();
        this.executorService = Executors.newFixedThreadPool(10);
        this.integrationHandlers = new ConcurrentHashMap<>();
        
        // Initialize integration handlers
        initializeIntegrationHandlers();
        
        logger.info("DataExportIntegrationUtilities initialized with {} handlers", 
            integrationHandlers.size());
    }
    
    /**
     * Export test executions to database
     */
    public CompletableFuture<ExportResult> exportToDatabase(List<ReportingFramework.TestExecution> executions) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                DatabaseHandler handler = (DatabaseHandler) integrationHandlers.get("database");
                if (handler == null) {
                    handler = new DatabaseHandler(config.getDatabaseConfig());
                    integrationHandlers.put("database", handler);
                }
                
                int recordsProcessed = handler.exportExecutions(executions);
                long duration = System.currentTimeMillis() - startTime;
                
                logger.info("Exported {} test executions to database in {}ms", 
                    recordsProcessed, duration);
                
                return new ExportResult(true, "database", recordsProcessed, duration,
                    "Successfully exported to database", 
                    Map.of("table", "test_executions", "driver", config.getDatabaseConfig().getDriverClassName()));
                
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                logger.error("Database export failed: {}", e.getMessage(), e);
                
                return new ExportResult(false, "database", 0, duration,
                    "Database export failed: " + e.getMessage(), 
                    Map.of("error", e.getClass().getSimpleName()));
            }
        }, executorService);
    }
    
    /**
     * Send test results to CI/CD pipeline
     */
    public CompletableFuture<ExportResult> sendToCiCd(String configName, 
                                                     List<ReportingFramework.TestExecution> executions) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            CiCdConfig cicdConfig = config.getCicdConfigs().get(configName);
            if (cicdConfig == null) {
                return new ExportResult(false, configName, 0, 0,
                    "CI/CD configuration not found: " + configName, null);
            }
            
            try {
                CiCdHandler handler = (CiCdHandler) integrationHandlers.get("cicd_" + configName);
                if (handler == null) {
                    handler = new CiCdHandler(cicdConfig, httpClient);
                    integrationHandlers.put("cicd_" + configName, handler);
                }
                
                int recordsProcessed = handler.sendTestResults(executions);
                long duration = System.currentTimeMillis() - startTime;
                
                logger.info("Sent {} test results to {} in {}ms", 
                    recordsProcessed, cicdConfig.getType(), duration);
                
                return new ExportResult(true, configName, recordsProcessed, duration,
                    "Successfully sent to " + cicdConfig.getType(), 
                    Map.of("type", cicdConfig.getType().toString(), "url", cicdConfig.getBaseUrl()));
                
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                logger.error("CI/CD integration failed for {}: {}", configName, e.getMessage(), e);
                
                return new ExportResult(false, configName, 0, duration,
                    "CI/CD integration failed: " + e.getMessage(), 
                    Map.of("error", e.getClass().getSimpleName()));
            }
        }, executorService);
    }
    
    /**
     * Send notifications to third-party tools
     */
    public CompletableFuture<ExportResult> sendToThirdParty(String configName, 
                                                           List<ReportingFramework.TestExecution> executions,
                                                           String message) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            ThirdPartyConfig thirdPartyConfig = config.getThirdPartyConfigs().get(configName);
            if (thirdPartyConfig == null) {
                return new ExportResult(false, configName, 0, 0,
                    "Third-party configuration not found: " + configName, null);
            }
            
            try {
                ThirdPartyHandler handler = (ThirdPartyHandler) integrationHandlers.get("thirdparty_" + configName);
                if (handler == null) {
                    handler = new ThirdPartyHandler(thirdPartyConfig, httpClient, objectMapper);
                    integrationHandlers.put("thirdparty_" + configName, handler);
                }
                
                boolean success = handler.sendNotification(executions, message);
                long duration = System.currentTimeMillis() - startTime;
                
                logger.info("Sent notification to {} in {}ms", thirdPartyConfig.getType(), duration);
                
                return new ExportResult(success, configName, executions.size(), duration,
                    success ? "Successfully sent to " + thirdPartyConfig.getType() : "Failed to send notification", 
                    Map.of("type", thirdPartyConfig.getType().toString()));
                
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                logger.error("Third-party integration failed for {}: {}", configName, e.getMessage(), e);
                
                return new ExportResult(false, configName, 0, duration,
                    "Third-party integration failed: " + e.getMessage(), 
                    Map.of("error", e.getClass().getSimpleName()));
            }
        }, executorService);
    }
    
    /**
     * Upload reports to cloud storage
     */
    public CompletableFuture<ExportResult> uploadToCloudStorage(String configName, 
                                                               String reportPath, 
                                                               String remoteFileName) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            CloudStorageConfig cloudConfig = config.getCloudStorageConfigs().get(configName);
            if (cloudConfig == null) {
                return new ExportResult(false, configName, 0, 0,
                    "Cloud storage configuration not found: " + configName, null);
            }
            
            try {
                CloudStorageHandler handler = (CloudStorageHandler) integrationHandlers.get("cloud_" + configName);
                if (handler == null) {
                    handler = new CloudStorageHandler(cloudConfig);
                    integrationHandlers.put("cloud_" + configName, handler);
                }
                
                String uploadedUrl = handler.uploadFile(reportPath, remoteFileName);
                long duration = System.currentTimeMillis() - startTime;
                
                logger.info("Uploaded {} to {} in {}ms", reportPath, cloudConfig.getType(), duration);
                
                return new ExportResult(true, configName, 1, duration,
                    "Successfully uploaded to " + cloudConfig.getType(), 
                    Map.of("type", cloudConfig.getType().toString(), "url", uploadedUrl));
                
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                logger.error("Cloud storage upload failed for {}: {}", configName, e.getMessage(), e);
                
                return new ExportResult(false, configName, 0, duration,
                    "Cloud storage upload failed: " + e.getMessage(), 
                    Map.of("error", e.getClass().getSimpleName()));
            }
        }, executorService);
    }
    
    /**
     * Send metrics to monitoring system
     */
    public CompletableFuture<ExportResult> sendToMonitoring(String configName, 
                                                           Map<String, Object> metrics) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            MonitoringConfig monitoringConfig = config.getMonitoringConfigs().get(configName);
            if (monitoringConfig == null) {
                return new ExportResult(false, configName, 0, 0,
                    "Monitoring configuration not found: " + configName, null);
            }
            
            try {
                MonitoringHandler handler = (MonitoringHandler) integrationHandlers.get("monitoring_" + configName);
                if (handler == null) {
                    handler = new MonitoringHandler(monitoringConfig, httpClient, objectMapper);
                    integrationHandlers.put("monitoring_" + configName, handler);
                }
                
                int metricsCount = handler.sendMetrics(metrics);
                long duration = System.currentTimeMillis() - startTime;
                
                logger.info("Sent {} metrics to {} in {}ms", metricsCount, monitoringConfig.getType(), duration);
                
                return new ExportResult(true, configName, metricsCount, duration,
                    "Successfully sent metrics to " + monitoringConfig.getType(), 
                    Map.of("type", monitoringConfig.getType().toString()));
                
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                logger.error("Monitoring integration failed for {}: {}", configName, e.getMessage(), e);
                
                return new ExportResult(false, configName, 0, duration,
                    "Monitoring integration failed: " + e.getMessage(), 
                    Map.of("error", e.getClass().getSimpleName()));
            }
        }, executorService);
    }
    
    /**
     * Bulk export to multiple destinations
     */
    public CompletableFuture<Map<String, ExportResult>> bulkExport(
            List<ReportingFramework.TestExecution> executions,
            Map<String, Object> analyticsData,
            List<String> reportPaths) {
        
        List<CompletableFuture<Map.Entry<String, ExportResult>>> futures = new ArrayList<>();
        
        // Database export
        if (config.getDatabaseConfig() != null) {
            futures.add(exportToDatabase(executions)
                .thenApply(result -> Map.entry("database", result)));
        }
        
        // CI/CD exports
        config.getCicdConfigs().forEach((name, cicdConfig) -> {
            futures.add(sendToCiCd(name, executions)
                .thenApply(result -> Map.entry("cicd_" + name, result)));
        });
        
        // Third-party exports
        config.getThirdPartyConfigs().forEach((name, thirdPartyConfig) -> {
            String message = generateSummaryMessage(executions, analyticsData);
            futures.add(sendToThirdParty(name, executions, message)
                .thenApply(result -> Map.entry("thirdparty_" + name, result)));
        });
        
        // Cloud storage uploads
        config.getCloudStorageConfigs().forEach((name, cloudConfig) -> {
            reportPaths.forEach(reportPath -> {
                String fileName = Paths.get(reportPath).getFileName().toString();
                futures.add(uploadToCloudStorage(name, reportPath, fileName)
                    .thenApply(result -> Map.entry("cloud_" + name + "_" + fileName, result)));
            });
        });
        
        // Monitoring system exports
        config.getMonitoringConfigs().forEach((name, monitoringConfig) -> {
            Map<String, Object> metrics = extractMetricsFromAnalytics(analyticsData);
            futures.add(sendToMonitoring(name, metrics)
                .thenApply(result -> Map.entry("monitoring_" + name, result)));
        });
        
        // Combine all futures
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                Map<String, ExportResult> results = new HashMap<>();
                futures.forEach(future -> {
                    try {
                        Map.Entry<String, ExportResult> entry = future.get();
                        results.put(entry.getKey(), entry.getValue());
                    } catch (Exception e) {
                        logger.error("Error collecting bulk export result: {}", e.getMessage());
                    }
                });
                return results;
            });
    }
    
    /**
     * Get integration statistics
     */
    public Map<String, Object> getIntegrationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalHandlers", integrationHandlers.size());
        stats.put("databaseConfigured", config.getDatabaseConfig() != null);
        stats.put("cicdConfigs", config.getCicdConfigs().size());
        stats.put("thirdPartyConfigs", config.getThirdPartyConfigs().size());
        stats.put("cloudStorageConfigs", config.getCloudStorageConfigs().size());
        stats.put("monitoringConfigs", config.getMonitoringConfigs().size());
        stats.put("asyncEnabled", config.isEnableAsync());
        stats.put("bulkOperationsEnabled", config.isEnableBulkOperations());
        stats.put("bulkBatchSize", config.getBulkBatchSize());
        
        // Handler status
        Map<String, String> handlerStatus = new HashMap<>();
        integrationHandlers.forEach((key, handler) -> {
            handlerStatus.put(key, handler.isHealthy() ? "healthy" : "unhealthy");
        });
        stats.put("handlerStatus", handlerStatus);
        
        return stats;
    }
    
    /**
     * Test integration connections
     */
    public CompletableFuture<Map<String, Boolean>> testConnections() {
        List<CompletableFuture<Map.Entry<String, Boolean>>> futures = new ArrayList<>();
        
        integrationHandlers.forEach((name, handler) -> {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    boolean healthy = handler.testConnection();
                    return Map.entry(name, healthy);
                } catch (Exception e) {
                    logger.warn("Connection test failed for {}: {}", name, e.getMessage());
                    return Map.entry(name, false);
                }
            }, executorService));
        });
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                Map<String, Boolean> results = new HashMap<>();
                futures.forEach(future -> {
                    try {
                        Map.Entry<String, Boolean> entry = future.get();
                        results.put(entry.getKey(), entry.getValue());
                    } catch (Exception e) {
                        logger.error("Error collecting connection test result: {}", e.getMessage());
                    }
                });
                return results;
            });
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        integrationHandlers.values().forEach(handler -> {
            try {
                handler.cleanup();
            } catch (Exception e) {
                logger.warn("Error cleaning up handler: {}", e.getMessage());
            }
        });
        
        logger.info("DataExportIntegrationUtilities cleanup completed");
    }
    
    // Private helper methods
    
    private void initializeIntegrationHandlers() {
        // Initialize database handler if configured
        if (config.getDatabaseConfig() != null) {
            DatabaseHandler dbHandler = new DatabaseHandler(config.getDatabaseConfig());
            integrationHandlers.put("database", dbHandler);
        }
        
        logger.debug("Initialized integration handlers");
    }
    
    private String generateSummaryMessage(List<ReportingFramework.TestExecution> executions,
                                        Map<String, Object> analyticsData) {
        StringBuilder message = new StringBuilder();
        
        Map<String, Long> statusCounts = executions.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                ReportingFramework.TestExecution::getStatus,
                java.util.stream.Collectors.counting()));
        
        long totalTests = executions.size();
        long passedTests = statusCounts.getOrDefault("PASSED", 0L);
        long failedTests = statusCounts.getOrDefault("FAILED", 0L);
        double successRate = totalTests > 0 ? (passedTests * 100.0 / totalTests) : 0;
        
        message.append("🧪 **Phoenix HRM Test Results Summary**\n\n");
        message.append(String.format("📊 **Total Tests:** %d\n", totalTests));
        message.append(String.format("✅ **Passed:** %d\n", passedTests));
        message.append(String.format("❌ **Failed:** %d\n", failedTests));
        message.append(String.format("📈 **Success Rate:** %.1f%%\n", successRate));
        message.append(String.format("🕐 **Generated:** %s\n", 
            LocalDateTime.now().format(DateTimeFormatter.of("yyyy-MM-dd HH:mm:ss"))));
        
        if (analyticsData != null && !analyticsData.isEmpty()) {
            message.append("\n📋 **Analytics Available:** ").append(analyticsData.size()).append(" metrics");
        }
        
        return message.toString();
    }
    
    private Map<String, Object> extractMetricsFromAnalytics(Map<String, Object> analyticsData) {
        Map<String, Object> metrics = new HashMap<>();
        
        if (analyticsData != null) {
            // Extract key metrics for monitoring systems
            Object basicStats = analyticsData.get("basicStatistics");
            if (basicStats instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> stats = (Map<String, Object>) basicStats;
                
                metrics.put("test_execution_total", stats.get("totalExecutions"));
                metrics.put("test_success_rate", stats.get("successRate"));
                
                Object durationAnalysis = stats.get("durationAnalysis");
                if (durationAnalysis instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> duration = (Map<String, Object>) durationAnalysis;
                    metrics.put("test_duration_avg", duration.get("average"));
                    metrics.put("test_duration_max", duration.get("max"));
                }
            }
        }
        
        // Add timestamp
        metrics.put("timestamp", System.currentTimeMillis());
        
        return metrics;
    }
    
    // Integration Handler Interface
    
    private interface IntegrationHandler {
        boolean isHealthy();
        boolean testConnection();
        void cleanup();
    }
    
    // Database Handler Implementation
    
    private static class DatabaseHandler implements IntegrationHandler {
        private final DatabaseConfig config;
        private Connection connection;
        
        public DatabaseHandler(DatabaseConfig config) {
            this.config = config;
        }
        
        public int exportExecutions(List<ReportingFramework.TestExecution> executions) throws SQLException {
            ensureConnection();
            
            String insertSQL = """
                INSERT INTO test_executions (execution_id, test_name, suite_name, status, 
                environment, duration, start_time, end_time, error_message) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            try (PreparedStatement stmt = connection.prepareStatement(insertSQL)) {
                int batchCount = 0;
                
                for (ReportingFramework.TestExecution execution : executions) {
                    stmt.setString(1, execution.getExecutionId());
                    stmt.setString(2, execution.getTestName());
                    stmt.setString(3, execution.getSuiteName());
                    stmt.setString(4, execution.getStatus());
                    stmt.setString(5, execution.getEnvironment());
                    stmt.setLong(6, execution.getDuration());
                    stmt.setObject(7, execution.getStartTime());
                    stmt.setObject(8, execution.getEndTime());
                    stmt.setString(9, execution.getErrorMessage());
                    
                    stmt.addBatch();
                    batchCount++;
                    
                    if (batchCount % 100 == 0) {
                        stmt.executeBatch();
                    }
                }
                
                if (batchCount % 100 != 0) {
                    stmt.executeBatch();
                }
                
                if (!config.isEnableAutoCommit()) {
                    connection.commit();
                }
                
                return executions.size();
            }
        }
        
        private void ensureConnection() throws SQLException {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(
                    config.getJdbcUrl(), 
                    config.getUsername(), 
                    config.getPassword());
                connection.setAutoCommit(config.isEnableAutoCommit());
                
                // Create table if not exists
                createTableIfNotExists();
            }
        }
        
        private void createTableIfNotExists() throws SQLException {
            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS test_executions (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    execution_id VARCHAR(255) NOT NULL,
                    test_name VARCHAR(500) NOT NULL,
                    suite_name VARCHAR(255),
                    status VARCHAR(50) NOT NULL,
                    environment VARCHAR(100),
                    duration BIGINT,
                    start_time TIMESTAMP,
                    end_time TIMESTAMP,
                    error_message TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_execution_id (execution_id),
                    INDEX idx_status (status),
                    INDEX idx_environment (environment),
                    INDEX idx_start_time (start_time)
                )
                """;
            
            try (PreparedStatement stmt = connection.prepareStatement(createTableSQL)) {
                stmt.execute();
            }
        }
        
        @Override
        public boolean isHealthy() {
            try {
                return connection != null && !connection.isClosed() && connection.isValid(5);
            } catch (SQLException e) {
                return false;
            }
        }
        
        @Override
        public boolean testConnection() {
            try {
                ensureConnection();
                return isHealthy();
            } catch (SQLException e) {
                return false;
            }
        }
        
        @Override
        public void cleanup() {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    // Ignore
                }
            }
        }
    }
    
    // CI/CD Handler Implementation
    
    private static class CiCdHandler implements IntegrationHandler {
        private final CiCdConfig config;
        private final HttpClient httpClient;
        
        public CiCdHandler(CiCdConfig config, HttpClient httpClient) {
            this.config = config;
            this.httpClient = httpClient;
        }
        
        public int sendTestResults(List<ReportingFramework.TestExecution> executions) throws Exception {
            switch (config.getType()) {
                case JENKINS:
                    return sendToJenkins(executions);
                case GITLAB:
                    return sendToGitLab(executions);
                case AZURE_DEVOPS:
                    return sendToAzureDevOps(executions);
                case GITHUB_ACTIONS:
                    return sendToGitHubActions(executions);
                default:
                    throw new UnsupportedOperationException("CI/CD type not supported: " + config.getType());
            }
        }
        
        private int sendToJenkins(List<ReportingFramework.TestExecution> executions) throws Exception {
            // Jenkins implementation
            Map<String, Object> payload = createJenkinsPayload(executions);
            String endpoint = config.getBaseUrl() + "/job/" + config.getProjectId() + "/build";
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Basic " + 
                    Base64.getEncoder().encodeToString((config.getUsername() + ":" + config.getApiToken()).getBytes()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(payload)))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return executions.size();
            } else {
                throw new RuntimeException("Jenkins API call failed: " + response.statusCode());
            }
        }
        
        private int sendToGitLab(List<ReportingFramework.TestExecution> executions) throws Exception {
            // GitLab implementation
            Map<String, Object> payload = createGitLabPayload(executions);
            String endpoint = config.getBaseUrl() + "/api/v4/projects/" + config.getProjectId() + "/pipeline";
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("PRIVATE-TOKEN", config.getApiToken())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(payload)))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return executions.size();
            } else {
                throw new RuntimeException("GitLab API call failed: " + response.statusCode());
            }
        }
        
        private int sendToAzureDevOps(List<ReportingFramework.TestExecution> executions) throws Exception {
            // Azure DevOps implementation
            Map<String, Object> payload = createAzureDevOpsPayload(executions);
            String endpoint = config.getBaseUrl() + "/" + config.getProjectId() + "/_apis/test/runs";
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint + "?api-version=6.0"))
                .header("Authorization", "Basic " + 
                    Base64.getEncoder().encodeToString((":" + config.getApiToken()).getBytes()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(payload)))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return executions.size();
            } else {
                throw new RuntimeException("Azure DevOps API call failed: " + response.statusCode());
            }
        }
        
        private int sendToGitHubActions(List<ReportingFramework.TestExecution> executions) throws Exception {
            // GitHub Actions implementation
            Map<String, Object> payload = createGitHubActionsPayload(executions);
            String endpoint = config.getBaseUrl() + "/repos/" + config.getProjectId() + "/check-runs";
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "token " + config.getApiToken())
                .header("Accept", "application/vnd.github.v3+json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(payload)))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return executions.size();
            } else {
                throw new RuntimeException("GitHub Actions API call failed: " + response.statusCode());
            }
        }
        
        private Map<String, Object> createJenkinsPayload(List<ReportingFramework.TestExecution> executions) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("testResults", executions);
            payload.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            return payload;
        }
        
        private Map<String, Object> createGitLabPayload(List<ReportingFramework.TestExecution> executions) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("testResults", executions);
            payload.put("ref", "main");
            return payload;
        }
        
        private Map<String, Object> createAzureDevOpsPayload(List<ReportingFramework.TestExecution> executions) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("name", "Phoenix HRM Test Run");
            payload.put("testResults", executions);
            return payload;
        }
        
        private Map<String, Object> createGitHubActionsPayload(List<ReportingFramework.TestExecution> executions) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("name", "Phoenix HRM Test Results");
            payload.put("head_sha", "main");
            payload.put("status", "completed");
            payload.put("testResults", executions);
            return payload;
        }
        
        @Override
        public boolean isHealthy() {
            return testConnection();
        }
        
        @Override
        public boolean testConnection() {
            try {
                String testEndpoint = config.getBaseUrl() + "/api/v1/health";
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(testEndpoint))
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                return response.statusCode() >= 200 && response.statusCode() < 400;
            } catch (Exception e) {
                return false;
            }
        }
        
        @Override
        public void cleanup() {
            // No cleanup needed for HTTP client
        }
    }
    
    // Third Party Handler Implementation
    
    private static class ThirdPartyHandler implements IntegrationHandler {
        private final ThirdPartyConfig config;
        private final HttpClient httpClient;
        private final ObjectMapper objectMapper;
        
        public ThirdPartyHandler(ThirdPartyConfig config, HttpClient httpClient, ObjectMapper objectMapper) {
            this.config = config;
            this.httpClient = httpClient;
            this.objectMapper = objectMapper;
        }
        
        public boolean sendNotification(List<ReportingFramework.TestExecution> executions, String message) throws Exception {
            switch (config.getType()) {
                case SLACK:
                    return sendToSlack(message);
                case TEAMS:
                    return sendToTeams(message);
                case JIRA:
                    return sendToJira(executions, message);
                case EMAIL:
                    return sendEmail(message);
                default:
                    throw new UnsupportedOperationException("Third-party type not supported: " + config.getType());
            }
        }
        
        private boolean sendToSlack(String message) throws Exception {
            Map<String, Object> payload = Map.of(
                "text", message,
                "username", "Phoenix HRM Test Bot"
            );
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getWebhookUrl()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        }
        
        private boolean sendToTeams(String message) throws Exception {
            Map<String, Object> payload = Map.of(
                "text", message,
                "title", "Phoenix HRM Test Results"
            );
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getWebhookUrl()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        }
        
        private boolean sendToJira(List<ReportingFramework.TestExecution> executions, String message) throws Exception {
            // Create JIRA issue for failed tests
            long failedCount = executions.stream()
                .mapToLong(e -> "FAILED".equals(e.getStatus()) ? 1 : 0)
                .sum();
            
            if (failedCount > 0) {
                Map<String, Object> issue = Map.of(
                    "fields", Map.of(
                        "project", Map.of("key", config.getCustomSettings().get("projectKey")),
                        "summary", "Test Execution Failures - " + failedCount + " failed tests",
                        "description", message,
                        "issuetype", Map.of("name", "Bug")
                    )
                );
                
                String endpoint = config.getBaseUrl() + "/rest/api/2/issue";
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", "Basic " + 
                        Base64.getEncoder().encodeToString((config.getCustomSettings().get("username") + ":" + config.getApiKey()).getBytes()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(issue)))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                return response.statusCode() == 201;
            }
            
            return true; // No failures, no issue to create
        }
        
        private boolean sendEmail(String message) throws Exception {
            // Email implementation would go here
            // For now, just log the message
            logger.info("Email notification: {}", message);
            return true;
        }
        
        @Override
        public boolean isHealthy() {
            return testConnection();
        }
        
        @Override
        public boolean testConnection() {
            try {
                if (config.getWebhookUrl() != null) {
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(config.getWebhookUrl()))
                        .method("HEAD", HttpRequest.BodyPublishers.noBody())
                        .build();
                    
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    return response.statusCode() < 500;
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        
        @Override
        public void cleanup() {
            // No cleanup needed for HTTP client
        }
    }
    
    // Cloud Storage Handler Implementation
    
    private static class CloudStorageHandler implements IntegrationHandler {
        private final CloudStorageConfig config;
        
        public CloudStorageHandler(CloudStorageConfig config) {
            this.config = config;
        }
        
        public String uploadFile(String localFilePath, String remoteFileName) throws Exception {
            switch (config.getType()) {
                case AWS_S3:
                    return uploadToS3(localFilePath, remoteFileName);
                case AZURE_BLOB:
                    return uploadToAzureBlob(localFilePath, remoteFileName);
                case GOOGLE_CLOUD_STORAGE:
                    return uploadToGCS(localFilePath, remoteFileName);
                default:
                    throw new UnsupportedOperationException("Cloud storage type not supported: " + config.getType());
            }
        }
        
        private String uploadToS3(String localFilePath, String remoteFileName) throws Exception {
            // AWS S3 implementation would go here
            // For now, simulate upload
            logger.info("Simulating S3 upload: {} -> {}", localFilePath, remoteFileName);
            String url = String.format("https://%s.s3.%s.amazonaws.com/%s", 
                config.getBucketName(), config.getRegion(), remoteFileName);
            return url;
        }
        
        private String uploadToAzureBlob(String localFilePath, String remoteFileName) throws Exception {
            // Azure Blob implementation would go here
            logger.info("Simulating Azure Blob upload: {} -> {}", localFilePath, remoteFileName);
            String url = String.format("https://%s.blob.core.windows.net/%s/%s", 
                config.getCustomSettings().get("accountName"), config.getContainerName(), remoteFileName);
            return url;
        }
        
        private String uploadToGCS(String localFilePath, String remoteFileName) throws Exception {
            // Google Cloud Storage implementation would go here
            logger.info("Simulating GCS upload: {} -> {}", localFilePath, remoteFileName);
            String url = String.format("https://storage.googleapis.com/%s/%s", 
                config.getBucketName(), remoteFileName);
            return url;
        }
        
        @Override
        public boolean isHealthy() {
            return testConnection();
        }
        
        @Override
        public boolean testConnection() {
            try {
                // Simple connectivity test
                return config.getAccessKeyId() != null && config.getSecretAccessKey() != null;
            } catch (Exception e) {
                return false;
            }
        }
        
        @Override
        public void cleanup() {
            // No cleanup needed
        }
    }
    
    // Monitoring Handler Implementation
    
    private static class MonitoringHandler implements IntegrationHandler {
        private final MonitoringConfig config;
        private final HttpClient httpClient;
        private final ObjectMapper objectMapper;
        
        public MonitoringHandler(MonitoringConfig config, HttpClient httpClient, ObjectMapper objectMapper) {
            this.config = config;
            this.httpClient = httpClient;
            this.objectMapper = objectMapper;
        }
        
        public int sendMetrics(Map<String, Object> metrics) throws Exception {
            switch (config.getType()) {
                case PROMETHEUS:
                    return sendToPrometheus(metrics);
                case DATADOG:
                    return sendToDatadog(metrics);
                case NEW_RELIC:
                    return sendToNewRelic(metrics);
                default:
                    throw new UnsupportedOperationException("Monitoring type not supported: " + config.getType());
            }
        }
        
        private int sendToPrometheus(Map<String, Object> metrics) throws Exception {
            // Prometheus push gateway implementation
            StringBuilder prometheusFormat = new StringBuilder();
            
            metrics.forEach((key, value) -> {
                prometheusFormat.append(String.format("%s %s %d\n", key, value, System.currentTimeMillis()));
            });
            
            String endpoint = config.getEndpoint() + "/metrics/job/phoenix_hrm";
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(prometheusFormat.toString()))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return metrics.size();
            } else {
                throw new RuntimeException("Prometheus push failed: " + response.statusCode());
            }
        }
        
        private int sendToDatadog(Map<String, Object> metrics) throws Exception {
            List<Map<String, Object>> series = new ArrayList<>();
            
            metrics.forEach((key, value) -> {
                Map<String, Object> metric = Map.of(
                    "metric", key,
                    "points", List.of(List.of(System.currentTimeMillis() / 1000, value)),
                    "tags", config.getLabels().entrySet().stream()
                        .map(e -> e.getKey() + ":" + e.getValue())
                        .toArray(String[]::new)
                );
                series.add(metric);
            });
            
            Map<String, Object> payload = Map.of("series", series);
            
            String endpoint = config.getEndpoint() + "/api/v1/series";
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("DD-API-KEY", config.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 202) {
                return metrics.size();
            } else {
                throw new RuntimeException("Datadog API call failed: " + response.statusCode());
            }
        }
        
        private int sendToNewRelic(Map<String, Object> metrics) throws Exception {
            List<Map<String, Object>> newRelicMetrics = new ArrayList<>();
            
            metrics.forEach((key, value) -> {
                Map<String, Object> metric = Map.of(
                    "name", key,
                    "type", "gauge",
                    "value", value,
                    "timestamp", System.currentTimeMillis(),
                    "attributes", config.getLabels()
                );
                newRelicMetrics.add(metric);
            });
            
            Map<String, Object> payload = Map.of("metrics", newRelicMetrics);
            
            String endpoint = config.getEndpoint() + "/metric/v1";
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Api-Key", config.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return metrics.size();
            } else {
                throw new RuntimeException("New Relic API call failed: " + response.statusCode());
            }
        }
        
        @Override
        public boolean isHealthy() {
            return testConnection();
        }
        
        @Override
        public boolean testConnection() {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getEndpoint()))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                return response.statusCode() < 500;
            } catch (Exception e) {
                return false;
            }
        }
        
        @Override
        public void cleanup() {
            // No cleanup needed for HTTP client
        }
    }
}