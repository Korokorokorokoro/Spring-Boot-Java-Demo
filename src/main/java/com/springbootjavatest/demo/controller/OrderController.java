package com.springbootjavatest.demo.controller;

import com.springbootjavatest.demo.exception.InvalidOrderDataException;
import com.springbootjavatest.demo.exception.InvalidOrderStateException;
import com.springbootjavatest.demo.exception.OrderNotFoundException;
import com.springbootjavatest.demo.model.Order;
import com.springbootjavatest.demo.model.OrderState;
import com.springbootjavatest.demo.repository.OrderRepository;
import com.springbootjavatest.demo.service.EmailService;
import com.springbootjavatest.demo.service.SmsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/order")
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderRepository repo;
    private final EmailService emailService;
    private final SmsService smsService;

    public OrderController(OrderRepository repo, EmailService emailService, SmsService smsService) {
        this.repo = repo;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    @GetMapping
    public List<Order> getAll() {
        logger.debug("Fetching all orders");
        return repo.findAll();
    }

    // Create new Order.
    @PostMapping
    public ResponseEntity<Order> create(@RequestBody Order order) {
        logger.info("Creating new order with details: {}", order.getDetails());
        
        // Validate order data
        if (order.getDetails() == null || order.getDetails().trim().isEmpty()) {
            logger.error("Attempt to create order with empty details");
            throw new InvalidOrderDataException("Order details cannot be empty");
        }
        
        Order savedOrder = repo.save(order);
        logger.info("Order created successfully with ID: {}", savedOrder.getId());

        emailService.sendNotification(
            "New Order Created",
            "A new order has been created:\n\nDetails: " + order.getDetails() + "\nStatus: " + order.getState()
        );

        smsService.sendNotification(
            "New Order Created",
            "A new order has been created:\n\nDetails: " + order.getDetails() + "\nStatus: " + order.getState()
        );

        return new ResponseEntity<>(savedOrder, HttpStatus.CREATED);
    }

    // Update existing Order by Id.
    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable Long id, @RequestBody Order updatedOrder) {
        logger.info("Updating order with ID: {}", id);
        
        // Validate order data
        if (updatedOrder.getDetails() == null || updatedOrder.getDetails().trim().isEmpty()) {
            logger.error("Attempt to update order {} with empty details", id);
            throw new InvalidOrderDataException("Order details cannot be empty");
        }
        
        Optional<Order> existingOrderOpt = repo.findById(id);

        if (existingOrderOpt.isEmpty()) {
            logger.error("Order not found with ID: {}", id);
            throw new OrderNotFoundException(id);
        }

        Order existingOrder = existingOrderOpt.get();
        boolean stateChanged = !existingOrder.getState().equals(updatedOrder.getState());

        if(stateChanged) {
            logger.info("Order {} state changed from {} to {}", id, existingOrder.getState(), updatedOrder.getState());
            
            emailService.sendNotification(
                "Order Status Updated",
                        "Order ID " + updatedOrder.getId() + " has a new status:\n\n" +
                        "Old Status: " + existingOrder.getState() + "\n" +
                        "New Status: " + updatedOrder.getState()
            );

            smsService.sendNotification(
                "Order Status Updated",
                        "Order ID " + updatedOrder.getId() + " has a new status:\n\n" +
                        "Old Status: " + existingOrder.getState() + "\n" +
                        "New Status: " + updatedOrder.getState()
            );
        }

        existingOrder.setDetails(updatedOrder.getDetails());
        existingOrder.setState(updatedOrder.getState());
        Order savedOrder = repo.save(existingOrder);
        logger.info("Order {} updated successfully", id);
                
        return new ResponseEntity<>(savedOrder, HttpStatus.OK);
    }

    // Retrieve specific Order by Id.
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        logger.debug("Fetching order with ID: {}", id);
        
        return repo.findById(id)
            .map(order -> {
                logger.debug("Order found with ID: {}", id);
                return new ResponseEntity<>(order, HttpStatus.OK);
            })
            .orElseThrow(() -> {
                logger.error("Order not found with ID: {}", id);
                return new OrderNotFoundException(id);
            });
    }

    // Search Orders by details and/or state.
    @GetMapping("/search")
    public List<Order> search(
            @RequestParam(required = false) String details,
            @RequestParam(required = false) String state
    ) {
        logger.debug("Searching orders with details: '{}', state: '{}'", details, state);
        
        boolean hasDetails = details != null && !details.isEmpty();
        boolean hasState = state != null && !state.isEmpty();

        if (!hasDetails && !hasState) {
            logger.debug("No search criteria provided, returning all orders");
            return repo.findAll();
        }

        OrderState stateEnum = null;
        if (hasState) {
            try {
                stateEnum = OrderState.valueOf(state.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.error("Invalid order state provided: {}", state);
                throw new InvalidOrderStateException(state);
            }
        }

        List<Order> results;
        if (hasDetails && hasState) {
            results = repo.findByDetailsContainingIgnoreCaseOrState(details, stateEnum);
        } else if (hasDetails) {
            results = repo.findByDetailsContainingIgnoreCase(details);
        } else {
            results = repo.findByState(stateEnum);
        }
        
        logger.debug("Search returned {} orders", results.size());
        return results;
    }

    // Delete a specific Order by Id.
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        logger.info("Deleting order with ID: {}", id);
        
        // Verify order exists before attempting to delete
        if (!repo.existsById(id)) {
            logger.error("Cannot delete order - not found with ID: {}", id);
            throw new OrderNotFoundException(id);
        }
        
        repo.deleteById(id);
        logger.info("Order {} deleted successfully", id);
    }
}
