package com.springbootjavatest.demo;

import com.springbootjavatest.demo.controller.OrderController;
import com.springbootjavatest.demo.exception.OrderNotFoundException;
import com.springbootjavatest.demo.model.Order;
import com.springbootjavatest.demo.model.OrderState;
import com.springbootjavatest.demo.repository.OrderRepository;
import com.springbootjavatest.demo.service.EmailService;
import com.springbootjavatest.demo.service.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class OrderControllerUnitTests {

    private OrderRepository repo;
    private EmailService emailService;
    private SmsService smsService;
    private OrderController controller;

    @BeforeEach
    void init() {
        repo = mock(OrderRepository.class);
        emailService = mock(EmailService.class);
        smsService = mock(SmsService.class);
        controller = new OrderController(repo, emailService, smsService);
    }

    @Test
    void createOrder_setsDefaultState_and_persists() {
        Order o = new Order("details create");
        when(repo.save(any(Order.class))).thenAnswer(inv -> {
            Order e = inv.getArgument(0);
            if (e.getId() == null) e.setId(1L);
            if (e.getState() == null) e.setState(OrderState.CREATED);
            return e;
        });

        ResponseEntity<Order> resp = controller.create(o);

        assertEquals(201, resp.getStatusCode().value());
        Order saved = resp.getBody();
        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals(OrderState.CREATED, saved.getState());
        assertEquals("details create", saved.getDetails());
        verify(emailService, times(1)).sendNotification(anyString(), anyString());
        verify(smsService, times(1)).sendNotification(anyString(), anyString());
    }

    @Test
    void getOrderById_returnsNotFound_whenMissing() {
        when(repo.findById(999L)).thenReturn(Optional.empty());
        
        // Now the controller throws OrderNotFoundException instead of returning NOT_FOUND
        assertThrows(OrderNotFoundException.class, () -> {
            controller.getOrderById(999L);
        });
    }

    @Test
    void searchFilters_byDetailsAndState() {
        Order a = new Order("apple item"); a.setId(1L); a.setState(OrderState.CREATED);
        Order b = new Order("banana item"); b.setId(2L); b.setState(OrderState.CREATED);
        Order c = new Order("apple completed"); c.setId(3L); c.setState(OrderState.COMPLETED);

        when(repo.findByDetailsContainingIgnoreCase("apple")).thenReturn(List.of(a, c));
        when(repo.findByState(OrderState.COMPLETED)).thenReturn(List.of(c));
        when(repo.findByDetailsContainingIgnoreCaseOrState("apple", OrderState.COMPLETED)).thenReturn(List.of(a, c));

        List<Order> byDetails = controller.search("apple", null);
        assertEquals(2, byDetails.size());

        List<Order> byState = controller.search(null, "COMPLETED");
        assertTrue(byState.size() >= 1);
        assertTrue(byState.stream().anyMatch(o -> "apple completed".equals(o.getDetails())));

        List<Order> byBoth = controller.search("apple", "COMPLETED");
        assertTrue(byBoth.size() >= 1);
    }

    @Test
    void updateOrder_changesState_and_triggersNotifications() {
        Order saved = new Order("to-update"); saved.setId(1L); saved.setState(OrderState.CREATED);
        when(repo.findById(saved.getId())).thenReturn(Optional.of(saved));
        when(repo.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        Order updated = new Order();
        updated.setId(saved.getId());
        updated.setDetails("updated");
        updated.setState(OrderState.COMPLETED);
        ResponseEntity<Order> resp = controller.updateOrder(saved.getId(), updated);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals(OrderState.COMPLETED, resp.getBody().getState());
        verify(emailService, times(1)).sendNotification(anyString(), anyString());
        verify(smsService, times(1)).sendNotification(anyString(), anyString());
    }
}
