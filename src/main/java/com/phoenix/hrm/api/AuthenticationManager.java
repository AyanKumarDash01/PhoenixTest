package com.phoenix.hrm.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authentication Manager for API Testing Framework
 * 
 * Provides comprehensive authentication and authorization management including:
 * - Multiple authentication schemes (Bearer, Basic, API Key, OAuth2)
 * - Token management with automatic refresh
 * - Session management and persistence
 * - Multi-user authentication support
 * - Role-based access control validation
 * - Authentication state tracking and reporting
 * - Secure credential storage and handling
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 5.0
 * @since Phase 5
 */
public class AuthenticationManager {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationManager.class);
    
    private final ApiTestFramework.ApiConfiguration config;
    private final ObjectMapper objectMapper;
    private final Map<String, AuthenticationContext> userContexts;
    private final Map<String, TokenInfo> tokenCache;
    private volatile String currentToken;
    private volatile String currentUser;
    private volatile AuthenticationScheme currentScheme;
    
    /**
     * Authentication schemes supported
     */
    public enum AuthenticationScheme {
        BEARER("Bearer"),
        BASIC("Basic"),
        API_KEY("ApiKey"),
        OAUTH2("OAuth2"),
        CUSTOM("Custom");
        
        private final String headerPrefix;
        
        AuthenticationScheme(String headerPrefix) {
            this.headerPrefix = headerPrefix;
        }
        
        public String getHeaderPrefix() {
            return headerPrefix;
        }
    }
    
    /**
     * Authentication context for a user
     */
    public static class AuthenticationContext {
        private final String username;
        private final AuthenticationScheme scheme;
        private String token;
        private String refreshToken;
        private LocalDateTime tokenExpiry;
        private LocalDateTime lastActivity;
        private Map<String, String> additionalHeaders;
        private Set<String> roles;
        private Map<String, Object> metadata;
        private boolean active;
        
        public AuthenticationContext(String username, AuthenticationScheme scheme) {
            this.username = username;
            this.scheme = scheme;
            this.additionalHeaders = new HashMap<>();
            this.roles = new HashSet<>();
            this.metadata = new HashMap<>();
            this.active = true;
            this.lastActivity = LocalDateTime.now();
        }
        
        // Getters and setters
        public String getUsername() { return username; }
        public AuthenticationScheme getScheme() { return scheme; }
        public String getToken() { return token; }
        public void setToken(String token) { 
            this.token = token; 
            this.lastActivity = LocalDateTime.now();
        }
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        public LocalDateTime getTokenExpiry() { return tokenExpiry; }
        public void setTokenExpiry(LocalDateTime tokenExpiry) { this.tokenExpiry = tokenExpiry; }
        public LocalDateTime getLastActivity() { return lastActivity; }
        public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }
        public Map<String, String> getAdditionalHeaders() { return additionalHeaders; }
        public Set<String> getRoles() { return roles; }
        public Map<String, Object> getMetadata() { return metadata; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        
        public boolean isTokenExpired() {
            return tokenExpiry != null && LocalDateTime.now().isAfter(tokenExpiry);
        }
        
        public boolean hasRole(String role) {
            return roles.contains(role);
        }
        
        public void addRole(String role) {
            roles.add(role);
        }
        
        public void removeRole(String role) {
            roles.remove(role);
        }
        
        @Override
        public String toString() {
            return String.format("AuthenticationContext{username='%s', scheme=%s, active=%b, roles=%s}", 
                username, scheme, active, roles);
        }
    }
    
    /**
     * Token information
     */
    public static class TokenInfo {
        private final String token;
        private final String tokenType;
        private final LocalDateTime issuedAt;
        private final LocalDateTime expiresAt;
        private final Set<String> scopes;
        private final Map<String, Object> claims;
        
        public TokenInfo(String token, String tokenType, LocalDateTime expiresAt) {
            this.token = token;
            this.tokenType = tokenType;
            this.issuedAt = LocalDateTime.now();
            this.expiresAt = expiresAt;
            this.scopes = new HashSet<>();
            this.claims = new HashMap<>();
        }
        
        // Getters
        public String getToken() { return token; }
        public String getTokenType() { return tokenType; }
        public LocalDateTime getIssuedAt() { return issuedAt; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public Set<String> getScopes() { return scopes; }
        public Map<String, Object> getClaims() { return claims; }
        
        public boolean isExpired() {
            return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
        }
        
        public boolean hasScope(String scope) {
            return scopes.contains(scope);
        }
        
        @Override
        public String toString() {
            return String.format("TokenInfo{type='%s', issuedAt=%s, expiresAt=%s, scopes=%s}", 
                tokenType, issuedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), 
                expiresAt != null ? expiresAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "never", 
                scopes);
        }
    }
    
    /**
     * Authentication result
     */
    public static class AuthenticationResult {
        private final boolean success;
        private final String message;
        private final String token;
        private final LocalDateTime expiry;
        private final Map<String, Object> userInfo;
        
        public AuthenticationResult(boolean success, String message, String token, 
                                  LocalDateTime expiry, Map<String, Object> userInfo) {
            this.success = success;
            this.message = message;
            this.token = token;
            this.expiry = expiry;
            this.userInfo = userInfo != null ? userInfo : new HashMap<>();
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getToken() { return token; }
        public LocalDateTime getExpiry() { return expiry; }
        public Map<String, Object> getUserInfo() { return userInfo; }
        
        @Override
        public String toString() {
            return String.format("AuthenticationResult{success=%b, message='%s'}", success, message);
        }
    }
    
    /**
     * Constructor
     */
    public AuthenticationManager(ApiTestFramework.ApiConfiguration config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.userContexts = new ConcurrentHashMap<>();
        this.tokenCache = new ConcurrentHashMap<>();
        this.currentScheme = AuthenticationScheme.valueOf(config.getAuthenticationScheme().toUpperCase());
        
        logger.debug("AuthenticationManager initialized with scheme: {}", currentScheme);
    }
    
    /**
     * Set authentication token for current session
     */
    public void setToken(String token) {
        this.currentToken = token;
        logger.debug("Authentication token set for current session");
    }
    
    /**
     * Clear authentication token
     */
    public void clearToken() {
        this.currentToken = null;
        this.currentUser = null;
        logger.debug("Authentication token cleared");
    }
    
    /**
     * Get current authentication token
     */
    public String getToken() {
        return currentToken;
    }
    
    /**
     * Check if authentication token is available
     */
    public boolean hasToken() {
        return currentToken != null && !currentToken.isEmpty();
    }
    
    /**
     * Authenticate user with username and password
     */
    public AuthenticationResult authenticate(String username, String password) {
        return authenticate(username, password, currentScheme);
    }
    
    /**
     * Authenticate user with specific scheme
     */
    public AuthenticationResult authenticate(String username, String password, AuthenticationScheme scheme) {
        try {
            logger.info("Attempting authentication for user: {} with scheme: {}", username, scheme);
            
            // Simulate authentication process (in real implementation, this would call actual API)
            AuthenticationResult result = performAuthentication(username, password, scheme);
            
            if (result.isSuccess()) {
                // Create authentication context
                AuthenticationContext context = new AuthenticationContext(username, scheme);
                context.setToken(result.getToken());
                context.setTokenExpiry(result.getExpiry());
                
                // Extract roles and metadata from user info
                extractUserInfoIntoContext(result.getUserInfo(), context);
                
                // Store context
                userContexts.put(username, context);
                
                // Set as current user
                this.currentUser = username;
                this.currentToken = result.getToken();
                this.currentScheme = scheme;
                
                // Cache token info
                if (result.getToken() != null) {
                    TokenInfo tokenInfo = new TokenInfo(result.getToken(), scheme.name(), result.getExpiry());
                    tokenCache.put(result.getToken(), tokenInfo);
                }
                
                logger.info("Authentication successful for user: {}", username);
            } else {
                logger.warn("Authentication failed for user: {} - {}", username, result.getMessage());
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Authentication error for user {}: {}", username, e.getMessage());
            return new AuthenticationResult(false, "Authentication error: " + e.getMessage(), null, null, null);
        }
    }
    
    /**
     * Refresh authentication token
     */
    public AuthenticationResult refreshToken(String refreshToken) {
        try {
            logger.debug("Attempting token refresh");
            
            // Find user context with this refresh token
            AuthenticationContext context = findContextByRefreshToken(refreshToken);
            if (context == null) {
                return new AuthenticationResult(false, "Invalid refresh token", null, null, null);
            }
            
            // Simulate token refresh (in real implementation, this would call actual API)
            AuthenticationResult result = performTokenRefresh(refreshToken);
            
            if (result.isSuccess()) {
                // Update context with new token
                context.setToken(result.getToken());
                context.setTokenExpiry(result.getExpiry());
                
                // Update current token if this is the current user
                if (context.getUsername().equals(currentUser)) {
                    this.currentToken = result.getToken();
                }
                
                // Update token cache
                TokenInfo tokenInfo = new TokenInfo(result.getToken(), context.getScheme().name(), result.getExpiry());
                tokenCache.put(result.getToken(), tokenInfo);
                
                logger.info("Token refresh successful for user: {}", context.getUsername());
            } else {
                logger.warn("Token refresh failed: {}", result.getMessage());
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Token refresh error: {}", e.getMessage());
            return new AuthenticationResult(false, "Token refresh error: " + e.getMessage(), null, null, null);
        }
    }
    
    /**
     * Logout current user
     */
    public void logout() {
        if (currentUser != null) {
            AuthenticationContext context = userContexts.get(currentUser);
            if (context != null) {
                context.setActive(false);
                
                // Remove from token cache
                if (context.getToken() != null) {
                    tokenCache.remove(context.getToken());
                }
                
                logger.info("User logged out: {}", currentUser);
            }
            
            this.currentUser = null;
            this.currentToken = null;
        }
    }
    
    /**
     * Logout specific user
     */
    public void logout(String username) {
        AuthenticationContext context = userContexts.get(username);
        if (context != null) {
            context.setActive(false);
            
            // Remove from token cache
            if (context.getToken() != null) {
                tokenCache.remove(context.getToken());
            }
            
            // Clear current session if this is the current user
            if (username.equals(currentUser)) {
                this.currentUser = null;
                this.currentToken = null;
            }
            
            logger.info("User logged out: {}", username);
        }
    }
    
    /**
     * Check if user has required role
     */
    public boolean hasRole(String role) {
        return hasRole(currentUser, role);
    }
    
    /**
     * Check if specific user has required role
     */
    public boolean hasRole(String username, String role) {
        if (username == null) return false;
        
        AuthenticationContext context = userContexts.get(username);
        return context != null && context.hasRole(role);
    }
    
    /**
     * Get current user authentication context
     */
    public AuthenticationContext getCurrentUserContext() {
        return currentUser != null ? userContexts.get(currentUser) : null;
    }
    
    /**
     * Get authentication context for specific user
     */
    public AuthenticationContext getUserContext(String username) {
        return userContexts.get(username);
    }
    
    /**
     * Get all active user contexts
     */
    public Map<String, AuthenticationContext> getActiveUserContexts() {
        Map<String, AuthenticationContext> activeContexts = new HashMap<>();
        userContexts.forEach((username, context) -> {
            if (context.isActive()) {
                activeContexts.put(username, context);
            }
        });
        return activeContexts;
    }
    
    /**
     * Get token information
     */
    public TokenInfo getTokenInfo(String token) {
        return tokenCache.get(token);
    }
    
    /**
     * Validate token
     */
    public boolean isTokenValid(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        TokenInfo tokenInfo = tokenCache.get(token);
        return tokenInfo != null && !tokenInfo.isExpired();
    }
    
    /**
     * Get authentication statistics
     */
    public Map<String, Object> getAuthenticationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalUsers = userContexts.size();
        long activeUsers = userContexts.values().stream().mapToLong(ctx -> ctx.isActive() ? 1 : 0).sum();
        int totalTokens = tokenCache.size();
        long expiredTokens = tokenCache.values().stream().mapToLong(token -> token.isExpired() ? 1 : 0).sum();
        
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("currentUser", currentUser);
        stats.put("currentScheme", currentScheme.name());
        stats.put("totalTokens", totalTokens);
        stats.put("expiredTokens", expiredTokens);
        stats.put("validTokens", totalTokens - expiredTokens);
        
        // Scheme distribution
        Map<String, Integer> schemeDistribution = new HashMap<>();
        userContexts.values().forEach(ctx -> 
            schemeDistribution.merge(ctx.getScheme().name(), 1, Integer::sum));
        stats.put("schemeDistribution", schemeDistribution);
        
        return stats;
    }
    
    /**
     * Cleanup expired tokens and inactive sessions
     */
    public void cleanup() {
        // Remove expired tokens
        int expiredTokens = 0;
        Iterator<Map.Entry<String, TokenInfo>> tokenIterator = tokenCache.entrySet().iterator();
        while (tokenIterator.hasNext()) {
            Map.Entry<String, TokenInfo> entry = tokenIterator.next();
            if (entry.getValue().isExpired()) {
                tokenIterator.remove();
                expiredTokens++;
            }
        }
        
        // Cleanup inactive user contexts (inactive for more than 24 hours)
        int inactiveUsers = 0;
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        Iterator<Map.Entry<String, AuthenticationContext>> userIterator = userContexts.entrySet().iterator();
        while (userIterator.hasNext()) {
            Map.Entry<String, AuthenticationContext> entry = userIterator.next();
            AuthenticationContext context = entry.getValue();
            if (!context.isActive() && context.getLastActivity().isBefore(cutoff)) {
                userIterator.remove();
                inactiveUsers++;
            }
        }
        
        if (expiredTokens > 0 || inactiveUsers > 0) {
            logger.debug("Authentication cleanup: removed {} expired tokens and {} inactive users", 
                expiredTokens, inactiveUsers);
        }
    }
    
    // Private helper methods
    
    private AuthenticationResult performAuthentication(String username, String password, AuthenticationScheme scheme) {
        // This is a mock implementation for testing purposes
        // In a real implementation, this would make actual API calls
        
        // Simulate different authentication scenarios
        if ("testuser".equals(username) && "password".equals(password)) {
            String token = generateMockToken(username);
            LocalDateTime expiry = LocalDateTime.now().plusHours(1);
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", username);
            userInfo.put("email", username + "@company.com");
            userInfo.put("roles", Arrays.asList("user", "employee"));
            userInfo.put("department", "Engineering");
            
            return new AuthenticationResult(true, "Authentication successful", token, expiry, userInfo);
        } else if ("admin".equals(username) && "admin123".equals(password)) {
            String token = generateMockToken(username);
            LocalDateTime expiry = LocalDateTime.now().plusHours(8);
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", username);
            userInfo.put("email", "admin@company.com");
            userInfo.put("roles", Arrays.asList("admin", "user", "manager"));
            userInfo.put("department", "Administration");
            
            return new AuthenticationResult(true, "Authentication successful", token, expiry, userInfo);
        } else {
            return new AuthenticationResult(false, "Invalid credentials", null, null, null);
        }
    }
    
    private AuthenticationResult performTokenRefresh(String refreshToken) {
        // Mock token refresh implementation
        String newToken = generateMockToken("refreshed_user");
        LocalDateTime expiry = LocalDateTime.now().plusHours(1);
        
        return new AuthenticationResult(true, "Token refreshed successfully", newToken, expiry, null);
    }
    
    private String generateMockToken(String username) {
        // Generate a mock JWT-like token for testing
        return "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9." + 
               Base64.getEncoder().encodeToString(username.getBytes()) + "." +
               Base64.getEncoder().encodeToString(String.valueOf(System.currentTimeMillis()).getBytes());
    }
    
    @SuppressWarnings("unchecked")
    private void extractUserInfoIntoContext(Map<String, Object> userInfo, AuthenticationContext context) {
        if (userInfo.containsKey("roles")) {
            Object roles = userInfo.get("roles");
            if (roles instanceof List) {
                ((List<String>) roles).forEach(context::addRole);
            }
        }
        
        // Store additional metadata
        userInfo.forEach((key, value) -> {
            if (!"roles".equals(key)) {
                context.getMetadata().put(key, value);
            }
        });
    }
    
    private AuthenticationContext findContextByRefreshToken(String refreshToken) {
        return userContexts.values().stream()
            .filter(ctx -> refreshToken.equals(ctx.getRefreshToken()))
            .findFirst()
            .orElse(null);
    }
}