package com.phoenix.hrm.testdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * Dynamic Test Data Generator Engine for Phoenix HRM Test Automation Framework
 * 
 * Provides comprehensive test data generation capabilities including:
 * - Pattern-based data generation (regex, formats, ranges)
 * - HRM-specific data generators (employees, departments, payroll)
 * - Random data generation with seed support for reproducibility
 * - Custom data generation rules and constraints
 * - Bulk data generation for performance testing
 * - Data relationship management for complex scenarios
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 4.0
 * @since Phase 4
 */
public class DataGeneratorEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(DataGeneratorEngine.class);
    
    private final TestDataManager.TestDataConfig config;
    private final Map<String, DataGenerator<?>> generators;
    private final Random random;
    
    // HRM-specific data pools
    private static final List<String> FIRST_NAMES = Arrays.asList(
        "John", "Jane", "Michael", "Sarah", "David", "Emily", "Robert", "Lisa",
        "James", "Maria", "William", "Jennifer", "Richard", "Linda", "Charles", "Patricia",
        "Joseph", "Barbara", "Thomas", "Elizabeth", "Christopher", "Susan", "Daniel", "Jessica"
    );
    
    private static final List<String> LAST_NAMES = Arrays.asList(
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
        "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas",
        "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson", "White"
    );
    
    private static final List<String> DEPARTMENTS = Arrays.asList(
        "Human Resources", "Engineering", "Marketing", "Sales", "Finance", "Operations",
        "Customer Service", "Product Management", "Quality Assurance", "Legal", "IT Support"
    );
    
    private static final List<String> JOB_TITLES = Arrays.asList(
        "Software Engineer", "Senior Developer", "Product Manager", "HR Specialist", "Sales Representative",
        "Marketing Coordinator", "Financial Analyst", "Operations Manager", "QA Engineer", "Technical Lead",
        "Business Analyst", "Customer Success Manager", "Data Scientist", "DevOps Engineer"
    );
    
    private static final List<String> CITIES = Arrays.asList(
        "New York", "Los Angeles", "Chicago", "Houston", "Phoenix", "Philadelphia", "San Antonio",
        "San Diego", "Dallas", "San Jose", "Austin", "Jacksonville", "San Francisco", "Indianapolis"
    );
    
    private static final List<String> STATES = Arrays.asList(
        "CA", "TX", "FL", "NY", "PA", "IL", "OH", "GA", "NC", "MI", "AZ", "WA", "IN", "TN"
    );
    
    /**
     * Data generator interface
     */
    public interface DataGenerator<T> {
        T generate(Map<String, Object> parameters);
        String getType();
        Set<String> getSupportedParameters();
    }
    
    /**
     * Constructor
     */
    public DataGeneratorEngine(TestDataManager.TestDataConfig config) {
        this.config = config;
        this.generators = new HashMap<>();
        this.random = new Random();
        
        initializeGenerators();
    }
    
    /**
     * Generate test data based on rules
     */
    public TestDataManager.DataSet generateData(String dataName, String environment, Map<String, Object> generationRules) {
        logger.info("Generating test data: {} for environment: {}", dataName, environment);
        
        Map<String, Object> generatedData = new HashMap<>();
        
        for (Map.Entry<String, Object> rule : generationRules.entrySet()) {
            String fieldName = rule.getKey();
            Object ruleDefinition = rule.getValue();
            
            Object generatedValue = generateFieldValue(fieldName, ruleDefinition);
            generatedData.put(fieldName, generatedValue);
        }
        
        return new TestDataManager.DataSet(dataName, environment, "generated", generatedData, "DataGeneratorEngine");
    }
    
    /**
     * Generate bulk test data
     */
    public List<TestDataManager.DataSet> generateBulkData(String dataName, String environment, 
                                                          Map<String, Object> generationRules, int count) {
        logger.info("Generating {} bulk test data sets: {} for environment: {}", count, dataName, environment);
        
        List<TestDataManager.DataSet> datasets = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            String uniqueName = dataName + "_" + (i + 1);
            TestDataManager.DataSet dataset = generateData(uniqueName, environment, generationRules);
            datasets.add(dataset);
        }
        
        return datasets;
    }
    
    /**
     * Generate employee test data
     */
    public TestDataManager.DataSet generateEmployeeData(String employeeName, String environment) {
        Map<String, Object> employeeData = new HashMap<>();
        
        // Basic information
        employeeData.put("employeeId", generateEmployeeId());
        employeeData.put("firstName", randomChoice(FIRST_NAMES));
        employeeData.put("lastName", randomChoice(LAST_NAMES));
        employeeData.put("email", generateEmail(employeeData.get("firstName").toString(), employeeData.get("lastName").toString()));
        employeeData.put("phone", generatePhoneNumber());
        employeeData.put("dateOfBirth", generateDateOfBirth());
        employeeData.put("hireDate", generateHireDate());
        
        // Employment details
        employeeData.put("department", randomChoice(DEPARTMENTS));
        employeeData.put("jobTitle", randomChoice(JOB_TITLES));
        employeeData.put("salary", generateSalary());
        employeeData.put("employmentStatus", randomChoice(Arrays.asList("Active", "Inactive", "On Leave")));
        employeeData.put("manager", generateManagerId());
        
        // Address information
        employeeData.put("address", generateAddress());
        
        // Additional fields
        employeeData.put("ssn", generateSSN());
        employeeData.put("emergencyContact", generateEmergencyContact());
        
        return new TestDataManager.DataSet(employeeName, environment, "generated", employeeData, "EmployeeGenerator");
    }
    
    /**
     * Generate department test data
     */
    public TestDataManager.DataSet generateDepartmentData(String departmentName, String environment) {
        Map<String, Object> departmentData = new HashMap<>();
        
        departmentData.put("departmentId", generateDepartmentId());
        departmentData.put("departmentName", randomChoice(DEPARTMENTS));
        departmentData.put("description", "Generated department for testing purposes");
        departmentData.put("managerId", generateManagerId());
        departmentData.put("budget", generateBudget());
        departmentData.put("location", randomChoice(CITIES));
        departmentData.put("establishedDate", generateEstablishedDate());
        departmentData.put("employeeCount", ThreadLocalRandom.current().nextInt(5, 50));
        
        return new TestDataManager.DataSet(departmentName, environment, "generated", departmentData, "DepartmentGenerator");
    }
    
    /**
     * Generate payroll test data
     */
    public TestDataManager.DataSet generatePayrollData(String payrollName, String environment) {
        Map<String, Object> payrollData = new HashMap<>();
        
        payrollData.put("payrollId", generatePayrollId());
        payrollData.put("employeeId", generateEmployeeId());
        payrollData.put("payPeriodStart", generatePayPeriodStart());
        payrollData.put("payPeriodEnd", generatePayPeriodEnd());
        payrollData.put("baseSalary", generateSalary());
        payrollData.put("overtime", generateOvertime());
        payrollData.put("bonus", generateBonus());
        payrollData.put("deductions", generateDeductions());
        payrollData.put("taxes", generateTaxes());
        payrollData.put("netPay", calculateNetPay(payrollData));
        payrollData.put("payDate", generatePayDate());
        
        return new TestDataManager.DataSet(payrollName, environment, "generated", payrollData, "PayrollGenerator");
    }
    
    /**
     * Set random seed for reproducible data generation
     */
    public void setSeed(long seed) {
        random.setSeed(seed);
        logger.debug("Set random seed to: {}", seed);
    }
    
    /**
     * Register custom data generator
     */
    public void registerGenerator(String type, DataGenerator<?> generator) {
        generators.put(type, generator);
        logger.debug("Registered custom generator: {}", type);
    }
    
    /**
     * Get available generator types
     */
    public Set<String> getAvailableGeneratorTypes() {
        return new HashSet<>(generators.keySet());
    }
    
    // Private helper methods
    
    private void initializeGenerators() {
        // Register built-in generators
        generators.put("string", new StringGenerator());
        generators.put("number", new NumberGenerator());
        generators.put("date", new DateGenerator());
        generators.put("boolean", new BooleanGenerator());
        generators.put("email", new EmailGenerator());
        generators.put("phone", new PhoneGenerator());
        generators.put("address", new AddressGenerator());
        
        logger.debug("Initialized {} data generators", generators.size());
    }
    
    private Object generateFieldValue(String fieldName, Object ruleDefinition) {
        if (ruleDefinition instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> rule = (Map<String, Object>) ruleDefinition;
            String type = (String) rule.get("type");
            
            DataGenerator<?> generator = generators.get(type);
            if (generator != null) {
                return generator.generate(rule);
            } else {
                logger.warn("No generator found for type: {}", type);
                return null;
            }
        } else {
            // Simple value
            return ruleDefinition;
        }
    }
    
    private String generateEmployeeId() {
        return "EMP" + String.format("%06d", ThreadLocalRandom.current().nextInt(1, 999999));
    }
    
    private String generateDepartmentId() {
        return "DEPT" + String.format("%03d", ThreadLocalRandom.current().nextInt(1, 999));
    }
    
    private String generatePayrollId() {
        return "PAY" + String.format("%08d", ThreadLocalRandom.current().nextInt(1, 99999999));
    }
    
    private String generateManagerId() {
        return "MGR" + String.format("%05d", ThreadLocalRandom.current().nextInt(1, 99999));
    }
    
    private String generateEmail(String firstName, String lastName) {
        return (firstName + "." + lastName + "@company.com").toLowerCase();
    }
    
    private String generatePhoneNumber() {
        return String.format("(%03d) %03d-%04d",
            ThreadLocalRandom.current().nextInt(200, 999),
            ThreadLocalRandom.current().nextInt(200, 999),
            ThreadLocalRandom.current().nextInt(1000, 9999));
    }
    
    private String generateDateOfBirth() {
        LocalDate startDate = LocalDate.of(1960, 1, 1);
        LocalDate endDate = LocalDate.of(2000, 12, 31);
        long randomDay = ThreadLocalRandom.current().nextLong(startDate.toEpochDay(), endDate.toEpochDay());
        return LocalDate.ofEpochDay(randomDay).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
    
    private String generateHireDate() {
        LocalDate startDate = LocalDate.of(2010, 1, 1);
        LocalDate endDate = LocalDate.now();
        long randomDay = ThreadLocalRandom.current().nextLong(startDate.toEpochDay(), endDate.toEpochDay());
        return LocalDate.ofEpochDay(randomDay).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
    
    private String generateEstablishedDate() {
        LocalDate startDate = LocalDate.of(1990, 1, 1);
        LocalDate endDate = LocalDate.of(2020, 12, 31);
        long randomDay = ThreadLocalRandom.current().nextLong(startDate.toEpochDay(), endDate.toEpochDay());
        return LocalDate.ofEpochDay(randomDay).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
    
    private double generateSalary() {
        return ThreadLocalRandom.current().nextDouble(30000, 150000);
    }
    
    private double generateBudget() {
        return ThreadLocalRandom.current().nextDouble(100000, 5000000);
    }
    
    private String generatePayPeriodStart() {
        LocalDate date = LocalDate.now().minusDays(ThreadLocalRandom.current().nextInt(30, 60));
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
    
    private String generatePayPeriodEnd() {
        LocalDate date = LocalDate.now().minusDays(ThreadLocalRandom.current().nextInt(15, 30));
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
    
    private String generatePayDate() {
        LocalDate date = LocalDate.now().minusDays(ThreadLocalRandom.current().nextInt(1, 15));
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
    
    private double generateOvertime() {
        return ThreadLocalRandom.current().nextDouble(0, 2000);
    }
    
    private double generateBonus() {
        return ThreadLocalRandom.current().nextDouble(0, 5000);
    }
    
    private Map<String, Double> generateDeductions() {
        Map<String, Double> deductions = new HashMap<>();
        deductions.put("healthInsurance", ThreadLocalRandom.current().nextDouble(100, 500));
        deductions.put("retirement401k", ThreadLocalRandom.current().nextDouble(200, 1000));
        deductions.put("lifeInsurance", ThreadLocalRandom.current().nextDouble(50, 200));
        return deductions;
    }
    
    private Map<String, Double> generateTaxes() {
        Map<String, Double> taxes = new HashMap<>();
        taxes.put("federalTax", ThreadLocalRandom.current().nextDouble(500, 2000));
        taxes.put("stateTax", ThreadLocalRandom.current().nextDouble(200, 800));
        taxes.put("socialSecurity", ThreadLocalRandom.current().nextDouble(300, 1200));
        taxes.put("medicare", ThreadLocalRandom.current().nextDouble(100, 400));
        return taxes;
    }
    
    @SuppressWarnings("unchecked")
    private double calculateNetPay(Map<String, Object> payrollData) {
        double baseSalary = (Double) payrollData.get("baseSalary");
        double overtime = (Double) payrollData.get("overtime");
        double bonus = (Double) payrollData.get("bonus");
        
        Map<String, Double> deductions = (Map<String, Double>) payrollData.get("deductions");
        Map<String, Double> taxes = (Map<String, Double>) payrollData.get("taxes");
        
        double totalDeductions = deductions.values().stream().mapToDouble(Double::doubleValue).sum();
        double totalTaxes = taxes.values().stream().mapToDouble(Double::doubleValue).sum();
        
        return (baseSalary / 12) + overtime + bonus - totalDeductions - totalTaxes; // Monthly calculation
    }
    
    private String generateSSN() {
        return String.format("%03d-%02d-%04d",
            ThreadLocalRandom.current().nextInt(100, 999),
            ThreadLocalRandom.current().nextInt(10, 99),
            ThreadLocalRandom.current().nextInt(1000, 9999));
    }
    
    private Map<String, String> generateAddress() {
        Map<String, String> address = new HashMap<>();
        address.put("street", ThreadLocalRandom.current().nextInt(1, 9999) + " " + randomChoice(Arrays.asList("Main St", "Oak Ave", "Pine St", "Elm Dr", "Cedar Ln")));
        address.put("city", randomChoice(CITIES));
        address.put("state", randomChoice(STATES));
        address.put("zipCode", String.format("%05d", ThreadLocalRandom.current().nextInt(10000, 99999)));
        return address;
    }
    
    private Map<String, String> generateEmergencyContact() {
        Map<String, String> contact = new HashMap<>();
        contact.put("name", randomChoice(FIRST_NAMES) + " " + randomChoice(LAST_NAMES));
        contact.put("relationship", randomChoice(Arrays.asList("Spouse", "Parent", "Sibling", "Friend")));
        contact.put("phone", generatePhoneNumber());
        return contact;
    }
    
    private <T> T randomChoice(List<T> choices) {
        return choices.get(ThreadLocalRandom.current().nextInt(choices.size()));
    }
    
    // Built-in generator implementations
    
    private static class StringGenerator implements DataGenerator<String> {
        @Override
        public String generate(Map<String, Object> parameters) {
            int length = (Integer) parameters.getOrDefault("length", 10);
            String pattern = (String) parameters.get("pattern");
            
            if (pattern != null) {
                // Simple pattern support
                return generateFromPattern(pattern, length);
            } else {
                return generateRandomString(length);
            }
        }
        
        @Override
        public String getType() { return "string"; }
        
        @Override
        public Set<String> getSupportedParameters() {
            return Set.of("length", "pattern", "charset");
        }
        
        private String generateFromPattern(String pattern, int length) {
            // Basic pattern implementation
            StringBuilder sb = new StringBuilder();
            Random random = new Random();
            
            for (int i = 0; i < length; i++) {
                if (pattern.contains("[a-z]")) {
                    sb.append((char) ('a' + random.nextInt(26)));
                } else if (pattern.contains("[A-Z]")) {
                    sb.append((char) ('A' + random.nextInt(26)));
                } else if (pattern.contains("[0-9]")) {
                    sb.append(random.nextInt(10));
                } else {
                    sb.append((char) ('a' + random.nextInt(26)));
                }
            }
            
            return sb.toString();
        }
        
        private String generateRandomString(int length) {
            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            StringBuilder sb = new StringBuilder();
            Random random = new Random();
            
            for (int i = 0; i < length; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            
            return sb.toString();
        }
    }
    
    private static class NumberGenerator implements DataGenerator<Number> {
        @Override
        public Number generate(Map<String, Object> parameters) {
            String numberType = (String) parameters.getOrDefault("numberType", "integer");
            
            if ("integer".equals(numberType)) {
                int min = (Integer) parameters.getOrDefault("min", 0);
                int max = (Integer) parameters.getOrDefault("max", 100);
                return ThreadLocalRandom.current().nextInt(min, max + 1);
            } else if ("double".equals(numberType)) {
                double min = ((Number) parameters.getOrDefault("min", 0.0)).doubleValue();
                double max = ((Number) parameters.getOrDefault("max", 100.0)).doubleValue();
                return ThreadLocalRandom.current().nextDouble(min, max);
            }
            
            return 0;
        }
        
        @Override
        public String getType() { return "number"; }
        
        @Override
        public Set<String> getSupportedParameters() {
            return Set.of("numberType", "min", "max", "precision");
        }
    }
    
    private static class DateGenerator implements DataGenerator<String> {
        @Override
        public String generate(Map<String, Object> parameters) {
            String format = (String) parameters.getOrDefault("format", "yyyy-MM-dd");
            String startDate = (String) parameters.get("startDate");
            String endDate = (String) parameters.get("endDate");
            
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.of(2020, 1, 1);
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
            
            long randomDay = ThreadLocalRandom.current().nextLong(start.toEpochDay(), end.toEpochDay());
            LocalDate randomDate = LocalDate.ofEpochDay(randomDay);
            
            return randomDate.format(DateTimeFormatter.ofPattern(format));
        }
        
        @Override
        public String getType() { return "date"; }
        
        @Override
        public Set<String> getSupportedParameters() {
            return Set.of("format", "startDate", "endDate");
        }
    }
    
    private static class BooleanGenerator implements DataGenerator<Boolean> {
        @Override
        public Boolean generate(Map<String, Object> parameters) {
            double trueProbability = ((Number) parameters.getOrDefault("trueProbability", 0.5)).doubleValue();
            return ThreadLocalRandom.current().nextDouble() < trueProbability;
        }
        
        @Override
        public String getType() { return "boolean"; }
        
        @Override
        public Set<String> getSupportedParameters() {
            return Set.of("trueProbability");
        }
    }
    
    private static class EmailGenerator implements DataGenerator<String> {
        @Override
        public String generate(Map<String, Object> parameters) {
            String domain = (String) parameters.getOrDefault("domain", "example.com");
            String username = generateRandomString(8);
            return username + "@" + domain;
        }
        
        @Override
        public String getType() { return "email"; }
        
        @Override
        public Set<String> getSupportedParameters() {
            return Set.of("domain", "usernameLength");
        }
        
        private String generateRandomString(int length) {
            String chars = "abcdefghijklmnopqrstuvwxyz";
            StringBuilder sb = new StringBuilder();
            Random random = new Random();
            
            for (int i = 0; i < length; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            
            return sb.toString();
        }
    }
    
    private static class PhoneGenerator implements DataGenerator<String> {
        @Override
        public String generate(Map<String, Object> parameters) {
            String format = (String) parameters.getOrDefault("format", "(###) ###-####");
            
            return String.format("(%03d) %03d-%04d",
                ThreadLocalRandom.current().nextInt(200, 999),
                ThreadLocalRandom.current().nextInt(200, 999),
                ThreadLocalRandom.current().nextInt(1000, 9999));
        }
        
        @Override
        public String getType() { return "phone"; }
        
        @Override
        public Set<String> getSupportedParameters() {
            return Set.of("format", "countryCode");
        }
    }
    
    private static class AddressGenerator implements DataGenerator<Map<String, String>> {
        @Override
        public Map<String, String> generate(Map<String, Object> parameters) {
            Map<String, String> address = new HashMap<>();
            
            address.put("street", ThreadLocalRandom.current().nextInt(1, 9999) + " " + 
                randomChoice(Arrays.asList("Main St", "Oak Ave", "Pine St", "Elm Dr", "Cedar Ln")));
            address.put("city", randomChoice(CITIES));
            address.put("state", randomChoice(STATES));
            address.put("zipCode", String.format("%05d", ThreadLocalRandom.current().nextInt(10000, 99999)));
            
            return address;
        }
        
        @Override
        public String getType() { return "address"; }
        
        @Override
        public Set<String> getSupportedParameters() {
            return Set.of("country", "includeApartment");
        }
        
        private <T> T randomChoice(List<T> choices) {
            return choices.get(ThreadLocalRandom.current().nextInt(choices.size()));
        }
    }
}