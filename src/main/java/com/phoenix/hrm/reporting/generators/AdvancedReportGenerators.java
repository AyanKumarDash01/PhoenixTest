package com.phoenix.hrm.reporting.generators;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.phoenix.hrm.reporting.ReportingFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Advanced Report Generators for Phoenix HRM Test Automation
 * 
 * Provides specialized report generators for:
 * - PDF reports with charts and visualizations
 * - Excel reports with data analysis and pivot tables
 * - CSV reports for data export and integration
 * - Allure format for integration with Allure reporting
 * - Enhanced HTML reports with interactive charts
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 6.0
 * @since Phase 6
 */
public class AdvancedReportGenerators {
    
    private static final Logger logger = LoggerFactory.getLogger(AdvancedReportGenerators.class);
    
    /**
     * PDF Report Generator with Charts and Data Visualization
     */
    public static class PdfReportGenerator implements ReportingFramework.ReportGenerator {
        
        private final ObjectMapper objectMapper;
        
        public PdfReportGenerator() {
            this.objectMapper = new ObjectMapper();
            this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        }
        
        @Override
        public String generateReport(String reportName, List<ReportingFramework.TestExecution> executions,
                                   Map<String, Object> analyticsData, ReportingFramework.ReportConfiguration config) {
            
            String reportPath = Paths.get(config.getOutputPath(), "pdf", reportName + ".pdf").toString();
            logger.info("Generating PDF report: {}", reportPath);
            
            try {
                // Create PDF report structure
                PdfReportData reportData = buildPdfReportData(executions, analyticsData, config);
                
                // Generate charts and visualizations
                generateCharts(reportData);
                
                // Create PDF document
                createPdfDocument(reportData, reportPath);
                
                logger.info("PDF report generated successfully: {}", reportPath);
                return reportPath;
                
            } catch (Exception e) {
                logger.error("Error generating PDF report: {}", e.getMessage(), e);
                throw new RuntimeException("PDF report generation failed", e);
            }
        }
        
        private PdfReportData buildPdfReportData(List<ReportingFramework.TestExecution> executions,
                                               Map<String, Object> analyticsData,
                                               ReportingFramework.ReportConfiguration config) {
            
            PdfReportData data = new PdfReportData();
            data.setTitle(config.getProjectName() + " - Test Execution Report");
            data.setGeneratedAt(LocalDateTime.now());
            data.setCompanyName(config.getCompanyName());
            data.setTotalTests(executions.size());
            
            // Calculate statistics
            Map<String, Integer> statusCounts = new HashMap<>();
            Map<String, Long> durationStats = new HashMap<>();
            Map<String, Integer> environmentCounts = new HashMap<>();
            
            long totalDuration = 0;
            for (ReportingFramework.TestExecution execution : executions) {
                String status = execution.getStatus();
                statusCounts.put(status, statusCounts.getOrDefault(status, 0) + 1);
                
                String environment = execution.getEnvironment();
                if (environment != null) {
                    environmentCounts.put(environment, environmentCounts.getOrDefault(environment, 0) + 1);
                }
                
                totalDuration += execution.getDuration();
            }
            
            data.setStatusDistribution(statusCounts);
            data.setEnvironmentDistribution(environmentCounts);
            data.setAverageDuration(executions.isEmpty() ? 0 : totalDuration / executions.size());
            data.setExecutions(executions);
            
            return data;
        }
        
        private void generateCharts(PdfReportData reportData) {
            // Generate pie chart for status distribution
            generateStatusPieChart(reportData);
            
            // Generate bar chart for environment distribution
            generateEnvironmentBarChart(reportData);
            
            // Generate timeline chart for execution duration
            generateDurationTimelineChart(reportData);
        }
        
        private void generateStatusPieChart(PdfReportData reportData) {
            // Implement pie chart generation using Java Graphics2D
            logger.debug("Generating status distribution pie chart");
            // Chart generation logic would go here
            reportData.setStatusChartPath("charts/status_distribution.png");
        }
        
        private void generateEnvironmentBarChart(PdfReportData reportData) {
            // Implement bar chart generation
            logger.debug("Generating environment distribution bar chart");
            reportData.setEnvironmentChartPath("charts/environment_distribution.png");
        }
        
