package com.springbootjavatest.demo.exception;

/**
 * Exception thrown when invalid order state is provided.
 */
public class InvalidOrderStateException extends RuntimeException {
    
    public InvalidOrderStateException(String state) {
        super("Invalid order state: " + state + ". Valid states are: CREATED, IN_PROGRESS, COMPLETED, CANCELLED");
    }
    
    public InvalidOrderStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
