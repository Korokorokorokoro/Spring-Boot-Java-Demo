package com.springbootjavatest.demo.controller;

import com.springbootjavatest.demo.model.Order;
import com.springbootjavatest.demo.model.OrderState;
import com.springbootjavatest.demo.repository.OrderRepository;
import com.springbootjavatest.demo.service.EmailService;
import com.springbootjavatest.demo.service.SmsService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/order")
public class OrderController {
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
        return repo.findAll();
    }

    // Create new Order.
    @PostMapping
    public ResponseEntity<Order> create(@RequestBody Order order) {
        Order savedOrder = repo.save(order);

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
        Optional<Order> existingOrderOpt = repo.findById(id);

        if (existingOrderOpt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Order existingOrder = existingOrderOpt.get();
        boolean stateChanged = !existingOrder.getState().equals(updatedOrder.getState());

        if(stateChanged) {
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
                
        return new ResponseEntity<>(savedOrder, HttpStatus.OK);
    }

    // Retrieve specific Order by Id.
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
    return repo.findById(id)
            .map(order -> new ResponseEntity<>(order, HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // Search Orders by details and/or state.
    @GetMapping("/search")
    public List<Order> search(
            @RequestParam(required = false) String details,
            @RequestParam(required = false) String state
    ) {
        boolean hasDetails = details != null && !details.isEmpty();
        boolean hasState = state != null && !state.isEmpty();

        if (!hasDetails && !hasState) {
            return repo.findAll();
        }

        OrderState stateEnum = null;
        if (hasState) {
            try {
                stateEnum = OrderState.valueOf(state.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid state, return empty list
                return List.of();
            }
        }

        if (hasDetails && hasState) {
            return repo.findByDetailsContainingIgnoreCaseOrState(details, stateEnum);
        }
        if (hasDetails) {
            return repo.findByDetailsContainingIgnoreCase(details);
        }
        return repo.findByState(stateEnum);
    }

    // Delete a specific Order by Id.
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repo.deleteById(id);
    }
}