        private void generateDurationTimelineChart(PdfReportData reportData) {
            // Implement timeline chart generation
            logger.debug("Generating duration timeline chart");
            reportData.setDurationChartPath("charts/duration_timeline.png");
        }
        
        private void createPdfDocument(PdfReportData reportData, String outputPath) {
            // Create PDF document using iText or similar library
            logger.debug("Creating PDF document at: {}", outputPath);
            
            try {
                Files.createDirectories(Paths.get(outputPath).getParent());
                
                // Mock PDF content creation - in real implementation, use iText
                StringBuilder pdfContent = new StringBuilder();
                pdfContent.append("PDF Report Content\n");
                pdfContent.append("Title: ").append(reportData.getTitle()).append("\n");
                pdfContent.append("Generated: ").append(reportData.getGeneratedAt()).append("\n");
                pdfContent.append("Total Tests: ").append(reportData.getTotalTests()).append("\n");
                pdfContent.append("Status Distribution: ").append(reportData.getStatusDistribution()).append("\n");
                
                Files.write(Paths.get(outputPath), pdfContent.toString().getBytes());
                
            } catch (IOException e) {
                throw new RuntimeException("Failed to create PDF document", e);
            }
        }
        
        // PDF Report Data Model
        private static class PdfReportData {
            private String title;
            private LocalDateTime generatedAt;
            private String companyName;
            private int totalTests;
            private Map<String, Integer> statusDistribution;
            private Map<String, Integer> environmentDistribution;
            private long averageDuration;
            private List<ReportingFramework.TestExecution> executions;
            private String statusChartPath;
            private String environmentChartPath;
            private String durationChartPath;
            
            // Getters and setters
            public String getTitle() { return title; }
            public void setTitle(String title) { this.title = title; }
            public LocalDateTime getGeneratedAt() { return generatedAt; }
            public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
            public String getCompanyName() { return companyName; }
            public void setCompanyName(String companyName) { this.companyName = companyName; }
            public int getTotalTests() { return totalTests; }
            public void setTotalTests(int totalTests) { this.totalTests = totalTests; }
            public Map<String, Integer> getStatusDistribution() { return statusDistribution; }
            public void setStatusDistribution(Map<String, Integer> statusDistribution) { this.statusDistribution = statusDistribution; }
            public Map<String, Integer> getEnvironmentDistribution() { return environmentDistribution; }
            public void setEnvironmentDistribution(Map<String, Integer> environmentDistribution) { this.environmentDistribution = environmentDistribution; }
            public long getAverageDuration() { return averageDuration; }
            public void setAverageDuration(long averageDuration) { this.averageDuration = averageDuration; }
            public List<ReportingFramework.TestExecution> getExecutions() { return executions; }
            public void setExecutions(List<ReportingFramework.TestExecution> executions) { this.executions = executions; }
            public String getStatusChartPath() { return statusChartPath; }
            public void setStatusChartPath(String statusChartPath) { this.statusChartPath = statusChartPath; }
            public String getEnvironmentChartPath() { return environmentChartPath; }
            public void setEnvironmentChartPath(String environmentChartPath) { this.environmentChartPath = environmentChartPath; }
            public String getDurationChartPath() { return durationChartPath; }
            public void setDurationChartPath(String durationChartPath) { this.durationChartPath = durationChartPath; }
        }
    }
    
    /**
     * Excel Report Generator with Data Analysis and Pivot Tables
     */
    public static class ExcelReportGenerator implements ReportingFramework.ReportGenerator {
        
        @Override
        public String generateReport(String reportName, List<ReportingFramework.TestExecution> executions,
                                   Map<String, Object> analyticsData, ReportingFramework.ReportConfiguration config) {
            
            String reportPath = Paths.get(config.getOutputPath(), "excel", reportName + ".xlsx").toString();
            logger.info("Generating Excel report: {}", reportPath);
            
            try {
                // Create Excel workbook with multiple sheets
                createExcelWorkbook(executions, analyticsData, config, reportPath);
                
                logger.info("Excel report generated successfully: {}", reportPath);
                return reportPath;
                
            } catch (Exception e) {
                logger.error("Error generating Excel report: {}", e.getMessage(), e);
                throw new RuntimeException("Excel report generation failed", e);
            }
        }
        
