package com.phoenix.hrm.reporting.templates;

import com.phoenix.hrm.reporting.templates.ReportTemplateEngine.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Demonstration application for the Report Template Engine
 * 
 * This demo showcases the comprehensive template capabilities including:
 * - Custom branding with Phoenix HRM theme
 * - Variable substitution and dynamic content generation
 * - Conditional logic and loops in templates
 * - Multiple output formats (HTML, Email)
 * - Internationalization support
 * - Performance metrics and statistics
 * - Template validation and error handling
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 6.0
 * @since Phase 6
 */
public class ReportTemplateEngineDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Phoenix HRM Report Template Engine Demo ===");
        System.out.println();
        
        // Initialize template engine with configuration
        TemplateConfiguration config = new TemplateConfiguration.Builder()
            .templateDirectory("src/test/resources/templates")
            .outputDirectory("target/reports")
            .enableCaching(true)
            .enableValidation(true)
            .enableMinification(false)
            .defaultLocale("en_US")
            .defaultTheme("default")
            .maxCacheSize(50)
            .addGlobalVariable("frameworkVersion", "6.0")
            .addGlobalVariable("companyName", "Phoenix HRM")
            .addGlobalVariable("supportEmail", "support@phoenix-hrm.com")
            .build();
        
        ReportTemplateEngine engine = new ReportTemplateEngine(config);
        
        // Generate sample test data
        Map<String, Object> testData = generateSampleTestData();
        
        // Create template context
        TemplateContext context = new TemplateContext(testData, "en_US", "default");
        
        // Demo 1: Render HTML Report Template
        System.out.println("1. Rendering HTML Report Template...");
        demoHtmlReportTemplate(engine, context);
        
        // Demo 2: Render Email Template
        System.out.println("\n2. Rendering Email Report Template...");
        demoEmailReportTemplate(engine, context);
        
        // Demo 3: Template with Spanish Localization
        System.out.println("\n3. Rendering Spanish Localized Template...");
        demoLocalizedTemplate(engine, testData);
        
        // Demo 4: Template with Dark Theme
        System.out.println("\n4. Rendering Dark Theme Template...");
        demoDarkThemeTemplate(engine, testData);
        
        // Demo 5: Custom Template Creation
        System.out.println("\n5. Creating and Rendering Custom Template...");
        demoCustomTemplate(engine, testData);
        
        // Demo 6: Template Validation
        System.out.println("\n6. Template Validation Demo...");
        demoTemplateValidation(engine);
        
        // Demo 7: Template Statistics
        System.out.println("\n7. Template Engine Statistics...");
        demoTemplateStatistics(engine);
        
        // Demo 8: Template Performance Test
        System.out.println("\n8. Template Performance Test...");
        demoPerformanceTest(engine, context);
        
        System.out.println("\n=== Demo Completed Successfully! ===");
        System.out.println("Check the 'target/reports' directory for generated files.");
    }
    
    /**
     * Demo 1: HTML Report Template Rendering
     */
    private static void demoHtmlReportTemplate(ReportTemplateEngine engine, TemplateContext context) {
        try {
            long startTime = System.currentTimeMillis();
            RenderResult result = engine.renderTemplate("test-report-detailed", context);
            long endTime = System.currentTimeMillis();
            
            if (result.isSuccess()) {
                System.out.printf("  ✅ HTML report rendered successfully in %dms%n", result.getRenderTime());
                System.out.printf("  📄 Output saved to: %s%n", result.getOutputPath());
                System.out.printf("  📊 Content length: %d characters%n", result.getContent().length());
                
                if (!result.getWarnings().isEmpty()) {
                    System.out.printf("  ⚠️  Warnings: %s%n", String.join(", ", result.getWarnings()));
                }
            } else {
                System.out.println("  ❌ HTML report rendering failed:");
                result.getErrors().forEach(error -> System.out.println("     - " + error));
            }
        } catch (Exception e) {
            System.out.println("  ❌ Exception during HTML rendering: " + e.getMessage());
        }
    }
    
    /**
     * Demo 2: Email Template Rendering
     */
    private static void demoEmailReportTemplate(ReportTemplateEngine engine, TemplateContext context) {
        try {
            RenderResult result = engine.renderTemplate("test-report-email", context);
            
            if (result.isSuccess()) {
                System.out.printf("  ✅ Email template rendered successfully in %dms%n", result.getRenderTime());
                System.out.printf("  📧 Output saved to: %s%n", result.getOutputPath());
                
                // Simulate email sending
                System.out.println("  📤 Email would be sent to: qa-team@phoenix-hrm.com");
                System.out.printf("  📝 Subject: Test Results - %s%n", 
                    context.getVariables().get("suiteName"));
            } else {
                System.out.println("  ❌ Email template rendering failed:");
                result.getErrors().forEach(error -> System.out.println("     - " + error));
            }
        } catch (Exception e) {
            System.out.println("  ❌ Exception during email rendering: " + e.getMessage());
        }
    }
    
    /**
     * Demo 3: Localized Template (Spanish)
     */
    private static void demoLocalizedTemplate(ReportTemplateEngine engine, Map<String, Object> testData) {
        try {
            TemplateContext spanishContext = new TemplateContext(testData, "es_ES", "default");
            RenderResult result = engine.renderTemplate("test-report-detailed", spanishContext);
            
            if (result.isSuccess()) {
                System.out.printf("  ✅ Spanish template rendered successfully in %dms%n", result.getRenderTime());
                System.out.println("  🌍 Localized content includes Spanish labels and formatting");
                
                // Show a sample of localized content
                String content = result.getContent();
                if (content.contains("Resultados de Prueba")) {
                    System.out.println("  🎯 Localization confirmed: Found 'Resultados de Prueba'");
                }
            } else {
                System.out.println("  ❌ Spanish template rendering failed:");
                result.getErrors().forEach(error -> System.out.println("     - " + error));
            }
        } catch (Exception e) {
            System.out.println("  ❌ Exception during Spanish rendering: " + e.getMessage());
        }
    }
    
    /**
     * Demo 4: Dark Theme Template
     */
    private static void demoDarkThemeTemplate(ReportTemplateEngine engine, Map<String, Object> testData) {
        try {
            TemplateContext darkContext = new TemplateContext(testData, "en_US", "dark");
            RenderResult result = engine.renderTemplate("test-report-detailed", darkContext);
            
            if (result.isSuccess()) {
                System.out.printf("  ✅ Dark theme template rendered successfully in %dms%n", result.getRenderTime());
                System.out.println("  🌙 Applied dark theme styling and colors");
                
                // Check for dark theme elements
                String content = result.getContent();
                if (content.contains("#212529") || content.contains("dark")) {
                    System.out.println("  🎨 Dark theme confirmed: Found dark color scheme");
                }
            } else {
                System.out.println("  ❌ Dark theme template rendering failed:");
                result.getErrors().forEach(error -> System.out.println("     - " + error));
            }
        } catch (Exception e) {
            System.out.println("  ❌ Exception during dark theme rendering: " + e.getMessage());
        }
    }
    
    /**
     * Demo 5: Custom Template Creation
     */
    private static void demoCustomTemplate(ReportTemplateEngine engine, Map<String, Object> testData) {
        try {
            // Create a simple custom template
            String customTemplateContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>{{suiteName}} - Custom Report</title>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 40px; }
                        .header { background: {{theme:primaryColor}}; color: white; padding: 20px; }
                        .stats { display: flex; gap: 20px; margin: 20px 0; }
                        .stat { background: #f5f5f5; padding: 15px; border-radius: 5px; }
                        .passed { color: green; }
                        .failed { color: red; }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <h1>{{suiteName}} Test Report</h1>
                        <p>Generated: {{now "yyyy-MM-dd HH:mm:ss"}}</p>
                    </div>
                    
                    <div class="stats">
                        <div class="stat">
                            <h3>Total Tests</h3>
                            <p>{{totalTests}}</p>
                        </div>
                        <div class="stat">
                            <h3 class="passed">Passed</h3>
                            <p>{{passedTests}}</p>
                        </div>
                        <div class="stat">
                            <h3 class="failed">Failed</h3>
                            <p>{{failedTests}}</p>
                        </div>
                    </div>
                    
                    <h2>Test Results</h2>
                    {{#each testResults}}
                    <div>
                        <strong>{{className}}.{{methodName}}</strong> - 
                        <span class="{{lowercase status}}">{{status}}</span>
                        ({{duration}}ms)
                    </div>
                    {{/each}}
                    
                    <hr>
                    <p><small>Generated by Phoenix HRM Test Automation Framework v{{frameworkVersion}}</small></p>
                </body>
                </html>
                """;
            
            // Create template
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("description", "Custom demo template");
            metadata.put("author", "Demo Application");
            
            Template customTemplate = engine.createTemplate(
                "custom-demo", 
                "Custom Demo Template", 
                Template.TemplateType.HTML, 
                customTemplateContent, 
                metadata
            );
            
            // Render custom template
            TemplateContext context = new TemplateContext(testData, "en_US", "default");
            RenderResult result = engine.renderTemplate("custom-demo", context);
            
            if (result.isSuccess()) {
                System.out.printf("  ✅ Custom template created and rendered successfully in %dms%n", result.getRenderTime());
                System.out.printf("  🎨 Template ID: %s%n", customTemplate.getId());
                System.out.printf("  📄 Output saved to: %s%n", result.getOutputPath());
            } else {
                System.out.println("  ❌ Custom template rendering failed:");
                result.getErrors().forEach(error -> System.out.println("     - " + error));
            }
        } catch (Exception e) {
            System.out.println("  ❌ Exception during custom template creation: " + e.getMessage());
        }
    }
    
    /**
     * Demo 6: Template Validation
     */
    private static void demoTemplateValidation(ReportTemplateEngine engine) {
        try {
            // Create a template with syntax errors for validation demo
            String invalidTemplate = """
                <html>
                <body>
                    <h1>{{title}}</h1>
                    {{#if condition}}
                        <p>Condition is true</p>
                    <!-- Missing {{/if}} -->
                    
                    {{#each items}}
                        <div>{{name}}</div>
                    <!-- Missing {{/each}} -->
                    
                    <p>Invalid syntax: {{invalid syntax here}}</p>
                    <p>Undefined variable: {{undefinedVariable}}</p>
                </body>
                </html>
                """;
            
            Template invalidTpl = engine.createTemplate(
                "invalid-demo", 
                "Invalid Template for Validation Demo", 
                Template.TemplateType.HTML, 
                invalidTemplate, 
                new HashMap<>()
            );
            
            // Validate template
            List<String> validationErrors = engine.validateTemplate("invalid-demo");
            
            System.out.printf("  🔍 Validation completed for template: %s%n", invalidTpl.getId());
            
            if (validationErrors.isEmpty()) {
                System.out.println("  ✅ No validation errors found");
            } else {
                System.out.printf("  ❌ Found %d validation errors:%n", validationErrors.size());
                validationErrors.forEach(error -> System.out.println("     - " + error));
            }
        } catch (Exception e) {
            System.out.println("  ❌ Exception during template validation: " + e.getMessage());
        }
    }
    
    /**
     * Demo 7: Template Engine Statistics
     */
    private static void demoTemplateStatistics(ReportTemplateEngine engine) {
        try {
            Map<String, Object> stats = engine.getTemplateStatistics();
            
            System.out.println("  📊 Template Engine Statistics:");
            System.out.printf("     Total Templates: %s%n", stats.get("totalTemplates"));
            System.out.printf("     Total Helpers: %s%n", stats.get("totalHelpers"));
            System.out.printf("     Caching Enabled: %s%n", stats.get("cacheEnabled"));
            System.out.printf("     Validation Enabled: %s%n", stats.get("validationEnabled"));
            System.out.printf("     Minification Enabled: %s%n", stats.get("minificationEnabled"));
            
            @SuppressWarnings("unchecked")
            Map<String, Long> typeDistribution = (Map<String, Long>) stats.get("templateTypeDistribution");
            if (typeDistribution != null && !typeDistribution.isEmpty()) {
                System.out.println("     Template Type Distribution:");
                typeDistribution.forEach((type, count) -> 
                    System.out.printf("       %s: %d%n", type, count));
            }
            
            @SuppressWarnings("unchecked")
            Set<String> themes = (Set<String>) stats.get("availableThemes");
            if (themes != null) {
                System.out.printf("     Available Themes: %s%n", String.join(", ", themes));
            }
            
            @SuppressWarnings("unchecked")
            Set<String> locales = (Set<String>) stats.get("availableLocales");
            if (locales != null) {
                System.out.printf("     Available Locales: %s%n", String.join(", ", locales));
            }
            
        } catch (Exception e) {
            System.out.println("  ❌ Exception while getting statistics: " + e.getMessage());
        }
    }
    
    /**
     * Demo 8: Performance Test
     */
    private static void demoPerformanceTest(ReportTemplateEngine engine, TemplateContext context) {
        try {
            System.out.println("  🏃 Running performance test (10 iterations)...");
            
            List<Long> renderTimes = new ArrayList<>();
            long totalStartTime = System.currentTimeMillis();
            
            for (int i = 0; i < 10; i++) {
                long iterationStart = System.nanoTime();
                RenderResult result = engine.renderTemplate("test-report-detailed", context);
                long iterationEnd = System.nanoTime();
                
                if (result.isSuccess()) {
                    long renderTimeMs = (iterationEnd - iterationStart) / 1_000_000;
                    renderTimes.add(renderTimeMs);
                } else {
                    System.out.printf("     Iteration %d failed%n", i + 1);
                }
            }
            
            long totalEndTime = System.currentTimeMillis();
            
            if (!renderTimes.isEmpty()) {
                // Calculate statistics
                long minTime = renderTimes.stream().mapToLong(Long::longValue).min().orElse(0);
                long maxTime = renderTimes.stream().mapToLong(Long::longValue).max().orElse(0);
                double avgTime = renderTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
                
                System.out.printf("  📈 Performance Results:%n");
                System.out.printf("     Total Time: %dms%n", totalEndTime - totalStartTime);
                System.out.printf("     Min Render Time: %dms%n", minTime);
                System.out.printf("     Max Render Time: %dms%n", maxTime);
                System.out.printf("     Avg Render Time: %.2fms%n", avgTime);
                System.out.printf("     Successful Renders: %d/10%n", renderTimes.size());
                
                if (avgTime < 100) {
                    System.out.println("  🚀 Excellent performance!");
                } else if (avgTime < 500) {
                    System.out.println("  ✅ Good performance");
                } else {
                    System.out.println("  ⚠️  Performance could be improved");
                }
            } else {
                System.out.println("  ❌ All performance test iterations failed");
            }
            
        } catch (Exception e) {
            System.out.println("  ❌ Exception during performance test: " + e.getMessage());
        }
    }
    
    /**
     * Generate comprehensive sample test data for template rendering
     */
    private static Map<String, Object> generateSampleTestData() {
        Map<String, Object> data = new HashMap<>();
        
        // Basic report information
        data.put("reportDate", LocalDateTime.now());
        data.put("suiteName", "Phoenix HRM User Management Tests");
        data.put("buildNumber", "BUILD-2024-001");
        data.put("buildBranch", "feature/user-management");
        data.put("totalDuration", "4m 32s");
        
        // Test statistics
        data.put("totalTests", 25);
        data.put("passedTests", 22);
        data.put("failedTests", 2);
        data.put("skippedTests", 1);
        
        // Environment information
        Map<String, Object> environment = new HashMap<>();
        environment.put("browser", "Chrome");
        environment.put("browserVersion", "119.0.6045.105");
        environment.put("os", "Windows 11");
        environment.put("javaVersion", "17.0.8");
        environment.put("testEnvironment", "QA");
        environment.put("testRunner", "TestNG");
        environment.put("parallelThreads", 4);
        data.put("environment", environment);
        
        // Detailed test results
        List<Map<String, Object>> testResults = new ArrayList<>();
        
        // Passed tests
        testResults.add(createTestResult("UserRegistrationTest", "testValidUserRegistration", 
            "PASSED", 1250, "Chrome", null, null));
        testResults.add(createTestResult("UserRegistrationTest", "testEmailValidation", 
            "PASSED", 890, "Chrome", null, null));
        testResults.add(createTestResult("UserLoginTest", "testValidLogin", 
            "PASSED", 1100, "Chrome", null, null));
        testResults.add(createTestResult("UserLoginTest", "testInvalidCredentials", 
            "PASSED", 950, "Chrome", null, null));
        testResults.add(createTestResult("UserProfileTest", "testUpdateProfile", 
            "PASSED", 1450, "Chrome", null, null));
        testResults.add(createTestResult("UserProfileTest", "testUploadAvatar", 
            "PASSED", 2100, "Chrome", null, null));
        testResults.add(createTestResult("PasswordResetTest", "testPasswordResetRequest", 
            "PASSED", 800, "Chrome", null, null));
        testResults.add(createTestResult("PasswordResetTest", "testPasswordResetConfirmation", 
            "PASSED", 1300, "Chrome", null, null));
        testResults.add(createTestResult("UserRolesTest", "testAssignRole", 
            "PASSED", 1050, "Chrome", null, null));
        testResults.add(createTestResult("UserRolesTest", "testRemoveRole", 
            "PASSED", 980, "Chrome", null, null));
        testResults.add(createTestResult("UserSearchTest", "testSearchByName", 
            "PASSED", 750, "Chrome", null, null));
        testResults.add(createTestResult("UserSearchTest", "testSearchByEmail", 
            "PASSED", 820, "Chrome", null, null));
        testResults.add(createTestResult("UserDeactivationTest", "testDeactivateUser", 
            "PASSED", 1200, "Chrome", null, null));
        testResults.add(createTestResult("UserDeactivationTest", "testReactivateUser", 
            "PASSED", 1150, "Chrome", null, null));
        testResults.add(createTestResult("UserPermissionsTest", "testViewPermissions", 
            "PASSED", 900, "Chrome", null, null));
        testResults.add(createTestResult("UserPermissionsTest", "testEditPermissions", 
            "PASSED", 1350, "Chrome", null, null));
        testResults.add(createTestResult("UserAuditTest", "testLoginAudit", 
            "PASSED", 650, "Chrome", null, null));
        testResults.add(createTestResult("UserAuditTest", "testActionAudit", 
            "PASSED", 780, "Chrome", null, null));
        testResults.add(createTestResult("UserBulkOperationsTest", "testBulkImport", 
            "PASSED", 3200, "Chrome", null, null));
        testResults.add(createTestResult("UserBulkOperationsTest", "testBulkExport", 
            "PASSED", 2800, "Chrome", null, null));
        testResults.add(createTestResult("UserNotificationsTest", "testWelcomeEmail", 
            "PASSED", 1100, "Chrome", null, null));
        testResults.add(createTestResult("UserNotificationsTest", "testPasswordChangeNotification", 
            "PASSED", 950, "Chrome", null, null));
        
        // Failed tests
        testResults.add(createTestResult("UserRegistrationTest", "testDuplicateEmailHandling", 
            "FAILED", 2300, "Chrome", 
            "Expected error message 'Email already exists' but got 'User registration failed'",
            "AssertionError: Expected error message 'Email already exists' but got 'User registration failed'\n" +
            "    at UserRegistrationTest.testDuplicateEmailHandling(UserRegistrationTest.java:85)\n" +
            "    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n" +
            "    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)"));
        
        testResults.add(createTestResult("UserProfileTest", "testInvalidProfileData", 
            "FAILED", 1800, "Chrome",
            "Profile validation failed: Phone number format not validated properly",
            "ValidationException: Phone number format not validated properly\n" +
            "    at ProfileValidator.validate(ProfileValidator.java:42)\n" +
            "    at UserProfileTest.testInvalidProfileData(UserProfileTest.java:120)"));
        
        // Skipped test
        testResults.add(createTestResult("UserIntegrationTest", "testLDAPIntegration", 
            "SKIPPED", 0, "Chrome", "LDAP server not available in test environment", null));
        
        data.put("testResults", testResults);
        
        // Performance metrics
        Map<String, Object> performance = new HashMap<>();
        Map<String, Object> fastestTest = new HashMap<>();
        fastestTest.put("name", "UserAuditTest.testLoginAudit");
        fastestTest.put("duration", 650);
        performance.put("fastestTest", fastestTest);
        
        Map<String, Object> slowestTest = new HashMap<>();
        slowestTest.put("name", "UserBulkOperationsTest.testBulkImport");
        slowestTest.put("duration", 3200);
        performance.put("slowestTest", slowestTest);
        
        performance.put("averageDuration", 1284.5);
        performance.put("setupTime", 2500);
        performance.put("teardownTime", 1800);
        data.put("performance", performance);
        
        // Test categories
        List<Map<String, Object>> testCategories = new ArrayList<>();
        testCategories.add(createTestCategory("Authentication", 6, 5, 1, 83.3));
        testCategories.add(createTestCategory("User Management", 8, 7, 1, 87.5));
        testCategories.add(createTestCategory("Permissions", 4, 4, 0, 100.0));
        testCategories.add(createTestCategory("Bulk Operations", 2, 2, 0, 100.0));
        testCategories.add(createTestCategory("Notifications", 2, 2, 0, 100.0));
        testCategories.add(createTestCategory("Integration", 1, 0, 0, 0.0));
        testCategories.add(createTestCategory("Audit", 2, 2, 0, 100.0));
        data.put("testCategories", testCategories);
        
        // Test tags
        List<Map<String, Object>> tags = new ArrayList<>();
        tags.add(createTag("smoke", 8));
        tags.add(createTag("regression", 15));
        tags.add(createTag("user-management", 25));
        tags.add(createTag("authentication", 6));
        tags.add(createTag("integration", 3));
        tags.add(createTag("performance", 2));
        data.put("tags", tags);
        
        // URLs for actions
        data.put("reportUrl", "https://ci.phoenix-hrm.com/reports/BUILD-2024-001/index.html");
        data.put("dashboardUrl", "https://dashboard.phoenix-hrm.com/test-automation");
        data.put("documentationUrl", "https://docs.phoenix-hrm.com/testing");
        data.put("supportUrl", "https://support.phoenix-hrm.com");
        data.put("settingsUrl", "https://dashboard.phoenix-hrm.com/settings");
        data.put("unsubscribeUrl", "https://notifications.phoenix-hrm.com/unsubscribe");
        data.put("preferencesUrl", "https://notifications.phoenix-hrm.com/preferences");
        
        return data;
    }
    
    /**
     * Create a test result entry
     */
    private static Map<String, Object> createTestResult(String className, String methodName, 
            String status, long duration, String environment, String errorMessage, String stackTrace) {
        Map<String, Object> result = new HashMap<>();
        result.put("className", className);
        result.put("methodName", methodName);
        result.put("status", status);
        result.put("duration", duration);
        result.put("environment", environment);
        result.put("startTime", LocalDateTime.now().minusMinutes((int)(Math.random() * 30)));
        
        if (errorMessage != null) {
            result.put("errorMessage", errorMessage);
        }
        if (stackTrace != null) {
            result.put("stackTrace", stackTrace);
        }
        
        return result;
    }
    
    /**
     * Create a test category entry
     */
    private static Map<String, Object> createTestCategory(String name, int total, int passed, int failed, double successRate) {
        Map<String, Object> category = new HashMap<>();
        category.put("name", name);
        category.put("total", total);
        category.put("passed", passed);
        category.put("failed", failed);
        category.put("successRate", successRate);
        return category;
    }
    
    /**
     * Create a test tag entry
     */
    private static Map<String, Object> createTag(String name, int count) {
        Map<String, Object> tag = new HashMap<>();
        tag.put("name", name);
        tag.put("count", count);
        return tag;
    }
}