package com.phoenix.hrm.core.exceptions;

/**
 * Custom runtime exception for Phoenix HRM test automation framework.
 * Provides consistent error handling across the framework with detailed error information.
 * 
 * Features:
 * - Runtime exception (unchecked)
 * - Detailed error messages with context
 * - Cause chain support for root cause analysis
 * - Framework-specific error categorization
 * 
 * @author Phoenix HRM Test Automation Team
 * @version 1.0
 */
public class FrameworkException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new FrameworkException with the specified detail message.
     * 
     * @param message the detail message
     */
    public FrameworkException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new FrameworkException with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public FrameworkException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new FrameworkException with the specified cause.
     * 
     * @param cause the cause of the exception
     */
    public FrameworkException(Throwable cause) {
        super(cause);
    }
    
    /**
     * Constructs a new FrameworkException with the specified detail message, cause,
     * suppression enabled or disabled, and writable stack trace enabled or disabled.
     * 
     * @param message the detail message
     * @param cause the cause of the exception
     * @param enableSuppression whether or not suppression is enabled or disabled
     * @param writableStackTrace whether or not the stack trace should be writable
     */
    public FrameworkException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}