        private void createExcelWorkbook(List<ReportingFramework.TestExecution> executions,
                                       Map<String, Object> analyticsData,
                                       ReportingFramework.ReportConfiguration config,
                                       String outputPath) throws IOException {
            
            Files.createDirectories(Paths.get(outputPath).getParent());
            
            // Mock Excel content creation - in real implementation, use Apache POI
            StringBuilder excelContent = new StringBuilder();
            excelContent.append("Excel Report Content\n");
            excelContent.append("Project: ").append(config.getProjectName()).append("\n");
            excelContent.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
            excelContent.append("\nTest Execution Summary:\n");
            
            // Summary sheet
            createSummarySheet(excelContent, executions, analyticsData);
            
            // Detailed results sheet
            createDetailedResultsSheet(excelContent, executions);
            
            // Analytics sheet
            createAnalyticsSheet(excelContent, analyticsData);
            
            // Charts sheet
            createChartsSheet(excelContent, executions);
            
            Files.write(Paths.get(outputPath), excelContent.toString().getBytes());
        }
        
        private void createSummarySheet(StringBuilder content, List<ReportingFramework.TestExecution> executions,
                                      Map<String, Object> analyticsData) {
            content.append("\n=== SUMMARY SHEET ===\n");
            content.append("Total Tests: ").append(executions.size()).append("\n");
            
            Map<String, Long> statusCounts = executions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    ReportingFramework.TestExecution::getStatus,
                    java.util.stream.Collectors.counting()));
            
