package com.phoenix.hrm.reporting.templates;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Report Template Engine for Phoenix HRM Test Automation
 * 
 * Provides comprehensive template management capabilities including:
 * - Custom branding and styling with corporate themes
 * - Advanced variable substitution with conditional logic
 * - Dynamic content generation with loops and conditions
 * - Multi-format template support (HTML, PDF, Word, Excel)
 * - Template inheritance and composition
 * - Internationalization and localization support
 * - Template validation and syntax checking
 * - Custom helper functions and filters
 * - Template caching and performance optimization
 * - Real-time template preview capabilities
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 6.0
 * @since Phase 6
 */
public class ReportTemplateEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportTemplateEngine.class);
    
    // Template variable patterns
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([^}]+?)\\s*\\}\\}");
    private static final Pattern CONDITIONAL_PATTERN = Pattern.compile("\\{\\{#if\\s+([^}]+?)\\}\\}(.*?)\\{\\{/if\\}\\}", Pattern.DOTALL);
    private static final Pattern LOOP_PATTERN = Pattern.compile("\\{\\{#each\\s+([^}]+?)\\}\\}(.*?)\\{\\{/each\\}\\}", Pattern.DOTALL);
    private static final Pattern HELPER_PATTERN = Pattern.compile("\\{\\{([^}]+?)\\s+([^}]+?)\\}\\}");
    
    private final TemplateConfiguration config;
    private final ObjectMapper objectMapper;
    private final Map<String, Template> templateCache;
    private final Map<String, TemplateHelper> helpers;
    private final ThemeManager themeManager;
    private final LocalizationManager localizationManager;
    private final TemplateValidator validator;
    
    /**
     * Template configuration
     */
    public static class TemplateConfiguration {
        private String templateDirectory = "src/test/resources/templates";
        private String outputDirectory = "target/reports";
        private boolean enableCaching = true;
        private boolean enableValidation = true;
        private boolean enableMinification = false;
        private String defaultLocale = "en_US";
        private String defaultTheme = "default";
        private int maxCacheSize = 100;
        private Map<String, String> globalVariables = new HashMap<>();
        private Map<String, String> customHelpers = new HashMap<>();
        
        // Builder pattern
        public static class Builder {
            private final TemplateConfiguration config = new TemplateConfiguration();
            
            public Builder templateDirectory(String directory) {
                config.templateDirectory = directory;
                return this;
            }
            
            public Builder outputDirectory(String directory) {
                config.outputDirectory = directory;
                return this;
            }
            
            public Builder enableCaching(boolean enable) {
                config.enableCaching = enable;
                return this;
            }
            
            public Builder enableValidation(boolean enable) {
                config.enableValidation = enable;
                return this;
            }
            
            public Builder enableMinification(boolean enable) {
                config.enableMinification = enable;
                return this;
            }
            
            public Builder defaultLocale(String locale) {
                config.defaultLocale = locale;
                return this;
            }
            
            public Builder defaultTheme(String theme) {
                config.defaultTheme = theme;
                return this;
            }
            
            public Builder maxCacheSize(int size) {
                config.maxCacheSize = size;
                return this;
            }
            
            public Builder addGlobalVariable(String key, String value) {
                config.globalVariables.put(key, value);
                return this;
            }
            
            public Builder addCustomHelper(String name, String className) {
                config.customHelpers.put(name, className);
                return this;
            }
            
            public TemplateConfiguration build() {
                return config;
            }
        }
        
        // Getters
        public String getTemplateDirectory() { return templateDirectory; }
        public String getOutputDirectory() { return outputDirectory; }
        public boolean isEnableCaching() { return enableCaching; }
        public boolean isEnableValidation() { return enableValidation; }
        public boolean isEnableMinification() { return enableMinification; }
        public String getDefaultLocale() { return defaultLocale; }
        public String getDefaultTheme() { return defaultTheme; }
        public int getMaxCacheSize() { return maxCacheSize; }
        public Map<String, String> getGlobalVariables() { return globalVariables; }
        public Map<String, String> getCustomHelpers() { return customHelpers; }
    }
    
    /**
     * Template representation
     */
    public static class Template {
        private final String id;
        private final String name;
        private final TemplateType type;
        private final String content;
        private final Map<String, Object> metadata;
        private final LocalDateTime created;
        private final LocalDateTime modified;
        private Template parent; // For template inheritance
        private Map<String, String> blocks; // For template blocks
        
        public enum TemplateType {
            HTML, PDF, WORD, EXCEL, EMAIL, CUSTOM
        }
        
        public Template(String id, String name, TemplateType type, String content, Map<String, Object> metadata) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.content = content;
            this.metadata = metadata != null ? metadata : new HashMap<>();
            this.created = LocalDateTime.now();
            this.modified = LocalDateTime.now();
            this.blocks = new HashMap<>();
        }
        
        // Getters and setters
        public String getId() { return id; }
        public String getName() { return name; }
        public TemplateType getType() { return type; }
        public String getContent() { return content; }
        public Map<String, Object> getMetadata() { return metadata; }
        public LocalDateTime getCreated() { return created; }
        public LocalDateTime getModified() { return modified; }
        public Template getParent() { return parent; }
        public void setParent(Template parent) { this.parent = parent; }
        public Map<String, String> getBlocks() { return blocks; }
        public void setBlocks(Map<String, String> blocks) { this.blocks = blocks; }
        
        @Override
        public String toString() {
            return String.format("Template{id='%s', name='%s', type=%s}", id, name, type);
        }
    }
    
    /**
     * Template rendering context
     */
    public static class TemplateContext {
        private final Map<String, Object> variables;
        private final String locale;
        private final String theme;
        private final Map<String, Object> metadata;
        
        public TemplateContext() {
            this.variables = new HashMap<>();
            this.locale = "en_US";
            this.theme = "default";
            this.metadata = new HashMap<>();
        }
        
        public TemplateContext(Map<String, Object> variables, String locale, String theme) {
            this.variables = new HashMap<>(variables);
            this.locale = locale != null ? locale : "en_US";
            this.theme = theme != null ? theme : "default";
            this.metadata = new HashMap<>();
        }
        
        public void addVariable(String key, Object value) {
            variables.put(key, value);
        }
        
        public void addVariables(Map<String, Object> vars) {
            variables.putAll(vars);
        }
        
        // Getters
        public Map<String, Object> getVariables() { return variables; }
        public String getLocale() { return locale; }
        public String getTheme() { return theme; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
    
    /**
     * Template rendering result
     */
    public static class RenderResult {
        private final boolean success;
        private final String content;
        private final String templateId;
        private final long renderTime;
        private final String outputPath;
        private final Map<String, Object> metadata;
        private final List<String> warnings;
        private final List<String> errors;
        
        public RenderResult(boolean success, String content, String templateId, long renderTime,
                          String outputPath, Map<String, Object> metadata, 
                          List<String> warnings, List<String> errors) {
            this.success = success;
            this.content = content;
            this.templateId = templateId;
            this.renderTime = renderTime;
            this.outputPath = outputPath;
            this.metadata = metadata != null ? metadata : new HashMap<>();
            this.warnings = warnings != null ? warnings : new ArrayList<>();
            this.errors = errors != null ? errors : new ArrayList<>();
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getContent() { return content; }
        public String getTemplateId() { return templateId; }
        public long getRenderTime() { return renderTime; }
        public String getOutputPath() { return outputPath; }
        public Map<String, Object> getMetadata() { return metadata; }
        public List<String> getWarnings() { return warnings; }
        public List<String> getErrors() { return errors; }
        
        @Override
        public String toString() {
            return String.format("RenderResult{success=%b, templateId='%s', renderTime=%dms}", 
                success, templateId, renderTime);
        }
    }
    
    /**
     * Constructor
     */
    public ReportTemplateEngine(TemplateConfiguration config) {
        this.config = config != null ? config : new TemplateConfiguration.Builder().build();
        this.objectMapper = new ObjectMapper();
        this.templateCache = new ConcurrentHashMap<>();
        this.helpers = new ConcurrentHashMap<>();
        this.themeManager = new ThemeManager(this.config);
        this.localizationManager = new LocalizationManager(this.config);
        this.validator = new TemplateValidator();
        
        // Initialize built-in helpers
        initializeBuiltInHelpers();
        
        // Load custom helpers
        loadCustomHelpers();
        
        // Load templates
        loadTemplates();
        
        logger.info("ReportTemplateEngine initialized with {} templates and {} helpers", 
            templateCache.size(), helpers.size());
    }
    
    /**
     * Render template with context
     */
    public RenderResult renderTemplate(String templateId, TemplateContext context) {
        long startTime = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        try {
            // Get template
            Template template = getTemplate(templateId);
            if (template == null) {
                errors.add("Template not found: " + templateId);
                return new RenderResult(false, null, templateId, 0, null, null, warnings, errors);
            }
            
            // Validate template if enabled
            if (config.isEnableValidation()) {
                List<String> validationErrors = validator.validateTemplate(template);
                if (!validationErrors.isEmpty()) {
                    errors.addAll(validationErrors);
                    return new RenderResult(false, null, templateId, 0, null, null, warnings, errors);
                }
            }
            
            // Merge global variables
            TemplateContext mergedContext = mergeGlobalVariables(context);
            
            // Apply theme
            String themedContent = themeManager.applyTheme(template.getContent(), mergedContext.getTheme());
            
            // Process template inheritance
            String inheritedContent = processTemplateInheritance(template, themedContent);
            
            // Render content
            String renderedContent = renderContent(inheritedContent, mergedContext, warnings, errors);
            
            // Apply localization
            renderedContent = localizationManager.localize(renderedContent, mergedContext.getLocale());
            
            // Minify if enabled
            if (config.isEnableMinification()) {
                renderedContent = minifyContent(renderedContent, template.getType());
            }
            
            // Save to file if needed
            String outputPath = saveRenderedContent(renderedContent, templateId, template.getType());
            
            long renderTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("templateType", template.getType().toString());
            metadata.put("locale", mergedContext.getLocale());
            metadata.put("theme", mergedContext.getTheme());
            metadata.put("variableCount", mergedContext.getVariables().size());
            
            logger.debug("Rendered template {} in {}ms", templateId, renderTime);
            
            return new RenderResult(true, renderedContent, templateId, renderTime, 
                outputPath, metadata, warnings, errors);
            
        } catch (Exception e) {
            long renderTime = System.currentTimeMillis() - startTime;
            logger.error("Template rendering failed for {}: {}", templateId, e.getMessage(), e);
            
            errors.add("Rendering failed: " + e.getMessage());
            return new RenderResult(false, null, templateId, renderTime, null, null, warnings, errors);
        }
    }
    
    /**
     * Create new template
     */
    public Template createTemplate(String id, String name, Template.TemplateType type, 
                                 String content, Map<String, Object> metadata) {
        Template template = new Template(id, name, type, content, metadata);
        
        if (config.isEnableCaching()) {
            templateCache.put(id, template);
            
            // Manage cache size
            if (templateCache.size() > config.getMaxCacheSize()) {
                // Remove oldest template (simple LRU)
                String oldestKey = templateCache.keySet().iterator().next();
                templateCache.remove(oldestKey);
            }
        }
        
        logger.info("Created template: {} ({})", name, type);
        return template;
    }
    
    /**
     * Load template from file
     */
    public Template loadTemplateFromFile(String templateId, String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("Template file not found: " + filePath);
        }
        
        String content = Files.readString(path);
        String name = path.getFileName().toString();
        Template.TemplateType type = determineTemplateType(name);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("filePath", filePath);
        metadata.put("fileSize", Files.size(path));
        metadata.put("lastModified", Files.getLastModifiedTime(path).toString());
        
        Template template = createTemplate(templateId, name, type, content, metadata);
        
        // Parse template blocks and inheritance
        parseTemplateStructure(template);
        
        return template;
    }
    
    /**
     * Save template to file
     */
    public void saveTemplateToFile(String templateId, String filePath) throws IOException {
        Template template = getTemplate(templateId);
        if (template == null) {
            throw new IllegalArgumentException("Template not found: " + templateId);
        }
        
        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, template.getContent());
        
        logger.info("Saved template {} to {}", templateId, filePath);
    }
    
    /**
     * Get template
     */
    public Template getTemplate(String templateId) {
        return templateCache.get(templateId);
    }
    
    /**
     * List all templates
     */
    public List<Template> getAllTemplates() {
        return new ArrayList<>(templateCache.values());
    }
    
    /**
     * Register custom helper
     */
    public void registerHelper(String name, TemplateHelper helper) {
        helpers.put(name, helper);
        logger.debug("Registered helper: {}", name);
    }
    
    /**
     * Preview template with sample data
     */
    public String previewTemplate(String templateId, Map<String, Object> sampleData) {
        TemplateContext context = new TemplateContext(sampleData, config.getDefaultLocale(), config.getDefaultTheme());
        RenderResult result = renderTemplate(templateId, context);
        
        if (result.isSuccess()) {
            return result.getContent();
        } else {
            return "Preview failed: " + String.join(", ", result.getErrors());
        }
    }
    
    /**
     * Validate template syntax
     */
    public List<String> validateTemplate(String templateId) {
        Template template = getTemplate(templateId);
        if (template == null) {
            return List.of("Template not found: " + templateId);
        }
        
        return validator.validateTemplate(template);
    }
    
    /**
     * Get template statistics
     */
    public Map<String, Object> getTemplateStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalTemplates", templateCache.size());
        stats.put("totalHelpers", helpers.size());
        stats.put("cacheEnabled", config.isEnableCaching());
        stats.put("validationEnabled", config.isEnableValidation());
        stats.put("minificationEnabled", config.isEnableMinification());
        
        // Template type distribution
        Map<String, Long> typeDistribution = templateCache.values().stream()
            .collect(Collectors.groupingBy(
                t -> t.getType().toString(),
                Collectors.counting()));
        stats.put("templateTypeDistribution", typeDistribution);
        
        // Theme and locale info
        stats.put("availableThemes", themeManager.getAvailableThemes());
        stats.put("availableLocales", localizationManager.getAvailableLocales());
        
        return stats;
    }
    
    // Private helper methods
    
    private void initializeBuiltInHelpers() {
        // Date/time helpers
        helpers.put("formatDate", new DateFormatHelper());
        helpers.put("now", new CurrentDateHelper());
        
        // String helpers
        helpers.put("uppercase", new UppercaseHelper());
        helpers.put("lowercase", new LowercaseHelper());
        helpers.put("capitalize", new CapitalizeHelper());
        helpers.put("truncate", new TruncateHelper());
        
        // Math helpers
        helpers.put("add", new AddHelper());
        helpers.put("subtract", new SubtractHelper());
        helpers.put("multiply", new MultiplyHelper());
        helpers.put("divide", new DivideHelper());
        helpers.put("round", new RoundHelper());
        
        // Logic helpers
        helpers.put("equals", new EqualsHelper());
        helpers.put("notEquals", new NotEqualsHelper());
        helpers.put("greaterThan", new GreaterThanHelper());
        helpers.put("lessThan", new LessThanHelper());
        
        // Collection helpers
        helpers.put("length", new LengthHelper());
        helpers.put("first", new FirstHelper());
        helpers.put("last", new LastHelper());
        helpers.put("join", new JoinHelper());
        
        logger.debug("Initialized {} built-in helpers", helpers.size());
    }
    
    private void loadCustomHelpers() {
        config.getCustomHelpers().forEach((name, className) -> {
            try {
                Class<?> clazz = Class.forName(className);
                TemplateHelper helper = (TemplateHelper) clazz.getDeclaredConstructor().newInstance();
                helpers.put(name, helper);
                logger.debug("Loaded custom helper: {} ({})", name, className);
            } catch (Exception e) {
                logger.warn("Failed to load custom helper {}: {}", name, e.getMessage());
            }
        });
    }
    
    private void loadTemplates() {
        try {
            Path templateDir = Paths.get(config.getTemplateDirectory());
            if (Files.exists(templateDir)) {
                Files.walk(templateDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> isTemplateFile(p.toString()))
                    .forEach(path -> {
                        try {
                            String templateId = generateTemplateId(path);
                            loadTemplateFromFile(templateId, path.toString());
                        } catch (Exception e) {
                            logger.warn("Failed to load template {}: {}", path, e.getMessage());
                        }
                    });
            }
        } catch (Exception e) {
            logger.warn("Error loading templates from directory: {}", e.getMessage());
        }
    }
    
    private boolean isTemplateFile(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".html") || lower.endsWith(".htm") || 
               lower.endsWith(".template") || lower.endsWith(".tmpl");
    }
    
    private String generateTemplateId(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }
    
    private Template.TemplateType determineTemplateType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return Template.TemplateType.HTML;
        if (lower.endsWith(".pdf")) return Template.TemplateType.PDF;
        if (lower.endsWith(".docx") || lower.endsWith(".doc")) return Template.TemplateType.WORD;
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) return Template.TemplateType.EXCEL;
        if (lower.endsWith(".eml") || lower.contains("email")) return Template.TemplateType.EMAIL;
        return Template.TemplateType.CUSTOM;
    }
    
    private TemplateContext mergeGlobalVariables(TemplateContext context) {
        Map<String, Object> mergedVariables = new HashMap<>(config.getGlobalVariables());
        mergedVariables.putAll(context.getVariables());
        
        return new TemplateContext(mergedVariables, context.getLocale(), context.getTheme());
    }
    
    private String processTemplateInheritance(Template template, String content) {
        if (template.getParent() != null) {
            String parentContent = template.getParent().getContent();
            
            // Replace blocks in parent template
            for (Map.Entry<String, String> block : template.getBlocks().entrySet()) {
                String blockPlaceholder = "{{block:" + block.getKey() + "}}";
                parentContent = parentContent.replace(blockPlaceholder, block.getValue());
            }
            
            return parentContent;
        }
        
        return content;
    }
    
    private String renderContent(String content, TemplateContext context, 
                               List<String> warnings, List<String> errors) {
        String result = content;
        
        try {
            // Process conditional statements
            result = processConditionals(result, context, warnings, errors);
            
            // Process loops
            result = processLoops(result, context, warnings, errors);
            
            // Process helper functions
            result = processHelpers(result, context, warnings, errors);
            
            // Process variables
            result = processVariables(result, context, warnings, errors);
            
        } catch (Exception e) {
            errors.add("Content rendering failed: " + e.getMessage());
            logger.error("Content rendering error", e);
        }
        
        return result;
    }
    
    private String processConditionals(String content, TemplateContext context,
                                     List<String> warnings, List<String> errors) {
        Matcher matcher = CONDITIONAL_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String condition = matcher.group(1).trim();
            String conditionalContent = matcher.group(2);
            
            try {
                boolean conditionResult = evaluateCondition(condition, context);
                String replacement = conditionResult ? conditionalContent : "";
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } catch (Exception e) {
                warnings.add("Failed to evaluate condition: " + condition);
                matcher.appendReplacement(result, "");
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private String processLoops(String content, TemplateContext context,
                              List<String> warnings, List<String> errors) {
        Matcher matcher = LOOP_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String arrayName = matcher.group(1).trim();
            String loopContent = matcher.group(2);
            
            try {
                Object arrayValue = context.getVariables().get(arrayName);
                String replacement = processLoop(arrayValue, loopContent, context);
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } catch (Exception e) {
                warnings.add("Failed to process loop: " + arrayName);
                matcher.appendReplacement(result, "");
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private String processHelpers(String content, TemplateContext context,
                                List<String> warnings, List<String> errors) {
        Matcher matcher = HELPER_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String helperExpression = matcher.group(0);
            String[] parts = helperExpression.replaceAll("[{}]", "").trim().split("\\s+");
            
            if (parts.length >= 2) {
                String helperName = parts[0];
                String[] args = Arrays.copyOfRange(parts, 1, parts.length);
                
                try {
                    TemplateHelper helper = helpers.get(helperName);
                    if (helper != null) {
                        String replacement = helper.execute(args, context);
                        matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                    } else {
                        warnings.add("Unknown helper: " + helperName);
                        matcher.appendReplacement(result, helperExpression);
                    }
                } catch (Exception e) {
                    warnings.add("Helper execution failed: " + helperName);
                    matcher.appendReplacement(result, helperExpression);
                }
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private String processVariables(String content, TemplateContext context,
                                  List<String> warnings, List<String> errors) {
        Matcher matcher = VARIABLE_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String variableName = matcher.group(1).trim();
            
            try {
                Object value = getVariableValue(variableName, context);
                String replacement = value != null ? value.toString() : "";
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } catch (Exception e) {
                warnings.add("Variable not found or invalid: " + variableName);
                matcher.appendReplacement(result, "{{" + variableName + "}}");
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private boolean evaluateCondition(String condition, TemplateContext context) {
        // Simple condition evaluation
        if (condition.contains("==")) {
            String[] parts = condition.split("==");
            if (parts.length == 2) {
                Object left = getVariableValue(parts[0].trim(), context);
                Object right = getVariableValue(parts[1].trim(), context);
                return Objects.equals(left, right);
            }
        } else if (condition.contains("!=")) {
            String[] parts = condition.split("!=");
            if (parts.length == 2) {
                Object left = getVariableValue(parts[0].trim(), context);
                Object right = getVariableValue(parts[1].trim(), context);
                return !Objects.equals(left, right);
            }
        } else {
            // Simple boolean check
            Object value = getVariableValue(condition, context);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            return value != null && !value.toString().isEmpty();
        }
        
        return false;
    }
    
    private String processLoop(Object arrayValue, String loopContent, TemplateContext context) {
        if (arrayValue == null) return "";
        
        StringBuilder result = new StringBuilder();
        
        if (arrayValue instanceof List) {
            List<?> list = (List<?>) arrayValue;
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                TemplateContext loopContext = createLoopContext(context, item, i, list.size());
                String itemContent = renderContent(loopContent, loopContext, new ArrayList<>(), new ArrayList<>());
                result.append(itemContent);
            }
        } else if (arrayValue instanceof Object[]) {
            Object[] array = (Object[]) arrayValue;
            for (int i = 0; i < array.length; i++) {
                Object item = array[i];
                TemplateContext loopContext = createLoopContext(context, item, i, array.length);
                String itemContent = renderContent(loopContent, loopContext, new ArrayList<>(), new ArrayList<>());
                result.append(itemContent);
            }
        }
        
        return result.toString();
    }
    
    private TemplateContext createLoopContext(TemplateContext parentContext, Object item, int index, int total) {
        Map<String, Object> loopVariables = new HashMap<>(parentContext.getVariables());
        loopVariables.put("this", item);
        loopVariables.put("@index", index);
        loopVariables.put("@first", index == 0);
        loopVariables.put("@last", index == total - 1);
        loopVariables.put("@total", total);
        
        // If item is a map, add its properties directly
        if (item instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> itemMap = (Map<String, Object>) item;
            loopVariables.putAll(itemMap);
        }
        
        return new TemplateContext(loopVariables, parentContext.getLocale(), parentContext.getTheme());
    }
    
    private Object getVariableValue(String variableName, TemplateContext context) {
        // Handle nested properties (e.g., user.name)
        if (variableName.contains(".")) {
            String[] parts = variableName.split("\\.");
            Object current = context.getVariables().get(parts[0]);
            
            for (int i = 1; i < parts.length && current != null; i++) {
                if (current instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) current;
                    current = map.get(parts[i]);
                } else {
                    // Try to access property via reflection (for Java objects)
                    try {
                        current = current.getClass().getMethod("get" + capitalize(parts[i])).invoke(current);
                    } catch (Exception e) {
                        return null;
                    }
                }
            }
            
            return current;
        } else {
            // Handle quoted strings
            if (variableName.startsWith("\"") && variableName.endsWith("\"")) {
                return variableName.substring(1, variableName.length() - 1);
            }
            
            // Handle numbers
            try {
                if (variableName.contains(".")) {
                    return Double.parseDouble(variableName);
                } else {
                    return Integer.parseInt(variableName);
                }
            } catch (NumberFormatException e) {
                // Not a number, continue
            }
            
            // Handle boolean
            if ("true".equalsIgnoreCase(variableName)) return true;
            if ("false".equalsIgnoreCase(variableName)) return false;
            
            return context.getVariables().get(variableName);
        }
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    private void parseTemplateStructure(Template template) {
        // Parse template for blocks and inheritance
        String content = template.getContent();
        
        // Look for extends directive
        Pattern extendsPattern = Pattern.compile("\\{\\{extends\\s+([^}]+?)\\}\\}");
        Matcher extendsMatcher = extendsPattern.matcher(content);
        if (extendsMatcher.find()) {
            String parentId = extendsMatcher.group(1).trim().replaceAll("['\"]", "");
            Template parent = getTemplate(parentId);
            if (parent != null) {
                template.setParent(parent);
            }
        }
        
        // Parse blocks
        Pattern blockPattern = Pattern.compile("\\{\\{block\\s+([^}]+?)\\}\\}(.*?)\\{\\{/block\\}\\}", Pattern.DOTALL);
        Matcher blockMatcher = blockPattern.matcher(content);
        while (blockMatcher.find()) {
            String blockName = blockMatcher.group(1).trim();
            String blockContent = blockMatcher.group(2);
            template.getBlocks().put(blockName, blockContent);
        }
    }
    
    private String minifyContent(String content, Template.TemplateType type) {
        if (type == Template.TemplateType.HTML) {
            // Simple HTML minification
            return content
                .replaceAll("\\s+", " ")
                .replaceAll(">\\s+<", "><")
                .trim();
        }
        return content;
    }
    
    private String saveRenderedContent(String content, String templateId, Template.TemplateType type) {
        try {
            String extension = getFileExtension(type);
            String fileName = templateId + "_" + System.currentTimeMillis() + extension;
            Path outputPath = Paths.get(config.getOutputDirectory(), fileName);
            
            Files.createDirectories(outputPath.getParent());
            Files.writeString(outputPath, content);
            
            return outputPath.toString();
        } catch (IOException e) {
            logger.warn("Failed to save rendered content: {}", e.getMessage());
            return null;
        }
    }
    
    private String getFileExtension(Template.TemplateType type) {
        return switch (type) {
            case HTML -> ".html";
            case PDF -> ".pdf";
            case WORD -> ".docx";
            case EXCEL -> ".xlsx";
            case EMAIL -> ".eml";
            default -> ".txt";
        };
    }
    
    // Template Helper Interface
    
    public interface TemplateHelper {
        String execute(String[] args, TemplateContext context);
    }
    
    // Built-in Helper Implementations
    
    private static class DateFormatHelper implements TemplateHelper {
        @Override
        public String execute(String[] args, TemplateContext context) {
            if (args.length < 2) return "";
            
            String dateVar = args[0];
            String format = args[1].replaceAll("['\"]", "");
            
            Object dateValue = context.getVariables().get(dateVar);
            if (dateValue instanceof LocalDateTime dateTime) {
                return dateTime.format(DateTimeFormatter.ofPattern(format));
            }
            
            return "";
        }
    }
    
    private static class CurrentDateHelper implements TemplateHelper {
        @Override
        public String execute(String[] args, TemplateContext context) {
            String format = args.length > 0 ? args[0].replaceAll("['\"]", "") : "yyyy-MM-dd HH:mm:ss";
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern(format));
        }
    }
    
    private static class UppercaseHelper implements TemplateHelper {
        @Override
        public String execute(String[] args, TemplateContext context) {
            if (args.length == 0) return "";
            
            Object value = context.getVariables().get(args[0]);
            return value != null ? value.toString().toUpperCase() : "";
        }
    }
    
    private static class LowercaseHelper implements TemplateHelper {
        @Override
        public String execute(String[] args, TemplateContext context) {
            if (args.length == 0) return "";
            
            Object value = context.getVariables().get(args[0]);
            return value != null ? value.toString().toLowerCase() : "";
        }
    }
    
    private static class CapitalizeHelper implements TemplateHelper {
        @Override
        public String execute(String[] args, TemplateContext context) {
            if (args.length == 0) return "";
            
            Object value = context.getVariables().get(args[0]);
            if (value == null) return "";
            
            String str = value.toString();
            if (str.isEmpty()) return str;
            
            return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
        }
    }
    
    private static class TruncateHelper implements TemplateHelper {
        @Override
        public String execute(String[] args, TemplateContext context) {
            if (args.length < 2) return "";
            
            Object value = context.getVariables().get(args[0]);
            if (value == null) return "";
            
            String str = value.toString();
            int length = Integer.parseInt(args[1]);
            
            if (str.length() <= length) return str;
            
            return str.substring(0, length) + "...";
        }
    }
    
    private static class LengthHelper implements TemplateHelper {
        @Override
        public String execute(String[] args, TemplateContext context) {
            if (args.length == 0) return "0";
            
            Object value = context.getVariables().get(args[0]);
            if (value == null) return "0";
            
            if (value instanceof List) {
                return String.valueOf(((List<?>) value).size());
            } else if (value instanceof Object[]) {
                return String.valueOf(((Object[]) value).length);
            } else {
                return String.valueOf(value.toString().length());
            }
        }
    }
    
    private static class AddHelper implements TemplateHelper {
        @Override
        public String execute(String[] args, TemplateContext context) {
            if (args.length < 2) return "0";
            
            try {
                double a = getNumericValue(args[0], context);
                double b = getNumericValue(args[1], context);
                return String.valueOf(a + b);
            } catch (Exception e) {
                return "0";
            }
        }
        
        private double getNumericValue(String arg, TemplateContext context) {
            try {
                return Double.parseDouble(arg);
            } catch (NumberFormatException e) {
                Object value = context.getVariables().get(arg);
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                return 0.0;
            }
        }
    }
    
    private static class SubtractHelper implements TemplateHelper {
        @Override
        public String execute(String[] args, TemplateContext context) {
            if (args.length < 2) return "0";
            
            try {
                double a = getNumericValue(args[0], context);
                double b = getNumericValue(args[1], context);
                return String.valueOf(a - b);
            } catch (Exception e) {
                return "0";
            }
        }
        
        private double getNumericValue(String arg, TemplateContext context) {
            try {
                return Double.parseDouble(arg);
            } catch (NumberFormatException e) {
                Object value = context.getVariables().get(arg);
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                return 0.0;
            }
        }
    }
    
    private static class MultiplyHelper implements TemplateHelper {
        @Override
        public String execute(String[] args, TemplateContext context) {
            if (args.length < 2) return "0";
            
            try {
                double a = getNumericValue(args[0], context);
                double b = getNumericValue(args[1], context);
                return String.valueOf(a * b);
            } catch (Exception e) {
                return "0";
            }
        }
        
        private double getNumericValue(String arg, TemplateContext context) {
            try {
                return Double.parseDouble(arg);
            } catch (NumberFormatException e) {
                Object value = context.getVariables().get(arg);
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                return 0.0;
            }
        }
    }
    
    private static class DivideHelper implements TemplateHelper {
        @Override
        public String execute(String[] args, TemplateContext context) {
            if (args.length < 2) return "0";
            
            try {
                double a = getNumericValue(args[0], context);
                double b = getNumericValue(args[1], context);
                if (b == 0) return "∞";
                return String.valueOf(a / b);
            } catch (Exception e) {
                return "0";
            }
        }
        
        private double getNumericValue(String arg, TemplateContext context) {
            try {
                return Double.parseDouble(arg);
            } catch (NumberFormatException e) {
                Object value = context.getVariables().get(arg);
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                return 0.0;
            }
        }
    }
    
    private static class RoundHelper implements TemplateHelper {
        @Override
        public String execute(String[] args, TemplateContext context) {
            if (args.length == 0) return "0";
            
            try {
                double value = getNumericValue(args[0], context);
                int places = args.length > 1 ? Integer.parseInt(args[1]) : 0;
                
                double scale = Math.pow(10, places);
                return String.valueOf(Math.round(value * scale) / scale);
            } catch (Exception e) {
                return "0";
            }
        }
        
        private double getNumericValue(String arg, TemplateContext context) {
            try {
                return Double.parseDouble(arg);
            } catch (NumberFormatException e) {
                Object value = context.getVariables().get(arg);
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                return 0.0;
            }
        }
    }
    
    private static class EqualsHelper implements TemplateHelper {
        @Override
        public String execute(String[] args, TemplateContext context) {
            if (args.length < 2) return "false";
            
            Object a = context.getVariables().get(args[0]);
            Object b = context.getVariables().get(args[1]);
            
            return String.valueOf(Objects.equals(a, b));
        }
    }
    
    private static class NotEqualsHelper implements TemplateHelper {
        @Override
        public String execute(String[] args, TemplateContext context) {
            if (args.length < 2) return "true";
            
            Object a = context.getVariables().get(args[0]);
            Object b = context.getVariables().get(args[1]);
            
            return String.valueOf(!Objects.equals(a, b));
        }
    }
    
    private static class GreaterThanHelper implements TemplateHelper {
        @Override
        public String execute(String[] args, TemplateContext context) {
            if (args.length < 2) return "false";
            
            try {
                double a = getNumericValue(args[0], context);
                double b = getNumericValue(args[1], context);
                return String.valueOf(a > b);
            } catch (Exception e) {
                return "false";
            }
        }
        
        private double getNumericValue(String arg, TemplateContext context) {
            try {
                return Double.parseDouble(arg);
            } catch (NumberFormatException e) {
                Object value = context.getVariables().get(arg);
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                return 0.0;
            }
        }
    }
    
    private static class LessThanHelper implements TemplateHelper {
        @Override
        public String execute(String[] args, TemplateContext context) {
            if (args.length < 2) return "false";
            
            try {
                double a = getNumericValue(args[0], context);
                double b = getNumericValue(args[1], context);
                return String.valueOf(a < b);
            } catch (Exception e) {
                return "false";
            }
        }
        
        private double getNumericValue(String arg, TemplateContext context) {
            try {
                return Double.parseDouble(arg);
            } catch (NumberFormatException e) {
                Object value = context.getVariables().get(arg);
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                return 0.0;
            }
        }
    }
    
    private static class FirstHelper implements TemplateHelper {
        @Override
        public String execute(String[] args, TemplateContext context) {
            if (args.length == 0) return "";
            
            Object value = context.getVariables().get(args[0]);
            if (value instanceof List && !((List<?>) value).isEmpty()) {
                return ((List<?>) value).get(0).toString();
            } else if (value instanceof Object[] && ((Object[]) value).length > 0) {
                return ((Object[]) value)[0].toString();
            }
            
            return "";
        }
    }
    
    private static class LastHelper implements TemplateHelper {
        @Override
        public String execute(String[] args, TemplateContext context) {
            if (args.length == 0) return "";
            
            Object value = context.getVariables().get(args[0]);
            if (value instanceof List list && !list.isEmpty()) {
                return list.get(list.size() - 1).toString();
            } else if (value instanceof Object[] array && array.length > 0) {
                return array[array.length - 1].toString();
            }
            
            return "";
        }
    }
    
    private static class JoinHelper implements TemplateHelper {
        @Override
        public String execute(String[] args, TemplateContext context) {
            if (args.length < 2) return "";
            
            Object value = context.getVariables().get(args[0]);
            String separator = args[1].replaceAll("['\"]", "");
            
            if (value instanceof List) {
                List<?> list = (List<?>) value;
                return list.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(separator));
            } else if (value instanceof Object[]) {
                Object[] array = (Object[]) value;
                return Arrays.stream(array)
                    .map(Object::toString)
                    .collect(Collectors.joining(separator));
            }
            
            return "";
        }
    }
    
    // Supporting classes
    
    private static class ThemeManager {
        private final TemplateConfiguration config;
        private final Map<String, Map<String, String>> themes;
        
        public ThemeManager(TemplateConfiguration config) {
            this.config = config;
            this.themes = new HashMap<>();
            initializeDefaultThemes();
        }
        
        private void initializeDefaultThemes() {
            // Default theme
            Map<String, String> defaultTheme = new HashMap<>();
            defaultTheme.put("primaryColor", "#007bff");
            defaultTheme.put("secondaryColor", "#6c757d");
            defaultTheme.put("backgroundColor", "#ffffff");
            defaultTheme.put("textColor", "#333333");
            themes.put("default", defaultTheme);
            
            // Dark theme
            Map<String, String> darkTheme = new HashMap<>();
            darkTheme.put("primaryColor", "#0d6efd");
            darkTheme.put("secondaryColor", "#6c757d");
            darkTheme.put("backgroundColor", "#212529");
            darkTheme.put("textColor", "#ffffff");
            themes.put("dark", darkTheme);
        }
        
        public String applyTheme(String content, String themeName) {
            Map<String, String> theme = themes.get(themeName);
            if (theme == null) {
                theme = themes.get("default");
            }
            
            String result = content;
            for (Map.Entry<String, String> entry : theme.entrySet()) {
                String placeholder = "{{theme:" + entry.getKey() + "}}";
                result = result.replace(placeholder, entry.getValue());
            }
            
            return result;
        }
        
        public Set<String> getAvailableThemes() {
            return themes.keySet();
        }
    }
    
    private static class LocalizationManager {
        private final TemplateConfiguration config;
        private final Map<String, Map<String, String>> messages;
        
        public LocalizationManager(TemplateConfiguration config) {
            this.config = config;
            this.messages = new HashMap<>();
            initializeDefaultMessages();
        }
        
        private void initializeDefaultMessages() {
            // English messages
            Map<String, String> enMessages = new HashMap<>();
            enMessages.put("test.results", "Test Results");
            enMessages.put("total.tests", "Total Tests");
            enMessages.put("passed.tests", "Passed Tests");
            enMessages.put("failed.tests", "Failed Tests");
            enMessages.put("success.rate", "Success Rate");
            messages.put("en_US", enMessages);
            
            // Spanish messages
            Map<String, String> esMessages = new HashMap<>();
            esMessages.put("test.results", "Resultados de Prueba");
            esMessages.put("total.tests", "Total de Pruebas");
            esMessages.put("passed.tests", "Pruebas Exitosas");
            esMessages.put("failed.tests", "Pruebas Fallidas");
            esMessages.put("success.rate", "Tasa de Éxito");
            messages.put("es_ES", esMessages);
        }
        
        public String localize(String content, String locale) {
            Map<String, String> localeMessages = messages.get(locale);
            if (localeMessages == null) {
                localeMessages = messages.get("en_US");
            }
            
            String result = content;
            for (Map.Entry<String, String> entry : localeMessages.entrySet()) {
                String placeholder = "{{i18n:" + entry.getKey() + "}}";
                result = result.replace(placeholder, entry.getValue());
            }
            
            return result;
        }
        
        public Set<String> getAvailableLocales() {
            return messages.keySet();
        }
    }
    
    private static class TemplateValidator {
        private final Pattern INVALID_SYNTAX = Pattern.compile("\\{\\{(?![/#]?\\w+).*?\\}\\}");
        
        public List<String> validateTemplate(Template template) {
            List<String> errors = new ArrayList<>();
            String content = template.getContent();
            
            // Check for unclosed tags
            checkUnclosedTags(content, errors);
            
            // Check for invalid syntax
            checkInvalidSyntax(content, errors);
            
            // Check for undefined variables (basic check)
            checkUndefinedVariables(content, errors);
            
            return errors;
        }
        
        private void checkUnclosedTags(String content, List<String> errors) {
            // Check for unclosed if statements
            long ifCount = content.split("\\{\\{#if").length - 1;
            long endIfCount = content.split("\\{\\{/if\\}\\}").length - 1;
            if (ifCount != endIfCount) {
                errors.add("Unclosed if statement(s): " + (ifCount - endIfCount) + " missing {{/if}}");
            }
            
            // Check for unclosed each statements
            long eachCount = content.split("\\{\\{#each").length - 1;
            long endEachCount = content.split("\\{\\{/each\\}\\}").length - 1;
            if (eachCount != endEachCount) {
                errors.add("Unclosed each statement(s): " + (eachCount - endEachCount) + " missing {{/each}}");
            }
        }
        
        private void checkInvalidSyntax(String content, List<String> errors) {
            Matcher matcher = INVALID_SYNTAX.matcher(content);
            while (matcher.find()) {
                errors.add("Invalid template syntax: " + matcher.group());
            }
        }
        
        private void checkUndefinedVariables(String content, List<String> errors) {
            // This is a basic check - in a real implementation, you'd want to track
            // available variables more comprehensively
            Matcher matcher = VARIABLE_PATTERN.matcher(content);
            Set<String> undefinedVars = new HashSet<>();
            
            while (matcher.find()) {
                String varName = matcher.group(1).trim();
                if (!varName.startsWith("@") && !varName.contains(".") && 
                    !varName.startsWith("\"") && !varName.matches("\\d+") &&
                    !varName.equals("true") && !varName.equals("false")) {
                    undefinedVars.add(varName);
                }
            }
            
            if (!undefinedVars.isEmpty()) {
                errors.add("Potentially undefined variables: " + String.join(", ", undefinedVars));
            }
        }
    }
}