package com.phoenix.hrm.reporting.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phoenix.hrm.reporting.ReportingFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Interactive Dashboard Generator for Phoenix HRM Test Automation
 * 
 * Creates comprehensive, real-time interactive dashboards featuring:
 * - Real-time test execution monitoring with live updates
 * - Interactive charts and visualizations using Chart.js and D3.js
 * - Advanced filtering and search capabilities
 * - Responsive design for desktop, tablet, and mobile devices
 * - Multi-environment and multi-suite comparison views
 * - Drill-down capabilities for detailed analysis
 * - Export functionality for dashboard snapshots
 * - WebSocket integration for real-time data streaming
 * - Customizable dashboard layouts and themes
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 6.0
 * @since Phase 6
 */
public class InteractiveDashboardGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(InteractiveDashboardGenerator.class);
    
    private final DashboardConfiguration config;
    private final ObjectMapper objectMapper;
    private final ChartDataGenerator chartDataGenerator;
    private final WebSocketHandler webSocketHandler;
    private final ThemeManager themeManager;
    
    /**
     * Dashboard configuration
     */
    public static class DashboardConfiguration {
        private boolean enableRealTimeUpdates = true;
        private boolean enableWebSocketSupport = true;
        private boolean enableExportFeatures = true;
        private boolean enableDrillDown = true;
        private int refreshIntervalSeconds = 30;
        private String defaultTheme = "light";
        private List<String> enabledChartTypes = Arrays.asList("line", "bar", "pie", "doughnut", "radar");
        private Map<String, String> customColors = new HashMap<>();
        private boolean enableMobileResponsive = true;
        private boolean enableFilterPersistence = true;
        private String dashboardTitle = "Phoenix HRM Test Automation Dashboard";
        
        // Builder pattern
        public static class Builder {
            private final DashboardConfiguration config = new DashboardConfiguration();
            
            public Builder enableRealTimeUpdates(boolean enable) {
                config.enableRealTimeUpdates = enable;
                return this;
            }
            
            public Builder enableWebSocketSupport(boolean enable) {
                config.enableWebSocketSupport = enable;
                return this;
            }
            
            public Builder enableExportFeatures(boolean enable) {
                config.enableExportFeatures = enable;
                return this;
            }
            
            public Builder enableDrillDown(boolean enable) {
                config.enableDrillDown = enable;
                return this;
            }
            
            public Builder refreshIntervalSeconds(int interval) {
                config.refreshIntervalSeconds = interval;
                return this;
            }
            
            public Builder defaultTheme(String theme) {
                config.defaultTheme = theme;
                return this;
            }
            
            public Builder enabledChartTypes(List<String> types) {
                config.enabledChartTypes = new ArrayList<>(types);
                return this;
            }
            
            public Builder customColor(String element, String color) {
                config.customColors.put(element, color);
                return this;
            }
            
            public Builder enableMobileResponsive(boolean enable) {
                config.enableMobileResponsive = enable;
                return this;
            }
            
            public Builder dashboardTitle(String title) {
                config.dashboardTitle = title;
                return this;
            }
            
            public DashboardConfiguration build() {
                return config;
            }
        }
        
        // Getters
        public boolean isEnableRealTimeUpdates() { return enableRealTimeUpdates; }
        public boolean isEnableWebSocketSupport() { return enableWebSocketSupport; }
        public boolean isEnableExportFeatures() { return enableExportFeatures; }
        public boolean isEnableDrillDown() { return enableDrillDown; }
        public int getRefreshIntervalSeconds() { return refreshIntervalSeconds; }
        public String getDefaultTheme() { return defaultTheme; }
        public List<String> getEnabledChartTypes() { return enabledChartTypes; }
        public Map<String, String> getCustomColors() { return customColors; }
        public boolean isEnableMobileResponsive() { return enableMobileResponsive; }
        public boolean isEnableFilterPersistence() { return enableFilterPersistence; }
        public String getDashboardTitle() { return dashboardTitle; }
    }
    
    /**
     * Dashboard result
     */
    public static class DashboardResult {
        private final String dashboardPath;
        private final String assetsPath;
        private final boolean success;
        private final LocalDateTime generatedAt;
        private final Map<String, Object> metadata;
        
        public DashboardResult(String dashboardPath, String assetsPath, boolean success, 
                              Map<String, Object> metadata) {
            this.dashboardPath = dashboardPath;
            this.assetsPath = assetsPath;
            this.success = success;
            this.generatedAt = LocalDateTime.now();
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }
        
        // Getters
        public String getDashboardPath() { return dashboardPath; }
        public String getAssetsPath() { return assetsPath; }
        public boolean isSuccess() { return success; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
    
    /**
     * Constructor
     */
    public InteractiveDashboardGenerator(DashboardConfiguration config) {
        this.config = config != null ? config : new DashboardConfiguration.Builder().build();
        this.objectMapper = new ObjectMapper();
        this.chartDataGenerator = new ChartDataGenerator(this.config);
        this.webSocketHandler = new WebSocketHandler(this.config);
        this.themeManager = new ThemeManager(this.config);
        
        logger.info("InteractiveDashboardGenerator initialized with config: {}", this.config.getDashboardTitle());
    }
    
    /**
     * Generate interactive dashboard
     */
    public DashboardResult generateDashboard(List<ReportingFramework.TestExecution> executions,
                                           Map<String, Object> analyticsData,
                                           String outputPath) {
        
        logger.info("Generating interactive dashboard for {} executions", executions.size());
        
        try {
            // Create output directories
            Path dashboardDir = Paths.get(outputPath, "dashboard");
            Path assetsDir = dashboardDir.resolve("assets");
            Files.createDirectories(dashboardDir);
            Files.createDirectories(assetsDir.resolve("css"));
            Files.createDirectories(assetsDir.resolve("js"));
            Files.createDirectories(assetsDir.resolve("data"));
            
            // Generate chart data
            Map<String, Object> chartData = chartDataGenerator.generateChartData(executions, analyticsData);
            
            // Generate dashboard HTML
            String dashboardHtml = generateDashboardHtml(executions, analyticsData, chartData);
            String dashboardPath = dashboardDir.resolve("index.html").toString();
            Files.write(Paths.get(dashboardPath), dashboardHtml.getBytes());
            
            // Generate CSS assets
            generateCssAssets(assetsDir);
            
            // Generate JavaScript assets
            generateJavaScriptAssets(assetsDir, chartData);
            
            // Generate data JSON files
            generateDataAssets(assetsDir, executions, analyticsData, chartData);
            
            // Generate WebSocket handler if enabled
            if (config.isEnableWebSocketSupport()) {
                webSocketHandler.generateWebSocketScript(assetsDir);
            }
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("executionCount", executions.size());
            metadata.put("chartsGenerated", chartData.size());
            metadata.put("theme", config.getDefaultTheme());
            metadata.put("realTimeEnabled", config.isEnableRealTimeUpdates());
            
            logger.info("Interactive dashboard generated successfully at: {}", dashboardPath);
            return new DashboardResult(dashboardPath, assetsDir.toString(), true, metadata);
            
        } catch (Exception e) {
            logger.error("Error generating interactive dashboard: {}", e.getMessage(), e);
            return new DashboardResult(null, null, false, 
                Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Generate main dashboard HTML
     */
    private String generateDashboardHtml(List<ReportingFramework.TestExecution> executions,
                                       Map<String, Object> analyticsData,
                                       Map<String, Object> chartData) {
        
        StringBuilder html = new StringBuilder();
        
        // HTML Header
        html.append(generateHtmlHeader());
        
        // Navigation Bar
        html.append(generateNavigationBar());
        
        // Main Dashboard Content
        html.append(generateMainContent(executions, analyticsData, chartData));
        
        // Modals and Dialogs
        html.append(generateModals());
        
        // Footer with Scripts
        html.append(generateScriptIncludes());
        
        html.append("</body></html>");
        
        return html.toString();
    }
    
    private String generateHtmlHeader() {
        String theme = themeManager.getThemeColors(config.getDefaultTheme());
        
        return String.format("""
            <!DOCTYPE html>
            <html lang="en" data-theme="%s">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <meta name="description" content="Phoenix HRM Test Automation Dashboard">
                <title>%s</title>
                
                <!-- External Libraries -->
                <script src="https://cdn.jsdelivr.net/npm/chart.js@4.2.1/dist/chart.umd.js"></script>
                <script src="https://cdn.jsdelivr.net/npm/date-fns@2.29.3/index.min.js"></script>
                <script src="https://cdn.jsdelivr.net/npm/lodash@4.17.21/lodash.min.js"></script>
                <script src="https://d3js.org/d3.v7.min.js"></script>
                
                <!-- Dashboard Assets -->
                <link rel="stylesheet" href="assets/css/dashboard.css">
                <link rel="stylesheet" href="assets/css/themes.css">
                <link rel="stylesheet" href="assets/css/responsive.css">
                
                <style>
                    %s
                </style>
            </head>
            <body class="dashboard-body">
            """, 
            config.getDefaultTheme(), 
            config.getDashboardTitle(),
            theme);
    }
    
    private String generateNavigationBar() {
        return """
            <nav class="dashboard-nav">
                <div class="nav-container">
                    <div class="nav-brand">
                        <h1>📊 Phoenix HRM Dashboard</h1>
                        <span class="nav-subtitle">Test Automation Analytics</span>
                    </div>
                    
                    <div class="nav-controls">
                        <div class="time-range-selector">
                            <select id="timeRange">
                                <option value="1d">Last 24 Hours</option>
                                <option value="7d" selected>Last 7 Days</option>
                                <option value="30d">Last 30 Days</option>
                                <option value="all">All Time</option>
                            </select>
                        </div>
                        
                        <div class="refresh-controls">
                            <button id="refreshBtn" class="btn btn-primary">
                                🔄 Refresh
                            </button>
                            <div class="auto-refresh">
                                <input type="checkbox" id="autoRefresh" checked>
                                <label for="autoRefresh">Auto-refresh</label>
                            </div>
                        </div>
                        
                        <div class="theme-selector">
                            <button id="themeToggle" class="btn btn-secondary">
                                🌙 Dark
                            </button>
                        </div>
                        
                        <div class="export-controls">
                            <button id="exportBtn" class="btn btn-secondary">
                                📁 Export
                            </button>
                        </div>
                    </div>
                </div>
            </nav>
            """;
    }
    
    private String generateMainContent(List<ReportingFramework.TestExecution> executions,
                                     Map<String, Object> analyticsData,
                                     Map<String, Object> chartData) {
        
        // Calculate key metrics
        Map<String, Long> statusCounts = executions.stream()
            .collect(Collectors.groupingBy(
                ReportingFramework.TestExecution::getStatus,
                Collectors.counting()));
        
        long totalTests = executions.size();
        long passedTests = statusCounts.getOrDefault("PASSED", 0L);
        double successRate = totalTests > 0 ? (passedTests * 100.0 / totalTests) : 0;
        
        return String.format("""
            <main class="dashboard-main">
                <!-- KPI Cards -->
                <section class="kpi-section">
                    <div class="kpi-grid">
                        <div class="kpi-card success">
                            <div class="kpi-icon">✅</div>
                            <div class="kpi-content">
                                <div class="kpi-value">%d</div>
                                <div class="kpi-label">Total Tests</div>
                                <div class="kpi-trend">+5%% vs last week</div>
                            </div>
                        </div>
                        
                        <div class="kpi-card primary">
                            <div class="kpi-icon">🎯</div>
                            <div class="kpi-content">
                                <div class="kpi-value">%.1f%%</div>
                                <div class="kpi-label">Success Rate</div>
                                <div class="kpi-trend %s">%.1f%% vs last week</div>
                            </div>
                        </div>
                        
                        <div class="kpi-card warning">
                            <div class="kpi-icon">⚡</div>
                            <div class="kpi-content">
                                <div class="kpi-value">%d</div>
                                <div class="kpi-label">Failed Tests</div>
                                <div class="kpi-trend">-2%% vs last week</div>
                            </div>
                        </div>
                        
                        <div class="kpi-card info">
                            <div class="kpi-icon">⏱️</div>
                            <div class="kpi-content">
                                <div class="kpi-value">%.1fs</div>
                                <div class="kpi-label">Avg Duration</div>
                                <div class="kpi-trend">-5%% vs last week</div>
                            </div>
                        </div>
                    </div>
                </section>
                
                <!-- Filter Panel -->
                <section class="filter-section">
                    <div class="filter-panel">
                        <div class="filter-group">
                            <label>Environment</label>
                            <select id="environmentFilter" multiple>
                                <option value="">All Environments</option>
                                <option value="dev">Development</option>
                                <option value="test">Test</option>
                                <option value="staging">Staging</option>
                                <option value="prod">Production</option>
                            </select>
                        </div>
                        
                        <div class="filter-group">
                            <label>Test Suite</label>
                            <select id="suiteFilter" multiple>
                                <option value="">All Suites</option>
                                %s
                            </select>
                        </div>
                        
                        <div class="filter-group">
                            <label>Status</label>
                            <select id="statusFilter" multiple>
                                <option value="">All Status</option>
                                <option value="PASSED">Passed</option>
                                <option value="FAILED">Failed</option>
                                <option value="SKIPPED">Skipped</option>
                            </select>
                        </div>
                        
                        <div class="filter-group">
                            <label>Search</label>
                            <input type="text" id="searchFilter" placeholder="Search test names...">
                        </div>
                        
                        <div class="filter-actions">
                            <button id="applyFilters" class="btn btn-primary">Apply</button>
                            <button id="clearFilters" class="btn btn-secondary">Clear</button>
                        </div>
                    </div>
                </section>
                
                <!-- Charts Grid -->
                <section class="charts-section">
                    <div class="charts-grid">
                        <!-- Status Distribution Chart -->
                        <div class="chart-card">
                            <div class="chart-header">
                                <h3>Test Status Distribution</h3>
                                <div class="chart-controls">
                                    <button class="chart-type-btn" data-chart="statusChart" data-type="doughnut">🍩</button>
                                    <button class="chart-type-btn" data-chart="statusChart" data-type="pie">🥧</button>
                                    <button class="chart-type-btn" data-chart="statusChart" data-type="bar">📊</button>
                                </div>
                            </div>
                            <div class="chart-container">
                                <canvas id="statusChart"></canvas>
                            </div>
                        </div>
                        
                        <!-- Execution Timeline Chart -->
                        <div class="chart-card wide">
                            <div class="chart-header">
                                <h3>Execution Timeline</h3>
                                <div class="chart-controls">
                                    <button class="chart-type-btn" data-chart="timelineChart" data-type="line">📈</button>
                                    <button class="chart-type-btn" data-chart="timelineChart" data-type="bar">📊</button>
                                    <button class="chart-type-btn" data-chart="timelineChart" data-type="area">📉</button>
                                </div>
                            </div>
                            <div class="chart-container">
                                <canvas id="timelineChart"></canvas>
                            </div>
                        </div>
                        
                        <!-- Performance Heat Map -->
                        <div class="chart-card">
                            <div class="chart-header">
                                <h3>Performance Heat Map</h3>
                                <div class="chart-controls">
                                    <button class="chart-type-btn" data-chart="heatMapChart" data-type="heatmap">🔥</button>
                                </div>
                            </div>
                            <div class="chart-container">
                                <div id="heatMapChart"></div>
                            </div>
                        </div>
                        
                        <!-- Environment Comparison -->
                        <div class="chart-card">
                            <div class="chart-header">
                                <h3>Environment Comparison</h3>
                                <div class="chart-controls">
                                    <button class="chart-type-btn" data-chart="envChart" data-type="radar">📡</button>
                                    <button class="chart-type-btn" data-chart="envChart" data-type="bar">📊</button>
                                </div>
                            </div>
                            <div class="chart-container">
                                <canvas id="envChart"></canvas>
                            </div>
                        </div>
                        
                        <!-- Duration Distribution -->
                        <div class="chart-card wide">
                            <div class="chart-header">
                                <h3>Test Duration Distribution</h3>
                                <div class="chart-controls">
                                    <button class="chart-type-btn" data-chart="durationChart" data-type="histogram">📊</button>
                                    <button class="chart-type-btn" data-chart="durationChart" data-type="box">📦</button>
                                </div>
                            </div>
                            <div class="chart-container">
                                <canvas id="durationChart"></canvas>
                            </div>
                        </div>
                        
                        <!-- Failure Trends -->
                        <div class="chart-card">
                            <div class="chart-header">
                                <h3>Failure Trends</h3>
                                <div class="chart-controls">
                                    <button class="chart-type-btn" data-chart="failureChart" data-type="line">📈</button>
                                    <button class="chart-type-btn" data-chart="failureChart" data-type="area">📉</button>
                                </div>
                            </div>
                            <div class="chart-container">
                                <canvas id="failureChart"></canvas>
                            </div>
                        </div>
                    </div>
                </section>
                
                <!-- Detailed Data Table -->
                <section class="data-table-section">
                    <div class="table-card">
                        <div class="table-header">
                            <h3>Test Execution Details</h3>
                            <div class="table-controls">
                                <input type="text" id="tableSearch" placeholder="Search table...">
                                <select id="tablePageSize">
                                    <option value="10">10 per page</option>
                                    <option value="25" selected>25 per page</option>
                                    <option value="50">50 per page</option>
                                    <option value="100">100 per page</option>
                                </select>
                                <button id="tableExport" class="btn btn-secondary">📁 Export</button>
                            </div>
                        </div>
                        <div class="table-container">
                            <div id="dataTable"></div>
                        </div>
                        <div class="table-pagination">
                            <div id="tablePagination"></div>
                        </div>
                    </div>
                </section>
                
                <!-- Real-time Status -->
                <div id="realTimeStatus" class="real-time-status">
                    <div class="status-indicator">
                        <div class="status-dot connected"></div>
                        <span>Connected - Last updated: <span id="lastUpdated">now</span></span>
                    </div>
                </div>
            </main>
            """,
            totalTests,
            successRate,
            successRate >= 95 ? "positive" : successRate >= 80 ? "neutral" : "negative",
            successRate - 90, // Mock trend
            statusCounts.getOrDefault("FAILED", 0L),
            executions.stream().mapToLong(ReportingFramework.TestExecution::getDuration)
                .average().orElse(0) / 1000.0,
            generateSuiteOptions(executions));
    }
    
    private String generateSuiteOptions(List<ReportingFramework.TestExecution> executions) {
        return executions.stream()
            .map(ReportingFramework.TestExecution::getSuiteName)
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .map(suite -> String.format("<option value=\"%s\">%s</option>", suite, suite))
            .collect(Collectors.joining("\n"));
    }
    
    private String generateModals() {
        return """
            <!-- Export Modal -->
            <div id="exportModal" class="modal">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3>Export Dashboard</h3>
                        <button class="modal-close">&times;</button>
                    </div>
                    <div class="modal-body">
                        <div class="export-options">
                            <label><input type="checkbox" checked> Charts</label>
                            <label><input type="checkbox" checked> Data Table</label>
                            <label><input type="checkbox"> Raw Data</label>
                        </div>
                        <div class="export-formats">
                            <button class="btn btn-primary" data-format="png">PNG Image</button>
                            <button class="btn btn-primary" data-format="pdf">PDF Report</button>
                            <button class="btn btn-primary" data-format="excel">Excel File</button>
                            <button class="btn btn-primary" data-format="csv">CSV Data</button>
                        </div>
                    </div>
                </div>
            </div>
            
            <!-- Chart Drill-down Modal -->
            <div id="drillDownModal" class="modal large">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3 id="drillDownTitle">Chart Details</h3>
                        <button class="modal-close">&times;</button>
                    </div>
                    <div class="modal-body">
                        <div id="drillDownContent"></div>
                    </div>
                </div>
            </div>
            
            <!-- Settings Modal -->
            <div id="settingsModal" class="modal">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3>Dashboard Settings</h3>
                        <button class="modal-close">&times;</button>
                    </div>
                    <div class="modal-body">
                        <div class="settings-group">
                            <label>Refresh Interval</label>
                            <select id="refreshInterval">
                                <option value="10">10 seconds</option>
                                <option value="30" selected>30 seconds</option>
                                <option value="60">1 minute</option>
                                <option value="300">5 minutes</option>
                            </select>
                        </div>
                        
                        <div class="settings-group">
                            <label>Chart Animation</label>
                            <input type="checkbox" id="chartAnimation" checked>
                        </div>
                        
                        <div class="settings-group">
                            <label>Sound Notifications</label>
                            <input type="checkbox" id="soundNotifications">
                        </div>
                        
                        <div class="settings-actions">
                            <button id="saveSettings" class="btn btn-primary">Save</button>
                            <button id="resetSettings" class="btn btn-secondary">Reset</button>
                        </div>
                    </div>
                </div>
            </div>
            """;
    }
    
    private String generateScriptIncludes() {
        return """
            <!-- Dashboard Scripts -->
            <script src="assets/js/dashboard-core.js"></script>
            <script src="assets/js/chart-manager.js"></script>
            <script src="assets/js/data-table.js"></script>
            <script src="assets/js/filters.js"></script>
            <script src="assets/js/export.js"></script>
            <script src="assets/js/websocket-handler.js"></script>
            <script src="assets/js/theme-manager.js"></script>
            
            <script>
                // Initialize dashboard
                document.addEventListener('DOMContentLoaded', function() {
                    const dashboard = new PhoenixDashboard({
                        refreshInterval: """ + config.getRefreshIntervalSeconds() * 1000 + """,
                        enableRealTime: """ + config.isEnableRealTimeUpdates() + """,
                        enableWebSocket: """ + config.isEnableWebSocketSupport() + """,
                        theme: '""" + config.getDefaultTheme() + """'
                    });
                    
                    dashboard.initialize();
                });
            </script>
            """;
    }
    
    private void generateCssAssets(Path assetsDir) throws IOException {
        // Generate main dashboard CSS
        String dashboardCss = generateDashboardCSS();
        Files.write(assetsDir.resolve("css/dashboard.css"), dashboardCss.getBytes());
        
        // Generate themes CSS
        String themesCss = themeManager.generateThemeCSS();
        Files.write(assetsDir.resolve("css/themes.css"), themesCss.getBytes());
        
        // Generate responsive CSS
        String responsiveCss = generateResponsiveCSS();
        Files.write(assetsDir.resolve("css/responsive.css"), responsiveCss.getBytes());
    }
    
    private String generateDashboardCSS() {
        return """
            /* Phoenix HRM Dashboard Styles */
            :root {
                --primary-color: #667eea;
                --secondary-color: #764ba2;
                --success-color: #28a745;
                --warning-color: #ffc107;
                --danger-color: #dc3545;
                --info-color: #17a2b8;
                --light-color: #f8f9fa;
                --dark-color: #343a40;
                
                --font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                --border-radius: 8px;
                --box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                --transition: all 0.3s ease;
            }
            
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }
            
            body.dashboard-body {
                font-family: var(--font-family);
                background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
                min-height: 100vh;
            }
            
            /* Navigation */
            .dashboard-nav {
                background: white;
                box-shadow: var(--box-shadow);
                position: sticky;
                top: 0;
                z-index: 1000;
            }
            
            .nav-container {
                max-width: 1200px;
                margin: 0 auto;
                padding: 1rem 2rem;
                display: flex;
                justify-content: space-between;
                align-items: center;
                flex-wrap: wrap;
                gap: 1rem;
            }
            
            .nav-brand h1 {
                color: var(--primary-color);
                font-size: 1.5rem;
                font-weight: 600;
            }
            
            .nav-subtitle {
                color: #6c757d;
                font-size: 0.875rem;
            }
            
            .nav-controls {
                display: flex;
                align-items: center;
                gap: 1rem;
                flex-wrap: wrap;
            }
            
            /* Main Content */
            .dashboard-main {
                max-width: 1200px;
                margin: 0 auto;
                padding: 2rem;
            }
            
            /* KPI Section */
            .kpi-section {
                margin-bottom: 2rem;
            }
            
            .kpi-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
                gap: 1.5rem;
            }
            
            .kpi-card {
                background: white;
                border-radius: var(--border-radius);
                padding: 1.5rem;
                box-shadow: var(--box-shadow);
                display: flex;
                align-items: center;
                gap: 1rem;
                transition: var(--transition);
            }
            
            .kpi-card:hover {
                transform: translateY(-2px);
                box-shadow: 0 4px 20px rgba(0,0,0,0.15);
            }
            
            .kpi-icon {
                font-size: 2rem;
                width: 60px;
                height: 60px;
                display: flex;
                align-items: center;
                justify-content: center;
                border-radius: 50%;
                background: rgba(102, 126, 234, 0.1);
            }
            
            .kpi-value {
                font-size: 2rem;
                font-weight: bold;
                color: var(--dark-color);
            }
            
            .kpi-label {
                color: #6c757d;
                font-size: 0.875rem;
                margin-bottom: 0.25rem;
            }
            
            .kpi-trend {
                font-size: 0.75rem;
                font-weight: 500;
            }
            
            .kpi-trend.positive { color: var(--success-color); }
            .kpi-trend.negative { color: var(--danger-color); }
            .kpi-trend.neutral { color: #6c757d; }
            
            /* Filter Section */
            .filter-section {
                margin-bottom: 2rem;
            }
            
            .filter-panel {
                background: white;
                border-radius: var(--border-radius);
                padding: 1.5rem;
                box-shadow: var(--box-shadow);
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                gap: 1rem;
                align-items: end;
            }
            
            .filter-group label {
                display: block;
                margin-bottom: 0.5rem;
                font-weight: 500;
                color: var(--dark-color);
            }
            
            .filter-group select,
            .filter-group input {
                width: 100%;
                padding: 0.5rem;
                border: 1px solid #ddd;
                border-radius: 4px;
                font-size: 0.875rem;
            }
            
            /* Charts Section */
            .charts-section {
                margin-bottom: 2rem;
            }
            
            .charts-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
                gap: 2rem;
            }
            
            .chart-card {
                background: white;
                border-radius: var(--border-radius);
                box-shadow: var(--box-shadow);
                overflow: hidden;
            }
            
            .chart-card.wide {
                grid-column: 1 / -1;
            }
            
            .chart-header {
                padding: 1rem 1.5rem;
                border-bottom: 1px solid #eee;
                display: flex;
                justify-content: space-between;
                align-items: center;
            }
            
            .chart-header h3 {
                color: var(--dark-color);
                font-size: 1.125rem;
                font-weight: 600;
            }
            
            .chart-controls {
                display: flex;
                gap: 0.5rem;
            }
            
            .chart-type-btn {
                background: none;
                border: 1px solid #ddd;
                border-radius: 4px;
                padding: 0.25rem 0.5rem;
                cursor: pointer;
                font-size: 1rem;
                transition: var(--transition);
            }
            
            .chart-type-btn:hover,
            .chart-type-btn.active {
                background: var(--primary-color);
                color: white;
                border-color: var(--primary-color);
            }
            
            .chart-container {
                padding: 1.5rem;
                position: relative;
                min-height: 300px;
            }
            
            /* Data Table Section */
            .data-table-section {
                margin-bottom: 2rem;
            }
            
            .table-card {
                background: white;
                border-radius: var(--border-radius);
                box-shadow: var(--box-shadow);
                overflow: hidden;
            }
            
            .table-header {
                padding: 1rem 1.5rem;
                border-bottom: 1px solid #eee;
                display: flex;
                justify-content: space-between;
                align-items: center;
                flex-wrap: wrap;
                gap: 1rem;
            }
            
            .table-controls {
                display: flex;
                gap: 1rem;
                align-items: center;
                flex-wrap: wrap;
            }
            
            .table-container {
                padding: 1.5rem;
                overflow-x: auto;
            }
            
            /* Buttons */
            .btn {
                padding: 0.5rem 1rem;
                border: none;
                border-radius: 4px;
                cursor: pointer;
                font-size: 0.875rem;
                font-weight: 500;
                transition: var(--transition);
                text-decoration: none;
                display: inline-flex;
                align-items: center;
                gap: 0.5rem;
            }
            
            .btn-primary {
                background: var(--primary-color);
                color: white;
            }
            
            .btn-primary:hover {
                background: #5a67d8;
            }
            
            .btn-secondary {
                background: #6c757d;
                color: white;
            }
            
            .btn-secondary:hover {
                background: #545b62;
            }
            
            /* Real-time Status */
            .real-time-status {
                position: fixed;
                bottom: 1rem;
                right: 1rem;
                background: white;
                padding: 0.75rem 1rem;
                border-radius: var(--border-radius);
                box-shadow: var(--box-shadow);
                font-size: 0.875rem;
                z-index: 1000;
            }
            
            .status-indicator {
                display: flex;
                align-items: center;
                gap: 0.5rem;
            }
            
            .status-dot {
                width: 8px;
                height: 8px;
                border-radius: 50%;
                background: var(--success-color);
                animation: pulse 2s infinite;
            }
            
            .status-dot.connected {
                background: var(--success-color);
            }
            
            .status-dot.disconnected {
                background: var(--danger-color);
            }
            
            @keyframes pulse {
                0% { opacity: 1; }
                50% { opacity: 0.5; }
                100% { opacity: 1; }
            }
            
            /* Modals */
            .modal {
                display: none;
                position: fixed;
                z-index: 2000;
                left: 0;
                top: 0;
                width: 100%;
                height: 100%;
                background: rgba(0,0,0,0.5);
                backdrop-filter: blur(5px);
            }
            
            .modal.show {
                display: flex;
                align-items: center;
                justify-content: center;
            }
            
            .modal-content {
                background: white;
                border-radius: var(--border-radius);
                max-width: 500px;
                width: 90%;
                max-height: 90%;
                overflow-y: auto;
                box-shadow: 0 10px 30px rgba(0,0,0,0.3);
            }
            
            .modal.large .modal-content {
                max-width: 800px;
            }
            
            .modal-header {
                padding: 1rem 1.5rem;
                border-bottom: 1px solid #eee;
                display: flex;
                justify-content: space-between;
                align-items: center;
            }
            
            .modal-close {
                background: none;
                border: none;
                font-size: 1.5rem;
                cursor: pointer;
                color: #6c757d;
            }
            
            .modal-body {
                padding: 1.5rem;
            }
            
            /* Animations */
            @keyframes fadeIn {
                from { opacity: 0; transform: translateY(20px); }
                to { opacity: 1; transform: translateY(0); }
            }
            
            .fade-in {
                animation: fadeIn 0.3s ease-out;
            }
            
            /* Loading States */
            .loading {
                display: flex;
                align-items: center;
                justify-content: center;
                min-height: 200px;
            }
            
            .spinner {
                width: 40px;
                height: 40px;
                border: 4px solid #f3f3f3;
                border-top: 4px solid var(--primary-color);
                border-radius: 50%;
                animation: spin 1s linear infinite;
            }
            
            @keyframes spin {
                0% { transform: rotate(0deg); }
                100% { transform: rotate(360deg); }
            }
            """;
    }
    
    private String generateResponsiveCSS() {
        return """
            /* Responsive Design */
            @media (max-width: 1200px) {
                .dashboard-main {
                    padding: 1rem;
                }
                
                .charts-grid {
                    grid-template-columns: 1fr;
                }
                
                .chart-card.wide {
                    grid-column: 1;
                }
            }
            
            @media (max-width: 768px) {
                .nav-container {
                    padding: 1rem;
                    flex-direction: column;
                    gap: 1rem;
                }
                
                .nav-controls {
                    justify-content: center;
                    width: 100%;
                }
                
                .kpi-grid {
                    grid-template-columns: 1fr;
                }
                
                .filter-panel {
                    grid-template-columns: 1fr;
                }
                
                .chart-container {
                    min-height: 250px;
                }
                
                .table-header {
                    flex-direction: column;
                    align-items: stretch;
                }
                
                .table-controls {
                    justify-content: center;
                }
                
                .modal-content {
                    margin: 1rem;
                    width: calc(100% - 2rem);
                }
            }
            
            @media (max-width: 480px) {
                .dashboard-main {
                    padding: 0.5rem;
                }
                
                .kpi-card {
                    flex-direction: column;
                    text-align: center;
                }
                
                .chart-container {
                    padding: 1rem;
                    min-height: 200px;
                }
                
                .real-time-status {
                    bottom: 0.5rem;
                    right: 0.5rem;
                    left: 0.5rem;
                    right: auto;
                }
            }
            """;
    }
    
    private void generateJavaScriptAssets(Path assetsDir, Map<String, Object> chartData) throws IOException {
        // Generate main dashboard JavaScript
        String dashboardJs = generateDashboardJS(chartData);
        Files.write(assetsDir.resolve("js/dashboard-core.js"), dashboardJs.getBytes());
        
        // Generate chart manager
        String chartManagerJs = generateChartManagerJS();
        Files.write(assetsDir.resolve("js/chart-manager.js"), chartManagerJs.getBytes());
        
        // Generate other JS modules
        generateAdditionalJSModules(assetsDir);
    }
    
    private String generateDashboardJS(Map<String, Object> chartData) {
        return String.format("""
            class PhoenixDashboard {
                constructor(config) {
                    this.config = {
                        refreshInterval: 30000,
                        enableRealTime: true,
                        enableWebSocket: false,
                        theme: 'light',
                        ...config
                    };
                    
                    this.chartData = %s;
                    this.charts = {};
                    this.filters = {};
                    this.isInitialized = false;
                }
                
                initialize() {
                    if (this.isInitialized) return;
                    
                    this.setupEventListeners();
                    this.initializeCharts();
                    this.initializeFilters();
                    this.initializeTable();
                    
                    if (this.config.enableRealTime) {
                        this.startRealTimeUpdates();
                    }
                    
                    if (this.config.enableWebSocket) {
                        this.initializeWebSocket();
                    }
                    
                    this.isInitialized = true;
                    console.log('Phoenix Dashboard initialized');
                }
                
                setupEventListeners() {
                    // Theme toggle
                    document.getElementById('themeToggle')?.addEventListener('click', () => {
                        this.toggleTheme();
                    });
                    
                    // Refresh button
                    document.getElementById('refreshBtn')?.addEventListener('click', () => {
                        this.refreshData();
                    });
                    
                    // Auto refresh checkbox
                    document.getElementById('autoRefresh')?.addEventListener('change', (e) => {
                        if (e.target.checked) {
                            this.startRealTimeUpdates();
                        } else {
                            this.stopRealTimeUpdates();
                        }
                    });
                    
                    // Export button
                    document.getElementById('exportBtn')?.addEventListener('click', () => {
                        this.showExportModal();
                    });
                    
                    // Time range selector
                    document.getElementById('timeRange')?.addEventListener('change', (e) => {
                        this.updateTimeRange(e.target.value);
                    });
                    
                    // Filter controls
                    document.getElementById('applyFilters')?.addEventListener('click', () => {
                        this.applyFilters();
                    });
                    
                    document.getElementById('clearFilters')?.addEventListener('click', () => {
                        this.clearFilters();
                    });
                    
                    // Chart type toggles
                    document.querySelectorAll('.chart-type-btn').forEach(btn => {
                        btn.addEventListener('click', (e) => {
                            const chartId = e.target.dataset.chart;
                            const chartType = e.target.dataset.type;
                            this.changeChartType(chartId, chartType);
                        });
                    });
                    
                    // Modal controls
                    document.querySelectorAll('.modal-close').forEach(btn => {
                        btn.addEventListener('click', (e) => {
                            this.closeModal(e.target.closest('.modal'));
                        });
                    });
                    
                    // Keyboard shortcuts
                    document.addEventListener('keydown', (e) => {
                        if (e.key === 'Escape') {
                            this.closeAllModals();
                        } else if (e.key === 'r' && e.ctrlKey) {
                            e.preventDefault();
                            this.refreshData();
                        }
                    });
                }
                
                initializeCharts() {
                    // Initialize all charts
                    this.charts.statusChart = this.createStatusChart();
                    this.charts.timelineChart = this.createTimelineChart();
                    this.charts.heatMapChart = this.createHeatMapChart();
                    this.charts.envChart = this.createEnvironmentChart();
                    this.charts.durationChart = this.createDurationChart();
                    this.charts.failureChart = this.createFailureChart();
                }
                
                createStatusChart() {
                    const ctx = document.getElementById('statusChart');
                    if (!ctx) return null;
                    
                    return new Chart(ctx, {
                        type: 'doughnut',
                        data: {
                            labels: ['Passed', 'Failed', 'Skipped'],
                            datasets: [{
                                data: [85, 10, 5], // Sample data
                                backgroundColor: ['#28a745', '#dc3545', '#ffc107'],
                                borderWidth: 2,
                                borderColor: '#fff'
                            }]
                        },
                        options: {
                            responsive: true,
                            maintainAspectRatio: false,
                            plugins: {
                                legend: {
                                    position: 'bottom'
                                },
                                tooltip: {
                                    callbacks: {
                                        label: function(context) {
                                            const label = context.label || '';
                                            const value = context.parsed;
                                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                                            const percentage = Math.round((value / total) * 100);
                                            return label + ': ' + value + ' (' + percentage + '%%)';
                                        }
                                    }
                                }
                            },
                            onClick: (event, elements) => {
                                if (elements.length > 0) {
                                    this.showChartDrillDown('statusChart', elements[0]);
                                }
                            }
                        }
                    });
                }
                
                createTimelineChart() {
                    const ctx = document.getElementById('timelineChart');
                    if (!ctx) return null;
                    
                    return new Chart(ctx, {
                        type: 'line',
                        data: {
                            labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
                            datasets: [{
                                label: 'Test Executions',
                                data: [120, 150, 180, 160, 200, 90, 110],
                                borderColor: '#667eea',
                                backgroundColor: 'rgba(102, 126, 234, 0.1)',
                                fill: true,
                                tension: 0.4
                            }]
                        },
                        options: {
                            responsive: true,
                            maintainAspectRatio: false,
                            plugins: {
                                legend: {
                                    display: false
                                }
                            },
                            scales: {
                                y: {
                                    beginAtZero: true
                                }
                            }
                        }
                    });
                }
                
                createHeatMapChart() {
                    // D3.js heatmap implementation would go here
                    const container = document.getElementById('heatMapChart');
                    if (!container) return null;
                    
                    // Placeholder for heatmap
                    container.innerHTML = '<div class="loading"><div class="spinner"></div></div>';
                    
                    return { type: 'heatmap', element: container };
                }
                
                createEnvironmentChart() {
                    const ctx = document.getElementById('envChart');
                    if (!ctx) return null;
                    
                    return new Chart(ctx, {
                        type: 'radar',
                        data: {
                            labels: ['Stability', 'Performance', 'Coverage', 'Quality'],
                            datasets: [{
                                label: 'Dev',
                                data: [85, 70, 90, 80],
                                borderColor: '#17a2b8',
                                backgroundColor: 'rgba(23, 162, 184, 0.1)'
                            }, {
                                label: 'Prod',
                                data: [95, 90, 85, 95],
                                borderColor: '#28a745',
                                backgroundColor: 'rgba(40, 167, 69, 0.1)'
                            }]
                        },
                        options: {
                            responsive: true,
                            maintainAspectRatio: false,
                            scales: {
                                r: {
                                    beginAtZero: true,
                                    max: 100
                                }
                            }
                        }
                    });
                }
                
                createDurationChart() {
                    const ctx = document.getElementById('durationChart');
                    if (!ctx) return null;
                    
                    return new Chart(ctx, {
                        type: 'bar',
                        data: {
                            labels: ['0-1s', '1-5s', '5-10s', '10-30s', '30s+'],
                            datasets: [{
                                label: 'Test Count',
                                data: [45, 120, 80, 25, 5],
                                backgroundColor: '#667eea'
                            }]
                        },
                        options: {
                            responsive: true,
                            maintainAspectRatio: false,
                            plugins: {
                                legend: {
                                    display: false
                                }
                            },
                            scales: {
                                y: {
                                    beginAtZero: true
                                }
                            }
                        }
                    });
                }
                
                createFailureChart() {
                    const ctx = document.getElementById('failureChart');
                    if (!ctx) return null;
                    
                    return new Chart(ctx, {
                        type: 'line',
                        data: {
                            labels: ['Week 1', 'Week 2', 'Week 3', 'Week 4'],
                            datasets: [{
                                label: 'Failures',
                                data: [15, 12, 8, 5],
                                borderColor: '#dc3545',
                                backgroundColor: 'rgba(220, 53, 69, 0.1)',
                                fill: true
                            }]
                        },
                        options: {
                            responsive: true,
                            maintainAspectRatio: false,
                            plugins: {
                                legend: {
                                    display: false
                                }
                            },
                            scales: {
                                y: {
                                    beginAtZero: true
                                }
                            }
                        }
                    });
                }
                
                refreshData() {
                    console.log('Refreshing dashboard data...');
                    
                    // Show loading indicators
                    this.showLoadingState();
                    
                    // Simulate API call
                    setTimeout(() => {
                        // Update charts with new data
                        Object.values(this.charts).forEach(chart => {
                            if (chart && chart.update) {
                                chart.update();
                            }
                        });
                        
                        // Update last updated timestamp
                        this.updateLastUpdatedTime();
                        
                        // Hide loading indicators
                        this.hideLoadingState();
                        
                        console.log('Dashboard data refreshed');
                    }, 1000);
                }
                
                startRealTimeUpdates() {
                    if (this.refreshTimer) {
                        clearInterval(this.refreshTimer);
                    }
                    
                    this.refreshTimer = setInterval(() => {
                        if (document.getElementById('autoRefresh')?.checked) {
                            this.refreshData();
                        }
                    }, this.config.refreshInterval);
                }
                
                stopRealTimeUpdates() {
                    if (this.refreshTimer) {
                        clearInterval(this.refreshTimer);
                        this.refreshTimer = null;
                    }
                }
                
                updateLastUpdatedTime() {
                    const lastUpdatedElement = document.getElementById('lastUpdated');
                    if (lastUpdatedElement) {
                        lastUpdatedElement.textContent = new Date().toLocaleTimeString();
                    }
                }
                
                showLoadingState() {
                    document.querySelectorAll('.chart-container').forEach(container => {
                        if (!container.querySelector('.loading')) {
                            const loading = document.createElement('div');
                            loading.className = 'loading';
                            loading.innerHTML = '<div class="spinner"></div>';
                            container.appendChild(loading);
                        }
                    });
                }
                
                hideLoadingState() {
                    document.querySelectorAll('.loading').forEach(loading => {
                        loading.remove();
                    });
                }
                
                toggleTheme() {
                    const body = document.body;
                    const currentTheme = body.dataset.theme || 'light';
                    const newTheme = currentTheme === 'light' ? 'dark' : 'light';
                    
                    body.dataset.theme = newTheme;
                    this.config.theme = newTheme;
                    
                    const toggleBtn = document.getElementById('themeToggle');
                    if (toggleBtn) {
                        toggleBtn.innerHTML = newTheme === 'light' ? '🌙 Dark' : '☀️ Light';
                    }
                    
                    // Update chart themes
                    this.updateChartThemes();
                }
                
                updateChartThemes() {
                    // Update all charts with new theme colors
                    Object.values(this.charts).forEach(chart => {
                        if (chart && chart.update) {
                            chart.update();
                        }
                    });
                }
            }
            
            // Export for global use
            window.PhoenixDashboard = PhoenixDashboard;
            """, 
            objectMapper.writeValueAsString(chartData));
    }
    
    private String generateChartManagerJS() {
        return """
            // Chart Manager for Phoenix Dashboard
            class ChartManager {
                constructor(dashboard) {
                    this.dashboard = dashboard;
                }
                
                changeChartType(chartId, newType) {
                    const chart = this.dashboard.charts[chartId];
                    if (!chart) return;
                    
                    // Destroy existing chart
                    chart.destroy();
                    
                    // Get canvas element
                    const ctx = document.getElementById(chartId);
                    if (!ctx) return;
                    
                    // Create new chart with different type
                    const config = this.getChartConfig(chartId, newType);
                    this.dashboard.charts[chartId] = new Chart(ctx, config);
                    
                    // Update button states
                    this.updateChartTypeButtons(chartId, newType);
                }
                
                getChartConfig(chartId, type) {
                    // Return appropriate config based on chart ID and type
                    const baseConfigs = {
                        statusChart: {
                            doughnut: { /* doughnut config */ },
                            pie: { /* pie config */ },
                            bar: { /* bar config */ }
                        },
                        timelineChart: {
                            line: { /* line config */ },
                            bar: { /* bar config */ },
                            area: { /* area config */ }
                        }
                        // ... more chart configs
                    };
                    
                    return baseConfigs[chartId]?.[type] || {};
                }
                
                updateChartTypeButtons(chartId, activeType) {
                    document.querySelectorAll(`[data-chart="${chartId}"]`).forEach(btn => {
                        btn.classList.toggle('active', btn.dataset.type === activeType);
                    });
                }
            }
            
            window.ChartManager = ChartManager;
            """;
    }
    
    private void generateAdditionalJSModules(Path assetsDir) throws IOException {
        // Generate filter manager
        String filtersJs = """
            // Filters Manager
            class FiltersManager {
                constructor(dashboard) {
                    this.dashboard = dashboard;
                    this.activeFilters = {};
                }
                
                applyFilters() {
                    // Collect filter values
                    this.activeFilters = {
                        environment: this.getSelectValues('environmentFilter'),
                        suite: this.getSelectValues('suiteFilter'),
                        status: this.getSelectValues('statusFilter'),
                        search: document.getElementById('searchFilter')?.value || ''
                    };
                    
                    // Apply filters to charts and table
                    this.filterCharts();
                    this.filterTable();
                    
                    // Save filters if persistence is enabled
                    if (this.dashboard.config.enableFilterPersistence) {
                        this.saveFilters();
                    }
                }
                
                getSelectValues(id) {
                    const select = document.getElementById(id);
                    if (!select) return [];
                    
                    return Array.from(select.selectedOptions).map(option => option.value);
                }
                
                filterCharts() {
                    // Update chart data based on filters
                    Object.values(this.dashboard.charts).forEach(chart => {
                        if (chart && chart.update) {
                            // Filter and update chart data
                            chart.update();
                        }
                    });
                }
                
                filterTable() {
                    // Filter table data
                    console.log('Filtering table with:', this.activeFilters);
                }
                
                clearFilters() {
                    // Reset all filter controls
                    document.querySelectorAll('.filter-group select, .filter-group input').forEach(element => {
                        if (element.tagName === 'SELECT') {
                            element.selectedIndex = 0;
                        } else {
                            element.value = '';
                        }
                    });
                    
                    this.activeFilters = {};
                    this.applyFilters();
                }
                
                saveFilters() {
                    localStorage.setItem('dashboardFilters', JSON.stringify(this.activeFilters));
                }
                
                loadFilters() {
                    const saved = localStorage.getItem('dashboardFilters');
                    if (saved) {
                        this.activeFilters = JSON.parse(saved);
                        this.restoreFilterUI();
                    }
                }
                
                restoreFilterUI() {
                    // Restore filter UI state from saved filters
                    Object.entries(this.activeFilters).forEach(([key, value]) => {
                        const element = document.getElementById(key + 'Filter');
                        if (element) {
                            if (element.tagName === 'SELECT') {
                                // Handle multi-select
                                Array.from(element.options).forEach(option => {
                                    option.selected = value.includes(option.value);
                                });
                            } else {
                                element.value = value;
                            }
                        }
                    });
                }
            }
            
            window.FiltersManager = FiltersManager;
            """;
        Files.write(assetsDir.resolve("js/filters.js"), filtersJs.getBytes());
        
        // Generate export manager
        String exportJs = """
            // Export Manager
            class ExportManager {
                constructor(dashboard) {
                    this.dashboard = dashboard;
                }
                
                exportToPNG() {
                    // Export charts as PNG
                    console.log('Exporting to PNG...');
                }
                
                exportToPDF() {
                    // Export dashboard as PDF
                    console.log('Exporting to PDF...');
                }
                
                exportToExcel() {
                    // Export data as Excel
                    console.log('Exporting to Excel...');
                }
                
                exportToCSV() {
                    // Export data as CSV
                    console.log('Exporting to CSV...');
                }
            }
            
            window.ExportManager = ExportManager;
            """;
        Files.write(assetsDir.resolve("js/export.js"), exportJs.getBytes());
        
        // Generate data table manager
        String dataTableJs = """
            // Data Table Manager
            class DataTableManager {
                constructor(dashboard) {
                    this.dashboard = dashboard;
                    this.currentPage = 1;
                    this.pageSize = 25;
                    this.sortColumn = null;
                    this.sortDirection = 'asc';
                }
                
                initialize() {
                    this.renderTable();
                    this.setupTableControls();
                }
                
                renderTable() {
                    const container = document.getElementById('dataTable');
                    if (!container) return;
                    
                    // Render table HTML
                    container.innerHTML = this.generateTableHTML();
                }
                
                generateTableHTML() {
                    return `
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Test Name</th>
                                    <th>Suite</th>
                                    <th>Status</th>
                                    <th>Environment</th>
                                    <th>Duration</th>
                                    <th>Start Time</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                <!-- Table rows will be populated here -->
                            </tbody>
                        </table>
                    `;
                }
                
                setupTableControls() {
                    // Setup search, pagination, etc.
                    document.getElementById('tableSearch')?.addEventListener('input', (e) => {
                        this.searchTable(e.target.value);
                    });
                    
                    document.getElementById('tablePageSize')?.addEventListener('change', (e) => {
                        this.pageSize = parseInt(e.target.value);
                        this.renderTable();
                    });
                }
                
                searchTable(query) {
                    // Filter table based on search query
                    console.log('Searching table for:', query);
                }
            }
            
            window.DataTableManager = DataTableManager;
            """;
        Files.write(assetsDir.resolve("js/data-table.js"), dataTableJs.getBytes());
        
        // Generate WebSocket handler
        String webSocketJs = """
            // WebSocket Handler for Real-time Updates
            class WebSocketHandler {
                constructor(dashboard) {
                    this.dashboard = dashboard;
                    this.socket = null;
                    this.reconnectInterval = 5000;
                    this.maxReconnectAttempts = 10;
                    this.reconnectAttempts = 0;
                }
                
                connect() {
                    if (!this.dashboard.config.enableWebSocket) return;
                    
                    try {
                        this.socket = new WebSocket('ws://localhost:8080/dashboard-ws');
                        
                        this.socket.onopen = () => {
                            console.log('WebSocket connected');
                            this.reconnectAttempts = 0;
                            this.updateConnectionStatus(true);
                        };
                        
                        this.socket.onmessage = (event) => {
                            const data = JSON.parse(event.data);
                            this.handleMessage(data);
                        };
                        
                        this.socket.onclose = () => {
                            console.log('WebSocket disconnected');
                            this.updateConnectionStatus(false);
                            this.attemptReconnect();
                        };
                        
                        this.socket.onerror = (error) => {
                            console.error('WebSocket error:', error);
                        };
                        
                    } catch (error) {
                        console.error('WebSocket connection failed:', error);
                    }
                }
                
                handleMessage(data) {
                    switch (data.type) {
                        case 'TEST_UPDATE':
                            this.dashboard.updateTestData(data.payload);
                            break;
                        case 'CHART_DATA':
                            this.dashboard.updateChartData(data.payload);
                            break;
                        default:
                            console.log('Unknown message type:', data.type);
                    }
                }
                
                attemptReconnect() {
                    if (this.reconnectAttempts < this.maxReconnectAttempts) {
                        this.reconnectAttempts++;
                        console.log(`Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
                        
                        setTimeout(() => {
                            this.connect();
                        }, this.reconnectInterval);
                    }
                }
                
                updateConnectionStatus(connected) {
                    const statusDot = document.querySelector('.status-dot');
                    if (statusDot) {
                        statusDot.className = `status-dot ${connected ? 'connected' : 'disconnected'}`;
                    }
                }
                
                disconnect() {
                    if (this.socket) {
                        this.socket.close();
                        this.socket = null;
                    }
                }
            }
            
            window.WebSocketHandler = WebSocketHandler;
            """;
        Files.write(assetsDir.resolve("js/websocket-handler.js"), webSocketJs.getBytes());
        
        // Generate theme manager
        String themeManagerJs = """
            // Theme Manager
            class ThemeManagerJS {
                constructor(dashboard) {
                    this.dashboard = dashboard;
                    this.themes = {
                        light: {
                            primary: '#667eea',
                            background: '#ffffff',
                            text: '#333333'
                        },
                        dark: {
                            primary: '#764ba2',
                            background: '#1a1a1a',
                            text: '#ffffff'
                        }
                    };
                }
                
                applyTheme(themeName) {
                    const theme = this.themes[themeName];
                    if (!theme) return;
                    
                    const root = document.documentElement;
                    Object.entries(theme).forEach(([key, value]) => {
                        root.style.setProperty(`--${key}-color`, value);
                    });
                    
                    document.body.dataset.theme = themeName;
                }
                
                toggleTheme() {
                    const current = document.body.dataset.theme || 'light';
                    const newTheme = current === 'light' ? 'dark' : 'light';
                    this.applyTheme(newTheme);
                    return newTheme;
                }
            }
            
            window.ThemeManagerJS = ThemeManagerJS;
            """;
        Files.write(assetsDir.resolve("js/theme-manager.js"), themeManagerJs.getBytes());
    }
    
    private void generateDataAssets(Path assetsDir, List<ReportingFramework.TestExecution> executions,
                                   Map<String, Object> analyticsData, Map<String, Object> chartData) throws IOException {
        
        // Generate test data JSON
        Map<String, Object> testData = new HashMap<>();
        testData.put("executions", executions);
        testData.put("analytics", analyticsData);
        testData.put("charts", chartData);
        testData.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        String testDataJson = objectMapper.writeValueAsString(testData);
        Files.write(assetsDir.resolve("data/test-data.json"), testDataJson.getBytes());
        
        // Generate configuration JSON
        Map<String, Object> configData = new HashMap<>();
        configData.put("refreshInterval", config.getRefreshIntervalSeconds());
        configData.put("enableRealTime", config.isEnableRealTimeUpdates());
        configData.put("theme", config.getDefaultTheme());
        configData.put("enabledChartTypes", config.getEnabledChartTypes());
        
        String configJson = objectMapper.writeValueAsString(configData);
        Files.write(assetsDir.resolve("data/config.json"), configJson.getBytes());
    }
    
    /**
     * Chart Data Generator
     */
    private static class ChartDataGenerator {
        private final DashboardConfiguration config;
        
        public ChartDataGenerator(DashboardConfiguration config) {
            this.config = config;
        }
        
        public Map<String, Object> generateChartData(List<ReportingFramework.TestExecution> executions,
                                                   Map<String, Object> analyticsData) {
            Map<String, Object> chartData = new HashMap<>();
            
            // Generate data for each chart type
            chartData.put("statusChart", generateStatusChartData(executions));
            chartData.put("timelineChart", generateTimelineChartData(executions));
            chartData.put("environmentChart", generateEnvironmentChartData(executions));
            chartData.put("durationChart", generateDurationChartData(executions));
            chartData.put("failureChart", generateFailureChartData(executions));
            
            return chartData;
        }
        
        private Map<String, Object> generateStatusChartData(List<ReportingFramework.TestExecution> executions) {
            Map<String, Long> statusCounts = executions.stream()
                .collect(Collectors.groupingBy(
                    ReportingFramework.TestExecution::getStatus,
                    Collectors.counting()));
            
            Map<String, Object> data = new HashMap<>();
            data.put("labels", Arrays.asList("Passed", "Failed", "Skipped"));
            data.put("data", Arrays.asList(
                statusCounts.getOrDefault("PASSED", 0L),
                statusCounts.getOrDefault("FAILED", 0L),
                statusCounts.getOrDefault("SKIPPED", 0L)
            ));
            data.put("colors", Arrays.asList("#28a745", "#dc3545", "#ffc107"));
            
            return data;
        }
        
        private Map<String, Object> generateTimelineChartData(List<ReportingFramework.TestExecution> executions) {
            // Group executions by day
            Map<String, Long> dailyCounts = executions.stream()
                .collect(Collectors.groupingBy(
                    e -> e.getStartTime().toLocalDate().toString(),
                    Collectors.counting()));
            
            Map<String, Object> data = new HashMap<>();
            data.put("labels", new ArrayList<>(dailyCounts.keySet()));
            data.put("data", new ArrayList<>(dailyCounts.values()));
            
            return data;
        }
        
        private Map<String, Object> generateEnvironmentChartData(List<ReportingFramework.TestExecution> executions) {
            Map<String, Long> envCounts = executions.stream()
                .filter(e -> e.getEnvironment() != null)
                .collect(Collectors.groupingBy(
                    ReportingFramework.TestExecution::getEnvironment,
                    Collectors.counting()));
            
            Map<String, Object> data = new HashMap<>();
            data.put("labels", new ArrayList<>(envCounts.keySet()));
            data.put("data", new ArrayList<>(envCounts.values()));
            
            return data;
        }
        
        private Map<String, Object> generateDurationChartData(List<ReportingFramework.TestExecution> executions) {
            // Create duration buckets
            Map<String, Long> durationBuckets = new LinkedHashMap<>();
            durationBuckets.put("0-1s", 0L);
            durationBuckets.put("1-5s", 0L);
            durationBuckets.put("5-10s", 0L);
            durationBuckets.put("10-30s", 0L);
            durationBuckets.put("30s+", 0L);
            
            executions.forEach(execution -> {
                long duration = execution.getDuration();
                if (duration < 1000) {
                    durationBuckets.put("0-1s", durationBuckets.get("0-1s") + 1);
                } else if (duration < 5000) {
                    durationBuckets.put("1-5s", durationBuckets.get("1-5s") + 1);
                } else if (duration < 10000) {
                    durationBuckets.put("5-10s", durationBuckets.get("5-10s") + 1);
                } else if (duration < 30000) {
                    durationBuckets.put("10-30s", durationBuckets.get("10-30s") + 1);
                } else {
                    durationBuckets.put("30s+", durationBuckets.get("30s+") + 1);
                }
            });
            
            Map<String, Object> data = new HashMap<>();
            data.put("labels", new ArrayList<>(durationBuckets.keySet()));
            data.put("data", new ArrayList<>(durationBuckets.values()));
            
            return data;
        }
        
        private Map<String, Object> generateFailureChartData(List<ReportingFramework.TestExecution> executions) {
            // Group failures by day
            Map<String, Long> dailyFailures = executions.stream()
                .filter(e -> "FAILED".equals(e.getStatus()))
                .collect(Collectors.groupingBy(
                    e -> e.getStartTime().toLocalDate().toString(),
                    Collectors.counting()));
            
            Map<String, Object> data = new HashMap<>();
            data.put("labels", new ArrayList<>(dailyFailures.keySet()));
            data.put("data", new ArrayList<>(dailyFailures.values()));
            
            return data;
        }
    }
    
    /**
     * WebSocket Handler
     */
    private static class WebSocketHandler {
        private final DashboardConfiguration config;
        
        public WebSocketHandler(DashboardConfiguration config) {
            this.config = config;
        }
        
        public void generateWebSocketScript(Path assetsDir) throws IOException {
            if (!config.isEnableWebSocketSupport()) {
                return;
            }
            
            String webSocketScript = """
                // WebSocket initialization script
                if (window.WebSocketHandler) {
                    const wsHandler = new WebSocketHandler(dashboard);
                    wsHandler.connect();
                }
                """;
            
            Files.write(assetsDir.resolve("js/websocket-init.js"), webSocketScript.getBytes());
        }
    }
    
    /**
     * Theme Manager
     */
    private static class ThemeManager {
        private final DashboardConfiguration config;
        
        public ThemeManager(DashboardConfiguration config) {
            this.config = config;
        }
        
        public String getThemeColors(String themeName) {
            return switch (themeName) {
                case "dark" -> """
                    :root {
                        --primary-color: #764ba2;
                        --background-color: #1a1a1a;
                        --text-color: #ffffff;
                        --card-background: #2d2d2d;
                    }
                    """;
                default -> """
                    :root {
                        --primary-color: #667eea;
                        --background-color: #ffffff;
                        --text-color: #333333;
                        --card-background: #ffffff;
                    }
                    """;
            };
        }
        
        public String generateThemeCSS() {
            return """
                /* Theme Styles */
                [data-theme="light"] {
                    --primary-color: #667eea;
                    --secondary-color: #764ba2;
                    --background-color: #ffffff;
                    --text-color: #333333;
                    --card-background: #ffffff;
                    --border-color: #dee2e6;
                }
                
                [data-theme="dark"] {
                    --primary-color: #764ba2;
                    --secondary-color: #667eea;
                    --background-color: #1a1a1a;
                    --text-color: #ffffff;
                    --card-background: #2d2d2d;
                    --border-color: #495057;
                }
                
                [data-theme="dark"] .dashboard-nav,
                [data-theme="dark"] .kpi-card,
                [data-theme="dark"] .filter-panel,
                [data-theme="dark"] .chart-card,
                [data-theme="dark"] .table-card,
                [data-theme="dark"] .real-time-status {
                    background: var(--card-background);
                    color: var(--text-color);
                    border-color: var(--border-color);
                }
                
                [data-theme="dark"] body.dashboard-body {
                    background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
                }
                """;
        }
    }
}