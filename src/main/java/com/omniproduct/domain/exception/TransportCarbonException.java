package com.omniproduct.domain.exception;

public class TransportCarbonException extends RuntimeException {
    
    public TransportCarbonException(String message) {
        super(message);
    }
    
    public TransportCarbonException(String message, Throwable cause) {
        super(message, cause);
    }
}
