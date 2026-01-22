package com.springbootjavatest.demo.exception;

/**
 * Exception thrown when order validation fails.
 */
public class InvalidOrderDataException extends RuntimeException {
    
    public InvalidOrderDataException(String message) {
        super(message);
    }
}
