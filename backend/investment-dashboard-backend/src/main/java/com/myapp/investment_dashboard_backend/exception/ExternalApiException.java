package com.myapp.investment_dashboard_backend.exception;

/**
 * Exception thrown when there are errors interacting with external APIs
 */
public class ExternalApiException extends Exception {
    
    /**
     * Create a new ExternalApiException with a message
     * @param message The error message
     */
    public ExternalApiException(String message) {
        super(message);
    }
    
    /**
     * Create a new ExternalApiException with a message and cause
     * @param message The error message
     * @param cause The underlying cause
     */
    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }
} 