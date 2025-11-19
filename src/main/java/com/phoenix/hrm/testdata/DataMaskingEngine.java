package com.phoenix.hrm.testdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Data Masking Engine for Phoenix HRM Test Automation Framework
 * 
 * Provides comprehensive data masking and anonymization capabilities including:
 * - Sensitive field identification and masking
 * - Multiple masking strategies (redaction, scrambling, tokenization, hashing)
 * - Pattern-based masking for structured data (SSN, credit cards, phones)
 * - Consistent masking for data integrity across related records
 * - Reversible and irreversible masking options
 * - Format-preserving masking for specific data types
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 4.0
 * @since Phase 4
 */
public class DataMaskingEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(DataMaskingEngine.class);
    
    private final TestDataManager.TestDataConfig config;
    private final Map<String, MaskingStrategy> maskingStrategies;
    private final Map<String, String> maskingCache; // For consistent masking
    private final SecureRandom secureRandom;
    
    // Masking patterns
    private static final Pattern SSN_PATTERN = Pattern.compile("\\d{3}-\\d{2}-\\d{4}");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\d{4}-\\d{4}-\\d{4}-\\d{4}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\(\\d{3}\\) \\d{3}-\\d{4}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    
    /**
     * Masking strategy interface
     */
    public interface MaskingStrategy {
        Object mask(Object originalValue, MaskingOptions options);
        String getStrategyName();
        boolean isReversible();
    }
    
    /**
     * Masking options configuration
     */
    public static class MaskingOptions {
        private boolean preserveFormat = true;
        private boolean maintainConsistency = true;
        private String maskingCharacter = "*";
        private int preservePrefix = 0;
        private int preserveSuffix = 0;
        private String salt = null;
        
        // Builder pattern
        public static class Builder {
            private final MaskingOptions options = new MaskingOptions();
            
            public Builder preserveFormat(boolean preserveFormat) {
                options.preserveFormat = preserveFormat;
                return this;
            }
            
            public Builder maintainConsistency(boolean maintainConsistency) {
                options.maintainConsistency = maintainConsistency;
                return this;
            }
            
            public Builder maskingCharacter(String maskingCharacter) {
                options.maskingCharacter = maskingCharacter;
                return this;
            }
            
            public Builder preservePrefix(int preservePrefix) {
                options.preservePrefix = preservePrefix;
                return this;
            }
            
            public Builder preserveSuffix(int preserveSuffix) {
                options.preserveSuffix = preserveSuffix;
                return this;
            }
            
            public Builder salt(String salt) {
                options.salt = salt;
                return this;
            }
            
            public MaskingOptions build() {
                return options;
            }
        }
        
        // Getters
        public boolean isPreserveFormat() { return preserveFormat; }
        public boolean isMaintainConsistency() { return maintainConsistency; }
        public String getMaskingCharacter() { return maskingCharacter; }
        public int getPreservePrefix() { return preservePrefix; }
        public int getPreserveSuffix() { return preserveSuffix; }
        public String getSalt() { return salt; }
    }
    
    /**
     * Constructor
     */
    public DataMaskingEngine(TestDataManager.TestDataConfig config) {
        this.config = config;
        this.maskingStrategies = new HashMap<>();
        this.maskingCache = new HashMap<>();
        this.secureRandom = new SecureRandom();
        
        initializeMaskingStrategies();
    }
    
    /**
     * Apply data masking to a dataset
     */
    public TestDataManager.DataSet maskSensitiveData(TestDataManager.DataSet originalDataSet) {
        if (!config.isEnableDataMasking()) {
            return originalDataSet;
        }
        
        logger.debug("Applying data masking to dataset: {}", originalDataSet.getName());
        
        Map<String, Object> maskedData = new HashMap<>();
        Map<String, Object> originalData = originalDataSet.getData();
        
        for (Map.Entry<String, Object> entry : originalData.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();
            
            if (isSensitiveField(fieldName)) {
                Object maskedValue = maskFieldValue(fieldName, fieldValue);
                maskedData.put(fieldName, maskedValue);
                logger.debug("Masked sensitive field: {}", fieldName);
            } else {
                maskedData.put(fieldName, fieldValue);
            }
        }
        
        return new TestDataManager.DataSet(
            originalDataSet.getName(),
            originalDataSet.getEnvironment(),
            originalDataSet.getVersion(),
            maskedData,
            "DataMaskingEngine"
        );
    }
    
    /**
     * Mask a single field value
     */
    public Object maskFieldValue(String fieldName, Object value) {
        if (value == null) {
            return null;
        }
        
        MaskingStrategy strategy = selectMaskingStrategy(fieldName, value);
        MaskingOptions options = createMaskingOptions(fieldName);
        
        return strategy.mask(value, options);
    }
    
    /**
     * Register custom masking strategy
     */
    public void registerMaskingStrategy(String fieldPattern, MaskingStrategy strategy) {
        maskingStrategies.put(fieldPattern, strategy);
        logger.debug("Registered custom masking strategy for pattern: {}", fieldPattern);
    }
    
    /**
     * Check if field is sensitive
     */
    public boolean isSensitiveField(String fieldName) {
        String lowerFieldName = fieldName.toLowerCase();
        
        // Check configured sensitive fields
        for (String sensitiveField : config.getSensitiveFields()) {
            if (lowerFieldName.contains(sensitiveField.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get masking statistics
     */
    public Map<String, Object> getMaskingStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSensitiveFields", config.getSensitiveFields().size());
        stats.put("registeredStrategies", maskingStrategies.size());
        stats.put("cachedMasks", maskingCache.size());
        stats.put("sensitiveFieldPatterns", new ArrayList<>(config.getSensitiveFields()));
        
        Map<String, Integer> strategyUsage = new HashMap<>();
        for (MaskingStrategy strategy : maskingStrategies.values()) {
            strategyUsage.merge(strategy.getStrategyName(), 1, Integer::sum);
        }
        stats.put("strategyUsage", strategyUsage);
        
        return stats;
    }
    
    /**
     * Clear masking cache
     */
    public void clearCache() {
        maskingCache.clear();
        logger.debug("Cleared masking cache");
    }
    
    // Private helper methods
    
    private void initializeMaskingStrategies() {
        // Register built-in masking strategies
        maskingStrategies.put("password", new RedactionStrategy());
        maskingStrategies.put("ssn", new FormatPreservingStrategy());
        maskingStrategies.put("socialSecurityNumber", new FormatPreservingStrategy());
        maskingStrategies.put("creditCard", new PartialMaskingStrategy());
        maskingStrategies.put("bankAccount", new HashingStrategy());
        maskingStrategies.put("salary", new NumberRangeStrategy());
        maskingStrategies.put("email", new EmailMaskingStrategy());
        maskingStrategies.put("phone", new PhoneMaskingStrategy());
        maskingStrategies.put("address", new AddressMaskingStrategy());
        maskingStrategies.put("default", new ScrambleStrategy());
        
        logger.debug("Initialized {} masking strategies", maskingStrategies.size());
    }
    
    private MaskingStrategy selectMaskingStrategy(String fieldName, Object value) {
        String lowerFieldName = fieldName.toLowerCase();
        
        // Find specific strategy for field
        for (Map.Entry<String, MaskingStrategy> entry : maskingStrategies.entrySet()) {
            if (lowerFieldName.contains(entry.getKey()) && !entry.getKey().equals("default")) {
                return entry.getValue();
            }
        }
        
        // Pattern-based strategy selection
        if (value instanceof String) {
            String stringValue = (String) value;
            
            if (SSN_PATTERN.matcher(stringValue).matches()) {
                return maskingStrategies.get("ssn");
            } else if (CREDIT_CARD_PATTERN.matcher(stringValue).find()) {
                return maskingStrategies.get("creditCard");
            } else if (PHONE_PATTERN.matcher(stringValue).matches()) {
                return maskingStrategies.get("phone");
            } else if (EMAIL_PATTERN.matcher(stringValue).matches()) {
                return maskingStrategies.get("email");
            }
        }
        
        // Default strategy
        return maskingStrategies.get("default");
    }
    
    private MaskingOptions createMaskingOptions(String fieldName) {
        MaskingOptions.Builder builder = new MaskingOptions.Builder();
        
        // Field-specific options
        String lowerFieldName = fieldName.toLowerCase();
        
        if (lowerFieldName.contains("email")) {
            builder.preserveFormat(true).preserveSuffix(10); // Preserve domain
        } else if (lowerFieldName.contains("phone")) {
            builder.preserveFormat(true).preservePrefix(1).preserveSuffix(4);
        } else if (lowerFieldName.contains("ssn")) {
            builder.preserveFormat(true).preserveSuffix(4);
        } else if (lowerFieldName.contains("creditcard")) {
            builder.preserveFormat(true).preserveSuffix(4);
        }
        
        return builder.build();
    }
    
    private String generateConsistentMask(String originalValue, MaskingOptions options) {
        if (!options.isMaintainConsistency()) {
            return generateRandomMask(originalValue, options);
        }
        
        // Check cache for consistent masking
        String cacheKey = originalValue + "_" + options.getMaskingCharacter();
        if (maskingCache.containsKey(cacheKey)) {
            return maskingCache.get(cacheKey);
        }
        
        // Generate new mask and cache it
        String maskedValue = generateRandomMask(originalValue, options);
        maskingCache.put(cacheKey, maskedValue);
        
        return maskedValue;
    }
    
    private String generateRandomMask(String originalValue, MaskingOptions options) {
        if (originalValue == null || originalValue.isEmpty()) {
            return originalValue;
        }
        
        StringBuilder masked = new StringBuilder();
        char maskChar = options.getMaskingCharacter().charAt(0);
        
        int prefixLength = Math.min(options.getPreservePrefix(), originalValue.length());
        int suffixLength = Math.min(options.getPreserveSuffix(), originalValue.length() - prefixLength);
        int maskLength = originalValue.length() - prefixLength - suffixLength;
        
        // Add preserved prefix
        if (prefixLength > 0) {
            masked.append(originalValue, 0, prefixLength);
        }
        
        // Add masked middle
        for (int i = 0; i < maskLength; i++) {
            if (options.isPreserveFormat() && !Character.isLetterOrDigit(originalValue.charAt(prefixLength + i))) {
                // Preserve special characters for format
                masked.append(originalValue.charAt(prefixLength + i));
            } else {
                masked.append(maskChar);
            }
        }
        
        // Add preserved suffix
        if (suffixLength > 0) {
            masked.append(originalValue.substring(originalValue.length() - suffixLength));
        }
        
        return masked.toString();
    }
    
    // Built-in masking strategy implementations
    
    private class RedactionStrategy implements MaskingStrategy {
        @Override
        public Object mask(Object originalValue, MaskingOptions options) {
            return "[REDACTED]";
        }
        
        @Override
        public String getStrategyName() { return "redaction"; }
        
        @Override
        public boolean isReversible() { return false; }
    }
    
    private class FormatPreservingStrategy implements MaskingStrategy {
        @Override
        public Object mask(Object originalValue, MaskingOptions options) {
            if (originalValue instanceof String) {
                return generateConsistentMask((String) originalValue, options);
            }
            return originalValue;
        }
        
        @Override
        public String getStrategyName() { return "format-preserving"; }
        
        @Override
        public boolean isReversible() { return false; }
    }
    
    private class PartialMaskingStrategy implements MaskingStrategy {
        @Override
        public Object mask(Object originalValue, MaskingOptions options) {
            if (originalValue instanceof String) {
                String value = (String) originalValue;
                if (value.length() > 8) {
                    return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
                }
                return generateConsistentMask(value, options);
            }
            return originalValue;
        }
        
        @Override
        public String getStrategyName() { return "partial-masking"; }
        
        @Override
        public boolean isReversible() { return false; }
    }
    
    private class HashingStrategy implements MaskingStrategy {
        @Override
        public Object mask(Object originalValue, MaskingOptions options) {
            if (originalValue instanceof String) {
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    String saltedValue = originalValue + (options.getSalt() != null ? options.getSalt() : "default-salt");
                    byte[] hashedBytes = md.digest(saltedValue.getBytes());
                    
                    StringBuilder sb = new StringBuilder();
                    for (byte b : hashedBytes) {
                        sb.append(String.format("%02x", b));
                    }
                    
                    // Return first 16 characters to maintain readability
                    return "HASH_" + sb.substring(0, 16).toUpperCase();
                    
                } catch (NoSuchAlgorithmException e) {
                    logger.warn("SHA-256 algorithm not available, falling back to scrambling", e);
                    return new ScrambleStrategy().mask(originalValue, options);
                }
            }
            return originalValue;
        }
        
        @Override
        public String getStrategyName() { return "hashing"; }
        
        @Override
        public boolean isReversible() { return false; }
    }
    
    private class NumberRangeStrategy implements MaskingStrategy {
        @Override
        public Object mask(Object originalValue, MaskingOptions options) {
            if (originalValue instanceof Number) {
                double original = ((Number) originalValue).doubleValue();
                // Create a range around the original value
                double min = original * 0.8;
                double max = original * 1.2;
                return min + (max - min) * secureRandom.nextDouble();
            }
            return originalValue;
        }
        
        @Override
        public String getStrategyName() { return "number-range"; }
        
        @Override
        public boolean isReversible() { return false; }
    }
    
    private class EmailMaskingStrategy implements MaskingStrategy {
        @Override
        public Object mask(Object originalValue, MaskingOptions options) {
            if (originalValue instanceof String) {
                String email = (String) originalValue;
                int atIndex = email.indexOf('@');
                
                if (atIndex > 0) {
                    String localPart = email.substring(0, atIndex);
                    String domainPart = email.substring(atIndex);
                    
                    if (localPart.length() > 2) {
                        String maskedLocal = localPart.charAt(0) + 
                                           "*".repeat(localPart.length() - 2) + 
                                           localPart.charAt(localPart.length() - 1);
                        return maskedLocal + domainPart;
                    }
                }
                
                return generateConsistentMask(email, options);
            }
            return originalValue;
        }
        
        @Override
        public String getStrategyName() { return "email-masking"; }
        
        @Override
        public boolean isReversible() { return false; }
    }
    
    private class PhoneMaskingStrategy implements MaskingStrategy {
        @Override
        public Object mask(Object originalValue, MaskingOptions options) {
            if (originalValue instanceof String) {
                String phone = (String) originalValue;
                if (PHONE_PATTERN.matcher(phone).matches()) {
                    // Mask middle digits: (###) ***-####
                    return phone.substring(0, 6) + "***" + phone.substring(9);
                }
                return generateConsistentMask(phone, options);
            }
            return originalValue;
        }
        
        @Override
        public String getStrategyName() { return "phone-masking"; }
        
        @Override
        public boolean isReversible() { return false; }
    }
    
    private class AddressMaskingStrategy implements MaskingStrategy {
        @Override
        @SuppressWarnings("unchecked")
        public Object mask(Object originalValue, MaskingOptions options) {
            if (originalValue instanceof Map) {
                Map<String, Object> address = new HashMap<>((Map<String, Object>) originalValue);
                
                // Mask street address but preserve city and state
                if (address.containsKey("street")) {
                    String street = (String) address.get("street");
                    address.put("street", "*** " + street.substring(street.lastIndexOf(' ') + 1));
                }
                
                // Partially mask zip code
                if (address.containsKey("zipCode")) {
                    String zip = (String) address.get("zipCode");
                    if (zip.length() == 5) {
                        address.put("zipCode", zip.substring(0, 3) + "**");
                    }
                }
                
                return address;
            }
            return originalValue;
        }
        
        @Override
        public String getStrategyName() { return "address-masking"; }
        
        @Override
        public boolean isReversible() { return false; }
    }
    
    private class ScrambleStrategy implements MaskingStrategy {
        @Override
        public Object mask(Object originalValue, MaskingOptions options) {
            if (originalValue instanceof String) {
                String value = (String) originalValue;
                List<Character> chars = new ArrayList<>();
                
                for (char c : value.toCharArray()) {
                    chars.add(c);
                }
                
                Collections.shuffle(chars, secureRandom);
                
                StringBuilder scrambled = new StringBuilder();
                for (char c : chars) {
                    scrambled.append(c);
                }
                
                return scrambled.toString();
            }
            return originalValue;
        }
        
        @Override
        public String getStrategyName() { return "scramble"; }
        
        @Override
        public boolean isReversible() { return false; }
    }
}