package com.springbootjavatest.demo.exception;

/**
 * Exception thrown when an Order is not found by ID.
 */
public class OrderNotFoundException extends RuntimeException {
    
    public OrderNotFoundException(Long id) {
        super("Order not found with ID: " + id);
    }
    
    public OrderNotFoundException(String message) {
        super(message);
    }
}