            statusCounts.forEach((status, count) -> 
                content.append(status).append(": ").append(count).append("\n"));
        }
        
        private void createDetailedResultsSheet(StringBuilder content, List<ReportingFramework.TestExecution> executions) {
            content.append("\n=== DETAILED RESULTS SHEET ===\n");
            content.append("Execution ID\tTest Name\tSuite\tStatus\tEnvironment\tDuration\tStart Time\n");
            
            for (ReportingFramework.TestExecution execution : executions) {
                content.append(execution.getExecutionId()).append("\t")
                       .append(execution.getTestName()).append("\t")
                       .append(execution.getSuiteName()).append("\t")
                       .append(execution.getStatus()).append("\t")
                       .append(execution.getEnvironment()).append("\t")
                       .append(execution.getDuration()).append("\t")
                       .append(execution.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
            }
        }
        
        private void createAnalyticsSheet(StringBuilder content, Map<String, Object> analyticsData) {
            content.append("\n=== ANALYTICS SHEET ===\n");
            analyticsData.forEach((key, value) -> 
                content.append(key).append(": ").append(value).append("\n"));
        }
        
        private void createChartsSheet(StringBuilder content, List<ReportingFramework.TestExecution> executions) {
            content.append("\n=== CHARTS SHEET ===\n");
            content.append("Charts and visualizations would be embedded here\n");
            
            // Calculate data for charts
            Map<String, Long> statusDistribution = executions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    ReportingFramework.TestExecution::getStatus,
                    java.util.stream.Collectors.counting()));
            
            content.append("Status Distribution Chart Data:\n");
            statusDistribution.forEach((status, count) -> 
                content.append("  ").append(status).append(": ").append(count).append("\n"));
        }
    }
    
    /**
     * CSV Report Generator for Data Export and Integration
     */
    public static class CsvReportGenerator implements ReportingFramework.ReportGenerator {
        
        @Override
        public String generateReport(String reportName, List<ReportingFramework.TestExecution> executions,
                                   Map<String, Object> analyticsData, ReportingFramework.ReportConfiguration config) {
            
            String reportPath = Paths.get(config.getOutputPath(), "csv", reportName + ".csv").toString();
            logger.info("Generating CSV report: {}", reportPath);
            
            try {
                createCsvReport(executions, analyticsData, config, reportPath);
                
                logger.info("CSV report generated successfully: {}", reportPath);
                return reportPath;
                
            } catch (Exception e) {
                logger.error("Error generating CSV report: {}", e.getMessage(), e);
                throw new RuntimeException("CSV report generation failed", e);
            }
        }
        
        private void createCsvReport(List<ReportingFramework.TestExecution> executions,
                                   Map<String, Object> analyticsData,
                                   ReportingFramework.ReportConfiguration config,
                                   String outputPath) throws IOException {
            
            Files.createDirectories(Paths.get(outputPath).getParent());
            
            StringBuilder csvContent = new StringBuilder();
            
            // CSV Header
            csvContent.append("Execution ID,Test Name,Suite Name,Status,Environment,Duration (ms),")
                     .append("Start Time,End Time,Error Message,Screenshots Count,Logs Count\n");
            
            // CSV Data Rows
            for (ReportingFramework.TestExecution execution : executions) {
                csvContent.append(escapeCSV(execution.getExecutionId())).append(",")
                         .append(escapeCSV(execution.getTestName())).append(",")
                         .append(escapeCSV(execution.getSuiteName())).append(",")
                         .append(escapeCSV(execution.getStatus())).append(",")
                         .append(escapeCSV(execution.getEnvironment())).append(",")
                         .append(execution.getDuration()).append(",")
                         .append(escapeCSV(execution.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))).append(",")
                         .append(escapeCSV(execution.getEndTime() != null ? 
                             execution.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "")).append(",")
                         .append(escapeCSV(execution.getErrorMessage())).append(",")
                         .append(execution.getScreenshots().size()).append(",")
                         .append(execution.getLogs().size()).append("\n");
            }
            
            Files.write(Paths.get(outputPath), csvContent.toString().getBytes());
        }
        
        private String escapeCSV(String value) {
            if (value == null) return "";
            
            // Escape quotes and wrap in quotes if contains comma, quote, or newline
            if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                return "\"" + value.replace("\"", "\"\"") + "\"";
            }
            return value;
        }
    }
    
    /**
     * Allure Report Generator for Integration with Allure Reporting
     */
    public static class AllureReportGenerator implements ReportingFramework.ReportGenerator {
        
        private final ObjectMapper objectMapper;
        
        public AllureReportGenerator() {
            this.objectMapper = new ObjectMapper();
            this.objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        }
        
        @Override
        public String generateReport(String reportName, List<ReportingFramework.TestExecution> executions,
                                   Map<String, Object> analyticsData, ReportingFramework.ReportConfiguration config) {
            
            String reportPath = Paths.get(config.getOutputPath(), "allure", "allure-results").toString();
            logger.info("Generating Allure report data: {}", reportPath);
            
            try {
                createAllureResults(executions, analyticsData, config, reportPath);
                
                logger.info("Allure report data generated successfully: {}", reportPath);
                return reportPath;
                
            } catch (Exception e) {
                logger.error("Error generating Allure report: {}", e.getMessage(), e);
                throw new RuntimeException("Allure report generation failed", e);
            }
        }
        
        private void createAllureResults(List<ReportingFramework.TestExecution> executions,
                                       Map<String, Object> analyticsData,
                                       ReportingFramework.ReportConfiguration config,
                                       String outputPath) throws IOException {
            
            Files.createDirectories(Paths.get(outputPath));
            
            // Generate Allure test results for each execution
            for (ReportingFramework.TestExecution execution : executions) {
                createAllureTestResult(execution, outputPath);
            }
            
            // Generate environment properties
            createAllureEnvironment(config, outputPath);
            
            // Generate categories for test classification
            createAllureCategories(outputPath);
        }
        
        private void createAllureTestResult(ReportingFramework.TestExecution execution, String outputPath) throws IOException {
            AllureTestResult result = new AllureTestResult();
            result.uuid = UUID.randomUUID().toString();
            result.name = execution.getTestName();
            result.fullName = execution.getSuiteName() + "." + execution.getTestName();
            result.status = mapToAllureStatus(execution.getStatus());
            result.start = execution.getStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            
            if (execution.getEndTime() != null) {
                result.stop = execution.getEndTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            
            // Add labels
            result.labels.add(new AllureLabel("suite", execution.getSuiteName()));
            result.labels.add(new AllureLabel("testClass", execution.getSuiteName()));
            result.labels.add(new AllureLabel("testMethod", execution.getTestName()));
            result.labels.add(new AllureLabel("environment", execution.getEnvironment()));
            
            // Add error details if failed
            if ("FAILED".equals(execution.getStatus()) && execution.getErrorMessage() != null) {
                AllureStatusDetails statusDetails = new AllureStatusDetails();
                statusDetails.message = execution.getErrorMessage();
                statusDetails.trace = execution.getStackTrace();
                result.statusDetails = statusDetails;
            }
            
            // Add attachments for screenshots
            for (String screenshot : execution.getScreenshots()) {
                AllureAttachment attachment = new AllureAttachment();
                attachment.name = "Screenshot";
                attachment.source = screenshot;
                attachment.type = "image/png";
                result.attachments.add(attachment);
            }
            
            // Write test result file
            String fileName = result.uuid + "-result.json";
            String filePath = Paths.get(outputPath, fileName).toString();
            objectMapper.writeValue(new File(filePath), result);
        }
        
        private void createAllureEnvironment(ReportingFramework.ReportConfiguration config, String outputPath) throws IOException {
            Properties envProps = new Properties();
            envProps.setProperty("Project", config.getProjectName());
            envProps.setProperty("Company", config.getCompanyName());
            envProps.setProperty("Generated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            config.getCustomProperties().forEach(envProps::setProperty);
            
            String envFilePath = Paths.get(outputPath, "environment.properties").toString();
            try (FileOutputStream fos = new FileOutputStream(envFilePath)) {
                envProps.store(fos, "Allure Environment Properties");
            }
        }
        
        private void createAllureCategories(String outputPath) throws IOException {
            List<AllureCategory> categories = new ArrayList<>();
            
            // Product defects
            AllureCategory productDefects = new AllureCategory();
            productDefects.name = "Product defects";
            productDefects.matchedStatuses.add("failed");
            categories.add(productDefects);
            
            // Test defects
            AllureCategory testDefects = new AllureCategory();
            testDefects.name = "Test defects";
            testDefects.matchedStatuses.add("broken");
            categories.add(testDefects);
            
            String categoriesFilePath = Paths.get(outputPath, "categories.json").toString();
            objectMapper.writeValue(new File(categoriesFilePath), categories);
        }
        
        private String mapToAllureStatus(String status) {
            return switch (status.toUpperCase()) {
                case "PASSED" -> "passed";
                case "FAILED" -> "failed";
                case "SKIPPED" -> "skipped";
                case "BROKEN" -> "broken";
                default -> "unknown";
            };
        }
        
        // Allure Data Models
        private static class AllureTestResult {
            public String uuid;
            public String name;
            public String fullName;
            public String status;
            public long start;
            public long stop;
            public List<AllureLabel> labels = new ArrayList<>();
            public List<AllureAttachment> attachments = new ArrayList<>();
            public AllureStatusDetails statusDetails;
        }
        
        private static class AllureLabel {
            public String name;
            public String value;
            
            public AllureLabel(String name, String value) {
                this.name = name;
                this.value = value;
            }
        }
        
        private static class AllureAttachment {
            public String name;
            public String source;
            public String type;
        }
        
        private static class AllureStatusDetails {
            public String message;
            public String trace;
        }
        
        private static class AllureCategory {
            public String name;
            public List<String> matchedStatuses = new ArrayList<>();
        }
    }
    
    /**
     * Enhanced HTML Report Generator with Interactive Charts
     */
    public static class EnhancedHtmlReportGenerator implements ReportingFramework.ReportGenerator {
        
        @Override
        public String generateReport(String reportName, List<ReportingFramework.TestExecution> executions,
                                   Map<String, Object> analyticsData, ReportingFramework.ReportConfiguration config) {
            
            String reportPath = Paths.get(config.getOutputPath(), "html", reportName + ".html").toString();
            logger.info("Generating Enhanced HTML report: {}", reportPath);
            
            try {
                createEnhancedHtmlReport(executions, analyticsData, config, reportPath);
                
                logger.info("Enhanced HTML report generated successfully: {}", reportPath);
                return reportPath;
                
            } catch (Exception e) {
                logger.error("Error generating Enhanced HTML report: {}", e.getMessage(), e);
                throw new RuntimeException("Enhanced HTML report generation failed", e);
            }
        }
        
        private void createEnhancedHtmlReport(List<ReportingFramework.TestExecution> executions,
                                            Map<String, Object> analyticsData,
                                            ReportingFramework.ReportConfiguration config,
                                            String outputPath) throws IOException {
            
            Files.createDirectories(Paths.get(outputPath).getParent());
            
            String htmlContent = generateEnhancedHtmlContent(executions, analyticsData, config);
            Files.write(Paths.get(outputPath), htmlContent.getBytes());
        }
        
        private String generateEnhancedHtmlContent(List<ReportingFramework.TestExecution> executions,
                                                 Map<String, Object> analyticsData,
                                                 ReportingFramework.ReportConfiguration config) {
            
            StringBuilder html = new StringBuilder();
            
            // HTML Structure with Chart.js integration
            html.append(getHtmlHeader(config));
            html.append(getHtmlBody(executions, analyticsData, config));
            html.append(getHtmlFooter());
            
            return html.toString();
        }
        
        private String getHtmlHeader(ReportingFramework.ReportConfiguration config) {
            StringBuilder header = new StringBuilder();
            header.append("<!DOCTYPE html>\n")
                .append("<html lang=\"en\">\n")
                .append("<head>\n")
                .append("    <meta charset=\"UTF-8\">\n")
                .append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
                .append("    <title>").append(config.getProjectName()).append(" - Test Report</title>\n")
                .append("    <script src=\"https://cdn.jsdelivr.net/npm/chart.js\"></script>\n")
                .append("    <style>\n")
                .append("        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }\n")
                .append("        .container { max-width: 1200px; margin: 0 auto; background: white; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n")
                .append("        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 8px 8px 0 0; text-align: center; }\n")
                .append("        .header h1 { margin: 0; font-size: 2.5em; }\n")
                .append("        .header p { margin: 10px 0 0 0; opacity: 0.9; }\n")
                .append("        .summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; padding: 30px; }\n")
                .append("        .summary-card { background: #f8f9fa; padding: 20px; border-radius: 8px; text-align: center; border-left: 4px solid #007bff; }\n")
                .append("        .summary-card h3 { margin: 0 0 10px 0; color: #333; }\n")
                .append("        .summary-card .value { font-size: 2em; font-weight: bold; color: #007bff; }\n")
                .append("        .charts-section { padding: 30px; }\n")
                .append("        .charts-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 30px; margin-bottom: 30px; }\n")
                .append("        .chart-container { background: #f8f9fa; padding: 20px; border-radius: 8px; }\n")
                .append("        .chart-title { text-align: center; margin-bottom: 20px; font-weight: bold; color: #333; }\n")
                .append("        .results-table { width: 100%; border-collapse: collapse; margin-top: 20px; }\n")
                .append("        .results-table th, .results-table td { border: 1px solid #ddd; padding: 12px; text-align: left; }\n")
                .append("        .results-table th { background-color: #f2f2f2; font-weight: bold; position: sticky; top: 0; }\n")
                .append("        .status-passed { color: #28a745; font-weight: bold; }\n")
                .append("        .status-failed { color: #dc3545; font-weight: bold; }\n")
                .append("        .status-skipped { color: #ffc107; font-weight: bold; }\n")
                .append("        .filter-controls { padding: 20px; background: #f8f9fa; margin-bottom: 20px; border-radius: 8px; }\n")
                .append("        .filter-controls select, .filter-controls input { margin: 0 10px; padding: 8px; border: 1px solid #ddd; border-radius: 4px; }\n")
                .append("        .footer { text-align: center; padding: 20px; color: #666; border-top: 1px solid #eee; }\n")
                .append("    </style>\n")
                .append("</head>\n");
            return header.toString();
        }
        
        private String getHtmlBody(List<ReportingFramework.TestExecution> executions,
                                 Map<String, Object> analyticsData,
                                 ReportingFramework.ReportConfiguration config) {
            
            StringBuilder body = new StringBuilder();
            
            // Calculate statistics
            Map<String, Long> statusCounts = executions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    ReportingFramework.TestExecution::getStatus,
                    java.util.stream.Collectors.counting()));
            
            long totalTests = executions.size();
            long passedTests = statusCounts.getOrDefault("PASSED", 0L);
            long failedTests = statusCounts.getOrDefault("FAILED", 0L);
            long skippedTests = statusCounts.getOrDefault("SKIPPED", 0L);
            double successRate = totalTests > 0 ? (passedTests * 100.0 / totalTests) : 0;
            
            body.append("<body>\n")
                .append("    <div class=\"container\">\n")
                .append("        <div class=\"header\">\n")
                .append("            <h1>").append(config.getProjectName()).append(" Test Report</h1>\n")
                .append("            <p>Generated on: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</p>\n")
                .append("            <p>Company: ").append(config.getCompanyName()).append("</p>\n")
                .append("        </div>\n")
                .append("        <div class=\"summary\">\n")
                .append("            <div class=\"summary-card\">\n")
                .append("                <h3>Total Tests</h3>\n")
                .append("                <div class=\"value\">").append(totalTests).append("</div>\n")
                .append("            </div>\n")
                .append("            <div class=\"summary-card\">\n")
                .append("                <h3>Passed</h3>\n")
                .append("                <div class=\"value status-passed\">").append(passedTests).append("</div>\n")
                .append("            </div>\n")
                .append("            <div class=\"summary-card\">\n")
                .append("                <h3>Failed</h3>\n")
                .append("                <div class=\"value status-failed\">").append(failedTests).append("</div>\n")
                .append("            </div>\n")
                .append("            <div class=\"summary-card\">\n")
                .append("                <h3>Success Rate</h3>\n")
                .append("                <div class=\"value\">").append(String.format("%.1f%%", successRate)).append("</div>\n")
                .append("            </div>\n")
                .append("        </div>\n")
                .append("        <div class=\"charts-section\">\n")
                .append("            <div class=\"charts-grid\">\n")
                .append("                <div class=\"chart-container\">\n")
                .append("                    <div class=\"chart-title\">Status Distribution</div>\n")
                .append("                    <canvas id=\"statusChart\"></canvas>\n")
                .append("                </div>\n")
                .append("                <div class=\"chart-container\">\n")
                .append("                    <div class=\"chart-title\">Test Duration Trends</div>\n")
                .append("                    <canvas id=\"durationChart\"></canvas>\n")
                .append("                </div>\n")
                .append("            </div>\n")
                .append("        </div>\n")
                .append("        <div style=\"padding: 30px;\">\n")
                .append("            <div class=\"filter-controls\">\n")
                .append("                <strong>Filter Results:</strong>\n")
                .append("                <select id=\"statusFilter\">\n")
                .append("                    <option value=\"\">All Statuses</option>\n")
                .append("                    <option value=\"PASSED\">Passed</option>\n")
                .append("                    <option value=\"FAILED\">Failed</option>\n")
                .append("                    <option value=\"SKIPPED\">Skipped</option>\n")
                .append("                </select>\n")
                .append("                <select id=\"environmentFilter\">\n")
                .append("                    <option value=\"\">All Environments</option>\n")
                .append("                    <option value=\"dev\">Development</option>\n")
                .append("                    <option value=\"test\">Test</option>\n")
                .append("                    <option value=\"prod\">Production</option>\n")
                .append("                </select>\n")
                .append("                <input type=\"text\" id=\"testNameFilter\" placeholder=\"Search test name...\">\n")
                .append("            </div>\n")
                .append("            <table class=\"results-table\" id=\"resultsTable\">\n")
                .append("                <thead>\n")
                .append("                    <tr>\n")
                .append("                        <th>Test Name</th>\n")
                .append("                        <th>Suite</th>\n")
                .append("                        <th>Status</th>\n")
                .append("                        <th>Environment</th>\n")
                .append("                        <th>Duration</th>\n")
                .append("                        <th>Start Time</th>\n")
                .append("                        <th>Details</th>\n")
                .append("                    </tr>\n")
                .append("                </thead>\n")
                .append("                <tbody>\n");
            
            // Add test results
            for (ReportingFramework.TestExecution execution : executions) {
                String statusClass = "status-" + execution.getStatus().toLowerCase();
                body.append("                    <tr>\n")
                    .append("                        <td>").append(execution.getTestName()).append("</td>\n")
                    .append("                        <td>").append(execution.getSuiteName()).append("</td>\n")
                    .append("                        <td><span class=\"").append(statusClass).append("\">").append(execution.getStatus()).append("</span></td>\n")
                    .append("                        <td>").append(execution.getEnvironment() != null ? execution.getEnvironment() : "N/A").append("</td>\n")
                    .append("                        <td>").append(execution.getDuration()).append(" ms</td>\n")
                    .append("                        <td>").append(execution.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</td>\n")
                    .append("                        <td>");
                
                if (execution.getErrorMessage() != null) {
                    body.append("<span title=\"").append(execution.getErrorMessage().replace("\"", "&quot;")).append("\">❌ Error</span>");
                } else {
                    body.append("✅ OK");
                }
                
                body.append("</td>\n")
                    .append("                    </tr>\n");
            }
            
            body.append("                </tbody>\n")
                .append("            </table>\n")
                .append("        </div>\n")
                .append("        <div class=\"footer\">\n")
                .append("            <p>Generated by Phoenix HRM Test Automation Framework - Advanced Reporting Module</p>\n")
                .append("        </div>\n")
                .append("    </div>\n");
            
            return body.toString();
        }
        
        private String getHtmlFooter() {
            StringBuilder footer = new StringBuilder();
            footer.append("    <script>\n")
                .append("        // Status Distribution Chart\n")
                .append("        const statusCtx = document.getElementById('statusChart').getContext('2d');\n")
                .append("        new Chart(statusCtx, {\n")
                .append("            type: 'doughnut',\n")
                .append("            data: {\n")
                .append("                labels: ['Passed', 'Failed', 'Skipped'],\n")
                .append("                datasets: [{\n")
                .append("                    data: [/* Status counts will be injected here */],\n")
                .append("                    backgroundColor: ['#28a745', '#dc3545', '#ffc107'],\n")
                .append("                    borderWidth: 2,\n")
                .append("                    borderColor: '#fff'\n")
                .append("                }]\n")
                .append("            },\n")
                .append("            options: {\n")
                .append("                responsive: true,\n")
                .append("                plugins: {\n")
                .append("                    legend: { position: 'bottom' }\n")
                .append("                }\n")
                .append("            }\n")
                .append("        });\n")
                .append("        \n")
                .append("        // Duration Trends Chart\n")
                .append("        const durationCtx = document.getElementById('durationChart').getContext('2d');\n")
                .append("        new Chart(durationCtx, {\n")
                .append("            type: 'line',\n")
                .append("            data: {\n")
                .append("                labels: [/* Time labels will be injected here */],\n")
                .append("                datasets: [{\n")
                .append("                    label: 'Test Duration (ms)',\n")
                .append("                    data: [/* Duration data will be injected here */],\n")
                .append("                    borderColor: '#007bff',\n")
                .append("                    backgroundColor: 'rgba(0, 123, 255, 0.1)',\n")
                .append("                    tension: 0.4,\n")
                .append("                    fill: true\n")
                .append("                }]\n")
                .append("            },\n")
                .append("            options: {\n")
                .append("                responsive: true,\n")
                .append("                scales: {\n")
                .append("                    y: { beginAtZero: true }\n")
                .append("                }\n")
                .append("            }\n")
                .append("        });\n")
                .append("        \n")
                .append("        // Filter functionality\n")
                .append("        function filterTable() {\n")
                .append("            const statusFilter = document.getElementById('statusFilter').value;\n")
                .append("            const environmentFilter = document.getElementById('environmentFilter').value;\n")
                .append("            const nameFilter = document.getElementById('testNameFilter').value.toLowerCase();\n")
                .append("            const table = document.getElementById('resultsTable');\n")
                .append("            const rows = table.getElementsByTagName('tbody')[0].getElementsByTagName('tr');\n")
                .append("            \n")
                .append("            for (let row of rows) {\n")
                .append("                let show = true;\n")
                .append("                const cells = row.getElementsByTagName('td');\n")
                .append("                \n")
                .append("                if (statusFilter && !cells[2].textContent.includes(statusFilter)) show = false;\n")
                .append("                if (environmentFilter && !cells[3].textContent.includes(environmentFilter)) show = false;\n")
                .append("                if (nameFilter && !cells[0].textContent.toLowerCase().includes(nameFilter)) show = false;\n")
                .append("                \n")
                .append("                row.style.display = show ? '' : 'none';\n")
                .append("            }\n")
                .append("        }\n")
                .append("        \n")
                .append("        document.getElementById('statusFilter').addEventListener('change', filterTable);\n")
                .append("        document.getElementById('environmentFilter').addEventListener('change', filterTable);\n")
                .append("        document.getElementById('testNameFilter').addEventListener('input', filterTable);\n")
                .append("    </script>\n")
                .append("</body>\n")
                .append("</html>\n");
            return footer.toString();
        }
    }
}