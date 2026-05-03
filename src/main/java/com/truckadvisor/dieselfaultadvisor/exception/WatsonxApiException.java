package com.truckadvisor.dieselfaultadvisor.exception;

/**
 * Custom exception for watsonx.ai API errors
 */
public class WatsonxApiException extends RuntimeException {
    
    public WatsonxApiException(String message) {
        super(message);
    }
    
    public WatsonxApiException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Made with Bob
