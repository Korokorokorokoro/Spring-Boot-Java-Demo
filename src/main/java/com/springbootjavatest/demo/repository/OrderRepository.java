package com.springbootjavatest.demo.repository;

import com.springbootjavatest.demo.model.Order;
import com.springbootjavatest.demo.model.OrderState;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

// Repository interface for managing Order entities with custom query methods.
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByDetailsContainingIgnoreCase(String details);
    List<Order> findByState(OrderState state);
    List<Order> findByDetailsContainingIgnoreCaseOrState(String details, OrderState state);
}