package com.springbootjavatest.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    private OrderState state;

    private String details;

    // Default and Parametised constructors for Order.
    public Order() {}
    public Order(String details) {
        this.details = details;
    }

    // When a newly created Order is added to the database, its state is automatically set to "CREATED".
    @PrePersist
    public void setDefaultState() {
        if (this.state == null) {
            this.state = OrderState.CREATED;
        }
    }

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public OrderState getState() { return state; }
    public void setState(OrderState state) { this.state = state; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}